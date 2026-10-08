package com.nota.notes.billing

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "ServerRegister"
private const val REGISTER_URL = "https://mohamedadel.cfd/nota-api/v1/users/register"

// التسجيل التلقائي الإيدمبوتنت: POST {accountId} + توكن Firebase الموثق — السيرفر يربط الحساب
// بصاحب التوكن (المرحلة 3). يُستخدم عند الدخول وفي EntitlementsRepo.refresh ليكون النظام شافيًا ذاتيًا.
@Singleton
class ServerRegister @Inject constructor(
    private val tokens: com.nota.notes.auth.IdTokenProvider
) {
    suspend fun ensureRegistered(accountId: String): Boolean = withContext(Dispatchers.IO) {
        var token = tokens.current()
        repeat(2) { attempt ->
            try {
                val url = URL(REGISTER_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
                }
                val body = JSONObject().apply { put("accountId", accountId) }.toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                conn.disconnect()
                if (code == 401 && attempt == 0) {
                    token = tokens.refresh()
                    if (!token.isNullOrBlank()) return@repeat
                }
                if (code in 200..299) return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "ensureRegistered failed: ${e.message}")
            }
        }
        false
    }
}
