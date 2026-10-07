package dev.wificloak.data

import android.content.Context
import android.util.AtomicFile
import java.io.File

class ConfigStore(context: Context) {
    // Keep the authoritative copy outside preferences managed by the framework.
    private val snapshot = AtomicFile(File(context.filesDir, "wifi_cloak.json"))
    private val legacyPrefs by lazy { context.getSharedPreferences(CONFIG_GROUP, Context.MODE_PRIVATE) }
    private var needsRecovery = false
    private var unreadable = false

    fun read(): CloakConfig {
        val hasFile = snapshot.baseFile.exists() || File("${snapshot.baseFile.path}.bak").exists()
        val hasLegacy = !hasFile && legacyPrefs.contains(SNAPSHOT_KEY)
        val config = runCatching {
            val raw = if (hasFile) snapshot.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
                else legacyPrefs.getString(SNAPSHOT_KEY, null)
            CloakConfig.decodeSnapshot(raw)
        }.getOrNull()
        if (config != null) {
            if (!hasFile) write(config)
            needsRecovery = false
            unreadable = false
            return config
        }
        needsRecovery = true
        unreadable = hasFile || hasLegacy
        return CloakConfig(enabled = !unreadable)
    }

    // Read the remote copy before publishing anything on the first connection.
    // A deliberately saved empty config is authoritative and must not be restored.
    fun recoverIfNeeded(readRemote: () -> CloakConfig?): CloakConfig? {
        if (!needsRecovery) return null
        val recovered = readRemote()
        check(recovered != null || !unreadable) { "配置读取失败，已保留原始数据，未覆盖框架配置" }
        val config = recovered ?: CloakConfig()
        write(config)
        return config
    }

    fun write(config: CloakConfig) {
        val encoded = config.encode().toByteArray(Charsets.UTF_8)
        val output = snapshot.startWrite()
        try {
            output.write(encoded)
            snapshot.finishWrite(output)
            check(snapshot.openRead().use { it.readBytes() }.contentEquals(encoded)) { "本地配置保存失败" }
        } catch (error: Exception) {
            snapshot.failWrite(output)
            throw IllegalStateException("本地配置保存失败", error)
        }
        needsRecovery = false
        unreadable = false
    }
}
