package dev.wificloak.data

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class ConfigTest {
    private fun profile(id: String = "office") = WifiProfile(id, "工作室", "Studio_5G", "3c:84:6a:12:7b:90", "a2:6c:84:19:2e:70",
        offline = true, createdAt = 123, lastUsedAt = 456, uses = 3)

    @Test fun offlineIdentityAndUseHistorySurvivePersistence() {
        val profile = profile()
        val config = CloakConfig(activeProfileId = profile.id, profiles = listOf(profile), rules = listOf(AppRule("test.app", profile.id)))
        assertEquals(config, CloakConfig.decode(config.encode()))
        assertTrue(CloakConfig.decode(config.encode()).profileFor("test.app")!!.offline)
    }

    @Test fun perAppAssignmentDoesNotFollowAnUnrelatedActiveProfile() {
        val first = profile()
        val second = profile("home").copy(ssid = "Home")
        val config = CloakConfig(activeProfileId = "home", profiles = listOf(first, second), rules = listOf(AppRule("test.app", "office")))
        assertEquals("Studio_5G", config.profileFor("test.app")!!.ssid)
        assertNull(config.profileFor("other.app"))
    }

    @Test fun corruptedOrUntrustedSnapshotDisablesControl() {
        assertFalse(CloakConfig.decode("garbage").enabled)
        val valid = CloakConfig(profiles = listOf(profile())).encode()
        assertFalse(CloakConfig.decode(valid.replace("3c:84:6a:12:7b:90", "ff:ff:ff:ff:ff:ff")).enabled)
        assertFalse(CloakConfig.decode(valid.replace("\"version\":1", "\"version\":999")).enabled)
    }

    @Test fun missingProfilesNeverCreateAnEffectiveRule() {
        val config = CloakConfig(profiles = listOf(profile()), rules = listOf(AppRule("test.app", "deleted")))
        assertTrue(CloakConfig.decode(config.encode()).rules.isEmpty())
    }

    @Test fun oneInvalidProfileDoesNotEraseOtherSavedWifi() {
        val valid = profile()
        val invalid = profile("broken").copy(mac = "02:00:00:00:00:00")
        val config = CloakConfig(activeProfileId = valid.id, profiles = listOf(valid, invalid),
            rules = listOf(AppRule("test.app", valid.id), AppRule("broken.app", invalid.id)))
        val restored = CloakConfig.decode(config.encode())
        assertEquals(listOf(valid), restored.profiles)
        assertEquals(valid, restored.activeProfile)
        assertEquals(listOf(AppRule("test.app", valid.id)), restored.rules)
        assertFalse(restored.enabled)
    }

    @Test fun malformedRuleDoesNotEraseWifiIdentityAndHistory() {
        val valid = profile()
        val json = JSONObject(CloakConfig(activeProfileId = valid.id, profiles = listOf(valid),
            rules = listOf(AppRule("test.app", valid.id))).encode())
        json.getJSONArray("rules").put(JSONObject().put("packageName", "broken.app"))
        val restored = CloakConfig.decode(json.toString())
        assertEquals(listOf(valid), restored.profiles)
        assertEquals(listOf(AppRule("test.app", valid.id)), restored.rules)
        assertFalse(restored.enabled)
    }

    @Test fun missingRulesAndDuplicateProfilesDoNotDiscardSavedWifi() {
        val valid = profile()
        val json = JSONObject(CloakConfig(activeProfileId = valid.id, profiles = listOf(valid, valid)).encode())
        json.remove("rules")
        val restored = CloakConfig.decode(json.toString())
        assertEquals(listOf(valid), restored.profiles)
        assertEquals(valid, restored.activeProfile)
        assertTrue(restored.rules.isEmpty())
        assertFalse(restored.enabled)
    }

    @Test fun ssidLimitIsUtf8BytesRatherThanCharacterCount() {
        assertTrue(ProfileValidator.errors(profile().copy(ssid = "中".repeat(10))).isEmpty())
        assertTrue(ProfileValidator.errors(profile().copy(ssid = "中".repeat(11))).containsKey("ssid"))
        assertTrue(ProfileValidator.errors(profile().copy(ssid = "line\nbreak")).containsKey("ssid"))
    }

    @Test fun redactedMulticastAndMalformedAddressesAreRejected() {
        listOf("02:00:00:00:00:00", "00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "01:ab:cd:ef:12:34", "not-a-mac").forEach {
            assertFalse(it, ProfileValidator.validAddress(it))
        }
        assertTrue(ProfileValidator.validAddress("A2:6C:84:19:2E:70"))
    }
}
