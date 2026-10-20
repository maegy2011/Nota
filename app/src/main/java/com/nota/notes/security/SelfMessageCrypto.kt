package com.nota.notes.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// P3-D4: تشفير عمودي إضافي لنصوص رسائل النفس — دفاع متعمق فوق تشفير القاعدة الكلي (SQLCipher).
// تصحيح للخطة: جدول self_messages فيه عمود text فقط — لا توجد أعمدة location/geofence — فشُفّر text.
// مفتاح Keystore مستقل nota_self_msg_key — ملف جديد، لا يمس KeyManager إطلاقًا.
// الصيغة: NOTAS1\n<iv b64>\n<ct b64> — الصفوف القديمة العادية تُقبل (legacy) وتُرحَّل بكسل عند أول قراءة.
// سياسة الفشل (موثقة): الكتابة تسقط للنص العادي عند تعذر Keystore بدل كسر الدردشة —
// SQLCipher يبقى الضابط الأساسي، وهذه طبقة عزل إضافية. القراءة لا ترمي أبدًا (تُظهر المخزن كما هو عند الفشل).
object SelfMessageCrypto {
    const val HEADER = "NOTAS1"
    private const val ALIAS = "nota_self_msg_key"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private val random = SecureRandom()
    private val b64e: Base64.Encoder = Base64.getEncoder()
    private val b64d: Base64.Decoder = Base64.getDecoder()

    // مفتاح الجهاز — طبقة رقيقة فوق Keystore (تُستدعى داخل runCatching من المتصل)
    fun deviceKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = javax.crypto.KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return gen.generateKey()
    }

    // ختم نص — يرمي عند الفشل (المتصل يسقط للعادي عبر runCatching)
    fun seal(key: SecretKey, text: String): String {
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return "$HEADER\n${b64e.encodeToString(iv)}\n${b64e.encodeToString(c.doFinal(text.toByteArray(Charsets.UTF_8)))}"
    }

    // فك مختوم — null إلا لرأس صحيح وفك ناجح
    fun open(key: SecretKey, stored: String): String? = runCatching {
        val lines = stored.split("\n")
        if (lines.size != 3 || lines[0] != HEADER) return null
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, b64d.decode(lines[1])))
        String(c.doFinal(b64d.decode(lines[2])), Charsets.UTF_8)
    }.getOrNull()

    fun isSealed(stored: String): Boolean = stored.startsWith("$HEADER\n")

    // فك للعرض: قديم عادي → كما هو، مختوم سليم → مفكوك، مختوم تالف → المخزن كما هو (لا ترمي أبدًا)
    fun decryptOrLegacy(key: SecretKey?, stored: String): String {
        if (!isSealed(stored) || key == null) return stored
        return open(key, stored) ?: stored
    }
}
