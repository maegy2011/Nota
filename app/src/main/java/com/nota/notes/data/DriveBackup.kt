package com.nota.notes.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject

// النسخ السحابي على Google Drive — مجلد التطبيق الخاص (appDataFolder) فقط:
// الملاحظات للنسخ فقط كما طلب المستخدم — لا تُعرض ولا تُحرر في Drive، وملف واحد باسم ثابت يُحدَّث
@Singleton
class DriveBackup @Inject constructor(@ApplicationContext private val ctx: Context) {

    // Phase 2أ: قفل التزامن — نسخة واحدة في كل مرة (tryLock بلا انتظار).
    internal val uploadMutex = Mutex()

    class AlreadyRunning : Exception("already-running")

    companion object {
        const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        // Phase 2ج: الحد الأقصى لحجم نسخة Drive (يُستخدم للتحذير لا للرفض — حوار).
        const val MAX_DRIVE_BYTES: Long = 100L * 1024 * 1024
        private const val FILE_PREFIX = "nota-backup-"
        private const val LEGACY_NAME = "nota-backup.json" // الاسم الثابت القديم — يُقرأ ويُستبدل بالدوران تلقائيًا
        private const val MIME_JSON = "application/json"
        private const val MIME_OCTET = "application/octet-stream"
    }

    data class RemoteVersion(val id: String, val modifiedTime: Long, val name: String)

    sealed interface TokenResult {
        data class Token(val value: String) : TokenResult
        data class Consent(val intent: Intent) : TokenResult // المستخدم لم يمنح نطاق Drive بعد — يُفتح بlauncher ثم تُعاد المحاولة
        data class Failed(val message: String?) : TokenResult
    }

    // رمز الوصول لنطاق Drive — يُطلب عند كل عملية (قصير العمر) — يعمل على IO فقط
    // إصلاح تعطل: account وintent قد يكونان null — الفحص بدل !! الذي كان يسقط التطبيق
    suspend fun token(): TokenResult = withContext(Dispatchers.IO) {
        val acct = GoogleSignIn.getLastSignedInAccount(ctx)
            ?: return@withContext TokenResult.Failed("not-signed-in")
        val gAccount = acct.account ?: return@withContext TokenResult.Failed("no-account")
        try {
            val t = GoogleAuthUtil.getToken(ctx, gAccount, "oauth2:$SCOPE_APPDATA")
            TokenResult.Token(t)
        } catch (e: UserRecoverableAuthException) {
            val ci = e.intent
            if (ci != null) TokenResult.Consent(ci) else TokenResult.Failed("consent-unavailable")
        } catch (e: Exception) {
            TokenResult.Failed(e.message)
        }
    }

    private fun conn(url: String, token: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 20_000
            readTimeout = 60_000
        }

    // إصلاح تعطل: errorStream قد يكون null (رد بلا جسم) — القراءة آمنة بدل NPE
    private fun readAll(c: HttpURLConnection): String {
        val code = try { c.responseCode } catch (e: Exception) { -1 }
        val stream = try {
            if (code in 200..299) c.inputStream else c.errorStream
        } catch (e: Exception) {
            null
        }
        return try {
            stream?.bufferedReader()?.readText().orEmpty()
        } catch (e: Exception) {
            ""
        }.also { runCatching { c.disconnect() } }
    }

