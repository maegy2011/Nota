package com.nota.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import com.nota.notes.R
import com.nota.notes.data.Folder
import com.nota.notes.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// شاشة المجلدات (ميزة مدفوعة): إنشاء بلون + عداد + تفاصيل الملاحظات + إعادة تسمية/حذف
// الدخول عبر بوابة واحدة في App: بلا صلاحية folders → PaywallScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onBack: () -> Unit,
    vm: FoldersViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
    // القفل المرئي (27 سبتمبر 2026): عرض المجلدات وفتحها مجانًا — الإنشاء والتعديل والقوائم مدفوعة
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    val folders by vm.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    // إحصائية كل مجلد للبطاقات (27 سبتمبر 2026): الإجمالي + المفضلة + التنبيهات + آخر نشاط
    val stats by vm.stats.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var createName by rememberSaveable { mutableStateOf("") }
    var createColor by rememberSaveable { mutableStateOf(0) }
    var menuOf by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    var renamingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameName by rememberSaveable { mutableStateOf("") }
    var renameColor by rememberSaveable { mutableStateOf(0) }
    var openedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val renaming = folders.find { it.id == renamingId }
    val opened = folders.find { it.id == openedId }
    val defaultId by vm.defaultFolderId.collectAsStateWithLifecycle()
    val unlockedIds by vm.unlockedIds.collectAsStateWithLifecycle()
    // المجلد المقفل يتطلب تحديًا عند كل فتح — إلا المفتوح بتحدٍ ناجح هذه الجلسة
    val openedLocked = opened != null && opened.locked && opened.id !in unlockedIds
    // زر رجوع الهاتف داخل تفاصيل مجلد يعود للقائمة — بلا هيدر داخلي (27 سبتمبر 2026)
    BackHandler(enabled = openedId != null) { openedId = null }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            // داخل DrawerShell: الهيدر المثبت من الغلاف — بلا شريط داخلي (27 سبتمبر 2026)
            contentWindowInsets = WindowInsets(0),
            floatingActionButton = {
                // الإنشاء في حوار بدل صف دائم — القائمة تأخذ الشاشة كاملة
                FloatingActionButton(
                    onClick = { if (locked) onLocked() else showCreate = true },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        if (locked) Icons.Filled.Lock else Icons.Filled.Add,
                        contentDescription = stringResource(R.string.mgr_folder_add)
                    )
                }
            }
        ) { p ->
            if (opened != null) {
                if (openedLocked) {
                    // ستار القفل بدل المحتوى — العناوين لا تُعرض قبل التحدي
                    LockedFolderGate(
                        name = opened.name,
                        authVm = authVm,
                        onUnlocked = { vm.markUnlocked(opened.id) },
                        onBack = { openedId = null },
                        modifier = Modifier.padding(p)
                    )
                } else {
                    // تفاصيل المجلد: ملاحظاته فقط + إزالة أي منها (تعود بلا مجلد)
                    FolderDetail(
                        folderId = opened.id,
                        notesFlow = vm.notesIn(opened.id),
                        onRemove = { if (locked) onLocked() else vm.moveNote(it, null) },
                        modifier = Modifier.padding(p)
                    )
                }
            } else {
                Column(Modifier.padding(p).padding(horizontal = 16.dp).fillMaxSize()) {
                    if (locked) {
                        ProLockedBanner(onLocked)
                    }
                    if (folders.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                            ManageEmptyState(
                                Icons.Filled.Folder,
                                stringResource(R.string.folders_empty)
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(folders, key = { it.id }) { f ->
                                val st = stats[f.id]
                                val isDefault = (f.id == defaultId)
                                // بطاقة المجلد (27 سبتمبر 2026): أيقونة ملوّنة + اسم + سطر الإحصائية + آخر نشاط + شارات — والأفعال الخمسة في قائمة ⋮ مسماة
                                val fColored = f.color in 1 until NotePalette.size
                                val fBg = if (fColored) noteCardBg(f.color) else MaterialTheme.colorScheme.surfaceContainer
                                val fOn = if (fColored) noteCardOn(f.color) else MaterialTheme.colorScheme.onSurface
                                val fSub = if (fColored) noteCardSub(fOn) else MaterialTheme.colorScheme.onSurfaceVariant
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = fBg, contentColor = fOn),
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { openedId = f.id }
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // أيقونة المجلد في دائرة بلون مميز — هوية بصرية لكل مجلد
                                        Box(
                                            Modifier.size(44.dp).clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                f.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            // سطر الإحصائية: الإجمالي • المفضلة • التنبيهات
                                            Text(
                                                stringResource(
                                                    R.string.folder_stat_line,
                                                    st?.total ?: 0, st?.favorites ?: 0, st?.reminders ?: 0
                                                ) +
                                                    (if (f.pinned) " • " + stringResource(R.string.pinned_label) else "") +
                                                    (if (f.locked) " • " + stringResource(R.string.locked_label) else ""),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = fSub
                                            )
                                            // آخر نشاط في المجلد — أو شارة الافتراضي
                                            if (st != null && st.total > 0) {
                                                Text(
                                                    stringResource(R.string.folder_last_active, folderLastActiveLabel(st.lastActive)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = fSub
                                                )
                                            }
                                            // شارة المجلد الافتراضي — ملاحظاتك الجديدة تُعرض عليه أولًا
                                            if (isDefault) {
                                                Text(
                                                    stringResource(R.string.default_folder_badge),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = fOn
                                                )
                                            }
                                        }
                                        Box {
                                            IconButton(onClick = { menuOf = f.id }) {
                                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_cd))
                                            }
                                            DropdownMenu(
                                                expanded = (menuOf == f.id),
                                                onDismissRequest = { menuOf = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(if (f.pinned) R.string.unpin_cd else R.string.pin_cd)) },
                                                    trailingIcon = if (locked) ({ Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) else null,
                                                    onClick = { menuOf = null; if (locked) onLocked() else vm.togglePin(f.id) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(if (isDefault) R.string.unset_default_cd else R.string.set_default_cd)) },
                                                    trailingIcon = if (locked) ({ Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) else null,
                                                    onClick = { menuOf = null; if (locked) onLocked() else vm.setDefault(if (isDefault) null else f.id) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.folder_rename)) },
                                                    trailingIcon = if (locked) ({ Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) else null,
                                                    onClick = {
                                                        menuOf = null
                                                        if (locked) { onLocked(); return@DropdownMenuItem }
                                                        renamingId = f.id
                                                        renameName = f.name
                                                        renameColor = f.color.coerceIn(NotePalette.indices)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(if (f.locked) R.string.folder_unlock_cd else R.string.folder_lock_cd)) },
                                                    trailingIcon = if (locked) ({ Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) else null,
                                                    onClick = { menuOf = null; if (locked) onLocked() else vm.toggleLock(f.id) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.delete_cd), color = MaterialTheme.colorScheme.error) },
                                                    trailingIcon = if (locked) ({ Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) else null,
                                                    onClick = { menuOf = null; if (locked) onLocked() else pendingDelete = f.id }
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
    }
    // حوار الإنشاء: اسم + لون — يُفتح من زر + بدل صف دائم يأكل الشاشة
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.mgr_folder_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        shape = RoundedCornerShape(20.dp),
                        value = createName,
                        onValueChange = { createName = it },
                        placeholder = { Text(stringResource(R.string.folder_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.folder_color),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FolderColorDots(selected = createColor, onPick = { createColor = it })
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = createName.trim()
                        if (clean.isNotEmpty()) {
                            scope.launch { vm.create(clean, createColor) }
                        }
                        createName = ""; createColor = 0; showCreate = false
                    },
                    enabled = createName.trim().isNotEmpty()
                ) {
                    Text(stringResource(R.string.folder_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.folder_delete_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { vm.delete(id) }
                    if (openedId == id) openedId = null
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    renaming?.let { f ->
        AlertDialog(
            onDismissRequest = { renamingId = null },
            title = { Text(stringResource(R.string.folder_rename_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        shape = RoundedCornerShape(20.dp),
                        value = renameName,
                        onValueChange = { renameName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            stringResource(R.string.folder_color),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FolderColorDots(selected = renameColor, onPick = { renameColor = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameName.trim().isNotEmpty()) {
                        vm.saveNameAndColor(f.id, renameName, renameColor)
                    }
                    renamingId = null
                }) {
                    Text(stringResource(R.string.folder_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// تفاصيل مجلد واحد: قائمة ملاحظاته + إزالة أي منها (تبقى بلا مجلد)
// تسمية آخر نشاط: اليوم/أمس/التاريخ — للمجلدات (27 سبتمبر 2026)
@Composable
private fun folderLastActiveLabel(lastActive: Long): String {
    val cal = java.util.Calendar.getInstance()
    val today = cal.get(java.util.Calendar.DAY_OF_YEAR)
    val thisYear = cal.get(java.util.Calendar.YEAR)
    cal.timeInMillis = lastActive
    val d = cal.get(java.util.Calendar.DAY_OF_YEAR)
    val y = cal.get(java.util.Calendar.YEAR)
    return when {
        y == thisYear && d == today -> stringResource(R.string.folder_last_today)
        (y == thisYear && d == today - 1) || (y == thisYear - 1 && today == 1 && d >= 365) ->
            stringResource(R.string.folder_last_yesterday)
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(lastActive))
    }
}

@Composable
private fun FolderDetail(
    folderId: Long,
    notesFlow: Flow<List<Note>>,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by notesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    if (notes.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(bottom = 80.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.folder_empty_notes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        // إحصائية المجلد من قائمته مباشرة: الإجمالي • المفضلة • التنبيهات (27 سبتمبر 2026)
        val favCount = notes.count { it.favorite }
        val remCount = notes.count { it.reminderAt != null }
        val dateFmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
        LazyColumn(
            modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.folder_stat_line, notes.size, favCount, remCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
            items(notes, key = { it.id }) { n ->
                val cardBg = noteCardBg(n.color)
                val cardOn = noteCardOn(n.color)
                val cardSub = noteCardSub(cardOn)
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg, contentColor = cardOn),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                n.title.ifBlank { stringResource(R.string.untitled) },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = cardOn
                            )
                            if (n.body.isNotBlank()) {
                                Text(
                                    n.body.take(140),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = cardSub,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            // سطر الميتا: التاريخ + شارات المفضلة/التثبيت/التنبيه
                            Row(
                                Modifier.padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    dateFmt.format(Date(n.updatedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cardSub
                                )
                                if (n.favorite) Icon(Icons.Filled.Star, contentDescription = null, tint = cardOn, modifier = Modifier.size(14.dp))
                                if (n.pinned) Icon(Icons.Filled.PushPin, contentDescription = null, tint = cardOn, modifier = Modifier.size(14.dp))
                                if (n.reminderAt != null) Icon(Icons.Filled.Notifications, contentDescription = null, tint = cardOn, modifier = Modifier.size(14.dp))
                                if (n.locked) Icon(Icons.Filled.Lock, contentDescription = null, tint = cardOn, modifier = Modifier.size(14.dp))
                            }
                        }
                        IconButton(onClick = { onRemove(n.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.folder_remove_note_cd), tint = cardOn)
                        }
                    }
                }
            }
        }
    }
}

// نقاط لون المجلد — نفس لوحة Sticky Notes (0 = افتراضي)
@Composable
private fun FolderColorDots(selected: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NotePalette.indices.forEach { i ->
            val preview = if (i == 0) MaterialTheme.colorScheme.surface else noteCardBg(i)
            val isSel = (selected == i)
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onPick(i) },
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
    }
}

// بوابة المجلد المقفل: ستار بدل المحتوى + تحدي PIN/بصمة فوري — الرفض يرجع للقائمة
@Composable
private fun LockedFolderGate(
    name: String,
    authVm: AuthViewModel,
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(true) }
    Column(
        modifier.fillMaxSize().padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            androidx.compose.material.icons.Icons.Filled.Lock,
            contentDescription = stringResource(R.string.locked_cd),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            stringResource(R.string.folder_locked_msg, name),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
    if (show) {
        LockChallenge(
            title = name,
            authVm = authVm,
            onUnlocked = { show = false; onUnlocked() },
            onDismiss = { show = false; onBack() }
        )
    }
}
