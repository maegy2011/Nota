package com.nota.notes.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.nota.notes.security.MediaCrypto
import com.nota.notes.security.PerFileMedia
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// صور داخل الملاحظة (كاميرا/معرض) — نسخ داخلية خاصة (files/note_media)، لا روابط خارجية.
// نفس قواعد الخلفيات: نسخ وتصغير فوري، بلا إذن تخزين، والحذف يمسح الملف بلا أثر.
object NoteMediaFiles {
    private const val DIR = "note_media"
    private const val MAX_COPY_DIM = 1600
    private const val MAX_LOAD_DIM = 1080

    fun dir(ctx: Context): File = File(ctx.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun file(ctx: Context, name: String): File = File(dir(ctx), name)

    // نسخ صورة (معرض/كاميرا) إلى التخزين الداخلي مصغّرة JPEG **مشفرة** — يرجع اسم الملف أو null
    suspend fun copyImage(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight, MAX_COPY_DIM) }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null
            val orient = readExifRotation(ctx, uri)
            val fixed = if (orient != 0) rotate(bmp, orient) else bmp
            val name = "img_${System.currentTimeMillis()}.jpg"
            val bos = ByteArrayOutputStream()
            fixed.compress(Bitmap.CompressFormat.JPEG, 85, bos)
            if (!bmp.isRecycled) bmp.recycle()
            if (fixed !== bmp && !fixed.isRecycled) fixed.recycle()
            val bytes = bos.toByteArray()
            try {
                // P3-D1: مفتاح خاص لكل ملف — السقوط للمسار القديم عند تعذر Keystore
                if (!PerFileMedia.writeNew(file(ctx, name), bytes)) MediaCrypto.encryptToFile(file(ctx, name), bytes)
            } finally {
                java.util.Arrays.fill(bytes, 0)
            }
            name
        }.getOrNull()
    }

    // تحميل مصغّر للعرض — يفك التشفير أولًا، والقديم العادي يُقبل ويُختم صامتًا
    suspend fun load(ctx: Context, name: String?): Bitmap? {
        if (name.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val f = file(ctx, name)
                if (!f.exists()) return@runCatching null
                val plain = PerFileMedia.readAll(f) ?: return@runCatching null
                PerFileMedia.sealPlain(f) // ترحيل صامت: قديم → مفتاح خاص (أو مشترك عند تعذر Keystore)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(plain, 0, plain.size, bounds)
                if (bounds.outWidth <= 0) return@runCatching null
                val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight, MAX_LOAD_DIM) }
                BitmapFactory.decodeByteArray(plain, 0, plain.size, opts)
            }.getOrNull()
        }
    }

    // نسخة مؤقتة مفكوكة للعارض الخارجي فقط (فعل مشاركة صريح من المستخدم) —
    // الأصل المشفر لا يُكشف أبدًا — المؤقتة تُستبدل مع كل عرض
    suspend fun decryptedTemp(ctx: Context, name: String?): java.io.File? = withContext(Dispatchers.IO) {
        if (name.isNullOrBlank()) return@withContext null
        runCatching {
            val plain = PerFileMedia.readAll(file(ctx, name)) ?: return@runCatching null
            val dir = java.io.File(ctx.cacheDir, "shared_tmp").apply { if (!exists()) mkdirs() }
            val safe = name.replace(Regex("[^A-Za-z0-9._-"), "_")
            val tmp = java.io.File(dir, "view_$safe")
            tmp.writeBytes(plain)
            java.util.Arrays.fill(plain, 0) // مسح البايتات الصافية من الذاكرة فور الكتابة
            tmp
        }.getOrNull()
    }

    // نسخ ملف عام (PDF/مستند/...) إلى التخزين الداخلي **مشفرًا** — حد 25MB مع حفظ الامتداد (قائمة + 27 سبتمبر 2026)
    suspend fun copyFile(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = ctx.contentResolver
            val ext = runCatching {
                resolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)) else null
                }?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() && it.length <= 5 } ?: "bin"
            }.getOrDefault("bin")
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
            if (bytes.size > 25 * 1024 * 1024) return@runCatching null
            val name = "file_${System.currentTimeMillis()}.$ext"
            try {
                if (!PerFileMedia.writeNew(file(ctx, name), bytes)) MediaCrypto.encryptToFile(file(ctx, name), bytes)
            } finally {
                java.util.Arrays.fill(bytes, 0)
            }
            name
        }.getOrNull()
    }

    // حجم الملف المفكوك للعرض — يُحسب مرة واحدة لكل ملف
    suspend fun fileSize(ctx: Context, name: String?): Long? = withContext(Dispatchers.IO) {
        if (name.isNullOrBlank()) return@withContext null
        runCatching { PerFileMedia.readAll(file(ctx, name))?.size?.toLong() }.getOrNull()
    }

    // ختم ملف كاميرا مباشر (كُتب عبر FileProvider قبل التشفير) — يُستدعى فور الالتقاط
    suspend fun sealExisting(ctx: Context, name: String?) = withContext(Dispatchers.IO) {
        if (name.isNullOrBlank()) return@withContext
        runCatching { PerFileMedia.sealPlain(file(ctx, name)) }
    }

    fun delete(ctx: Context, name: String?) {
        if (name.isNullOrBlank()) return
        runCatching { file(ctx, name).delete() }
    }

    // إتلاف آمن لملف وسائط قبل حذفه — عند تفعيل الخيار من الإعدادات
    fun shred(ctx: Context, name: String?, method: String = SettingsStore.SHRED_SINGLE) {
        if (name.isNullOrBlank()) return
        runCatching { SecureDelete.shred(file(ctx, name), method) }
    }

    // ملف صوتي جديد للتسجيل (.m4a) — يُنشأ فارغًا ويُكتب بالمسجل مباشرة
    fun newAudioFile(ctx: Context): String {
        val name = "rec_${System.currentTimeMillis()}.m4a"
        runCatching { file(ctx, name).createNewFile() }
        return name
    }

    // حفظ لوحة رسم PNG **مشفرة** — من البت ماب النهائي بجودة كاملة
    suspend fun saveDrawing(ctx: Context, bmp: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            val name = "draw_${System.currentTimeMillis()}.png"
            val bos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
            val bytes = bos.toByteArray()
            try {
                if (!PerFileMedia.writeNew(file(ctx, name), bytes)) MediaCrypto.encryptToFile(file(ctx, name), bytes)
            } finally {
                java.util.Arrays.fill(bytes, 0)
            }
            name
        }.getOrNull()
    }

    // مدة الصوت للعرض (m:ss) — يفك التشفير لنسخة مؤقتة أولًا — null إن تعذّر
    suspend fun audioDuration(ctx: Context, name: String?): String? = withContext(Dispatchers.IO) {
        if (name.isNullOrBlank()) return@withContext null
        runCatching {
            // المشفر يُفك لنسخة مؤقتة — القارئ يحتاج مسار ملف حقيقي
            val tmp = decryptedTemp(ctx, name) ?: return@runCatching null
            val r = android.media.MediaMetadataRetriever()
            try {
                r.setDataSource(tmp.absolutePath)
                val ms = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: return@runCatching null
                "%d:%02d".format(ms / 60000, (ms % 60000) / 1000)
            } finally {
                runCatching { r.release() }
            }
        }.getOrNull()
    }

    private fun sampleFor(w: Int, h: Int, maxDim: Int): Int {
        var s = 1
        while (w / s > maxDim || h / s > maxDim) s *= 2
        return s
    }

    // تدوير الكاميرا: صور الكاميرا تُخزن مائلة مع EXIF — يُقرأ ويُصحح عند النسخ
    private fun readExifRotation(ctx: Context, uri: Uri): Int = runCatching {
        val ins = ctx.contentResolver.openInputStream(uri) ?: return 0
        ins.use {
            val exif = androidx.exifinterface.media.ExifInterface(it)
            when (exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL)) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
    }.getOrDefault(0)

    private fun rotate(bmp: Bitmap, deg: Int): Bitmap {
        val m = android.graphics.Matrix().apply { postRotate(deg.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
