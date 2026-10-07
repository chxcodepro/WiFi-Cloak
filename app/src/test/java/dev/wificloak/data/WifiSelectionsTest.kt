package dev.wificloak.data

import org.junit.Assert.*
import org.junit.Test

class WifiSelectionsTest {
    private val wifi = ScannedWifi("Studio_5G", "3c:84:6a:12:7b:90", 5180, -48)
    private val mac = "a2:6c:84:19:2e:70"

    @Test fun selectedScanIsSavedWithCapturedMacAndOfflineReuse() {
        val saved = WifiSelections.saveAndUse(CloakConfig(), wifi, mac, now = 1000)
        val restored = CloakConfig.decode(saved.encode())
        assertEquals(wifi.ssid, restored.activeProfile!!.ssid)
        assertEquals(wifi.bssid, restored.activeProfile!!.bssid)
        assertEquals(mac, restored.activeProfile!!.mac)
        assertTrue(restored.activeProfile!!.offline)
        assertEquals(1, restored.profiles.size)
    }

    @Test fun repeatingScanUpdatesExistingBackupRatherThanDuplicatingIt() {
        val first = WifiSelections.saveAndUse(CloakConfig(), wifi, mac, now = 1000)
        val second = WifiSelections.saveAndUse(first, wifi.copy(bssid = wifi.bssid.uppercase(), rssi = -60), mac, now = 2000)
        assertEquals(1, second.profiles.size)
        assertEquals(first.activeProfileId, second.activeProfileId)
        assertEquals(1000L, second.activeProfile!!.createdAt)
        assertEquals(2000L, second.activeProfile!!.lastUsedAt)
        assertEquals(2, second.activeProfile!!.uses)
        assertEquals(-60, second.activeProfile!!.rssi)
    }

    @Test fun sameSsidOnDifferentAccessPointsRemainsSeparate() {
        val first = WifiSelections.saveAndUse(CloakConfig(), wifi, mac, now = 1000)
        val second = WifiSelections.saveAndUse(first, wifi.copy(bssid = "3c:84:6a:12:7b:92"), mac, now = 2000)
        assertEquals(2, second.profiles.size)
        assertNotEquals(first.activeProfileId, second.activeProfileId)
    }

    @Test fun selectingBackupKeepsItsMacAndMovesAllControlledApps() {
        val first = WifiSelections.saveAndUse(CloakConfig(), wifi, mac, now = 1000)
        val second = WifiSelections.saveAndUse(first, wifi.copy(ssid = "Garden", bssid = "3c:84:6a:12:7b:92"), "b2:6c:84:19:2e:70", now = 2000)
        val config = second.copy(rules = listOf(AppRule("app.one", second.activeProfileId!!), AppRule("app.two", second.activeProfileId)))
        val selected = WifiSelections.useSaved(config, first.activeProfileId!!, now = 3000)
        assertEquals(mac, selected.activeProfile!!.mac)
        assertTrue(selected.rules.all { it.profileId == first.activeProfileId })
        assertTrue(selected.activeProfile!!.offline)
        assertEquals(2, selected.activeProfile!!.uses)
    }

    @Test fun unreadableDeviceMacCannotCreateBackup() {
        assertThrows(IllegalArgumentException::class.java) {
            WifiSelections.saveAndUse(CloakConfig(), wifi, "02:00:00:00:00:00")
        }
    }

    @Test fun legacyAssignmentsBecomeOneSelectedWifiWithoutDeletingBackups() {
        val first = WifiSelections.saveAndUse(CloakConfig(), wifi, mac, now = 1000)
        val second = WifiSelections.saveAndUse(first, wifi.copy(ssid = "Garden", bssid = "3c:84:6a:12:7b:92"), mac, now = 2000)
        val legacy = second.copy(profiles = second.profiles.map { it.copy(offline = false) },
            rules = listOf(AppRule("app.one", first.activeProfileId!!), AppRule("app.two", second.activeProfileId!!)))
        val normalized = WifiSelections.normalize(legacy)
        assertEquals(2, normalized.profiles.size)
        assertTrue(normalized.profiles.all { it.offline })
        assertTrue(normalized.rules.all { it.profileId == second.activeProfileId })
        assertEquals(second.activeProfile!!.uses, normalized.activeProfile!!.uses)
    }

    @Test fun rootOutputSkipsRedactedAndMalformedAddresses() {
        assertEquals(mac, MacAddresses.firstValid("02:00:00:00:00:00\n$mac\n"))
        assertNull(MacAddresses.firstValid("permission denied"))
        assertNull(MacAddresses.firstValid("ff:ff:ff:ff:ff:ff"))
        assertNull(MacAddresses.firstValid("aa:bb:cc:dd:ee:ff:12:34"))
    }
}
