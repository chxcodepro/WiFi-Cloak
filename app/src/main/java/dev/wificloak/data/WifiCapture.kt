package dev.wificloak.data

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class ScannedWifi(val ssid: String, val bssid: String, val frequency: Int, val rssi: Int) {
    val key: String get() = "$ssid\u0000${bssid.lowercase()}"
}

data class WifiScan(val networks: List<ScannedWifi>, val fresh: Boolean)

class WifiCapture(private val context: Context) {
    private val manager = context.applicationContext.getSystemService(WifiManager::class.java)

    suspend fun scan(): WifiScan {
        check(manager.isWifiEnabled) { "打开 WiFi 后再扫描" }
        check(context.getSystemService(LocationManager::class.java).isLocationEnabled) { "打开系统定位后再扫描" }
        return withTimeoutOrNull(15_000) { awaitScan() } ?: results(false)
    }

    @Suppress("DEPRECATION")
    private suspend fun awaitScan(): WifiScan = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean(false)
        val registered = AtomicBoolean(false)
        lateinit var receiver: BroadcastReceiver
        fun cleanup() {
            if (registered.compareAndSet(true, false)) runCatching { context.unregisterReceiver(receiver) }
        }
        fun finish(result: Result<WifiScan>) {
            if (completed.compareAndSet(false, true)) {
                cleanup()
                if (continuation.isActive) continuation.resumeWith(result)
            }
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                    finish(runCatching { results(intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) })
            }
        }
        continuation.invokeOnCancellation { completed.set(true); cleanup() }
        try {
            ContextCompat.registerReceiver(context, receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), ContextCompat.RECEIVER_EXPORTED)
            registered.set(true)
            if (!continuation.isActive) cleanup()
            else if (!manager.startScan()) finish(runCatching { results(false) })
        } catch (error: Exception) { finish(Result.failure(error)) }
    }

    @Suppress("DEPRECATION")
    private fun results(fresh: Boolean): WifiScan {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            throw SecurityException("精确定位权限不可用")
        return WifiScan(
        manager.scanResults.mapNotNull { result ->
            val ssid = result.SSID.orEmpty()
            val bssid = result.BSSID.orEmpty().lowercase()
            if (ssid.isBlank() || !ProfileValidator.validAddress(bssid)) null
            else ScannedWifi(ssid, bssid, result.frequency, result.level.coerceIn(-127, -1))
        }.sortedByDescending { it.rssi }.distinctBy { it.key }, fresh
        )
    }

    @Suppress("DEPRECATION")
    fun deviceMac(): String {
        val direct = runCatching { manager.connectionInfo?.macAddress }.getOrNull()
        MacAddresses.firstValid(direct.orEmpty())?.let { return it }
        val hardware = runCatching {
            NetworkInterface.getNetworkInterfaces().toList().sortedBy { it.name }
                .filter { it.name.startsWith("wlan") || it.name.startsWith("wifi") }
                .mapNotNull { network -> network.hardwareAddress?.joinToString(":") { "%02x".format(it.toInt().and(255)) } }
        }.getOrDefault(emptyList())
        hardware.firstOrNull(ProfileValidator::validAddress)?.let { return it }
        val process = try {
            ProcessBuilder("su", "-c", "for p in /sys/class/net/wlan*/address /sys/class/net/wifi*/address; do [ -f \"\$p\" ] && cat \"\$p\"; done; exit 0")
                .redirectErrorStream(true).start()
        } catch (_: Exception) { error("无法读取设备 MAC，请授予 Root 后重试") }
        try {
            check(process.waitFor(25, TimeUnit.SECONDS)) { "Root 授权超时，请授权后重试" }
            check(process.exitValue() == 0) { "Root 未授权，无法采集设备 MAC" }
            return MacAddresses.firstValid(process.inputStream.bufferedReader().readText())
                ?: error("未读取到有效设备 MAC，请检查 WiFi 和 Root 授权")
        } finally {
            process.destroyForcibly()
            runCatching { process.inputStream.close(); process.errorStream.close(); process.outputStream.close() }
        }
    }
}

object MacAddresses {
    private val pattern = Regex("(?i)(?<![0-9a-f:])[0-9a-f]{2}(?::[0-9a-f]{2}){5}(?![0-9a-f:])")
    fun firstValid(raw: String): String? = pattern.findAll(raw).map { it.value.lowercase() }.firstOrNull(ProfileValidator::validAddress)
}
