package com.nota.notes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import com.nota.notes.data.ChecklistItem
import com.nota.notes.data.NoteAttachment
import com.nota.notes.data.NoteMediaFiles
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import com.nota.notes.data.ReminderSpec
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.nota.notes.data.NoteBgFiles
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import com.nota.notes.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.nota.notes.ui.MarkdownFormat

internal fun findAll(haystack: String, needle: String): List<IntRange> {
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

// الدفعة ج: مشغل مساعدة — kind: 0=أمر / و1=#وسم و2=[[ملاحظة — from بداية المشغل في النص
internal data class AssistTrigger(val kind: Int, val from: Int, val query: String)

// عنصر شيت الإضافة (28 سبتمبر 2026): سجل واحد يبني التبويبات والبحث و"المستخدمة مؤخرًا"
// free = مجاني للجميع (بلا بوابة Pro) — act = الفعل بعد القفل
private data class SheetEntry(
    val key: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val desc: String = "",
    val free: Boolean = false,
    val act: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteEditorScreen(
    title: String, body: TextFieldValue,
    createdAt: Long = 0L, updatedAt: Long = 0L,
    color: Int = 0, onColor: (Int) -> Unit = {},
    // وضع العرض المحمي: كل الإمكانيات متاحة ما عدا التعديل — زر التعديل يفتح الكتابة
    readOnly: Boolean = false, onEdit: () -> Unit = {},
    // الأرشيف من داخل الملاحظة: أرشفة أو استعادة حسب حالتها (للمحفوظة فقط)
    isArchived: Boolean = false, onArchive: () -> Unit = {},
    // حجم خط المحرر من الإعدادات المتقدمة small|normal|large — الأسطر المسطرة تتبع تلقائيًا
    font: String = "normal",
    // الحفظ التلقائي: مؤشر نصي تحت التواريخ + زر الحفظ اليدوي يبقى دائمًا
    autosave: Boolean = true,
    // التأثيرات والحركة (28 سبتمبر 2026): اهتزاز + أنيميشن القائمة الدائرية — إطفاؤهما معًا من الإعدادات
    effects: Boolean = true,
    // صورة الخلفية: اسم ملف داخلي — العرض باهت فوقه ستار اللون لتُقرأ النصوص
    background: String? = null,
    onPickBg: () -> Unit = {}, onRemoveBg: () -> Unit = {},
    // تصدير PDF: مشاركة الملاحظة الحالية كمستند (زر صريح — للعرض والتعديل معًا)
    onExportPdf: () -> Unit = {},
    // نسخ للحافظة: العنوان + النص بضغطة واحدة (زر صريح — للعرض والتعديل معًا)
    onCopy: () -> Unit = {},
    // الدفعة د: مشاركة Markdown/HTML + فتح روابط [[ ]] + قلب مهام المعاينة في العرض
    onShareMarkdown: () -> Unit = {},
    onShareHtml: () -> Unit = {},
    onOpenNoteLink: (String) -> Unit = {},
    onPreviewTaskFlip: (String) -> Unit = {},
    // الوسوم: تُعرض أسفل الملاحظة دائمًا + تُحرَّر في وضع الكتابة فقط
    tags: List<String> = emptyList(),
    onTagsChange: (List<String>) -> Unit = {},
    // التصنيف: تسمية واحدة حرة (شخصي/عمل) — مجاني للجميع — يُعرض دائمًا ويُحرَّر في وضع الكتابة
    category: String = "",
    onCategoryChange: (String) -> Unit = {},
    existingCategories: List<String> = emptyList(),
    // الدفعة ج: إكمال #الوسوم من وسوم الحساب + روابط [[الملاحظات]] من القائمة — فارغ = معطل
    existingTags: List<String> = emptyList(),
    // شيت الإضافة: آخر الإضافات المستخدمة (28 سبتمبر 2026) — يخزّنها الأب لتبقى بين الجلسات
    recentSheet: List<String> = emptyList(),
    onRecentSheet: (List<String>) -> Unit = {},
    // قوالب المستخدم (28 سبتمبر 2026): يحفظ النص الحالي كقالب ويعيد إدراجه في أي ملاحظة
    customTpls: List<Pair<String, String>> = emptyList(),
    onSaveTpl: (String, String) -> Unit = { _, _ -> },
    onDelTpl: (Int) -> Unit = {},
    allNotes: List<MarkdownFormat.NoteLink> = emptyList(),
    // التذكير: الموعد + قاعدة التكرار (بسيط أو مخصص) + الإشعار المسبق — صف الجرس أسفل الوسوم
    reminder: ReminderSpec = ReminderSpec(),
    onReminderChange: (ReminderSpec) -> Unit = {},
    // الموقع: إحداثيات اختيارية — الالتقاط من GPS (يعمل أوفلاين) — ميزة مدفوعة
    latitude: Double? = null,
    longitude: Double? = null,
    onLocationChange: (Double?, Double?) -> Unit = { _, _ -> },
    canUseLocation: Boolean = false,
    onLocationLocked: () -> Unit = {},
    // تذكير الموقع (27 سبتمبر 2026): إحداثيات السياج + نصف القطر + الاسم — null = بلا سياج
    geoLat: Double? = null,
    geoLng: Double? = null,
    geoRadiusM: Float = 200f,
    geoName: String = "",
    onGeofenceChange: (Double?, Double?, Float, String) -> Unit = { _, _, _, _ -> },
    onGeofenceClear: () -> Unit = {},
    // بوابة PRO: الوسوم/التذكير/PDF/الخلفيات/الوسائط/النسخ مدفوعة — العرض يبقى، والتحرير يُقفل مع تنبيه
    proAllowed: Boolean = true,
    onProLocked: () -> Unit = {},
    // محتوى الملاحظة: مهام checklist + صور داخلية (التسجيل والرسم قريبًا) — قسم + أسفل الموقع
    tasks: List<ChecklistItem> = emptyList(),
    onAddTask: (String) -> Unit = {},
    onToggleTask: (ChecklistItem) -> Unit = {},
    onRenameTask: (ChecklistItem, String) -> Unit = { _, _ -> },
    onDeleteTask: (Long) -> Unit = {},
    attachments: List<NoteAttachment> = emptyList(),
    // الذكاء الاصطناعي BYOK (27 سبتمبر 2026): إعادة كتابة + تحسين تنسيق — المفتاح من الإعدادات
    aiReady: Boolean = false,
    onAiSetup: () -> Unit = {},
    onAiRun: (mode: String, text: String, onDone: (Result<String>) -> Unit) -> Unit = { _, _, cb -> cb(Result.failure(Exception("nokey"))) },
    onTakePhoto: () -> Unit = {},
    onPickImage: () -> Unit = {},
    onPickFile: () -> Unit = {}, // ملف عام — قائمة + 27 سبتمبر 2026
    onDeleteAttachment: (NoteAttachment) -> Unit = {},
    onSaveAudio: (String) -> Unit = {},
    onSaveDrawing: (String) -> Unit = {},
    onTitle: (String) -> Unit, onBody: (TextFieldValue) -> Unit,
    onSave: () -> Unit, onBack: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    // خلفية المحرر تتلون بلون الملاحظة مثل Sticky Notes — وكل المحتوى بلون النص المقابل لها
    val bg = noteCardBg(color)
    val onCol = noteCardOn(color)
    // نمط النص الأساسي ولون العلامات الخافت — مبكرًا لأن العرض الفوري يُبنى عليه قبل التحويل
    val baseTextStyle = when (font) {
        "small" -> MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 24.sp).copy(color = onCol)
        "large" -> MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 34.sp).copy(color = onCol)
        else -> MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 28.sp).copy(color = onCol)
    }
    val dimColor = onCol.copy(alpha = 0.5f)

    // قسم التفاصيل مطوي افتراضيًا لكل ملاحظة + قائمة الخيارات — فيبقى مجال الكتابة واسعًا
    var menu by remember { mutableStateOf(false) }
    // شيت زر +: كل الإضافات (وسوم/تذكير/موقع/وسائط/خلفية) — والكتابة بملء الشاشة
    var addSheet by remember { mutableStateOf(false) }
    var recDlg by remember { mutableStateOf(false) }
    var drawDlg by remember { mutableStateOf(false) }
    var outlineSheet by remember { mutableStateOf(false) }
    // حوار الرابط من شريط التحديد السياقي: رابطه المدخل + راية الفتح
    var linkDialogOpen by remember { mutableStateOf(false) }
    // الذكاء الاصطناعي (27 سبتمبر 2026): شيت اختيار + معاينة قبل الاستبدال — لا استبدال تلقائي أبدًا
    var aiSheet by remember { mutableStateOf(false) }
    var aiWorking by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiResult by remember { mutableStateOf<String?>(null) }
    // آخر مهمة للـ retry من المعاينة مباشرة (تحسين 27 سبتمبر 2026)
    var aiMode by remember { mutableStateOf("rewrite") }
    val aiClipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    // البحث داخل الملاحظة (مبسط): شريط استعلام + عدّاد + سابق/تالٍ — للعرض والتحرير معًا — يبقى عند الدوران
    var findOpen by rememberSaveable(createdAt) { mutableStateOf(false) }
    var findQuery by rememberSaveable(createdAt) { mutableStateOf("") }
    var findIdx by rememberSaveable(createdAt) { mutableStateOf(0) }
    val findFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val contentFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    // محرر النص الغني (قرار 26 سبتمبر 2026): الحالة الغنية مصدر الحقيقة — تُصدَّر Markdown للأب
    // بلا أي تعديل على المكتبة: دوالها العامة فقط (toggle/addLink/lists/setMarkdown/toMarkdown)
    val rich = remember(createdAt) { RichTextState() }
    var lastLoadedMd by remember(createdAt) { mutableStateOf<String?>(null) }
    var lastSentMd by remember(createdAt) { mutableStateOf<String?>(null) }
    var seeded by remember(createdAt) { mutableStateOf(false) }
    // الكتل الخاصة تُحفظ خارج المحرر (27 سبتمبر 2026): تُستخرج قبل التحميل وتُعاد بعد التصدير — فلا تضيع أبدًا
    var specials by remember(createdAt) { mutableStateOf(emptyList<String>()) }
    // مزامنة الداخل من الأب (فتح ملاحظة/تغيير خارجي) — الكتابة تُصدَّر بالاتجاه الآخر فقط
    LaunchedEffect(body.text, createdAt) {
        if (lastLoadedMd != body.text) {
            val (safe, sp) = MarkdownFormat.extractSpecial(body.text)
            specials = sp
            rich.setMarkdown(safe)
            lastLoadedMd = body.text
            lastSentMd = body.text
            seeded = true
        }
    }
    val currentMd = remember(rich.annotatedString, specials) {
        MarkdownFormat.restoreSpecial(rich.toMarkdown(), specials)
    }
    // التراجع/الإعادة: مكدسا لقطات (عنوان + Markdown) بحد 50 — المطبق لا يُدفع (lastSentMd يمنع الالتفاف)
    var undoStack by remember(createdAt) { mutableStateOf(listOf<Pair<String, String>>()) }
    var redoStack by remember(createdAt) { mutableStateOf(listOf<Pair<String, String>>()) }
    // سجل التراجع الدائم (28 سبتمبر 2026): يُقرأ مرة عند فتح الملاحظة ويُكتب عند الخروج منها
    // فالإرجاع يبقى ممكنًا بعد إعادة تشغيل التطبيق — قبلها كان المكدس في الذاكرة فقط
    val undoCtx = LocalContext.current.applicationContext
    val undoNow by rememberUpdatedState(undoStack)
    var undoLoaded by remember(createdAt) { mutableStateOf(false) }
    LaunchedEffect(createdAt) {
        if (createdAt > 0L && !undoLoaded) {
            undoLoaded = true
            val j = com.nota.notes.data.UndoJournal.load(undoCtx, createdAt)
            // لا نطمس المكدع الحيّ: نفتحه فقط لو كان فاضيًا (فتح أول مرة)
            if (j.isNotEmpty() && undoStack.isEmpty()) undoStack = j
        }
    }
    DisposableEffect(createdAt) {
        onDispose { if (createdAt > 0L) com.nota.notes.data.UndoJournal.save(undoCtx, createdAt, undoNow) }
    }
    // مسح التنسيق يدفع لقطة ما-قبل-التغيير يدويًا — فتُتجاوز الدفعة التلقائية اللاحقة (وإلا احتاجت الاستعادة ضغطتين)
    var skipNextAutoPush by remember(createdAt) { mutableStateOf(false) }
    // تأكيدات قبل الإجراءات (بأمر محمد 27 سبتمبر 2026): أرشفة/مسح تنسيق/إزالة خلفية/حذف مرفق أو مهمة
    var confirmArchive by remember { mutableStateOf(false) }
    var confirmClearFormat by remember { mutableStateOf(false) }
    var confirmRemoveBg by remember { mutableStateOf(false) }
    var confirmDelAttachment by remember { mutableStateOf<NoteAttachment?>(null) }
    var confirmDelTask by remember { mutableStateOf<Long?>(null) }
    fun pushHistory() {
        undoStack = (undoStack + (title to currentMd)).takeLast(50)
        if (redoStack.isNotEmpty()) redoStack = emptyList()
    }
    fun applyExternalMd(md: String) {
        val (safe, sp) = MarkdownFormat.extractSpecial(md)
        specials = sp
        rich.setMarkdown(safe)
        lastLoadedMd = md
        lastSentMd = md
        onBody(TextFieldValue(md))
    }
    fun handleTitle(t: String) { pushHistory(); onTitle(t) }
    // تصدير كل ضغطة للأب (إحصائيات/فهرس/حفظ تلقائي تعمل على النص المصدَّر)
    LaunchedEffect(currentMd) {
        if (!seeded) return@LaunchedEffect
        if (currentMd == lastSentMd) return@LaunchedEffect
        if (skipNextAutoPush) skipNextAutoPush = false else pushHistory()
        lastSentMd = currentMd
        lastLoadedMd = currentMd
        onBody(TextFieldValue(currentMd))
    }
    fun undo() {
        val prev = undoStack.lastOrNull() ?: return
        undoStack = undoStack.dropLast(1)
        redoStack = (redoStack + (title to currentMd)).takeLast(50)
        onTitle(prev.first); applyExternalMd(prev.second)
    }
    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        redoStack = redoStack.dropLast(1)
        undoStack = (undoStack + (title to currentMd)).takeLast(50)
        onTitle(next.first); applyExternalMd(next.second)
    }
    // الفهرس من النص المصدَّر — القفز بتحديد أول تطابق (بلا دفع في history التراجع)
    val headings = remember(currentMd) { MarkdownFormat.extractHeadings(currentMd) }
    // نتائج البحث على النص الصريح (plain) — لا Markdown المصدَّر: إحداثيات التحديد بمساحة النص المرئي نفسه
    // (إحداثيات Markdown تنزاح مع **/# /[]() فتُسقط التحديد في غير موضعه أو خارج النص)
    val findMatches = remember(rich.annotatedString.text, findQuery, findOpen) {
        if (!findOpen || findQuery.isEmpty()) emptyList()
        else findAll(rich.annotatedString.text, findQuery)
    }
    LaunchedEffect(findOpen, findQuery, findIdx, findMatches) {
        if (!findOpen || findMatches.isEmpty()) return@LaunchedEffect
        val len = rich.annotatedString.text.length
        val m = findMatches[findIdx.coerceIn(findMatches.indices)]
        rich.selection = TextRange(m.first.coerceIn(0, len), (m.last + 1).coerceIn(0, len))
    }
    // مطابقات العرض على المتن الكامل المستعاد (يشمل الكتل الخاصة) — للمعاينة الأصلية (27 سبتمبر 2026)
    val viewMatches = remember(currentMd, findQuery, findOpen) {
        if (!findOpen || findQuery.isEmpty()) emptyList()
        else findAll(currentMd, findQuery)
    }
    LaunchedEffect(findOpen) {
        if (findOpen) runCatching { findFocus.requestFocus() }
    }
    // قائمة + الشاملة (27 سبتمبر 2026): بحث + إدراج كتل في النهاية + قوالب
    // الإلحاق في النهاية عمدًا: إحداثيات Markdown تنزاح عن النص المرئي فالإدراج في المنتصف غير موثوق
    // الكتل الخاصة (اقتباس/فاصل/جدول/كود) تُحفظ خارج المحرر عبر extractSpecial/restoreSpecial فلا تضيع
    // إعدادات hunt: التبويب النشط + سجل آخر الإضافات المستخدمة (محفوظ عبر الجلسات)
    var sheetQuery by remember { mutableStateOf("") }
    // 28 سبتمبر 2026: التبويب النشط في شيت الإضافة (وسائط/إدراج/تنظيم) — يمنع موضع الحفظ على المفتوح
    var sheetTab by rememberSaveable(createdAt) { mutableIntStateOf(0) }
    // محرّر الجدول (28 سبتمبر 2026): شبكة خلايا حقيقية بدل إدراج نص جاهز
    var tableDlg by remember { mutableStateOf(false) }
    // حوار حفظ القالب (28 سبتمبر 2026): الاسم + معاينة + إدارة القوالب المحفوظة
    var tplDlg by remember { mutableStateOf(false) }
    // عارض الصور المدمج (28 سبتمبر 2026): تكبير/تصغير وسحب ونقر مزدوج للتهيئة
    var viewer by remember { mutableStateOf<NoteAttachment?>(null) }
    // وضع التركيز (28 سبتمبر 2026): يخفي كل ما ليس النص — مع مؤقّت اختياري
    var focusMode by remember { mutableStateOf(false) }
    var focusTimer by remember { mutableIntStateOf(0) }
    var focusTimerRun by remember { mutableStateOf(false) }
    // إدراج الكتل في موضع المؤشر (28 سبتمبر 2026): القيد القديم كان على إحداثيات Markdown
    // فقط، والمحرر بقى WYSIWYG فعليًا — فالمؤشر المرئي هو المرجع الصحيح الآن.
    // الترميز (## أو > أو ```) لا يفهمه المحرر كنص، فنعالج كل نوع بأسلوبه:
    // الفقرات عبر فقرة جديدة، والكود عبر addCodeSpan على التحديد، والباقي عبر النص صراحةً.
    fun insertBlockAtCursor(block: String) {
        pushHistory()
        skipNextAutoPush = true
        val b = block.trim()
        val t = rich.annotatedString.text
        val at = if (rich.selection.collapsed) rich.selection.start else rich.selection.end
            .coerceIn(0, t.length)
        // فقرة جديدة قبلها إن كنّا داخل نص: الإدراج في منتصف السطر يفسد البنية
        val needBreak = at > 0 && t.getOrNull(at - 1) != '\n'
        val payload = (if (needBreak) "\n" else "") + b + "\n"
        rich.addTextAtIndex(at, payload)
        val end = at + payload.length
        rich.selection = TextRange(end, end)
    }
    fun insertBlock(block: String) {
        pushHistory()
        skipNextAutoPush = true
        val base = currentMd.trimEnd()
        val next = if (base.isEmpty()) block.trim() else base + "\n\n" + block.trim()
        applyExternalMd(next)
        val len = rich.annotatedString.text.length
        rich.selection = TextRange(len, len)
    }
    fun applyTemplate(body: String) {
        // فارغة → استبدال، وإلا إلحاق في النهاية بلا مساس بالمحتوى (فلا حاجة لتأكيد)
        if (currentMd.isBlank()) {
            pushHistory()
            skipNextAutoPush = true
            applyExternalMd(body.trim())
        } else insertBlock(body)
        val len = rich.annotatedString.text.length
        rich.selection = TextRange(len, len)
    }
    val todayStr = remember { java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()) }
    // وصفات وضع التركيز (28 سبتمبر 2026) — تُقرأ مرة واحدة في التركيب
    val exitFocusCd = stringResource(R.string.focus_exit_cd)
    val nowStr = remember { java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date()) }
    // اتجاه الكتابة من لغة النص نفسه (أول حرف قوي) — لا لغة الجهاز: ملاحظة إنجليزية على جهاز عربي تُكتب LTR
    // الفارغ بلا قوي → لغة الجهاز كاحتياطي — والمختلط يُحسم لكل فقرة عبر ContentOr (قرار 26 سبتمبر 2026)
    fun firstStrongRtl(text: String): Boolean? {
        for (c in text) {
            when (Character.getDirectionality(c)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return true
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
            }
        }
        return null
    }
    val deviceRtl = remember { java.util.Locale.getDefault().language == "ar" }
    val rtl = firstStrongRtl(currentMd) ?: firstStrongRtl(title) ?: deviceRtl
    // شريط وضع التركيز: زر خروج فقط (28 سبتمبر 2026) — منفصل كـ lambda معلن بتوعبه المحدد
    val focusTopBar: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth().padding(top = 44.dp, end = 12.dp), contentAlignment = Alignment.CenterEnd) {
            IconButton(
                onClick = { focusMode = false; focusTimerRun = false },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(onCol.copy(alpha = 0.08f))
                    .semantics { contentDescription = exitFocusCd }
            ) { Icon(Icons.Filled.Close, contentDescription = null, tint = onCol) }
        }
    }
    Scaffold(
        // داخل DrawerShell: حواف النظام يستهلكها الغلاف الخارجي — بلا ازدواج هنا
        contentWindowInsets = WindowInsets(0),
        containerColor = bg,
        floatingActionButton = {
            // زر + في وضع التحرير فقط — ووضع التركيز يخفيه (28 سبتمبر 2026): النص وحده على الشاشة
            if (!readOnly && !focusMode) {
                FloatingActionButton(
                    onClick = { addSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_add))
                }
            }
        },
        topBar = {
        if (focusMode) focusTopBar() else {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                actions = {
                    // التراجع/الإعادة في وضع التعديل فقط — بجانب البحث
                    if (!readOnly) {
                        IconButton(onClick = ::undo, enabled = undoStack.isNotEmpty()) {
                            Icon(Icons.Filled.Undo, contentDescription = stringResource(R.string.undo_cd))
                        }
                        IconButton(onClick = ::redo, enabled = redoStack.isNotEmpty()) {
                            Icon(Icons.Filled.Redo, contentDescription = stringResource(R.string.redo_cd))
                        }
                    }
                    // الخلفية موحدة في شيت + (Branch G): بلا زر علوي مكرر — الشريط للكتابة فقط
                    // البحث داخل الملاحظة — للعرض والتحرير معًا (يفتح/يغلق شريط البحث)
                    IconButton(onClick = {
                        if (findOpen) { findOpen = false; findQuery = ""; findIdx = 0 }
                        else findOpen = true
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.bar_find_cd))
                    }
                    // قائمة الخيارات: نسخ/PDF/أرشيف — الشريط العلوي للكتابة فقط (تراجع/خلفية/بحث/حفظ)
                    val hasContent = title.isNotBlank() || currentMd.isNotBlank()
                    if (hasContent || createdAt > 0L) {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_cd))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (hasContent) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.copy_note)) },
                                    leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                    onClick = { menu = false; if (proAllowed) onCopy() else onProLocked() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_pdf)) },
                                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                    onClick = { menu = false; if (proAllowed) onExportPdf() else onProLocked() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_markdown)) },
                                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                    onClick = { menu = false; if (proAllowed) onShareMarkdown() else onProLocked() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_html)) },
                                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                    onClick = { menu = false; if (proAllowed) onShareHtml() else onProLocked() }
                                )
                            }
                            // زر الأرشيف/الاستعادة للملاحظة المحفوظة فقط
                            if (createdAt > 0L) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (isArchived) R.string.unarchive_cd else R.string.archive_note_cd)) },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(if (isArchived) R.drawable.ic_unarchive else R.drawable.ic_archive),
                                            contentDescription = null
                                        )
                                    },
                                    onClick = { menu = false; confirmArchive = true }
                                )
                            }
                        }
                    }
                    // زر التعديل يظهر في وضع العرض فقط — وزر الحفظ في وضع التعديل فقط
                    if (readOnly) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_cd))
                        }
                    } else {
                        IconButton(onClick = onSave, enabled = title.isNotBlank() || currentMd.isNotBlank()) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save_cd))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bg,
                    titleContentColor = onCol,
                    navigationIconContentColor = onCol,
                    actionIconContentColor = onCol
                )
            )
        }
        }
    ) { p ->
        CompositionLocalProvider(LocalContentColor provides onCol) {
        // الخلفية: الصورة باهتة (28%) وفوقها ستار بلون الملاحظة (55%) — النصوص مقروءة دائمًا
        Box(Modifier.padding(p).fillMaxSize()) {
            NoteBgLayer(name = background)
            if (background != null) {
                Box(Modifier.fillMaxSize().background(bg.copy(alpha = 0.55f)))
            }
            // لمسة فاخرة: خط ذهبي رفيع تحت الشريط العلوي — هوية Premium Warm
            Box(
                Modifier.fillMaxWidth().height(2.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(goldGradient())
            )
        Column(Modifier.padding(16.dp).fillMaxSize()) {
            // شريط البحث داخل الملاحظة (مبسط): استعلام + عدّاد + سابق/تالٍ + إغلاق — فوق العنوان
            if (findOpen) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        BasicTextField(
                            value = findQuery,
                            onValueChange = { findQuery = it; findIdx = 0 },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                                if (findMatches.isNotEmpty()) findIdx = (findIdx + 1) % findMatches.size
                            }),
                            decorationBox = { inner ->
                                Box(Modifier.padding(horizontal = 8.dp)) {
                                    if (findQuery.isEmpty()) {
                                        Text(
                                            stringResource(R.string.find_hint),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier.weight(1f).focusRequester(findFocus)
                        )
                        Text(
                            // أرقام العدّاد بلغة الجهاز: عربية مشرقية على ar-EG وغربية على en
                            if (findMatches.isEmpty()) String.format(Locale.getDefault(), "%d/%d", 0, 0)
                            else String.format(
                                Locale.getDefault(), "%d/%d",
                                findIdx.coerceIn(findMatches.indices) + 1, findMatches.size
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { if (findMatches.isNotEmpty()) findIdx = (findIdx - 1 + findMatches.size) % findMatches.size },
                            enabled = findMatches.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.find_prev_cd))
                        }
                        IconButton(
                            onClick = { if (findMatches.isNotEmpty()) findIdx = (findIdx + 1) % findMatches.size },
                            enabled = findMatches.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.find_next_cd))
                        }
                        IconButton(onClick = { findOpen = false; findQuery = ""; findIdx = 0 }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.find_close_cd))
                        }
                    }
                }
            }
            // العنوان بلا إطار: أميري متوسط فاخر + مؤشر ذهبي + شريط تمييز — حضور يليق بالعرض والتحرير
            // العنوان سطر واحد + سحب أفقي لقراءة الباقي (قرار 26 سبتمبر 2026): مرآة TextFieldValue للتحكم بالمؤشر
            var titleField by remember(createdAt) { mutableStateOf(TextFieldValue(title)) }
            // مزامنة الخارج فقط (تراجع/ملاحظة) — الكتابة تُرسل للأب ولا تُعاد كتابتها هنا
            LaunchedEffect(title) {
                if (titleField.text != title) titleField = TextFieldValue(title, TextRange(titleField.selection.start.coerceIn(0, title.length)))
            }
            var titleLayout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
            var dragX by remember { mutableStateOf(0f) }
            var lastX by remember { mutableStateOf(0f) }
            BasicTextField(
                value = titleField,
                onValueChange = { titleField = it; handleTitle(it.text) },
                readOnly = readOnly,
                singleLine = true,
                maxLines = 1,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = onCol,
                    // العنوان يتبع لغته هو: أول قوي فيه وإلا اتجاه المتن (قرار 26 سبتمبر 2026)
                    textAlign = TextAlign.Start,
                    textDirection = if (firstStrongRtl(title) ?: rtl) TextDirection.ContentOrRtl else TextDirection.ContentOrLtr
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                onTextLayout = { titleLayout = it },
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                stringResource(R.string.note_title_hint),
                                style = MaterialTheme.typography.headlineMedium,
                                color = onCol.copy(alpha = 0.5f)
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    .pointerInput(titleLayout) {
                        // السحب الأفقي يحرّك المؤشر (فيتبعه العرض) — النقرة تمر للحقل عادي
                        detectHorizontalDragGestures(
                            onDragStart = { off -> dragX = off.x; lastX = off.x },
                            onDragCancel = {},
                            onDragEnd = {},
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                // تتبع يدوي للحركة (positionChange غير متاحة في نسخة Compose هنا)
                                dragX += change.position.x - lastX
                                lastX = change.position.x
                                titleLayout?.let { layout ->
                                    val off = layout.getOffsetForPosition(Offset(dragX, 1f))
                                    titleField = titleField.copy(selection = TextRange(off))
                                }
                            }
                        )
                    }
            )
            // شريط ذهبي قصير تحت العنوان — توقيع فاخر
            if (title.isNotBlank()) {
                Box(
                    Modifier.width(64.dp).height(3.dp).padding(top = 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(goldGradient())
                )
            }
            // شريط التنسيق صار عائمًا قابلًا للسحب (انظر أسفل الشاشة) — حُذف من هنا ليحرر مساحة الكتابة
            // ورق مسطّر + محرر غني (قرار 26 سبتمبر 2026): الأسطر من onTextLayout — تتحرك مع النص
            // العرض render حقيقي (RichText) بدل Markdown الخام — والتحرير WYSIWYG بالمكتبة خامًا
            val (bodySize, bodyGap) = when (font) {
                "small" -> 14.sp to 24.sp
                "large" -> 20.sp to 34.sp
                else -> 16.sp to 28.sp
            }
            val ruleStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = bodySize, lineHeight = bodyGap,
                // بداية السطر مضمونة: Start = يمين في RTL ويسار في LTR — بلا اعتماد على افتراضي المكتبة
                textAlign = TextAlign.Start,
                textDirection = if (rtl) TextDirection.ContentOrRtl else TextDirection.ContentOrLtr
            )
            val density = LocalDensity.current
            val ruleGap = with(density) { bodyGap.toPx() }
            val ruleOffset = with(density) { 2.dp.toPx() }
            // سطور الدفتر: أوضح في الليلي (45%) وخفيفة نهارًا — مقروءة دائمًا بلا طغيان
            val ruleColor = noteCardOn(color).copy(alpha = if (LocalNotaDark.current) 0.45f else 0.22f)
            val scroll = rememberScrollState()
            var lineBottoms by remember { mutableStateOf(emptyList<Float>()) }
            var measuredGap by remember { mutableStateOf<Float?>(null) }
            var viewportH by remember { mutableIntStateOf(0) }
            val viewportMinH = if (viewportH > 0) with(density) { viewportH.toDp() } else null
            // الدفتر مشترك (عرض/تحرير): يُحسب في التركيب فيشترك في إعادة التركيب مع الأسطر
            val ruled = Modifier.fillMaxWidth()
                .then(if (viewportMinH != null) Modifier.heightIn(min = viewportMinH) else Modifier)
                .drawBehind {
                    val w = 1.dp.toPx()
                    val h = size.height
                    if (lineBottoms.isNotEmpty()) {
                        lineBottoms.forEach { b ->
                            val y = b - ruleOffset
                            if (y >= 0f && y <= h)
                                drawLine(ruleColor, Offset(0f, y), Offset(size.width, y), strokeWidth = w)
                        }
                        val gap = measuredGap ?: ruleGap
                        var y = lineBottoms.last() + gap - ruleOffset
                        while (y <= h) {
                            if (y >= 0f)
                                drawLine(ruleColor, Offset(0f, y), Offset(size.width, y), strokeWidth = w)
                            y += gap
                        }
                    } else {
                        var y = ruleGap - ruleOffset
                        while (y <= h) {
                            drawLine(ruleColor, Offset(0f, y), Offset(size.width, y), strokeWidth = w)
                            y += ruleGap
                        }
                    }
                }
            fun captureLayout(r: androidx.compose.ui.text.TextLayoutResult) {
                lineBottoms = if (r.lineCount > 0) (0 until r.lineCount).map { r.getLineBottom(it) } else emptyList()
                measuredGap = if (r.lineCount >= 2) r.getLineBottom(1) - r.getLineBottom(0) else null
            }
            // تحرير الكتل الخاصة في وضع التحرير (27 سبتمبر 2026): المؤشر داخل سطر البديل يُظهر زر تحرير خام
            var rawEditIdx by remember { mutableStateOf<Int?>(null) }
            var rawEditText by remember { mutableStateOf("") }
            val phAtCursor = remember(rich.selection, rich.annotatedString.text, specials) {
                if (readOnly || !rich.selection.collapsed) null
                else Regex("‹كتلة خاصة #(\\d+)›").findAll(rich.annotatedString.text)
                    .firstOrNull { m -> rich.selection.start in m.range }?.groupValues?.get(1)?.toIntOrNull()
                    ?.takeIf { it in specials.indices }
            }
            if (!readOnly && phAtCursor != null) {
                val kind = specials[phAtCursor].trimStart().let {
                    when {
                        it.startsWith("```") -> stringResource(R.string.special_kind_code)
                        it.startsWith(">") -> stringResource(R.string.special_kind_quote)
                        it.startsWith("|") -> stringResource(R.string.special_kind_table)
                        else -> stringResource(R.string.special_kind_divider)
                    }
                }
                AssistChip(
                    onClick = { rawEditIdx = phAtCursor; rawEditText = specials[phAtCursor] },
                    label = { Text(stringResource(R.string.special_edit_chip, kind)) },
                    leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null) },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            rawEditIdx?.let { i ->
                AlertDialog(
                    onDismissRequest = { rawEditIdx = null },
                    title = { Text(stringResource(R.string.special_edit_title)) },
                    text = {
                        OutlinedTextField(
                            value = rawEditText,
                            onValueChange = { rawEditText = it },
                            singleLine = false,
                            minLines = 4,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val old = specials.getOrNull(i)
                            if (old != null) {
                                val updated = currentMd.replaceFirst(old, rawEditText.trim())
                                if (updated != currentMd) applyExternalMd(updated)
                            }
                            rawEditIdx = null
                        }) { Text(stringResource(R.string.special_edit_save)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { rawEditIdx = null }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp)
                    .onSizeChanged { viewportH = it.height }
            ) {
                Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
                // العرض معاينة أصلية (27 سبتمبر 2026): تعرض كل الكتل (اقتباس/فاصل/جدول/كود) + تظليل البحث — المحرر الغني يسقطها
                if (readOnly) {
                    MarkdownPreview(
                        text = currentMd,
                        tint = onCol,
                        modifier = ruled,
                        onNoteLink = onOpenNoteLink,
                        onTaskToggle = { lineStart ->
                            MarkdownFormat.flipTaskLine(currentMd, lineStart)?.let { applyExternalMd(it) }
                        },
                        findQuery = if (findOpen) findQuery else "",
                        findIdx = findIdx,
                        matchBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        currentBg = MaterialTheme.colorScheme.primary
                    )
                } else {
                    BasicRichTextEditor(
                        state = rich,
                        modifier = ruled,
                        readOnly = false,
                        textStyle = ruleStyle.copy(color = onCol),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        onTextLayout = ::captureLayout,
                        decorationBox = { inner ->
                            Box {
                                if (rich.annotatedString.text.isEmpty()) {
                                    Text(
                                        stringResource(R.string.note_body_hint),
                                        style = ruleStyle,
                                        color = onCol.copy(alpha = 0.7f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                }
            }
            }
            // سطر معلومات واحد: التواريخ + العدّاد + الحفظ — بدل 3 صفوف دائمة تأكل مساحة الكتابة
            val stats = remember(currentMd) {
                // الإحصائية على النص الصافي بلا علامات Markdown — من النص المصدَّر للمحرر الغني
                val t = MarkdownFormat.stripMarkdown(currentMd)
                val lines = if (t.isBlank()) 0 else t.lines().size
                val words = if (t.isBlank()) 0 else t.trim().split("\\s+".toRegex()).size
                Triple(lines, words, t.length)
            }
            val infoParts = mutableListOf<String>()
            if (createdAt > 0L) {
                infoParts.add(
                    if (updatedAt != createdAt)
                        stringResource(R.string.created_at, fmt.format(Date(createdAt))) + " • " +
                        stringResource(R.string.updated_at, fmt.format(Date(updatedAt)))
                    else
                        stringResource(R.string.created_at, fmt.format(Date(createdAt)))
                )
            }
            infoParts.add(stringResource(R.string.stats_format, stats.first, stats.second, stats.third))
            if (!readOnly && autosave) infoParts.add(stringResource(R.string.autosave_on))
            Text(
                infoParts.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = onCol.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            // المحتوى (صور/مهام): عرض دائم كمادة الملاحظة — الإضافة من زر + وحقل المهام في التحرير
            if (attachments.isNotEmpty()) AttachmentGrid(
                attachments = attachments, readOnly = readOnly || !proAllowed, tint = onCol,
                onDelete = { confirmDelAttachment = it },
                // 28 سبتمبر 2026: عارض الصور المدمج بدل قفز لتطبيق خارجي
                onOpenImage = { viewer = it }
            )
            if (tasks.isNotEmpty() || !readOnly) ChecklistBlock(
                tasks = tasks, readOnly = readOnly || !proAllowed, tint = onCol,
                onToggle = onToggleTask, onAdd = onAddTask, onRename = onRenameTask, onDelete = { confirmDelTask = it },
                focusReq = contentFocus
            )
            // صفوف مضغوطة للموجود فقط — الضغطة (تحرير) تفتح شيت +، والعرض بلا تفاعل
            if (tags.isNotEmpty()) TagEditorRow(
                tags = tags, readOnly = true, tint = onCol,
                onChange = onTagsChange,
                modifier = Modifier.padding(top = 4.dp).then(
                    if (!readOnly) Modifier.clickable { if (proAllowed) addSheet = true else onProLocked() } else Modifier
                )
            )
            // التصنيف: رقاقة دائمًا + حقل تحرير مباشر في وضع الكتابة (مجاني — بلا شيت ولا بوابة)
            CategoryRow(
                category = category, readOnly = readOnly, tint = onCol,
                existing = existingCategories, onChange = onCategoryChange,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (reminder.at != null) ReminderRow(
                spec = reminder, readOnly = true, tint = onCol,
                onChange = onReminderChange,
                modifier = Modifier.padding(top = 4.dp).then(
                    if (!readOnly) Modifier.clickable { if (proAllowed) addSheet = true else onProLocked() } else Modifier
                )
            )
            if (latitude != null && longitude != null) LocationRow(
                latitude = latitude, longitude = longitude,
                readOnly = true, tint = onCol,
                canUse = canUseLocation,
                onChange = { _, _ -> },
                onLocked = onLocationLocked,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (geoLat != null && geoLng != null) GeofenceRow(
                geoLat = geoLat, geoLng = geoLng, geoRadiusM = geoRadiusM, geoName = geoName,
                readOnly = true, tint = onCol,
                onChange = { _, _, _, _ -> },
                onClear = {},
                modifier = Modifier.padding(top = 4.dp)
            )
            // مساحة أمان تحت المحتوى حتى لا يغطي زر + آخر سطر
            if (!readOnly) Spacer(Modifier.height(88.dp))
            // شيت زر + الشامل (27 سبتمبر 2026): بحث + أقسام — كل مميزات الملاحظة في مكان واحد
            if (addSheet) {
                ModalBottomSheet(onDismissRequest = { addSheet = false; sheetQuery = "" }) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp).padding(bottom = 24.dp)
                    ) {
                        Text(
                            stringResource(R.string.content_add),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        // سجل العناصر (28 سبتمبر 2026): مصدر واحد يبني التبويبات والبحث و"المستخدمة مؤخرًا"
                        val tplMeeting = stringResource(R.string.tpl_body_meeting, todayStr)
                        val tplDiary = stringResource(R.string.tpl_body_diary, todayStr)
                        val tplShopping = stringResource(R.string.tpl_body_shopping)
                        val mediaEntries = listOf(
                            SheetEntry("photo", Icons.Filled.AddAPhoto, stringResource(R.string.content_take_photo), act = { onTakePhoto() }),
                            SheetEntry("image", Icons.Filled.Image, stringResource(R.string.content_add_image), act = { onPickImage() }),
                            SheetEntry("file", Icons.Filled.AttachFile, stringResource(R.string.content_pick_file), stringResource(R.string.content_pick_file_d), act = { onPickFile() }),
                            SheetEntry("audio", Icons.Filled.Mic, stringResource(R.string.content_recording), act = { recDlg = true }),
                            SheetEntry("draw", Icons.Filled.Brush, stringResource(R.string.content_drawing), act = { drawDlg = true }),
                            SheetEntry("tasks", Icons.Filled.CheckBox, stringResource(R.string.content_checkboxes), act = { runCatching { contentFocus.requestFocus() } })
                        )
                        val insertEntries = listOf(
                            SheetEntry("head", Icons.Filled.Title, stringResource(R.string.ins_heading), stringResource(R.string.ins_heading_d), act = { insertBlockAtCursor("## ") }),
                            SheetEntry("code", Icons.Filled.Code, stringResource(R.string.ins_code), stringResource(R.string.ins_code_d), act = { insertBlockAtCursor("```\n\n```") }),
                            SheetEntry("quote", Icons.Filled.FormatQuote, stringResource(R.string.ins_quote), stringResource(R.string.ins_quote_d), act = { insertBlockAtCursor("> ") }),
                            SheetEntry("hr", Icons.Filled.HorizontalRule, stringResource(R.string.ins_divider), stringResource(R.string.ins_divider_d), act = { insertBlockAtCursor("---") }),
                            SheetEntry("table", Icons.Filled.TableChart, stringResource(R.string.ins_table), stringResource(R.string.ins_table_d), act = { tableDlg = true }),
                            SheetEntry("link", Icons.Filled.Link, stringResource(R.string.ins_link), stringResource(R.string.ins_link_d), free = true, act = { linkDialogOpen = true }),
                            SheetEntry("date", Icons.Filled.CalendarToday, stringResource(R.string.ins_date), stringResource(R.string.ins_date_d), act = { insertBlockAtCursor(nowStr) }),
                            SheetEntry(
                                "focus", Icons.Filled.CenterFocusStrong,
                                stringResource(R.string.focus_title),
                                stringResource(R.string.focus_sub),
                                free = true,
                                act = { focusMode = !focusMode }
                            ),
                            SheetEntry(
                                "timer", Icons.Filled.Timer,
                                stringResource(R.string.timer_title),
                                stringResource(R.string.timer_sub),
                                free = true,
                                act = { focusTimerRun = !focusTimerRun }
                            )
                        )
                        val tplEntries = listOf(
                            SheetEntry("tpl_m", Icons.Filled.Groups, stringResource(R.string.tpl_meeting), act = { applyTemplate(tplMeeting) }),
                            SheetEntry("tpl_d", Icons.Filled.Book, stringResource(R.string.tpl_diary), act = { applyTemplate(tplDiary) }),
                            SheetEntry("tpl_s", Icons.Filled.ShoppingCart, stringResource(R.string.tpl_shopping), act = { applyTemplate(tplShopping) })
                        ) + customTpls.mapIndexed { i, (nm, bd) ->
                            // القوالب المخصصة تُحذف بالضغط المطوّل (فلا تحتل صفًا بحذف دائم)
                            SheetEntry("tpl_c$i", Icons.Filled.Star, nm, stringResource(R.string.tpl_custom_use), act = { applyTemplate(bd) })
                        } + listOf(
                            SheetEntry(
                                "tpl_new", Icons.Filled.BookmarkAdd, stringResource(R.string.tpl_custom_save),
                                stringResource(R.string.tpl_custom_save_d), free = true,
                                act = { tplDlg = true }
                            )
                        )
                        val orgEntries = buildList {
                            add(SheetEntry("bg_pick", Icons.Filled.Image, stringResource(R.string.bg_pick), act = { onPickBg() }))
                            if (background != null)
                                add(SheetEntry("bg_del", Icons.Filled.Close, stringResource(R.string.bg_remove), act = { confirmRemoveBg = true }))
                            add(SheetEntry("outline", Icons.Filled.List, stringResource(R.string.sheet_outline), stringResource(R.string.sheet_outline_d), free = true, act = { outlineSheet = true }))
                            add(SheetEntry("copy", Icons.Filled.ContentCopy, stringResource(R.string.copy_note), act = { onCopy() }))
                            add(SheetEntry("pdf", Icons.Filled.PictureAsPdf, stringResource(R.string.export_pdf), act = { onExportPdf() }))
                            add(SheetEntry("md", Icons.Filled.Share, stringResource(R.string.share_markdown), act = { onShareMarkdown() }))
                            add(SheetEntry("html", Icons.Filled.Share, stringResource(R.string.share_html), act = { onShareHtml() }))
                        }
                        val allEntries = mediaEntries + insertEntries + tplEntries + orgEntries
                        // البحث بالعنوان والوصف معًا (28 سبتمبر 2026): كان بالعنوان فتبخّت الوصفات من البحث
                        fun sheetHit(e: SheetEntry, q: String) =
                            e.label.contains(q, true) || e.desc.contains(q, true)
                        val searching = sheetQuery.isNotBlank()
                        val hits = if (searching) allEntries.filter { sheetHit(it, sheetQuery) } else emptyList()
                        val clearSearchCd = stringResource(R.string.sheet_clear_cd)
                        // صف عنصر: نقرة تُغلق الشيت وتسجّل الاستخدام + القفل المرئي للمجاني (28 سبتمبر 2026)
                        @Composable
                        fun SheetRow(e: SheetEntry) {
                            ContentRow(
                                icon = e.icon,
                                label = e.label,
                                description = e.desc,
                                contentDescription = e.label,
                                onClick = {
                                    addSheet = false; sheetQuery = ""
                                    onRecentSheet((listOf(e.key) + recentSheet.filter { it != e.key }).take(6))
                                    if (e.free || proAllowed) e.act() else onProLocked()
                                }
                            )
                        }
                        // عنوان قسم: يظهر بلا بحث فقط — أثناء البحث تكون النتائج متلاصقة
                        @Composable
                        fun SheetSec(t: String) {
                            if (searching) return
                            Text(
                                t,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        OutlinedTextField(
                            value = sheetQuery,
                            onValueChange = { sheetQuery = it },
                            placeholder = { Text(stringResource(R.string.sheet_search)) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            // عدّاد النتائج + زر مسح (28 سبتمبر 2026): البحث بقى مفهومًا لما يشوف كام لقى
                            trailingIcon = {
                                if (searching) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            stringResource(R.string.sheet_results, hits.size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        IconButton(
                                            onClick = { sheetQuery = "" },
                                            modifier = Modifier.size(40.dp).semantics { contentDescription = clearSearchCd }
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 4.dp)
                        )
                        // التبويبات: القائمة الطويلة مقسومة ثلاثة (28 سبتمبر 2026) — وتختفي أثناء البحث
                        if (!searching) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    stringResource(R.string.sheet_tab_media),
                                    stringResource(R.string.sheet_tab_insert),
                                    stringResource(R.string.sheet_tab_org)
                                ).forEachIndexed { i, t ->
                                    FilterChip(
                                        selected = sheetTab == i,
                                        onClick = { sheetTab = i },
                                        label = { Text(t) },
                                        modifier = Modifier.semantics { contentDescription = t }
                                    )
                                }
                            }
                        }
                        // مستخدمة مؤخرًا (28 سبتمبر 2026): آخر 6 إضافات — أسرع طريق للعنصر المتكرر
                        if (!searching && recentSheet.isNotEmpty()) {
                            SheetSec(stringResource(R.string.sheet_recent))
                            recentSheet.forEach { k -> allEntries.firstOrNull { it.key == k }?.let { SheetRow(it) } }
                        }
                        if (searching) {
                            if (hits.isEmpty()) {
                                Text(
                                    stringResource(R.string.sheet_no_results),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            } else hits.forEach { SheetRow(it) }
                        } else when (sheetTab) {
                            0 -> { SheetSec(stringResource(R.string.sheet_sec_media)); mediaEntries.forEach { SheetRow(it) } }
                            1 -> {
                                SheetSec(stringResource(R.string.sheet_sec_text)); insertEntries.forEach { SheetRow(it) }
                                SheetSec(stringResource(R.string.sheet_sec_tpl)); tplEntries.forEach { SheetRow(it) }
                            }
                            else -> {
                                SheetSec(stringResource(R.string.sheet_sec_org))
                                Text(
                                    stringResource(R.string.bg_colors),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                ColorPickerRow(selected = color, onPick = { if (proAllowed) onColor(it) else onProLocked() })
                                orgEntries.forEach { SheetRow(it) }
                            }
                        }
                        // حقول التنظيم (28 سبتمبر 2026): تبقى ظاهرة دائمًا — كانت تختفي أثناء البحث فتبدو معطّلة
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        Text(
                            stringResource(R.string.sheet_sec_fields),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            // التصنيف مدمج في قائمة الإضافة (27 سبتمبر 2026): تحرير مباشر مجاني — بلا شيت ولا بوابة
                            CategoryRow(
                                category = category, readOnly = false,
                                tint = MaterialTheme.colorScheme.onSurface,
                                existing = existingCategories, onChange = onCategoryChange,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            TagEditorRow(
                                tags = tags, readOnly = !proAllowed,
                                tint = MaterialTheme.colorScheme.onSurface,
                                onChange = onTagsChange,
                                modifier = Modifier.padding(top = 8.dp).then(
                                    if (proAllowed) Modifier else Modifier.clickable { onProLocked() }
                                )
                            )
                            ReminderRow(
                                spec = reminder, readOnly = !proAllowed,
                                tint = MaterialTheme.colorScheme.onSurface,
                                onChange = onReminderChange,
                                modifier = Modifier.padding(top = 4.dp).then(
                                    if (proAllowed) Modifier else Modifier.clickable { onProLocked() }
                                )
                            )
                            LocationRow(
                                latitude = latitude, longitude = longitude,
                                readOnly = false, tint = MaterialTheme.colorScheme.onSurface,
                                canUse = canUseLocation,
                                onChange = onLocationChange,
                                onLocked = onLocationLocked,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            // تذكير الموقع داخل الشيت — مدفوعة كالتذكير الزمني (27 سبتمبر 2026)
                            GeofenceRow(
                                geoLat = geoLat, geoLng = geoLng, geoRadiusM = geoRadiusM, geoName = geoName,
                                readOnly = !proAllowed,
                                tint = MaterialTheme.colorScheme.onSurface,
                                onChange = onGeofenceChange,
                                onClear = onGeofenceClear,
                                modifier = Modifier.padding(top = 4.dp).then(
                                    if (proAllowed) Modifier else Modifier.clickable { onProLocked() }
                                )
                            )
                        }
                    }
                }
            }
            // حوارات التأكيد قبل الإجراءات (بأمر محمد 27 سبتمبر 2026)
            if (confirmArchive) {
                ConfirmDialog(
                    message = stringResource(R.string.confirm_archive_msg),
                    onConfirm = { confirmArchive = false; onArchive() },
                    onDismiss = { confirmArchive = false }
                )
            }
            if (confirmClearFormat) {
                ConfirmDialog(
                    message = stringResource(R.string.confirm_clear_format_msg),
                    onConfirm = {
                        confirmClearFormat = false
                        pushHistory()
                        skipNextAutoPush = true
                        rich.clearSpanStyles()
                        rich.clearRichSpans()
                    },
                    onDismiss = { confirmClearFormat = false }
                )
            }
            if (confirmRemoveBg) {
                ConfirmDialog(
                    message = stringResource(R.string.confirm_remove_bg_msg),
                    onConfirm = { confirmRemoveBg = false; onRemoveBg() },
                    onDismiss = { confirmRemoveBg = false }
                )
            }
            confirmDelAttachment?.let { a ->
                ConfirmDialog(
                    message = stringResource(R.string.confirm_del_attachment_msg),
                    onConfirm = { confirmDelAttachment = null; onDeleteAttachment(a) },
                    onDismiss = { confirmDelAttachment = null },
                    danger = true
                )
            }
            confirmDelTask?.let { id ->
                ConfirmDialog(
                    message = stringResource(R.string.confirm_del_task_msg),
                    onConfirm = { confirmDelTask = null; onDeleteTask(id) },
                    onDismiss = { confirmDelTask = null },
                    danger = true
                )
            }
            if (recDlg) {
                RecordingDialog(
                    onDismiss = { recDlg = false },
                    onSave = { f -> onSaveAudio(f); recDlg = false }
                )
            }
            if (drawDlg) {
                DrawingDialog(
                    onDismiss = { drawDlg = false },
                    onSave = { f -> onSaveDrawing(f); drawDlg = false }
                )
            }
            // Link dialog (appears when link button in contextual toolbar is pressed)
            if (linkDialogOpen) {
                LinkDialog(
                    onDismiss = { linkDialogOpen = false },
                    onConfirm = { url ->
                        linkDialogOpen = false
                        // رابط عبر الدالة العامة: فارغ + رابط حالي = إزالة — بلا تحديد يُتجاهل
                        if (url.isBlank()) { if (rich.isLink) rich.removeLink() }
                        else if (!rich.selection.collapsed) rich.addLinkToSelection(url)
                    }
                )
            }
            // الذكاء الاصطناعي: اختيار المهمة ثم معاينة النتيجة — الاستخدام يدفع history فيستعيده التراجع
            if (aiSheet) {
                ModalBottomSheet(onDismissRequest = { if (!aiWorking) { aiSheet = false; aiError = null } }) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.ai_title), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.ai_sheet_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        val errCode = aiError
                        if (errCode != null) {
                            val msg = when {
                                errCode == "nokey" -> stringResource(R.string.ai_err_nokey)
                                errCode == "empty" -> stringResource(R.string.ai_err_empty)
                                errCode == "https-required" -> stringResource(R.string.ai_err_https)
                                errCode == "net" -> stringResource(R.string.ai_err_net)
                                errCode.startsWith("http:") -> stringResource(R.string.ai_err_http, errCode.removePrefix("http:"))
                                else -> stringResource(R.string.ai_err_parse)
                            }
                            Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                        }
                        fun aiRun(mode: String) {
                            aiMode = mode; aiError = null; aiWorking = true
                            onAiRun(mode, currentMd) { r ->
                                aiWorking = false
                                r.onSuccess { aiResult = it }.onFailure { aiError = it.message ?: "parse" }
                            }
                        }
                        Button(
                            onClick = { aiRun("rewrite") },
                            enabled = !aiWorking && currentMd.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (aiWorking) stringResource(R.string.ai_working) else stringResource(R.string.ai_rewrite))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { aiRun("format") },
                            enabled = !aiWorking && currentMd.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.ai_format))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { aiRun("summarize") },
                            enabled = !aiWorking && currentMd.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.ai_summarize))
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            val ar = aiResult
            if (ar != null) {
                AlertDialog(
                    onDismissRequest = { aiResult = null },
                    title = { Text(stringResource(R.string.ai_preview_title)) },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(ar, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pushHistory()
                            skipNextAutoPush = true
                            applyExternalMd(ar)
                            aiResult = null; aiSheet = false
                        }) { Text(stringResource(R.string.ai_use)) }
                    },
                    dismissButton = {
                        Row {
                            // إعادة المحاولة بنفس المهمة + نسخ النتيجة — بلا خروج من المعاينة
                            TextButton(onClick = {
                                aiError = null; aiWorking = true
                                onAiRun(aiMode, currentMd) { r ->
                                    aiWorking = false
                                    r.onSuccess { aiResult = it }.onFailure { aiError = it.message ?: "parse" }
                                }
                            }) { Text(if (aiWorking) stringResource(R.string.ai_working) else stringResource(R.string.ai_retry)) }
                            TextButton(onClick = {
                                aiClipboard.setText(androidx.compose.ui.text.AnnotatedString(ar))
                            }) { Text(stringResource(R.string.ai_copy)) }
                            TextButton(onClick = { aiResult = null }) { Text(stringResource(R.string.ai_discard)) }
                        }
                    }
                )
            }
            // عارض الصور المدمج (28 سبتمبر 2026)
            viewer?.let { a ->
                ImageViewerDialog(attachment = a, onDismiss = { viewer = null })
            }
            // القوالب المخصصة: حوار يحفظ النص الحالي + يحذف القديم (28 سبتمبر 2026)
            if (tplDlg) {
                SaveTemplateDialog(
                    preview = currentMd.take(4000),
                    existing = customTpls,
                    onSave = { nm -> onSaveTpl(nm, currentMd); tplDlg = false },
                    onDelete = { i -> onDelTpl(i) },
                    onDismiss = { tplDlg = false }
                )
            }
            // محرّر الجدول (28 سبتمبر 2026): شبكة خلايا حقيقية — بدل إدراج نص Markdown جاهز
            if (tableDlg) {
                TableEditorDialog(
                    onDismiss = { tableDlg = false },
                    onInsert = { md -> tableDlg = false; insertBlock(md) }
                )
            }
            // فهرس العناوين: قفز بتحديد أول تطابق في النص — للعرض والتحرير معًا (بلا طي: لا يدعمه المحرر)
            if (outlineSheet) {
                OutlineSheet(
                    headings = headings,
                    folded = emptySet(),
                    hasBody = { _ -> false },
                    onToggleFold = { _ -> },
                    onJump = { h ->
                        outlineSheet = false
                        // قفزة مؤشر فقط — بلا دفع في history التراجع
                        val idx = rich.annotatedString.text.indexOf(h.text)
                        if (idx >= 0) rich.selection = TextRange(idx, idx)
                    },
                    onDismiss = { outlineSheet = false }
                )
            }
        }
        // القائمة الدائرية عائمة فوق الكيبورد أبدًا وقابلة للإمساك (28 سبتمبر 2026)
        if (!readOnly) {
            // موضع القائمة وطويها يبقيان عند الدوران — Offset يُحفظ كقائمة [x, y]
            var barOff by rememberSaveable(createdAt, stateSaver = Saver(
                save = { o: Offset -> listOf(o.x, o.y) },
                restore = { Offset(it.getOrElse(0) { 0f }, it.getOrElse(1) { 0f }) }
            )) { mutableStateOf(Offset.Zero) }
            var barCollapsed by rememberSaveable(createdAt) { mutableStateOf(false) }
            // أبعاد الحاوية المرئية فوق الكيبورد: منها تُحسب حدود السحب (لا تغادر الشاشة أبدًا)
            var barAreaPx by remember { mutableStateOf(IntSize.Zero) }
            val den2 = LocalDensity.current
            val menuSpanPx = with(den2) { 264.dp.toPx() }          // قطر الحلقة المرسومة
            val topSafePx = with(den2) { 150.dp.toPx() }           // أعلى ارتفاع مسموح (تحت الشريط العلوي)
            // الحدود: أفقيًا نصف ما زاد عن قطر القائمة، ورأسيًا صعودًا فقط — فلا تختفي تحت الكيبورد أبدًا
            val maxUp = (barAreaPx.height - topSafePx - menuSpanPx).coerceAtLeast(0f)
            val maxSide = ((barAreaPx.width - menuSpanPx) / 2f).coerceAtLeast(0f)
            fun moveBy(dx: Float, dy: Float) {
                barOff = Offset((barOff.x + dx).coerceIn(-maxSide, maxSide), (barOff.y + dy).coerceIn(-maxUp, 0f))
            }
            Box(
                Modifier
                    .fillMaxSize()
                    // القاعدة الذهبية: imePadding على الحاوية نفسها — فترتفع القائمة مع الكيبورد تلقائيًا
                    .imePadding()
                    .onSizeChanged { barAreaPx = it },
                contentAlignment = Alignment.BottomCenter
            ) {
                RichFormattingToolbar(
                    rich = rich,
                    onOutline = { outlineSheet = true },
                    onLink = { linkDialogOpen = true },
                    effects = effects,
                    onMove = ::moveBy,
                    onReset = { barOff = Offset.Zero },
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 24.dp)
                        .offset { IntOffset(barOff.x.roundToInt(), barOff.y.roundToInt()) },
                    collapsed = barCollapsed,
                    onToggleCollapse = { barCollapsed = !barCollapsed },
                    onAi = { if (aiReady) aiSheet = true else onAiSetup() },
                    onClearFormatting = {
                        // بلا تحديد موسَّع: لا شيء يُمسح — نتجاهل حتى لا تتلوث history التراجع
                        if (rich.selection.collapsed) return@RichFormattingToolbar
                        confirmClearFormat = true
                    }
                )
            }
        }
        }
    }
}
}

// صف التصنيف: رقاقة العرض + حقل التحرير المباشر مع اقتراحات الموجود — مجاني دائمًا
// FlowRow في اقتراحات التصنيف تحتاج ExperimentalLayoutApi
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun CategoryRow(
    category: String,
    readOnly: Boolean,
    tint: Color,
    existing: List<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        if (category.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Icon(Icons.Filled.Label, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Text(
                    category,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        if (!readOnly) {
            OutlinedTextField(
                value = category,
                onValueChange = { onChange(it.take(64)) },
                label = { Text(stringResource(R.string.cat_field_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
            // اقتراحات التصنيف (28 سبتمبر 2026): عدّاد + بحث + عرض مُوسّع + حالة "لا نتائج"
            // بدل 6 أزرار مبهمة — فمع 30 تصنيفًا كان المستخدم بلا طريق للوصول لباقيهم
            val pool = existing.filter { it != category }
            var catQuery by remember(category) { mutableStateOf("") }
            var catOpen by remember(category) { mutableStateOf(false) }
            val filtered = if (catQuery.isBlank()) pool else pool.filter { it.contains(catQuery, true) }
            val showAll = catOpen || catQuery.isNotBlank()
            if (pool.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.cat_count, pool.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (pool.size > 6) {
                        TextButton(
                            onClick = { catOpen = !catOpen },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text(stringResource(if (catOpen) R.string.cat_less else R.string.cat_more_btn, if (catOpen) 0 else pool.size - 6)) }
                    }
                }
                // حقل البحث يظهر فقط مع تعدد التصنيفات — ويُضيّق الاقتراحات فورًا
                if (pool.size > 6 && !catOpen) {
                    OutlinedTextField(
                        value = catQuery,
                        onValueChange = { catQuery = it },
                        placeholder = { Text(stringResource(R.string.cat_search_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    )
                }
                if (filtered.isEmpty()) {
                    Text(
                        stringResource(R.string.cat_no_results),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    val shown = if (showAll) filtered else filtered.take(6)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        shown.forEach { s ->
                            TextButton(
                                onClick = { onChange(s); catQuery = "" },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) { Text(s) }
                        }
                    }
                }
            }
        }
    }
}

// صف التذكير: جرس + ملخص الموعد — الضغطة تفتح حوار الضبط (كتابة فقط)، وفي العرض يعرض فقط
@Composable
internal fun ReminderRow(
    spec: ReminderSpec,
    readOnly: Boolean,
    tint: Color,
    onChange: (ReminderSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    var dlg by remember { mutableStateOf(false) }
    val fmt = remember { java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.getDefault()) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (!readOnly) dlg = true }, enabled = !readOnly) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = stringResource(R.string.reminder_title),
                tint = if (spec.at != null) tint else tint.copy(alpha = 0.5f)
            )
        }
        Text(
            if (spec.at != null) reminderSummary(spec, fmt) else stringResource(R.string.reminder_none),
            style = MaterialTheme.typography.labelMedium,
            color = tint.copy(alpha = 0.8f),
            modifier = Modifier.then(if (!readOnly) Modifier.clickable { dlg = true } else Modifier)
        )
    }
    if (dlg) {
        ReminderDialog(
            init = spec,
            onDismiss = { dlg = false },
            onSave = { s -> onChange(s); dlg = false },
            onClear = { onChange(ReminderSpec()); dlg = false }
        )
    }
}

// صف تذكير الموقع: دبوس ساعة + ملخص السياج — الضغطة تفتح حوار الضبط (27 سبتمبر 2026)
@Composable
internal fun GeofenceRow(
    geoLat: Double?,
    geoLng: Double?,
    geoRadiusM: Float,
    geoName: String,
    readOnly: Boolean,
    tint: Color,
    onChange: (Double?, Double?, Float, String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dlg by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (!readOnly) dlg = true }, enabled = !readOnly) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = stringResource(R.string.geo_title),
                tint = if (geoLat != null && geoLng != null) tint else tint.copy(alpha = 0.5f)
            )
        }
        Text(
            if (geoLat != null && geoLng != null)
                if (geoName.isNotBlank()) stringResource(R.string.geo_summary_named, geoName, geoRadiusM.toInt())
                else stringResource(R.string.geo_summary_fmt, geoRadiusM.toInt())
            else stringResource(R.string.geo_none),
            style = MaterialTheme.typography.labelMedium,
            color = tint.copy(alpha = 0.8f),
            modifier = Modifier.then(if (!readOnly) Modifier.clickable { dlg = true } else Modifier)
        )
    }
    if (dlg) {
        GeofenceDialog(
            initLat = geoLat, initLng = geoLng, initRadius = geoRadiusM, initName = geoName,
            onDismiss = { dlg = false },
            onSave = { la, ln, r, name -> onChange(la, ln, r, name); dlg = false },
            onClear = { onClear(); dlg = false }
        )
    }
}

// حوار السياج: الموقع الحالي + نصف القطر + الاسم — يحتاج موقعًا دقيقًا (أمامي) وخلفية للدخول الموثوق
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun GeofenceDialog(
    initLat: Double?,
    initLng: Double?,
    initRadius: Float,
    initName: String,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Float, String) -> Unit,
    onClear: () -> Unit
) {
    val ctx = LocalContext.current
    var lat by remember { mutableStateOf(initLat) }
    var lng by remember { mutableStateOf(initLng) }
    var radius by remember { mutableStateOf(initRadius.coerceIn(100f, 2000f)) }
    var name by remember { mutableStateOf(initName) }
    var locating by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var askedBg by remember { mutableStateOf(false) }
    fun fetch() {
        locating = true; err = null
        try {
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { loc ->
                    locating = false
                    if (loc != null) { lat = loc.latitude; lng = loc.longitude }
                    else err = ctx.getString(R.string.location_failed)
                }
                .addOnFailureListener { locating = false; err = ctx.getString(R.string.location_failed) }
        } catch (e: SecurityException) { locating = false; err = ctx.getString(R.string.location_denied) }
    }
    val bgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { fetch() }
    val fineLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) { err = ctx.getString(R.string.location_denied); return@rememberLauncherForActivityResult }
        // موقع الخلفية (للدخول والتطبيق مغلق) — الرفض لا يمنع الحفظ لكن السياج قد لا ينطلق مغلقًا
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && !askedBg) {
            askedBg = true
            val bgOk = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!bgOk) runCatching {
                bgLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                return@rememberLauncherForActivityResult
            }
        }
        fetch()
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.geo_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedButton(
                    onClick = {
                        val ok = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (ok) fetch() else fineLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            locating -> stringResource(R.string.location_locating)
                            lat != null && lng != null -> stringResource(R.string.geo_picked_fmt, lat!!, lng!!)
                            else -> stringResource(R.string.geo_use_current)
                        }
                    )
                }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp)) }
                Text(stringResource(R.string.geo_radius), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    listOf(100f to "100م", 200f to "200م", 500f to "500م", 1000f to "1كم").forEach { (v, label) ->
                        ReminderChip(text = label, isSel = radius == v, onClick = { radius = v })
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(64) },
                    label = { Text(stringResource(R.string.geo_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                if (lat != null && lng != null) {
                    TextButton(onClick = onClear, modifier = Modifier.padding(top = 4.dp)) {
                        Text(stringResource(R.string.geo_clear), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { val la = lat; val ln = lng; if (la != null && ln != null) onSave(la, ln, radius, name) },
                enabled = lat != null && lng != null
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

// صف الموقع: دبوس + الإحداثيات — الالتقاط يطلب إذن الموقع أولًا (مدفوعة) — والضغطة على الإحداثيات تفتح الخرائط
@Composable
internal fun LocationRow(
    latitude: Double?,
    longitude: Double?,
    readOnly: Boolean,
    tint: Color,
    canUse: Boolean,
    onChange: (Double?, Double?) -> Unit,
    onLocked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var locating by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var askedOnce by remember { mutableStateOf(false) }
    // الرفض الدائم (لا تسأل مجددًا): يُفتح إعدادات التطبيق مباشرة بدل طلب ميت
    fun openAppSettings() {
        runCatching {
            ctx.startActivity(android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:com.nota.notes")
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    fun fetch() {
        locating = true; err = null
        try {
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { loc ->
                    if (loc != null) { locating = false; onChange(loc.latitude, loc.longitude) }
                    else client.lastLocation
                        .addOnSuccessListener { last ->
                            locating = false
                            if (last != null) onChange(last.latitude, last.longitude)
                            else err = ctx.getString(R.string.location_failed)
                        }
                        .addOnFailureListener { locating = false; err = ctx.getString(R.string.location_failed) }
                }
                .addOnFailureListener { locating = false; err = ctx.getString(R.string.location_failed) }
        } catch (e: SecurityException) { locating = false; err = ctx.getString(R.string.location_denied) }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { fetch(); return@rememberLauncherForActivityResult }
        // مرفوض: دائم (بلا حوار قادم) → الإعدادات — ومؤقت → رسالة فقط
        val act = ctx as? android.app.Activity
        val permanent = askedOnce && act != null &&
            !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(act, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (permanent) { err = ctx.getString(R.string.location_denied_forever); openAppSettings() }
        else err = ctx.getString(R.string.location_denied)
    }
    fun attach() {
        if (readOnly) return
        if (!canUse) { onLocked(); return }
        val ok = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (ok) fetch() else { askedOnce = true; permLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION) }
    }
    val coords = if (latitude != null && longitude != null)
        String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude) else null
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (coords != null) openMap(ctx, latitude, longitude) else attach() }, enabled = !readOnly) {
            Icon(
                Icons.Filled.Place,
                contentDescription = stringResource(R.string.location_attach),
                tint = if (coords != null) tint else tint.copy(alpha = 0.5f)
            )
        }
        Text(
            when {
                locating -> stringResource(R.string.location_locating)
                err != null -> err!!
                coords != null -> coords
                else -> stringResource(R.string.location_no)
            },
            style = MaterialTheme.typography.labelMedium,
            color = tint.copy(alpha = 0.8f),
            modifier = Modifier.clickable(enabled = !readOnly) { if (coords != null) openMap(ctx, latitude, longitude) else attach() }
        )
        if (coords != null && !readOnly) {
            IconButton(onClick = { onChange(null, null) }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.location_remove), tint = tint.copy(alpha = 0.7f))
            }
        }
    }
}

// فتح الإحداثيات في تطبيق الخرائط (اختيار المستخدم — يعمل مع خرائط أوفلاين أيضًا)
private fun openMap(ctx: android.content.Context, lat: Double?, lng: Double?) {
    if (lat == null || lng == null) return
    val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng")
    runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri)) }
}

// ملخص التذكير: التاريخ + وصف القاعدة (بسيط أو مخصص) + الإشعار المسبق في سطر واحد
@Composable
private fun reminderSummary(spec: ReminderSpec, fmt: java.text.SimpleDateFormat): String {
    val at = spec.at ?: return stringResource(R.string.reminder_none)
    val rep = repeatDescribe(spec)
    val bef = when (spec.beforeMin) {
        10L -> stringResource(R.string.before_10m)
        60L -> stringResource(R.string.before_1h)
        1440L -> stringResource(R.string.before_1d)
        else -> stringResource(R.string.before_at)
    }
    return stringResource(R.string.reminder_summary, fmt.format(java.util.Date(at)), rep, bef)
}

// وصف قاعدة التكرار: البسيط بكلمة واحدة — والمخصص بجملة (كل يومين • أيام محددة • حتى تاريخ…)
@Composable
fun repeatDescribe(spec: ReminderSpec): String {
    if (spec.repeat == "none") return stringResource(R.string.repeat_once)
    val unit = when (spec.repeat) {
        "daily" -> stringResource(R.string.repeat_unit_day)
        "weekly" -> stringResource(R.string.repeat_unit_week)
        "monthly" -> stringResource(R.string.repeat_unit_month)
        else -> stringResource(R.string.repeat_unit_year)
    }
    val every = if (spec.interval > 1) stringResource(R.string.repeat_every_n, spec.interval, unit) else when (spec.repeat) {
        "daily" -> stringResource(R.string.repeat_daily)
        "weekly" -> stringResource(R.string.repeat_weekly)
        "monthly" -> stringResource(R.string.repeat_monthly)
        else -> stringResource(R.string.repeat_yearly)
    }
    val extra = when {
        spec.repeat == "weekly" && spec.days.isNotBlank() -> " • " + weekdayNames(parseDaysUi(spec.days))
        spec.repeat == "monthly" && spec.monthMode == ReminderSpec.MONTH_NTH -> " • " + stringResource(R.string.repeat_month_nth)
        else -> ""
    }
    val tail = when (spec.end) {
        ReminderSpec.END_UNTIL -> " • " + stringResource(R.string.repeat_until,
            java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(spec.endValue)))
        ReminderSpec.END_COUNT -> " • " + stringResource(R.string.repeat_count, spec.endValue)
        else -> ""
    }
    return every + extra + tail
}

