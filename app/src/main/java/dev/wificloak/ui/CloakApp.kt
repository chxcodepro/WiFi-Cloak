package dev.wificloak.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.wificloak.ManagerViewModel

private val destinations = listOf("WiFi", "应用", "设置")
private val destinationIcons = listOf(Icons.Outlined.Wifi, Icons.Outlined.Apps, Icons.Outlined.Tune)

@Composable
fun CloakApp(manager: ManagerViewModel) {
    val context = LocalContext.current
    val config by manager.config.collectAsStateWithLifecycle()
    val framework by manager.bridge.state.collectAsStateWithLifecycle()
    val apps by manager.apps.collectAsStateWithLifecycle()
    val pending by manager.pending.collectAsStateWithLifecycle()
    val loading by manager.loading.collectAsStateWithLifecycle()
    val syncError by manager.syncError.collectAsStateWithLifecycle()
    val scanned by manager.scanned.collectAsStateWithLifecycle()
    val scanning by manager.scanning.collectAsStateWithLifecycle()
    val selecting by manager.selecting.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var destination by rememberSaveable { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            manager.scanWifi()
        else manager.notify("请授予精确定位权限后重新扫描")
    }
    LaunchedEffect(Unit) { manager.events.collect { snackbar.showSnackbar(it) } }

    fun scan() {
        val required = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        if (required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) manager.scanWifi()
        else launcher.launch(required.toTypedArray())
    }

    fun openFramework() {
        val intent = context.packageManager.getLaunchIntentForPackage("org.lsposed.manager")
        if (intent != null) runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { manager.notify("请从 LSPosed 快捷方式打开管理器") }
        else manager.notify("请从 LSPosed 快捷方式打开管理器")
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints {
            val expanded = maxWidth >= 600.dp
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                if (!expanded) NavigationBar {
                    destinations.forEachIndexed { index, label ->
                        NavigationBarItem(selected = destination == index, onClick = { destination = index },
                            icon = { Icon(destinationIcons[index], label) }, label = { Text(label) })
                    }
                }
            }) { insets ->
                Row(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                    if (expanded) NavigationRail {
                        destinations.forEachIndexed { index, label ->
                            NavigationRailItem(selected = destination == index, onClick = { destination = index },
                                icon = { Icon(destinationIcons[index], label) }, label = { Text(label) })
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        when (destination) {
                            0 -> ProfilesScreen(config, framework, syncError, scanned, scanning, selecting,
                                onScan = ::scan, onUseScanned = manager::useScannedWifi,
                                onUseSaved = { manager.useProfile(it.id) }, onDelete = { manager.deleteProfile(it.id) },
                                onSettings = { destination = 2 })
                            1 -> AppsScreen(config, framework, apps, pending, loading,
                                onToggle = manager::toggleApp, onRefresh = { manager.reloadApps(); manager.refresh() },
                                onConfigure = { destination = 0 }, onSettings = { destination = 2 })
                            2 -> SettingsScreen(config, framework, syncError,
                                onEnabled = { manager.setEnabled(it) }, onSync = { manager.refresh() }, onFramework = ::openFramework)
                        }
                    }
                }
            }
        }
    }
}
