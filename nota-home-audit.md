# Jard Home Screen — NotesListScreen (nota-home-audit)

Generated: 2026-10-22. Source of truth = code. Compared against the descriptive document previously sent.

## 0. الإجابات على الأسئلة السبعة الحرجة

### س-1 — BottomAppBar: كم زرًا فعليًا؟
- `NotesListScreen.kt` lines 339-362:
```kotlin
if (selectionMode) {
    BottomAppBar {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isFavorite) {  // زر 1 (شرطي)
                Button(onClick = { val ids = sel; selected = emptySet(); onUnfavoriteMany(ids) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.fav_remove))
                }
            }
            Button(onClick = { val ids = sel; selected = emptySet(); if (isArchive) onUnarchiveMany(ids) else pendingBatchArchive = ids }, modifier = Modifier.weight(1f)) {
                Text(stringResource(if (isArchive) R.string.menu_unarchive else R.string.menu_archive))
            }
            Button(onClick = { pendingBatchDelete = sel }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.delete_cd))
            }
        }
    }
}
```
- **LIST / ARCHIVE**: 2 أزرار (أرشفة/استعادة + حذف). **FAVORITES**: 3 أزرار (إزالة من المفضلة + أرشفة/استعادة + حذف). "الزر الثالث" هو حذف (delete) — الوصف ذكر "ثلاثة أزرار" لكن literal 2-3 حسب السياق.

### س-2 — حاجب "جاري الحذف": متى يظهر؟
- `NotesListScreen.kt` lines 875-895:
```kotlin
if (deleting) {
    AlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.delete_title)) }, text = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(stringResource(R.string.delete_progress), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp))
        }
    }, confirmButton = {})
}
```
- يُفعَّل بـ `deleting: Boolean` (param من `NotesViewModel.deleting`). يُضبط true في: `delete()`، `deleteMany()`، `deletePermanently()`، `deleteManyPermanently()`، `emptyTrash()`. → يظهر في **الحذف الناعم (نقل للسلة)** وفي **الحذف النهائي** — كلاهما.

### س-3 — تبويبات المفضلة: ما شرط الظهور؟
- `NotesListScreen.kt` lines 193-194, 200-201:
```kotlin
val hasArchiveTab = isFavorite && archivedTabNotes != null
var favTab by remember(hasArchiveTab) { mutableStateOf(0) }
val shown: List<Note> = if (hasArchiveTab && favTab == 1) archivedTabNotes!! else notes
```
- TabRow (lines 395-401) يُعرض فقط عندما `hasArchiveTab` true. "أرشيف لاحق" = `archivedTabNotes != null` (تمرير من App.kt). التبويبان: 0 = النشطة (notes)، 1 = المؤرشفة (archivedTabNotes).

### س-4 — FAB: 20dp أم 56dp؟
- `NotesListScreen.kt` lines 377-384:
```kotlin
FloatingActionButton(onClick = { Haptics.confirm(hctx); onAdd() }, shape = RoundedCornerShape(20.dp), containerColor = MaterialTheme.colorScheme.primary) {
    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_note_cd))
}
```
- **20dp = corner radius (shape)**، ليس حجم الزر (لا `Modifier.size`). حجم FAB الافتراضي (56dp في Material3). "20dp" في الوصف أشار إلى زاوية، لا حجم.

### س-5 — قائمة ⋮: ما الخيارات الفعلية؟
- `NotesListScreen.kt` lines 722-778 — 9 بنود (مع شروط):
1. `menu_deselect`/`menu_select` (toggle)
2. `move_to_folder` — `if (!isArchive)` + showProLocks lock
3. `fav_remove`/`fav_add` — `if (!isArchive || n.favorite)` + showProLocks lock
4. `unlock_menu`/`lock_menu` — showProLocks lock
5. `unpin_menu`/`pin_menu` — showProLocks lock
6. `menu_unarchive` (if isArchive) / `menu_archive` (else) — بدون lock
7. `duplicate_menu` — `if (!isArchive && !isFavorite)` + showProLocks lock
8. `copy_note` — showProLocks lock
9. `delete_cd` — بدون lock (لون error)
- **لا يوجد "مشاركة" (Share) ولا "تصدير" (Export/PDF/MD/HTML)**. الوصف ذكر 9 خيارات — المطابق.

### س-6 — الملاحظة المقفلة: كيف تظهر في القائمة؟
- `NotesListScreen.kt` lines 532, 554-558:
```kotlin
if (n.pinned || n.locked || n.background != null || n.favorite || n.archivedAt != null) {
    ...
    if (n.locked) {
        Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.locked_cd), tint = cardOn, modifier = Modifier.size(14.dp))
    }
}
```
- **شارة Lock 14dp فقط** في صف الشارات. العنوان والنص يُعرضان **بشكل طبيعي** (لا إخفاء). لا تأثير بصرى آخر.

