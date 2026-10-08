package com.nota.notes.ui

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nota.notes.R
import com.nota.notes.data.ChecklistItem
import com.nota.notes.data.NoteAttachment
import com.nota.notes.data.NoteBgFiles
import com.nota.notes.data.NoteMediaFiles
import kotlinx.coroutines.launch

// صف ألوان الملاحظة الأصلي في NoteColors.kt — لا تكرار هنا

// رقاقة اختيار (تذكير/تكرار/قبل): محدد بخلفية خفيفة وإطار
@Composable
fun ReminderChip(text: String, isSel: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme.primary
    Box(
        Modifier.clip(RoundedCornerShape(16.dp))
            .border(1.dp, c.copy(alpha = if (isSel) 1f else 0.4f), RoundedCornerShape(16.dp))
            .background(if (isSel) c.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

// طبقة صورة الخلفية: باهتة تحت ستار اللون — المفقود (جهاز جديد) = لون فقط بلا خطأ
@Composable
fun NoteBgLayer(name: String?) {
    if (name.isNullOrBlank()) return
    val ctx = LocalContext.current
    var bmp by remember(name) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(name) { bmp = NoteBgFiles.load(ctx, name) }
    bmp?.let {
        Image(
            it.asImageBitmap(), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.28f
        )
    }
}

// صف زر +: أيقونة + تسمية بملء العرض + وصف اختياري تحتها (قائمة + 27 سبتمبر 2026)
@Composable
fun ContentRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    description: String = "",
    // 28 سبتمبر 2026: وصف للقارئ الآلي — الأيقونة ظاهرية لكن الصف كله يحتاج تسمية
    contentDescription: String = label
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// صف الوسوم: رقائق العرض + حقل إضافة في التحرير — مكرر بلا حدود
@Composable
fun TagEditorRow(
    tags: List<String>,
    readOnly: Boolean,
    tint: Color,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    fun submit() {
        val t = input.trim().take(32)
        if (t.isNotBlank() && !tags.contains(t)) onChange(tags + t)
        input = ""
    }
    Column(modifier.fillMaxWidth()) {
        if (tags.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    key(tag) {
                        AssistChip(
                            onClick = {},
                            label = { Text(tag, color = tint) },
                            trailingIcon = if (!readOnly) {
                                {
                                    IconButton(
                                        onClick = { onChange(tags - tag) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close, contentDescription = stringResource(R.string.tag_remove_cd),
                                            tint = tint, modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
        if (!readOnly) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(32) },
                    label = { Text(stringResource(R.string.tag_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { submit() }, enabled = input.isNotBlank()) {
                    Text(stringResource(R.string.tag_add))
                }
            }
        }
    }
}

// شبكة المرفقات: مصغرات الصور + صفوف الصوت — العرض دائمًا، والحذف في التحرير فقط
@Composable
fun AttachmentGrid(
    attachments: List<NoteAttachment>,
    readOnly: Boolean,
    tint: Color,
    onDelete: (NoteAttachment) -> Unit,
    // 28 سبتمبر 2026: عارض داخلي بدل فتح تطبيق خارجي — onOpenImage يفتح العارض المدمج
    onOpenImage: ((NoteAttachment) -> Unit)? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val images = remember(attachments) {
        attachments.filter { it.kind == NoteAttachment.KIND_IMAGE || it.kind == NoteAttachment.KIND_DRAWING }
    }
    val audios = remember(attachments) {
        attachments.filter { it.kind == NoteAttachment.KIND_AUDIO }
    }
    val files = remember(attachments) {
        attachments.filter { it.kind == NoteAttachment.KIND_FILE }
    }
    fun openViewer(a: NoteAttachment) {
        // العارض الداخلي إن توفّر — وإلا فالتطبيق الخارجي كحل احتياطي
        val internal = onOpenImage
        if (internal != null && (a.kind == NoteAttachment.KIND_IMAGE || a.kind == NoteAttachment.KIND_DRAWING)) {
            internal(a); return
        }
        scope.launch {
            val tmp = NoteMediaFiles.decryptedTemp(ctx, a.file)
            if (tmp == null) return@launch
            runCatching {
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", tmp)
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure {
                Toast.makeText(ctx, ctx.getString(R.string.content_no_viewer), Toast.LENGTH_SHORT).show()
            }
        }
    }
    if (images.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            images.forEach { a ->
                key(a.id) { ThumbItem(attachment = a, readOnly = readOnly, onView = { openViewer(a) }, onDelete = { onDelete(a) }) }
            }
        }
    }
    audios.forEach { a ->
        key(a.id) { AudioRow(attachment = a, readOnly = readOnly, tint = tint, onDelete = { onDelete(a) }) }
    }
    files.forEach { a ->
        key(a.id) { FileRow(attachment = a, readOnly = readOnly, tint = tint, onDelete = { onDelete(a) }) }
    }
}

// صف ملف عام: أيقونة حسب الامتداد + الاسم + الحجم — الفتح بتطبيق خارجي عبر نسخة مؤقتة مفكوكة (قائمة + 27 سبتمبر 2026)
@Composable
private fun FileRow(
    attachment: NoteAttachment,
    readOnly: Boolean,
    tint: Color,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var size by remember(attachment.file) { mutableStateOf<String?>(null) }
    LaunchedEffect(attachment.file) {
        size = NoteMediaFiles.fileSize(ctx, attachment.file)?.let { fmtBytes(it) }
    }
    val ext = attachment.file.substringAfterLast('.', "").lowercase()
    val icon = when (ext) {
        "pdf" -> Icons.Filled.PictureAsPdf
        "doc", "docx", "odt", "rtf", "txt" -> Icons.Filled.Description
        "xls", "xlsx", "csv", "ods" -> Icons.Filled.TableChart
        "ppt", "pptx", "odp" -> Icons.Filled.Slideshow
        "zip", "rar", "7z" -> Icons.Filled.FolderZip
        else -> Icons.Filled.InsertDriveFile
    }
    val mime = when (ext) {
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "csv" -> "text/csv"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "zip" -> "application/zip"
        else -> "*/*"
    }
    fun open() {
        scope.launch {
            val tmp = NoteMediaFiles.decryptedTemp(ctx, attachment.file) ?: return@launch
            runCatching {
                val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", tmp)
                ctx.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, mime)
                        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure {
                android.widget.Toast.makeText(ctx, ctx.getString(R.string.content_no_viewer), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { open() }) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Column(Modifier.weight(1f).clickable { open() }) {
            Text(attachment.file, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
            Text(
                (size ?: "…") + " • " + ext.uppercase(),
                style = MaterialTheme.typography.labelSmall, color = tint.copy(alpha = 0.7f)
            )
        }
        if (!readOnly) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_task_del), tint = tint.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun ThumbItem(
    attachment: NoteAttachment,
    readOnly: Boolean,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    var bmp by remember(attachment.file) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(attachment.file) { bmp = NoteMediaFiles.load(ctx, attachment.file) }
    Box(
        Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onView() }
    ) {
        val b = bmp
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!readOnly) {
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete_cd))
            }
        }
    }
}

@Composable
private fun AudioRow(
    attachment: NoteAttachment,
    readOnly: Boolean,
    tint: Color,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var duration by remember(attachment.file) { mutableStateOf<String?>(null) }
    var player by remember(attachment.file) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(attachment.file) { mutableStateOf(false) }
    LaunchedEffect(attachment.file) { duration = NoteMediaFiles.audioDuration(ctx, attachment.file) }
    DisposableEffect(attachment.file) {
        onDispose {
            runCatching { player?.stop() }
            player?.release()
            player = null
        }
    }
    fun toggle() {
        val p = player
        if (p != null) {
            runCatching { p.stop() }
            p.release()
            player = null
            playing = false
            return
        }
        scope.launch {
            val tmp = NoteMediaFiles.decryptedTemp(ctx, attachment.file) ?: return@launch
            val mp = MediaPlayer()
            player = mp
            runCatching {
                mp.setDataSource(tmp.absolutePath)
                mp.setOnPreparedListener { prepared ->
                    if (prepared === player) {
                        prepared.start()
                        playing = true
                    }
                }
                mp.setOnCompletionListener { done ->
                    done.release()
                    if (player === done) {
                        player = null
                        playing = false
                    }
                }
                mp.prepareAsync()
            }.onFailure {
                runCatching { mp.release() }
                if (player === mp) {
                    player = null
                    playing = false
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { toggle() }) {
            Icon(
                if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.audio_play), tint = tint
            )
        }
        Text(
            stringResource(R.string.audio_clip) + (duration?.let { " • $it" } ?: ""),
            style = MaterialTheme.typography.labelMedium, color = tint,
            modifier = Modifier.weight(1f)
        )
        if (!readOnly) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_task_del), tint = tint.copy(alpha = 0.7f))
            }
        }
    }
}

// كتلة المهام: الصف كله قابل للنقر (قانون فيتس) + شطب عند الإنجاز + إعادة تسمية + حذف + حقل إضافة
@Composable
fun ChecklistBlock(
    tasks: List<ChecklistItem>,
    readOnly: Boolean,
    tint: Color,
    onToggle: (ChecklistItem) -> Unit,
    onAdd: (String) -> Unit,
    onRename: (ChecklistItem, String) -> Unit,
    onDelete: (Long) -> Unit,
    focusReq: FocusRequester
) {
    var newText by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ChecklistItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    fun submit() {
        val t = newText.trim().take(200)
        if (t.isNotBlank()) {
            onAdd(t)
            newText = ""
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        tasks.forEach { t ->
            key(t.id) {
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(enabled = !readOnly) { onToggle(t) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = t.done,
                        onCheckedChange = { if (!readOnly) onToggle(t) },
                        enabled = !readOnly
                    )
                    Text(
                        t.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = tint,
                            textDecoration = if (t.done) TextDecoration.LineThrough else null
                        ),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    if (!readOnly) {
                        IconButton(onClick = { renameTarget = t; renameText = t.text }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.content_task_rename), tint = tint.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = { onDelete(t.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_task_del), tint = tint.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
        if (!readOnly) {
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it.take(200) },
                label = { Text(stringResource(R.string.content_task_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().focusRequester(focusReq)
            )
        }
    }
    val rt = renameTarget
    if (rt != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.content_task_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(200) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = renameText.trim()
                        if (t.isNotBlank()) onRename(rt, t)
                        renameTarget = null
                    }
                ) { Text(stringResource(R.string.save_cd)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

// ===== الحوارات: تسجيل صوتي + لوحة رسم + ضبط التذكير =====

// حوار التسجيل الصوتي: إذن عند التسجيل فقط + سقف 30 دقيقة/32MB يحفظ تلقائيًا + إعادة المحاولة تمسح الفاشل أولًا
@Composable
fun RecordingDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val ctx = LocalContext.current
    var rec by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var seconds by remember { mutableStateOf(0) }
    var err by remember { mutableStateOf<String?>(null) }
    var askedOnce by remember { mutableStateOf(false) }

    fun stopRec(save: Boolean) {
        val r = rec
        rec = null
        if (r != null) {
            runCatching { r.stop() }
            runCatching { r.release() }
        }
        val f = fileName
        fileName = null
        if (save && seconds > 0 && f != null) {
            onSave(f)
        } else {
            NoteMediaFiles.delete(ctx, f)
        }
        seconds = 0
    }

    fun startRec() {
        err = null
        NoteMediaFiles.delete(ctx, fileName)
        val name = NoteMediaFiles.newAudioFile(ctx)
        val r = android.media.MediaRecorder()
        try {
            r.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(NoteMediaFiles.file(ctx, name).absolutePath)
            r.setMaxDuration(30 * 60 * 1000)
            r.setMaxFileSize(32L * 1024 * 1024)
            r.setOnInfoListener { _, what, _ ->
                if (what == android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                ) {
                    stopRec(true)
                    onDismiss()
                }
            }
            r.prepare()
            r.start()
            fileName = name
            seconds = 0
            rec = r
        } catch (e: Exception) {
            runCatching { r.release() }
            NoteMediaFiles.delete(ctx, name)
            err = ctx.getString(R.string.rec_failed)
        }
    }

    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRec()
            return@rememberLauncherForActivityResult
        }
        val act = ctx as? android.app.Activity
        val permanent = askedOnce && act != null &&
            !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(act, android.Manifest.permission.RECORD_AUDIO)
        err = ctx.getString(if (permanent) R.string.rec_denied_forever else R.string.rec_denied)
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            val r = rec
            rec = null
            if (r != null) {
                runCatching { r.stop() }
                runCatching { r.release() }
            }
            NoteMediaFiles.delete(ctx, fileName)
        }
    }
    LaunchedEffect(rec) {
        while (rec != null) {
            kotlinx.coroutines.delay(1000)
            seconds++
        }
    }

    fun requestOrStart() {
        val ok = androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (ok) startRec() else {
            askedOnce = true
            permLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    val mm = "%d:%02d".format(seconds / 60, seconds % 60)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = {
            if (rec != null) stopRec(false)
            onDismiss()
        },
        title = { Text(stringResource(R.string.rec_title)) },
        text = {
            Column {
                Text(mm, style = MaterialTheme.typography.headlineMedium)
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            if (rec != null) {
                TextButton(onClick = { stopRec(true); onDismiss() }) { Text(stringResource(R.string.rec_stop)) }
            } else {
                TextButton(onClick = { requestOrStart() }) { Text(stringResource(R.string.rec_start)) }
            }
        },
        dismissButton = {
            Row {
                if (rec == null && err != null) {
                    TextButton(onClick = { requestOrStart() }) { Text(stringResource(R.string.rec_retry)) }
                }
                TextButton(onClick = { if (rec != null) stopRec(false) else NoteMediaFiles.delete(ctx, fileName); onDismiss() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

// لوحة الرسم: ضربات (نقاط + لون + عرض بالبكسل) + تراجع + مسح + تصدير PNG بخلفية بيضاء داخل كوروتين
private data class DrawStroke(val points: List<androidx.compose.ui.geometry.Offset>, val color: Color, val widthPx: Float)

@Composable
fun DrawingDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var strokes by remember { mutableStateOf(listOf<DrawStroke>()) }
    var current by remember { mutableStateOf<List<androidx.compose.ui.geometry.Offset>?>(null) }
    var color by remember { mutableStateOf(Color.Black) }
    var thick by remember { mutableStateOf(false) }
    var canvasPx by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var saving by remember { mutableStateOf(false) }
    val widthPx = if (thick) 12f else 4f

    fun export() {
        val size = canvasPx
        if (size.width <= 0 || size.height <= 0 || strokes.isEmpty()) return
        saving = true
        scope.launch {
            val file = runCatching {
                val bmp = android.graphics.Bitmap.createBitmap(size.width, size.height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                for (s in strokes) {
                    if (s.points.size < 2) continue
                    val paint = android.graphics.Paint().apply {
                        this.color = s.color.toArgb()
                        strokeWidth = s.widthPx
                        style = android.graphics.Paint.Style.STROKE
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        isAntiAlias = true
                    }
                    val path = android.graphics.Path()
                    path.moveTo(s.points[0].x, s.points[0].y)
                    for (i in 1 until s.points.size) path.lineTo(s.points[i].x, s.points[i].y)
                    canvas.drawPath(path, paint)
                }
                NoteMediaFiles.saveDrawing(ctx, bmp).also { bmp.recycle() }
            }.getOrNull()
            saving = false
            if (file != null) onSave(file) else {
                Toast.makeText(ctx, ctx.getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.draw_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                        .onSizeChanged { canvasPx = it }
                        .pointerInput(color, thick) {
                            var pts = emptyList<androidx.compose.ui.geometry.Offset>()
                            detectDragGestures(
                                onDragStart = { off ->
                                    pts = listOf(off)
                                    current = pts
                                },
                                onDragEnd = {
                                    if (pts.size > 1) strokes = strokes + DrawStroke(pts, color, widthPx)
                                    pts = emptyList()
                                    current = null
                                },
                                onDragCancel = {
                                    pts = emptyList()
                                    current = null
                                },
                                onDrag = { change, _ ->
                                    pts = pts + change.position
                                    current = pts
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth()) {
                        for (s in strokes) {
                            if (s.points.size < 2) continue
                            drawPath(
                                path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(s.points[0].x, s.points[0].y)
                                    for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                                },
                                color = s.color,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = s.widthPx, join = androidx.compose.ui.graphics.StrokeJoin.Round, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                        current?.let { pts ->
                            if (pts.size > 1) {
                                drawPath(
                                    path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(pts[0].x, pts[0].y)
                                        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                                    },
                                    color = color,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = widthPx, join = androidx.compose.ui.graphics.StrokeJoin.Round, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Color.Black, Color.Red, Color.Blue, Color(0xFF2E7D32)).forEach { c ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(c)
                                .then(if (c == color) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                .clickable { color = c }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { thick = false }) { Text(stringResource(R.string.draw_thin)) }
                    TextButton(onClick = { thick = true }) { Text(stringResource(R.string.draw_thick)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { strokes = strokes.dropLast(1) }, enabled = strokes.isNotEmpty()) {
                        Text(stringResource(R.string.draw_undo))
                    }
                    TextButton(onClick = { strokes = emptyList(); current = null }) {
                        Text(stringResource(R.string.draw_clear))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { export() }, enabled = strokes.isNotEmpty() && !saving) {
                        Text(stringResource(R.string.draw_save))
                    }
                }
            }
        }
    }
}

// حوار ضبط التذكير: تاريخ + وقت + تكرار (بسيط/مخصص) + إشعار مسبق — ناتجه ReminderSpec جاهز للجدولة
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    init: com.nota.notes.data.ReminderSpec,
    onDismiss: () -> Unit,
    onSave: (com.nota.notes.data.ReminderSpec) -> Unit,
    onClear: () -> Unit
) {
    val cal = remember(init) {
        java.util.Calendar.getInstance().apply { init.at?.let { timeInMillis = it } }
    }
    var dateMs by remember(init) {
        mutableStateOf(init.at?.let {
            val c = java.util.Calendar.getInstance().apply { timeInMillis = it }
            c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
            c.timeInMillis
        })
    }
    var hour by remember(init) { mutableStateOf(cal.get(java.util.Calendar.HOUR_OF_DAY)) }
    var minute by remember(init) { mutableStateOf(cal.get(java.util.Calendar.MINUTE)) }
    var repeat by remember(init) { mutableStateOf(init.repeat) }
    var interval by remember(init) { mutableStateOf(init.interval.coerceIn(1, 99).toString()) }
    var days by remember(init) {
        mutableStateOf(init.days.split(",").mapNotNull { it.trim().toIntOrNull()?.takeIf { d -> d in 1..7 } }.toSet())
    }
    var monthMode by remember(init) { mutableStateOf(init.monthMode) }
    var end by remember(init) { mutableStateOf(init.end) }
    var endDateMs by remember(init) {
        mutableStateOf(if (init.end == com.nota.notes.data.ReminderSpec.END_UNTIL && init.endValue > 0) init.endValue else null as Long?)
    }
    var endCount by remember(init) {
        mutableStateOf(if (init.end == com.nota.notes.data.ReminderSpec.END_COUNT && init.endValue > 0) init.endValue.toString() else "10")
    }
    var beforeMin by remember(init) { mutableStateOf(init.beforeMin) }
    var dateDlg by remember { mutableStateOf(false) }
    var timeDlg by remember { mutableStateOf(false) }
    var endDateDlg by remember { mutableStateOf(false) }

    val dateFmt = remember { java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()) }
    fun atMillis(): Long? {
        val d = dateMs ?: return null
        val c = java.util.Calendar.getInstance().apply {
            timeInMillis = d
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
        }
        return c.timeInMillis
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderChip(
                        text = dateMs?.let { dateFmt.format(java.util.Date(it)) } ?: stringResource(R.string.reminder_pick_date),
                        isSel = dateMs != null,
                        onClick = { dateDlg = true }
                    )
                    ReminderChip(
                        text = "%02d:%02d".format(hour, minute),
                        isSel = dateMs != null,
                        onClick = { timeDlg = true }
                    )
                }
                Text(stringResource(R.string.repeat_title), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                val reps = listOf(
                    "none" to stringResource(R.string.repeat_once),
                    "daily" to stringResource(R.string.repeat_daily),
                    "weekly" to stringResource(R.string.repeat_weekly),
                    "monthly" to stringResource(R.string.repeat_monthly),
                    "yearly" to stringResource(R.string.repeat_yearly)
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    reps.forEach { (v, label) ->
                        ReminderChip(text = label, isSel = repeat == v, onClick = { repeat = v })
                    }
                }
                if (repeat != "none") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.repeat_every), modifier = Modifier.padding(end = 8.dp))
                        OutlinedTextField(
                            value = interval,
                            onValueChange = { interval = it.filter { ch -> ch.isDigit() }.take(2) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.width(72.dp)
                        )
                    }
                    if (repeat == "weekly") {
                        Text(stringResource(R.string.repeat_weekdays), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf(7, 1, 2, 3, 4, 5, 6).forEach { d ->
                                ReminderChip(
                                    text = weekdayShortStrLocal(d),
                                    isSel = d in days,
                                    onClick = { days = if (d in days) days - d else days + d }
                                )
                            }
                        }
                    }
                    if (repeat == "monthly") {
                        Text(stringResource(R.string.repeat_month_mode), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            ReminderChip(
                                text = stringResource(R.string.repeat_month_same),
                                isSel = monthMode == com.nota.notes.data.ReminderSpec.MONTH_SAME,
                                onClick = { monthMode = com.nota.notes.data.ReminderSpec.MONTH_SAME }
                            )
                            ReminderChip(
                                text = stringResource(R.string.repeat_month_nth),
                                isSel = monthMode == com.nota.notes.data.ReminderSpec.MONTH_NTH,
                                onClick = { monthMode = com.nota.notes.data.ReminderSpec.MONTH_NTH }
                            )
                        }
                    }
                    Text(stringResource(R.string.repeat_end), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        ReminderChip(
                            text = stringResource(R.string.repeat_end_forever),
                            isSel = end == com.nota.notes.data.ReminderSpec.END_FOREVER,
                            onClick = { end = com.nota.notes.data.ReminderSpec.END_FOREVER }
                        )
                        ReminderChip(
                            text = endDateMs?.let { dateFmt.format(java.util.Date(it)) } ?: stringResource(R.string.repeat_end_until),
                            isSel = end == com.nota.notes.data.ReminderSpec.END_UNTIL,
                            onClick = { end = com.nota.notes.data.ReminderSpec.END_UNTIL; endDateDlg = true }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        ReminderChip(
                            text = stringResource(R.string.repeat_end_count),
                            isSel = end == com.nota.notes.data.ReminderSpec.END_COUNT,
                            onClick = { end = com.nota.notes.data.ReminderSpec.END_COUNT }
                        )
                        if (end == com.nota.notes.data.ReminderSpec.END_COUNT) {
                            OutlinedTextField(
                                value = endCount,
                                onValueChange = { endCount = it.filter { ch -> ch.isDigit() }.take(3) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.width(72.dp).padding(start = 8.dp)
                            )
                            Text(stringResource(R.string.repeat_end_times), modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
                Text(stringResource(R.string.before_title), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    listOf(
                        0L to stringResource(R.string.before_at),
                        10L to stringResource(R.string.before_10m),
                        60L to stringResource(R.string.before_1h),
                        1440L to stringResource(R.string.before_1d)
                    ).forEach { (v, label) ->
                        ReminderChip(text = label, isSel = beforeMin == v, onClick = { beforeMin = v })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val at = atMillis() ?: return@TextButton
                    val order = listOf(7, 1, 2, 3, 4, 5, 6)
                    onSave(
                        com.nota.notes.data.ReminderSpec(
                            at = at,
                            repeat = repeat,
                            beforeMin = beforeMin,
                            interval = interval.toIntOrNull()?.coerceIn(1, 99) ?: 1,
                            days = order.filter { it in days }.joinToString(","),
                            monthMode = monthMode,
                            end = end,
                            endValue = when (end) {
                                com.nota.notes.data.ReminderSpec.END_UNTIL -> endDateMs ?: 0L
                                com.nota.notes.data.ReminderSpec.END_COUNT -> endCount.toLongOrNull() ?: 0L
                                else -> 0L
                            }
                        )
                    )
                },
                enabled = dateMs != null
            ) { Text(stringResource(R.string.save_cd)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text(stringResource(R.string.reminder_clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )

    if (dateDlg) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = dateMs ?: System.currentTimeMillis()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { dateDlg = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { dateMs = it }; dateDlg = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { dateDlg = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { androidx.compose.material3.DatePicker(state) }
    }
    if (endDateDlg) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = endDateMs ?: System.currentTimeMillis()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { endDateDlg = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { endDateMs = it }; endDateDlg = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { endDateDlg = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { androidx.compose.material3.DatePicker(state) }
    }
    if (timeDlg) {
        val state = androidx.compose.material3.rememberTimePickerState(initialHour = hour, initialMinute = minute)
        androidx.compose.ui.window.Dialog(onDismissRequest = { timeDlg = false }) {
            androidx.compose.material3.Surface(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.reminder_time), style = MaterialTheme.typography.titleMedium)
                    androidx.compose.material3.TimePicker(state, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { timeDlg = false }) { Text(stringResource(R.string.cancel)) }
                        TextButton(onClick = { hour = state.hour; minute = state.minute; timeDlg = false }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun weekdayShortStrLocal(day: Int): String = stringResource(
    when (day) {
        java.util.Calendar.SATURDAY -> R.string.day_sat
        java.util.Calendar.SUNDAY -> R.string.day_sun
        java.util.Calendar.MONDAY -> R.string.day_mon
        java.util.Calendar.TUESDAY -> R.string.day_tue
        java.util.Calendar.WEDNESDAY -> R.string.day_wed
        java.util.Calendar.THURSDAY -> R.string.day_thu
        else -> R.string.day_fri
    }
)

// فهرس العناوين: قفز للأقسام + طي/فتح كل قسم — يعمل في العرض والتحرير
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineSheet(
    headings: List<MarkdownFormat.OutlineHeading>,
    folded: Set<Int>,
    hasBody: (Int) -> Boolean,
    onToggleFold: (Int) -> Unit,
    onJump: (MarkdownFormat.OutlineHeading) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp).padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(R.string.outline_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (headings.isEmpty()) {
                Text(
                    stringResource(R.string.outline_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            headings.forEach { h ->
                key(h.lineIdx) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onJump(h) }
                            .padding(horizontal = (16 + (h.level - 1) * 16).dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            h.text.ifBlank { "…" },
                            style = when (h.level) {
                                1 -> MaterialTheme.typography.titleSmall
                                2 -> MaterialTheme.typography.bodyLarge
                                else -> MaterialTheme.typography.bodyMedium
                            },
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (hasBody(h.lineIdx)) {
                            IconButton(onClick = { onToggleFold(h.lineIdx) }) {
                                Icon(
                                    if (h.lineIdx in folded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.outline_title)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// شيت المساعدة السياقية (/ أو # أو [[): بحث داخلي + اختيار — المحرر لا يتغير أثناء الفتح فالمواضع آمنة
data class SlashItem(val id: String, val symbol: String, val label: String)

sealed interface AssistMode {
    data class Slash(val items: List<SlashItem>) : AssistMode
    data class Tags(val tags: List<String>) : AssistMode
    data class Notes(val notes: List<MarkdownFormat.NoteLink>) : AssistMode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistSheet(
    title: String,
    initialQuery: String,
    mode: AssistMode,
    onPickSlash: (SlashItem) -> Unit,
    onPickTag: (String) -> Unit,
    onPickNote: (MarkdownFormat.NoteLink) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember(mode) { mutableStateOf(initialQuery) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 24.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text(stringResource(R.string.slash_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Column(
                Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                when (mode) {
                    is AssistMode.Slash -> {
                        val items = mode.items.filter { filter.isBlank() || it.label.contains(filter, ignoreCase = true) }
                        items.forEach { item ->
                            key(item.id) {
                                Row(
                                    Modifier.fillMaxWidth().clickable { onPickSlash(item) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.symbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    Text(item.label, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        if (items.isEmpty()) {
                            Text(
                                stringResource(R.string.assist_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                    is AssistMode.Tags -> {
                        val tags = mode.tags.filter { filter.isBlank() || it.contains(filter, ignoreCase = true) }.take(20)
                        tags.forEach { tag ->
                            key(tag) {
                                Row(
                                    Modifier.fillMaxWidth().clickable { onPickTag(tag) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(40.dp))
                                    Text(tag, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        if (tags.isEmpty()) {
                            Text(
                                stringResource(R.string.assist_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                    is AssistMode.Notes -> {
                        val notes = mode.notes.filter { filter.isBlank() || it.title.contains(filter, ignoreCase = true) }.take(20)
                        notes.forEach { n ->
                            key(n.id) {
                                Row(
                                    Modifier.fillMaxWidth().clickable { onPickNote(n) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("[]", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(40.dp))
                                    Text(
                                        n.title.ifBlank { "…" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        if (notes.isEmpty()) {
                            Text(
                                stringResource(R.string.assist_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// تحويل إحداثيات النص الخام لنص المعروض: buildInline يطوي صيغ الروابط [نص](رابط) و[[ملاحظة]] للتسمية فقط (27 سبتمبر 2026)
private fun mapToBuilt(content: String, rawIdx: Int): Int {
    val re = Regex("""\[[^\]]+\]\([^)]+\)|\[\[[^\]]+\]\]""")
    var shift = 0
    for (m in re.findAll(content)) {
        if (m.range.first >= rawIdx) break
        val tok = m.value
        val built = if (tok.startsWith("[[")) tok.removePrefix("[[").removeSuffix("]]")
        else Regex("""\[([^\]]+)\]\(([^)]+)\)""").matchEntire(tok)?.groupValues?.get(1) ?: tok
        if (m.range.last < rawIdx) shift += tok.length - built.length
        else return m.range.first - shift
    }
    return rawIdx - shift
}

// تظليل نتائج البحث داخل كتلة معاينة — الإحداثيات بمساحة نص الكتلة الخام قبل الطي
private fun AnnotatedString.highlightMatches(
    content: String,
    matches: List<IntRange>,
    curLocal: Int,
    matchBg: Color,
    currentBg: Color
): AnnotatedString {
    if (matches.isEmpty() || matchBg == Color.Transparent) return this
    val b = AnnotatedString.Builder(this)
    matches.forEachIndexed { i, r ->
        val bs = mapToBuilt(content, r.first.coerceIn(0, content.length))
        val be = mapToBuilt(content, (r.last + 1).coerceIn(0, content.length)).coerceAtLeast(bs)
        if (be > bs) b.addStyle(
            androidx.compose.ui.text.SpanStyle(background = if (i == curLocal) currentBg else matchBg),
            bs, be
        )
    }
    return b.toAnnotatedString()
}

private fun findAllLocal(haystack: String, needle: String): List<IntRange> {
    if (needle.isEmpty()) return emptyList()
    val out = mutableListOf<IntRange>()
    val h = haystack.lowercase()
    val n = needle.lowercase()
    var from = 0
    while (true) {
        val i = h.indexOf(n, from)
        if (i < 0) break
        out.add(i until i + n.length)
        from = i + n.length
    }
    return out
}

// معاينة Markdown مقروءة: كتل أصلية + روابط نقّارة + مهام تتبدل + نسخ الكود — للعرض والتحرير
@Composable
fun MarkdownPreview(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onNoteLink: (String) -> Unit = {},
    onTaskToggle: (Int) -> Unit = {},
    // تظليل البحث داخل المعاينة (27 سبتمبر 2026): مطابقة محلية لكل كتلة + عدّاد تراكمي للنتيجة الحالية
    findQuery: String = "",
    findIdx: Int = 0,
    matchBg: Color = Color.Transparent,
    currentBg: Color = Color.Transparent,
) {
    val ctx = LocalContext.current
    val blocks = remember(text) { MarkdownFormat.parseBlocks(text) }
    val linkColor = MaterialTheme.colorScheme.primary
    val dim = tint.copy(alpha = 0.5f)
    val base = SpanStyle(color = tint, fontSize = 16.sp)
    fun openUrl(url: String) {
        runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(ctx, ctx.getString(R.string.content_no_viewer), Toast.LENGTH_SHORT).show()
        }
    }
    fun copyCode(code: String) {
        runCatching {
            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("nota-code", code))
            Toast.makeText(ctx, ctx.getString(R.string.copied), Toast.LENGTH_SHORT).show()
        }
    }
    Column(modifier) {
        if (blocks.isEmpty()) {
            Text(
                stringResource(R.string.note_body_hint),
                style = MaterialTheme.typography.bodyLarge.copy(color = tint.copy(alpha = 0.7f))
            )
            return@Column
        }
        // عدّاد تراكمي لنتائج البحث عبر الكتل بالترتيب — يحدد النتيجة الحالية العامة (27 سبتمبر 2026)
        var seen = 0
        // مطابقة محلية + تظليل: تُحسب على نص الكتلة الخام وتُطبق على النص المبني مع تعويض طي الروابط
        fun localHl(content: String): Triple<List<IntRange>, Int, (AnnotatedString) -> AnnotatedString> {
            val lm = findAllLocal(content, findQuery)
            val cur = findIdx - seen
            seen += lm.size
            return Triple(lm, cur) { built -> built.highlightMatches(content, lm, cur, matchBg, currentBg) }
        }
        blocks.forEachIndexed { idx, b ->
            key(idx) {
                when (b) {
                    is MarkdownFormat.PreviewBlock.Heading -> {
                        val (lm, cur, hl) = localHl(b.text)
                        LinkText(
                            hl(buildInline(b.text, base.copy(
                                fontFamily = AmiriFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = when (b.level) {
                                    1 -> 24.sp; 2 -> 20.sp; else -> 18.sp
                                }
                            ), dim, linkColor)),
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                            onClickLink = { tag, target ->
                                if (tag == "URL") openUrl(target) else onNoteLink(target)
                            }
                        )
                    }
                    is MarkdownFormat.PreviewBlock.Quote -> {
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Box(
                                Modifier.width(3.dp).heightIn(min = 24.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            )
                            Spacer(Modifier.width(8.dp))
                            val (lmQ, curQ, hlQ) = localHl(b.text)
                            LinkText(
                                hlQ(buildInline(b.text, base.copy(fontStyle = FontStyle.Italic), dim, linkColor)),
                                modifier = Modifier.weight(1f),
                                onClickLink = { tag, target ->
                                    if (tag == "URL") openUrl(target) else onNoteLink(target)
                                }
                            )
                        }
                    }
                    is MarkdownFormat.PreviewBlock.Divider -> {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = tint.copy(alpha = 0.3f))
                    }
                    is MarkdownFormat.PreviewBlock.Bullet -> {
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Spacer(Modifier.width((b.indent * 4).coerceAtMost(32).dp))
                            Text("• ", style = MaterialTheme.typography.bodyLarge.copy(color = tint))
                            val (lmB, curB, hlB) = localHl(b.text)
                            LinkText(
                                hlB(buildInline(b.text, base, dim, linkColor)),
                                modifier = Modifier.weight(1f),
                                onClickLink = { tag, target ->
                                    if (tag == "URL") openUrl(target) else onNoteLink(target)
                                }
                            )
                        }
                    }
                    is MarkdownFormat.PreviewBlock.Ordered -> {
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Spacer(Modifier.width((b.indent * 4).coerceAtMost(32).dp))
                            Text("${b.num}. ", style = MaterialTheme.typography.bodyLarge.copy(color = tint))
                            val (lmO, curO, hlO) = localHl(b.text)
                            LinkText(
                                hlO(buildInline(b.text, base, dim, linkColor)),
                                modifier = Modifier.weight(1f),
                                onClickLink = { tag, target ->
                                    if (tag == "URL") openUrl(target) else onNoteLink(target)
                                }
                            )
                        }
                    }
                    is MarkdownFormat.PreviewBlock.Task -> {
                        Row(
                            Modifier.fillMaxWidth().clickable { onTaskToggle(b.lineStart) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.width((b.indent * 4).coerceAtMost(32).dp))
                            Checkbox(checked = b.checked, onCheckedChange = { onTaskToggle(b.lineStart) })
                            val (lmT, curT, hlT) = localHl(b.text)
                            LinkText(
                                hlT(buildInline(b.text, base, dim, linkColor)),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDecoration = if (b.checked) TextDecoration.LineThrough else null
                                ),
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                onClickLink = { tag, target ->
                                    if (tag == "URL") openUrl(target) else onNoteLink(target)
                                }
                            )
                        }
                    }
                    is MarkdownFormat.PreviewBlock.Code -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = tint.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { copyCode(b.text) }) {
                                        Text(stringResource(R.string.copy_note))
                                    }
                                }
                                val (lmC, curC, hlC) = localHl(b.text)
                                Text(
                                    hlC(AnnotatedString(b.text)),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = tint
                                    )
                                )
                            }
                        }
                    }
                    is MarkdownFormat.PreviewBlock.Table -> {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .padding(12.dp)
                        ) {
                            b.rows.forEachIndexed { ri, row ->
                                Row(Modifier.fillMaxWidth()) {
                                    row.forEach { cell ->
                                        val (lmCell, curCell, hlCell) = localHl(cell)
                                        LinkText(
                                            hlCell(buildInline(cell, base.copy(
                                                fontWeight = if (ri == 0) FontWeight.Bold else null
                                            ), dim, linkColor)),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 3,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            onClickLink = { tag, target ->
                                                if (tag == "URL") openUrl(target) else onNoteLink(target)
                                            }
                                        )
                                    }
                                }
                                if (ri == 0 && b.rows.size > 1) {
                                    HorizontalDivider(color = tint.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                    is MarkdownFormat.PreviewBlock.Para -> {
                        val (lmP, curP, hlP) = localHl(b.text)
                        LinkText(
                            hlP(buildInline(b.text, base, dim, linkColor)),
                            modifier = Modifier.padding(vertical = 4.dp),
                            onClickLink = { tag, target ->
                                if (tag == "URL") openUrl(target) else onNoteLink(target)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip,
    onClickLink: (String, String) -> Unit = { _, _ -> }
) {
    androidx.compose.foundation.text.ClickableText(
        text = text,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier,
        onClick = { off ->
            text.getStringAnnotations(off, off).firstOrNull()?.let { ann ->
                onClickLink(ann.tag, ann.item)
            }
        }
    )
}

private fun buildInline(content: String, base: SpanStyle, dim: Color, linkColor: Color): AnnotatedString {
    val b = AnnotatedString.Builder()
    val re = Regex("""\[[^\]]+\]\([^)]+\)|\[\[[^\]]+\]\]""")
    var last = 0
    for (m in re.findAll(content)) {
        if (m.range.first > last) {
            b.append(MarkdownFormat.parseInlineSpans(content.substring(last, m.range.first), base, dim))
        }
        val tok = m.value
        if (tok.startsWith("[[")) {
            val t = tok.removePrefix("[[").removeSuffix("]]")
            b.pushStringAnnotation("NOTE", t)
            b.pushStyle(base.copy(color = linkColor, textDecoration = TextDecoration.Underline))
            b.append(t)
            b.pop()
            b.pop()
        } else {
            val mm = Regex("""\[([^\]]+)\]\(([^)]+)\)""").matchEntire(tok)
            if (mm != null) {
                b.pushStringAnnotation("URL", mm.groupValues[2])
                b.pushStyle(base.copy(color = linkColor, textDecoration = TextDecoration.Underline))
                b.append(mm.groupValues[1])
                b.pop()
                b.pop()
            } else {
                b.append(MarkdownFormat.parseInlineSpans(tok, base, dim))
            }
        }
        last = m.range.last + 1
    }
    if (last < content.length) {
        b.append(MarkdownFormat.parseInlineSpans(content.substring(last), base, dim))
    }
    return b.toAnnotatedString()
}

// مفتاح أوضاع العرض الفاخر: حبة واحدة بثلاثة خيارات — المحدد بتدرج ذهبي ونص أبيض
@Composable
fun ViewModeSegment(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { i, label ->
                key(i) {
                    val sel = i == selected
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .then(if (sel) Modifier.background(goldGradient()) else Modifier)
                            .clickable { onSelect(i) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
