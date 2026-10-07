package dev.wificloak

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.wificloak.data.AppRule
import dev.wificloak.data.CloakConfig
import dev.wificloak.data.ConfigStore
import dev.wificloak.data.ScannedWifi
import dev.wificloak.data.WifiSelections
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManagerFlowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun selectingBackupPersistsIdentityAndUpdatesControlledApps() {
        val store = ConfigStore(ui.activity)
        val original = store.read()
        try {
            val first = WifiSelections.saveAndUse(CloakConfig(), ScannedWifi("Studio_5G", "3c:84:6a:12:7b:90", 5180, -48), "a2:6c:84:19:2e:70")
            val second = WifiSelections.saveAndUse(first, ScannedWifi("Backup_Garden", "3c:84:6a:12:7b:92", 2412, -55), "b2:6c:84:19:2e:70")
            val seeded = first.copy(profiles = second.profiles, rules = listOf(AppRule("test.app", first.activeProfileId!!)))
            store.write(seeded)
            ui.activityRule.scenario.recreate()
            ui.onNodeWithText("备用 WiFi · 2").performScrollTo().performClick()
            ui.onNodeWithText("Backup_Garden").performScrollTo().performClick()
            ui.waitUntil(10_000) { store.read().activeProfile?.ssid == "Backup_Garden" }
            val persisted = store.read()
            assertEquals("b2:6c:84:19:2e:70", persisted.activeProfile!!.mac)
            assertTrue(persisted.activeProfile!!.offline)
            assertEquals(persisted.activeProfileId, persisted.profileFor("test.app")!!.id)
        } finally { store.write(original) }
    }
}
