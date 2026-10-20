package com.nota.notes.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// P3-D1: مفاتيح لكل ملف (defense in depth) — مفتاح عشوائي AES-256 لكل ملف وسائط،
// يُغلَّف (wrap) بالمفتاح الرئيسي nota_media_key (يعمل كـ KEK) عبر AES/GCM.
// لا يمس KeyManager/MediaCrypto إطلاقًا: المفتاح الرئيسي يُمرَّر كمعامل (حقن) —
// الإنتاج يمرر KeyManager.getOrCreateMediaKey()، والاختبارات مفاتيح برمجية.
// ملاحظة Keystore حرجة: مفاتيح Keystore غير قابلة للاستخراج (encoded=null) —
// لذا لا نتحقق أبدًا من طول المفتاح الرئيسي عبر encoded (كان سيكسر الجهاز)؛
// Cipher.init يرمي عند المفتاح الفاسد والمتصل يسقط للمسار القديم.
object PerFileKeys {
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private val random = SecureRandom()

    data class Wrapped(val iv: ByteArray, val ct: ByteArray)

    // مفتاح ملف جديد — برمجي قابل للاستخراج (يُغلَّف فورًا ثم يُمسح)
    fun generate(): SecretKey {
        val gen = KeyGenerator.getInstance("AES")
        gen.init(256, random)
        return gen.generateKey()
    }

    // تغليف مفتاح الملف بالمفتاح الرئيسي — يرمي عند الفشل (المتصل يقرر السقوط لـ legacy)
    fun wrap(master: SecretKey, fileKey: SecretKey): Wrapped {
        val raw = fileKey.encoded ?: throw IllegalArgumentException("unextractable file key")
        require(raw.size == 32) { "bad file key" }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, master, GCMParameterSpec(GCM_TAG_BITS, iv))
        return Wrapped(iv, c.doFinal(raw))
    }

    // فك التغليف — null عند أي فشل (مفتاح خاطئ/عبث/تلف) بلا كشف للسبب
    fun unwrap(master: SecretKey, w: Wrapped): SecretKey? = runCatching {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, master, GCMParameterSpec(GCM_TAG_BITS, w.iv))
        SecretKeySpec(c.doFinal(w.ct), "AES")
    }.getOrNull()
}
