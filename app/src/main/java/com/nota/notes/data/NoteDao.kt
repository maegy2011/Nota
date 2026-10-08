package com.nota.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// فصل الحسابات: كل قوائم الملاحظات تُرشَّح بالمالك — المرئي = بلا مالك (محلية) أو مالكها الحالي
// المسجَّل خروجًا (acct=null) يرى المحلية فقط — فملاحظات حساب آخر لا تتسرب أبدًا على الجهاز المشترك
@Dao
interface NoteDao {
    // التصنيف: تسمية حرة واحدة للملاحظة — مستقلة عن المجلد (حاوية) والوسوم (متعددة)
    // النشطة فقط (غير المؤرشفة) — البحث يشمل الوسوم أيضًا
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) AND (title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%' OR EXISTS (SELECT 1 FROM note_tags nt JOIN tags t ON t.id = nt.tagId WHERE nt.noteId = notes.id AND t.name LIKE '%' || :q || '%')) ORDER BY updatedAt DESC")
    fun search(q: String, acct: String?): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY updatedAt DESC")
    fun all(acct: String?): Flow<List<Note>>

    // الأرشيف فقط — مع بحث داخله (يشمل الوسوم)
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NOT NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) AND (title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%' OR EXISTS (SELECT 1 FROM note_tags nt JOIN tags t ON t.id = nt.tagId WHERE nt.noteId = notes.id AND t.name LIKE '%' || :q || '%')) ORDER BY updatedAt DESC")
    fun searchArchived(q: String, acct: String?): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NOT NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY updatedAt DESC")
    fun archived(acct: String?): Flow<List<Note>>

    // المفضلة فقط — مع بحث داخلها (تستبعد الأرشيف والمحذوفة، ويشمل الوسوم)
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NULL AND favorite = 1 AND (ownerAccountId IS NULL OR ownerAccountId = :acct) AND (title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%' OR EXISTS (SELECT 1 FROM note_tags nt JOIN tags t ON t.id = nt.tagId WHERE nt.noteId = notes.id AND t.name LIKE '%' || :q || '%')) ORDER BY updatedAt DESC")
    fun searchFavorites(q: String, acct: String?): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NULL AND favorite = 1 AND (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY updatedAt DESC")
    fun favorites(acct: String?): Flow<List<Note>>

    // المفضلة المؤرشفة لاحقًا — تبويب منفصل في شاشة المفضلة (لا تُضاف مؤرشفة جديدة للمفضلة أبدًا)
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NOT NULL AND favorite = 1 AND (ownerAccountId IS NULL OR ownerAccountId = :acct) AND (title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%' OR EXISTS (SELECT 1 FROM note_tags nt JOIN tags t ON t.id = nt.tagId WHERE nt.noteId = notes.id AND t.name LIKE '%' || :q || '%')) ORDER BY updatedAt DESC")
    fun searchFavoritesArchived(q: String, acct: String?): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NOT NULL AND favorite = 1 AND (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY updatedAt DESC")
    fun favoritesArchived(acct: String?): Flow<List<Note>>

    // ملاحظات مجلد واحد — لتفاصيل المجلد (تشمل المؤرشفة، تستبعد المحذوفة نهائيًا)
    @Query("SELECT * FROM notes WHERE folderId = :folderId AND deletedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY updatedAt DESC")
    fun notesIn(folderId: Long, acct: String?): Flow<List<Note>>

    // التصنيفات: قيم مميزة غير فارغة للملاحظات الحية — للاقتراح والفلترة والإدارة
    @Query("SELECT DISTINCT category FROM notes WHERE category IS NOT NULL AND category != '' AND deletedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY category")
    fun distinctCategories(acct: String?): Flow<List<String>>

    @Query("SELECT category AS name, COUNT(*) AS count FROM notes WHERE category IS NOT NULL AND category != '' AND deletedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) GROUP BY category")
    fun categoryCounts(acct: String?): Flow<List<CategoryCount>>

    @Query("UPDATE notes SET category = :newName WHERE category = :old AND (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun renameCategory(acct: String?, old: String, newName: String)

    @Query("UPDATE notes SET category = NULL WHERE category = :name AND (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun clearCategory(acct: String?, name: String)

    // عناوين أحدث الملاحظات النشطة — لكاش الويدجت (بلا فتح مشفر ثقيل من الـ provider)
    // المقفلة (ملاحظة أو مجلدها) مستبعدة — عناوينها لا تظهر على الشاشة الرئيسية أبدًا
    @Query("SELECT n.title FROM notes n LEFT JOIN folders f ON n.folderId = f.id WHERE n.deletedAt IS NULL AND n.archivedAt IS NULL AND n.locked = 0 AND (f.locked IS NULL OR f.locked = 0) AND (n.ownerAccountId IS NULL OR n.ownerAccountId = :acct) ORDER BY n.updatedAt DESC LIMIT :limit")
    suspend fun latestTitles(limit: Int, acct: String?): List<String>

    @Insert
    suspend fun insert(n: Note): Long

    @Update
    suspend fun update(n: Note)

    // شاشة التنبيهات: كل الملاحظات ذات الموعد مرتبة بالأقرب — تشمل المؤرشفة (تُفتح من سياقها)
    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL AND deletedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY reminderAt ASC")
    fun reminded(acct: String?): Flow<List<Note>>

    // كل الملاحظات دفعة واحدة للنسخ الاحتياطي — تشمل الأرشيف (archivedAt) فيُستعاد معها
    // النطاق يمنع رفع ملاحظات حساب آخر إلى درايف الحساب الحالي — تسرب سحابي مغلق
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun allOnce(acct: String?): List<Note>

    // النسخة الكاملة (27 سبتمبر 2026): تشمل سلة المحذوفات أيضًا — تُستعاد السلة سلةً بتواريخ حذفها
    @Query("SELECT * FROM notes WHERE (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun backupAllOnce(acct: String?): List<Note>

    // أحدث تعديل محلي — لتحذير القدم عند معاينة نسخة أقدم من البيانات (27 سبتمبر 2026)
    @Query("SELECT MAX(updatedAt) FROM notes WHERE (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun newestUpdate(acct: String?): Long?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): Note?

    // المرحلة 0 — ربط الملاحظات المحلية بالحساب عند أول دخول: اليتيمة فقط (ownerAccountId IS NULL) تُنسب للحساب
    @Query("UPDATE notes SET ownerAccountId = :accountId WHERE ownerAccountId IS NULL")
    suspend fun claimUnowned(accountId: String): Int

    // حذف نهائي مباشر — بلا سلة مهملات بأمر المستخدم
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    // سلة المهملات (27 سبتمبر 2026): المحذوفة نرمًا — الأحدث حذفًا أولًا + بحث داخلها
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) ORDER BY deletedAt DESC")
    fun trashed(acct: String?): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct) AND (title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%') ORDER BY deletedAt DESC")
    fun searchTrashed(q: String, acct: String?): Flow<List<Note>>

    @Query("UPDATE notes SET deletedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDelete(id: Long, at: Long)

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    // التطهير: من تجاوز 30 يومًا يُحذف نهائيًا — يُستدعى عند فتح القفل
    @Query("SELECT id FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff AND (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun expiredIds(cutoff: Long, acct: String?): List<Long>

    @Query("SELECT id FROM notes WHERE deletedAt IS NOT NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun trashedIds(acct: String?): List<Long>

    // السياجات النشطة لإعادة التسجيل بعد الإقلاع (27 سبتمبر 2026)
    @Query("SELECT * FROM notes WHERE geoLat IS NOT NULL AND geoLng IS NOT NULL AND deletedAt IS NULL AND (ownerAccountId IS NULL OR ownerAccountId = :acct)")
    suspend fun geofencedOnce(acct: String?): List<Note>
}
