package com.nota.notes.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// يعمل عبر DbHolder المعلّق — أول وصول يفتح المشفر على IO دون تجميد الواجهة
class NotesRepository(
    private val holder: DbHolder,
    @ApplicationContext private val appCtx: Context,
    private val accounts: AccountStore
) {
    // فصل الحسابات: الحساب الحالي يُمرَّر لكل استعلام — فلا تتداخل ملاحظات مستخدمين أبدًا
    // تغيّره يُعيد تشغيل كل التدفقات (تُجمَع معه في الـ ViewModels) — فالخروج/الدخول يبدّل القوائم فورًا
    fun accountChanges(): kotlinx.coroutines.flow.Flow<String?> = accounts.accountId
    private suspend fun acct(): String? = accounts.currentAccountId()
    // الإتلاف الآمن اختياري: null = حذف عادي، وإلا طريقة التمريرات من الإعدادات
    private val shredPrefs = SettingsStore(appCtx)
    private suspend fun shredMethod(): String? =
        if (shredPrefs.shredEnabled.first()) shredPrefs.shredMethod.first() else null
    fun all(): Flow<List<Note>> = flow { emitAll(holder.dao().all(acct())) }
    fun search(q: String): Flow<List<Note>> = flow { emitAll(holder.dao().search(q.trim(), acct())) }
    // السلة (27 سبتمبر 2026): قائمة + بحث — بنطاق الحساب كالبقية
    fun trashed(): Flow<List<Note>> = flow { emitAll(holder.dao().trashed(acct())) }
    fun searchTrash(q: String): Flow<List<Note>> = flow { emitAll(holder.dao().searchTrashed(q.trim(), acct())) }
    // نقل نرم للسلة: deletedAt=الآن + إلغاء المنبه (فلا ينطلق لملاحظة محذوفة)
    suspend fun trashOne(id: Long) = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        dao.byId(id)?.let { n ->
            if (n.deletedAt == null) {
                dao.softDelete(id, System.currentTimeMillis())
                com.nota.notes.reminder.ReminderScheduler.cancel(appCtx, id)
                com.nota.notes.geo.GeoScheduler.remove(appCtx, id)
            }
        }
    }
    suspend fun trashMany(ids: Set<Long>) = withContext(Dispatchers.IO) {
        ids.forEach { trashOne(it) }
    }
    // الاستعادة تُعيد تسجيل السياج إن وُجدت إحداثياته، وتُعيد جدولة التذكيرات فورًا (27 سبتمبر 2026)
    suspend fun restoreOne(id: Long) = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        dao.restore(id)
        dao.byId(id)?.let { n ->
            val la = n.geoLat; val ln = n.geoLng
            if (la != null && ln != null) com.nota.notes.geo.GeoScheduler.add(appCtx, id, la, ln, n.geoRadiusM)
            // حذف الملاحظة كان قد ألغى منبهها؛ لا تنتظر فتح القفل التالي لإعادته.
            com.nota.notes.reminder.ReminderScheduler.rescheduleAll(appCtx)
        }
    }
    suspend fun restoreMany(ids: Set<Long>) = withContext(Dispatchers.IO) { ids.forEach { restoreOne(it) } }
    // P3-D2: ترحيل أسماء legacy → معتمة (idempotent) — الملف + جانبي .key معًا، ثم مراجع DB.
    // اليتيم (بلا مرجع DB) يُعتَّم أيضًا بلا ضرر. كل خطوة محمية — الفشل لا يُسقط الصيانة.
    suspend fun migrateMediaFilenames() = withContext(Dispatchers.IO) {
        runCatching {
            val content = holder.get().content()
            renameDirFiles(NoteMediaFiles.dir(appCtx)) { old, new ->
                runCatching { content.renameFile(old, new) }
                Unit
            }
            renameDirFiles(NoteBgFiles.dir(appCtx)) { old, new ->
                runCatching { holder.dao().renameBackground(old, new) }
                Unit
            }
        }
    }

    private suspend fun renameDirFiles(dir: java.io.File, updateRef: suspend (String, String) -> Unit) {
        val files = dir.listFiles()?.filter { it.isFile && OpaqueFileNames.isLegacy(it.name) } ?: return
        files.forEach { f ->
            runCatching {
                val newName = OpaqueFileNames.random(OpaqueFileNames.extOf(f.name))
                val newFile = java.io.File(dir, newName)
                // المفتاح الجانبي يُعاد تسميته مع ملفه — وإلا يُيتم الملف الجديد (غير قابل للفك)
                val kf = com.nota.notes.security.PerFileMedia.keyFileFor(f)
                val hasKey = kf.exists()
                if (!f.renameTo(newFile)) return@runCatching
                if (hasKey) runCatching {
                    kf.renameTo(com.nota.notes.security.PerFileMedia.keyFileFor(newFile))
                }
                updateRef(f.name, newName)
            }
        }
    }

    // تطهير المنتهية (30 يومًا): حذف نهائي كامل (ملفات + منبهات) — تُستدعى عند فتح القفل
    suspend fun purgeTrash(): Int = withContext(Dispatchers.IO) {
        runCatching { migrateMediaFilenames() } // P3-D2: صيانة فتح القفل تشمل تعتيم الأسماء القديمة
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val ids = holder.dao().expiredIds(cutoff, acct())
        ids.forEach { hardDelete(it) }
        ids.size
    }
    // الحذف النهائي الكامل (كان جسم delete): ملفات + إتلاف + منبه + أيتام + VACUUM
    // عناوين الأحدث للويدجت — تُقرأ بعد فتح القفل فقط (الـ provider لا يفك التشفير أبدًا)
    suspend fun latestTitles(limit: Int = 3): List<String> = holder.dao().latestTitles(limit, acct())
    suspend fun save(id: Long?, title: String, body: String, color: Int = 0, folderId: Long? = null, background: String? = null, lat: Double? = null, lng: Double? = null, category: String? = null): Long {
        val dao = holder.dao()
        val now = System.currentTimeMillis()
        val cat = category?.trim()?.take(64)?.takeIf { it.isNotBlank() }
        // الجديدة تُنسب للحساب الحالي فورًا (أو تبقى محلية بلا حساب) — فلا تُرى من حساب آخر أبدًا
        if (id == null || id == 0L) return dao.insert(Note(title = title, body = body, createdAt = now, updatedAt = now, color = color, folderId = folderId, category = cat, background = background, latitude = lat, longitude = lng, ownerAccountId = acct()))
        else {
            val old = dao.byId(id) ?: return id
            // استبدال الخلفية يمسح الملف القديم فورًا — بلا بقايا صور على القرص (إتلاف آمن عند التفعيل)
            if (background != old.background) {
                val m = shredMethod()
                if (m != null) NoteBgFiles.shred(appCtx, old.background, m) else NoteBgFiles.delete(appCtx, old.background)
            }
            dao.update(old.copy(title = title, body = body, updatedAt = now, color = color, category = cat, background = background, latitude = lat, longitude = lng))
            return id
        }
    }
    // تكرار ملاحظة: نفس المحتوى واللون والمجلد والخلفية (ملف مستقل) — بلا دبوس ولا مفضلة — والقفل يُورَّث حتى لا تتسرب المقفلة بنسخة مفتوحة
    // العنوان الجديد يُبنى خارجًا (صيغة copy_title_fmt) — يرجع معرف النسخة أو -1
    suspend fun duplicate(id: Long, newTitle: String): Long = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        val old = dao.byId(id) ?: return@withContext -1L
        val now = System.currentTimeMillis()
        val bg = NoteBgFiles.duplicate(appCtx, old.background)
        val newId = dao.insert(
            Note(
                title = newTitle, body = old.body, createdAt = now, updatedAt = now,
                color = old.color, folderId = old.folderId, category = old.category, background = bg,
                pinned = false, locked = old.locked, favorite = false,
                latitude = old.latitude, longitude = old.longitude,
                ownerAccountId = old.ownerAccountId
            )
        )
        // الوسوم تُنسخ كما هي — النسخة تحمل نفس التصنيف
        holder.tagDao().tagsOf(id).takeIf { it.isNotEmpty() }?.let { holder.tagDao().setTags(newId, it) }
        // المهام تُنسخ غير منجزة (عمل جديد) — والصور تُنسخ ملفات مستقلة (kind محفوظ)
        runCatching { holder.contentDao().tasksOf(id) }.getOrDefault(emptyList()).forEachIndexed { i, t ->
            runCatching { holder.contentDao().insertTask(ChecklistItem(noteId = newId, text = t.text, done = false, sort = i)) }
        }
        runCatching { holder.contentDao().attachmentsOf(id) }.getOrDefault(emptyList()).forEach { a ->
            runCatching {
                val src = NoteMediaFiles.file(appCtx, a.file)
                if (src.exists()) {
                    // إصلاح: الامتداد حسب النوع (m4a/png) — لا jpg للكل حتى لا يكسر التشغيل والعارض
                    val ext = when (a.kind) {
                        NoteAttachment.KIND_AUDIO -> "m4a"
                        NoteAttachment.KIND_DRAWING -> "png"
                        else -> "jpg"
                    }
                    val dst = "img_${System.currentTimeMillis()}_${a.id}.$ext"
                    src.copyTo(NoteMediaFiles.file(appCtx, dst))
                    holder.contentDao().insertAttachment(NoteAttachment(noteId = newId, file = dst, kind = a.kind))
                }
            }
        }
        newId
    }
    // القفل الخاص — يقلب الحالة (updatedAt لا تتغير حتى لا تقفز المقفلة زمنيًا)
    suspend fun toggleLock(id: Long): Boolean = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        val old = dao.byId(id) ?: return@withContext false
        dao.update(old.copy(locked = !old.locked))
        !old.locked
    }
    // التثبيت بالدبوس — يقلب الحالة (updatedAt لا تتغير حتى لا تقفز المثبتة للأعلى زمنيًا)
    suspend fun togglePin(id: Long): Boolean = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        val old = dao.byId(id) ?: return@withContext false
        dao.update(old.copy(pinned = !old.pinned))
        !old.pinned
    }

    // الوسوم: كل الأسماء + خريطة noteId ← أسماء + تعيين كامل لملاحظة
    fun allTags(): Flow<List<Tag>> = flow { emitAll(holder.tagDao().allTags(acct())) }
    fun tagsMap(): Flow<Map<Long, List<String>>> =
        flow { emitAll(holder.tagDao().allMappings(acct())) }.map { rows -> rows.groupBy({ it.noteId }, { it.name }) }
    suspend fun tagsOf(noteId: Long): List<String> = holder.tagDao().tagsOf(noteId)
    // إدارة الوسوم: العدّادات + إعادة التسمية (تدمج) + الحذف (يفك الروابط فقط)
    fun tagCounts(): Flow<Map<String, Int>> =
        flow { emitAll(holder.tagDao().tagCounts(acct())) }.map { rows -> rows.associate { it.name to it.count } }
    suspend fun renameTag(old: String, newName: String) = holder.tagDao().renameTag(old, newName)
    // إنشاء وسم مستقل من الإدارة (قرار 26 سبتمبر 2026): يُحفظ بلا ملاحظات — يظهر بعدّاد صفر
    suspend fun createTag(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        holder.tagDao().insertTags(listOf(com.nota.notes.data.Tag(name = clean)))
    }

    // التصنيفات: تسمية واحدة حرة للملاحظة — بنطاق الحساب الحالي كبقية الاستعلامات
    fun categories(): Flow<List<String>> =
        flow { emitAll(holder.dao().distinctCategories(acct())) }
    fun categoryCounts(): Flow<Map<String, Int>> =
        flow { emitAll(holder.dao().categoryCounts(acct())) }.map { rows -> rows.associate { it.name to it.count } }
    suspend fun setCategory(id: Long, category: String?) {
        val dao = holder.dao()
        val n = dao.byId(id) ?: return
        if (n.ownerAccountId != null && n.ownerAccountId != acct()) return
        dao.update(n.copy(category = category?.trim()?.take(64)?.takeIf { it.isNotBlank() }, updatedAt = System.currentTimeMillis()))
    }
    suspend fun renameCategory(old: String, newName: String) {
        val clean = newName.trim().take(64)
        if (clean.isBlank()) return
        holder.dao().renameCategory(acct(), old, clean)
    }
    suspend fun clearCategory(name: String) = holder.dao().clearCategory(acct(), name)
    suspend fun deleteTag(name: String) = holder.tagDao().deleteTag(name)
    // شاشة التنبيهات: كل ذات الموعد مرتبة بالأقرب
    fun reminders(): Flow<List<Note>> = flow { emitAll(holder.dao().reminded(acct())) }
    suspend fun setTags(noteId: Long, names: List<String>) = holder.tagDao().setTags(noteId, names)
    // الأرشيف: إخفاء من الرئيسية بلا حذف — والاستعادة تعيدها نشطة
    suspend fun archive(id: Long) = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        dao.byId(id)?.let { dao.update(it.copy(archivedAt = System.currentTimeMillis())) }
    }
    suspend fun unarchive(id: Long) = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        dao.byId(id)?.let { dao.update(it.copy(archivedAt = null)) }
    }
    fun archived(): Flow<List<Note>> = flow { emitAll(holder.dao().archived(acct())) }
    fun searchArchived(q: String): Flow<List<Note>> = flow { emitAll(holder.dao().searchArchived(q.trim(), acct())) }
    // المفضلة: بحث مستقل + قلب يقلب الحالة (updatedAt لا تتغير)
    fun favorites(): Flow<List<Note>> = flow { emitAll(holder.dao().favorites(acct())) }
    fun searchFavorites(q: String): Flow<List<Note>> = flow { emitAll(holder.dao().searchFavorites(q.trim(), acct())) }
    // المفضلة المؤرشفة لاحقًا — تبويب منفصل (قراءة فقط من حيث الإضافة)
    fun favoritesArchived(): Flow<List<Note>> = flow { emitAll(holder.dao().favoritesArchived(acct())) }
    fun searchFavoritesArchived(q: String): Flow<List<Note>> = flow { emitAll(holder.dao().searchFavoritesArchived(q.trim(), acct())) }
    suspend fun toggleFavorite(id: Long): Boolean = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        val old = dao.byId(id) ?: return@withContext false
        // المؤرشفة لا تُضاف للمفضلة أبدًا — الإزالة منها مسموحة فقط
        if (old.archivedAt != null && !old.favorite) return@withContext false
        dao.update(old.copy(favorite = !old.favorite))
        !old.favorite
    }
    suspend fun unfavoriteMany(ids: Set<Long>) = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        ids.forEach { dao.byId(it)?.let { dao.update(it.copy(favorite = false)) } }
    }

    // النسخ الاحتياطي: كل الملاحظات (نشطة + أرشيف) دفعة واحدة
    // النسخ الاحتياطي: كل الملاحظات مع وسومها وتذكيراتها — التشفير في BackupManager
    // أحدث تعديل محلي — لتحذير القدم عند المعاينة (27 سبتمبر 2026)
    suspend fun newestUpdate(): Long = withContext(Dispatchers.IO) {
        runCatching { holder.dao().newestUpdate(acct()) ?: 0L }.getOrDefault(0L)
    }

    // نسخة التطبيق لبطاقة النسخة (v3) — تُقرأ مرة واحدة وتُمرَّر للترميز
    fun appVersion(): String = runCatching {
        appCtx.packageManager.getPackageInfo(appCtx.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    // مجلدات الحساب للنسخة (v3): الاسم + اللون — تُعاد عند الاستعادة
    suspend fun backupFolders(): List<BackupFolder> = withContext(Dispatchers.IO) {
        runCatching { holder.folderDao().allOnce(acct()) }.getOrDefault(emptyList())
            .map { BackupFolder(it.name, it.color) }
    }

    // نسخة أمان تلقائية قبل أي استعادة دمج (27 سبتمبر 2026): مشفرة بمفتاح الجهاز في files — يُحتفظ بآخر 3 فقط
    suspend fun safetySnapshot(): Unit = withContext(Dispatchers.IO) {
        runCatching {
            val dir = java.io.File(appCtx.filesDir, "auto-backups").also { it.mkdirs() }
            val json = BackupCodec.encode(backupNotes(), backupFolders(), appVersion())
            BackupManager.export(dir, json)
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(3)?.forEach { runCatching { it.delete() } }
        }
    }

    suspend fun backupNotes(): List<BackupNote> = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        val tagDao = holder.tagDao()
        // خريطة المجلدات id→name لحفظ العضوية في النسخة (v3)
        val folderNames = runCatching { holder.folderDao().allOnce(acct()) }.getOrDefault(emptyList())
            .associate { it.id to it.name }
        // النسخ للدرايف بنطاق الحساب الحالي فقط — ملاحظات حساب آخر لا تُرفع أبدًا
        // النسخة الكاملة تشمل السلة (27 سبتمبر 2026) — deletedAt يُحفظ ويُستعاد
        dao.backupAllOnce(acct()).map { n ->
            BackupNote(title = n.title, body = n.body, createdAt = n.createdAt,
                updatedAt = n.updatedAt, color = n.color, archivedAt = n.archivedAt, pinned = n.pinned,
                folder = n.folderId?.let { folderNames[it] },
                deletedAt = n.deletedAt,
                geoLat = n.geoLat, geoLng = n.geoLng, geoRadiusM = n.geoRadiusM, geoName = n.geoName,
                locked = n.locked, background = n.background, favorite = n.favorite, category = n.category,
                tags = runCatching { tagDao.tagsOf(n.id) }.getOrDefault(emptyList()),
                reminderAt = n.reminderAt, reminderRepeat = n.reminderRepeat, reminderBeforeMin = n.reminderBeforeMin,
                latitude = n.latitude, longitude = n.longitude,
                repeatInterval = n.repeatInterval, repeatDays = n.repeatDays, repeatMonthMode = n.repeatMonthMode,
                repeatEnd = n.repeatEnd, repeatEndValue = n.repeatEndValue,
                tasks = runCatching { holder.contentDao().tasksOf(n.id) }.getOrDefault(emptyList())
                    .map { BackupTask(it.text, it.done) },
                attachments = runCatching { holder.contentDao().attachmentsOf(n.id) }.getOrDefault(emptyList())
                    .map { BackupAttachment(it.kind, it.file) })
        }
    }

    // المرحلة 0 — أول دخول بحساب: الملاحظات المحلية اليتيمة تُنسب للحساب (تبقى مشفرة على الجهاز كما هي)
    // نسب اليتيم (ملاحظات + مجلدات) للحساب عند أول دخول — تبقى مشفرة على الجهاز كما هي
    suspend fun claimLocalNotes(accountId: String): Int = withContext(Dispatchers.IO) {
        val n = holder.dao().claimUnowned(accountId)
        runCatching { holder.folderDao().claimUnowned(accountId) }
        n
    }

    // الاستعادة بالدمج: المكرر (نفس العنوان+النص+تاريخ الإنشاء) يُتخطى، والجديد يُدرج بهوية جديدة —
    // يرجع (مضافة، متخطاة). الأرشيف يُستعاد مؤرشفًا لأنه محفوظ في النسخة
    // نتيجة الدمج — addedIds للتراجع الآمن (نقل للسلة لا حذف) — سجل الاستعادة (27 سبتمبر 2026)
    data class RestoreResult(val added: Int, val skipped: Int, val addedIds: List<Long>)
    suspend fun restoreMerge(items: List<BackupNote>, folders: List<BackupFolder> = emptyList()): RestoreResult = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        // مجلدات النسخة تُعاد إنشاؤها إن غابت (v3) — بنطاق الحساب الحالي — ثم تُنسب إليها الملاحظات
        val folderIds = mutableMapOf<String, Long>()
        runCatching {
            val fdao = holder.folderDao()
            val a = acct()
            fdao.allOnce(a).forEach { folderIds[it.name] = it.id }
            folders.forEach { bf ->
                if (!folderIds.containsKey(bf.name))
                    folderIds[bf.name] = fdao.insert(Folder(name = bf.name, color = bf.color, ownerAccountId = a))
            }
        }
        // كشف المكرر بنطاق الحساب الحالي — يشمل السلة فلا تتكرر مستعادة محذوفة
        val have = dao.backupAllOnce(acct()).map { Triple(it.title, it.body, it.createdAt) }.toSet()
        var added = 0; var skipped = 0
        val addedIds = mutableListOf<Long>()
        items.forEach { b ->
            if (have.contains(Triple(b.title, b.body, b.createdAt))) { skipped++; return@forEach }
            // المستعادة تُنسب للحساب الحالي فورًا — فلا تظهر لحساب آخر على الجهاز
            // وتعود لمجلدها بالاسم — الغائب أُعيد إنشاؤه أعلاه (v3)
            val newId = dao.insert(Note(title = b.title, body = b.body, createdAt = b.createdAt,
                updatedAt = b.updatedAt, color = b.color, archivedAt = b.archivedAt, pinned = b.pinned,
                deletedAt = b.deletedAt,
                geoLat = b.geoLat, geoLng = b.geoLng, geoRadiusM = b.geoRadiusM, geoName = b.geoName,
                folderId = b.folder?.let { folderIds[it] },
                locked = b.locked, background = b.background, favorite = b.favorite, category = b.category,
                reminderAt = b.reminderAt, reminderRepeat = b.reminderRepeat, reminderBeforeMin = b.reminderBeforeMin,
                latitude = b.latitude, longitude = b.longitude,
                repeatInterval = b.repeatInterval, repeatDays = b.repeatDays, repeatMonthMode = b.repeatMonthMode,
                repeatEnd = b.repeatEnd, repeatEndValue = b.repeatEndValue, ownerAccountId = acct()))
            if (b.tags.isNotEmpty()) runCatching { holder.tagDao().setTags(newId, b.tags) }
            // المهام تُستعاد بنفس الترتيب — والمرفقات تُسجَّل متى وُجدت ملفاتها (نفس الجهاز)
            if (b.tasks.isNotEmpty()) runCatching {
                b.tasks.forEachIndexed { i, t ->
                    holder.contentDao().insertTask(ChecklistItem(noteId = newId, text = t.text, done = t.done, sort = i))
                }
            }
            if (b.attachments.isNotEmpty()) runCatching {
                b.attachments.filter { NoteMediaFiles.file(appCtx, it.file).exists() }.forEach { a ->
                    holder.contentDao().insertAttachment(NoteAttachment(noteId = newId, file = a.file, kind = a.kind))
                }
            }
            // التذكير المستقبلي يُجدوَل من جديد — الماضي يُترك سجلًا بلا منبه
            // المحذوفة (سلة) لا تُجدوَل — منبهها أُلغي يوم حذفها
            if (b.deletedAt == null) {
                if (b.reminderAt != null && b.reminderAt > System.currentTimeMillis())
                    runCatching { com.nota.notes.reminder.ReminderScheduler.schedule(appCtx, newId, b.reminderAt, b.reminderBeforeMin) }
                // السياج يُعاد تسجيله — بلا إذن موقع يُتجاهل بصمت ويُطلب عند الاستخدام
                if (b.geoLat != null && b.geoLng != null)
                    runCatching { com.nota.notes.geo.GeoScheduler.add(appCtx, newId, b.geoLat, b.geoLng, b.geoRadiusM) }
            }
            addedIds.add(newId)
            added++
        }
        RestoreResult(added, skipped, addedIds)
    }

    // سجل الاستعادة عبر مخزن الإعدادات — تمرير للواجهات (27 سبتمبر 2026)
    fun restoreLog(): kotlinx.coroutines.flow.Flow<List<RestoreRecord>> = shredPrefs.restoreLog
    suspend fun restoreLogOnce(): List<RestoreRecord> = shredPrefs.restoreLog.first()
    suspend fun logRestore(r: RestoreRecord) = shredPrefs.logRestore(r)
    suspend fun markRestoreUndone(id: Long) = shredPrefs.markRestoreUndone(id)

    // التراجع عن دمج: المضافة تُنقل للسلة (قابلة للاستعادة 30 يومًا) — لا حذف نهائي أبدًا (27 سبتمبر 2026)
    suspend fun undoRestore(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        var n = 0
        ids.forEach { id ->
            runCatching {
                val cur = holder.dao().byId(id)
                if (cur != null && cur.deletedAt == null) { trashOne(id); n++ }
            }
        }
        n
    }

    // التذكير: حفظ الموعد والتكرار والإشعار المسبق + جدولة المنبه — والمسح يلغي المنبه
    suspend fun setReminder(id: Long, spec: ReminderSpec) = withContext(Dispatchers.IO) {
        holder.dao().byId(id)?.let {
            holder.dao().update(it.copy(reminderAt = spec.at, reminderRepeat = spec.repeat,
                reminderBeforeMin = spec.beforeMin, repeatInterval = spec.interval.coerceIn(1, 99),
                repeatDays = spec.days, repeatMonthMode = spec.monthMode,
                repeatEnd = spec.end, repeatEndValue = spec.endValue))
            if (spec.at != null) com.nota.notes.reminder.ReminderScheduler.schedule(appCtx, id, spec.at, spec.beforeMin)
            else com.nota.notes.reminder.ReminderScheduler.cancel(appCtx, id)
        }
    }
    // تذكير الموقع (27 سبتمبر 2026): حفظ الإحداثيات + تسجيل السياج — والمسح يلغي التسجيل
    suspend fun setGeofence(id: Long, lat: Double, lng: Double, radiusM: Float, name: String) = withContext(Dispatchers.IO) {
        holder.dao().byId(id)?.let {
            holder.dao().update(it.copy(geoLat = lat, geoLng = lng,
                geoRadiusM = radiusM.coerceIn(com.nota.notes.geo.GeoScheduler.MIN_RADIUS, com.nota.notes.geo.GeoScheduler.MAX_RADIUS),
                geoName = name.trim().take(64)))
            com.nota.notes.geo.GeoScheduler.add(appCtx, id, lat, lng, radiusM)
        }
    }
    suspend fun clearGeofence(id: Long) = withContext(Dispatchers.IO) {
        holder.dao().byId(id)?.let {
            holder.dao().update(it.copy(geoLat = null, geoLng = null, geoName = ""))
            com.nota.notes.geo.GeoScheduler.remove(appCtx, id)
        }
    }
    // توافق قديم: الثلاثية الكلاسيكية (تُستخدم داخليًا فقط)
    suspend fun setReminder(id: Long, at: Long, repeat: String, beforeMin: Long) =
        setReminder(id, ReminderSpec(at = at, repeat = repeat, beforeMin = beforeMin))
    suspend fun clearReminder(id: Long) = withContext(Dispatchers.IO) {
        holder.dao().byId(id)?.let {
            holder.dao().update(it.copy(reminderAt = null, reminderRepeat = com.nota.notes.reminder.ReminderScheduler.REPEAT_NONE, reminderBeforeMin = 0L,
                repeatInterval = 1, repeatDays = "", repeatMonthMode = ReminderSpec.MONTH_SAME,
                repeatEnd = ReminderSpec.END_FOREVER, repeatEndValue = 0L))
            com.nota.notes.reminder.ReminderScheduler.cancel(appCtx, id)
        }
    }
    // إعادة جدولة كل المنبهات المستقبلية — عند فتح القفل وبعد الاستعادة
    suspend fun rescheduleReminders() = withContext(Dispatchers.IO) {
        com.nota.notes.reminder.ReminderScheduler.rescheduleAll(appCtx)
    }
    // إلغاء منبه المحذوفة — حتى لا تنطلق لملاحظة لم تعد موجودة
    // حذف نهائي بلا أثر: DELETE ثم VACUUM يعيد بناء ملف القاعدة المشفرة
    // فيمسح بقايا الصف من صفحات SQLite المحررة (freelist) — لا يبقى بايت واحد قابل للاسترجاع من الملف
    // + حذف ملف الخلفية إن وُجد — الصورة تُمسح مع ملاحظتها
    // محتوى الملاحظة: مهام checklist + مرفقات صور (الصوت والرسم لاحقًا بنفس الجدول)
    suspend fun tasksOf(noteId: Long): List<ChecklistItem> = holder.contentDao().tasksOf(noteId)
    suspend fun addTask(noteId: Long, text: String): Long = withContext(Dispatchers.IO) {
        val dao = holder.contentDao()
        val clean = text.trim().take(200)
        if (clean.isEmpty()) return@withContext -1L
        val next = (dao.tasksOf(noteId).maxOfOrNull { it.sort } ?: -1) + 1
        dao.insertTask(ChecklistItem(noteId = noteId, text = clean, sort = next))
    }
    suspend fun toggleTask(item: ChecklistItem): Boolean = withContext(Dispatchers.IO) {
        holder.contentDao().updateTask(item.copy(done = !item.done))
        !item.done
    }
    suspend fun renameTask(item: ChecklistItem, text: String) = withContext(Dispatchers.IO) {
        val clean = text.trim().take(200)
        if (clean.isNotEmpty()) holder.contentDao().updateTask(item.copy(text = clean))
    }
    suspend fun deleteTask(id: Long) = withContext(Dispatchers.IO) { holder.contentDao().deleteTask(id) }
    suspend fun attachmentsOf(noteId: Long): List<NoteAttachment> = holder.contentDao().attachmentsOf(noteId)
    suspend fun addAttachment(noteId: Long, file: String, kind: String = NoteAttachment.KIND_IMAGE): Long =
        withContext(Dispatchers.IO) { holder.contentDao().insertAttachment(NoteAttachment(noteId = noteId, file = file, kind = kind)) }
    suspend fun deleteAttachment(att: NoteAttachment) = withContext(Dispatchers.IO) {
        holder.contentDao().deleteAttachment(att.id)
        val m = shredMethod()
        if (m != null) NoteMediaFiles.shred(appCtx, att.file, m) else NoteMediaFiles.delete(appCtx, att.file)
    }
    // ملفات مرفقات المحذوفة تُمسح معها — بلا بقايا صور على القرص (السجلات بالتتابع تلقائيًا)
    private suspend fun deleteAttachmentFiles(noteId: Long, method: String? = null) {
        runCatching { holder.contentDao().filesOf(noteId) }.getOrDefault(emptyList())
            .forEach { if (method != null) NoteMediaFiles.shred(appCtx, it, method) else NoteMediaFiles.delete(appCtx, it) }
    }
    // طمس صف القاعدة قبل حذفه (أفضل جهد — WAL والقاعدة مشفرة أصلًا) ثم VACUUM الموجود يستعيد الصفحات
    private suspend fun shredRowMask(id: Long) {
        runCatching {
            val dao = holder.dao()
            dao.byId(id)?.let { n -> dao.update(n.copy(title = SecureDelete.maskText(n.title), body = SecureDelete.maskText(n.body))) }
        }
    }
    // الحذف من السلة وفي التطهير = نهائي — أما حذف القوائم فنقل نرم للسلة (27 سبتمبر 2026)
    suspend fun delete(id: Long) = trashOne(id)
    private suspend fun hardDelete(id: Long) {
        val db = holder.get()
        val m = shredMethod()
        db.notes().byId(id)?.let {
            if (m != null) NoteBgFiles.shred(appCtx, it.background, m) else NoteBgFiles.delete(appCtx, it.background)
        }
        deleteAttachmentFiles(id, m)
        if (m != null) shredRowMask(id)
        db.notes().deleteById(id)
        com.nota.notes.reminder.ReminderScheduler.cancel(appCtx, id)
        // الروابط تُمسح بالتتابع تلقائيًا — والوسوم اليتيمة تُنظَّف هنا حتى لا تتراكم
        runCatching { db.tags().deleteOrphans() }
        db.openHelper.writableDatabase.execSQL("VACUUM")
    }
    suspend fun deletePermanently(id: Long) = withContext(Dispatchers.IO) { hardDelete(id) }
    // إفراغ السلة: حذف نهائي لكل المحذوف نرمًا — بتأكيد مزدوج من الواجهة
    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val db = holder.get()
        holder.dao().trashedIds(acct()).forEach { hardDelete(it) }
        db.openHelper.writableDatabase.execSQL("VACUUM")
    }

    // إجراءات جماعية على المحدد: VACUUM واحد بعد كل الحذف — والأرشفة تتخطى المؤرشف أصلًا
    suspend fun archiveMany(ids: Set<Long>) = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        val now = System.currentTimeMillis()
        ids.forEach { dao.byId(it)?.let { n -> if (n.archivedAt == null) dao.update(n.copy(archivedAt = now)) } }
    }
    suspend fun unarchiveMany(ids: Set<Long>) = withContext(Dispatchers.IO) {
        val dao = holder.dao()
        ids.forEach { dao.byId(it)?.let { n -> if (n.archivedAt != null) dao.update(n.copy(archivedAt = null)) } }
    }
    // الجماعي من القوائم = نقل نرم — ومن السلة = نهائي (27 سبتمبر 2026)
    suspend fun deleteMany(ids: Set<Long>) = trashMany(ids)
    suspend fun deleteManyPermanently(ids: Set<Long>) = withContext(Dispatchers.IO) {
        val db = holder.get()
        ids.forEach { hardDelete(it) }
        db.openHelper.writableDatabase.execSQL("VACUUM")
    }
}
