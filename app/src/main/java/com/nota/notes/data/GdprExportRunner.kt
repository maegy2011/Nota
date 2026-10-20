package com.nota.notes.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// P3-B1 — عدّاء التصدير (يحتاج جهازًا: قراءة DB + فك المرفقات + كتابة cacheDir).
// المنطق النقي في GdprExport (يُختبر على JVM) — هنا التجميع فقط.
// المسار: notes (بما فيها السلة) -> notes.json + attachments/ مفكوكة + settings.json
// (بعد sanitize) + restore_log.json + README.txt -> ZIP في cacheDir -> ShareIntent من الواجهة.
@Singleton
class GdprExportRunner @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repo: NotesRepository,
    private val settings: SettingsStore
) {
    // MediaCrypto كائن مفرد (object) — لا حقن — نفس نمط NoteMediaFiles.
    suspend fun export(onProgress: (Float) -> Unit = {}): File = withContext(Dispatchers.IO) {
        onProgress(0.05f)
        // 1) الملاحظات الحية + السلة (مع deleted flag) — لقطة واحدة عبر first().
        val live = repo.all().first()
        val trashed = repo.trashed().first()
        onProgress(0.25f)
        val noteObjs = (live.map { it to false } + trashed.map { it to true }).map { (n, del) ->
            GdprExport.noteToJson(
                id = n.id, title = n.title, body = n.body,
                createdAt = n.createdAt, updatedAt = n.updatedAt, color = n.color,
                deleted = del, archived = n.archivedAt != null, pinned = n.pinned,
                favorite = n.favorite, folderId = n.folderId, category = n.category,
                tags = runCatching { repo.tagsOf(n.id) }.getOrDefault(emptyList())
            )
        }
        onProgress(0.45f)
        // 2) المرفقات مفكوكة — فك عبر PerFileMedia (مفتاح خاص أو قديم)، والساقط يُقرأ خامًا (ملفات قديمة غير مختومة).
        // P3-D1: ملفات المفاتيح الجانبية (*.key) تُستبعد دائمًا — مادة مفاتيح لا تغادر الجهاز أبدًا.
        // أي ملف يفشل تمامًا يُتخطى ويُوثَّق في README (لا يُسقط التصدير).
        val files = mutableMapOf<String, ByteArray>()
        val skipped = mutableListOf<String>()
        val mediaFiles = runCatching { NoteMediaFiles.dir(ctx).listFiles()?.filter { !com.nota.notes.security.PerFileMedia.isKeySidecar(it.name) } ?: emptyList() }.getOrDefault(emptyList())
        val total = mediaFiles.size.coerceAtLeast(1)
        mediaFiles.forEachIndexed { i, f ->
            val bytes = runCatching { com.nota.notes.security.PerFileMedia.readAll(f) ?: f.readBytes() }.getOrNull()
            if (bytes != null) files["attachments/${f.name}"] = bytes else skipped.add(f.name)
            onProgress(0.45f + 0.3f * (i + 1) / total)
        }
        // 3) الإعدادات بعد sanitize — بلا مفاتيح حساسة.
        val rawSettings = runCatching { settings.dumpAll() }.getOrDefault(emptyMap())
        val clean = GdprExport.sanitizeSettings(rawSettings)
        onProgress(0.8f)
        files["notes.json"] = GdprExport.buildNotesJson(noteObjs).toByteArray(Charsets.UTF_8)
        files["settings.json"] = GdprExport.buildSettingsJson(clean).toByteArray(Charsets.UTF_8)
        val log = runCatching { repo.restoreLogOnce() }.getOrDefault(emptyList())
        val logJson = JSONArray(log.map { r ->
            JSONObject().put("id", r.id).put("at", r.at).put("src", r.src)
                .put("added", r.added).put("skipped", r.skipped).put("undone", r.undone)
        }).toString(2)
        files["restore_log.json"] = logJson.toByteArray(Charsets.UTF_8)
        var readme = GdprExport.buildReadme()
        if (skipped.isNotEmpty()) readme += "\nملفات تُخطيت (فشل فك التشفير): ${skipped.size}\n"
        files["README.txt"] = readme.toByteArray(Charsets.UTF_8)
        onProgress(0.9f)
        // 4) تحذير الحجم — يُحسب قبل الكتابة، والواجهة تعرضه.
        val zip = GdprExport.assembleZip(files)
        val out = File(ctx.cacheDir, "nota-export-${System.currentTimeMillis()}.zip")
        out.writeBytes(zip)
        onProgress(1f)
        out
    }

    fun isOversize(file: File): Boolean = GdprExport.isOversize(file.length())
}
