package com.nota.notes.data

import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import org.junit.Test

// P2-A: pure JVM — SecureDelete touches only java.io + SecureRandom, no Android APIs.
// Out of scope by design: "data unrecoverable from flash" needs physical analysis.
class SecureDeleteTest {

    private fun tempFile(bytes: ByteArray): java.io.File {
        val dir = Files.createTempDirectory("shred-test").toFile()
        val f = java.io.File(dir, "secret.bin")
        f.writeBytes(bytes)
        return f
    }

    @Test fun `shred SINGLE deletes file`() {
        val f = tempFile(ByteArray(1024) { it.toByte() })
        assertThat(SecureDelete.shred(f, SettingsStore.SHRED_SINGLE)).isTrue()
        assertThat(f.exists()).isFalse()
    }

    @Test fun `shred DOD deletes file`() {
        val f = tempFile(ByteArray(1024) { 0x41 })
        assertThat(SecureDelete.shred(f, SettingsStore.SHRED_DOD)).isTrue()
        assertThat(f.exists()).isFalse()
    }

    @Test fun `shred SCHNEIER deletes file`() {
        val f = tempFile(ByteArray(1024) { 0x42 })
        assertThat(SecureDelete.shred(f, SettingsStore.SHRED_SCHNEIER)).isTrue()
        assertThat(f.exists()).isFalse()
    }

    @Test fun `shred handles nonexistent file gracefully`() {
        // API contract: nonexistent file counts as already gone → returns true, no throw
        val f = java.io.File(System.getProperty("java.io.tmpdir"), "nota-no-such-file-xyz.bin")
        f.delete()
        assertThat(SecureDelete.shred(f)).isTrue()
    }

    @Test fun `shred does not fail on empty file`() {
        val f = tempFile(ByteArray(0))
        assertThat(SecureDelete.shred(f)).isTrue()
        assertThat(f.exists()).isFalse()
    }
}
