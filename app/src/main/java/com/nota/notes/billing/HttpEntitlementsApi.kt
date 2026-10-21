package com.nota.notes.billing

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "Entitlements"
private const val ENTITLEMENTS_BASE = "https://mohamedadel.cfd/nota-api"

// سؤال الاستحقاق الحي: GET /v1/entitlements?accountId= — نفس أسلوب HttpURLConnection في GoogleAuthManager.
// أي خطأ شبكة يرمي استثناء — الـ Repo يعالج الأوفلاين بالكاش أصلًا.
// TODO(phase6-backend): POST /v1/purchases — يستقبل {accountId, productId, purchaseToken} بإثبات Firebase،
// يتحقق من الإيصال لدى Play، ثم يرقّي الاستحقاق. التطبيق جاهز (reportPurchase) — ينقص endpoint السيرفر.
@Singleton
class HttpEntitlementsApi @Inject constructor(
    private val tokens: com.nota.notes.auth.IdTokenProvider
) : EntitlementsApi {
    override suspend fun getEntitlements(accountId: String): Entitlement = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(accountId, "UTF-8")
        val url = URL("$ENTITLEMENTS_BASE/v1/entitlements?accountId=$encoded")
        // 401 مرة واحدة = تجديد التوكن وإعادة المحاولة — ثم الفشل يُرمى كالمعتاد
        var token = tokens.current()
        repeat(2) { attempt ->
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val code = conn.responseCode
                if (code == 401 && attempt == 0) {
                    token = tokens.refresh()
                    if (!token.isNullOrBlank()) return@repeat
                    // التجديد فشل: لا جلسة Firebase على الجهاز — حالة صريحة للواجهة بدل الصمت
                    throw AuthExpiredException()
                }
                if (code !in 200..299) throw IOException("entitlements HTTP $code")
                val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                return@withContext parse(body)
            } catch (e: Exception) {
                if (e is IOException) throw e
                Log.w(TAG, "entitlements parse failed: ${e.message}")
                throw IOException("entitlements parse failed", e)
            } finally {
                runCatching { conn.disconnect() }
            }
        }
        throw IOException("entitlements HTTP 401")
    }

    private fun parse(body: String): Entitlement {
        val obj = JSONObject(body)
        val pro = obj.optBoolean("pro", false)
        val features = mutableListOf<String>()
        val arr = obj.optJSONArray("features")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val f = arr.optString(i, "")
                if (f.isNotBlank()) features.add(f)
            }
        }
        // السيرفر يرد expiresAt نص ISO أو null (ورقم millis إن تغيّر لاحقًا)
        val raw = obj.opt("expiresAt")
        val expiresAt: Long? = when {
            raw is Number -> raw.toLong().takeIf { v -> v > 0L }
            raw is String && raw.isNotBlank() -> runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
            else -> null
        }
        // 6ج: trialUsed سيرفريًا — true تعني استُهلكت التجربة من قبل (تمنع الالتفاف بإعادة التثبيت).
        val trialUsed = obj.optBoolean("trialUsed", false)
        return Entitlement(pro = pro, features = features, expiresAt = expiresAt, trialUsed = trialUsed)
    }
}