// أسماء أيام الأسبوع مختصرة بترتيب السبت→الجمعة — تُستخدم في الرقائق والملخص
@Composable
private fun weekdayShort(day: Int): String = stringResource(when (day) {
    java.util.Calendar.SATURDAY -> R.string.day_sat
    java.util.Calendar.SUNDAY -> R.string.day_sun
    java.util.Calendar.MONDAY -> R.string.day_mon
    java.util.Calendar.TUESDAY -> R.string.day_tue
    java.util.Calendar.WEDNESDAY -> R.string.day_wed
    java.util.Calendar.THURSDAY -> R.string.day_thu
    else -> R.string.day_fri
})

// تحليل CSV الأيام للعرض — مرتبة سبت→جمعة
private fun parseDaysUi(csv: String): List<Int> {
    val order = listOf(7, 1, 2, 3, 4, 5, 6)
    val set = csv.split(",").mapNotNull { it.trim().toIntOrNull()?.takeIf { d -> d in 1..7 } }.toSet()
    return order.filter { it in set }
}

@Composable
private fun weekdayNames(days: List<Int>): String {
    val ctx = LocalContext.current
    return days.joinToString("، ") { d -> weekdayShortStr(ctx, d) }
}

// نسخة بلا compose للاستخدام داخل لامدا joinToString — نفس الخريطة حرفيًا
private fun weekdayShortStr(ctx: android.content.Context, day: Int): String = ctx.getString(when (day) {
    java.util.Calendar.SATURDAY -> R.string.day_sat
    java.util.Calendar.SUNDAY -> R.string.day_sun
    java.util.Calendar.MONDAY -> R.string.day_mon
    java.util.Calendar.TUESDAY -> R.string.day_tue
    java.util.Calendar.WEDNESDAY -> R.string.day_wed
    java.util.Calendar.THURSDAY -> R.string.day_thu
    else -> R.string.day_fri
})

