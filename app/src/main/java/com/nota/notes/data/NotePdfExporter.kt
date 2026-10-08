package com.nota.notes.data

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.nota.notes.ui.MarkdownFormat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// تصدير الملاحظة PDF — أوفلاين تمامًا عبر PdfDocument المدمج (بلا مكتبات ولا شبكة).
// الدفعة د: يمشي كتل Markdown (عناوين/قوائم/مهام/اقتباس/كود/جداول) مع inline حقيقي (عريض/مائل/خط/كود).
// الخصوصية: الملف يُبنى في الكاش الخاص ثم يُشارك عبر FileProvider بصلاحية قراءة لمرة واحدة.
object NotePdfExporter {
    private const val PAGE_W = 595 // A4 بدقة 72
    private const val PAGE_H = 842
    private const val MARGIN = 40

    // يبني ملف PDF من العنوان والنص — يرجع الملف أو null (فارغة/فشل)
    suspend fun export(
        ctx: Context,
        appName: String,
        title: String,
        body: String,
        createdAt: Long,
        latitude: Double? = null, // الموقع المرفق — سطر إحداثيات أسفل الترويسة عند وجوده
        longitude: Double? = null,
        tasks: List<com.nota.notes.data.ChecklistItem> = emptyList() // المهام تُطبع أسفل المتن (☑/☐)
    ): File? {
        if (title.isBlank() && body.isBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val contentW = PAGE_W - MARGIN * 2
                // خطوط التطبيق (أميري للعربية + تاجوال للبيانات) — والافتراضي احتياط عند الفشل
                val arabic = runCatching { androidx.core.content.res.ResourcesCompat.getFont(ctx, com.nota.notes.R.font.amiri_regular) }.getOrNull()
                val arabicBold = runCatching { androidx.core.content.res.ResourcesCompat.getFont(ctx, com.nota.notes.R.font.amiri_bold) }.getOrNull()
                val defBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                fun paint(size: Float, bold: Boolean, italic: Boolean = false, gray: Boolean = false, mono: Boolean = false): TextPaint {
                    return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = size
                        if (mono) {
                            typeface = Typeface.MONOSPACE
                        } else if (bold) {
                            typeface = arabicBold ?: defBold
                        } else if (arabic != null) {
                            typeface = arabic
                        }
                        if (italic) typeface = Typeface.create(typeface, Typeface.ITALIC)
                        if (gray) color = 0xFF757575.toInt()
                    }
                }
                val titlePaint = paint(22f, true)
                val metaPaint = paint(12f, false, gray = true)
                val bodyPaint = paint(15f, false)
                val hPaints = mapOf(1 to paint(20f, true), 2 to paint(18f, true), 3 to paint(16f, true))
                val quotePaint = paint(15f, false, italic = true, gray = true)
                val codePaint = paint(13.5f, false, mono = true)
                val fmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                val layouts = mutableListOf<Layout>()
                val gaps = mutableListOf<Int>()
                fun add(text: CharSequence, p: TextPaint, gap: Int = 14) {
                    if (text.isBlank()) return
                    layouts.add(layoutOf(text, p, contentW))
                    gaps.add(gap)
                }
                add(title.ifBlank { appName }, titlePaint)
                add("$appName • ${fmt.format(Date(if (createdAt > 0) createdAt else System.currentTimeMillis()))}", metaPaint, 18)
                if (latitude != null && longitude != null)
                    add(String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude), metaPaint)
                // المهام المنجزة بعلامة والمفتوحة بمربع — بعد المتن مباشرة
                if (tasks.isNotEmpty()) add(tasks.joinToString("\n") { (if (it.done) "☑ " else "☐ ") + it.text }, bodyPaint)
                // متن Markdown كتلةً كتلة
                var olNum = 0
                for (b in MarkdownFormat.parseBlocks(body)) {
                    when (b) {
                        is MarkdownFormat.PreviewBlock.Heading -> {
                            olNum = 0
                            add(inlineSpanned(b.text), hPaints[b.level] ?: bodyPaint, 12)
                        }
                        is MarkdownFormat.PreviewBlock.Quote -> {
                            olNum = 0
                            add(inlineSpanned(b.text), quotePaint)
                        }
                        is MarkdownFormat.PreviewBlock.Divider -> {
                            olNum = 0
                            add("────────────────", metaPaint, 12)
                        }
                        is MarkdownFormat.PreviewBlock.Bullet -> {
                            olNum = 0
                            val pad = "  ".repeat((b.indent / 2).coerceAtMost(4))
                            add(plainPrefix(pad + "• ", inlineSpanned(b.text)), bodyPaint, 6)
                        }
                        is MarkdownFormat.PreviewBlock.Ordered -> {
                            olNum++
                            val pad = "  ".repeat((b.indent / 2).coerceAtMost(4))
                            add(plainPrefix("$pad$olNum. ", inlineSpanned(b.text)), bodyPaint, 6)
                        }
                        is MarkdownFormat.PreviewBlock.Task -> {
                            olNum = 0
                            val pad = "  ".repeat((b.indent / 2).coerceAtMost(4))
                            add(plainPrefix(pad + (if (b.checked) "☑ " else "☐ "), inlineSpanned(b.text)), bodyPaint, 6)
                        }
                        is MarkdownFormat.PreviewBlock.Code -> {
                            olNum = 0
                            // سطرًا سطرًا ليتكسر عبر الصفحات بدل القص (تدقيق الدفعة د)
                            b.text.split('\n').forEach { ln ->
                                add(if (ln.isBlank()) " " else ln, codePaint, 4)
                            }
                        }
                        is MarkdownFormat.PreviewBlock.Table -> {
                            olNum = 0
                            // صفًا صفًا لنفس السبب
                            b.rows.forEach { row ->
                                add(row.joinToString(" | ").ifBlank { " " }, codePaint, 4)
                            }
                        }
                        is MarkdownFormat.PreviewBlock.Para -> {
                            olNum = 0
                            add(inlineSpanned(b.text), bodyPaint)
                        }
                    }
                }
                val doc = PdfDocument()
                try {
                    var page = startPage(doc)
                    var y = MARGIN
                    layouts.forEachIndexed { i, lay ->
                        val h = lay.height
                        if (y + h > PAGE_H - MARGIN) {
                            doc.finishPage(page)
                            page = startPage(doc)
                            y = MARGIN
                        }
                        page.canvas.save()
                        page.canvas.translate(MARGIN.toFloat(), y.toFloat())
                        lay.draw(page.canvas)
                        page.canvas.restore()
                        y += h + gaps[i]
                    }
                    doc.finishPage(page)
                    val dir = File(ctx.cacheDir, "pdf").apply { if (!exists()) mkdirs() }
                    // تنظيف ملفات قديمة — الكاش لا يتضخم بملفات PDF منسية
                    dir.listFiles()?.forEach { runCatching { it.delete() } }
                    val safe = title.trim().take(24)
                        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        .ifBlank { appName }
                    val out = File(dir, "$safe-${System.currentTimeMillis()}.pdf")
                    out.outputStream().use { doc.writeTo(it) }
                    out
                } finally {
                    doc.close()
                }
            }.getOrNull()
        }
    }

    private fun startPage(doc: PdfDocument): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        return doc.startPage(info)
    }

    private fun layoutOf(text: CharSequence, paint: TextPaint, width: Int): Layout {
        return if (Build.VERSION.SDK_INT >= 23) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setLineSpacing(4f, 1f)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 4f, true)
        }
    }

    private fun plainPrefix(prefix: String, spanned: Spanned): Spanned {
        val s = SpannableString(prefix.toString() + spanned.toString())
        val off = prefix.length
        (spanned as? SpannableString)?.getSpans(0, spanned.length, Any::class.java)?.forEach { sp ->
            s.setSpan(sp, off + spanned.getSpanStart(sp), off + spanned.getSpanEnd(sp), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return s
    }

    /** inline (عريض/مائل/يتوسطه خط/كود/تمييز/روابط) → Spanned حقيقي للـ PDF. */
    /** inline (عريض/مائل/يتوسطه خط/كود/تمييز/روابط) → Spanned حقيقي للـ PDF. */
    private fun inlineSpanned(s: String): Spanned {
        val sb = StringBuilder()
        val spans = mutableListOf<Triple<Int, Int, Any>>()
        var i = 0
        val src = s
        while (i < src.length) {
            // كود
            if (src[i] == '`') {
                val c = src.indexOf('`', i + 1)
                if (c > i + 1) {
                    val st = sb.length
                    sb.append(src.substring(i + 1, c))
                    spans.add(Triple(st, sb.length, TypefaceSpan("monospace")))
                    i = c + 1
                    continue
                }
            }
            // عريض
            if (src.startsWith("**", i)) {
                val c = src.indexOf("**", i + 2)
                if (c > i + 2) {
                    val st = sb.length
                    sb.append(src.substring(i + 2, c))
                    spans.add(Triple(st, sb.length, StyleSpan(Typeface.BOLD)))
                    i = c + 2
                    continue
                }
            }
            // خط
            if (src.startsWith("~~", i)) {
                val c = src.indexOf("~~", i + 2)
                if (c > i + 2) {
                    val st = sb.length
                    sb.append(src.substring(i + 2, c))
                    spans.add(Triple(st, sb.length, StrikethroughSpan()))
                    i = c + 2
                    continue
                }
            }
            // تمييز
            if (src.startsWith("==", i)) {
                val c = src.indexOf("==", i + 2)
                if (c > i + 2) {
                    val st = sb.length
                    sb.append(src.substring(i + 2, c))
                    spans.add(Triple(st, sb.length, BackgroundColorSpan(0xFFFFEB3B.toInt())))
                    i = c + 2
                    continue
                }
            }
            // مائل مفرد بحراسة snake_case
            if (src[i] == '_' && (i == 0 || (!src[i - 1].isLetterOrDigit() && src[i - 1] != '_'))) {
                var c = i + 1
                var found = -1
                while (c < src.length) {
                    if (src[c] == '_' && !src[c - 1].isWhitespace() &&
                        (c + 1 >= src.length || (!src[c + 1].isLetterOrDigit() && src[c + 1] != '_'))
                    ) {
                        found = c
                        break
                    }
                    c++
                }
                if (found > i + 1) {
                    val st = sb.length
                    sb.append(src.substring(i + 1, found))
                    spans.add(Triple(st, sb.length, StyleSpan(Typeface.ITALIC)))
                    i = found + 1
                    continue
                }
            }
            // رابط [نص](url) → النص فقط
            if (src[i] == '[' && !src.startsWith("[[", i)) {
                val m = Regex("""\[([^\]]+)\]\(([^)]+)\)""").find(src, i)
                if (m != null && m.range.first == i) {
                    sb.append(m.groupValues[1])
                    i = m.range.last + 1
                    continue
                }
            }
            // رابط [[عنوان]] → العنوان عريضًا
            if (src.startsWith("[[", i)) {
                val c = src.indexOf("]]", i + 2)
                if (c > i + 2) {
                    val st = sb.length
                    sb.append(src.substring(i + 2, c))
                    spans.add(Triple(st, sb.length, StyleSpan(Typeface.BOLD)))
                    i = c + 2
                    continue
                }
            }
            sb.append(src[i])
            i++
        }
        val res = SpannableString(sb.toString())
        spans.forEach { (a, b, sp) -> res.setSpan(sp, a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        return res
    }
}