### س-7 — منتقي المفضلة: من هم "المرشحون"؟
- `App.kt` line 1492: `favCandidates = notes.filter { !it.favorite && it.archivedAt == null }`
- `NotesListScreen.kt` lines 793-799:
```kotlin
if (pickFavOpen && isFavorite) {
    NotePickDialog(title = stringResource(R.string.fav_add_title), notes = favCandidates, emptyText = stringResource(R.string.mgr_empty_candidates), onPick = { id -> pickFavOpen = false; onToggleFavorite(id) }, onDismiss = { pickFavOpen = false })
}
```
- المرشحون = ملاحظات `!favorite && archivedAt == null` من تدفق `notes` (القائمة المعروضة). الترتيب = sortNotes (pinned أولاً + sortOrder). لا فلترة إضافية.

---

## القسم 1 — البنية والملفات

### س1.1 — الملفات
- `app/src/main/java/com/nota/notes/ui/NotesListScreen.kt` — 1065 سطر — شاشة القائمة (Home/Archive/Favorites).
- `app/src/main/java/com/nota/notes/ui/NotesViewModel.kt` — 341 سطر — ViewModel.
- `app/src/main/java/com/nota/notes/data/NotesRepository.kt` — 591 سطر — Repository.
- `app/src/main/java/com/nota/notes/data/NoteDao.kt` — 199 سطر — DAO.
- لا يوجد `NoteCard.kt` منفصل — البطاقة inline داخل NotesListScreen (lines 510-670).
- لا يوجد `ArchiveScreen.kt`/`FavoritesScreen.kt` منفصل — نفس Composable مع params `isArchive`/`isFavorite`.

### س1.2 — العلاقة مع App.kt
- `App.kt` line 78 imports `NotesListScreen`. استدعاؤها في 3 فروع (lines 1447, 1488, 1525): `screen == Screen.ARCHIVE`, `Screen.FAVORITES`, `else` (LIST). كلها داخل `DrawerShell(...) { open -> NotesListScreen(...) }`.

### س1.3 — Variants
- **نفس Composable** للثلاثة (LIST/ARCHIVE/FAVORITES) — params: `isArchive`, `isFavorite`, `archivedTabNotes`, `favCandidates`, `onBack`, `showFab`, `showAccount`, `showProLocks`. الفروق: FAB يُخفى في ARCHIVE/FAVORITES؛ Back arrow في ARCHIVE/FAVORITES؛ favCandidates فقط في FAVORITES.

---

## القسم 2 — TopAppBar والحالات

### س2.1 — الحالات (3)
- `NotesListScreen.kt` lines 226-255:
```kotlin
title = {
    if (selectionMode) { Text(stringResource(R.string.selected_count, sel.size), style = MaterialTheme.typography.titleLarge) }
    else if (searchOpen) { OutlinedTextField(value = query, onValueChange = onQuery, ..., modifier = Modifier.fillMaxWidth().height(52.dp).focusRequester(searchFocus)) ... }
    else { Text(stringResource(when { isArchive -> R.string.archive_title; isFavorite -> R.string.favorites_title; else -> R.string.app_name }), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}
```

### س2.2 — NavigationIcon
- lines 270-282: selectionMode → Close (clear); isArchive/isFavorite → ArrowBack (onBack); else → Menu (onNav).

### س2.3 — Actions
- selectionMode: `TextButton` "تحديد الكل" (lines 285-288).
- searchOpen: fav_add (if isFavorite), sort, filter (lines 290-310).
- else: search, sort, filter (lines 312-332). ترتيب: Search, Sort (List icon), Filter (Tune).

### س2.4 — BackHandler
- **لا يوجد BackHandler في NotesListScreen.kt** (grep returned empty). Back يُدار في App.kt (lines 1188-1203) على مستوى AuthedApp. داخل NotesListScreen، Back بُدّل بـ `onBack` callback (ArrowBack icon).

---

## القسم 3 — البحث (Search)

### س3.1 — UI الحقل
- `OutlinedTextField` (lines 236-248): shape `RoundedCornerShape(20.dp)`, height 52dp, leadingIcon Search, trailingIcon Close (مسح/إغلاق), `singleLine = true`, `ImeAction.Search`, `KeyboardActions(onSearch = { keyboard?.hide() })`.