// شريط التنسيق الكامل (قرار 26 سبتمبر 2026): أزرار رفيعة على الدوال العامة فقط — بلا مساس بالمكتبة
// المعروض فقط ما ينجو من الحفظ (B/I/S/كود/رابط/H1-H3/قوائم) — الاقتباس والفاصل والمهام بلا استدارة Markdown
@Composable
internal fun RichFormattingToolbar(
    rich: RichTextState,
    onOutline: () -> Unit,
    onLink: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    dragHandle: (@Composable () -> Unit)? = null,
    collapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
    // الذكاء الاصطناعي (27 سبتمبر 2026): قمر إضافي يفتح شيت إعادة الكتابة/التنسيق
    // البحث حُذف من الدائرة (27 سبتمبر 2026): موجود في الشريط العلوي بجوار الحفظ — بلا تكرار
    onAi: () -> Unit = {},
    onClearFormatting: () -> Unit = { rich.clearSpanStyles(); rich.clearRichSpans() },
    // 28 سبتمبر 2026: إمساك وتحريك + أنيميشن واهتزاز أقوى، وكلها تُطفأ من إعداد التأثيرات
    effects: Boolean = true,
    onMove: (Float, Float) -> Unit = { _, _ -> },
    onReset: () -> Unit = {}
) {
    // القائمة الدائرية (27 سبتمبر 2026): زر المنتصف يفتح/يغلق — والأدوات على محيط الدائرة
    // بلا شريط أفقي ولا سحب — المقبض القديم (dragHandle) لم يعد يُعرض
    val hctx = LocalContext.current
    val open = !collapsed
    // وصفان للإمكانية: القائمة تُسحب وتُعاد للوسط بضغطة مطوّلة (28 سبتمبر 2026)
    val moveDesc = stringResource(R.string.radial_move_cd)
    val resetDesc = stringResource(R.string.radial_reset_cd)
    // زر الرجوع يقفل الدائرة أولًا بدل الخروج من المحرر (تحسين 27 سبتمبر 2026)
    BackHandler(enabled = open) { onToggleCollapse() }
    val density = LocalDensity.current
    // الحالات النشطة من الواجهة العامة — تُقرأ في التركيب فتُحدَّث الأزرار مع التحديد
    val cur = rich.currentSpanStyle
    val isBold = (cur.fontWeight?.weight ?: 400) > 400
    val isItalic = cur.fontStyle == FontStyle.Italic
    val isStrike = cur.textDecoration?.contains(TextDecoration.LineThrough) == true
    // عناصر الدائرة: نص أو أيقونة — تُبنى على المحيط بزوايا متساوية
    val linkLabel = stringResource(R.string.format_link)
    val sats = listOf(
        RadialSat(null, Icons.Filled.List, stringResource(R.string.outline_title), false, closeAfter = true, act = onOutline),
        RadialSat("B", null, "B", isBold) { rich.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
        RadialSat("I", null, "I", isItalic) { rich.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
        RadialSat("S", null, "S", isStrike) { rich.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) },
        RadialSat("<>", null, "<>", rich.isCodeSpan) { rich.toggleCodeSpan() },
        RadialSat("H1", null, "H1", false) { rich.toggleSpanStyle(SpanStyle(fontSize = 2.em, fontWeight = FontWeight.Bold)) },
        RadialSat("H2", null, "H2", false) { rich.toggleSpanStyle(SpanStyle(fontSize = 1.5.em, fontWeight = FontWeight.Bold)) },
        RadialSat("H3", null, "H3", false) { rich.toggleSpanStyle(SpanStyle(fontSize = 1.17.em, fontWeight = FontWeight.Bold)) },
        RadialSat("•", null, "•", rich.isUnorderedList) { rich.toggleUnorderedList() },
        RadialSat("1.", null, "1.", rich.isOrderedList) { rich.toggleOrderedList() },
        RadialSat(null, Icons.Filled.Link, linkLabel, rich.isLink, closeAfter = true, act = onLink),
        RadialSat(null, Icons.Filled.FormatClear, stringResource(R.string.format_clear_cd), false, closeAfter = true, act = onClearFormatting),
        RadialSat("AI", null, stringResource(R.string.ai_title), false, closeAfter = true, act = onAi)
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // خلفية الحلقة: تلتقط الضغطات على الفراغ بين الأزرار — ضغط مطوّل = إعادة الضبط للوسط
        Box(
            Modifier.matchParentSize()
                .pointerInput(effects) {
                    detectTapGestures(onLongPress = { Haptics.confirm(hctx); onReset() })
                }
                .semantics {
                    contentDescription = resetDesc
                    customActions = listOf(CustomAccessibilityAction(resetDesc) { onReset(); true })
                }
        )
        Box(
            // الإمساك بالضغط المطوّل فقط (28 سبتمبر 2026): السحب الفوري كان يختطف تحديد النص
            // تحت القائمة — فصرنا نطلب ضغطًا مطوّلًا، وهو النمط المعتاد للأدوات العائمة
            modifier = Modifier.size(264.dp)
                .pointerInput(effects) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { Haptics.grab(hctx) },
                        onDragEnd = { Haptics.release(hctx) },
                        onDragCancel = {},
                        onDrag = { change, drag ->
                            change.consume()
                            onMove(drag.x, drag.y)
                        }
                    )
                }
                .semantics { contentDescription = moveDesc },
            contentAlignment = Alignment.Center
        ) {
            sats.forEachIndexed { i, s ->
                // من القمة ومع عقارب الساعة — محايد الاتجاه (بلا مشكلة RTL)
                // دخول متدرج: كل زر يتأخر عن سابقه 18ms — والخروج فوري متزامن
                val r by animateDpAsState(
                    targetValue = if (open) 104.dp else 0.dp,
                    animationSpec = if (effects) {
                        if (open) tween(320, delayMillis = i * 22) else tween(170)
                    } else snap(),
                    label = "radial$i"
                )
                val a by animateFloatAsState(
                    targetValue = if (open) 1f else 0f,
                    animationSpec = if (effects) {
                        if (open) tween(220, delayMillis = i * 22) else tween(120)
                    } else snap(),
                    label = "radialA$i"
                )
                // قفزة مرنة عند الاكتمال — إحساس حي بدل الظهور الجامد (فورية عند إطفاء التأثيرات)
                val pop by animateFloatAsState(
                    targetValue = if (open) 1f else 0.5f,
                    animationSpec = if (effects) spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)
                    else snap(),
                    label = "radialP$i"
                )
                val ang = (-90.0 + i * (360.0 / sats.size)) * kotlin.math.PI / 180.0
                val rp = with(density) { r.toPx() }
                val dx = (kotlin.math.cos(ang) * rp).roundToInt()
                val dy = (kotlin.math.sin(ang) * rp).roundToInt()
                Surface(
                    shape = CircleShape,
                    color = if (s.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size((44 * pop).dp)
                        .offset { IntOffset(dx, dy) }
                        .alpha(a)
                ) {
                    // اللقطات الفردية (رابط/مسح/فهرس) تقفل الدائرة بعد الفعل — والمفاتيح تبقيها مفتوحة
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().let {
                        if (open) it.clickable(onClick = {
                            if (s.closeAfter) Haptics.confirm(hctx) else Haptics.tick(hctx)
                            s.act(); if (s.closeAfter) onToggleCollapse()
                        }) else it
                    }) {
                        if (s.icon != null) {
                            Icon(
                                s.icon, contentDescription = s.desc,
                                tint = if (s.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                s.label ?: "", style = MaterialTheme.typography.labelLarge,
                                color = if (s.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            // زر المنتصف: يفتح ويغلق الدائرة — بنبضة اهتزاز اتجاهية + تنفّس-spring
            val fabScale by animateFloatAsState(
                targetValue = if (open) 1f else 0.94f,
                animationSpec = if (effects) spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium) else snap(),
                label = "radialFab"
            )
            FloatingActionButton(
                onClick = {
                    if (effects) { if (collapsed) Haptics.open(hctx) else Haptics.close(hctx) } else Haptics.tick(hctx)
                    onToggleCollapse()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(60.dp).graphicsLayer {
                    scaleX = fabScale; scaleY = fabScale
                }
            ) {
                Icon(
                    if (open) Icons.Filled.Close else Icons.Filled.FormatSize,
                    contentDescription = stringResource(if (open) R.string.bar_collapse_cd else R.string.bar_expand_cd)
                )
            }
        }
    }
}

// عنصر القائمة الدائرية: تسمية نصية أو أيقونة + حالة نشاط + فعل
private data class RadialSat(
    val label: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val desc: String,
    val active: Boolean,
    val closeAfter: Boolean = false,
    val act: () -> Unit
)

@Composable
private fun BarBtn(label: String, active: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BarDivider() {
    VerticalDivider(
        modifier = Modifier.height(24.dp).padding(horizontal = 2.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

// محرّر الجدول (28 سبتمبر 2026): شبكة خلايا قابلة للتحرير + صفوف/أعمدة متغيرة — والمخرج Markdown
// عارض الصور المدمج (28 سبتمبر 2026): ملء الشاشة + تكبير باللمس + سحب + نقر مزدوج للتهيئة
// بدل القفز لتطبيق خارجي — كان بيخرج من النوتة تمامًا
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageViewerDialog(
    attachment: NoteAttachment,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var bmp by remember(attachment.file) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(attachment.file) { bmp = NoteMediaFiles.load(ctx, attachment.file) }
    val closeCd = stringResource(R.string.viewer_close_cd)
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val b = bmp
            if (b != null) {
                Image(
                    b.asImageBitmap(),
                    contentDescription = stringResource(R.string.viewer_title),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // إيماءة واحدة: تكبير + سحب + نقر مزدوج تهيئة (28 سبتمبر 2026)
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 6f)
                                if (scale > 1f) {
                                    offX += pan.x
                                    offY += pan.y
                                } else {
                                    offX = 0f; offY = 0f
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                if (scale > 1f) { scale = 1f; offX = 0f; offY = 0f } else scale = 2.5f
                            })
                        }
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationX = offX; translationY = offY
                        }
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }
            // زر الإغلاق أعلى اليمين — مع وصف للقارئ الآلي
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .semantics { contentDescription = closeCd }
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}

// حوار القوالب المخصصة (28 سبتمبر 2026): يحفظ نص الملاحظة الحالي كقالب + يحذف القديم
// المعاينة محدودة بـ 4000 حرف: الحوار للاسم والحفظ، لا لقراءة النص كاملًا
@Composable
internal fun SaveTemplateDialog(
    preview: String,
    existing: List<Pair<String, String>>,
    onSave: (String) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tpl_custom_save)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text(stringResource(R.string.tpl_custom_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    stringResource(R.string.tpl_custom_preview, preview.length),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (existing.isNotEmpty()) {
                    Text(
                        stringResource(R.string.tpl_custom_saved),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    existing.forEachIndexed { i, t ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                t.first,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDelete(i) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.content_task_del),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim().ifBlank { preview.take(24) }) },
                enabled = preview.isNotBlank() && existing.size < 20
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
internal fun TableEditorDialog(
    onDismiss: () -> Unit,
    onInsert: (String) -> Unit
) {
    var grid by remember { mutableStateOf(listOf(listOf("", ""), listOf("", ""))) }
    val maxCells = 24 // سقف معقول: 6×4 كحد أقصى (يمنع جدولًا عملاقًا يعلق العرض)
    fun cells() = grid.size * (grid.firstOrNull()?.size ?: 2)
    fun addRow() {
        if (cells() >= maxCells) return
        val w = grid.firstOrNull()?.size ?: 2
        grid = grid + listOf(List(w) { "" })
    }
    fun addCol() {
        if (cells() >= maxCells) return
        grid = grid.map { it + "" }
    }
    fun set(r: Int, c: Int, v: String) {
        grid = grid.mapIndexed { ri, row ->
            if (ri != r) row else row.mapIndexed { ci, cell -> if (ci == c) v else cell }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.table_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                grid.forEachIndexed { r, row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { c, cell ->
                            OutlinedTextField(
                                value = cell,
                                onValueChange = { set(r, c, it.take(60)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { addRow() }, enabled = grid.size < 6) { Text(stringResource(R.string.table_add_row)) }
                    TextButton(onClick = { addCol() }, enabled = (grid.firstOrNull()?.size ?: 0) < 4) {
                        Text(stringResource(R.string.table_add_col))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // الترويسة الصف الأول، والفاصل بعدها — نفس صيغة Markdown التي يقرأها MarkdownPreview
                val head = grid.first().joinToString("|") { " ${it.trim()} " }.let { "|$it|" }
                val sep = grid.first().joinToString("|") { "---" }.let { "|$it|" }
                val rowsMd = grid.drop(1).joinToString("\n") { r -> r.joinToString("|") { " ${it.trim()} " }.let { "|${it}|" } }
                onInsert(listOf(head, sep, rowsMd).filter { it.isNotBlank() }.joinToString("\n"))
            }) { Text(stringResource(R.string.table_insert)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
internal fun LinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.link_dialog_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(stringResource(R.string.link_dialog_hint))
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text(stringResource(R.string.link_dialog_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url) }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}