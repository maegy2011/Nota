package com.nota.notes.data

import org.json.JSONArray
import org.json.JSONObject

// ملاحظة مسطحة للنسخ الاحتياطي — تشمل الأرشيف (archivedAt) فيستعاد معها
// P3-3أ (format v4): كل ملاحظة تحمل id ثابتًا (نص Room ID — يقبل UUID) — مفتاح الدمج بدل الثلاثي الضعيف.
// ملاحظات v2/v3 بلا id تُمنح UUID عشوائيًا عند الفك (توافق خلفي — تُدرج دائمًا، والتكرار موثق في UI).
data class BackupNote(
    val id: String = "",
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val color: Int,
    val archivedAt: Long?,
    val pinned: Boolean = false, // التثبيت يُحفظ مع النسخة — opt للتوافق مع نسخ v2
    val locked: Boolean = false, // القفل الخاص يُحفظ مع النسخة — opt للتوافق
    val background: String? = null, // اسم ملف الخلفية فقط — الملف نفسه لا يُنسخ (خاص بالجهاز)
    val backgroundSha: String? = null, // بصمة خلفية سحابة نوتا (للمشتركين) — كالمرفقات
    val favorite: Boolean = false, // المفضلة تُحفظ مع النسخة — opt للتوافق
    val category: String? = null, // التصنيف يُحفظ مع النسخة — opt للتوافق مع نسخ v2
    val tags: List<String> = emptyList(), // الوسوم تُحفظ مع النسخة — opt للتوافق
    val reminderAt: Long? = null, // موعد التذكير — opt للتوافق
    val reminderRepeat: String = "none", // تكرار التذكير — opt للتوافق
    val reminderBeforeMin: Long = 0L, // الإشعار المسبق — opt للتوافق
    val latitude: Double? = null, // إحداثيات الموقع — opt للتوافق
    val longitude: Double? = null,
    val repeatInterval: Int = 1, // التكرار المخصص — opt للتوافق
    val repeatDays: String = "",
    val repeatMonthMode: String = "same",
    val repeatEnd: String = "forever",
    val repeatEndValue: Long = 0L,
    val tasks: List<BackupTask> = emptyList(), // مهام checklist (نص + منجز) — opt للتوافق
    val attachments: List<BackupAttachment> = emptyList(), // المرفقات (نوع + ملف) — device-local كالخلفية (الغائب يُتجاهل)
    val folder: String? = null, // اسم المجلد الحاوي — opt (v3) — يُعاد إنشاؤه عند الاستعادة
    val deletedAt: Long? = null, // سلة المحذوفات — opt (v3) — تُستعاد السلة سلةً بتاريخ حذفها
    val geoLat: Double? = null, // سياج الموقع — opt (v3) — يُعاد تسجيله عند الاستعادة
    val geoLng: Double? = null,
    val geoRadiusM: Float = 200f,
    val geoName: String = ""
)

// مجلد واحد في النسخة (v3): الاسم + اللون — يُعاد إنشاؤه عند الاستعادة إن غاب
data class BackupFolder(val name: String, val color: Int = 0)

// بطاقة النسخة (v3): تُعرض معاينة قبل الاستعادة — بلا فك كامل
// P3-3أ: version + sha256 اختياري (كشف تلف أقوى) — يُكتب عند التصدير ويُتجاهل عند الفك بلا كسر.
data class BackupMeta(val count: Int, val createdAt: Long, val appVersion: String, val version: Int = 4, val sha256: String? = null)

// مهمة واحدة في النسخة — الترتيب محفوظ بترتيب القائمة (الأول = الأعلى)
data class BackupTask(val text: String, val done: Boolean = false)

// مرفق واحد: النوع + اسم الملف الداخلي — device-local (الغائب يُتجاهل عند الاستعادة)
// sha: بصمة البلوب على سحابة نوتا (للمشتركين) — تُرفع البايتات المشفرة كما هي فالسيرفر لا يقرأ شيئًا
data class BackupAttachment(val kind: String, val file: String, val sha: String? = null)

