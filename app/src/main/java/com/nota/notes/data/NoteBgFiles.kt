package com.nota.notes.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.nota.notes.security.MediaCrypto
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// صور خلفيات الملاحظات — نسخ داخلية خاصة (files/note_bg)، لا روابط خارجية قابلة للسحب.
// الخصوصية: الصورة المختارة تُنسخ وتصغّر فورًا، والأصل الخارجي لا يُحفظ ولا يُطلب إذن تخزين.
object NoteBgFiles {
    private const val DIR = "note_bg"
    private const val MAX_COPY_DIM = 1600 // أطول ضلع عند النسخ — توازن جودة/حجم
    private const val MAX_LOAD_DIM = 1080 // أطول ضلع عند العرض

    fun dir(ctx: Context): File = File(ctx.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun file(ctx: Context, name: String): File = File(dir(ctx), name)

    // نسخ صورة مختارة إلى التخزين الداخلي مصغّرة JPEG **مشفرة** — يرجع اسم الملف أو null
    suspend fun copyPicked(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return@runCatching null
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(w, h, MAX_COPY_DIM) }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null
            val name = "bg_${System.currentTimeMillis()}.jpg"
            val bos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, bos)
            if (!bmp.isRecycled) bmp.recycle()
            MediaCrypto.encryptToFile(file(ctx, name), bos.toByteArray())
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
                val plain = MediaCrypto.decryptFile(f) ?: return@runCatching null
                MediaCrypto.sealIfPlain(f) // ترحيل صامت: قديم عادي → مشفر
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(plain, 0, plain.size, bounds)
                if (bounds.outWidth <= 0) return@runCatching null
                val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight, MAX_LOAD_DIM) }
                BitmapFactory.decodeByteArray(plain, 0, plain.size, opts)
            }.getOrNull()
        }
    }

    // حذف ملف خلفية — عند إزالتها أو حذف ملاحظتها (بلا أثر كالحذف النهائي للنص)
    fun delete(ctx: Context, name: String?) {
        if (name.isNullOrBlank()) return
        runCatching { file(ctx, name).delete() }
    }

    // إتلاف آمن لملف خلفية قبل حذفه — عند تفعيل الخيار من الإعدادات
    fun shred(ctx: Context, name: String?, method: String = SettingsStore.SHRED_SINGLE) {
        if (name.isNullOrBlank()) return
        runCatching { SecureDelete.shred(file(ctx, name), method) }
    }

    // تكرار ملف خلفية لملاحظة منسوخة — كل نسخة تملك ملفها **المشفر بمفتاح جديد (iv جديد)**
    suspend fun duplicate(ctx: Context, name: String?): String? {
        if (name.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val src = file(ctx, name)
                if (!src.exists()) return@runCatching null
                val plain = MediaCrypto.decryptFile(src) ?: return@runCatching null
                val dst = "bg_${System.currentTimeMillis()}_copy.jpg"
                MediaCrypto.encryptToFile(file(ctx, dst), plain)
                dst
            }.getOrNull()
        }
    }

    private fun sampleFor(w: Int, h: Int, maxDim: Int): Int {
        var s = 1
        val longest = maxOf(w, h)
        while (longest / (s * 2) >= maxDim) s *= 2
        return s
    }
}
