package dev.wificloak.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.wificloak.data.WifiSelections

@Composable
fun ManualWifiDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit,
    saving: Boolean = false, saveError: String? = null, onInputChanged: () -> Unit = {}) {
    var ssid by rememberSaveable { mutableStateOf("") }
    var bssid by rememberSaveable { mutableStateOf("") }
    var mac by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val errors = if (submitted) WifiSelections.manualErrors(ssid, bssid, mac) else emptyMap()
    val focus = LocalFocusManager.current

    fun save() {
        if (saving) return
        submitted = true
        if (WifiSelections.manualErrors(ssid, bssid, mac).isNotEmpty()) return
        focus.clearFocus()
        onSave(ssid, bssid, mac)
    }

    AlertDialog(
        modifier = Modifier.safeDrawingPadding().imePadding(),
        properties = DialogProperties(decorFitsSystemWindows = false),
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("手动添加 WiFi") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("保存为备用 WiFi，并设为当前 WiFi。", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = ssid, onValueChange = { ssid = it; onInputChanged() },
                    modifier = Modifier.fillMaxWidth().testTag("manual-ssid"), label = { Text("SSID") },
                    singleLine = true, enabled = !saving, isError = "ssid" in errors,
                    supportingText = { Text(errors["ssid"] ?: "WiFi 名称，最多 32 字节") },
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }))
                OutlinedTextField(value = bssid, onValueChange = { bssid = it; onInputChanged() },
                    modifier = Modifier.fillMaxWidth().testTag("manual-bssid"), label = { Text("BSSID") },
                    placeholder = { Text("3c:84:6a:12:7b:90") }, singleLine = true, enabled = !saving,
                    isError = "bssid" in errors, supportingText = { Text(errors["bssid"] ?: "接入点地址，6 组十六进制数，以冒号分隔") },
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }))
                OutlinedTextField(value = mac, onValueChange = { mac = it; onInputChanged() },
                    modifier = Modifier.fillMaxWidth().testTag("manual-mac"), label = { Text("MAC") },
                    placeholder = { Text("a2:6c:84:19:2e:70") }, singleLine = true, enabled = !saving,
                    isError = "mac" in errors, supportingText = { Text(errors["mac"] ?: "设备地址，6 组十六进制数，以冒号分隔") },
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }))
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = { save() }, enabled = !saving) { Text(if (saving) "保存中…" else "保存并使用") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("取消") } }
    )
}
