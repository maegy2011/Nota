package com.nota.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    // كل الوسوم مرتبة أبجديًا — لصف الفلترة واقتراحات المحرر
    // أسماء الوسوم للفلترة — المرتبطة بملاحظات الحساب الحالي فقط (أسماء حساب آخر لا تظهر)
    @Query("SELECT DISTINCT t.* FROM tags t JOIN note_tags nt ON nt.tagId = t.id JOIN notes n ON n.id = nt.noteId WHERE n.deletedAt IS NULL AND (n.ownerAccountId IS NULL OR n.ownerAccountId = :acct) ORDER BY t.name COLLATE NOCASE ASC")
    fun allTags(acct: String?): Flow<List<Tag>>

    // كل الروابط مسطّحة — تُجمَّع في الذاكرة لخريطة noteId ← أسماء الوسوم — بنطاق الحساب (وسوم حساب آخر لا تتسرب)
    @Query("SELECT nt.noteId AS noteId, t.name AS name FROM note_tags nt JOIN tags t ON t.id = nt.tagId JOIN notes n ON n.id = nt.noteId WHERE n.deletedAt IS NULL AND (n.ownerAccountId IS NULL OR n.ownerAccountId = :acct)")
    fun allMappings(acct: String?): Flow<List<NoteTagName>>

    // عدّادات الوسوم لشاشة الإدارة — وسم + عدد ملاحظاته الحيّة — بنطاق الحساب
    @Query("SELECT t.name AS name, COUNT(n.id) AS count FROM tags t LEFT JOIN note_tags nt ON nt.tagId = t.id LEFT JOIN notes n ON n.id = nt.noteId AND n.deletedAt IS NULL AND (n.ownerAccountId IS NULL OR n.ownerAccountId = :acct) GROUP BY t.id ORDER BY t.name COLLATE NOCASE ASC")
    fun tagCounts(acct: String?): Flow<List<TagCount>>

    @Query("UPDATE note_tags SET tagId = :newId WHERE tagId = :oldId")
    suspend fun moveLinks(oldId: Long, newId: Long)

    // إعادة تسمية وسم من الإدارة: تطبيع + دمج في الموجود بلا حساسية حالة (روابط القديم تنتقل للجديد)
    @Transaction
    suspend fun renameTag(old: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty() || clean.equals(old, ignoreCase = true)) return
        val oldId = idOf(old) ?: return
        insertTags(listOf(Tag(name = clean)))
        val newId = idOf(clean) ?: return
        if (newId != oldId) {
            moveLinks(oldId, newId)
            deleteOrphans()
        }
    }

    // حذف وسم من الإدارة: فك كل روابطه ثم تنظيف اليتيم — الملاحظات نفسها لا تُمس
    @Transaction
    suspend fun deleteTag(name: String) {
        val id = idOf(name) ?: return
        unlinkTagEverywhere(id)
        deleteOrphans()
    }

    @Query("DELETE FROM note_tags WHERE tagId = :tagId")
    suspend fun unlinkTagEverywhere(tagId: Long)

    @Query("SELECT t.name FROM note_tags nt JOIN tags t ON t.id = nt.tagId WHERE nt.noteId = :noteId ORDER BY t.name COLLATE NOCASE ASC")
    suspend fun tagsOf(noteId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<Tag>): List<Long>

    // المطابقة بلا حساسية حالة — «عمل» و«عمل» وسم واحد بصيغة أول إدخال
    @Query("SELECT id FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun idOf(name: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(link: NoteTag)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId AND tagId NOT IN (:keepIds)")
    suspend fun unlinkExcept(noteId: Long, keepIds: Set<Long>)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun unlinkAll(noteId: Long)

    // تنظيف الوسوم اليتيمة — بعد كل حفظ حتى لا تتراكم وسوم بلا ملاحظات
    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tagId FROM note_tags)")
    suspend fun deleteOrphans()

    // تعيين وسوم ملاحظة كاملة: تطبيع (قص فراغات + إسقاط الفارغ والمكرر بلا حساسية حالة) ثم ربط وحذف الزائد واليتيم
    @Transaction
    suspend fun setTags(noteId: Long, names: List<String>) {
        val clean = names.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        if (clean.isEmpty()) {
            unlinkAll(noteId)
            deleteOrphans()
            return
        }
        insertTags(clean.map { Tag(name = it) })
        val ids = clean.mapNotNull { idOf(it) }.toSet()
        unlinkExcept(noteId, ids)
        ids.forEach { link(NoteTag(noteId, it)) }
        deleteOrphans()
    }
}
