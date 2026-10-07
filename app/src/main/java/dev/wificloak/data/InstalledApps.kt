package dev.wificloak.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(val packageName: String, val label: String, val system: Boolean, val icon: Bitmap?)

class InstalledApps(private val context: Context) {
    fun load(): List<InstalledApp> {
        val manager = context.packageManager
        return manager.getInstalledApplications(0)
            .filter { it.packageName != context.packageName && it.packageName != "android" }
            .map { info ->
                InstalledApp(
                    info.packageName,
                    runCatching { info.loadLabel(manager).toString() }.getOrDefault(info.packageName),
                    info.flags.and(ApplicationInfo.FLAG_SYSTEM) != 0,
                    runCatching { info.loadIcon(manager).toBitmap(96, 96) }.getOrNull()
                )
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}
