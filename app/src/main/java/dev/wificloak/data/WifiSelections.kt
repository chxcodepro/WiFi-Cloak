package dev.wificloak.data

object WifiSelections {
    fun normalize(config: CloakConfig): CloakConfig = config.copy(
        profiles = config.profiles.map { it.copy(offline = true) },
        rules = config.activeProfile?.let { active -> config.rules.map { it.copy(profileId = active.id) } } ?: emptyList()
    )

    fun saveAndUse(config: CloakConfig, wifi: ScannedWifi, mac: String, now: Long = System.currentTimeMillis()): CloakConfig {
        val existing = config.profiles.find { it.ssid == wifi.ssid && it.bssid.equals(wifi.bssid, true) }
        val profile = (existing ?: WifiProfile(name = wifi.ssid, ssid = wifi.ssid, bssid = wifi.bssid, mac = mac, createdAt = now))
            .copy(ssid = wifi.ssid, bssid = wifi.bssid.lowercase(), mac = mac.lowercase(), frequency = wifi.frequency,
                rssi = wifi.rssi, offline = true, lastUsedAt = now, uses = (existing?.uses ?: 0) + 1)
        require(ProfileValidator.errors(profile).isEmpty()) { "扫描信息不完整，请重新扫描" }
        return config.copy(activeProfileId = profile.id,
            profiles = config.profiles.filterNot { it.id == profile.id } + profile,
            rules = config.rules.map { it.copy(profileId = profile.id) })
    }

    fun useSaved(config: CloakConfig, id: String, now: Long = System.currentTimeMillis()): CloakConfig {
        require(config.profiles.any { it.id == id }) { "备用 WiFi 已移除，请重新选择" }
        return config.copy(activeProfileId = id,
            profiles = config.profiles.map { if (it.id == id) it.copy(offline = true, lastUsedAt = now, uses = it.uses + 1) else it },
            rules = config.rules.map { it.copy(profileId = id) })
    }
}
