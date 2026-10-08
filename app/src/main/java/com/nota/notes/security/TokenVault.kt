package com.nota.notes.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

// خزينة توكن الدخول: AES-256-GCM بمفتاح Keystore — التوكن لا يُخزن مكشوفًا أبدًا.
// الصيغة: Base64(iv[12] + cipher) ببادئة V1: — والقيم القديمة المكشوفة تُهاجر ذاتيًا عند القراءة.
object TokenVault {
    private const val PREFIX = "V1:"
    private const val GCM_BITS = 128

    fun seal(plain: String): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, KeyManager.getOrCreateTokenKey(), GCMParameterSpec(GCM_BITS, iv))
        }
        val enc = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(iv + enc, Base64.NO_WRAP)
    }

    // null = تالف (يُتجاهل ويُطلب تجديد) — لا يُرمى أبدًا من مسار القراءة
    fun unseal(stored: String): String? {
        if (!stored.startsWith(PREFIX)) return null
        return try {
            val raw = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            if (raw.size <= 12) return null
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(
                    Cipher.DECRYPT_MODE, KeyManager.getOrCreateTokenKey(),
                    GCMParameterSpec(GCM_BITS, raw, 0, 12)
                )
            }
            c.doFinal(raw, 12, raw.size - 12).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    // JWT صالح شكليًا (ثلاثة مقاطع) — لكشف القيم القديمة المكشوفة وترحيلها
    fun looksLikeJwt(s: String): Boolean =
        s.count { it == '.' } == 2 && s.length in 32..4096
}
