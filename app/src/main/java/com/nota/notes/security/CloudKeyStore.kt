package com.nota.notes.security

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.cloudStore by preferencesDataStore("nota_cloud")
private val Context.cloudStoreBak by preferencesDataStore("nota_cloud_bak")

// مفتاح المستخدم الخاص للسحابة: 256 بت عشوائية — ملكه وحده.
// يُحفظ على الجهاز مغلفًا بمفتاح Keystore (مرتبط بالجهاز) للاستخدام اليومي،
// وعبارة الاسترداد (نسخته المقروءة) معه هو فقط — بها ينقل مفتاحه لجهاز جديد
// أو يستعيد بعد حذف التطبيق. السيرفر لا يرى المفتاح أبدًا بأي صورة.
// القاعدة الدائمة: PIN الشاشة قفل فقط — لا يدخل في تشفير السحابة إطلاقًا.
@Singleton
class CloudKeyStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val K_IV = stringPreferencesKey("cloud_iv")
    private val K_ENC = stringPreferencesKey("cloud_enc")

    // null = لا مفتاح على هذا الجهاز (جديد/بعد حذف) — يلزم استيراد عبارة الاسترداد
    suspend fun getEntropy(): ByteArray? {
        val p = ctx.cloudStore.data.first()
        val iv = p[K_IV]
        val enc = p[K_ENC]
        if (iv != null && enc != null) {
            return runCatching { aesDecrypt(iv, enc) }.getOrNull()
        }
        val b = ctx.cloudStoreBak.data.first()
        val bIv = b[K_IV]
        val bEnc = b[K_ENC]
        if (bIv != null && bEnc != null) {
            val raw = runCatching { aesDecrypt(bIv, bEnc) }.getOrNull() ?: return null
            ctx.cloudStore.edit { it[K_IV] = bIv; it[K_ENC] = bEnc }
            return raw
        }
        return null
    }

    // توليد أول مفتاح (أول نسخة سحابية) — يُعرض للمستخدم مرة واحدة للحفظ
    suspend fun getOrCreateEntropy(): ByteArray {
        getEntropy()?.let { return it }
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        storeEntropy(raw)
        return raw
    }

    suspend fun storeEntropy(raw: ByteArray) {
        require(raw.size == 32) { "bad entropy" }
        val (iv, enc) = aesEncrypt(raw)
        val ivS = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encS = Base64.encodeToString(enc, Base64.NO_WRAP)
        ctx.cloudStore.edit { it[K_IV] = ivS; it[K_ENC] = encS }
        ctx.cloudStoreBak.edit { it[K_IV] = ivS; it[K_ENC] = encS }
    }

    // عبارة الاسترداد: Base64 مجمعة بمجموعات — تُعرض مرة واحدة وتُحفظ عند المستخدم
    fun exportPhrase(raw: ByteArray): String {
        val b64 = Base64.encodeToString(raw, Base64.NO_WRAP)
        return b64.chunked(4).joinToString("-")
    }

    // تقبل العبارة بأي فواصل/مسافات — null إن كانت تالفة
    fun importPhrase(s: String): ByteArray? {
        return try {
            val clean = s.filter { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
            val raw = Base64.decode(clean, Base64.NO_WRAP)
            if (raw.size == 32) raw else null
        } catch (e: Exception) { null }
    }

    private fun aesEncrypt(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val key = KeyManager.getOrCreateCloudWrapKey()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key)
        return c.iv to c.doFinal(plain)
    }

    private fun aesDecrypt(ivB64: String, encB64: String): ByteArray {
        val key = KeyManager.getOrCreateCloudWrapKey()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(
            Cipher.DECRYPT_MODE, key,
            javax.crypto.spec.GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP))
        )
        return c.doFinal(Base64.decode(encB64, Base64.NO_WRAP))
    }
}
