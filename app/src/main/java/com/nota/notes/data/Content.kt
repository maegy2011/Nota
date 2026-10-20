package com.nota.notes.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

// عنصر مهمة في قائمة مهام ملاحظة — يُرتَّب يدويًا (sort) ويُعلَّم منجزًا (done)
// حذف الملاحظة يمسح مهامها تلقائيًا (CASCADE) — بلا أيتام
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("noteId")]
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val text: String,
    val done: Boolean = false,
    val sort: Int = 0
)

// مرفق ملاحظة: صورة (كاميرا/معرض) — لاحقًا صوت ورسم — الملف داخل مجلد التطبيق فقط
// حذف الملاحظة يمسح السجلات (CASCADE) — والملفات تُمسح معها صراحةً من المستودع
@Entity(
    tableName = "note_attachments",
    foreignKeys = [
        ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("noteId")]
)
data class NoteAttachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val file: String, // اسم الملف داخل مجلد المرفقات — لا مسار مطلق (ينجو من نقل البيانات)
    val kind: String = KIND_IMAGE, // image|audio|drawing — الصوت والرسم للمرحلة التالية
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val KIND_IMAGE = "image"
        const val KIND_AUDIO = "audio"
        const val KIND_DRAWING = "drawing"
        const val KIND_FILE = "file" // ملف عام (PDF/مستند/...) — قائمة + 27 سبتمبر 2026
    }
}

@Dao
interface ContentDao {
    // المهام: مرتبة يدويًا — للعرض التفاعلي في المحرر والعارض
    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY sort ASC, id ASC")
    suspend fun tasksOf(noteId: Long): List<ChecklistItem>

    @Insert
    suspend fun insertTask(item: ChecklistItem): Long

    @Update
    suspend fun updateTask(item: ChecklistItem)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT COUNT(*) FROM checklist_items WHERE noteId = :noteId AND NOT done")
    suspend fun openCount(noteId: Long): Int

    // المرفقات: الأحدث أولًا — للشبكة المصغرة في المحرر
    @Query("SELECT * FROM note_attachments WHERE noteId = :noteId ORDER BY createdAt DESC, id DESC")
    suspend fun attachmentsOf(noteId: Long): List<NoteAttachment>

    @Insert
    suspend fun insertAttachment(a: NoteAttachment): Long

    @Query("DELETE FROM note_attachments WHERE id = :id")
    suspend fun deleteAttachment(id: Long)

    @Query("SELECT file FROM note_attachments WHERE noteId = :noteId")
    suspend fun filesOf(noteId: Long): List<String>

    // P3-D2: ترحيل الأسماء المعتمة — يُحدّث مرجع الملف بعد إعادة تسميته على القرص (يرجع عدد الصفوف)
    @Query("UPDATE note_attachments SET file = :newName WHERE file = :oldName")
    suspend fun renameFile(oldName: String, newName: String): Int
}
