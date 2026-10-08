package com.nota.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    // فصل الحسابات: المرئي = بلا مالك أو مالكها الحالي — كالملاحظات تمامًا
    @Query("SELECT * FROM folders WHERE (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY createdAt ASC")
    fun all(acct: String?): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY createdAt ASC")
    suspend fun allOnce(acct: String?): List<Folder>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): Folder?

    @Insert
    suspend fun insert(f: Folder): Long

    @Update
    suspend fun update(f: Folder)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)

    // فك ارتباط ملاحظات المجلد المحذوف بدل حذفها — الملاحظات تعود بلا مجلد — بنطاق الحساب فقط
    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId AND (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun detachNotes(folderId: Long, acct: String?)

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId AND deletedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun noteCount(folderId: Long, acct: String?): Int

    // عدّادات كل المجلدات دفعة واحدة — للشارات بلا N استعلام — بنطاق الحساب
    @Query("SELECT folderId AS folderId, COUNT(*) AS count FROM notes WHERE folderId IS NOT NULL AND deletedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) GROUP BY folderId")
    fun counts(acct: String?): Flow<List<FolderNoteCount>>

    // إحصائية كل مجلد دفعة واحدة (27 سبتمبر 2026): الإجمالي + المفضلة + التنبيهات + المثبتة + آخر نشاط — بنطاق الحساب
    @Query("SELECT folderId AS folderId, COUNT(*) AS total, SUM(CASE WHEN favorite THEN 1 ELSE 0 END) AS favorites, SUM(CASE WHEN reminderAt IS NOT NULL THEN 1 ELSE 0 END) AS reminders, SUM(CASE WHEN pinned THEN 1 ELSE 0 END) AS pinned, MAX(updatedAt) AS lastActive FROM notes WHERE folderId IS NOT NULL AND deletedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) GROUP BY folderId")
    fun stats(acct: String?): Flow<List<FolderStatsRow>>

    // نسب المجلدات اليتيمة للحساب عند أول دخول — كالملاحظات تمامًا
    @Query("UPDATE folders SET ownerAccountId = :accountId WHERE ownerAccountId IS NULL")
    suspend fun claimUnowned(accountId: String): Int
}

// صف العداد — أسماء الأعمدة تطابق الحقول حرفيًا
// تحسين المجلدات: عداد الملاحظات على كل مجلد

data class FolderNoteCount(val folderId: Long, val count: Int)

// صف الإحصائية — أسماء الأعمدة تطابق الحقول حرفيًا
data class FolderStatsRow(val folderId: Long, val total: Int, val favorites: Int, val reminders: Int, val pinned: Int, val lastActive: Long)
