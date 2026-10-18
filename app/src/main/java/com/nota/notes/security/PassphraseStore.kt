package com.nota.notes.security

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.secureStore by preferencesDataStore("nota_secure")
// نسخة احتياطية للمفتاح في ملف مستقل — ضياع ملف واحد لا يُسقط قاعدة البيانات
private val Context.secureStoreBak by preferencesDataStore("nota_secure_bak")

@Singleton
class PassphraseStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val K_IV = stringPreferencesKey("db_iv")
    private val K_ENC = stringPreferencesKey("db_enc")
    private val K_SALT = stringPreferencesKey("pin_salt")
    private val K_PIN = stringPreferencesKey("pin_hash")
    private val K_FAILS = androidx.datastore.preferences.core.intPreferencesKey("pin_fails")
    private val K_LOCK_UNTIL = androidx.datastore.preferences.core.longPreferencesKey("pin_lock_until")
    // تتبع طول PIN (6+ أم لا) + إسقاط بطاقة التوصية — بلا تخزين الطول نفسه
    private val K_PIN_MIN6 = androidx.datastore.preferences.core.booleanPreferencesKey("pin_min6_ok")
    private val K_PIN_CARD_DISMISSED = androidx.datastore.preferences.core.booleanPreferencesKey("pin_card_dismissed")

    // قراءة متسامحة: تلف ملف واحد لا يمنع تجربة النسخة المستقلة الأخرى.
    private suspend fun readSecure(): Preferences? = try {
        ctx.secureStore.data.first()
    } catch (e: IOException) {
        null
    }

    private suspend fun readSecureBak(): Preferences? = try {
        ctx.secureStoreBak.data.first()
    } catch (e: IOException) {
        null
    }

    // هل يوجد مفتاح مخزن (أساسي أو احتياطي)؟ — غيابه مع وجود ملف قاعدة = ملف يتيم لا يُفتح أبدًا
    suspend fun hasPassphrase(): Boolean {
        val p = readSecure()
        if (p?.get(K_IV) != null && p?.get(K_ENC) != null) return true
        val b = readSecureBak()
        return b?.get(K_IV) != null && b?.get(K_ENC) != null
    }

    // سباق التثبيت النظيف: فتحان متزامنان (الواجهة + جدولة التذكيرات عند الدخول) بمخزن فارغ
    // كانا يولدان مفتاحين مختلفين — الملف يُخلق بأحدهما والفتح بالآخر فيسقط — قفل مشترك لكل النسخ
    private companion object {
        private const val GCM_TAG_BITS = 128
        private val genMutex = Mutex()
        private val pinMutex = Mutex()
    }

    suspend fun getOrCreateDbPassphrase(): ByteArray = genMutex.withLock { getOrCreateDbPassphraseLocked() }

    private suspend fun readBackupPassphrase(): ByteArray? {
        // السقوط للنسخة الاحتياطية قبل توليد مفتاح جديد — التوليد الأعمى هو ما يُيتّم القاعدة القديمة
        val bak = readSecureBak()
        val bIv = bak?.get(K_IV)
        val bEnc = bak?.get(K_ENC)
        if (bIv == null || bEnc == null) return null
        return try {
            val raw = aesDecrypt(Base64.decode(bIv, Base64.NO_WRAP), Base64.decode(bEnc, Base64.NO_WRAP))
            // ترميم الأساسي من الاحتياطي — الشفاء الذاتي
            runCatching {
                ctx.secureStore.edit {
                    it[K_IV] = bIv
                    it[K_ENC] = bEnc
                }
            }
            raw
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getOrCreateDbPassphraseLocked(): ByteArray {
        val prefs = readSecure()
        val ivB = prefs?.get(K_IV)
        val encB = prefs?.get(K_ENC)
        if (ivB != null && encB != null) {
            try {
                return aesDecrypt(Base64.decode(ivB, Base64.NO_WRAP), Base64.decode(encB, Base64.NO_WRAP))
            } catch (e: Exception) {
                // المفتاح الأساسي موجود لكنه غير قابل للفك: جرّب الاحتياطي أولًا بدل إسقاط التطبيق فورًا.
                readBackupPassphrase()?.let { return it }
                throw e
            }
        }
        readBackupPassphrase()?.let { return it }
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val (iv, enc) = aesEncrypt(raw)
        val ivS = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encS = Base64.encodeToString(enc, Base64.NO_WRAP)
        ctx.secureStore.edit {
            it[K_IV] = ivS
            it[K_ENC] = encS
        }
        // نفس المفتاح في الملف الاحتياطي — الاستقلال التام بين الملفين
        ctx.secureStoreBak.edit {
            it[K_IV] = ivS
            it[K_ENC] = encS
        }
        return raw
    }

    suspend fun getOrCreatePinSalt(): ByteArray = pinMutex.withLock { getOrCreatePinSaltLocked() }

    private suspend fun getOrCreatePinSaltLocked(): ByteArray {
        val prefs = readSecure()
        // إن تلف الأساسي، لا تولّد ملحًا جديدًا قبل تجربة الاحتياطي — وإلا أبطلت PIN الحالي.
        (prefs?.get(K_SALT) ?: readSecureBak()?.get(K_SALT))?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        ctx.secureStore.edit { it[K_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP) }
        return salt
    }

    // تخزين الصيغة الكاملة (الجديدة pbkdf2$... أو أي صيغة قادمة) — التوافق مع القديم محفوظ عند القراءة
    suspend fun savePinHashFull(v: String) {
        ctx.secureStore.edit { it[K_PIN] = v }
    }

    suspend fun readPinHash(): String? =
        ctx.secureStore.data.map { it[K_PIN] }.first()

    // درع التخمين (A07/M3): محاولات PIN فاشلة متصاعدة — تُحفظ لتُقاوم إعادة تشغيل التطبيق.
    // من الثالثة: قفل 30 ثانية يتضاعف (60، 120…) حتى سقف 10 دقائق. النجاح يُصفّر العداد.
    suspend fun pinLockUntil(): Long =
        ctx.secureStore.data.map { it[K_LOCK_UNTIL] ?: 0L }.first()

    suspend fun notePinFail(now: Long = System.currentTimeMillis()): Long = pinMutex.withLock {
        // القراءة والكتابة داخل نفس القفل حتى لا تضيع محاولة متزامنة وتضعف درع التخمين.
        val fails = (readSecure()?.get(K_FAILS) ?: readSecureBak()?.get(K_FAILS) ?: 0) + 1
        val lockMs = if (fails < 3) 0L
        else (30_000L shl (fails - 3).coerceAtMost(4)).coerceAtMost(600_000L)
        val until = if (lockMs == 0L) 0L else now + lockMs
        ctx.secureStore.edit { it[K_FAILS] = fails; it[K_LOCK_UNTIL] = until }
        until
    }

    suspend fun resetPinFails() = pinMutex.withLock {
        ctx.secureStore.edit { it[K_FAILS] = 0; it[K_LOCK_UNTIL] = 0L }
    }

    // هل الـ PIN الحالي 6 أرقام فأكثر؟ null = مستخدم قديم قبل التتبع (الطول مجهول من الـ hash)
    suspend fun savePinMin6(ok: Boolean) {
        ctx.secureStore.edit { it[K_PIN_MIN6] = ok }
    }

    suspend fun readPinMin6(): Boolean? =
        ctx.secureStore.data.map { it[K_PIN_MIN6] }.first()

    suspend fun pinCardDismissed(): Boolean =
        ctx.secureStore.data.map { it[K_PIN_CARD_DISMISSED] ?: false }.first()

    suspend fun dismissPinCard() {
        ctx.secureStore.edit { it[K_PIN_CARD_DISMISSED] = true }
    }

    // إسقاط تحذير الروت نهائيًا — خيار صريح من المستخدم
    private val K_ROOT_DISMISSED = androidx.datastore.preferences.core.booleanPreferencesKey("root_warn_dismissed")

    suspend fun rootWarnDismissed(): Boolean =
        ctx.secureStore.data.map { it[K_ROOT_DISMISSED] ?: false }.first()

    suspend fun dismissRootWarn() {
        ctx.secureStore.edit { it[K_ROOT_DISMISSED] = true }
    }

    private fun aesEncrypt(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val key = KeyManager.getOrCreateDbKey()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key)
        return c.iv to c.doFinal(plain)
    }

    private fun aesDecrypt(iv: ByteArray, enc: ByteArray): ByteArray {
        val key = KeyManager.getOrCreateDbKey()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(GCM_TAG_BITS, iv))
        return c.doFinal(enc)
    }

}
