package com.nota.notes.data

import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import org.junit.Test

// P3-D3: pure JVM — cryptoShred touches only java.io; migrateShredMethod is a pure companion fun.
class CryptoShredTest {

    private fun tempFile(bytes: ByteArray): java.io.File {
        val dir = Files.createTempDirectory("cryptoshred-test").toFile()
        val f = java.io.File(dir, "secret.bin")
        f.writeBytes(bytes)
        return f
    }

    @Test fun `cryptoShred wipes key first then deletes file`() {
        val order = mutableListOf<String>()
        val f = tempFile(ByteArray(1024) { it.toByte() })
        val keyFile = java.io.File(f.parent, f.name + ".key").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val ok = SecureDelete.cryptoShred(f) { order += "key"; keyFile.delete() }
        order += "file-gone:${!f.exists()}"
        assertThat(ok).isTrue()
        assertThat(order).containsExactly("key", "file-gone:true").inOrder()
        assertThat(f.exists()).isFalse()
    }

    @Test fun `cryptoShred on missing file still wipes key and returns true`() {
        var wiped = false
        val f = java.io.File(System.getProperty("java.io.tmpdir"), "nota-no-such-crypto-xyz.bin")
        f.delete()
        assertThat(SecureDelete.cryptoShred(f) { wiped = true }).isTrue()
        assertThat(wiped).isTrue()
    }

    @Test fun `legacy shred routes cryptoshred method to cryptoShred`() {
        val f = tempFile(ByteArray(64) { 0x41 })
        assertThat(SecureDelete.shred(f, SettingsStore.SHRED_CRYPTOSHRED)).isTrue()
        assertThat(f.exists()).isFalse()
    }

    @Test fun `legacy overwrite methods still work for stored values`() {
        val f = tempFile(ByteArray(64) { 0x42 })
        @Suppress("DEPRECATION")
        assertThat(SecureDelete.shred(f, SettingsStore.SHRED_DOD)).isTrue()
        assertThat(f.exists()).isFalse()
    }

    @Test fun `migrateShredMethod maps every legacy value to cryptoshred`() {
        assertThat(SettingsStore.migrateShredMethod(SettingsStore.SHRED_CRYPTOSHRED))
            .isEqualTo(SettingsStore.SHRED_CRYPTOSHRED)
        assertThat(SettingsStore.migrateShredMethod(SettingsStore.SHRED_SINGLE))
            .isEqualTo(SettingsStore.SHRED_CRYPTOSHRED)
        @Suppress("DEPRECATION")
        assertThat(SettingsStore.migrateShredMethod(SettingsStore.SHRED_DOD))
            .isEqualTo(SettingsStore.SHRED_CRYPTOSHRED)
        @Suppress("DEPRECATION")
        assertThat(SettingsStore.migrateShredMethod(SettingsStore.SHRED_SCHNEIER))
            .isEqualTo(SettingsStore.SHRED_CRYPTOSHRED)
        assertThat(SettingsStore.migrateShredMethod("whatever")).isEqualTo(SettingsStore.SHRED_CRYPTOSHRED)
        assertThat(SettingsStore.SHRED_OPTIONS).containsExactly(SettingsStore.SHRED_CRYPTOSHRED)
    }
}