### س3.2 — التركيز التلقائي
- lines 217, 249: `val searchFocus = remember { FocusRequester() }` + `LaunchedEffect(Unit) { searchFocus.requestFocus(); keyboard?.show() }` (داخل searchOpen branch). يُفرض التركيز دائماً عند فتح البحث. لا خيار تعطيل.

### س3.3 — Backend
- `NotesViewModel.kt` lines 78-84:
```kotlin
private val raw = combine(query.debounce(200), includeArchive, acctTick) { q, inc, _ -> q to inc }.flatMapLatest { (q, inc) ->
    if (q.isBlank()) repo.all()
    else if (!inc) repo.search(q.trim())
    else combine(repo.search(q.trim()), repo.searchArchived(q.trim())) { a, b -> a + b }
}
```
- `NoteDao.kt` search: `WHERE ... AND (title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%' OR EXISTS (SELECT 1 FROM note_tags nt JOIN tags t ON t.id = nt.tagId WHERE nt.noteId = notes.id AND t.name LIKE '%' || :q || '%'))`. أعمدة: title, body, tags (name). لا attachments. SQL LIKE (بلا FTS).

### س3.4 — Debouncing
- `query.debounce(200)` في ViewModel (200ms). لا debounce إضافي.

### س3.5 — Highlighting
- `NotesListScreen.kt` lines 1028-1041 `searchSnippet`: window 40+40 = 80 char + query + ellipses. تعليق "نافذة ~90 حرفًا". `highlightedText` يُظلل range بخلفية primary 0.3 alpha. `MarkdownFormat.stripMarkdown` على body. lowercase() للأحرف اللاتينية (لا normalisation للعربية/diacritics).

### س3.6 — البحث في المحتوى المقفل
- **لا فحص `locked` في استعلام البحث.** الملاحظة المقفلة تُبحث وتُعرض (title + body snippet) في القائمة. `latestTitles` (DAO) يستبعد locked من cache الويدجت، لكن البحث في القائمة لا.

---

## القسم 4 — الفلاتر (Filters)

### س4.1 — الحالة
- `NotesViewModel.kt`: `colorFilter: MutableStateFlow<Int?>`, `tagFilter: MutableStateFlow<String?>`, `categoryFilter: MutableStateFlow<String?>`. جلسة فقط (لا حفظ). `tagAndCat = combine(tagFilter, categoryFilter)`.

### س4.2 — ظهور بطاقة الفلاتر
- `NotesListScreen.kt` line 390: `if (filtersOpen || hasActiveFilter || query.isNotBlank())`. hasActiveFilter = `colorFilter != null || tagFilter != null || categoryFilter != null || includeArchive`.

### س4.3 — سلوك الوسوم
- `NotesViewModel.kt` lines 88-92: `list.filter { ... (tag == null || tags[n.id]?.contains(tag) == true) ... }`. **OR (contains) — وسم واحد فقط** (tag: String? not Set). لا تفعيل متعدد.

### س4.4 — "مسح الفلاتر"
- line 408: `onColorFilter(null); onTagFilter(null); onCategoryFilter(null); onIncludeArchive(false)`. **لا يمسح query**. color/tag/category/includeArchive فقط.

### س4.5 — تضمين الأرشيف
- line 404: `if (!isArchive && (query.isNotBlank() || includeArchive))`. الـ Checkbox يظهر في LIST + search أو includeArchive.

---

## القسم 5 — العداد السياقي

### س5.1 — عدّاد النتائج
- lines 398-408: `shown.size` (filtered list). النص: `R.string.results_fmt` عند search/activeFilter، else `R.string.notes_count`. Empty → "".

### س5.2 — زر "مسح الفلاتر"
- يظهر `if (hasActiveFilter)`. onClick يمسح color/tag/category/includeArchive (لا query).

---

## القسم 6 — حالة الفراغ (Empty States)

### س6.1 — الأنواع
- `NotesListScreen.kt` lines 456-495: **نوعان** فقط (query blank vs not blank):
```kotlin
if (shown.isEmpty()) {
    Column(...) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) { Box { Icon(if (query.isBlank()) Icons.Filled.Add else Icons.Filled.Search, ...) } }
        Text(if (query.isBlank()) stringResource(R.string.empty_no_notes) else stringResource(R.string.empty_no_results))
        if (query.isBlank() && !isFavorite && !isArchive) { Button(onClick = onAdd) { ... empty_cta_add } }
        else { OutlinedButton(onClick = { onQuery("") }) { ... empty_cta_clear } }
    }
}
```
- وصف "4 أنواع" — الكود: 2 (blank/not). CTA يظهر فقط في LIST الفارغة.

---

## القسم 7 — قائمة الملاحظات (List)

