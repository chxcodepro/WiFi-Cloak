package dev.wificloak

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wificloak.data.AppRule
import dev.wificloak.data.CloakConfig
import dev.wificloak.data.ConfigStore
import dev.wificloak.data.InstalledApp
import dev.wificloak.data.InstalledApps
import dev.wificloak.data.ScannedWifi
import dev.wificloak.data.WifiScan
import dev.wificloak.data.WifiSelections
import dev.wificloak.data.WifiCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ManagerViewModel(application: Application) : AndroidViewModel(application) {
    val bridge = (application as CloakApplication).bridge
    private val store = ConfigStore(application)
    private val mutableConfig = MutableStateFlow(WifiSelections.normalize(store.read()))
    val config = mutableConfig.asStateFlow()
    private val mutableApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps = mutableApps.asStateFlow()
    val loading = MutableStateFlow(true)
    val pending = MutableStateFlow<Set<String>>(emptySet())
    val syncError = MutableStateFlow<String?>(null)
    val scanned = MutableStateFlow<WifiScan?>(null)
    val scanning = MutableStateFlow(false)
    val selecting = MutableStateFlow<String?>(null)
    private val wifiCapture = WifiCapture(application)
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { InstalledApps(application).load() } }
                .onSuccess { mutableApps.value = it }.onFailure { messages.send("应用列表读取失败，请重试") }
            loading.value = false
        }
        viewModelScope.launch {
            bridge.state.map { it.connected }.distinctUntilChanged().collect { connected -> if (connected) sync() }
        }
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        bridge.refresh()
        if (bridge.state.value.connected) sync()
    }

    fun reloadApps() = viewModelScope.launch {
        loading.value = true
        runCatching { withContext(Dispatchers.IO) { InstalledApps(getApplication()).load() } }
            .onSuccess { mutableApps.value = it }.onFailure { messages.send("应用列表读取失败") }
        loading.value = false
    }

    private suspend fun persist(next: CloakConfig) {
        withContext(Dispatchers.IO) { store.write(next) }
        mutableConfig.value = next
        if (bridge.state.value.connected) {
            runCatching { withContext(Dispatchers.IO) { bridge.publish(next) } }
                .onSuccess { syncError.value = null }
                .onFailure { syncError.value = it.message ?: "框架配置同步失败" }
        } else syncError.value = "等待连接 LSPosed"
    }

    fun sync() = viewModelScope.launch {
        mutex.withLock {
            runCatching {
                val recovered = withContext(Dispatchers.IO) { store.recoverIfNeeded(bridge::readConfig) }
                if (recovered != null) mutableConfig.value = WifiSelections.normalize(recovered)
                withContext(Dispatchers.IO) { bridge.publish(mutableConfig.value) }
            }
                .onSuccess { syncError.value = null }
                .onFailure { syncError.value = it.message ?: "同步失败" }
        }
    }

    fun useProfile(id: String) = mutate("备用 WiFi 已选用") { WifiSelections.useSaved(it, id) }

    fun deleteProfile(id: String) = mutate("配置已删除") { previous ->
        check(previous.activeProfileId != id && previous.rules.none { it.profileId == id }) { "先选择其他 WiFi，再移除此备用项" }
        val remaining = previous.profiles.filterNot { it.id == id }
        previous.copy(profiles = remaining, activeProfileId = previous.activeProfileId.takeUnless { it == id } ?: remaining.firstOrNull()?.id)
    }

    fun setEnabled(value: Boolean) = mutate(if (value) "控制已开启" else "控制已暂停") { it.copy(enabled = value) }

    fun toggleApp(packageName: String, enable: Boolean) {
        if (packageName in pending.value) return
        if (enable && mutableConfig.value.activeProfile == null) { notify("先扫描并选择 WiFi，或选用备用 WiFi"); return }
        if (!bridge.state.value.connected) { notify("先在 LSPosed 启用模块并重新打开"); return }
        pending.value += packageName
        viewModelScope.launch {
            try {
                val approved = if (enable) {
                    if (withContext(Dispatchers.IO) { packageName in bridge.scope() }) true
                    else withTimeoutOrNull(60_000) { withContext(Dispatchers.IO) { bridge.request(packageName) } } == true
                } else {
                    withContext(Dispatchers.IO) { bridge.remove(packageName) }
                    true
                }
                if (!approved) { messages.send("未获得作用域授权，可重新勾选"); return@launch }
                mutex.withLock {
                    val previous = mutableConfig.value
                    val profile = previous.activeProfile
                    if (enable && profile == null) error("配置已变更，请重新选择")
                    val remaining = previous.rules.filterNot { it.packageName == packageName }
                    persist(previous.copy(rules = if (enable) remaining + AppRule(packageName, profile!!.id) else remaining,
                        profiles = if (enable) previous.profiles.map { if (it.id == profile!!.id) it.copy(lastUsedAt = System.currentTimeMillis(), uses = it.uses + 1) else it } else previous.profiles))
                }
                messages.send(if (syncError.value != null) "已保存，等待配置同步" else if (enable) "已授权，重启目标应用生效" else "已移除，重启目标应用生效")
            } catch (error: Exception) { messages.send(error.message ?: "作用域操作失败") }
            finally { pending.value -= packageName }
        }
    }

    fun scanWifi() = viewModelScope.launch {
        if (scanning.value) return@launch
        scanning.value = true
        try {
            scanned.value = withContext(Dispatchers.IO) { wifiCapture.scan() }
            if (scanned.value?.fresh == false) messages.send("扫描受系统限制，显示最近结果")
        } catch (error: SecurityException) { messages.send("请授予精确定位权限后重新扫描") }
        catch (error: Exception) { messages.send(error.message ?: "WiFi 扫描失败") }
        finally { scanning.value = false }
    }

    fun useScannedWifi(wifi: ScannedWifi) {
        if (selecting.value != null) return
        selecting.value = wifi.key
        viewModelScope.launch {
            try {
                val mac = withContext(Dispatchers.IO) { wifiCapture.deviceMac() }
                mutex.withLock { persist(WifiSelections.saveAndUse(mutableConfig.value, wifi, mac)) }
                messages.send(if (syncError.value != null) "WiFi 已保存为备用，等待框架同步" else "WiFi 已选用并保存为备用，重启目标应用生效")
            } catch (error: Exception) { messages.send(error.message ?: "WiFi 采集失败，请重试") }
            finally { selecting.value = null }
        }
    }

    fun notify(message: String) { messages.trySend(message) }

    private fun mutate(message: String, success: () -> Unit = {}, failure: () -> Unit = {}, action: (CloakConfig) -> CloakConfig) = viewModelScope.launch {
        mutex.withLock {
            runCatching { persist(action(mutableConfig.value)) }
                .onSuccess {
                    success()
                    messages.send(if (syncError.value != null) "$message，等待配置同步" else "$message，重启目标应用生效")
                }
                .onFailure { failure(); messages.send(it.message ?: "保存失败") }
        }
    }
}
