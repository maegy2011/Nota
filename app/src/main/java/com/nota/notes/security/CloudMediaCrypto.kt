package com.nota.notes.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// تشفير الوسائط للسحابة بمفتاح المستخدم الخاص وحده (256 بت من CloudKeyStore) — صفر معرفة:
// السيرفر يخزن بايتات مبهمة فقط. المفتاح لا يُنقل ولا يُخزن سحابيًا بأي صورة —
// نسخه على الجهاز مغلفة بـ Keystore للراحة، وعبارة الاسترداد مع المستخدم للنقل.
// PIN الشاشة قفل فقط ولا يدخل هنا إطلاقًا.
// الصيغة: NOTAC2 + سطر iv + سطر مشفر (Base64) — مفتاح مباشر بلا اشتقاق (المفتاح عشوائي أصلًا).
// القاعدة الدائمة: لا بايت مفكوك واحد يغادر الجهاز.
object CloudMediaCrypto {
    private const val HEADER = "NOTAC2"
    private const val GCM_TAG_BITS = 128
    private val PREFIX = (HEADER + "\n").toByteArray()

    fun encryptKey(key: ByteArray, plain: ByteArray): ByteArray {
        require(key.size == 32) { "bad key" }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val body = HEADER + "\n" +
            Base64.encodeToString(c.iv, Base64.NO_WRAP) + "\n" +
            Base64.encodeToString(c.doFinal(plain), Base64.NO_WRAP)
        return body.toByteArray()
    }

    // null = مفتاح غلط أو ملف تالف — لا استثناء يكشف الفرق
    fun decryptKey(key: ByteArray, blob: ByteArray): ByteArray? {
        return try {
            if (key.size != 32 || !startsWith(blob, PREFIX)) return null
            val lines = String(blob).split("\n")
            if (lines.size != 3 || lines[0] != HEADER) return null
            val iv = Base64.decode(lines[1], Base64.NO_WRAP)
            val enc = Base64.decode(lines[2], Base64.NO_WRAP)
            if (iv.isEmpty() || enc.isEmpty()) return null
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            c.doFinal(enc)
        } catch (e: Exception) { null }
    }

    fun wipe(b: ByteArray) {
        java.util.Arrays.fill(b, 0)
    }

    // هل هذه بايتات ظرف مشفر (NOTAC2) أم نص عادي (نسخ قديمة)؟
    fun isEnvelope(blob: ByteArray): Boolean = startsWith(blob, PREFIX)

    private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        for (i in prefix.indices) if (data[i] != prefix[i]) return false
        return true
    }
}
