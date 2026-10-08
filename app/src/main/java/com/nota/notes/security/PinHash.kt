package com.nota.notes.security

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHash {
    // PBKDF2-HMAC-SHA256 — مكتبة النظام فقط، بلا اعتماديات جديدة
    // 310 ألف دورة = توصية OWASP 2023+ لـ HMAC-SHA256 — القديم (120k) يُقبل عند التحقق ويُرقّى تلقائيًا
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256
    private const val PREFIX = "pbkdf2"

    // الصيغة الجديدة: pbkdf2$<iter>$<saltHex>$<hashHex>
    fun hash(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        try {
            val dk = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return PREFIX + "\$" + iterations + "\$" + salt.toHex() + "\$" + dk.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(pin: String, expected: String): Boolean {
        // صيغة جديدة
        if (expected.startsWith(PREFIX + "\$")) {
            val p = expected.split("\$")
            if (p.size != 4) return false
            val iter = p[1].toIntOrNull() ?: return false
            if (iter <= 0) return false
            val salt = p[2].fromHex() ?: return false
            val want = p[3].fromHex() ?: return false
            if (salt.isEmpty() || want.isEmpty()) return false
            val spec = PBEKeySpec(pin.toCharArray(), salt, iter, want.size * 8)
            try {
                val got = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
                return MessageDigest.isEqual(got, want) // مقارنة ثابتة الزمن — ضد timing attack
            } finally {
                spec.clearPassword()
            }
        }
        // قديم V1: saltHex:hashHex — SHA-256 مرة واحدة (يُقبل للتوافق ثم يُرقّى تلقائيًا)
        val parts = expected.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].fromHex() ?: return false
        val want = parts[1].fromHex() ?: return false
        if (salt.isEmpty() || want.isEmpty()) return false
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray())
        return MessageDigest.isEqual(md.digest(), want)
    }

    fun isLegacy(expected: String): Boolean = !expected.startsWith(PREFIX + "\$")

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.fromHex(): ByteArray? {
        if (isEmpty() || length % 2 != 0) return null
        return try {
            chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
