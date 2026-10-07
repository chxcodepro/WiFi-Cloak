package dev.wificloak.data

import android.app.Application
import android.content.Context
import android.util.AtomicFile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, shadows = [PosixAtomicFile::class])
class ConfigStoreTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val file get() = File(context.filesDir, "wifi_cloak.json")
    private val prefs get() = context.getSharedPreferences(CONFIG_GROUP, Context.MODE_PRIVATE)
    private fun savedConfig(): CloakConfig {
        val profile = WifiProfile("office", "工作室", "Studio_5G", "3c:84:6a:12:7b:90", "a2:6c:84:19:2e:70",
            createdAt = 123, lastUsedAt = 456, uses = 3)
        return CloakConfig(activeProfileId = profile.id, profiles = listOf(profile), rules = listOf(AppRule("test.app", profile.id)))
    }

    @Before fun clearStorage() {
        file.delete()
        File("${file.path}.bak").delete()
        File("${file.path}.new").delete()
        prefs.edit().clear().commit()
    }

    @Test fun legacyUpgradeMigratesAllDataAndSurvivesPreferenceRemoval() {
        val original = savedConfig()
        prefs.edit().putString(SNAPSHOT_KEY, original.encode()).commit()
        assertEquals(original, ConfigStore(context).read())
        assertTrue(file.isFile)
        prefs.edit().clear().commit()
        assertEquals(original, ConfigStore(context).read())
    }

    @Test fun privateSnapshotSurvivesStoreRecreationAndIgnoresStalePreferences() {
        val original = savedConfig()
        ConfigStore(context).write(original)
        prefs.edit().putString(SNAPSHOT_KEY, CloakConfig().encode()).commit()
        val reopened = ConfigStore(context)
        assertEquals(original, reopened.read())
        assertNull(reopened.recoverIfNeeded { fail("有效本地配置不应读取远程快照"); null })
    }

    @Test fun interruptedWriteKeepsPreviousSnapshot() {
        val original = savedConfig()
        ConfigStore(context).write(original)
        File("${file.path}.new").writeText("{unfinished")
        assertEquals(original, ConfigStore(context).read())
    }

    @Test fun subsequentSaveReplacesThePreviousSnapshot() {
        val store = ConfigStore(context)
        val original = savedConfig()
        store.write(original)
        val updated = original.copy(enabled = false, profiles = original.profiles.map { it.copy(uses = 4) })
        store.write(updated)
        assertEquals(updated, ConfigStore(context).read())
    }

    @Test fun atomicBackupIsRecoveredBeforeConsultingLegacyPreferences() {
        val original = savedConfig()
        File("${file.path}.bak").writeText(original.encode())
        file.writeText("{unfinished")
        assertEquals(original, ConfigStore(context).read())
    }

    @Test fun missingLocalSnapshotIsRestoredFromFrameworkBeforePublishing() {
        val store = ConfigStore(context)
        assertTrue(store.read().profiles.isEmpty())
        val original = savedConfig()
        assertEquals(original, store.recoverIfNeeded { original })
        assertEquals(original, ConfigStore(context).read())
    }

    @Test fun intentionallyEmptyLocalSnapshotCannotResurrectOldRemoteProfiles() {
        val empty = CloakConfig(enabled = false)
        ConfigStore(context).write(empty)
        val store = ConfigStore(context)
        assertEquals(empty, store.read())
        assertNull(store.recoverIfNeeded { fail("已保存的空配置必须优先"); savedConfig() })
    }

    @Test fun unreadableSnapshotIsPreservedWhenRecoveryFails() {
        file.writeText("{corrupted")
        val store = ConfigStore(context)
        assertFalse(store.read().enabled)
        assertThrows(IllegalStateException::class.java) { store.recoverIfNeeded { null } }
        assertEquals("{corrupted", file.readText())
        val original = savedConfig()
        assertEquals(original, store.recoverIfNeeded { original })
        assertEquals(original, ConfigStore(context).read())
    }

    @Test fun remoteReadFailureDoesNotPersistAnEmptySnapshot() {
        val store = ConfigStore(context)
        store.read()
        assertThrows(IllegalStateException::class.java) { store.recoverIfNeeded { error("框架暂时不可读") } }
        assertFalse(file.exists())
        assertEquals(savedConfig(), store.recoverIfNeeded { savedConfig() })
    }

    @Test fun offlineUserSaveTakesPriorityOverLaterRecovery() {
        val store = ConfigStore(context)
        store.read()
        val original = savedConfig()
        store.write(original)
        assertNull(store.recoverIfNeeded { fail("用户已保存配置，不应恢复远程旧数据"); null })
        assertEquals(original, ConfigStore(context).read())
    }
}

// Android's rename replaces an existing destination. File.renameTo on Windows
// does not, so emulate only that filesystem operation in the Android runtime.
@Implements(AtomicFile::class)
class PosixAtomicFile {
    companion object {
        @Implementation
        @JvmStatic
        fun rename(source: File, target: File) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
