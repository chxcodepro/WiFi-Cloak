package dev.wificloak.hook

import android.content.SharedPreferences
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Log
import dev.wificloak.data.CONFIG_GROUP
import dev.wificloak.data.CloakConfig
import dev.wificloak.data.SNAPSHOT_KEY
import dev.wificloak.data.WifiProfile
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

class WifiModule : XposedModule() {
    private lateinit var preferences: SharedPreferences
    @Volatile private var config = CloakConfig(enabled = false)
    @Volatile private var nextRefresh = 0L
    @Volatile private var lastRaw: String? = null
    private lateinit var packageName: String
    private val installed = ConcurrentHashMap.newKeySet<String>()
    private val failures = ConcurrentHashMap.newKeySet<String>()
    private val reentrant = ThreadLocal.withInitial { false }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SNAPSHOT_KEY) nextRefresh = 0L
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName == "dev.wificloak" || param.packageName == "android") return
        if (!installed.add(param.packageName)) return
        packageName = param.packageName
        try {
            preferences = getRemotePreferences(CONFIG_GROUP)
            runCatching { preferences.registerOnSharedPreferenceChangeListener(listener) }
            installWifiHooks()
        } catch (error: Throwable) {
            log(Log.ERROR, "WifiCloak", "WiFi hooks unavailable for ${param.packageName}", error)
        }
    }

    @Synchronized
    private fun currentProfile(): WifiProfile? {
        val now = SystemClock.elapsedRealtime()
        if (now >= nextRefresh) {
            nextRefresh = now + 500
            try {
                val raw = preferences.getString(SNAPSHOT_KEY, null)
                if (raw != lastRaw) { config = CloakConfig.decode(raw); lastRaw = raw }
            } catch (_: Throwable) { config = CloakConfig(enabled = false); nextRefresh = now + 1500 }
        }
        return if (config.enabled) config.profileFor(packageName) else null
    }

    private fun installWifiHooks() {
        infoValue("getSSID") { "\"${it.ssid}\"" }
        infoValue("getBSSID") { it.bssid }
        infoValue("getMacAddress") { it.mac }
        infoValue("getWifiSsid") { wifiSsid(it.ssid) }
        infoValue("getHiddenSSID") { false }
        infoValue("getSupplicantState") { SupplicantState.COMPLETED }
        infoValue("getNetworkId") { 1 }
        infoValue("getRssi") { it.rssi }
        infoValue("getFrequency") { it.frequency }
        infoValue("getLinkSpeed") { 144 }
        intercept(WifiManager::class.java, "getConnectionInfo") { chain, profile ->
            if (profile.offline) makeInfo(profile) else {
                val real = chain.proceed()
                if (real is WifiInfo && real.networkId >= 0) makeInfo(profile) else real
            }
        }
        intercept(WifiManager::class.java, "getScanResults") { chain, profile ->
            when {
                profile.offline -> safeScan(profile)
                connectionState(chain.thisObject) == false -> chain.proceed()
                else -> safeScan(profile)
            }
        }
        intercept(WifiManager::class.java, "getConfiguredNetworks") { chain, profile ->
            when {
                profile.offline -> safeConfiguration(profile)
                connectionState(chain.thisObject) == false -> chain.proceed()
                else -> safeConfiguration(profile)
            }
        }
        intercept(NetworkCapabilities::class.java, "getTransportInfo") { chain, profile ->
            val original = chain.proceed()
            if (original is WifiInfo && (profile.offline || original.networkId >= 0)) makeInfo(profile) else original
        }
        intercept(NetworkInterface::class.java, "getHardwareAddress") { chain, profile ->
            val network = chain.thisObject as? NetworkInterface
            if (network?.name?.let { it.startsWith("wlan") || it.startsWith("wifi") } == true)
                profile.mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
            else chain.proceed()
        }
    }

    @Suppress("DEPRECATION")
    private fun connectionState(manager: Any?): Boolean? {
        reentrant.set(true)
        return try { (manager as? WifiManager)?.connectionInfo?.networkId?.let { it >= 0 } }
        catch (error: Throwable) { reportFailure("connectionState", error); null }
        finally { reentrant.set(false) }
    }

    private fun infoValue(method: String, value: (WifiProfile) -> Any?) {
        intercept(WifiInfo::class.java, method) { chain, profile ->
            val info = chain.thisObject as? WifiInfo
            val connected = if (profile.offline) true else {
                reentrant.set(true)
                try { info?.networkId?.let { it >= 0 } == true } finally { reentrant.set(false) }
            }
            if (connected) value(profile) else chain.proceed()
        }
    }

    private fun intercept(type: Class<*>, name: String, handler: (XposedInterface.Chain, WifiProfile) -> Any?) {
        val method = type.declaredMethods.find { it.name == name && it.parameterCount == 0 } ?: return
        try {
            hook(method).intercept { chain ->
                if (reentrant.get()) chain.proceed() else {
                    val profile = currentProfile()
                    if (profile == null) chain.proceed() else handler(chain, profile)
                }
            }
        } catch (error: Throwable) {
            log(Log.WARN, "WifiCloak", "Hook unavailable: ${type.simpleName}.$name", error)
        }
    }

    private fun wifiSsid(ssid: String): Any? = runCatching {
        val type = Class.forName("android.net.wifi.WifiSsid")
        val creator = type.declaredMethods.firstOrNull { it.name == "fromBytes" && it.parameterCount == 1 }
            ?: type.getDeclaredMethod("createFromByteArray", ByteArray::class.java)
        creator.isAccessible = true
        creator.invoke(null, ssid.toByteArray(Charsets.UTF_8))
    }.onFailure { reportFailure("WifiSsid", it) }.getOrNull()

    private fun makeInfo(profile: WifiProfile): WifiInfo? = runCatching {
        val constructor = WifiInfo::class.java.getDeclaredConstructor().apply { isAccessible = true }
        val info = constructor.newInstance()
        fun set(name: String, type: Class<*>, value: Any) {
            WifiInfo::class.java.getDeclaredMethod(name, type).apply { isAccessible = true }.invoke(info, value)
        }
        val ssid = wifiSsid(profile.ssid) ?: error("Synthetic WifiSsid unavailable")
        set("setSSID", ssid.javaClass, ssid)
        set("setBSSID", String::class.java, profile.bssid)
        set("setMacAddress", String::class.java, profile.mac)
        set("setNetworkId", Int::class.javaPrimitiveType!!, 1)
        set("setRssi", Int::class.javaPrimitiveType!!, profile.rssi)
        set("setFrequency", Int::class.javaPrimitiveType!!, profile.frequency)
        set("setLinkSpeed", Int::class.javaPrimitiveType!!, 144)
        set("setSupplicantState", SupplicantState::class.java, SupplicantState.COMPLETED)
        info
    }.onFailure { reportFailure("WifiInfo", it) }.getOrNull()

    private fun safeScan(profile: WifiProfile): List<ScanResult> = runCatching { listOf(makeScan(profile)) }
        .getOrElse { reportFailure("ScanResult", it); emptyList() }

    private fun safeConfiguration(profile: WifiProfile): List<WifiConfiguration> = runCatching { listOf(makeConfiguration(profile)) }
        .getOrElse { reportFailure("WifiConfiguration", it); emptyList() }

    private fun reportFailure(label: String, error: Throwable) {
        if (failures.add(label)) log(Log.WARN, "WifiCloak", "Synthetic $label unavailable; real identity withheld", error)
    }

    @Suppress("DEPRECATION")
    private fun makeScan(profile: WifiProfile): ScanResult = ScanResult::class.java.getDeclaredConstructor()
        .apply { isAccessible = true }.newInstance().apply {
        SSID = profile.ssid
        BSSID = profile.bssid
        capabilities = "[WPA2-PSK-CCMP][ESS]"
        level = profile.rssi
        frequency = profile.frequency
        timestamp = SystemClock.elapsedRealtimeNanos() / 1000
        wifiSsid(profile.ssid)?.let { value ->
            runCatching { ScanResult::class.java.getField("wifiSsid").set(this, value) }
        }
    }

    @Suppress("DEPRECATION")
    private fun makeConfiguration(profile: WifiProfile): WifiConfiguration = WifiConfiguration().apply {
        SSID = "\"${profile.ssid}\""
        BSSID = profile.bssid
        networkId = 1
        status = WifiConfiguration.Status.CURRENT
    }
}
