package com.nota.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nota.notes.R
import com.nota.notes.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// شاشة إدارة الوسوم: كل وسم بعدّاده — إعادة تسمية (تدمج) + حذف (يفك الروابط) + الضغطة تعرض ملاحظاته
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    counts: Map<String, Int>,
    onShowNotes: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    // إنشاء وسم مستقل (قرار 26 سبتمبر 2026): يُحفظ بعدّاد صفر حتى يُسند لملاحظة
    onCreate: (String) -> Unit = {},
    // القفل المرئي (27 سبتمبر 2026): العرض والفلترة مجانًا — التعديل والحذف والإنشاء مدفوع
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    var renameOf by remember { mutableStateOf<String?>(null) }
    var deleteOf by remember { mutableStateOf<String?>(null) }
    var createOpen by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            // داخل DrawerShell: الهيدر المثبت من الغلاف — بلا شريط داخلي (27 سبتمبر 2026)
            contentWindowInsets = WindowInsets(0),
            floatingActionButton = {
                if (locked) {
                    FloatingActionButton(onClick = onLocked) {
                        Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.mgr_create_tag))
                    }
                } else {
                    FloatingActionButton(onClick = { createOpen = true }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.mgr_create_tag))
                    }
                }
            }
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                if (locked) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ProLockedBanner(onLocked)
                    }
                }
            if (counts.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    ManageEmptyState(Icons.Filled.Edit, stringResource(R.string.mgr_empty_tags))
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(counts.keys.sortedWith(String.CASE_INSENSITIVE_ORDER), key = { it }) { tag ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth().clickable { onShowNotes(tag) }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("#$tag", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        stringResource(R.string.mgr_notes_count, counts[tag] ?: 0),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (locked) {
                                    LockedIconButton(Icons.Filled.Edit, stringResource(R.string.mgr_rename_cd), onLocked)
                                    LockedIconButton(Icons.Filled.Delete, stringResource(R.string.delete_cd), onLocked, tint = MaterialTheme.colorScheme.error)
                                } else {
                                    IconButton(onClick = { renameOf = tag }) {
                                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.mgr_rename_cd))
                                    }
                                    IconButton(onClick = { deleteOf = tag }) {
                                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_cd), tint = MaterialTheme.colorScheme.error)
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
    val rt = renameOf
    if (rt != null) {
        var name by remember(rt) { mutableStateOf(rt) }
        AlertDialog(
            onDismissRequest = { renameOf = null },
            title = { Text(stringResource(R.string.mgr_tag_rename_title)) },
            text = {
                OutlinedTextField(
                    shape = RoundedCornerShape(20.dp),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.mgr_tag_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onRename(rt, name.trim()); renameOf = null },
                    enabled = name.trim().isNotEmpty() && !name.trim().equals(rt, ignoreCase = true)
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameOf = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    if (createOpen) {
        CreateTextDialog(
            title = stringResource(R.string.mgr_create_tag),
            label = stringResource(R.string.mgr_tag_name),
            onDismiss = { createOpen = false },
            onConfirm = { onCreate(it); createOpen = false }
        )
    }
    val dt = deleteOf
    if (dt != null) {
        AlertDialog(
            onDismissRequest = { deleteOf = null },
            title = { Text(stringResource(R.string.mgr_tag_delete_title)) },
            text = { Text(stringResource(R.string.mgr_tag_delete_msg, dt)) },
            confirmButton = {
                TextButton(onClick = { onDelete(dt); deleteOf = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteOf = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

// شاشة إدارة التصنيفات: تسمية واحدة حرة للملاحظة (شخصي/عمل) — الضغطة تفلتر الرئيسية، والقلم يعيد التسمية، والحذف يزيل التسمية فقط (الملاحظات تبقى) — مجانية للجميع
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    counts: Map<String, Int>,
    onShowNotes: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    // إنشاء تصنيف (قرار 26 سبتمبر 2026): بلا جدول مستقل — الاسم يُسند لملاحظة مختارة فورًا
    notes: List<Note> = emptyList(),
    onCreate: (String, Long) -> Unit = { _, _ -> }
) {
    var renameOf by remember { mutableStateOf<String?>(null) }
    var deleteOf by remember { mutableStateOf<String?>(null) }
    var createName by remember { mutableStateOf<String?>(null) }
    var pickForCat by remember { mutableStateOf<String?>(null) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            // داخل DrawerShell: الهيدر المثبت من الغلاف — بلا شريط داخلي (27 سبتمبر 2026)
            contentWindowInsets = WindowInsets(0),
            floatingActionButton = {
                FloatingActionButton(onClick = { createName = "" }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.mgr_create_cat))
                }
            }
        ) { pad ->
            if (counts.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                    ManageEmptyState(Icons.Filled.Edit, stringResource(R.string.cat_empty))
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(counts.keys.sortedWith(String.CASE_INSENSITIVE_ORDER), key = { it }) { cat ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth().clickable { onShowNotes(cat) }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(cat, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        stringResource(R.string.mgr_notes_count, counts[cat] ?: 0),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { renameOf = cat }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.mgr_rename_cd))
                                }
                                IconButton(onClick = { deleteOf = cat }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_cd), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val rt = renameOf
    if (rt != null) {
        var name by remember(rt) { mutableStateOf(rt) }
        AlertDialog(
            onDismissRequest = { renameOf = null },
            title = { Text(stringResource(R.string.cat_rename_title)) },
            text = {
                OutlinedTextField(
                    shape = RoundedCornerShape(20.dp),
                    value = name,
                    onValueChange = { name = it.take(64) },
                    label = { Text(stringResource(R.string.mgr_tag_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onRename(rt, name.trim()); renameOf = null },
                    enabled = name.trim().isNotEmpty() && !name.trim().equals(rt, ignoreCase = true)
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameOf = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    val cn = createName
    if (cn != null && pickForCat == null) {
        CreateTextDialog(
            title = stringResource(R.string.mgr_create_cat),
            label = stringResource(R.string.cat_field_hint),
            onDismiss = { createName = null },
            onConfirm = { pickForCat = it; }
        )
    }
    val pf = pickForCat
    if (pf != null) {
        NotePickDialog(
            title = stringResource(R.string.mgr_pick_note),
            notes = notes,
            emptyText = stringResource(R.string.mgr_empty_notes),
            onPick = { id -> onCreate(pf, id); pickForCat = null; createName = null },
            onDismiss = { pickForCat = null }
        )
    }
    val dt = deleteOf
    if (dt != null) {
        AlertDialog(
            onDismissRequest = { deleteOf = null },
            title = { Text(stringResource(R.string.cat_delete_title)) },
            text = { Text(stringResource(R.string.cat_delete_msg, dt)) },
            confirmButton = {
                TextButton(onClick = { onDelete(dt); deleteOf = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteOf = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

// شاشة إدارة التنبيهات: كل ذات الموعد بالأقرب — الضغطة تفتح الملاحظة والجرس المكسور يلغي التذكير
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    notes: List<Note>,
    onOpen: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onBack: () -> Unit,
    // إنشاء تذكير (قرار 26 سبتمبر 2026): اختيار ملاحظة بلا موعد ثم ضبطه
    candidates: List<Note> = emptyList(),
    onSetReminder: (Long, com.nota.notes.data.ReminderSpec) -> Unit = { _, _ -> },
    // القفل المرئي (27 سبتمبر 2026): العرض والفتح مجانًا — الإلغاء والإنشاء مدفوع
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    var pickOpen by remember { mutableStateOf(false) }
    var specFor by remember { mutableStateOf<Long?>(null) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    // تبويبات الماضي/اليوم/القادم (27 سبتمبر 2026): التقسيم بموعد التذكير — الماضية ما قبل الآن، والحالية باقي اليوم، والمستقبلية ما بعده
    var tab by remember { mutableIntStateOf(1) }
    val now = System.currentTimeMillis()
    val endOfToday = remember {
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val timed = remember(notes) { notes.filter { it.reminderAt != null } }
    val pastList = remember(timed, now) { timed.filter { (it.reminderAt ?: 0L) < now } }
    val currentList = remember(timed, now) { timed.filter { val a = it.reminderAt ?: 0L; a >= now && a < endOfToday } }
    val futureList = remember(timed) { timed.filter { (it.reminderAt ?: 0L) >= endOfToday } }
    val tabTitles = listOf(stringResource(R.string.mgr_tab_past), stringResource(R.string.mgr_tab_current), stringResource(R.string.mgr_tab_future))
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            // داخل DrawerShell: حواف النظام يستهلكها الغلاف الخارجي — بلا ازدواج هنا
            contentWindowInsets = WindowInsets(0),
            // تبويبات الماضي/اليوم/القادم تحت الهيدر المثبت مباشرة (27 سبتمبر 2026)
            topBar = {
                Column {
                    TabRow(selectedTabIndex = tab) {
                        tabTitles.forEachIndexed { i, t ->
                            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                        }
                    }
                }
            },
            floatingActionButton = {
                if (locked) {
                    FloatingActionButton(onClick = onLocked) {
                        Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.mgr_create_reminder))
                    }
                } else {
                    FloatingActionButton(onClick = { pickOpen = true }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.mgr_create_reminder))
                    }
                }
            }
        ) { pad ->
            val shown = when (tab) { 0 -> pastList; 2 -> futureList; else -> currentList }
            val emptyText = when (tab) {
                0 -> stringResource(R.string.mgr_empty_past)
                2 -> stringResource(R.string.mgr_empty_future)
                else -> stringResource(R.string.mgr_empty_current)
            }
            Column(Modifier.fillMaxSize().padding(pad)) {
            if (locked) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ProLockedBanner(onLocked)
                }
            }
            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    ManageEmptyState(Icons.Filled.NotificationsOff, emptyText)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown, key = { it.id }) { n ->
                        val past = (n.reminderAt ?: 0L) <= System.currentTimeMillis()
                        Card(
                            colors = if (past) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(n.id) }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = stringResource(R.string.reminder_title),
                                    tint = if (past) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(
                                        n.title.ifBlank { stringResource(R.string.untitled) },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        reminderLine(n, fmt),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (locked) {
                                    LockedIconButton(Icons.Filled.NotificationsOff, stringResource(R.string.reminder_clear), onLocked, tint = MaterialTheme.colorScheme.error)
                                } else {
                                    IconButton(onClick = { onCancel(n.id) }) {
                                        Icon(
                                            Icons.Filled.NotificationsOff,
                                            contentDescription = stringResource(R.string.reminder_clear),
                                            tint = MaterialTheme.colorScheme.error
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
    if (pickOpen && specFor == null) {
        NotePickDialog(
            title = stringResource(R.string.mgr_create_reminder),
            notes = candidates,
            emptyText = stringResource(R.string.mgr_empty_candidates),
            onPick = { id -> specFor = id },
            onDismiss = { pickOpen = false }
        )
    }
    val sf = specFor
    if (sf != null) {
        ReminderDialog(
            init = com.nota.notes.data.ReminderSpec(),
            onDismiss = { specFor = null },
            onSave = { spec -> onSetReminder(sf, spec); specFor = null; pickOpen = false },
            onClear = { specFor = null }
        )
    }
}

// سطر التذكير في الإدارة: التاريخ + التكرار + المسبق — والماضي يُعلَّم بكلمة «انتهى»
@Composable
private fun reminderLine(n: Note, fmt: SimpleDateFormat): String {
    // الوصف الموحد من القاعدة: بسيط أو مخصص (كل يومين • أيام • حتى تاريخ…)
    val spec = com.nota.notes.data.ReminderSpec(at = n.reminderAt, repeat = n.reminderRepeat,
        beforeMin = n.reminderBeforeMin, interval = n.repeatInterval, days = n.repeatDays,
        monthMode = n.repeatMonthMode, end = n.repeatEnd, endValue = n.repeatEndValue)
    val rep = com.nota.notes.ui.repeatDescribe(spec)
    val base = fmt.format(Date(n.reminderAt ?: 0L)) + " • " + rep
    return if ((n.reminderAt ?: 0L) <= System.currentTimeMillis()) "$base • " + stringResource(R.string.mgr_reminder_past)
    else base
}


// حالة الفراغ الموحدة لشاشات الإدارة: أيقونة + نص بتوسيط — الاتساق يبني الثقة
// حوار إنشاء نصي مشترك (قرار 26 سبتمبر 2026): وسم/تصنيف جديد بالاسم
@Composable
internal fun CreateTextDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                shape = RoundedCornerShape(20.dp),
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()); }, enabled = name.trim().isNotEmpty()) {
                Text(stringResource(R.string.mgr_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// منتقي ملاحظة مشترك (قرار 26 سبتمبر 2026): لإضافة مفضلة/تذكير/تصنيف من شاشات الإدارة
@Composable
internal fun NotePickDialog(
    title: String,
    notes: List<Note>,
    emptyText: String,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (notes.isEmpty()) {
                Text(emptyText, style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                    items(notes, key = { it.id }) { n ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(n.id) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                n.title.ifBlank { stringResource(R.string.untitled) },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun ManageEmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
