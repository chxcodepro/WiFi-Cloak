package dev.wificloak.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.wificloak.data.CloakConfig
import dev.wificloak.data.ScannedWifi
import dev.wificloak.data.WifiProfile
import dev.wificloak.data.WifiScan
import dev.wificloak.service.FrameworkState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    config: CloakConfig, framework: FrameworkState, syncError: String?, scan: WifiScan?, scanning: Boolean, selecting: String?,
    onScan: () -> Unit, onUseScanned: (ScannedWifi) -> Unit, onUseSaved: (WifiProfile) -> Unit,
    onDelete: (WifiProfile) -> Unit, onSettings: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var deleting by remember { mutableStateOf<WifiProfile?>(null) }
    val current = config.activeProfile
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TopAppBar(title = { Text("WiFi", fontWeight = FontWeight.SemiBold) })
        LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                FrameworkChip(framework, config.enabled, syncError, onSettings)
                Spacer(Modifier.weight(1f))
                Text("${config.rules.count { it.packageName in framework.scope }} 个应用", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            if (current != null) item {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Wifi, null, Modifier.size(28.dp))
                            Text(current.ssid, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("当前", style = MaterialTheme.typography.labelLarge)
                        }
                        IdentityLine("BSSID", current.bssid)
                        IdentityLine("MAC", current.mac)
                    }
                }
            }
            item {
                Button(onClick = { tab = 0; onScan() }, enabled = !scanning && selecting == null,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp)) {
                    if (scanning) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.WifiFind, null, Modifier.size(20.dp))
                    Text(if (scanning) "扫描中" else "扫描 WiFi", Modifier.padding(start = 10.dp))
                }
            }
            if (selecting != null) item {
                Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("采集设备 MAC", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                TabRow(selectedTabIndex = tab, modifier = Modifier.padding(horizontal = 24.dp)) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("附近 WiFi") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("备用 WiFi · ${config.profiles.size}") })
                }
            }
            if (tab == 0) {
                if (scan?.fresh == false) item { Text("最近扫描结果", Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (!scanning && scan?.networks.isNullOrEmpty()) item {
                    EmptyWifi(if (scan == null) "尚未扫描 WiFi" else "未发现可用 WiFi", Icons.Outlined.WifiFind)
                }
                items(scan?.networks.orEmpty(), key = { it.key }) { wifi ->
                    WifiChoiceRow(wifi.ssid, wifi.bssid, "${band(wifi.frequency)} · ${wifi.rssi} dBm",
                        selected = current?.ssid == wifi.ssid && current.bssid.equals(wifi.bssid, true), enabled = selecting == null,
                        onSelect = { onUseScanned(wifi) })
                }
            } else {
                if (config.profiles.isEmpty()) item { EmptyWifi("暂无备用 WiFi", Icons.Outlined.History) }
                items(config.profiles.sortedWith(compareByDescending<WifiProfile> { it.id == config.activeProfileId }
                    .thenByDescending { it.lastUsedAt }.thenByDescending { it.createdAt }), key = { it.id }) { profile ->
                    val used = if (profile.lastUsedAt > 0) DateFormat.getDateInstance(DateFormat.SHORT).format(Date(profile.lastUsedAt)) else "尚未使用"
                    WifiChoiceRow(profile.ssid, profile.bssid, "$used · 使用 ${profile.uses} 次",
                        selected = profile.id == config.activeProfileId, enabled = selecting == null,
                        onSelect = { onUseSaved(profile) },
                        trailing = { IconButton(onClick = { deleting = profile }, enabled = selecting == null) {
                            Icon(Icons.Outlined.DeleteOutline, "移除备用 WiFi ${profile.ssid}")
                        } })
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    deleting?.let { profile ->
        val referenced = config.activeProfileId == profile.id || config.rules.any { it.profileId == profile.id }
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text(if (referenced) "WiFi 正在使用" else "移除“${profile.ssid}”？") },
            text = { Text(if (referenced) "先选择其他 WiFi，再移除此备用项。" else "此 WiFi 将从备用列表移除。") },
            confirmButton = { TextButton(onClick = { if (!referenced) onDelete(profile); deleting = null }) { Text(if (referenced) "知道了" else "移除") } },
            dismissButton = { if (!referenced) TextButton(onClick = { deleting = null }) { Text("取消") } })
    }
}

@Composable
fun FrameworkChip(framework: FrameworkState, enabled: Boolean, syncError: String?, onClick: () -> Unit) {
    val label = when {
        !framework.connected -> "框架服务未连接"
        framework.api < 101 -> "框架需升级"
        syncError != null -> "信息待同步"
        !enabled -> "控制已暂停"
        else -> "框架服务已连接"
    }
    AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(Icons.Outlined.Shield, null, Modifier.size(18.dp)) })
}

@Composable
private fun IdentityLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(.24f), style = MaterialTheme.typography.labelMedium)
        Text(value, Modifier.weight(.76f), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun WifiChoiceRow(ssid: String, bssid: String, detail: String, selected: Boolean, enabled: Boolean,
    onSelect: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(ssid, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(bssid, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun EmptyWifi(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun band(frequency: Int): String = when (frequency) {
    in 2400..2500 -> "2.4 GHz"
    in 4900..5924 -> "5 GHz"
    in 5925..7125 -> "6 GHz"
    else -> "$frequency MHz"
}
