package dev.wificloak.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.wificloak.data.CloakConfig
import dev.wificloak.data.InstalledApp
import dev.wificloak.service.FrameworkState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    config: CloakConfig, framework: FrameworkState, apps: List<InstalledApp>, pending: Set<String>, loading: Boolean,
    onToggle: (String, Boolean) -> Unit, onRefresh: () -> Unit,
    onConfigure: () -> Unit, onSettings: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var controlled by rememberSaveable { mutableStateOf(false) }
    var system by rememberSaveable { mutableStateOf(false) }
    val actual = config.rules.map { it.packageName }.toSet().intersect(framework.scope)
    val filtered = remember(apps, search, controlled, system, actual) {
        apps.filter { (!it.system || system || it.packageName in actual) && (!controlled || it.packageName in actual) &&
            (it.label.contains(search.trim(), true) || it.packageName.contains(search.trim(), true)) }
            .sortedByDescending { it.packageName in actual }
    }
    Column {
        TopAppBar(title = { Text("应用", fontWeight = FontWeight.SemiBold) }, actions = {
            IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "刷新应用与作用域") }
        })
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索应用或包名") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Outlined.Close, "清空搜索") } }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !controlled, onClick = { controlled = false }, label = { Text("全部") })
                FilterChip(selected = controlled, onClick = { controlled = true }, label = { Text("受控 ${actual.size}") })
                FilterChip(selected = system, onClick = { system = !system }, label = { Text("系统应用") })
            }
            if (!framework.connected) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LSPosed 未连接", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onSettings) { Text("查看框架") }
            }
            else if (!config.enabled) Text("控制已暂停", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (config.activeProfile == null) TextButton(onClick = onConfigure) { Text("选择 WiFi") }
        }
        if (loading) Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.size(28.dp)); Text("读取应用中", Modifier.padding(top = 16.dp))
        } else if (filtered.isEmpty()) Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.Apps, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (search.isNotBlank()) "没有匹配的应用" else if (controlled) "还没有受控应用" else "没有可显示的应用",
                style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { if (search.isNotEmpty()) search = "" else if (controlled) controlled = false else onRefresh() }) {
                Text(if (search.isNotEmpty()) "清空搜索" else if (controlled) "查看全部应用" else "重新读取")
            }
        } else LazyColumn(Modifier.padding(top = 12.dp)) {
            items(filtered, key = { it.packageName }) { app ->
                val busy = app.packageName in pending
                val profile = config.profileFor(app.packageName)
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Row(Modifier.fillMaxWidth().toggleable(
                        value = app.packageName in actual, role = Role.Checkbox, enabled = !busy,
                        onValueChange = { onToggle(app.packageName, it) }
                    ).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (app.icon != null) Image(app.icon.asImageBitmap(), null, Modifier.size(40.dp))
                        else Icon(Icons.Outlined.Apps, null, Modifier.size(40.dp))
                        Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (busy) CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                        else Checkbox(checked = app.packageName in actual, onCheckedChange = null)
                    }
                    if (profile != null) Row(Modifier.fillMaxWidth().padding(start = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (app.packageName in actual) "已授权" else "未授权", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Text(profile.ssid, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 16.dp, bottom = 12.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
