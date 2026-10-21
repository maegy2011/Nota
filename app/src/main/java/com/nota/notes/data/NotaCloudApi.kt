package com.nota.notes.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant

private const val TAG = "NotaCloud"
private const val CLOUD_BASE = "https://mohamedadel.cfd/nota-api"

// سحابة نوتا: حصص ورفع/تنزيل عبر السيرفر — نفس أسلوب HttpEntitlementsApi (HttpURLConnection + IO).
// أي خطأ شبكة يرمي — الـ ViewModel يعرض الرسالة.
class CloudHttpException(
    val code: String,
    message: String,
    val quotaBytes: Long = 0L,
    val usedBytes: Long = 0L
) : IOException(message)

data class CloudQuota(val quotaBytes: Long, val usedBytes: Long) {
    val freeBytes: Long = maxOf(0L, quotaBytes - usedBytes)
}

data class CloudUpload(val sha256: String, val sizeBytes: Long, val usedBytes: Long, val quotaBytes: Long, val dedupe: Boolean)

data class CloudPointer(val sha256: String, val updatedAt: Long)

@Singleton
class NotaCloudApi @Inject constructor(
    private val tokens: com.nota.notes.auth.IdTokenProvider
) {
    private fun conn(url: String, method: String, readMs: Int = 10_000, token: String? = null): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = readMs
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }

    private fun readBody(c: HttpURLConnection): String {
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        return (stream?.use { it.readBytes().toString(Charsets.UTF_8) }).orEmpty()
    }

    private fun errOf(code: Int, body: String, op: String): IOException {
        return try {
            val o = JSONObject(body)
            CloudHttpException(o.optString("code", "HTTP_$code"), o.optString("message", op), o.optLong("quotaBytes"), o.optLong("usedBytes"))
        } catch (e: Exception) {
            if (code == 401) CloudHttpException("UNAUTHENTICATED", "auth required", 0L, 0L)
            else IOException("$op HTTP $code")
        }
    }

    // 401 مرة واحدة = تجديد التوكن وإعادة المحاولة — ثم الفشل يُرمى لصاحبه.
    // بلا جلسة Firebase (التجديد null) = الجلسة منتهية: AUTH_EXPIRED صريحة للواجهة بدل التعليق.
    private suspend fun <T> withTokenRetry(op: suspend (String?) -> T): T {
        val t0 = tokens.current()
        try {
            return op(t0)
        } catch (e: CloudHttpException) {
            if (e.code != "UNAUTHENTICATED") throw e
            val t1 = tokens.refresh() ?: throw CloudHttpException("AUTH_EXPIRED", "session expired, sign in again", 0L, 0L)
            return op(t1)
        }
    }

    suspend fun getQuota(accountId: String): CloudQuota = withTokenRetry { token ->
        withContext(Dispatchers.IO) {
            val enc = URLEncoder.encode(accountId, "UTF-8")
            val c = conn("$CLOUD_BASE/v1/cloud/quota?accountId=$enc", "GET", 10_000, token)
        try {
            val body = readBody(c)
            if (c.responseCode !in 200..299) throw errOf(c.responseCode, body, "quota")
            val o = JSONObject(body)
            CloudQuota(o.optLong("quotaBytes"), o.optLong("usedBytes"))
        } finally {
            c.disconnect()
        }
        }
    }

    suspend fun uploadBlob(accountId: String, sha256: String, bytes: ByteArray, mime: String): CloudUpload =
        withTokenRetry { token ->
            withContext(Dispatchers.IO) {
                val enc = URLEncoder.encode(accountId, "UTF-8")
                val mimeEnc = URLEncoder.encode(mime, "UTF-8")
                val c = conn("$CLOUD_BASE/v1/cloud/blobs/$sha256?accountId=$enc&mime=$mimeEnc", "PUT", 60_000, token)
            try {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/octet-stream")
                c.outputStream.use { it.write(bytes) }
                val body = readBody(c)
                if (c.responseCode !in 200..299) throw errOf(c.responseCode, body, "upload")
                val o = JSONObject(body)
                CloudUpload(o.optString("sha256"), o.optLong("sizeBytes"), o.optLong("usedBytes"), o.optLong("quotaBytes"), o.optBoolean("dedupe", false))
            } finally {
                c.disconnect()
            }
            }
        }

    suspend fun downloadBlob(accountId: String, sha256: String): ByteArray = withTokenRetry { token ->
        withContext(Dispatchers.IO) {
            val enc = URLEncoder.encode(accountId, "UTF-8")
            val c = conn("$CLOUD_BASE/v1/cloud/blobs/$sha256?accountId=$enc", "GET", 60_000, token)
        try {
            if (c.responseCode !in 200..299) throw errOf(c.responseCode, readBody(c), "download")
            c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
        }
    }

    suspend fun setPointer(accountId: String, name: String, sha256: String) = withTokenRetry { token ->
        withContext(Dispatchers.IO) {
            val enc = URLEncoder.encode(accountId, "UTF-8")
            val c = conn("$CLOUD_BASE/v1/cloud/pointers/$name?accountId=$enc&sha=$sha256", "PUT", 10_000, token)
        try {
            val body = readBody(c)
            if (c.responseCode !in 200..299) throw errOf(c.responseCode, body, "pointer")
        } finally {
            c.disconnect()
        }
        }
    }

    // Phase 4ج: حذف نسخة سحابية (pointer + blob) — لا يمس .nota المحلي
    // (تُستخدم أيضًا في 6أ لحذف كل النسخ عند حذف الحساب — إن وُجدت آلية سرد لاحقًا)
    suspend fun deletePointer(accountId: String, name: String) = withTokenRetry { token ->
        withContext(Dispatchers.IO) {
            val enc = URLEncoder.encode(accountId, "UTF-8")
            val c = conn("$CLOUD_BASE/v1/cloud/pointers/$name?accountId=$enc", "DELETE", 10_000, token)
            try { val body = readBody(c); if (c.responseCode !in 200..299 && c.responseCode != 404) throw errOf(c.responseCode, body, "delete-pointer") } finally { c.disconnect() }
        }
    }
    suspend fun deleteBlob(accountId: String, sha256: String) = withTokenRetry { token ->
        withContext(Dispatchers.IO) {
            val enc = URLEncoder.encode(accountId, "UTF-8")
            val c = conn("$CLOUD_BASE/v1/cloud/blobs/$sha256?accountId=$enc", "DELETE", 10_000, token)
            try { val body = readBody(c); if (c.responseCode !in 200..299 && c.responseCode != 404) throw errOf(c.responseCode, body, "delete-blob") } finally { c.disconnect() }
        }
    }

    // 6أ: حذف الحساب سحابيًا — يُستدعى أولًا عند التنفيذ النهائي (بعد 14 يومًا)، ثم الحذف المحلي.
    // TODO(phase6-backend): endpoint حقيقي DELETE /v1/account — حاليًا يُرجع false دائمًا (لا endpoint بعد).
    // القاعدة: الفشل هنا لا يوقف الحذف المحلي أبدًا (يُسجَّل ويُستمر).
    suspend fun deleteAccount(accountId: String): Boolean = withTokenRetry { token ->
        withContext(Dispatchers.IO) {
            val enc = URLEncoder.encode(accountId, "UTF-8")
            val c = conn("$CLOUD_BASE/v1/account?accountId=$enc", "DELETE", 10_000, token)
            try {
                val body = readBody(c)
                if (c.responseCode == 501 || c.responseCode == 404) return@withContext false // endpoint غير موجود بعد
                if (c.responseCode !in 200..299) throw errOf(c.responseCode, body, "delete-account")
                true
            } finally {
                c.disconnect()
            }
        }
    }

    // 6ب: رفع مجزأ قابل للاستئناف — PUT /v1/cloud/blobs/{sha}/chunks?offset= بجسم القطعة فقط.
    // TODO(phase6-backend): endpoint حقيقي — حاليًا 404/501 متوقعة فيُترجمت لرمز صريح يفهمه الـ VM.
    // القاعدة: غياب الـ endpoint = رجوع صامت للرفع الكامل، لا فشل للمستخدم أبدًا.
    suspend fun uploadChunk(accountId: String, sha256: String, offset: Long, chunk: ByteArray): Long =
        withTokenRetry { token ->
            withContext(Dispatchers.IO) {
                val enc = URLEncoder.encode(accountId, "UTF-8")
                val c = conn("$CLOUD_BASE/v1/cloud/blobs/$sha256/chunks?accountId=$enc&offset=$offset", "PUT", 60_000, token)
            try {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/octet-stream")
                c.outputStream.use { it.write(chunk) }
                val body = readBody(c)
                if (c.responseCode == 404 || c.responseCode == 501)
                    throw CloudHttpException("NO_CHUNK_ENDPOINT", "chunked upload not supported", 0L, 0L)
                if (c.responseCode !in 200..299) throw errOf(c.responseCode, body, "upload-chunk")
                offset + chunk.size.toLong()
            } finally {
                c.disconnect()
            }
            }
        }

    // null = لا نسخة بعد (404) — ليست خطأ
    suspend fun getPointer(accountId: String, name: String): CloudPointer? = withTokenRetry { token ->
        withContext(Dispatchers.IO) {
            val enc = URLEncoder.encode(accountId, "UTF-8")
            val c = conn("$CLOUD_BASE/v1/cloud/pointers/$name?accountId=$enc", "GET", 10_000, token)
        try {
            if (c.responseCode == 404) return@withContext null
            val body = readBody(c)
            if (c.responseCode !in 200..299) throw errOf(c.responseCode, body, "pointer")
            val o = JSONObject(body)
            val at = runCatching { Instant.parse(o.optString("updatedAt")).toEpochMilli() }.getOrDefault(0L)
            CloudPointer(o.optString("sha256"), at)
        } finally {
            c.disconnect()
        }
        }
    }
}
