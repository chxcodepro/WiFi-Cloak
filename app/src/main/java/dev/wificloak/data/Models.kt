package dev.wificloak.data

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

const val CONFIG_GROUP = "wifi_cloak"
const val SNAPSHOT_KEY = "snapshot"

data class WifiProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ssid: String,
    val bssid: String,
    val mac: String,
    val offline: Boolean = true,
    val frequency: Int = 5180,
    val rssi: Int = -48,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0,
    val uses: Int = 0
)

data class AppRule(val packageName: String, val profileId: String)

data class CloakConfig(
    val enabled: Boolean = true,
    val activeProfileId: String? = null,
    val profiles: List<WifiProfile> = emptyList(),
    val rules: List<AppRule> = emptyList()
) {
    val activeProfile get() = profiles.find { it.id == activeProfileId }
    fun profileFor(packageName: String): WifiProfile? =
        rules.find { it.packageName == packageName }?.let { rule -> profiles.find { it.id == rule.profileId } }

    fun encode(): String = JSONObject().apply {
        put("version", 1)
        put("enabled", enabled)
        put("activeProfileId", activeProfileId ?: JSONObject.NULL)
        put("profiles", JSONArray().apply {
            profiles.forEach { p -> put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("ssid", p.ssid)
                put("bssid", p.bssid); put("mac", p.mac); put("offline", p.offline)
                put("frequency", p.frequency); put("rssi", p.rssi)
                put("createdAt", p.createdAt); put("lastUsedAt", p.lastUsedAt); put("uses", p.uses)
            }) }
        })
        put("rules", JSONArray().apply {
            rules.forEach { put(JSONObject().put("packageName", it.packageName).put("profileId", it.profileId)) }
        })
    }.toString()

    companion object {
        fun decode(raw: String?): CloakConfig {
            if (raw.isNullOrBlank()) return CloakConfig()
            return decodeSnapshot(raw) ?: CloakConfig(enabled = false)
        }

        internal fun decodeSnapshot(raw: String?): CloakConfig? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val json = JSONObject(raw)
                require(json.optInt("version") == 1)
                var damaged = false
                val profiles = json.getJSONArray("profiles").let { list ->
                    (0 until list.length()).mapNotNull { index -> runCatching { list.getJSONObject(index).let { p ->
                        WifiProfile(
                            id = p.getString("id"), name = p.getString("name"), ssid = p.getString("ssid"),
                            bssid = p.getString("bssid"), mac = p.getString("mac"),
                            offline = p.optBoolean("offline", true), frequency = p.optInt("frequency", 5180),
                            rssi = p.optInt("rssi", -48), createdAt = p.optLong("createdAt"),
                            lastUsedAt = p.optLong("lastUsedAt"), uses = p.optInt("uses")
                        ).also { require(ProfileValidator.errors(it).isEmpty()) }
                    } }.getOrElse { damaged = true; null } }
                }.distinctBy { it.id }.also { if (it.size != json.getJSONArray("profiles").length()) damaged = true }
                val rules = (json.optJSONArray("rules") ?: JSONArray().also { damaged = true }).let { list ->
                    (0 until list.length()).mapNotNull { index -> runCatching { list.getJSONObject(index).let {
                        AppRule(it.getString("packageName"), it.getString("profileId"))
                    } }.getOrElse { damaged = true; null } }.filter { rule -> profiles.any { it.id == rule.profileId } }
                        .distinctBy { it.packageName }
                }
                val active = if (json.isNull("activeProfileId")) null else json.optString("activeProfileId")
                CloakConfig(json.optBoolean("enabled", true) && !damaged, active?.takeIf { id -> profiles.any { it.id == id } }, profiles, rules)
            }.getOrNull()
        }
    }
}

object ProfileValidator {
    private val address = Regex("^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}$")
    fun errors(p: WifiProfile): Map<String, String> = buildMap {
        if (p.name.isBlank() || p.name.length > 40) put("name", "输入 1–40 个字符")
        if (p.ssid.isBlank() || p.ssid.toByteArray(StandardCharsets.UTF_8).size > 32 || p.ssid.any { it.isISOControl() })
            put("ssid", "SSID 需为 1–32 字节")
        if (!validAddress(p.bssid)) put("bssid", "输入有效的单播 BSSID")
        if (!validAddress(p.mac)) put("mac", "输入有效的单播 MAC")
        if (p.frequency !in 2400..7125) put("frequency", "频率超出有效范围")
        if (p.rssi !in -127..-1) put("rssi", "信号强度超出有效范围")
    }
    fun validAddress(value: String): Boolean = address.matches(value) &&
        value.substring(0, 2).toInt(16).and(1) == 0 &&
        value != "00:00:00:00:00:00" && !value.equals("02:00:00:00:00:00", true)
}
