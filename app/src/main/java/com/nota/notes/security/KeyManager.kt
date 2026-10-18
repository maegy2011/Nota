package com.nota.notes.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object KeyManager {
    private const val ALIAS_DB = "nota_db_key"
    private const val ALIAS_BACKUP = "nota_backup_key"
    private const val ALIAS_MEDIA = "nota_media_key"
    private const val ALIAS_CLOUD_WRAP = "nota_cloud_wrap"
    private const val ALIAS_TOKEN = "nota_token_vault"
    private const val ALIAS_DRIVE_PW = "nota_drive_pw"
    private const val ALIAS_BIO = "nota_bio_challenge"

    fun getOrCreateDbKey(): SecretKey = getOrCreate(ALIAS_DB)

    // مفتاح مستقل للنسخ الاحتياطي — كان ثابتًا موجودًا بلا استخدام، والنسخ كانت تُشفر بمفتاح قاعدة البيانات نفسه
    fun getOrCreateBackupKey(): SecretKey = getOrCreate(ALIAS_BACKUP)

    // مفتاح مستقل لملفات الوسائط (خلفيات/صور) — عزل كامل عن مفاتيح القاعدة والنسخ
    fun getOrCreateMediaKey(): SecretKey = getOrCreate(ALIAS_MEDIA)

    // تغليف مفتاح السحابة الخاص بالمستخدم على هذا الجهاز فقط — للراحة اليومية؛
    // النقل بين الأجهزة بعبارة الاسترداد التي مع المستخدم وحده
    fun getOrCreateCloudWrapKey(): SecretKey = getOrCreate(ALIAS_CLOUD_WRAP)

    // خزينة توكن الدخول: تشفير التوكن قبل تخزينه — لا توكن مكشوفًا في أي ملف (مستوى بنكي)
    fun getOrCreateTokenKey(): SecretKey = getOrCreate(ALIAS_TOKEN)

    // كلمة مرور Drive الثابتة: تُحفظ مشفرة بهذا المفتاح — الحماية من قراءة السيرفر لا من الجهاز (27 سبتمبر 2026)
    fun getOrCreateDrivePwKey(): SecretKey = getOrCreate(ALIAS_DRIVE_PW)

    // تحدي البصمة المرتبط حيويًا: مفتاح Keystore يتطلب مصادقة بيومترية عند كل استخدام،
    // ويُبطل تلقائيًا عند إضافة/إزالة بصمة (fail-closed ضد بصمة دخيلة بعد سرقة الجهاز).
    // يُستخدم حصرًا لبناء CryptoObject في مساري الفتح والاسترداد — لا تشفير بيانات به.
    fun getBioChallengeCipher(): javax.crypto.Cipher {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if ((ks.getEntry(ALIAS_BIO, null) as? KeyStore.SecretKeyEntry) == null) {
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(ALIAS_BIO, KeyProperties.PURPOSE_ENCRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
            if (android.os.Build.VERSION.SDK_INT >= 24) spec.setInvalidatedByBiometricEnrollment(true)
            gen.init(spec.build())
            gen.generateKey()
        }
        val key = (ks.getEntry(ALIAS_BIO, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: throw java.security.KeyStoreException("bio key missing")
        return javax.crypto.Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
            init(javax.crypto.Cipher.ENCRYPT_MODE, key) // يرمي KeyPermanentlyInvalidatedException بعد تغيير البصمات
        }
    }

    // بعد فتح ناجح بالـ PIN: إن أُبطل مفتاح التحدي (بصمة جديدة شرعية) يُحذف ليُعاد توليده —
    // المهاجم ببصمة جديدة لا يملك الـ PIN أصلًا فلا يصل لهذه النقطة أبدًا.
    fun refreshBioChallengeIfInvalidated() {
        try {
            getBioChallengeCipher()
        } catch (e: Exception) {
            if (e is android.security.keystore.KeyPermanentlyInvalidatedException) {
                runCatching {
                    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    ks.deleteEntry(ALIAS_BIO)
                }
            }
        }
    }

    private fun getOrCreate(alias: String): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build())
        return gen.generateKey()
    }

}
