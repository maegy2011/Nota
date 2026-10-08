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
