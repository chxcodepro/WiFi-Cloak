package dev.wificloak.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.wificloak.data.CloakConfig
import dev.wificloak.service.FrameworkState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(config: CloakConfig, framework: FrameworkState, syncError: String?, onEnabled: (Boolean) -> Unit,
    onSync: () -> Unit, onFramework: () -> Unit) {
    Column {
        TopAppBar(title = { Text("设置", fontWeight = FontWeight.SemiBold) })
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("WiFi 信息控制", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Switch(checked = config.enabled, onCheckedChange = onEnabled)
                }
                HorizontalDivider()
                Text("LSPosed", style = MaterialTheme.typography.titleLarge)
                SettingValue("框架服务", if (framework.connected) "已连接" else "未连接")
                SettingValue("模块开关", "请在 LSPosed 查看")
                SettingValue("框架", if (framework.connected) "${framework.name} ${framework.version}" else "—")
                SettingValue("API", if (framework.connected) framework.api.toString() else "—")
                SettingValue("配置同步", when {
                    !framework.connected -> "等待连接"
                    syncError != null -> "同步失败"
                    else -> "已同步"
                })
                SettingValue("作用域", "${framework.scope.size} 个应用")
                if (framework.api in 1..100) Text("需要支持现代 API 101 的框架", color = MaterialTheme.colorScheme.error)
                if (syncError != null && framework.connected) Text(syncError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onFramework, modifier = Modifier.fillMaxWidth()) { Text("打开 LSPosed") }
                OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) { Text("刷新并同步") }
                HorizontalDivider()
                Text("WiFi Cloak", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SettingValue("版本", "1.1.2")
                SettingValue("备用 WiFi", "${config.profiles.size} 份")
            }
        }
    }
}

@Composable
private fun SettingValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(.45f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(.55f))
    }
}
