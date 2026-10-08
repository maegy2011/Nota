package com.nota.notes.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nota.notes.R
import com.nota.notes.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// سلة المهملات (27 سبتمبر 2026): المحذوف نرمًا 30 يومًا — استعادة/حذف نهائي/إفراغ + بحث + تحديد جماعي
// الحذف النهائي وحده بتأكيد مزدوج — أما النقل للسلة فتأكيد واحد يكفي (الاستعادة متاحة)
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    notes: List<Note>,
    query: String,
    onQuery: (String) -> Unit,
    deleting: Boolean,
    onBack: () -> Unit,
    onRestore: (Long) -> Unit,
    onRestoreMany: (Set<Long>) -> Unit,
    onDeleteForever: (Long) -> Unit,
    onDeleteManyForever: (Set<Long>) -> Unit,
    onEmptyTrash: () -> Unit,
    // زر القائمة الجانبية في الهيدر (27 سبتمبر 2026) — عند التحديد يتحول لمسح التحديد
    onMenu: () -> Unit = {},
) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var confirmOne by remember { mutableStateOf<Long?>(null) }
    var confirmOne2 by remember { mutableStateOf<Long?>(null) }
    var confirmBatch2 by remember { mutableStateOf<Set<Long>?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmEmpty2 by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selected.isEmpty()) stringResource(R.string.trash_title)
                        else stringResource(R.string.selected_count, selected.size)
                    )
                },
                navigationIcon = {
                    if (selected.isEmpty()) {
                        IconButton(onClick = onMenu) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu_cd))
                        }
                    } else {
                        IconButton(onClick = { selected = emptySet() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (selected.isNotEmpty()) {
                        IconButton(onClick = { onRestoreMany(selected); selected = emptySet() }) {
                            Icon(Icons.Filled.RestoreFromTrash, contentDescription = stringResource(R.string.trash_restore))
                        }
                        IconButton(onClick = { confirmBatch2 = selected }) {
                            Icon(
                                Icons.Filled.DeleteForever,
                                contentDescription = stringResource(R.string.trash_delete_forever),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (notes.isNotEmpty()) {
                        IconButton(onClick = { confirmEmpty = true }) {
                            Icon(
                                Icons.Filled.DeleteForever,
                                contentDescription = stringResource(R.string.trash_empty),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(
                value = query, onValueChange = onQuery,
                placeholder = { Text(stringResource(R.string.trash_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(if (query.isBlank()) R.string.trash_empty_state else R.string.empty_no_results),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes, key = { it.id }) { n ->
                        val sel = n.id in selected
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { selected = if (sel) selected - n.id else selected + n.id },
                                onLongClick = { selected = if (sel) selected - n.id else selected + n.id }
                            )
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (selected.isNotEmpty()) {
                                    Checkbox(
                                        checked = sel,
                                        onCheckedChange = { selected = if (it) selected + n.id else selected - n.id }
                                    )
                                }
                                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        n.title.ifBlank { stringResource(R.string.untitled) },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    if (n.body.isNotBlank()) {
                                        Text(
                                            remember(n.body) { MarkdownFormat.stripMarkdown(n.body.lines().take(2).joinToString("\n")) },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        stringResource(
                                            R.string.trash_meta_fmt,
                                            n.deletedAt?.let { fmt.format(Date(it)) } ?: "",
                                            daysLeftText(n.deletedAt)
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                IconButton(onClick = { onRestore(n.id) }) {
                                    Icon(Icons.Filled.RestoreFromTrash, contentDescription = stringResource(R.string.trash_restore))
                                }
                                IconButton(onClick = { confirmOne = n.id }) {
                                    Icon(
                                        Icons.Filled.DeleteForever,
                                        contentDescription = stringResource(R.string.trash_delete_forever),
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
    // حذف نهائي مفرد: تأكيدان متتاليان — لا رجعة بعده
    confirmOne?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmOne = null },
            title = { Text(stringResource(R.string.trash_delete_forever)) },
            text = { Text(stringResource(R.string.delete_confirm2_msg)) },
            confirmButton = {
                TextButton(onClick = { confirmOne = null; confirmOne2 = id }) {
                    Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmOne = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    confirmOne2?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmOne2 = null },
            title = { Text(stringResource(R.string.delete_confirm2_title)) },
            text = { Text(stringResource(R.string.batch_delete2_msg, 1)) },
            confirmButton = {
                TextButton(onClick = { onDeleteForever(id); confirmOne2 = null }) {
                    Text(stringResource(R.string.delete_confirm2_btn), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmOne2 = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    // الجماعي النهائي: تأكيد مزدوج ثم تنفيذ وخروج من التحديد
    confirmBatch2?.let { ids ->
        AlertDialog(
            onDismissRequest = { confirmBatch2 = null },
            title = { Text(stringResource(R.string.delete_confirm2_title)) },
            text = { Text(stringResource(R.string.batch_delete2_msg, ids.size)) },
            confirmButton = {
                TextButton(onClick = { onDeleteManyForever(ids); confirmBatch2 = null; selected = emptySet() }) {
                    Text(stringResource(R.string.delete_confirm2_btn), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmBatch2 = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    // إفراغ السلة كاملة: تأكيد مزدوج
    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text(stringResource(R.string.trash_empty)) },
            text = { Text(stringResource(R.string.trash_empty_msg)) },
            confirmButton = {
                TextButton(onClick = { confirmEmpty = false; confirmEmpty2 = true }) {
                    Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (confirmEmpty2) {
        AlertDialog(
            onDismissRequest = { confirmEmpty2 = false },
            title = { Text(stringResource(R.string.delete_confirm2_title)) },
            text = { Text(stringResource(R.string.trash_empty_msg2)) },
            confirmButton = {
                TextButton(onClick = { onEmptyTrash(); confirmEmpty2 = false }) {
                    Text(stringResource(R.string.delete_confirm2_btn), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty2 = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.delete_title)) },
            text = {},
            confirmButton = {},
        )
    }
}

// المتبقي حتى الحذف النهائي (30 يومًا من deletedAt) — بصياغة عربية سليمة
@Composable
private fun daysLeftText(deletedAt: Long?): String {
    if (deletedAt == null) return ""
    val left = ((deletedAt + 30L * 24 * 60 * 60 * 1000 - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    val en = Locale.getDefault().language == "en"
    if (en) return if (left == 1) "1 day left" else "$left days left"
    return when (left) {
        0 -> "تُحذف اليوم"
        1 -> "يوم واحد متبقٍ"
        2 -> "يومان متبقيان"
        in 3..10 -> "$left أيام متبقية"
        else -> "$left يومًا متبقٍ"
    }
}