    // كل نسخنا في مجلد التطبيق — الأحدث أولًا (المؤرخة + الثابتة القديمة) — للتنزيل والتدوير
    private fun findAll(token: String): List<RemoteVersion> {
        val q = URLEncoder.encode("name contains 'nota-backup' and 'appDataFolder' in parents and trashed = false", "UTF-8")
        val c = conn("https://www.googleapis.com/drive/v3/files?q=$q&spaces=appDataFolder&orderBy=modifiedTime%20desc&fields=files(id,modifiedTime,name)", token, "GET")
        val body = readAll(c)
        if (c.responseCode !in 200..299) throw IllegalStateException("drive-list:${c.responseCode}")
        val arr = JSONObject(body).optJSONArray("files") ?: return emptyList()
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            RemoteVersion(o.getString("id"), runCatching {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(
                    o.optString("modifiedTime", "").take(19))?.time ?: 0L
            }.getOrDefault(0L), o.optString("name", ""))
        }
    }

    // عملية برمز صالح مع إعادة واحدة عند 401 (رمز منتهٍ) — إبطال ثم رمز جديد
    private suspend fun <T> authed(op: (String) -> T): T {
        val t = when (val r = token()) {
            is TokenResult.Token -> r.value
            is TokenResult.Consent -> throw ConsentNeeded(r.intent)
            is TokenResult.Failed -> throw IllegalStateException(r.message ?: "auth-failed")
        }
        try {
            return op(t)
        } catch (e: IllegalStateException) {
            if (!e.message.orEmpty().contains(":401")) throw e
            runCatching { com.google.android.gms.auth.GoogleAuthUtil.invalidateToken(ctx, t) }
            val t2 = when (val r = token()) {
                is TokenResult.Token -> r.value
                is TokenResult.Consent -> throw ConsentNeeded(r.intent)
                is TokenResult.Failed -> throw IllegalStateException(r.message ?: "auth-failed")
            }
            return op(t2)
        }
    }

    // هل توجد أي نسخة سحابية؟ — حارس الرفع الفارغ (27 سبتمبر 2026)
    suspend fun hasBackup(): Boolean = withContext(Dispatchers.IO) {
        authed { t -> findAll(t).isNotEmpty() }
    }

    // الرفع: ملف مؤرخ جديد كل مرة + تدوير حسب اختيار المستخدم (1..7) — الأقدم يُحذف عند التجاوز (27 سبتمبر 2026)
    // Phase 2أ: tryLock — محاولة ثانية أثناء الرفع تُرفض فورًا بـ AlreadyRunning.
    // Phase 2ب: onProgress(pct 0..100) — يُستدعى أثناء كتابة الجسم على دفعات.
    suspend fun upload(json: String, keep: Int = 5, onProgress: ((Int) -> Unit)? = null): Unit = withContext(Dispatchers.IO) {
        if (!uploadMutex.tryLock()) throw AlreadyRunning()
        try {
        authed { t ->
            val boundary = "nota${System.currentTimeMillis()}"
            // المحتوى غلاف مشفر بكلمة المرور الثابتة — الامتداد .nota والنوع ثنائي (27 سبتمبر 2026)
            val name = "$FILE_PREFIX${System.currentTimeMillis()}.nota"
            val meta = JSONObject().put("name", name).put("mimeType", MIME_OCTET)
                .put("parents", org.json.JSONArray().put("appDataFolder"))
            val head = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${meta}\r\n" +
                "--$boundary\r\nContent-Type: $MIME_OCTET\r\n\r\n"
            val tail = "\r\n--$boundary--\r\n"
            val body = head.toByteArray(Charsets.UTF_8) + json.toByteArray(Charsets.UTF_8) + tail.toByteArray(Charsets.UTF_8)
            val c = conn("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id", t, "POST")
            c.doOutput = true
            c.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            // Phase 2ب: كتابة مجزأة لتتبع التقدم — تحديث كل ~10% فقط لتقليل الضجيج.
            c.outputStream.use { out ->
                val chunk = 64 * 1024
                var written = 0
                var lastPct = -1
                while (written < body.size) {
                    val n = minOf(chunk, body.size - written)
                    out.write(body, written, n)
                    written += n
                    val pct = (written * 100 / body.size).coerceIn(0, 100)
                    if (pct / 10 != lastPct / 10 || pct == 100) {
                        lastPct = pct
                        runCatching { onProgress?.invoke(pct) }
                    }
                }
            }
            val resp = readAll(c)
            if (c.responseCode !in 200..299) throw IllegalStateException("drive-upload:${c.responseCode} $resp")
            // التحقق يستخدم معرف الرفع المرجع مباشرة، لا أحدث نتيجة بحث — حتى لا تُقارن أو تُحذف نسخة أخرى عند التزامن أو تأخر الفهرسة.
            val newId = runCatching { JSONObject(resp).optString("id") }.getOrNull().orEmpty()
            if (newId.isBlank()) throw IllegalStateException("drive-upload-no-id")
            // تحقق بعد الرفع (27 سبتمبر 2026): تنزيل فوري للملف نفسه ومقارنة البصمة — المرفوع لا يُعتمد إلا بالتطابق.
            val echo = downloadById(t, newId)
            if (sha256(echo) != sha256(json)) {
                runCatching {
                    val d = conn("https://www.googleapis.com/drive/v3/files/$newId", t, "DELETE")
                    readAll(d)
                }
                throw IllegalStateException("verify-failed")
            }
            // تدوير: حذف ما زاد عن العدد المختار مع إبقاء الملف المرفوع حديثًا دائمًا.
            runCatching {
                val keepCount = keep.coerceIn(1, 7)
                val all = findAll(t)
                // إن لم يظهر الملف الجديد بعد في الفهرس، لا ندير التدوير في هذه الدورة حتى لا نحذف نسخة سليمة خطأ.
                if (all.none { it.id == newId }) return@runCatching
                val keepIds = (listOf(newId) + all.map { it.id }.filter { it != newId }).take(keepCount).toSet()
                all.filter { it.id !in keepIds }.forEach { old ->
                    val d = conn("https://www.googleapis.com/drive/v3/files/${old.id}", t, "DELETE")
                    readAll(d)
                }
            }
        }
        } finally {
            uploadMutex.unlock()
        }
    }

    private fun sha256(s: String): String {
        val d = java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun downloadById(t: String, id: String): String {
        val c = conn("https://www.googleapis.com/drive/v3/files/$id?alt=media", t, "GET")
        val body = readAll(c)
        if (c.responseCode !in 200..299) throw IllegalStateException("drive-download:${c.responseCode}")
        return body
    }

    private fun downloadLatestInner(t: String): Pair<String, Long> {
        val latest = findAll(t).firstOrNull() ?: throw IllegalStateException("no-backup")
        return downloadById(t, latest.id) to latest.modifiedTime
    }

    // التنزيل: أحدث نسخة + تاريخها على السيرفر — يعمل على IO فقط
    suspend fun downloadLatest(): Pair<String, Long> = withContext(Dispatchers.IO) {
        authed { t -> downloadLatestInner(t) }
    }

    // Phase 4ج: حذف نسخة مفردة + حذف الكل — Drive appData فقط، لا يمس .nota المحلي
    suspend fun listVersions(): List<RemoteVersion> = withContext(Dispatchers.IO) { authed { tt -> findAll(tt) } }
    suspend fun deleteById(id: String): Unit = withContext(Dispatchers.IO) {
        authed { tt ->
            val c = conn("https://www.googleapis.com/drive/v3/files/$id", tt, "DELETE")
            readAll(c)
            if (c.responseCode !in 200..299 && c.responseCode != 404) throw IllegalStateException("drive-delete:${c.responseCode}")
        }
    }
    suspend fun deleteAll(): Int = withContext(Dispatchers.IO) {
        authed { tt ->
            val all = findAll(tt)
            var n = 0
            all.forEach { v ->
                runCatching {
                    val c = conn("https://www.googleapis.com/drive/v3/files/${v.id}", tt, "DELETE")
                    readAll(c)
                    if (c.responseCode in 200..299 || c.responseCode == 404) n++
                }
            }
            n
        }
    }

    // التنزيل: محتوى أحدث نسخة — للتوافق مع النداءات القديمة
    suspend fun download(): String = downloadLatest().first

    // إشارة الموافقة المطلوبة — تلتقطها الواجهة فتفتح intent القبول ثم تُعيد المحاولة
    class ConsentNeeded(val intent: Intent) : Exception("consent-needed")
}
