package com.nota.notes.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// P3-B1 — تصدير GDPR: منطق نقي يُختبر على JVM (بلا Context).
// القاعدة: المستخدم يطلب بياناته => تُصدَّر مفكوكة التشفير (المرفقات تُفك قبل التمرير لهذه الطبقة).
// المفاتيح الحساسة لا تدخل settings.json أبدًا — القائمة SENSITIVE_KEYS هي مصدر الحقيقة الوحيد.
object GdprExport {
    // حد التحذير: 500MB — فوقه يُعرض "الحجم كبير — قد يستغرق وقتًا" (قرار موثق في البرومبت).
    const val SIZE_WARN_BYTES: Long = 500L * 1024 * 1024

    // المفاتيح الحساسة المستبعدة صراحةً من settings.json — أي مفتاح جديد حساس يُضاف هنا أولًا.
    val SENSITIVE_KEYS: Set<String> = setOf(
        "ai_key_sealed",      // مفتاح AI مشفر — سر المستخدم
        "drive_pw_enc",       // كلمة مرور Drive مشفرة
        "nota_recovery_key",  // hash مفتاح الاسترداد (أساسي)
        "nota_recovery_key_bak", // hash مفتاح الاسترداد (احتياطي)
        "id_token",           // توكن الجلسة
        "account_id",         // معرف الحساب السحابي
        "drive_pw_next",      // مؤقت داخلي — لا قيمة تصديرية
        "cloud_backup_hash"   // بصمة داخلية — لا قيمة تصديرية
    )

    fun isOversize(totalBytes: Long): Boolean = totalBytes > SIZE_WARN_BYTES

    // ملاحظة واحدة -> JSON مع flag deleted (المحذوفة من السلة تُصدَّر موسومة).
    fun noteToJson(
        id: Long, title: String, body: String, createdAt: Long, updatedAt: Long,
        color: Int, deleted: Boolean, archived: Boolean, pinned: Boolean,
        favorite: Boolean, folderId: Long?, category: String?, tags: List<String> = emptyList()
    ): JSONObject = JSONObject()
        .put("id", id).put("title", title).put("body", body)
        .put("createdAt", createdAt).put("updatedAt", updatedAt)
        .put("color", color).put("deleted", deleted).put("archived", archived)
        .put("pinned", pinned).put("favorite", favorite)
        .put("folderId", folderId ?: JSONObject.NULL)
        .put("category", category ?: JSONObject.NULL)
        .put("tags", JSONArray(tags))

    fun buildNotesJson(items: List<JSONObject>): String = JSONArray(items).toString(2)

    // إسقاط المفاتيح الحساسة — يُرجع نسخة نظيفة فقط.
    fun sanitizeSettings(all: Map<String, String>): Map<String, String> =
        all.filterKeys { it !in SENSITIVE_KEYS }

    fun buildSettingsJson(sanitized: Map<String, String>): String =
        JSONObject(sanitized).toString(2)

    fun buildReadme(): String = """
        |Nota — تصدير بياناتك / Your data export
        |========================================
        |notes.json — كل ملاحظاتك (بما فيها المحذوفة من السلة، موسومة deleted:true).
        |  All your notes (including trashed ones, flagged deleted:true).
        |attachments/ — مرفقاتك مفكوكة التشفير — افتحها مباشرة.
        |  Your attachments, decrypted — open directly.
        |settings.json — إعداداتك (بلا مفاتيح حساسة: لا مفاتيح AI ولا سحابة ولا hashes).
        |  Your settings (no sensitive keys: no AI/cloud keys, no hashes).
        |restore_log.json — سجل عمليات الاستعادة (آخر 20).
        |  Restore operations log (last 20).
        |ملاحظة: هذا الأرشيف بياناتك الخاصة — احفظه في مكان آمن.
        |Note: this archive is your private data — store it somewhere safe.
    """.trimMargin()

    // تجميع ZIP من خريطة اسم -> بايتات (حتمي الترتيب الأبجدي لثبات الاختبارات).
    fun assembleZip(files: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.toSortedMap().forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    // أسماء المدخلات — للتحقق في الاختبارات (ZIP قابل للفتح ويحتوي المتوقع).
    fun unzipNames(zip: ByteArray): List<String> {
        val names = mutableListOf<String>()
        ZipInputStream(zip.inputStream()).use { zin ->
            var e = zin.nextEntry
            while (e != null) {
                names.add(e.name)
                zin.closeEntry()
                e = zin.nextEntry
            }
        }
        return names.sorted()
    }

    fun unzipBytes(zip: ByteArray, name: String): ByteArray? {
        ZipInputStream(zip.inputStream()).use { zin ->
            var e = zin.nextEntry
            while (e != null) {
                if (e.name == name) return zin.readBytes()
                zin.closeEntry()
                e = zin.nextEntry
            }
        }
        return null
    }
}