### س7.1 — LazyColumn
- lines 497-500: `LazyColumn(contentPadding = PaddingValues(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(12.dp))`. items(shown, key = { it.id }, contentType = { "note" }).

### س7.2 — NoteCard
- Inline (lines 501-670). عناصر по порядку: checkbox (selection), Column: badge row (favorite/pinned/locked/background/archived), title (highlighted, 1 line, horizontalScroll), body snippet (2 lines), updatedAt (labelSmall), reminder bell+date, chips row (tags≤5 + overflow, category, folder).

### س7.3 — التفاعلات
- `combinedClickable(onClick = { if (selectionMode) toggle(n.id) else onOpen(n.id) }, onLongClick = { selected = selected + n.id })`. سطر 512.

### س7.4 — الشارات
- lines 532-568: شارات 14dp. شارة "مؤرشف" (archivedAt != null) — Text labelSmall مع حد chipEdge + background. a11y: contentDescription على الأيقونات (fav_cd, pinned_cd, locked_cd, bg_cd, reminder_title). archived badge = Text (no cd).

### س7.5 — العنوان
- lines 575-585: `highlightedText(full = titleText, range = titleRange, style = titleMedium.copy(fontWeight = if (RTL) Medium else Bold), color = cardOn, maxLines = 1, textAlign = Start, modifier = Modifier.horizontalScroll(rememberScrollState()).clipToBounds())`. RTL: horizontalScroll.

### س7.6 — المتن
- lines 586-600: `MarkdownFormat.stripMarkdown(n.body)`; maxLines=2; lineHeight 22sp (RTL) / 20sp. snippet context if search.

### س7.7 — minuteTick
- lines 208-213:
```kotlin
var minuteTick by remember { mutableStateOf(0L) }
LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000); minuteTick = System.currentTimeMillis() } }
```
- قراءة في كل بطاقة (line 638): `if (minuteTick >= 0 && n.reminderAt != null && n.reminderAt > System.currentTimeMillis())`. minuteTick >= 0 دائماً true → إعادة تركيب كل بطاقة كل 60s.

### س7.8 — وقت "آخر تعديل"
- `SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())` — لغة الجهاز. updatedAt (Long ms).

### س7.9 — الرقائق (Chips)
- tags.take(5) + "+N" overflow. category chip. folder chip (border + color dot). horizontalScroll. modifier background/border RoundedCornerShape(10.dp).

---

## القسم 8 — وضع التحديد (Selection Mode)

### س8.1 — الحالة
- line 196: `var selected by remember { mutableStateOf(setOf<Long>()) }`. selectionMode = `sel.isNotEmpty()`. toggle flips membership. X (clear) → `selected = emptySet()`.

### س8.2 — "تحديد الكل"
- line 285: `TextButton(onClick = { selected = if (allIds.all { it in sel }) emptySet() else allIds })`. allIds = `shown.map { it.id }.toSet()` (المحملة/المعروضة). **ليس كل DB** — فقط shown (filtered list).

### س8.3 — العداد
- line 227: `Text(stringResource(R.string.selected_count, sel.size))`. sel = `selected.intersect(allIds)` (trim absent).

### س8.4 — التبديل بين التبويبات
- lines 193-194: `var favTab by remember(hasArchiveTab) { mutableStateOf(0) }`. remember keyed on hasArchiveTab → resetting favTab to 0 when hasArchiveTab changes. selected is separate remember (not keyed) → persists across tab switch within same composable. **selected not cleared on tab switch.**

---

## القسم 9 — قائمة ⋮ (Context Menu)

### س9.1 — كل عنصر
(see س-5). شروط: `!isArchive` (move), `!isArchive || n.favorite` (favorite), `!isArchive && !isFavorite` (duplicate).

### س9.2 — الإجراءات المفقودة
- **لا "مشاركة" (Share) ولا "تصدير" (Export/PDF/MD/HTML)** في قائمة ⋮. `onCopyNote` ينسخ للحافظة (title + body) — implemented in App.kt (not shown). `onDuplicateNote` duplicates.

### س9.3 — "نسخ للحافظة"
- callback `onCopyNote: (Note) -> Unit` — implementation in App.kt (not in NotesListScreen). Prompt does not expose body.

### س9.4 — "حذف"
- single: `pendingDelete = n.id` → AlertDialog → `onDelete(id)` (soft delete / trash). batch: `pendingBatchDelete = sel` → `onDeleteMany(ids)` → `vm.deleteMany` → `repo.trashMany` (soft delete). **دائمًا نقل للسلة** (soft delete), لا حذف نهائي من هذه الشاشة.

---

## القسم 10 — FAB

