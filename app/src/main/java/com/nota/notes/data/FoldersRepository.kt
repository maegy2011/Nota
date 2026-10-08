package com.nota.notes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

// مستودع المجلدات — يعمل عبر DbHolder المعلّق مثل NotesRepository — وبنطاق الحساب الحالي
class FoldersRepository(private val holder: DbHolder, private val accounts: AccountStore) {
    fun accountChanges(): kotlinx.coroutines.flow.Flow<String?> = accounts.accountId
    private suspend fun acct(): String? = accounts.currentAccountId()
    fun all(): Flow<List<Folder>> = flow { emitAll(holder.folderDao().all(acct())) }

    // عدّادات الملاحظات لكل مجلد — تتحدث تلقائيًا مع تغيّر الملاحظات
    fun counts(): Flow<List<FolderNoteCount>> = flow { emitAll(holder.folderDao().counts(acct())) }

    // إحصائية كل مجلد — تتحدث تلقائيًا مع تغيّر الملاحظات (27 سبتمبر 2026)
    fun stats(): Flow<List<FolderStatsRow>> = flow { emitAll(holder.folderDao().stats(acct())) }

    // ملاحظات مجلد واحد — لتفاصيل المجلد
    fun notesIn(folderId: Long): Flow<List<Note>> = flow { emitAll(holder.dao().notesIn(folderId, acct())) }

    suspend fun create(name: String, color: Int = 0): Long = withContext(Dispatchers.IO) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "empty folder name" }
        holder.folderDao().insert(Folder(name = clean, color = color, ownerAccountId = acct()))
    }

    suspend fun rename(id: Long, name: String) = withContext(Dispatchers.IO) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "empty folder name" }
        holder.folderDao().byId(id)?.let { holder.folderDao().update(it.copy(name = clean)) }
    }

    // لون المجلد — فهرس لوحة Sticky Notes مثل الملاحظات
    suspend fun setColor(id: Long, color: Int) = withContext(Dispatchers.IO) {
        holder.folderDao().byId(id)?.let { holder.folderDao().update(it.copy(color = color)) }
    }

    // تحديث الاسم واللون معًا في كتابة واحدة — استدعاء rename ثم setColor منفصلين
    // كانا قراءة-تعديل-كتابة متزامنتين فيفوز أحدهما ويضيع الآخر (تغيير اللون لا يظهر)
    suspend fun renameWithColor(id: Long, name: String, color: Int) = withContext(Dispatchers.IO) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "empty folder name" }
        holder.folderDao().byId(id)?.let { holder.folderDao().update(it.copy(name = clean, color = color)) }
    }

    // حذف المجلد يُبقي الملاحظات (تعود بلا مجلد) — لا حذف متتابع أبدًا — وحارس المالك يمنع مساس حساب آخر
    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val dao = holder.folderDao()
        val a = acct()
        val f = dao.byId(id)
        if (f == null || !(f.ownerAccountId == null || f.ownerAccountId == a)) return@withContext
        dao.detachNotes(id, a)
        dao.deleteById(id)
    }

    suspend fun moveNote(noteId: Long, folderId: Long?) = withContext(Dispatchers.IO) {
        holder.dao().byId(noteId)?.let { holder.dao().update(it.copy(folderId = folderId)) }
    }
    // تثبيت المجلد بالدبوس — يقلب الحالة (متعدد: أي عدد يُثبت معًا)
    suspend fun togglePin(id: Long) = withContext(Dispatchers.IO) {
        holder.folderDao().byId(id)?.let { holder.folderDao().update(it.copy(pinned = !it.pinned)) }
    }
    // القفل الخاص للمجلد — يقلب الحالة (تفاصيله وملاحظاته تتطلب تحديًا)
    suspend fun toggleLock(id: Long) = withContext(Dispatchers.IO) {
        holder.folderDao().byId(id)?.let { holder.folderDao().update(it.copy(locked = !it.locked)) }
    }
}
