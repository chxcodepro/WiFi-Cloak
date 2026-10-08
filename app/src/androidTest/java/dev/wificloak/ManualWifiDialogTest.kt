package dev.wificloak

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.wificloak.ui.CloakTheme
import dev.wificloak.ui.ManualWifiDialog
import dev.wificloak.ui.ProfilesScreen
import dev.wificloak.data.CloakConfig
import dev.wificloak.service.FrameworkState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualWifiDialogTest {
    @get:Rule val ui = createComposeRule()

    @Test fun invalidInputStaysOpenAndValidInputReachesSaveCallback() {
        var saved: Triple<String, String, String>? = null
        ui.setContent {
            CloakTheme {
                ManualWifiDialog(onDismiss = {}, onSave = { ssid, bssid, mac ->
                    saved = Triple(ssid, bssid, mac)
                })
            }
        }
        ui.onNodeWithText("保存并使用").performClick()
        ui.onNodeWithText("SSID 需为 1–32 字节").assertIsDisplayed()
        assertNull(saved)
        ui.onNodeWithTag("manual-ssid").performTextInput("Manual_Office")
        ui.onNodeWithTag("manual-bssid").performTextInput("3c:84:6a:12:7b:90")
        ui.onNodeWithTag("manual-mac").performTextInput("a2:6c:84:19:2e:70")
        ui.onNodeWithText("保存并使用").performClick()
        ui.runOnIdle { assertEquals(Triple("Manual_Office", "3c:84:6a:12:7b:90", "a2:6c:84:19:2e:70"), saved) }
    }

    @Test fun restoredFormKeepsDraftAndReceivesThePendingSaveResult() {
        val state = mutableStateOf(ManualSaveState())
        val restoration = StateRestorationTester(ui)
        restoration.setContent {
            CloakTheme {
                ProfilesScreen(config = CloakConfig(), framework = FrameworkState(), syncError = null,
                    scan = null, scanning = false, selecting = null, onScan = {}, onUseScanned = {},
                    onUseSaved = {}, onDelete = {}, onSettings = {}, manualSave = state.value,
                    onClearManualError = {}, onSaveManual = { _, _, _ -> state.value = state.value.copy(saving = true) })
            }
        }
        ui.onNodeWithText("手动添加").performScrollTo().performClick()
        ui.onNodeWithTag("manual-ssid").performTextInput("Draft_Office")
        ui.onNodeWithTag("manual-bssid").performTextInput("3c:84:6a:12:7b:90")
        ui.onNodeWithTag("manual-mac").performTextInput("a2:6c:84:19:2e:70")
        ui.onNodeWithText("保存并使用").performClick()
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithTag("manual-ssid").assertTextContains("Draft_Office")
        ui.onNodeWithText("保存中…").assertIsDisplayed()
        ui.runOnIdle { state.value = state.value.copy(saving = false, savedCount = 1) }
        ui.onNodeWithText("手动添加 WiFi").assertDoesNotExist()
        ui.onNodeWithText("暂无备用 WiFi").assertIsDisplayed()
    }
}