### س10.1 — التكوين
- lines 375-384: `FloatingActionButton(onClick = { Haptics.confirm(hctx); onAdd() }, shape = RoundedCornerShape(20.dp), containerColor = MaterialTheme.colorScheme.primary)`. Standard FAB (not Extended). No Modifier.size.

### س10.2 — الظهور
- line 375: `if (!isArchive && !isFavorite && showFab)`. showFab param from App.kt: `showFab = !isPro || fabPref || !barHasAdd` (line 1531).

### س10.3 — Haptic
- `Haptics.confirm(hctx)` (Haptics.kt). Type not specified in this file.

---

## القسم 11 — BottomAppBar (Selection Mode)

### س11.1 — الأزرار
- 2 أو 3 (see س-1). fav_remove (if isFavorite), archive/unarchive (or pendingBatchArchive), delete (red).

### س11.2 — الديناميكية
- زر archive/unarchive: `if (isArchive) onUnarchiveMany(ids) else pendingBatchArchive = ids`. زر delete: `pendingBatchDelete = sel` → confirm dialog → deleteMany.

---

## القسم 12 — الحوارات (Dialogs)

### س12.1 — قائمة
- `NotePickDialog` (pickFavOpen && isFavorite) — منتقي مفضلة.
- `ConfirmDialog` (pendingArchive) — تأكيد أرشفة مفرد.
- `ConfirmDialog` (pendingBatchArchive) — تأكيد أرشفة جماعي.
- `AlertDialog` (pendingDelete) — نقل للسلة (مفرد).
- `AlertDialog` (moveFor) — نقل لمجلد.
- `AlertDialog` (pendingBatchDelete) — نقل للسلة (جماعي).
- `AlertDialog` (deleting) — حاجب.

### س12.2 — "حاجب جاري الحذف"
- see س-2. `deleting` from NotesViewModel._deleting.

---

## القسم 13 — ViewModel

### س13.1 — NotesViewModel StateFlows
- `query: MutableStateFlow<String>` (line 24)
- `colorFilter: MutableStateFlow<Int?>` (line 27)
- `categoryFilter: MutableStateFlow<String?>` (line 30)
- `tagFilter: MutableStateFlow<String?>` (line 33)
- `includeArchive: MutableStateFlow<Boolean>` (line 72)
- `archiveQuery`, `favoriteQuery`, `trashQuery: MutableStateFlow<String>` (lines 96, 104, 233)
- `notes`, `archived`, `favorites`, `favoritesArchived`, `trashed`: StateFlow<List<Note>>
- `allTags`, `tagsMap`, `tagCounts`, `categories`, `categoryCounts`, `reminders`: StateFlow
- `deleting: StateFlow<Boolean>` (line 258)
- `restoreLog: Flow<List<RestoreRecord>>`

### س13.2 — State organization
- StateFlows منفصلة (لا data class واحد). ViewModel واحد (NotesViewModel) لجميع Variants (LIST/ARCHIVE/FAVORITES/TRASH) — نفس instance.

### س13.3 — Search/filter/sort combine
- `raw = combine(query.debounce(200), includeArchive, acctTick)` → flatMapLatest → repo.all()/search()/combine(search, searchArchived).
- `notes = combine(raw, store.sortOrder, colorFilter, tagAndCat, tagsMap)` → filter in memory → sortNotes.
- sort: pinned first, then sort order (UPDATED_ASC, CREATED_DESC/ASC, TITLE_ASC, else UPDATED_DESC).

### س13.4 — Pro Gating في VM
- `proNow()` (private, line 142): trial active OR entitlements.cached.pro.
- `togglePin`, `toggleLock`, `toggleFavorite`: `if (!proNow()) return@launch`.
- `duplicateNow`: `if (!proNow()) return -1L`.
- **archive, delete, save, setReminder, setCategory, tags, tasks, attachments — لا فحص proNow.** (UI gating only via showProLocks + App.kt callbacks.)

### س13.5 — _sessionTick / acctTick
- `_sessionTick: MutableStateFlow(0)` (line 42). `onUnlocked()` increments. `acctTick = combine(repo.accountChanges(), _sessionTick)`. يُعيد تشغيل كل التدفقات بعد فتح القفل.

---

## القسم 14 — Repository والـ DAO

### س14.1 — NotesRepository
- `all()`, `search(q)`, `archived()`, `searchArchived(q)`, `favorites()`, `searchFavorites(q)`, `favoritesArchived()`, `searchFavoritesArchived(q)`, `trashed()`, `searchTrash(q)`.
- `archive(id)`, `unarchive(id)`, `togglePin(id)`, `toggleLock(id)`, `toggleFavorite(id)`, `unfavoriteMany(ids)`, `duplicate(id, newTitle)`.
- `delete(id)` → `trashOne` (soft delete). `deletePermanently` → `hardDelete`. `emptyTrash`, `purgeTrash`.

