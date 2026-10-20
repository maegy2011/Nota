package com.nota.notes.data

import java.security.SecureRandom

// P3-D2: أسماء ملفات وسائط معتمة — 32 خانة hex عشوائية + الامتداد (مثال: a7f3e9c1….jpg).
// الأسماء القديمة (img_<ts> / file_<ts> / rec_<ts> / draw_<ts> / bg_<ts>) كشفت التوقيت والنوع والعدد.
// الخريطة hash→note موجودة أصلًا ولا تحتاج جدولًا جديدًا:
//   - note_attachments.file → noteId  (المرفقات بأنواعها)
//   - notes.background → id          (الخلفيات)
// الترحيل: NotesRepository.migrateMediaFilenames() عند فتح القفل — idempotent، يُعيد تسمية
// الملف + المفتاح الجانبي (.key) معًا ثم يُحدّث مراجع DB. اليتيم (بلا مرجع) يُعتَّم أيضًا بلا ضرر.
object OpaqueFileNames {
    private val random = SecureRandom()
    private val OPAQUE = Regex("^[0-9a-f]{32}\\.[A-Za-z0-9]{1,5}$")
    private val LEGACY_PREFIXES = listOf("img_", "file_", "rec_", "draw_", "bg_")

    // اسم معتم جديد بامتداد مُعقَّم (أحرف وأرقام فقط، ≤5)
    fun random(ext: String): String {
        val clean = ext.lowercase().filter { it.isLetterOrDigit() }.take(5).ifEmpty { "bin" }
        val b = ByteArray(16).also { random.nextBytes(it) }
        return b.joinToString("") { "%02x".format(it) } + ".$clean"
    }

    fun isOpaque(name: String): Boolean = OPAQUE.matches(name)

    fun isLegacy(name: String): Boolean =
        !isOpaque(name) && !name.endsWith(".key") && LEGACY_PREFIXES.any { name.startsWith(it) }

    fun extOf(name: String): String = name.substringAfterLast('.', "bin").take(5)
}
