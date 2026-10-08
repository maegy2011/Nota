package com.nota.notes.security

import android.util.Base64
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

// تشفير ملفات الوسائط (خلفيات/صور الملاحظات) بمفتاح Keystore مستقل — AES-256-GCM.
// الصيغة: NOTAM1 + سطر iv + سطر enc (Base64) — الملفات القديمة غير المشفرة تُقرأ كاحتياطي
// وتُختم عند أول لمس (إعادة تشفير صامتة)، فلا كسر للبيانات القائمة.
object MediaCrypto {
    private const val HEADER = "NOTAM1"
    private const val GCM_TAG_BITS = 128
    private val PREFIX = (HEADER + "\n").toByteArray()

    fun encryptToFile(file: File, plain: ByteArray) {
        val key = KeyManager.getOrCreateMediaKey()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key)
        val body = HEADER + "\n" +
            Base64.encodeToString(c.iv, Base64.NO_WRAP) + "\n" +
            Base64.encodeToString(c.doFinal(plain), Base64.NO_WRAP)
        file.writeText(body)
    }

    // يرجع البايتات الصافية — مشفرة أو قديمة عادية — أو null عند الغياب/التلف
    fun decryptFile(file: File): ByteArray? {
        return try {
            if (!file.exists()) return null
            val raw = file.readBytes()
            if (!startsWith(raw, PREFIX)) return raw // ملف قديم غير مشفر — يُقبل ويُختم لاحقًا
            val lines = String(raw).split("\n")
            if (lines.size != 3 || lines[0] != HEADER) return null
            decrypt(
                Base64.decode(lines[1], Base64.NO_WRAP),
                Base64.decode(lines[2], Base64.NO_WRAP)
            )
        } catch (e: Exception) { null }
    }

    fun isEncrypted(file: File): Boolean {
        return try {
            if (!file.exists()) return false
            val raw = file.readBytes(64)
            startsWith(raw, PREFIX)
        } catch (e: Exception) { false }
    }

    // ختم ملف قديم عادي إلى مشفر — يُستدعى عند أول قراءة ناجحة (ترحيل صامت تدريجي)
    fun sealIfPlain(file: File) {
        try {
            if (!file.exists() || isEncrypted(file)) return
            val raw = file.readBytes()
            if (raw.isEmpty()) return
            encryptToFile(file, raw)
        } catch (e: Exception) { /* أفضل جهد — الملف العادي يبقى مقروءًا */ }
    }

    private fun decrypt(iv: ByteArray, enc: ByteArray): ByteArray {
        val key = KeyManager.getOrCreateMediaKey()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return c.doFinal(enc)
    }

    private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        for (i in prefix.indices) if (data[i] != prefix[i]) return false
        return true
    }

    private fun File.readBytes(limit: Int): ByteArray {
        inputStream().use { ins ->
            val buf = ByteArray(limit)
            var n = 0
            while (n < limit) {
                val r = ins.read(buf, n, limit - n)
                if (r <= 0) break
                n += r
            }
            return buf.copyOf(n)
        }
    }
}
