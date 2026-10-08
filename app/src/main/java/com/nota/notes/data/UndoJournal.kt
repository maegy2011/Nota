package com.nota.notes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// سجل تراجع دائم (28 سبتمبر 2026): مكدس التراجع كان في الذاكرة فقط، فبإغلاق التطبيق
// كان "مسح التنسيق" بالخطأ خسارة نهائية. الآن اللقطة تُكتب في ملف لكل ملاحظة
// وتُقرأ عند فتحها — فالإرجاع يعمل بعد إعادة تشغيل التطبيق كمان.
object UndoJournal {
    private const val MAX = 20          // لقطة لكل ملاحظة (الذاكرة كانت 50 — الملف أخف)
    private const val MAX_MD = 120_000  // سقف النص لكل لقطة: يمنع تضخّم ملفPrefs
    private fun file(ctx: Context, noteId: Long) =
        File(File(ctx.filesDir, "undo"), "note_$noteId.json")

    // كتابة الدفعة كاملة عند الخروج من الملاحظة (مرة واحدة) — لا كتابة مع كل ضغطة مفتاح
    fun save(ctx: Context, noteId: Long, stack: List<Pair<String, String>>) {
        if (noteId <= 0L) return
        val keep = stack.takeLast(MAX).filter { it.second.length <= MAX_MD }
        if (keep.isEmpty()) { file(ctx, noteId).delete(); return }
        runCatching {
            val arr = JSONArray()
            keep.forEach { (t, md) ->
                arr.put(JSONObject().put("t", t.take(200)).put("m", md))
            }
            val f = file(ctx, noteId)
            f.parentFile?.mkdirs()
            // كتابة عبر ملف مؤقت ثم تبديل: يمنع ملفًا نصف مكتوب لو مات التطبيق وسط الحفظ
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(arr.toString())
            if (f.exists()) f.delete()
            tmp.renameTo(f)
        }
    }

    // قراءة: بطلبين إذا كان الملف تالفًا — لا نُفشل فتح الملاحظة بسبب سجل
    fun load(ctx: Context, noteId: Long): List<Pair<String, String>> {
        if (noteId <= 0L) return emptyList()
        val f = file(ctx, noteId)
        if (!f.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                o.optString("t") to o.optString("m")
            }.filter { it.second.isNotEmpty() }
        }.getOrDefault(emptyList())
    }

    // تفريغ السجل بعد حفظ نسخة جديدة: السجل القديم بقى بلا معنى ويأخذ مساحة
    fun clear(ctx: Context, noteId: Long) {
        if (noteId > 0L) runCatching { file(ctx, noteId).delete() }
    }
}