// ترميز/فك JSON للنسخ — تحقق صارم عند الفك: التالف يُتجاهل سطرًا سطرًا بلا كسر الاستعادة
object BackupCodec {
    const val V = 4 // نسخة الصيغة الحالية — الفك يقبل v2/v3/v4، والترميز يُخرج v4 فقط
    // بصمة SHA-256 hex لنص مصفوفة الملاحظات — تُحسب على نفس السلسلة المُرمَّزة (كشف تلف اختياري).
    fun sha256Hex(s: String): String {
        val d = java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }
    fun encode(notes: List<BackupNote>): String = encode(notes, emptyList(), "")
    fun encode(notes: List<BackupNote>, folders: List<BackupFolder>, appVersion: String = ""): String {
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(JSONObject()
                .put("id", n.id)
                .put("title", n.title)
                .put("body", n.body)
                .put("createdAt", n.createdAt)
                .put("updatedAt", n.updatedAt)
                .put("color", n.color)
                .put("archivedAt", if (n.archivedAt == null) JSONObject.NULL else n.archivedAt)
                .put("pinned", n.pinned)
                .put("locked", n.locked)
                .put("background", if (n.background == null) JSONObject.NULL else n.background)
                .put("backgroundSha", if (n.backgroundSha == null) JSONObject.NULL else n.backgroundSha)
                .put("favorite", n.favorite)
                .put("category", if (n.category == null) JSONObject.NULL else n.category)
                .put("tags", JSONArray(n.tags))
                .put("reminderAt", if (n.reminderAt == null) JSONObject.NULL else n.reminderAt)
                .put("reminderRepeat", n.reminderRepeat)
                .put("reminderBeforeMin", n.reminderBeforeMin)
                .put("latitude", if (n.latitude == null) JSONObject.NULL else n.latitude)
                .put("longitude", if (n.longitude == null) JSONObject.NULL else n.longitude)
                .put("repeatInterval", n.repeatInterval)
                .put("repeatDays", n.repeatDays)
                .put("repeatMonthMode", n.repeatMonthMode)
                .put("repeatEnd", n.repeatEnd)
                .put("repeatEndValue", n.repeatEndValue)
                .put("tasks", JSONArray(n.tasks.map { t -> JSONObject().put("text", t.text).put("done", t.done) }))
                .put("attachments", JSONArray(n.attachments.map { a -> JSONObject().put("kind", a.kind).put("file", a.file).put("sha", if (a.sha == null) JSONObject.NULL else a.sha) }))
                .put("folder", if (n.folder == null) JSONObject.NULL else n.folder)
                .put("deletedAt", if (n.deletedAt == null) JSONObject.NULL else n.deletedAt)
                .put("geoLat", if (n.geoLat == null) JSONObject.NULL else n.geoLat)
                .put("geoLng", if (n.geoLng == null) JSONObject.NULL else n.geoLng)
                .put("geoRadiusM", n.geoRadiusM.toDouble())
                .put("geoName", n.geoName))
        }
        val farr = JSONArray()
        folders.forEach { f -> farr.put(JSONObject().put("name", f.name).put("color", f.color)) }
        val meta = JSONObject().put("count", notes.size).put("createdAt", System.currentTimeMillis()).put("appVersion", appVersion)
            .put("sha256", sha256Hex(arr.toString()))
        return JSONObject().put("app", "nota").put("v", V).put("meta", meta).put("folders", farr).put("notes", arr).toString()
    }

    // بطاقة النسخة للمعاينة قبل الاستعادة — v3/v4 من meta، وv2 من طول المصفوفة (بلا تاريخ)
    // P3-3أ: تُرجع version + sha256 من meta متى وُجدا (v4) — غيابهما (v2/v3) = null/2 بلا كسر.
    fun preview(json: String): BackupMeta {
        val root = JSONObject(json)
        if (root.optString("app") != "nota") throw IllegalArgumentException("not a nota backup")
        val m = root.optJSONObject("meta")
        return if (m != null) BackupMeta(m.optInt("count", 0), m.optLong("createdAt", 0L), m.optString("appVersion", ""),
            root.optInt("v", 3), if (m.isNull("sha256")) null else m.optString("sha256", null))
        else BackupMeta(root.optJSONArray("notes")?.length() ?: 0, 0L, "", 2, null)
    }

    // مجلدات النسخة — تُعاد إنشاؤها عند الاستعادة إن غابت (v3 فقط)
    fun foldersOf(json: String): List<BackupFolder> {
        val out = mutableListOf<BackupFolder>()
        val root = JSONObject(json)
        if (root.optString("app") != "nota") throw IllegalArgumentException("not a nota backup")
        val farr = root.optJSONArray("folders") ?: return out
        for (i in 0 until farr.length()) {
            val o = farr.optJSONObject(i) ?: continue
            val name = o.optString("name", "").trim().take(64)
            if (name.isNotBlank()) out.add(BackupFolder(name, o.optInt("color", 0).coerceIn(0, NotePalettePreviewMax)))
        }
        return out
    }

    fun decode(json: String): List<BackupNote> {
        val out = mutableListOf<BackupNote>()
        val root = JSONObject(json)
        if (root.optString("app") != "nota") throw IllegalArgumentException("not a nota backup")
        val arr = root.getJSONArray("notes")
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                val body = o.optString("body", "")
                val created = o.optLong("createdAt", 0L).takeIf { it > 0 } ?: continue
                // P3-3أ: id ثابت من النسخة (v4) — الغائب (v2/v3) يُمنح UUID عشوائيًا (توافق خلفي).
                val bid = o.optString("id", "").trim().take(64).takeIf { it.isNotBlank() }
                    ?: java.util.UUID.randomUUID().toString()
                out.add(BackupNote(
                    id = bid,
                    title = o.optString("title", ""),
                    body = body,
                    createdAt = created,
                    updatedAt = o.optLong("updatedAt", created),
                    color = o.optInt("color", 0).coerceIn(0, NotePalettePreviewMax),
                    archivedAt = if (o.isNull("archivedAt")) null else o.optLong("archivedAt", 0L).takeIf { it > 0 },
                    pinned = o.optBoolean("pinned", false),
                    locked = o.optBoolean("locked", false),
                    background = if (o.isNull("background")) null else o.optString("background", null),
                    backgroundSha = if (o.isNull("backgroundSha")) null else o.optString("backgroundSha", null),
                    favorite = o.optBoolean("favorite", false),
                    category = if (o.isNull("category")) null else o.optString("category", "").trim().take(64).takeIf { it.isNotBlank() },
                    tags = o.optJSONArray("tags")?.let { arr2 -> List(arr2.length()) { arr2.optString(it) }.filter { it.isNotBlank() } } ?: emptyList(),
                    reminderAt = if (o.isNull("reminderAt")) null else o.optLong("reminderAt", 0L).takeIf { it > 0 },
                    reminderRepeat = o.optString("reminderRepeat", "none").takeIf { it in setOf("none", "daily", "weekly", "monthly", "yearly") } ?: "none",
                    reminderBeforeMin = o.optLong("reminderBeforeMin", 0L).coerceAtLeast(0L),
                    latitude = if (o.isNull("latitude")) null else o.optDouble("latitude"),
                    longitude = if (o.isNull("longitude")) null else o.optDouble("longitude"),
                    repeatInterval = o.optInt("repeatInterval", 1).coerceIn(1, 99),
                    repeatDays = o.optString("repeatDays", ""),
                    repeatMonthMode = o.optString("repeatMonthMode", "same").takeIf { it == "nth" } ?: "same",
                    repeatEnd = o.optString("repeatEnd", "forever").takeIf { it in setOf("forever", "until", "count") } ?: "forever",
                    repeatEndValue = o.optLong("repeatEndValue", 0L).coerceAtLeast(0L),
                    tasks = o.optJSONArray("tasks")?.let { arr2 ->
                        List(arr2.length()) { arr2.optJSONObject(it) }
                            .mapNotNull { jo -> jo?.optString("text", "")?.takeIf { t -> t.isNotBlank() }?.let { t -> BackupTask(t.take(200), jo.optBoolean("done", false)) } }
                    } ?: emptyList(),
                    attachments = o.optJSONArray("attachments")?.let { arr2 ->
                        List(arr2.length()) { arr2.optJSONObject(it) }.mapNotNull { jo ->
                            val f = jo?.optString("file", "")?.takeIf { s -> s.isNotBlank() } ?: return@mapNotNull null
                            val k = jo.optString("kind", "image").takeIf { s -> s in setOf("image", "audio", "drawing") } ?: "image"
                            BackupAttachment(k, f, if (jo.isNull("sha")) null else jo.optString("sha", null)?.takeIf { s -> s.matches(Regex("[0-9a-f]{64}")) })
                        }
                    } ?: emptyList(),
                    folder = if (o.isNull("folder")) null else o.optString("folder", "").trim().take(64).takeIf { it.isNotBlank() },
                    deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt", 0L).takeIf { it > 0 },
                    geoLat = if (o.isNull("geoLat")) null else o.optDouble("geoLat"),
                    geoLng = if (o.isNull("geoLng")) null else o.optDouble("geoLng"),
                    geoRadiusM = o.optDouble("geoRadiusM", 200.0).toFloat().coerceIn(100f, 2000f),
                    geoName = o.optString("geoName", "").take(64)
                ))
            } catch (e: Exception) { /* سطر تالف واحد لا يكسر الاستعادة */ }
        }
        return out
    }

    private const val NotePalettePreviewMax = 7 // أعلى فهرس في لوحة الألوان
}
