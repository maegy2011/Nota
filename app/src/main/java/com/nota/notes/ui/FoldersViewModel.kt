package com.nota.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nota.notes.billing.EntitlementsRepo
import com.nota.notes.data.Folder
import com.nota.notes.data.FoldersRepository
import com.nota.notes.data.Note
import com.nota.notes.data.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// المجلدات (ميزة مدفوعة) — عمليات CRUD + لون + نقل + عدّادات + مجلد افتراضي (IO داخل المستودع، بلا runBlocking)
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repo: FoldersRepository,
    private val settings: SettingsStore,
    // المرحلة 1ج: فحص Pro داخل الـ VM (defense in depth) — من نفس مصادر premiumVm.isPro().
    // بلا حقن PremiumViewModel عمدًا: نسخة VM منفصلة ستملك trialActive=false دائمًا فتكسر التجربة.
    private val entitlements: EntitlementsRepo
) : ViewModel() {
    // فصل الحسابات: المجلدات والعدادات تُعاد مع تغيّر الحساب — كالملاحظات تمامًا
    private val acctTick = repo.accountChanges()
    // المثبتة أولًا ثم الأقدم إنشاءً — داخل كل مجموعة (أي عدد يُثبت معًا)
    val folders: Flow<List<Folder>> = acctTick.flatMapLatest { repo.all() }.map { list ->
        list.sortedWith(compareByDescending<Folder> { it.pinned }.thenBy { it.createdAt })
    }

    // عداد الملاحظات لكل مجلد — خريطة id→count لشارات البطاقات
    val counts: StateFlow<Map<Long, Int>> = acctTick.flatMapLatest { repo.counts() }
        .map { list -> list.associate { it.folderId to it.count } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // إحصائية كل مجلد — خريطة id→صف الإحصائية لبطاقات المجلدات (27 سبتمبر 2026)
    val stats: StateFlow<Map<Long, com.nota.notes.data.FolderStatsRow>> = acctTick.flatMapLatest { repo.stats() }
        .map { list -> list.associate { it.folderId to it } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // المجلد الافتراضي للملاحظات الجديدة — null = القائمة الرئيسية
    val defaultFolderId: StateFlow<Long?> = settings.defaultFolderId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun notesIn(folderId: Long): Flow<List<Note>> = repo.notesIn(folderId)

    suspend fun create(name: String, color: Int = 0): Long = repo.create(name, color)

    fun rename(id: Long, name: String) {
        viewModelScope.launch { runCatching { repo.rename(id, name) } }
    }

    fun setColor(id: Long, color: Int) {
        viewModelScope.launch { runCatching { repo.setColor(id, color) } }
    }

    // حفظ الاسم واللون معًا — كتابة ذرية واحدة بلا سباق
    fun saveNameAndColor(id: Long, name: String, color: Int) {
        viewModelScope.launch { runCatching { repo.renameWithColor(id, name, color) } }
    }

    fun setDefault(id: Long?) {
        viewModelScope.launch { runCatching { settings.setDefaultFolderId(id) } }
    }
    fun togglePin(id: Long) {
        viewModelScope.launch { runCatching { repo.togglePin(id) } }
    }
    // القفل الخاص للمجلد — يقلب الحالة + ينسى فتح الجلسة إن أُقفل من جديد
    fun toggleLock(id: Long) {
        viewModelScope.launch {
            runCatching { repo.toggleLock(id) }
            _unlockedIds.value = _unlockedIds.value - id
        }
    }
    // مجلدات فُتحت بتحدٍ ناجح هذه الجلسة — القفل التلقائي عند الخلفية ينساها كلها
    private val _unlockedIds = kotlinx.coroutines.flow.MutableStateFlow(setOf<Long>())
    val unlockedIds: StateFlow<Set<Long>> = _unlockedIds
    fun markUnlocked(id: Long) { _unlockedIds.value = _unlockedIds.value + id }
    fun relockAll() { _unlockedIds.value = emptySet() }

    // المرحلة 1ج: نسخة متزامنة-المصادر من premiumVm.isPro() — الـ UI يمنع أصلًا (proLock).
    private suspend fun proNow(): Boolean {
        if (com.nota.notes.data.SettingsStore.isTrialActive(settings.trialStart.first())) return true
        return entitlements.cached.first().pro
    }

    fun moveNote(noteId: Long, folderId: Long?) {
        // المرحلة 1ج: النقل للمجلدات مدفوع — فحص داخل الـ VM لا في الـ UI فقط.
        viewModelScope.launch { if (!proNow()) return@launch; runCatching { repo.moveNote(noteId, folderId) } }
    }

    // حذف المجلد يمسح الافتراضي إن كان هو — بلا id معلق
    suspend fun delete(id: Long) {
        repo.delete(id)
        if (defaultFolderId.value == id) runCatching { settings.setDefaultFolderId(null) }
    }
}
