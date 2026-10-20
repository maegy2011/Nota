package com.nota.notes.security

import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import javax.crypto.KeyGenerator
import org.junit.Test

// P3-D1: pure JVM — PerFileKeys/PerFileMedia use only javax.crypto + java.util.Base64 + java.io.
// No android.* APIs, no Keystore: software keys injected as the master stand-in.
// Device-only thin layer (KeyManager.getOrCreateMediaKey as master) is out of scope by design.
class PerFileKeysTest {

    private fun softwareMaster(): javax.crypto.SecretKey {
        val gen = KeyGenerator.getInstance("AES")
        gen.init(256)
        return gen.generateKey()
    }

    @Test fun `generated file keys are unique 256-bit`() {
        val keys = (1..16).map { PerFileKeys.generate().encoded }
        assertThat(keys.distinct().size).isEqualTo(16)
        keys.forEach { assertThat(it.size).isEqualTo(32) }
    }

    @Test fun `wrap then unwrap roundtrips the file key`() {
        val master = softwareMaster()
        val fk = PerFileKeys.generate()
        val w = PerFileKeys.wrap(master, fk)
        val back = PerFileKeys.unwrap(master, w)
        assertThat(back).isNotNull()
        assertThat(back!!.encoded).isEqualTo(fk.encoded)
    }

    @Test fun `unwrap with wrong master fails closed`() {
        val master = softwareMaster()
        val fk = PerFileKeys.generate()
        val w = PerFileKeys.unwrap(softwareMaster(), PerFileKeys.wrap(master, fk))
        assertThat(w).isNull()
    }

    @Test fun `unwrap with tampered ciphertext fails closed`() {
        val master = softwareMaster()
        val fk = PerFileKeys.generate()
        val w = PerFileKeys.wrap(master, fk)
        val tampered = w.ct.copyOf().also { it[0] = (it[0].toInt() xor 0xFF).toByte() }
        assertThat(PerFileKeys.unwrap(master, PerFileKeys.Wrapped(w.iv, tampered))).isNull()
    }

    @Test fun `sealNew then decrypt roundtrips via sidecar key`() {
        val master = softwareMaster()
        val plain = "secret-bytes-نوتا".toByteArray(Charsets.UTF_8)
        val sealed = PerFileMedia.sealNew(master, plain)
        val fk = PerFileMedia.parseKeyFile(master, sealed.keyBytes)
        assertThat(fk).isNotNull()
        assertThat(PerFileMedia.decryptWithKey(fk!!, sealed.fileBytes)).isEqualTo(plain)
    }

    @Test fun `sealed envelopes carry NOTAM2 and NOTAK1 headers`() {
        val sealed = PerFileMedia.sealNew(softwareMaster(), byteArrayOf(1, 2, 3))
        assertThat(PerFileMedia.isPerFile(sealed.fileBytes)).isTrue()
        assertThat(String(sealed.keyBytes, Charsets.UTF_8).startsWith("NOTAK1\n")).isTrue()
        assertThat(PerFileMedia.isPerFile("NOTAM1\nx\ny".toByteArray())).isFalse()
        assertThat(PerFileMedia.isLegacyEnvelope("NOTAM1\nx\ny".toByteArray())).isTrue()
    }

    @Test fun `writeNew and readAll roundtrip through temp files`() {
        val master = softwareMaster()
        val dir = Files.createTempDirectory("p3d1-test").toFile()
        val f = java.io.File(dir, "media.bin")
        val plain = ByteArray(4096) { it.toByte() }
        assertThat(PerFileMedia.writeNew(f, plain, master)).isTrue()
        assertThat(PerFileMedia.keyFileFor(f).exists()).isTrue()
        assertThat(PerFileMedia.readAll(f, master, legacyDecrypt = { null })).isEqualTo(plain)
    }

    @Test fun `readAll falls back to legacy decrypt for old files`() {
        val master = softwareMaster()
        val dir = Files.createTempDirectory("p3d1-legacy").toFile()
        val f = java.io.File(dir, "img_123.jpg")
        f.writeBytes("NOTAM1\nold".toByteArray())
        val legacy = "legacy-plain".toByteArray(Charsets.UTF_8)
        // legacy path decrypts AND lazily migrates to a per-file key
        assertThat(PerFileMedia.readAll(f, master, legacyDecrypt = { legacy })).isEqualTo(legacy)
        assertThat(PerFileMedia.isPerFile(f.readBytes())).isTrue()
        assertThat(PerFileMedia.keyFileFor(f).exists()).isTrue()
        // migrated file now decrypts without the legacy hook
        assertThat(PerFileMedia.readAll(f, master, legacyDecrypt = { null })).isEqualTo(legacy)
    }

    @Test fun `readAll fails closed after key deletion (crypto-shred)`() {
        val master = softwareMaster()
        val dir = Files.createTempDirectory("p3d1-shred").toFile()
        val f = java.io.File(dir, "media.bin")
        val plain = "doomed".toByteArray(Charsets.UTF_8)
        assertThat(PerFileMedia.writeNew(f, plain, master)).isTrue()
        PerFileMedia.cryptoShred(f)
        assertThat(f.exists()).isFalse()
        assertThat(PerFileMedia.keyFileFor(f).exists()).isFalse()
    }

    @Test fun `key sidecars are recognizable by suffix`() {
        assertThat(PerFileMedia.isKeySidecar("abc123.key")).isTrue()
        assertThat(PerFileMedia.isKeySidecar("abc123.jpg")).isFalse()
    }
}