### س14.2 — NoteDao queries
- `search`: LIKE on title/body/tags. `all`: no filter. `archived`, `favorites`, `favoritesArchived`, `trashed` variants. `latestTitles`: excludes locked notes (and locked folders) — widget cache only.
- `ORDER BY updatedAt DESC` on all list queries. **لا ORDER BY pinned** — pinned sorting in VM.

### س14.3 — Transactions
- `@Transaction` not used in NoteDao. `hardDelete` does VACUUM manually. `restoreMerge` uses individual inserts.

---

## القسم 15 — الترتيب (Sorting)

### س15.1 — خيارات
- `SettingsStore`: SORT_UPDATED_DESC/ASC, SORT_CREATED_DESC/ASC, SORT_TITLE_ASC. (5 options, SortOptions composable lines 1008-1022).

### س15.2 — التنفيذ
- في ViewModel (`sortNotes`, lines 96-106): pinned first, then sort. SQL ORDER BY updatedAt DESC (base). **Pinned-first دائماً** (لا حسب SortOption).

### س15.3 — Pinned-first
- `val pinned = list.filter { it.pinned }; val rest = list.filter { !it.pinned }`. pinned always first.

---

## القسم 16 — إمكانية الوصول (Accessibility)

### س16.1 — contentDescription
- 27 occurrence in NotesListScreen.kt. Examples: clear_search_cd, clear_selection, back_cd, menu_cd, fav_add_title, sort_cd, filter_cd, search_hint, new_note_cd, fav_cd, pinned_cd, locked_cd, bg_cd, reminder_title, more_cd, delete_cd, etc.

### س16.2 — Semantics على البطاقات
- **لا `Modifier.semantics { }` على NoteCard.** لا إعلان "مقفلة"/"مؤرشفة" لقارئ الشاشة beyond contentDescription على الأيقونات.

### س16.3 — TalkBack
- **لا `liveRegion`** في NotesListScreen.kt. Needs device to verify TalkBack behavior.

---

## القسم 17 — i18n

### س17.1 — المفاتيح
- `grep -rn "R.string" ... | wc -l` → 27 contentDescription + many more in Text(stringResource(...)). جميع النصوص عبر `stringResource(R.string.*)`. `grep -c 'Text("'` = 0 (لا نصوص خام).
- اللغات: values, values-en, values-es, values-fr, values-it, values-ja (6 لغات). (P3-D5 removed hi/ru/zh.)

### س17.2 — نصوص خام
- **0** `Text("...")` raw. All use stringResource.

---

## القسم 18 — الأداء والذاكرة

### س18.1 — إعادة التركيب
- NotesListScreen collects: notes, query, sortOrder, colorFilter, includeArchive, tagsByNote (tagsMap), allTags, tagFilter, allCategories, categoryFilter, folders, deleting, showProLocks, showFab, showAccount, accountId, accountLabel, showDates, favCandidates, archivedTabNotes, etc. Many collectAsStateWithLifecycle. minuteTick forces all cards recompose every 60s.

### س18.2 — minuteTick
- see س7.7. كل بطاقة تُعيد تركيب كل 60s (minuteTick >= 0 always true).

### س18.3 — Search debounce
- 200ms debounce in VM. كل حرف بعد 200ms → query. LIKE query on DB. No additional throttling.

### س18.4 — LazyColumn
- `key = { it.id }`, `contentType = { "note" }`. selectAll uses `allIds` = shown (filtered). With 1000 notes, selectAll selects only loaded/filtered (shown), not all DB.

---

## القسم 19 — حفظ الحالة (State Persistence)

### س19.1 — remember vs rememberSaveable
- `grep -c "rememberSaveable"` = **0** in NotesListScreen.kt. All state uses `remember` (lost on rotation/activity recreate):
  - selected: `remember { mutableStateOf(setOf<Long>()) }`
  - filtersOpen: `remember { mutableStateOf(false) }`
  - minuteTick: `remember { mutableStateOf(0L) }`
  - searchOpen: `remember(query) { mutableStateOf(query.isNotBlank()) }`
  - sortMenu, menuFor, pickFavOpen, moveFor: `remember { mutableStateOf(...) }`
  - searchFocus: `remember { FocusRequester() }`
  - pendingDelete, pendingBatchDelete, pendingArchive, pendingBatchArchive: `remember { mutableStateOf(...) }`
- **All lost on rotation.**

