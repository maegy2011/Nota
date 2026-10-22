package com.nota.notes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nota.notes.R
import com.nota.notes.data.Folder
import com.nota.notes.data.Note
import com.nota.notes.data.SettingsStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    notes: List<Note>,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
    // درج التنقل: زر القائمة يفتحه من الرئيسية — وأفاتار الحساب يفتح شاشة الحساب
    onNav: () -> Unit = {},
    sortOrder: String = "updated_desc",
    onSortChange: (String) -> Unit = {},
    colorFilter: Int? = null,
    onColorFilter: (Int?) -> Unit = {},
    // وضع الأرشيف: زر رجوع + عنوان أرشيف + زر استعادة لكل بطاقة — بلا زر إضافة ولا زر أرشيف
    isArchive: Boolean = false,
    // وضع المفضلة: نفس سلوك الأرشيف (استعراض + إزالة من المفضلة) — القلب يضيف من أي قائمة
    isFavorite: Boolean = false,
    // تبويب المفضلة المؤرشفة لاحقًا — تُمرَّر من شاشة المفضلة فقط (المؤرشفة لا تُضاف للمفضلة)
    archivedTabNotes: List<Note>? = null,
    onOpenFavorites: () -> Unit = {},
    onBack: () -> Unit = {},
    // أفاتار الحساب: مدخل شاشة الحساب من الرئيسية — يظهر في الوضع العادي فقط
    showAccount: Boolean = false,
    accountId: String? = null,
    accountLabel: String? = null,
    onAccount: () -> Unit = {},
    // المجلدات (مدفوعة): للشارة على البطاقة + النقل من القائمة
    folders: List<Folder> = emptyList(),
    onMoveToFolder: (Long, Long?) -> Unit = { _, _ -> },
    onUnarchive: (Long) -> Unit = {},
    // تضمين الأرشيف في بحث الرئيسية — يظهر فقط مع وجود استعلام في القائمة الرئيسية
    includeArchive: Boolean = false,
    onIncludeArchive: (Boolean) -> Unit = {},
    showDates: Boolean = true,
    // إجراءات مفردة وجماعية — الجماعية تُطبَّق على المحدد دفعة واحدة
    onArchive: (Long) -> Unit = {},
    onArchiveMany: (Set<Long>) -> Unit = {},
    onUnarchiveMany: (Set<Long>) -> Unit = {},
    onDeleteMany: (Set<Long>) -> Unit = {},
    // التثبيت بالدبوس — المثبتة أولًا في كل ترتيب
    onTogglePin: (Long) -> Unit = {},
    // القفل الخاص — فتحها يتطلب تحدي PIN/بصمة
    onToggleLock: (Long) -> Unit = {},
    // المفضلة — قلب يضيف/يزيل + إزالة جماعية للمحدد
    onToggleFavorite: (Long) -> Unit = {},
    onUnfavoriteMany: (Set<Long>) -> Unit = {},
    // إضافة للمفضلة من شاشتها (قرار 26 سبتمبر 2026): مرشحون بلا مفضلة يُختار منهم
    favCandidates: List<Note> = emptyList(),
    // نسخ الملاحظة للحافظة — عنوان + نص من قائمة البطاقة مباشرة
    onCopyNote: (Note) -> Unit = {},
    // الوسوم أسفل البطاقة: خريطة noteId ← أسماء + اسم المجلد كتصنيف
    tagsByNote: Map<Long, List<String>> = emptyMap(),
    // الفلترة بالوسم — صف رقائق تحت فلتر اللون (الرئيسية فقط تُمرَّر لها الوسوم)
    allTags: List<String> = emptyList(),
    tagFilter: String? = null,
    onTagFilter: (String?) -> Unit = {},
    // الفلترة بالتصنيف — تسمية واحدة حرة (الرئيسية فقط تُمرَّر لها التصنيفات)
    allCategories: List<String> = emptyList(),
    categoryFilter: String? = null,
    onCategoryFilter: (String?) -> Unit = {},
    // تكرار الملاحظة — نسخة بعنوان يوضح أنها نسخة (تُفتح بعد إنشائها)
    onDuplicateNote: (Note) -> Unit = {},
    // حالة الحذف من الـ ViewModel — تُعرض حاجبة حتى تمام العملية
    deleting: Boolean = false,
    // شارات القفل على الإجراءات المدفوعة في القائمة (تُمرَّر !isPro — قاعدة 26 سبتمبر 2026)
    showProLocks: Boolean = false,
    // إظهار FAB — يُخفى بتفضيل Pro مع إجبار عند غياب الإضافة من الشريط (قرار 26 سبتمبر 2026)
    showFab: Boolean = true
) {
    // تأكيد قبل الحذف النهائي — بلا سلة مهملات بأمر المستخدم
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    var pendingBatchDelete by remember { mutableStateOf<Set<Long>?>(null) }
    // تأكيد قبل الأرشفة (بأمر محمد 27 سبتمبر 2026): مفرد + جماعي — الاستعادة بلا تأكيد (عكسية آمنة)
    var pendingArchive by remember { mutableStateOf<Long?>(null) }
    var pendingBatchArchive by remember { mutableStateOf<Set<Long>?>(null) }
    // التأكيد الثاني المتتالي + حالة الحذف حتى تمامه (قرار 23 سبتمبر 2026)
    var sortMenu by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Long?>(null) }
    var pickFavOpen by remember { mutableStateOf(false) }
    var moveFor by remember { mutableStateOf<Long?>(null) }
    val foldersById = remember(folders) { folders.associateBy { it.id } }
    // تبويبا المفضلة: النشطة + المؤرشفة لاحقًا — التحديد والعداد والقائمة يتبعان التبويب المعروض
    val hasArchiveTab = isFavorite && archivedTabNotes != null
    var favTab by remember(hasArchiveTab) { mutableStateOf(0) }
    val shown: List<Note> = if (hasArchiveTab && favTab == 1) archivedTabNotes!! else notes
    // التحديد المتعدد: ضغطة مطولة أو "تحديد" من القائمة تدخل الوضع، والضغط يبدّل، وX تخرج
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val allIds = remember(shown) { shown.map { it.id }.toSet() }
    val sel = selected.intersect(allIds) // تقليم الغائبة بعد حذف أو تغيّر القائمة
    val selectionMode = sel.isNotEmpty()
    fun toggle(id: Long) { selected = if (id in selected) selected - id else selected + id }
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    // حالة الفلاتر مرفوعة فوق الـ Scaffold — الهيدر يبدّلها والمحتوى يعرض بطاقتها
    var filtersOpen by remember { mutableStateOf(false) }
    val hasActiveFilter = colorFilter != null || tagFilter != null || categoryFilter != null || includeArchive
    // دقة الجرس: إعادة تركيب كل دقيقة حتى يختفي الموعد الماضي بلا انتظار تغيير في القائمة
    val minuteTick = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            minuteTick.value = System.currentTimeMillis()
        }
    }
    // البحث الديناميكي: مخفي افتراضيًا — أيقونة تفتحه في نفس مكان الهيدر مع ضبط الارتفاع تلقائيًا
    var searchOpen by remember(query) { mutableStateOf(query.isNotBlank()) }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Scaffold(
        // داخل DrawerShell: حواف النظام يستهلكها الغلاف الخارجي — بلا ازدواج هنا
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text(
                            stringResource(R.string.selected_count, sel.size),
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else if (searchOpen) {
                        // مربع البحث يظهر في نفس مكان الهيدر — الارتفاع يتضبط تلقائيًا مع TopAppBar
                        OutlinedTextField(
                            value = query, onValueChange = onQuery,
                            placeholder = { Text(stringResource(if (isArchive) R.string.search_archive_hint else if (isFavorite) R.string.search_favorites_hint else R.string.search_hint)) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (query.isNotEmpty()) onQuery("")
                                    else { searchOpen = false; focusManager.clearFocus() }
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search_cd))
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            modifier = Modifier.fillMaxWidth().height(52.dp).focusRequester(searchFocus)
                        )
                        LaunchedEffect(Unit) { searchFocus.requestFocus(); keyboard?.show() }
                    } else {
                        Text(
                            stringResource(when {
                                isArchive -> R.string.archive_title
                                isFavorite -> R.string.favorites_title
                                else -> R.string.app_name
                            }),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { selected = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_selection))
                        }
                    } else if (isArchive || isFavorite) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                        }
                    } else {
                        IconButton(onClick = onNav) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu_cd))
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        // تحديد الكل / إلغاء تحديد الكل بضغطة واحدة — زر نصي بلا اعتماد على أيقونات ناقصة
                        TextButton(onClick = { selected = if (allIds.all { it in sel }) emptySet() else allIds }) {
                            Text(stringResource(R.string.select_all))
                        }
                    } else if (searchOpen) {
                        // الإضافة للمفضلة من شاشتها (قرار 26 سبتمبر 2026)
                        if (isFavorite) {
                            IconButton(onClick = { pickFavOpen = true }) {
                                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.fav_add_title))
                            }
                        }
                        // البحث مفتوح: الترتيب والفلتر يبقيان ظاهرين — والمربع ينكمش لهما تلقائيًا (قرار 26 سبتمبر 2026)
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.Filled.List, contentDescription = stringResource(R.string.sort_cd))
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortOptions(sortOrder) { onSortChange(it); sortMenu = false }
                        }
                        IconButton(onClick = { filtersOpen = !filtersOpen }) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.filter_cd),
                                tint = if (filtersOpen || hasActiveFilter) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // أيقونة البحث تفتحه ديناميكيًا في نفس مكان الهيدر + الترتيب والفرز
                        IconButton(onClick = { searchOpen = true }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_hint))
                        }
                        // الترتيب والفرز بجوار البحث في الهيدر (قرار 23 سبتمبر 2026)
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.Filled.List, contentDescription = stringResource(R.string.sort_cd))
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortOptions(sortOrder) { onSortChange(it); sortMenu = false }
                        }
                        IconButton(onClick = { filtersOpen = !filtersOpen }) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.filter_cd),
                                tint = if (filtersOpen || hasActiveFilter) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // شريط الإجراءات الجماعية: أرشفة/استعادة + حذف للمحدد دفعة واحدة — والمفضلة إزالة منها
            if (selectionMode) {
                BottomAppBar {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isFavorite) {
                            Button(
                                onClick = { val ids = sel; selected = emptySet(); onUnfavoriteMany(ids) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.fav_remove))
                            }
                        }
                        Button(
                            onClick = {
                                val ids = sel; selected = emptySet()
                                if (isArchive) onUnarchiveMany(ids) else pendingBatchArchive = ids
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(if (isArchive) R.string.menu_unarchive else R.string.menu_archive))
                        }
                        Button(
                            onClick = { pendingBatchDelete = sel },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.delete_cd))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // الأرشيف والمفضلة استعراض فقط — لا إنشاء ملاحظات داخلهما
            if (!isArchive && !isFavorite && showFab) {
                val hctx = LocalContext.current
                FloatingActionButton(
                    onClick = { Haptics.confirm(hctx); onAdd() },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_note_cd))
                }
            }
        },
        content = { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)
            ) {
            // تبويبا المفضلة: النشطة والمؤرشفة لاحقًا — تُعرض فقط عند توفر التبويب الثاني
            if (hasArchiveTab) {
                TabRow(selectedTabIndex = favTab, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Tab(selected = favTab == 0, onClick = { favTab = 0; selected = emptySet() }, text = { Text(stringResource(R.string.favorites_tab_active)) })
                    Tab(selected = favTab == 1, onClick = { favTab = 1; selected = emptySet() }, text = { Text(stringResource(R.string.favorites_tab_archived)) })
                }
            }
            // بطاقة الفلاتر: ألوان + وسوم + أرشيف — ظاهرة عند الفتح أو البحث أو وجود فلتر نشط
            if (filtersOpen || hasActiveFilter || query.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            stringResource(R.string.filter_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        ColorFilterRow(selected = colorFilter, onPick = onColorFilter)
                        // الفلترة بالوسم — تظهر فقط عند وجود وسوم (نقرة تفعّل، ونقرة ثانية تلغي)
                        if (allTags.isNotEmpty()) {
                            TagFilterRow(tags = allTags, selected = tagFilter, onPick = onTagFilter)
                        }
                        // الفلترة بالتصنيف — تظهر فقط عند وجود تصنيفات (نقرة تفعّل، ونقرة ثانية تلغي)
                        if (allCategories.isNotEmpty()) {
                            CategoryFilterRow(cats = allCategories, selected = categoryFilter, onPick = onCategoryFilter)
                        }
                        // تضمين الأرشيف فلتر كغيره — داخل البطاقة بدل صف مشروط منفصل
                        if (!isArchive && (query.isNotBlank() || includeArchive)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = includeArchive, onCheckedChange = onIncludeArchive)
                                Text(
                                    stringResource(R.string.include_archive),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { onIncludeArchive(!includeArchive) }
                                )
                            }
                        }
                    }
                }
            }
            // العداد السياقي: العدد + سياقه (بحث/فلتر) + زر مسح الفلاتر عند التفعيل
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when {
                        shown.isEmpty() -> ""
                        query.isNotBlank() || hasActiveFilter -> stringResource(R.string.results_fmt, shown.size)
                        else -> stringResource(R.string.notes_count, shown.size)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (hasActiveFilter) {
                    TextButton(onClick = { onColorFilter(null); onTagFilter(null); onCategoryFilter(null); onIncludeArchive(false) }) {
                        Text(stringResource(R.string.clear_filters))
                    }
                }
            }
            if (shown.isEmpty()) {
                // حالة الفراغ المرقاة: أيقونة + نص + زر إجراء — لا نص وحيد (قاعدة حالات UX)
                Column(
                    Modifier.fillMaxSize().padding(bottom = 80.dp).padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                if (query.isBlank()) Icons.Filled.Add else Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (query.isBlank()) stringResource(R.string.empty_no_notes) else stringResource(R.string.empty_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    if (query.isBlank() && !isFavorite && !isArchive) {
                        Button(onClick = onAdd) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.empty_cta_add))
                        }
                    } else {
                        OutlinedButton(onClick = { onQuery("") }) {
                            Text(stringResource(R.string.empty_cta_clear))
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shown, key = { it.id }, contentType = { "note" }) { n ->
                        // خلفية Sticky Notes حسب لون الملاحظة — النصوص تتبع لونها المقروء تلقائيًا
                        val cardBg = noteCardBg(n.color)
                        val cardOn = noteCardOn(n.color)
                        val cardSub = noteCardSub(cardOn)
                        // حد رفيع لشارة الأرشيف حسب المواصفة (لا لون وحده لعمى الألوان — معها تسمية نصية)
                        val chipEdge = if (LocalNotaDark.current) Color(0xFF5C5346) else Color(0xFFDFD2BE)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg, contentColor = cardOn),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            // حدود outlineVariant 1dp في الفاتح (الباستيل على الورق الدافئ قد يتداخل) + حد التحديد الداكن
                            border = if (n.id in sel) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                else if (!LocalNotaDark.current) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                else null,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { if (selectionMode) toggle(n.id) else onOpen(n.id) },
                                onLongClick = { selected = selected + n.id }
                            )
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                                // مربع التحديد يظهر في وضع التحديد فقط
                                if (selectionMode) {
                                    Checkbox(
                                        checked = (n.id in sel),
                                        onCheckedChange = { toggle(n.id) }
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    // صف الشارات الموحد: دبوس/قفل/خلفية/قلب + شارة الأرشيف — سطر واحد بدل سطرين
                                    if (n.pinned || n.locked || n.background != null || n.favorite || n.archivedAt != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        ) {
                                            if (n.favorite) {
                                                Icon(
                                                    Icons.Filled.Favorite,
                                                    contentDescription = stringResource(R.string.fav_cd),
                                                    tint = cardOn,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            if (n.pinned) {
                                                Icon(
                                                    Icons.Filled.PushPin,
                                                    contentDescription = stringResource(R.string.pinned_cd),
                                                    tint = cardOn,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            if (n.locked) {
                                                Icon(
                                                    Icons.Filled.Lock,
                                                    contentDescription = stringResource(R.string.locked_cd),
                                                    tint = cardOn,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            if (n.background != null) {
                                                Icon(
                                                    Icons.Filled.Image,
                                                    contentDescription = stringResource(R.string.bg_cd),
                                                    tint = cardOn,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            // شارة الأرشيف — تميّز نتائج الأرشيف المدمجة في بحث الرئيسية
                                            if (n.archivedAt != null) {
                                                Text(
                                                    stringResource(R.string.archived_badge),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = cardOn,
                                                    modifier = Modifier
                                                        .defaultMinSize(minWidth = 48.dp)
                                                        .background(cardSub.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                        .border(1.dp, chipEdge, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    // شارة المجلد العلوية محذوفة: الرقاقة السفلية (بنقطة اللون والاسم) تكفي — بلا تكرار
                                    // البحث يظلل التطابق في العنوان + سطر السياق من المتن (27 سبتمبر 2026)
                                    val qTrim = query.trim()
                                    val titleText = n.title.ifBlank { stringResource(R.string.untitled) }
                                    val titleRange = remember(titleText, qTrim) {
                                        if (qTrim.isEmpty()) null
                                        else titleText.lowercase().indexOf(qTrim.lowercase()).takeIf { it >= 0 }
                                            ?.let { IntRange(it, it + qTrim.length - 1) }
                                    }
                                    highlightedText(
                                        full = titleText,
                                        range = titleRange,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (LocalLayoutDirection.current == LayoutDirection.Rtl) FontWeight.Medium else FontWeight.Bold
                                        ),
                                        color = cardOn,
                                        maxLines = 1,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.horizontalScroll(
                                            rememberScrollState()
                                        ).clipToBounds()
                                    )
                                    if (n.body.isNotBlank()) {
                                        val strippedBody = remember(n.body) { MarkdownFormat.stripMarkdown(n.body) }
                                        val snip = remember(strippedBody, qTrim) {
                                            if (qTrim.isEmpty()) null else searchSnippet(strippedBody, qTrim)
                                        }
                                        highlightedText(
                                            // مع بحث: سطر السياق حول التطابق — وبلا بحث: أول سطرين (قرار 26 سبتمبر 2026)
                                            full = snip?.first ?: strippedBody.lines().take(2).joinToString("\n"),
                                            range = snip?.second,
                                            maxLines = 2,
                                            // ارتفاع 1.55× للعربية (تشكيل ونزول) — 20sp الافتراضي لا يكفي
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                lineHeight = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 22.sp else 20.sp
                                            ),
                                            color = cardSub,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    // تاريخ البطاقة: آخر تعديل فقط — بلا تاريخ الإنشاء (قرار 26 سبتمبر 2026)
                                    if (showDates) {
                                        Text(
                                            stringResource(R.string.updated_at, fmt.format(Date(n.updatedAt))),
                                            // أرقام التواريخ بلون كامل لا شفاف — الرفيع عند 11sp يُثقَّل
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cardOn,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                    // التذكير المستقبلي: جرس + الموعد — الماضي يُخفى (المنبه انطلق وانتهى)
                                    // minuteTick قراءة مقصودة: تُعيد التركيب كل دقيقة لدقة الجرس
                                    val showReminder by remember(n.reminderAt, minuteTick) {
                                        derivedStateOf {
                                            minuteTick.value >= 0 && n.reminderAt != null && n.reminderAt > System.currentTimeMillis()
                                        }
                                    }
                                    if (showReminder) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Notifications,
                                                contentDescription = stringResource(R.string.reminder_title),
                                                tint = cardOn,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                fmt.format(Date(n.reminderAt!!)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cardOn
                                            )
                                        }
                                    }
                                    // الوسوم والتصنيف أسفل البطاقة: رقائق #وسم (حتى 5) + رقاقة التصنيف + اسم المجلد
                                    val noteTags = tagsByNote[n.id].orEmpty()
                                    val catFolder = n.folderId?.let { foldersById[it] }
                                    val noteCat = n.category?.takeIf { it.isNotBlank() }
                                    if (noteTags.isNotEmpty() || noteCat != null || catFolder != null) {
                                        Row(
                                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            noteTags.take(5).forEach { t ->
                                                Text(
                                                    "#" + t,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = cardOn,
                                                    maxLines = 1,
                                                    modifier = Modifier
                                                        .background(cardSub.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (noteTags.size > 5) {
                                                Text(
                                                    "+${noteTags.size - 5}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = cardSub,
                                                    modifier = Modifier.padding(horizontal = 2.dp)
                                                )
                                            }
                                            noteCat?.let { c ->
                                                Text(
                                                    c,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = cardOn,
                                                    maxLines = 1,
                                                    modifier = Modifier
                                                        .background(cardSub.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            catFolder?.let { f ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .border(1.dp, chipEdge, RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Box(
                                                        Modifier.size(8.dp).clip(CircleShape)
                                                            .background(noteCardBg(f.color.coerceIn(NotePalette.indices)))
                                                    )
                                                    Text(f.name, style = MaterialTheme.typography.labelSmall, color = cardOn, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                                // قائمة منبثقة بكل إجراء — داخل Box لتلتصق بزر ⋮ لا ببداية الصف (قرار 26 سبتمبر 2026)
                                if (!selectionMode) {
                                    Box {
                                    IconButton(onClick = { menuFor = n.id }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_cd), tint = cardOn)
                                    }
                                    DropdownMenu(expanded = (menuFor == n.id), onDismissRequest = { menuFor = null }) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(if (n.id in sel) R.string.menu_deselect else R.string.menu_select)) },
                                            onClick = { toggle(n.id); menuFor = null }
                                        )
                                        // النقل إلى مجلد — من القائمة الرئيسية فقط (الأرشيف استعراض)
                                        if (!isArchive) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.move_to_folder)) },
                                                trailingIcon = { if (showProLocks) Icon(Icons.Filled.Lock, contentDescription = null) },
                                                onClick = { menuFor = null; moveFor = n.id }
                                            )
                                        }
                                        // المؤرشفة لا تُضاف للمفضلة — تُعرض الإزالة فقط لمن أُرشفت وهي مفضلة
                                        if (!isArchive || n.favorite) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(if (n.favorite) R.string.fav_remove else R.string.fav_add)) },
                                                trailingIcon = { if (showProLocks) Icon(Icons.Filled.Lock, contentDescription = null) },
                                                onClick = { menuFor = null; onToggleFavorite(n.id) }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(stringResource(if (n.locked) R.string.unlock_menu else R.string.lock_menu)) },
                                            trailingIcon = { if (showProLocks) Icon(Icons.Filled.Lock, contentDescription = null) },
                                            onClick = { menuFor = null; onToggleLock(n.id) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(if (n.pinned) R.string.unpin_menu else R.string.pin_menu)) },
                                            trailingIcon = { if (showProLocks) Icon(Icons.Filled.Lock, contentDescription = null) },
                                            onClick = { menuFor = null; onTogglePin(n.id) }
                                        )
                                        if (isArchive) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.menu_unarchive)) },
                                                onClick = { menuFor = null; onUnarchive(n.id) }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.menu_archive)) },
                                                onClick = { menuFor = null; pendingArchive = n.id }
                                            )
                                        }
                                        // التكرار من الرئيسية فقط — الأرشيف والمفضلة استعراض (قرار 26 سبتمبر 2026)
                                        if (!isArchive && !isFavorite) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.duplicate_menu)) },
                                                trailingIcon = { if (showProLocks) Icon(Icons.Filled.Lock, contentDescription = null) },
                                                onClick = { menuFor = null; onDuplicateNote(n) }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.copy_note)) },
                                            trailingIcon = { if (showProLocks) Icon(Icons.Filled.Lock, contentDescription = null) },
                                            onClick = { menuFor = null; onCopyNote(n) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete_cd), color = MaterialTheme.colorScheme.error) },
                                            onClick = { menuFor = null; pendingDelete = n.id }
                                        )
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    )
    // منتقي الإضافة للمفضلة — من مرشحي الشاشة (قرار 26 سبتمبر 2026)
    if (pickFavOpen && isFavorite) {
        NotePickDialog(
            title = stringResource(R.string.fav_add_title),
            notes = favCandidates,
            emptyText = stringResource(R.string.mgr_empty_candidates),
            onPick = { id -> pickFavOpen = false; onToggleFavorite(id) },
            onDismiss = { pickFavOpen = false }
        )
    }
    // تأكيد الأرشفة: مفرد + جماعي (بأمر محمد 27 سبتمبر 2026)
    pendingArchive?.let { id ->
        ConfirmDialog(
            message = stringResource(R.string.confirm_archive_msg),
            onConfirm = { pendingArchive = null; onArchive(id) },
            onDismiss = { pendingArchive = null }
        )
    }
    pendingBatchArchive?.let { ids ->
        ConfirmDialog(
            message = stringResource(R.string.confirm_batch_archive_msg, ids.size),
            onConfirm = { pendingBatchArchive = null; onArchiveMany(ids) },
            onDismiss = { pendingBatchArchive = null }
        )
    }
    // التأكيد الأول للمفرد — الموافقة تفتح الثاني بدل الحذف
    // النقل للسلة بتأكيد واحد فقط (27 سبتمبر 2026) — الاستعادة متاحة 30 يومًا فلا داعي للتأكيد المزدوج
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.trash_move_title)) },
            text = { Text(stringResource(R.string.trash_move_msg)) },
            confirmButton = {
                TextButton(onClick = { onDelete(id); pendingDelete = null }) {
                    Text(stringResource(R.string.trash_move_btn), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    // نقل ملاحظة إلى مجلد — من القائمة الرئيسية فقط
    moveFor?.let { id ->
        AlertDialog(
            onDismissRequest = { moveFor = null },
            title = { Text(stringResource(R.string.move_to_folder)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onMoveToFolder(id, null); moveFor = null }.padding(vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.no_folder))
                    }
                    folders.forEach { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onMoveToFolder(id, f.id); moveFor = null }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_folder),
                                contentDescription = null,
                                tint = noteCardOn(f.color.coerceIn(NotePalette.indices)),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moveFor = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    // النقل الجماعي للسلة بتأكيد واحد — الموافقة تنفذ وتخرج من وضع التحديد (27 سبتمبر 2026)
    pendingBatchDelete?.let { ids ->
        AlertDialog(
            onDismissRequest = { pendingBatchDelete = null },
            title = { Text(stringResource(R.string.trash_move_title)) },
            text = { Text(stringResource(R.string.batch_trash_msg, ids.size)) },
            confirmButton = {
                TextButton(onClick = { onDeleteMany(ids); pendingBatchDelete = null; selected = emptySet() }) {
                    Text(stringResource(R.string.trash_move_btn), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBatchDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    // حالة الحذف حتى تمامه — حاجبة بلا زر (مهم مع الإتلاف متعدد التمريرات)
    if (deleting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.delete_title)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        stringResource(R.string.delete_progress),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            },
            confirmButton = {}
        )
    }
}

// صف الفلترة باللون — نقطة تعني "أرني هذا اللون فقط"، والضغط على المختارة أو الأولى (الكل) يلغي الفلترة
@Composable
private fun ColorFilterRow(selected: Int?, onPick: (Int?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.filter_by_color),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // الأولى = الكل (تُلغي الفلترة)
        FilterDot(
            preview = MaterialTheme.colorScheme.surface,
            isSel = (selected == null),
            onClick = { onPick(null) }
        )
        NotePalette.indices.drop(1).forEach { i ->
            FilterDot(
                preview = noteCardBg(i),
                isSel = (selected == i),
                onClick = { onPick(if (selected == i) null else i) }
            )
        }
    }
}

// صف الفلترة بالوسوم: الكل + رقائق — المختارة مميزة بإطار، والنقرة الثانية تلغي
@Composable
private fun TagFilterRow(tags: List<String>, selected: String?, onPick: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TagChip(text = stringResource(R.string.filter_all_tags), isSel = (selected == null), onClick = { onPick(null) })
        tags.forEach { t ->
            TagChip(text = "#" + t, isSel = (selected == t), onClick = { onPick(if (selected == t) null else t) })
        }
    }
}

@Composable
private fun CategoryFilterRow(cats: List<String>, selected: String?, onPick: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TagChip(text = stringResource(R.string.filter_all_cats), isSel = (selected == null), onClick = { onPick(null) })
        cats.forEach { c ->
            TagChip(text = c, isSel = (selected == c), onClick = { onPick(if (selected == c) null else c) })
        }
    }
}

@Composable
private fun TagChip(text: String, isSel: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .background(
                if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun FilterDot(preview: androidx.compose.ui.graphics.Color, isSel: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(if (isSel) 32.dp else 28.dp)
                .clip(CircleShape)
                .background(preview)
                .border(
                    width = if (isSel) 2.dp else 1.dp,
                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
    }
}

// قائمة خيارات الترتيب — تُحفظ في الإعدادات وتبقى بعد إغلاق التطبيق
@Composable
internal fun SortOptions(selected: String, onPick: (String) -> Unit) {
    val opts = listOf(
        SettingsStore.SORT_UPDATED_DESC to stringResource(R.string.sort_updated_desc),
        SettingsStore.SORT_UPDATED_ASC to stringResource(R.string.sort_updated_asc),
        SettingsStore.SORT_CREATED_DESC to stringResource(R.string.sort_created_desc),
        SettingsStore.SORT_CREATED_ASC to stringResource(R.string.sort_created_asc),
        SettingsStore.SORT_TITLE_ASC to stringResource(R.string.sort_title_az)
    )
    opts.forEach { (v, label) ->
        DropdownMenuItem(
            text = { Text(if (v == selected) "✓ $label" else label) },
            onClick = { onPick(v) }
        )
    }
}

// بحث القائمة بسطر السياق (27 سبتمبر 2026): نافذة ~90 حرفًا حول أول تطابق + مداه للتظليل — بلا تطابق تُرجع null
private fun searchSnippet(stripped: String, query: String): Pair<String, IntRange>? {
    if (query.isEmpty()) return null
    val i = stripped.lowercase().indexOf(query.lowercase())
    if (i < 0) return null
    val start = (i - 40).coerceAtLeast(0)
    val end = (i + query.length + 40).coerceAtMost(stripped.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < stripped.length) "…" else ""
    val text = prefix + stripped.substring(start, end).replace('\n', ' ') + suffix
    val off = prefix.length - start
    return text to IntRange(i + off, i + off + query.length - 1)
}

// نص مظلل: خلفية بلون الأساسي الشفاف على مدى التطابق — للعنوان وسطر السياق في نتائج البحث
@Composable
private fun highlightedText(
    full: String,
    range: IntRange?,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    maxLines: Int,
    modifier: Modifier = Modifier,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    val bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val text = remember(full, range, bg) {
        if (range == null) AnnotatedString(full)
        else AnnotatedString.Builder(full).apply {
            addStyle(
                SpanStyle(background = bg),
                range.first.coerceIn(0, full.length),
                (range.last + 1).coerceIn(0, full.length)
            )
        }.toAnnotatedString()
    }
    Text(text = text, style = style, color = color, maxLines = maxLines, modifier = modifier, overflow = overflow, textAlign = textAlign)
}
