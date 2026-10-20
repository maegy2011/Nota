package com.nota.notes.data

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// P2-A: BackupManager envelope tests. Robolectric is required because the
// production code uses android.util.Base64 (no JVM equivalent on unit classpath).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupManagerTest {

    private val sample = """{"notes":[{"id":1,"title":"t","body":"hello"}]}"""
    private val strong = "Correct-Horse-Battery-9"

    @Test fun `encrypt then decrypt roundtrip returns original data`() {
        val env = BackupManager.encryptEnvelope(sample, strong)
        assertThat(BackupManager.decryptEnvelope(env, strong)).isEqualTo(sample)
    }

    // P3-0: كلمة خاطئة → IllegalStateException موحدة (لا AEADBadTagException خام).
    @Test fun `decrypt with wrong password throws IllegalStateException (not AEADBadTagException)`() {
        val env = BackupManager.encryptEnvelope(sample, strong)
        var thrown: Exception? = null
        try {
            BackupManager.decryptEnvelope(env, "Correct-Horse-Battery-8")
        } catch (e: Exception) {
            thrown = e
        }
        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown).isNotInstanceOf(javax.crypto.AEADBadTagException::class.java)
        assertThat(thrown?.cause).isInstanceOf(java.security.GeneralSecurityException::class.java)
    }

    // P3-0: عبث في النص المشفر → IllegalStateException موحدة.
    @Test fun `decrypt with tampered ciphertext throws IllegalStateException`() {
        val env = BackupManager.encryptEnvelope(sample, strong)
        val lines = env.split("\n").toMutableList()
        val enc = lines[5]
        val c = enc[0]
        lines[5] = (if (c != 'A') 'A' else 'B') + enc.substring(1)
        var thrown: Exception? = null
        try {
            BackupManager.decryptEnvelope(lines.joinToString("\n"), strong)
        } catch (e: Exception) {
            thrown = e
        }
        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown).isNotInstanceOf(javax.crypto.AEADBadTagException::class.java)
    }

    // P3-0: نص مقتطع → IllegalStateException موحدة (لا تسريب Base64/IllegalArgumentException).
    @Test fun `decrypt with truncated ciphertext throws IllegalStateException`() {
        val env = BackupManager.encryptEnvelope(sample, strong)
        val lines = env.split("\n").toMutableList()
        lines[5] = lines[5].substring(0, lines[5].length / 2)
        var thrown: Exception? = null
        try {
            BackupManager.decryptEnvelope(lines.joinToString("\n"), strong)
        } catch (e: Exception) {
            thrown = e
        }
        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
    }

    @Test fun `decrypt with tampered ciphertext throws`() {
        val env = BackupManager.encryptEnvelope(sample, strong)
        val lines = env.split("\n").toMutableList()
        val enc = lines[5]
        // Flip one Base64 char to a different valid char (GCM must detect tampering)
        val c = enc[0]
        lines[5] = (if (c != 'A') 'A' else 'B') + enc.substring(1)
        var thrown: Exception? = null
        try {
            BackupManager.decryptEnvelope(lines.joinToString("\n"), strong)
        } catch (e: Exception) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
    }

    @Test fun `encrypt with 7-char password throws`() {
        var thrown: Exception? = null
        try {
            BackupManager.encryptEnvelope(sample, "1234567")
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
    }

    @Test fun `encrypt with common weak passwords throws`() {
        for (pw in listOf("password", "12345678", "aaaaaaaa", "abcdefgh")) {
            var thrown: Exception? = null
            try {
                BackupManager.encryptEnvelope(sample, pw)
            } catch (e: IllegalArgumentException) {
                thrown = e
            }
            assertWithMessage("password <%s> must be rejected", pw).that(thrown).isNotNull()
        }
    }

    @Test fun `encrypt with strong password succeeds`() {
        val env = BackupManager.encryptEnvelope(sample, strong)
        assertThat(env.startsWith("NOTA\nPW\n")).isTrue()
        assertThat(BackupManager.decryptEnvelope(env, strong)).isEqualTo(sample)
    }

    @Test fun `decrypt legacy 4-char backup succeeds`() {
        // A3-legacy option 1 (best): build the envelope with the same algorithm
        // directly (PBKDF2+AES-GCM), bypassing encryptEnvelope's require() —
        // no production helper added, no behavior change. 100k iters = minimum
        // accepted by decryptEnvelope's coerceIn(100_000, 2_000_000).
        val legacyPw = "abcd"
        val iters = 100_000
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(legacyPw.toCharArray(), salt, iters, 256)
        val tmp = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        spec.clearPassword()
        val key = SecretKeySpec(tmp.encoded, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val env = "NOTA\nPW\n$iters\n" +
            Base64.encodeToString(salt, Base64.NO_WRAP) + "\n" +
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + "\n" +
            Base64.encodeToString(cipher.doFinal(sample.toByteArray()), Base64.NO_WRAP)
        // decryptEnvelope deliberately has NO minimum-length check (backward compat)
        assertThat(BackupManager.decryptEnvelope(env, legacyPw)).isEqualTo(sample)
    }

    @Test fun `decrypt rejects bad format`() {
        var thrown: Exception? = null
        try {
            BackupManager.decryptEnvelope("not-an-envelope", strong)
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
    }

    @Test fun `decrypt rejects empty password`() {
        val env = BackupManager.encryptEnvelope(sample, strong)
        var thrown: Exception? = null
        try {
            BackupManager.decryptEnvelope(env, "")
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
    }
}