### س19.2 — Scroll state
- **لا `rememberLazyListState`** في NotesListScreen.kt (grep returned empty). Scroll position not saved.

### س19.3 — ViewModel state
- ViewModel survives config change (Hilt). MutableStateFlows (query, colorFilter, tagFilter, categoryFilter, includeArchive, selected? no selected is in Composable) persist. selected is Composable-level → lost on rotation.

---

## القسم 20 — Edge Cases و OnResume

### س20.1 — العودة من المحرر
- App.kt BackHandler: `editing = null` returns to list. Flow collection (notes) updates automatically via Room Flow. No manual refresh.

### س20.2 — القفل التلقائي
- App.kt: ON_RESUME checks `authVm.auth.checkAutoLock()`. If locked → flushPendingSave → clearSensitiveData → lockAndWipe → unlocked=false. NotesViewModel.clearSensitiveData clears query/filters. **selected, searchOpen, filtersOpen lost anyway (remember).**

### س20.3 — إضافة ملاحظة
- FAB → onAdd() → startNewNote() (editing=0L). After save → editing=id. Return to list: Flow updates. No manual refresh needed.

### س20.4 — حذف آخر ملاحظة
- shown.isEmpty() → EmptyState type 1 (empty_no_notes) with CTA "إضافة ملاحظة" (if LIST, not favorite/archive).

### س20.5 — التبديل بين Variants
- LIST → ARCHIVE → FAVORITES → LIST: same composable instance recreated per screen branch. selected persists within same composable? Actually screen change triggers different branch → different NotesListScreen instance → selected resets (remember). filtersOpen/searchOpen reset. **Scroll position lost.**

---

## القسم 21 — Pro Gating Surface

### س21.1 — isPro/showProLocks/proLock
- NotesListScreen has NO direct `isPro` reference. `showProLocks: Boolean = false` param. App.kt passes `showProLocks = !isPro`.
- App.kt callbacks (lines 1543-1555) gate: `if (isPro) { vm.togglePin(it) } else proLock()`, etc. for pin/lock/favorite/copy/duplicate.
- `onMoveToFolder`: `if (premiumVm.canUseFolders()) { ... } else proLock()`.
- move_to_folder/favorite/lock/pin/duplicate/copy show lock icon if showProLocks.

### س21.2 — VM vs UI
- VM: togglePin/toggleLock/toggleFavorite/duplicateNow check proNow() internally (defense in depth). archive, delete, save, setReminder, setCategory, tags, tasks, attachments — **no proNow check** (rely on UI gating). Risk: direct VM calls bypass UI gating for these.

### س21.3 — شارات القفل
- `trailingIcon = { if (showProLocks) Icon(Icons.Filled.Lock, contentDescription = null) }` on 6 menu items. proLock() shows Toast(R.string.pro_only) + navigates to PAYWALL.

---

## القسم 22 — الاختبارات الموجودة

### س22.1 — اختبارات NotesListScreen
- `find app/src/test -name "*NotesList*" -o -name "*NotesViewModel*" -o -name "*NoteCard*"` → **0 files**. No tests for NotesListScreen, NotesViewModel, or NoteCard.

### س22.2 — الاختبارات الموجودة
- 15 test files total, none covering Home UI/VM. SettingsSearchTest, SecurityStatusTest are UI-related but not NotesList.

### س22.3 — Robolectric
- No Robolectric tests for NotesListScreen. JVM pure only (data layer).

---

## القسم 23 — تناقضات الوصف مقابل الكود

