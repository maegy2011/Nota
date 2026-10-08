package com.nota.notes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nota.notes.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// شاشة رسائل النفس (27 سبتمبر 2026 — ترقية): محادثة المستخدم مع نفسه بأسلوب واتساب
// بحث + رؤوس تواريخ يومية + تعديل/نسخ/تحويل لملاحظة/حذف — مجانية للجميع
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SelfChatScreen(
    vm: SelfChatViewModel,
    onBack: () -> Unit
) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val ctx = LocalContext.current
    var draft by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var menuMsg by remember { mutableStateOf<com.nota.notes.data.SelfMessage?>(null) }
    var editingMsg by remember { mutableStateOf<com.nota.notes.data.SelfMessage?>(null) }
    var editText by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var forwardedToast by remember { mutableStateOf(false) }

    // البحث: فلترة لحظية بالنص — بلا استعلام جديد
    val visible = remember(messages, query) {
        val q = query.trim()
        if (q.isEmpty()) messages else messages.filter { it.text.contains(q, ignoreCase = true) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && query.isBlank()) listState.animateScrollToItem(messages.size - 1)
    }

    fun dayKey(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }
    fun dayLabel(ts: Long): String {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return when {
            c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == today ->
                ctx.getString(R.string.self_chat_today)
            c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == today - 1 ->
                ctx.getString(R.string.self_chat_yesterday)
            else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(ts))
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            // شريط البحث: يُفتح بأيقونة من الهيدر المثبت؟ — هنا صف علوي خفيف يظهر عند الطلب
            if (searchOpen) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.self_chat_search_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                    IconButton(onClick = { searchOpen = false; query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
            } else if (messages.size >= 5) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { searchOpen = true }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.self_chat_search_hint))
                    }
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.self_chat_clear))
                        }
                    }
                }
            }
            if (visible.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(64.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (query.isBlank()) stringResource(R.string.self_chat_empty)
                            else stringResource(R.string.self_chat_no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    var lastDay = ""
                    visible.forEach { m ->
                        val dk = dayKey(m.createdAt)
                        if (dk != lastDay) {
                            lastDay = dk
                            item(key = "day-$dk-${m.id}") {
                                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                                        Text(dayLabel(m.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                        item(key = m.id) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
                                    modifier = Modifier.widthIn(max = 300.dp).combinedClickable(
                                        onLongClick = { menuMsg = m },
                                        onClick = { }
                                    )
                                ) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(m.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            remember(m.createdAt) {
                                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.createdAt))
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // وضع التعديل: شريط علوي صغير فوق الإدخال + حفظ/إلغاء
            val em = editingMsg
            if (em != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.self_chat_editing), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        val t = editText.trim()
                        if (t.isNotEmpty()) vm.edit(em.id, t)
                        editingMsg = null
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save))
                    }
                    IconButton(onClick = { editingMsg = null }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
            }
            // شريط الإدخال: حقل + زر إرسال
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = if (em != null) editText else draft,
                    onValueChange = { if (em != null) editText = it else draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.self_chat_hint)) },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
                val cur = if (em != null) editText else draft
                IconButton(
                    onClick = {
                        val t = cur.trim()
                        if (t.isEmpty()) return@IconButton
                        Haptics.confirm(ctx)
                        if (em != null) { vm.edit(em.id, t); editingMsg = null }
                        else { vm.send(t); draft = "" }
                    },
                    enabled = cur.isNotBlank()
                ) {
                    Icon(if (em != null) Icons.Filled.Check else Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.self_chat_send))
                }
            }
        }
    }

    // قائمة الرسالة بالضغطة المطولة: تعديل/نسخ/تحويل لملاحظة/حذف
    val mm = menuMsg
    if (mm != null) {
        ModalBottomSheet(onDismissRequest = { menuMsg = null }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                val copyLabel = stringResource(R.string.copied)
                SheetAction(stringResource(R.string.self_chat_edit)) {
                    editText = mm.text; editingMsg = mm; menuMsg = null
                }
                SheetAction(stringResource(R.string.self_chat_copy)) {
                    runCatching {
                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("nota", mm.text))
                        android.widget.Toast.makeText(ctx, copyLabel, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    menuMsg = null
                }
                SheetAction(stringResource(R.string.self_chat_to_note)) {
                    vm.forwardToNote(mm.text) { forwardedToast = true }
                    menuMsg = null
                }
                SheetAction(stringResource(R.string.delete)) {
                    vm.delete(mm.id); menuMsg = null
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (forwardedToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(ctx, R.string.self_chat_forwarded, android.widget.Toast.LENGTH_SHORT).show()
            forwardedToast = false
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.self_chat_clear_title)) },
            text = { Text(stringResource(R.string.self_chat_clear_msg)) },
            confirmButton = { TextButton(onClick = { vm.clear(); confirmClear = false }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
