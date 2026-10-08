package com.nota.notes.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nota.notes.data.AccountStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.entitlementsCache by preferencesDataStore("entitlements_cache")

// طبقة الاستحقاق: تسأل السيرفر مع كاش DataStore قصير (~24h) للعمل أوفلاين.
// بلا حساب مسجل → FREE دائمًا. أي فشل شبكة → آخر كاش معروف (الأوفلاين أولًا).
@Singleton
class EntitlementsRepo @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: EntitlementsApi,
    private val accounts: AccountStore,
    private val registrar: ServerRegister
) {
    private val K_PRO = booleanPreferencesKey("pro")
    private val K_FEATURES = stringPreferencesKey("features_csv")
    private val K_EXPIRES = longPreferencesKey("expires_at")
    private val K_CACHED_AT = longPreferencesKey("cached_at")
    private val K_ACCOUNT = stringPreferencesKey("account") // نطاق الكاش: كاش حساب لا يُعطى لحساب آخر أبدًا

    companion object {
        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L // ~24 ساعة

        fun isFresh(cachedAt: Long, now: Long = System.currentTimeMillis()): Boolean =
            cachedAt > 0L && (now - cachedAt) < CACHE_TTL_MS
    }

    // الكاش مربوط بالحساب: تبدّل الحساب يُسقط كاش القديم فورًا — فلا يرث مستخدم PRO غيره
    val cached: Flow<Entitlement> = ctx.entitlementsCache.data.combine(accounts.accountId) { store, acct ->
        if (acct == null || store[K_ACCOUNT] != acct) Entitlement.FREE
        else Entitlement(
            pro = store[K_PRO] ?: false,
            features = store[K_FEATURES]?.split(",")?.filter { f -> f.isNotBlank() } ?: emptyList(),
            expiresAt = store[K_EXPIRES]?.takeIf { v -> v > 0L }
        )
    }

    // تحديث من السيرفر: كاش طازج يُعاد فورًا (ما لم يُطلب force)، وإلا سؤال جديد ثم حفظ.
    // شفاء ذاتي: قبل أي سؤال شبكة يُضمَن التسجيل أولًا (idempotent) فيُنشَأ أي حساب مفقود.
    suspend fun refresh(force: Boolean = false): Entitlement = withContext(Dispatchers.IO) {
        val accountId = accounts.currentAccountId() ?: return@withContext Entitlement.FREE
        val store = ctx.entitlementsCache.data.first()
        val cachedAt = store[K_CACHED_AT] ?: 0L
        // الطزاجة لا تُحتسب إلا لنفس الحساب — كاش حساب آخر كأنه غير موجود
        if (!force && store[K_ACCOUNT] == accountId && isFresh(cachedAt)) {
            return@withContext Entitlement(
                pro = store[K_PRO] ?: false,
                features = store[K_FEATURES]?.split(",")?.filter { f -> f.isNotBlank() } ?: emptyList(),
                expiresAt = store[K_EXPIRES]?.takeIf { v -> v > 0L }
            )
        }
        if (registrar.ensureRegistered(accountId)) {
            accounts.setPendingRegister(false)
        }
        val fresh = try {
            api.getEntitlements(accountId)
        } catch (e: AuthExpiredException) {
            // الجلسة منتهية: تُرفع للواجهة لتطلب إعادة الدخول — لا تُدفن في الكاش
            throw e
        } catch (e: Exception) {
            // أوفلاين: آخر كاش معروف بدل الفشل
            return@withContext Entitlement(
                pro = store[K_PRO] ?: false,
                features = store[K_FEATURES]?.split(",")?.filter { f -> f.isNotBlank() } ?: emptyList(),
                expiresAt = store[K_EXPIRES]?.takeIf { v -> v > 0L }
            )
        }
        ctx.entitlementsCache.edit {
            it[K_ACCOUNT] = accountId
            it[K_PRO] = fresh.pro
            it[K_FEATURES] = fresh.features.joinToString(",")
            if (fresh.expiresAt == null) it.remove(K_EXPIRES) else it[K_EXPIRES] = fresh.expiresAt
            it[K_CACHED_AT] = System.currentTimeMillis()
        }
        fresh
    }

    suspend fun hasFeature(name: String): Boolean = withContext(Dispatchers.IO) {
        val current = try {
            refresh()
        } catch (e: Exception) {
            cached.first()
        }
        hasFeature(current.features, name) || (current.pro && name == Entitlement.FEATURE_FOLDERS)
    }
}