| # | ما يقوله الوصف | ما يقوله الكود | الحكم |
|---|---|---|---|
| 1 | "BottomAppBar: ثلاثة أزرار" | 2 (LIST/ARCHIVE) أو 3 (FAVORITES) | جزئي |
| 2 | "FAB دائري 20dp" | shape RoundedCornerShape(20.dp) — corner radius, not size | تناقض |
| 3 | "حاجب جاري الحذف" | موجود (deleting flag) — كلا الحذفين | متطابق |
| 4 | "تبويبا المفضلة عند وجود أرشيف لاحق" | hasArchiveTab = isFavorite && archivedTabNotes != null | متطابق |
| 5 | "بحث: نافذة ~90 حرفًا" | 40+40 = 80 + query + ellipses | جزئي (~80) |
| 6 | "قائمة ⋮: 9 خيارات" | 9 items (with conditionals) | متطابق |
| 7 | "بطاقة Card بارتفاع 1dp" | cardElevation(defaultElevation = 1.dp) | متطابق |
| 8 | "شارة مؤرشف بحد مميز" | Text with border chipEdge (a11y for color blindness) | متطابق |
| 9 | "العنوان سطر واحد قابل للتمرير الأفقي" | horizontalScroll(rememberScrollState()).clipToBounds() | متطابق |
| 10 | "يُعاد التركيب كل دقيقة عبر minuteTick" | LaunchedEffect 60_000ms, minuteTick >= 0 always true → all cards | متطابق |
| 11 | "بحث في الملاحظات المقفلة" | لا فحص locked في SQL search | **تناقض** (يبحث في locked) |
| 12 | "4 أنواع empty state" | 2 أنواع (blank/not blank) | تناقض |
| 13 | "rememberSaveable للحالة" | 0 rememberSaveable; all remember | تناقض (state lost on rotation) |
| 14 | "BackHandler" | لا BackHandler في NotesListScreen | تناقض (back at App level) |
| 15 | "rememberLazyListState" | لا rememberLazyListState | تناقض (scroll lost) |
| 16 | "share/export" | غير موجود | غير منفّذ |
| 17 | "Semantics على البطاقات" | لا Modifier.semantics | غير منفّذ |
| 18 | "liveRegion" | لا liveRegion | غير منفّذ |
| 19 | "debounce" | 200ms in VM | متطابق |
| 20 | " Pro gating في VM" | togglePin/Lock/Favorite/Duplicate check proNow; archive/delete/save/etc. do not | جزئي |
| 21 | "share/export في قائمة ⋮" | غير موجود | غير منفّذ |
| 22 | "تبويبات المفضلة: active/archived" | hasArchiveTab = isFavorite && archivedTabNotes != null | متطابق |
| 23 | "favCandidates source" | notes.filter { !favorite && !archived } | متطابق |

---

## قائمة "غير منفّذ"
- Share/Export (PDF/MD/HTML) في قائمة ⋮.
- Semantics/TalkBack liveRegion على البطاقات.
- rememberSaveable (state persistence on rotation).
- rememberLazyListState (scroll persistence).
- BackHandler داخل NotesListScreen.
- اختبارات (NotesListScreen, NotesViewModel, NoteCard) — 0.
- Full-text search (FTS) — uses LIKE.
- Multi-tag filter (only single tag).
- Search in attachments.

## قائمة "موجود لكن لا يعمل كما يوحي"
- `locked` badge only (title/body shown) — like Entitlements `locked` cosmetic.
- `showProLocks` is cosmetic lock icon; VM enforces proNow only for pin/lock/favorite/duplicate, not archive/delete/save/etc.
- `minuteTick >= 0` always true → all cards recompose every 60s regardless of reminders.
- `searchOpen = remember(query)` resets state when query changes.
- `allIds.all { it in sel }` for "select all" — only shown/filtered, not all DB.
- favTab remember keyed on hasArchiveTab resets to 0 on tab availability change.
- selected not cleared on tab switch (favTab change).

## قائمة "يحتاج جهازًا"
- TalkBack behavior (no semantics/liveRegion).
- Search with Arabic diacritics (lowercase() only).
- minuteTick recomposition impact on performance (1000 notes).
- FAB size actual (default 56dp — needs visual check).
- Locked note title/body visibility (security review).
- Back navigation from list (handled at App level).
- Scroll persistence on rotation.
- Empty state CTA behavior.
- Filter card show/hide animation.
- Selection mode with 1000 notes (selectAll scope).
- RTL horizontalScroll on title.
- Note card color contrast (a11y).

---

## معلومات إضافية

- Files: NotesListScreen.kt (1065 lines), NotesViewModel.kt (341), NotesRepository.kt (591), NoteDao.kt (199), App.kt (~1800).
- Tests: 15 total, 0 for Home.
- Languages: 6 (values, en, es, fr, it, ja).
- contentDescription: 27 occurrences.
- Raw Text("..."): 0.
- remember: 8. rememberSaveable: 0.
- rememberLazyListState: 0. BackHandler (in NotesListScreen): 0.
- isPro direct in NotesListScreen: 0. showProLocks: param.
- favCandidates source: App.kt line 1492 `notes.filter { !it.favorite && it.archivedAt == null }`.
- proNow: NotesViewModel private, uses trial + entitlements.cached.pro.
- unlock callback: vm.onUnlocked() increments _sessionTick.
- search debounce: 200ms. minuteTick: 60s.
- sort options: 5 (UPDATED_DESC/ASC, CREATED_DESC/ASC, TITLE_ASC). pinned always first.
- SQL: LIKE on title/body/tags. No FTS. ORDER BY updatedAt DESC.
- latestTitles (DAO): excludes locked notes/folders (widget cache).
- search in locked: NOT excluded (security concern).