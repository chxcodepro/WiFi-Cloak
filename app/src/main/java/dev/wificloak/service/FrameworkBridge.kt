package dev.wificloak.service

import dev.wificloak.data.CONFIG_GROUP
import dev.wificloak.data.CloakConfig
import dev.wificloak.data.SNAPSHOT_KEY
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class FrameworkState(
    val connected: Boolean = false,
    val name: String = "LSPosed 未连接",
    val version: String = "",
    val api: Int = 0,
    val scope: Set<String> = emptySet(),
    val error: String? = null
)

interface ScopeGateway {
    suspend fun request(packageName: String): Boolean
    fun remove(packageName: String)
    fun scope(): Set<String>
}

class FrameworkBridge : ScopeGateway {
    @Volatile private var service: XposedService? = null
    private val connectionLock = Any()
    private val mutable = MutableStateFlow(FrameworkState())
    val state = mutable.asStateFlow()
    private val listener = object : XposedServiceHelper.OnServiceListener {
        override fun onServiceBind(service: XposedService) {
            synchronized(connectionLock) { this@FrameworkBridge.service = service }
            refresh()
        }
        override fun onServiceDied(service: XposedService) {
            synchronized(connectionLock) {
                if (this@FrameworkBridge.service === service) {
                    this@FrameworkBridge.service = null
                    mutable.value = FrameworkState(error = "框架连接已断开")
                }
            }
        }
    }

    fun connect() = XposedServiceHelper.registerListener(listener)

    fun refresh() {
        val current = service ?: return
        val refreshed = runCatching {
            FrameworkState(true, current.frameworkName, current.frameworkVersion, current.apiVersion, current.scope.toSet())
        }.getOrElse { FrameworkState(error = "无法读取框架状态") }
        synchronized(connectionLock) {
            if (service === current) mutable.value = refreshed
        }
    }

    override fun scope(): Set<String> = requireService().scope.toSet()

    override suspend fun request(packageName: String): Boolean = suspendCancellableCoroutine { continuation ->
        runCatching {
            requireService().requestScope(listOf(packageName), object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) {
                    refresh()
                    if (continuation.isActive) continuation.resume(packageName in approved)
                }
                override fun onScopeRequestFailed(message: String) {
                    refresh()
                    mutable.value = mutable.value.copy(error = message)
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }.onFailure {
            mutable.value = mutable.value.copy(error = "作用域申请失败")
            if (continuation.isActive) continuation.resume(false)
        }
    }

    override fun remove(packageName: String) {
        requireService().removeScope(listOf(packageName))
        refresh()
        check(packageName !in scope()) { "框架尚未移除作用域" }
    }

    fun readConfig(): CloakConfig? {
        val raw = requireService().getRemotePreferences(CONFIG_GROUP).getString(SNAPSHOT_KEY, null) ?: return null
        return CloakConfig.decodeSnapshot(raw) ?: error("框架配置读取失败，未覆盖原始数据")
    }

    fun publish(config: CloakConfig) {
        val current = requireService()
        val actual = current.scope.toSet()
        val published = config.copy(rules = config.rules.filter { it.packageName in actual })
        check(current.getRemotePreferences(CONFIG_GROUP).edit().putString(SNAPSHOT_KEY, published.encode()).commit()) {
            "框架配置同步失败"
        }
    }

    private fun requireService(): XposedService = service?.also {
        check(it.apiVersion >= 101) { "需要现代 Xposed API 101 或更高版本" }
    } ?: error("先在 LSPosed 启用模块并重新打开")
}
