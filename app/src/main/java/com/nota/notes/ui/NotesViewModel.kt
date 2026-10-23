package com.nota.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nota.notes.billing.EntitlementsRepo
import com.nota.notes.data.Note
import com.nota.notes.data.NotesRepository
import com.nota.notes.data.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repo: NotesRepository,
    private val store: SettingsStore,
    // المرحلة 1ج: فحص Pro داخل الـ VM (defense in depth) — من نفس مصادر premiumVm.isPro().
    // بلا حقن PremiumViewModel عمدًا: نسخة VM منفصلة ستملك trialActive=false دائمًا فتكسر التجربة.
    private val entitlements: EntitlementsRepo
) : ViewModel() {
    val query = MutableStateFlow("")
    // فلترة حسب اللون — حالة جلسة فقط (لا تُحفظ)، null = الكل
    val colorFilter = MutableStateFlow<Int?>(null)

    // التصنيف: تسمية واحدة حرة — الفلترة في الذاكرة كالوسم (بلا استعلام جديد)
    val categoryFilter = MutableStateFlow<String?>(null)
    fun setCategoryFilter(cat: String?) { categoryFilter.value = cat }
    // فلترة حسب الوسم — حالة جلسة فقط، null = الكل
    val tagFilter = MutableStateFlow<String?>(null)
    fun setTagFilter(tag: String?) { tagFilter.value = tag }
    // دمج فلتر الوسم والتصنيف في تدفق واحد — نسخة الكوروتين بلا combine سداسي
    private val tagAndCat = combine(tagFilter, categoryFilter) { t, c -> t to c }

    // P1-A (أكتوبر 2026): نبضة الجلسة — onUnlocked() يعيد تشغيل كل التدفقات بعد إعادة فتح DB.
    // (إغلاق DB أثناء القفل يُميت التدفقات المجموعة؛ بدون نبضة تتجمد القوائم على آخر قيمة.)
    private val _sessionTick = MutableStateFlow(0)
    // فصل الحسابات: كل القوائم تُعاد مع تغيّر الحساب — فالخروج/الدخول يبدّل المعروض فورًا بلا تسرب
    private val acctTick = combine(repo.accountChanges(), _sessionTick) { a, _ -> a }

    // P1-A: تُنادى عند القفل — تمسح نصوص البحث والفلاتر من الذاكرة (القوائم نفسها يعاد تحميلها عند الفتح).
    fun clearSensitiveData() {
        query.value = ""
        archiveQuery.value = ""
        favoriteQuery.value = ""
        trashQuery.value = ""
        colorFilter.value = null
        tagFilter.value = null
        categoryFilter.value = null
    }

    // P1-A: تُنادى بعد فتح القفل — تعيد تشغيل التدفقات ضد اتصال DB الجديد.
    fun onUnlocked() { _sessionTick.value = _sessionTick.value + 1 }

    // P1-A fix 4 (أكتوبر 2026): debounce المحرر 300ms في App.kt (لا هنا) — والقفل يُخرج المحرر
    // من التركيب فيلغي الـ autosave المعلق → آخر ≤300ms تضيع. الحل: خطاف تفريغ صريح يُستدعى
    // قبل المسح/القفل. التسجيل من App.kt عبر SideEffect (يُحدَّث كل recomposition فيحمل أحدث حالة).
    // النطاق عمدًا: saveNow واحد (كتابة Room واحدة ≈ عشرات ms < 200ms) — الوسوم/التذكير/السياج
    // لحظية التغيير للملاحظات المحفوظة، والمعلقة للجديدة تُحفظ مع أول حفظ بعد الفتح (الحالة تبقى في rememberSaveable).
    @Volatile private var pendingDraftSaver: (suspend () -> Unit)? = null
    fun setPendingDraftSaver(saver: (suspend () -> Unit)?) { pendingDraftSaver = saver }
    // ترجع true عند حفظ فعلي، false عند غياب مسودة متسخة أو فشل — لا ترمي أبدًا (القفل لا يتعطل).
    suspend fun flushPendingSave(): Boolean = try {
        val saver = pendingDraftSaver ?: return false
        saver()
        pendingDraftSaver = null
        true
    } catch (e: Exception) { false }
    // الوسوم: كل الأسماء لصف الفلترة + خريطة noteId ← أسماء لعرضها أسفل البطاقة
    val allTags: StateFlow<List<String>> =
        acctTick.flatMapLatest { repo.allTags() }.map { list -> list.map { it.name } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tagsMap: StateFlow<Map<Long, List<String>>> =
        acctTick.flatMapLatest { repo.tagsMap() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // خيار تضمين الأرشيف في بحث الرئيسية — مطفأ افتراضيًا، وبلا استعلام لا أثر له
    val includeArchive = MutableStateFlow(false)

    // القاعدة ترجع الأحدث تعديلًا أولًا؛ أي ترتيب آخر يُطبَّق هنا في الذاكرة —
    // قائمة الملاحظات الشخصية بالمئات فقط، والفرز هنا أبسط وأأمن من 10 استعلامات مكررة
    private val raw = combine(query.debounce(200), includeArchive, acctTick) { q, inc, _ -> q to inc }
        .flatMapLatest { (q, inc) ->
            if (q.isBlank()) repo.all()
            else if (!inc) repo.search(q.trim())
            else combine(repo.search(q.trim()), repo.searchArchived(q.trim())) { a, b -> a + b }
        }

    val notes: StateFlow<List<Note>> =
        combine(raw, store.sortOrder, colorFilter, tagAndCat, tagsMap) { list, sort, color, tc, tags ->
            val (tag, cat) = tc
            val filtered = list.filter { n ->
                (color == null || n.color == color) && (tag == null || tags[n.id]?.contains(tag) == true) && (cat == null || n.category == cat)
            }
            sortNotes(filtered, sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // التمرير اللانهائي: LazyColumn يركّب الصفوف المرئية فقط ويعيد تدويرها —
    // المثبتة بالدبوس أولًا دائمًا — ثم الترتيب المختار داخل كل مجموعة
    private fun sortNotes(list: List<Note>, sort: String): List<Note> {
        val pinned = list.filter { it.pinned }
        val rest = list.filter { !it.pinned }
        val bySort: (List<Note>) -> List<Note> = { l -> when (sort) {
        SettingsStore.SORT_UPDATED_ASC -> l.sortedBy { it.updatedAt }
        SettingsStore.SORT_CREATED_DESC -> l.sortedByDescending { it.createdAt }
        SettingsStore.SORT_CREATED_ASC -> l.sortedBy { it.createdAt }
        SettingsStore.SORT_TITLE_ASC -> l.sortedBy { it.title.lowercase() }
        else -> l // SORT_UPDATED_DESC — ترتيب القاعدة الأصلي
        } }
        return bySort(pinned) + bySort(rest)
    }

    fun setColorFilter(color: Int?) { colorFilter.value = color }

    // الأرشيف: بحث مستقل + نفس الترتيب والفلترة اللونية
    val archiveQuery = MutableStateFlow("")
    private val archivedRaw = combine(archiveQuery.debounce(200), acctTick) { q, _ -> q }.flatMapLatest { q ->
        if (q.isBlank()) repo.archived() else repo.searchArchived(q.trim())
    }
    val archived: StateFlow<List<Note>> =
        combine(archivedRaw, store.sortOrder, colorFilter, tagAndCat, tagsMap) { list, sort, color, tc, tags ->
            val (tag, cat) = tc
            val filtered = list.filter { n ->
                (color == null || n.color == color) && (tag == null || tags[n.id]?.contains(tag) == true) && (cat == null || n.category == cat)
            }
            sortNotes(filtered, sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun archive(id: Long) = viewModelScope.launch { archiveNow(id) }
    internal suspend fun archiveNow(id: Long) { repo.archive(id) }
    fun unarchive(id: Long) = viewModelScope.launch { repo.unarchive(id) }

    // النسخ الاحتياطي (v3): الملاحظات + المجلدات + بطاقة النسخة — التشفير في BackupManager
    suspend fun buildBackupJson(): String =
        com.nota.notes.data.BackupCodec.encode(repo.backupNotes(), repo.backupFolders(), repo.appVersion())

    // معاينة النسخة قبل الاستعادة — العدد والتاريخ بلا فك كامل
    fun previewBackupJson(json: String): com.nota.notes.data.BackupMeta =
        com.nota.notes.data.BackupCodec.preview(json)

    // تحذير القدم (27 سبتمبر 2026): النسخة أقدم من أحدث تعديل محلي = دمجها قد يعيد محذوفات
    suspend fun isPreviewStale(meta: com.nota.notes.data.BackupMeta): Boolean =
        meta.createdAt > 0 && meta.createdAt < repo.newestUpdate()

    // P3-3ج: ملاحظات المعاينة للاختيار الجزئي (تُستخدم في حوار المعاينة قبل الدمج).
    fun previewNotes(json: String): List<com.nota.notes.data.BackupNote> =
        runCatching { com.nota.notes.data.BackupCodec.decode(json) }.getOrDefault(emptyList())

    // الاستعادة بالدمج — نسخة أمان أولًا ثم دمج ثم تسجيل في سجل الاستعادة (قابل للتراجع)
    // P3-3ب: التعارضات تُسجَّل مع السجل (احتفظ بالمحلي + سجّل) وتُحل لاحقًا من بطاقة السجل.
    // P3-3ج: selectedIds = null تعني الكل؛ غيرها تُستعاد المحددة فقط.
    suspend fun restoreBackupJson(json: String, source: String = "file", selectedIds: Set<String>? = null): com.nota.notes.data.NotesRepository.RestoreResult {
        repo.safetySnapshot()
        val r = repo.restoreMerge(
            com.nota.notes.data.BackupCodec.decode(json),
            com.nota.notes.data.BackupCodec.foldersOf(json),
            selectedIds
        )
        runCatching {
            repo.logRestore(com.nota.notes.data.RestoreRecord(src = source, added = r.added, skipped = r.skipped, addedIds = r.addedIds, conflicts = r.conflicts))
        }
        return r
    }

    // P3-3ب: حل تعارض واحد — يعيد true عند النجاح (ويُزيله من السجل).
    suspend fun resolveTakeRemote(recordId: Long, conflictId: String): Boolean {
        val rec = repo.restoreLogOnce().find { it.id == recordId } ?: return false
        val c = rec.conflicts.find { it.id == conflictId } ?: return false
        if (!repo.takeRemote(c.remote)) return false
        runCatching { repo.replaceRestore(rec.copy(conflicts = rec.conflicts.filterNot { it.id == conflictId })) }
        return true
    }
    suspend fun resolveKeepBoth(recordId: Long, conflictId: String): Boolean {
        val rec = repo.restoreLogOnce().find { it.id == recordId } ?: return false
        val c = rec.conflicts.find { it.id == conflictId } ?: return false
        runCatching { repo.keepBoth(c.remote) }.getOrNull() ?: return false
        runCatching { repo.replaceRestore(rec.copy(conflicts = rec.conflicts.filterNot { it.id == conflictId })) }
        return true
    }
    // "احتفظ بالمحلية" = إسقاط التعارض من السجل (المحلي محفوظ أصلًا — لا تغيير بيانات).
    suspend fun resolveKeepLocal(recordId: Long, conflictId: String): Boolean {
        val rec = repo.restoreLogOnce().find { it.id == recordId } ?: return false
        if (rec.conflicts.none { it.id == conflictId }) return false
        runCatching { repo.replaceRestore(rec.copy(conflicts = rec.conflicts.filterNot { it.id == conflictId })) }
        return true
    }
    // سجل الاستعادة + التراجع بالنقل للسلة (27 سبتمبر 2026)
    val restoreLog = repo.restoreLog()
    // P3-3د: لقطة السجل لاختيار أحدث سجل بعد الاستعادة (snackbar التراجع).
    suspend fun restoreLogOnce() = repo.restoreLogOnce()
    suspend fun undoRestore(id: Long): Int {
        val rec = repo.restoreLogOnce().find { it.id == id } ?: return 0
        if (rec.undone || rec.addedIds.isEmpty()) return 0
        val n = repo.undoRestore(rec.addedIds)
        runCatching { repo.markRestoreUndone(id) }
        return n
    }

    fun save(id: Long?, title: String, body: String, color: Int = 0, folderId: Long? = null, background: String? = null, lat: Double? = null, lng: Double? = null, category: String? = null) = viewModelScope.launch {
        repo.save(id, title, body, color, folderId, background, lat, lng, category)
    }
    // نسخة معلقة ترجع المعرف — للحفظ التلقائي (الجديدة تُفتح بنفس الجلسة بلا تكرار)
    suspend fun saveNow(id: Long?, title: String, body: String, color: Int = 0, folderId: Long? = null, background: String? = null, lat: Double? = null, lng: Double? = null, category: String? = null): Long =
        repo.save(id, title, body, color, folderId, background, lat, lng, category)
    // المرحلة 1ج: نسخة متزامنة-المصادر من premiumVm.isPro() — الـ UI يمنع أصلًا (proLock)،
    // وهذا يحمي من السباقات والاستدعاءات المباشرة. الرفض صامت هنا عمدًا: التغذية الراجعة في الـ UI.
    internal suspend fun proNow(): Boolean {
        if (com.nota.notes.data.SettingsStore.isTrialActive(store.trialStart.first())) return true
        return entitlements.cached.first().pro
    }

    fun togglePin(id: Long) = viewModelScope.launch {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
// Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return@launch; runCatching { repo.togglePin(id) } }
    fun toggleLock(id: Long) = viewModelScope.launch {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
// Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return@launch; runCatching { repo.toggleLock(id) } }
    // المفضلة: بحث مستقل + نفس الترتيب والفلترة اللونية — القلب يقلب الحالة
    val favoriteQuery = MutableStateFlow("")
    private val favoritesRaw = combine(favoriteQuery.debounce(200), acctTick) { q, _ -> q }.flatMapLatest { q ->
        if (q.isBlank()) repo.favorites() else repo.searchFavorites(q.trim())
    }
    val favorites: StateFlow<List<Note>> =
        combine(favoritesRaw, store.sortOrder, colorFilter, tagAndCat, tagsMap) { list, sort, color, tc, tags ->
            val (tag, cat) = tc
            val filtered = list.filter { n ->
                (color == null || n.color == color) && (tag == null || tags[n.id]?.contains(tag) == true) && (cat == null || n.category == cat)
            }
            sortNotes(filtered, sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // المفضلة المؤرشفة لاحقًا — نفس البحث والترتيب والفلترة كالتبويب النشط
    private val favoritesArchivedRaw = combine(favoriteQuery.debounce(200), acctTick) { q, _ -> q }.flatMapLatest { q ->
        if (q.isBlank()) repo.favoritesArchived() else repo.searchFavoritesArchived(q.trim())
    }
    val favoritesArchived: StateFlow<List<Note>> =
        combine(favoritesArchivedRaw, store.sortOrder, colorFilter, tagAndCat, tagsMap) { list, sort, color, tc, tags ->
            val (tag, cat) = tc
            val filtered = list.filter { n ->
                (color == null || n.color == color) && (tag == null || tags[n.id]?.contains(tag) == true) && (cat == null || n.category == cat)
            }
            sortNotes(filtered, sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun toggleFavorite(id: Long) = viewModelScope.launch {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
// Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return@launch; runCatching { repo.toggleFavorite(id) } }
    fun unfavoriteMany(ids: Set<Long>) = viewModelScope.launch { repo.unfavoriteMany(ids) }
    // تكرار ملاحظة — يرجع معرف النسخة (تُفتح بعده مباشرة من App)
    // المرحلة 1ج: بلا Pro يُرجع -1 (المنادي duplicateAndOpen يعالجها بفرع else الآمن) — بلا رمي عمدًا.
    suspend fun duplicateNow(id: Long, newTitle: String): Long {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
// Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return -1L
        return repo.duplicate(id, newTitle)
    }
    // التذكير — حفظ/مسح + إعادة جدولة الكل (إقلاع/استعادة)
    fun setReminder(id: Long, spec: com.nota.notes.data.ReminderSpec) = viewModelScope.launch { setReminderNow(id, spec) }
    internal suspend fun setReminderNow(id: Long, spec: com.nota.notes.data.ReminderSpec) {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
        // Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return
        runCatching { repo.setReminder(id, spec) }
    }
    fun setReminder(id: Long, at: Long, repeat: String, beforeMin: Long) = setReminder(id, com.nota.notes.data.ReminderSpec(at = at, repeat = repeat, beforeMin = beforeMin))
    fun clearReminder(id: Long) = viewModelScope.launch {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
        // Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return@launch
        runCatching { repo.clearReminder(id) }
    }
    fun rescheduleReminders() = viewModelScope.launch { runCatching { repo.rescheduleReminders() } }
    // إدارة الوسوم + شاشة التنبيهات — العدّادات والمواعيد تتدفق مباشرة من القاعدة
    val tagCounts: StateFlow<Map<String, Int>> =
        acctTick.flatMapLatest { repo.tagCounts() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val reminders: StateFlow<List<Note>> =
        acctTick.flatMapLatest { repo.reminders() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun renameTag(old: String, newName: String) = viewModelScope.launch { runCatching { repo.renameTag(old, newName) } }

    // التصنيفات: العدّادات والفلترة والإدارة تتدفق مباشرة من القاعدة — مجانية للجميع
    val categories: StateFlow<List<String>> =
        acctTick.flatMapLatest { repo.categories() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categoryCounts: StateFlow<Map<String, Int>> =
        acctTick.flatMapLatest { repo.categoryCounts() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    fun setCategory(id: Long, category: String?) = viewModelScope.launch { setCategoryNow(id, category) }
    internal suspend fun setCategoryNow(id: Long, category: String?) {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
        // Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return
        runCatching { repo.setCategory(id, category) }
    }
    fun renameCategory(old: String, newName: String) = viewModelScope.launch { runCatching { repo.renameCategory(old, newName) } }
    fun clearCategory(name: String) = viewModelScope.launch { runCatching { repo.clearCategory(name) } }
    fun deleteTag(name: String) = viewModelScope.launch { runCatching { repo.deleteTag(name) } }
    fun createTag(name: String) = viewModelScope.launch { runCatching { repo.createTag(name) } }
    // وسوم ملاحظة واحدة — تُحمَّل عند الفتح وتُحفظ مع كل حفظ
    suspend fun loadTags(id: Long): List<String> = runCatching { repo.tagsOf(id) }.getOrDefault(emptyList())
    fun saveTags(id: Long, tags: List<String>) = viewModelScope.launch { saveTagsNow(id, tags) }
    internal suspend fun saveTagsNow(id: Long, tags: List<String>) {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
        // Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return
        runCatching { repo.setTags(id, tags) }
    }
    // محتوى الملاحظة: مهام + مرفقات — تحميل/إضافة/قلب/حذف (الفشل صامت كالوسوم)
    suspend fun loadTasks(id: Long): List<com.nota.notes.data.ChecklistItem> = runCatching { repo.tasksOf(id) }.getOrDefault(emptyList())
    suspend fun loadAttachments(id: Long): List<com.nota.notes.data.NoteAttachment> = runCatching { repo.attachmentsOf(id) }.getOrDefault(emptyList())
    fun addTask(id: Long, text: String, onDone: (Long) -> Unit = {}) = viewModelScope.launch { addTaskNow(id, text, onDone) }
    internal suspend fun addTaskNow(id: Long, text: String, onDone: (Long) -> Unit = {}) {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
        // Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return
        runCatching { repo.addTask(id, text) }.onSuccess { if (it > 0) onDone(it) }
    }
    fun toggleTask(item: com.nota.notes.data.ChecklistItem, onDone: () -> Unit = {}) = viewModelScope.launch {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
        // Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return@launch
        runCatching { repo.toggleTask(item) }.onSuccess { onDone() }
    }
    fun renameTask(item: com.nota.notes.data.ChecklistItem, text: String, onDone: () -> Unit = {}) = viewModelScope.launch {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
        // Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return@launch
        runCatching { repo.renameTask(item, text) }.onSuccess { onDone() }
    }
    fun deleteTask(id: Long, onDone: () -> Unit = {}) = viewModelScope.launch {
        // Pro gating (defense-in-depth) — UI also gates via showProLocks.
        // Direct VM callers (widgets, deep links, future features) bypass UI gating.
        if (!proNow()) return@launch
        runCatching { repo.deleteTask(id) }.onSuccess { onDone() }
    }
    fun deleteAttachment(att: com.nota.notes.data.NoteAttachment, onDone: () -> Unit = {}) = viewModelScope.launch {
        runCatching { repo.deleteAttachment(att) }.onSuccess { onDone() }
    }
    // نسخة معلقة ترجع المعرف — لإرفاق الصورة/الصوت/الرسم فور جهوزيته
    suspend fun addAttachmentNow(id: Long, file: String, kind: String = com.nota.notes.data.NoteAttachment.KIND_IMAGE): Long =
        repo.addAttachment(id, file, kind)
    // عناوين الأحدث لكاش الويدجت — تُستدعى بعد فتح القفل فقط
    suspend fun widgetTitles(): List<String> = repo.latestTitles()
    // حالة الحذف: تُعرض في الواجهة حتى تمامه (مهم مع الإتلاف متعدد التمريرات — قد يطول)
    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting
    fun delete(id: Long) = viewModelScope.launch {
        _deleting.value = true
        try { repo.delete(id) } finally { _deleting.value = false }
    }
    fun archiveMany(ids: Set<Long>) = viewModelScope.launch { repo.archiveMany(ids) }
    fun unarchiveMany(ids: Set<Long>) = viewModelScope.launch { repo.unarchiveMany(ids) }
    fun deleteMany(ids: Set<Long>) = viewModelScope.launch {
        _deleting.value = true
        try { repo.deleteMany(ids) } finally { _deleting.value = false }
    }
    // السلة (27 سبتمبر 2026): قائمة + بحث + استعادة + حذف نهائي + تطهير المنتهية عند الفتح
    val trashQuery = MutableStateFlow("")
    val trashed: StateFlow<List<Note>> =
        combine(trashQuery.debounce(200), acctTick) { q, _ -> q }.flatMapLatest { q ->
            if (q.isBlank()) repo.trashed() else repo.searchTrash(q.trim())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun restoreOne(id: Long) = viewModelScope.launch { repo.restoreOne(id) }
    fun restoreMany(ids: Set<Long>) = viewModelScope.launch { repo.restoreMany(ids) }
    fun deletePermanently(id: Long) = viewModelScope.launch {
        _deleting.value = true
        try { repo.deletePermanently(id) } finally { _deleting.value = false }
    }
    fun deleteManyPermanently(ids: Set<Long>) = viewModelScope.launch {
        _deleting.value = true
        try { repo.deleteManyPermanently(ids) } finally { _deleting.value = false }
    }
    fun purgeTrash() = viewModelScope.launch { runCatching { repo.purgeTrash() } }
    fun setGeofence(id: Long, lat: Double, lng: Double, radiusM: Float, name: String) =
        viewModelScope.launch { runCatching { repo.setGeofence(id, lat, lng, radiusM, name) } }
    fun clearGeofence(id: Long) = viewModelScope.launch { runCatching { repo.clearGeofence(id) } }
    fun emptyTrash() = viewModelScope.launch {
        _deleting.value = true
        try { repo.emptyTrash() } finally { _deleting.value = false }
    }
}
