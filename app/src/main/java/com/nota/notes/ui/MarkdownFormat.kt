package com.nota.notes.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.isUnspecified

/**
 * أدوات تنسيق Markdown داخل محرر الملاحظة — بلا مكتبات خارجية.
 * التخزين يبقى نصًا عاديًا (Markdown)؛ والعرض الحي يطبق الأنماط فقط.
 */
object MarkdownFormat {

    /**
     * يلف المحدد بـ [prefix] و [suffix].
     * التحديد الفارغ: يدخل العلامتين ويضع المؤشر بينهما.
     */
    fun wrapSelection(text: String, sel: TextRange, prefix: String, suffix: String = prefix): Pair<String, TextRange> {
        val start = sel.start.coerceIn(0, text.length)
        val end = sel.end.coerceIn(start, text.length)
        val selected = text.substring(start, end)

        val result = StringBuilder(text.length + prefix.length + suffix.length)
        result.append(text, 0, start)
        result.append(prefix)
        result.append(selected)
        result.append(suffix)
        result.append(text, end, text.length)

        val newStart = start + prefix.length
        val newEnd = if (selected.isEmpty()) newStart else newStart + selected.length
        return result.toString() to TextRange(newStart, newEnd)
    }

    /**
     * لفّ تبديلي: لو المحدد ملفوف أصلًا بنفس العلامات يفكها، وإلا يلفه.
     * يعمل فقط للعلامات المتماثلة (prefix == suffix)؛ غير ذلك يلف دائمًا.
     */
    fun toggleWrap(text: String, sel: TextRange, prefix: String, suffix: String = prefix): Pair<String, TextRange> {
        if (prefix != suffix) return wrapSelection(text, sel, prefix, suffix)
        val start = sel.start.coerceIn(0, text.length)
        val end = sel.end.coerceIn(start, text.length)

        val hasPrefix = start >= prefix.length && text.substring(start - prefix.length, start) == prefix
        val hasSuffix = end + suffix.length <= text.length && text.substring(end, end + suffix.length) == suffix

        if (hasPrefix && hasSuffix) {
            val result = StringBuilder(text.length - prefix.length - suffix.length)
            result.append(text, 0, start - prefix.length)
            result.append(text, start, end)
            result.append(text, end + suffix.length, text.length)

            val newStart = start - prefix.length
            val newEnd = end - prefix.length
            return result.toString() to TextRange(newStart, newEnd)
        }
        return wrapSelection(text, sel, prefix, suffix)
    }

    /** يبني رابط Markdown: [نص](رابط) حول المحدد. */
    fun wrapLink(text: String, sel: TextRange, url: String): Pair<String, TextRange> {
        val start = sel.start.coerceIn(0, text.length)
        val end = sel.end.coerceIn(start, text.length)
        val selected = text.substring(start, end)
        val clean = url.trim()
        if (clean.isEmpty()) return text to sel

        val result = StringBuilder(text.length + selected.length + clean.length + 4)
        result.append(text, 0, start)
        result.append('[')
        result.append(selected)
        result.append("](")
        result.append(clean)
        result.append(')')
        result.append(text, end, text.length)

        val newStart = start
        val newEnd = start + selected.length + clean.length + 4
        return result.toString() to TextRange(newStart, newEnd)
    }

    /**
     * يحوّل علامات السطر الواحد إلى أنماط حقيقية:
     * **عريض** و _مائل_ و ~~يتوسطه خط~~ و ==تمييز== و `كود`.
     * العلامات نفسها تُرسم بلون خافت. لا يكسر اتجاه الفقرات (معالجة خطية).
     */
    fun parseInlineSpans(
        text: String,
        base: SpanStyle,
        dimColor: Color
    ): AnnotatedString {
        val b = AnnotatedString.Builder()
        b.pushStyle(base)
        var i = 0
        while (i < text.length) {
            // عريض: **نص**
            if (text.startsWith("**", i)) {
                val close = text.indexOf("**", i + 2)
                if (close > i + 2) {
                    styled(b, text.substring(i, i + 2), SpanStyle(color = dimColor))
                    styled(b, text.substring(i + 2, close), base.copy(fontWeight = FontWeight.Bold))
                    styled(b, "**", SpanStyle(color = dimColor))
                    i = close + 2
                    continue
                }
            }
            // يتوسطه خط: ~~نص~~ (قبل المفرد حتى لا يلتبس)
            if (text.startsWith("~~", i)) {
                val close = text.indexOf("~~", i + 2)
                if (close > i + 2) {
                    styled(b, text.substring(i, i + 2), SpanStyle(color = dimColor))
                    styled(b, text.substring(i + 2, close), base.copy(textDecoration = TextDecoration.LineThrough))
                    styled(b, "~~", SpanStyle(color = dimColor))
                    i = close + 2
                    continue
                }
            }
            // تمييز: ==نص==
            if (text.startsWith("==", i)) {
                val close = text.indexOf("==", i + 2)
                if (close > i + 2) {
                    styled(b, text.substring(i, i + 2), SpanStyle(color = dimColor))
                    styled(
                        b, text.substring(i + 2, close),
                        base.copy(background = Color(0xFFFFEB3B).copy(alpha = 0.45f))
                    )
                    styled(b, "==", SpanStyle(color = dimColor))
                    i = close + 2
                    continue
                }
            }
            // مائل: _نص_ (مفرد، بشروط حدّية حتى لا يكسر snake_case)
            if (text[i] == '_' && !text.startsWith("__", i) && isOpenUnderscore(text, i)) {
                val close = findCloseUnderscore(text, i + 1)
                if (close > i + 1) {
                    styled(b, "_", SpanStyle(color = dimColor))
                    styled(b, text.substring(i + 1, close), base.copy(fontStyle = FontStyle.Italic))
                    styled(b, "_", SpanStyle(color = dimColor))
                    i = close + 1
                    continue
                }
            }
            // كود: `نص`
            if (text[i] == '`') {
                val close = text.indexOf('`', i + 1)
                if (close > i + 1) {
                    styled(b, "`", SpanStyle(color = dimColor))
                    styled(
                        b, text.substring(i + 1, close),
                        base.copy(
                            fontFamily = FontFamily.Monospace,
                            background = base.color.copy(alpha = 0.12f)
                        )
                    )
                    styled(b, "`", SpanStyle(color = dimColor))
                    i = close + 1
                    continue
                }
            }
            b.append(text[i].toString())
            i++
        }
        b.pop()
        return b.toAnnotatedString()
    }

    private fun styled(b: AnnotatedString.Builder, s: String, style: SpanStyle) {
        b.pushStyle(style)
        b.append(s)
        b.pop()
    }

    private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

    private fun isOpenUnderscore(text: String, i: Int): Boolean {
        if (i + 1 >= text.length) return false
        val prevOk = i == 0 || !isWordChar(text[i - 1])
        val nextOk = !text[i + 1].isWhitespace() && text[i + 1] != '_'
        return prevOk && nextOk
    }

    private fun findCloseUnderscore(text: String, from: Int): Int {
        var j = from
        while (j < text.length) {
            if (text[j] == '_' && !text[j - 1].isWhitespace()) {
                val afterOk = j + 1 >= text.length || !isWordChar(text[j + 1])
                if (afterOk) return j
            }
            j++
        }
        return -1
    }

    fun parseInlineMarkdown(
        text: String,
        base: SpanStyle,
        dimColor: Color
    ): AnnotatedString {
        val b = AnnotatedString.Builder()
        b.pushStyle(base)
        val lines = text.split('\n')
        lines.forEachIndexed { li, line ->
            appendBlockLine(b, line, base, dimColor)
            if (li != lines.lastIndex) b.append("\n")
        }
        b.pop()
        return b.toAnnotatedString()
    }

    /** سطر واحد بمستوى الكتلة: عنوان/اقتباس/فاصل/قائمة — ثم inline للمحتوى. */
    private fun appendBlockLine(b: AnnotatedString.Builder, line: String, base: SpanStyle, dim: Color) {
        // فاصل: --- أو *** أو ___ (تدقيق الدفعة ب)
        if (line.trim().matches(Regex("(\\*{3,}|-{3,}|_{3,})"))) {
            styled(b, line, base.copy(background = dim.copy(alpha = 0.25f)))
            return
        }
        // عنوان: #/##/###
        val h = Regex("^(#{1,3})\\s(.*)$").matchEntire(line)
        if (h != null) {
            val level = h.groupValues[1].length
            styled(b, h.groupValues[1] + " ", SpanStyle(color = dim))
            val scale = when (level) { 1 -> 1.35f; 2 -> 1.2f; else -> 1.1f }
            val fs = base.fontSize
            val hs = if (fs.isUnspecified) base.copy(fontWeight = FontWeight.Bold)
                else base.copy(fontWeight = FontWeight.Bold, fontSize = fs * scale)
            parseInlineSpansInto(b, h.groupValues[2], hs, dim)
            return
        }
        // اقتباس: >
        val q = Regex("^(>+)\\s?(.*)$").matchEntire(line)
        if (q != null) {
            styled(b, q.groupValues[1] + " ", SpanStyle(color = dim))
            parseInlineSpansInto(b, q.groupValues[2], base, dim)
            return
        }
        // علامة قائمة: - / 1. / - [ ]
        val m = Regex("^(\\s*((\\d+)\\. |[-*+] (\\[[ x]\\])? ?))").find(line)
        if (m != null && m.groupValues[1].isNotBlank()) {
            styled(b, m.groupValues[1], SpanStyle(color = dim))
            parseInlineSpansInto(b, line.substring(m.groupValues[1].length), base, dim)
            return
        }
        parseInlineSpansInto(b, line, base, dim)
    }

    /** محلل inline يعمل على مقطع (يُستخدم لمحتوى السطر بعد بادئة الكتلة). */
    private fun parseInlineSpansInto(b: AnnotatedString.Builder, s: String, base: SpanStyle, dimColor: Color) {
        val parsed = parseInlineSpans(s, base, dimColor)
        b.append(parsed)
    }
    fun stripMarkdown(text: String): String {
        var t = text
        t = t.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        t = t.replace(Regex("~~(.+?)~~"), "$1")
        t = t.replace(Regex("==(.+?)=="), "$1")
        t = t.replace(Regex("`(.+?)`"), "$1")
        t = t.replace(Regex("(?m)^#{1,3}\\s+"), "")
        t = t.replace(Regex("(?m)^>\\s?"), "")
        // الروابط ذات الأقواس الداخلية تُحتفظ بنصها فقط — الصيغة تدعم مستوى أقواس واحدًا داخل الرابط.
        t = t.replace(Regex("\\[([^\\]]+)]\\(((?:[^()\\s]|\\([^()\\s]*\\))*)\\)"), "$1")
        t = t.replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "")
        t = t.replace(Regex("(?m)^\\s*[-*+]\\s+(\\[[ x]\\]\\s+)?"), "")
        return t
    }

    // ===== الكتل الخاصة (27 سبتمبر 2026): اقتباس/فاصل/جدول/كود مسيّج لا يفهمها محلل مكتبة المحرر فيسقطها عند التحميل
    // (فحصنا الشفرة + سجل الإصدارات حتى v1.2.0 عبر DDG: لا دعم لها upstream) — الحل: تُستخرج لسطر بديل مرئي
    // قبل setMarkdown، وتُعاد حرفيًا بعد toMarkdown — فتُحفظ ولا تضيع أبدًا
    private val SPECIAL_RE = Regex("‹كتلة خاصة #(\\d+)›")
    private fun specialPh(i: Int) = "‹كتلة خاصة #$i›"

    /** يفصل الكتل الخاصة عن المتن الآمن للمحرر — يرجع (المتن الآمن، الكتل بالترتيب). */
    fun extractSpecial(md: String): Pair<String, List<String>> {
        val lines = md.split('\n')
        val safe = mutableListOf<String>()
        val specials = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val t = line.trim()
            // كود مسيّج: حتى الإغلاق أو نهاية النص
            if (t.startsWith("```")) {
                val buf = mutableListOf(line)
                i++
                while (i < lines.size) {
                    buf.add(lines[i])
                    val end = lines[i].trim().startsWith("```")
                    i++
                    if (end) break
                }
                specials.add(buf.joinToString("\n"))
                safe.add(specialPh(specials.size - 1))
                continue
            }
            // اقتباس: سطور متتالية تبدأ بـ >
            if (t.startsWith(">")) {
                val buf = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    buf.add(lines[i])
                    i++
                }
                specials.add(buf.joinToString("\n"))
                safe.add(specialPh(specials.size - 1))
                continue
            }
            // فاصل أفقي
            if (t.matches(Regex("(\\*{3,}|-{3,}|_{3,})"))) {
                specials.add(line)
                safe.add(specialPh(specials.size - 1))
                i++
                continue
            }
            // جدول: سطور متتالية |...| حرفيًا (بما فيها سطر الفواصل)
            if (t.startsWith("|") && t.endsWith("|")) {
                val buf = mutableListOf<String>()
                while (i < lines.size) {
                    val u = lines[i].trim()
                    if (!(u.startsWith("|") && u.endsWith("|"))) break
                    buf.add(lines[i])
                    i++
                }
                specials.add(buf.joinToString("\n"))
                safe.add(specialPh(specials.size - 1))
                continue
            }
            safe.add(line)
            i++
        }
        return safe.joinToString("\n") to specials
    }

    /** يعيد الكتل الخاصة لمواضعها — السطر المحذوف من المستخدم يعني حذف كتلته صراحةً. */
    fun restoreSpecial(safe: String, specials: List<String>): String {
        if (specials.isEmpty()) return safe
        return SPECIAL_RE.replace(safe) { m ->
            val idx = m.groupValues[1].toIntOrNull()
            if (idx != null && idx in specials.indices) specials[idx] else m.value
        }
    }

    // ===== الدفعة ب: عناوين + طي + قوائم ذكية =====

    data class OutlineHeading(val lineIdx: Int, val level: Int, val text: String, val lineStart: Int)

    /** كل عناوين H1-H3 مع مواضعها. */
    fun extractHeadings(text: String): List<OutlineHeading> {
        val out = mutableListOf<OutlineHeading>()
        var off = 0
        text.split('\n').forEachIndexed { li, line ->
            val m = Regex("^(#{1,3})\\s+(.*)$").matchEntire(line)
            if (m != null) out.add(OutlineHeading(li, m.groupValues[1].length, m.groupValues[2].trim(), off))
            off += line.length + 1
        }
        return out
    }

    data class FoldSection(val lineIdx: Int, val level: Int, val start: Int, val end: Int)

    /** مقاطع الطي: من نهاية سطر العنوان حتى عنوان مماثل/أعلى أو نهاية النص. */
    fun computeFoldSections(text: String): List<FoldSection> {
        val lines = text.split('\n')
        val heads = extractHeadings(text)
        if (heads.isEmpty()) return emptyList()
        val starts = mutableListOf<Int>()
        var off = 0
        lines.forEach { l -> starts.add(off); off += l.length + 1 }
        return heads.mapIndexed { hi, h ->
            val bodyStart = starts[h.lineIdx] + lines[h.lineIdx].length + 1
            var end = text.length
            for (j in hi + 1 until heads.size) {
                if (heads[j].level <= h.level) {
                    end = starts[heads[j].lineIdx]
                    break
                }
            }
            FoldSection(h.lineIdx, h.level, bodyStart.coerceAtMost(text.length), end.coerceIn(0, text.length))
        }.filter { it.end > it.start }
    }

    data class FoldSpan(val origStart: Int, val origEnd: Int, val transStart: Int, val transEnd: Int)
    data class FoldedText(val text: String, val spans: List<FoldSpan>)

    /** يطوي المقاطع المحددة: العنوان يبقى + علامة … بدل الجسم. */
    fun applyFolds(text: String, folded: Set<Int>): FoldedText {
        if (folded.isEmpty()) return FoldedText(text, emptyList())
        val secs = computeFoldSections(text).filter { it.lineIdx in folded }
        if (secs.isEmpty()) return FoldedText(text, emptyList())
        val sb = StringBuilder()
        val spans = mutableListOf<FoldSpan>()
        var cur = 0
        for (s in secs.sortedBy { it.start }) {
            if (s.start < cur) continue
            sb.append(text, cur, s.start)
            val ts = sb.length
            sb.append(" …")
            spans.add(FoldSpan(s.start, s.end, ts, sb.length))
            cur = s.end
        }
        sb.append(text, cur, text.length)
        return FoldedText(sb.toString(), spans)
    }

    /** تخطيط إزاحات للطي: المؤشر داخل المطوي يقفز لحافته. */
    class FoldOffsetMapping(private val origLen: Int, private val transLen: Int, private val spans: List<FoldSpan>) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            val o = offset.coerceIn(0, origLen)
            var shift = 0
            for (s in spans) {
                if (o < s.origStart) return (o + shift).coerceIn(0, transLen)
                if (o < s.origEnd) return s.transEnd
                shift = s.transEnd - s.origEnd
            }
            return (o + shift).coerceIn(0, transLen)
        }
        override fun transformedToOriginal(offset: Int): Int {
            val t = offset.coerceIn(0, transLen)
            var shift = 0
            for (s in spans) {
                if (t < s.transStart) return (t - shift).coerceIn(0, origLen)
                if (t < s.transEnd) return s.origStart
                shift = s.transEnd - s.origEnd
            }
            return (t - shift).coerceIn(0, origLen)
        }
    }

    /** لفّ/فكّ بادئة سطر (عنوان/اقتباس/قائمة). ordered=true يعني قائمة مرقمة بأي رقم. */
    fun toggleLinePrefix(text: String, sel: TextRange, prefix: String, ordered: Boolean = false): Pair<String, TextRange> {
        val caret = sel.start.coerceIn(0, text.length)
        val ls = text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        var le = text.indexOf('\n', caret)
        if (le < 0) le = text.length
        val line = text.substring(ls, le)
        val stripped: String?
        val markerLen: Int
        if (ordered) {
            val m = Regex("^\\s*\\d+\\.\\s+").find(line)
            if (m != null) {
                stripped = line.substring(m.value.length); markerLen = m.value.length
            } else {
                stripped = null; markerLen = 0
            }
        } else {
            if (line.startsWith(prefix)) {
                stripped = line.substring(prefix.length); markerLen = prefix.length
            } else {
                // عنوان بمستوى آخر → استبداله
                val hm = Regex("^#{1,3} ").find(line)
                if (hm != null && prefix.trimStart().startsWith("#")) {
                    val rest = line.substring(hm.value.length)
                    val nt = text.substring(0, ls) + prefix + rest + text.substring(le)
                    val ns = (caret - hm.value.length + prefix.length).coerceIn(ls, ls + prefix.length + rest.length)
                    return nt to TextRange(ns, ns)
                }
                stripped = null; markerLen = 0
            }
        }
        return if (stripped != null) {
            val nt = text.substring(0, ls) + stripped + text.substring(le)
            val ns = (caret - markerLen).coerceAtLeast(ls)
            nt to TextRange(ns, ns)
        } else {
            // للمرقمة: أدخل الرقم الصحيح حسب التسلسل
            var ins = prefix
            if (ordered) {
                ins = nextOrderedMarker(text, ls) + " "
            }
            val nt = text.substring(0, ls) + ins + text.substring(ls)
            val ns = (caret + ins.length).coerceAtMost(nt.length)
            nt to TextRange(ns, ns)
        }
    }

    private fun nextOrderedMarker(text: String, lineStart: Int): String {
        if (lineStart <= 0) return "1."
        var pe = lineStart - 1
        if (pe >= 0 && text[pe] == '\n') pe--
        if (pe < 0) return "1."
        val ps = text.lastIndexOf('\n', pe).let { if (it < 0) 0 else it + 1 }
        val m = Regex("^\\s*(\\d+)\\.\\s+").find(text.substring(ps, pe + 1))
        return "${(m?.groupValues?.get(1)?.toIntOrNull() ?: 0) + 1}."
    }

    /** إدخال فاصل ---: سطر فارغ يُستبدل، وإلا يُدرج حول المؤشر. */
    fun insertDivider(text: String, sel: TextRange): Pair<String, TextRange> {
        val caret = sel.start.coerceIn(0, text.length)
        val ls = text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        var le = text.indexOf('\n', caret)
        if (le < 0) le = text.length
        val line = text.substring(ls, le)
        return if (line.isBlank()) {
            val nt = text.substring(0, ls) + "---" + text.substring(le)
            val ns = (ls + 3).coerceAtMost(nt.length)
            nt to TextRange(ns, ns)
        } else {
            val ins = "\n---\n"
            val nt = text.substring(0, caret) + ins + text.substring(caret)
            val ns = caret + ins.length
            nt to TextRange(ns, ns)
        }
    }

    /** إعادة ترقيم القوائم المرقمة المتتالية 1..n — نص مطابق إن لم يتغير شيء. */
    private val ORDERED_RE = Regex("(?m)^\\s*\\d+\\.\\s+")

    fun renumberLists(text: String): String {
        if (!ORDERED_RE.containsMatchIn(text)) return text
        val lines = text.split('\n')
        var changed = false
        var expect = 0
        val out = lines.map { line ->
            val m = Regex("^(\\s*)(\\d+)(\\.\\s+.*)$").matchEntire(line)
            if (m != null) {
                expect++
                val want = m.groupValues[1] + expect + m.groupValues[3]
                if (want != line) {
                    changed = true
                    want
                } else line
            } else {
                if (line.isNotBlank()) expect = 0
                line
            }
        }
        return if (changed) out.joinToString("\n") else text
    }

    /** إعادة ترقيم مع ضبط المؤشر: الترقيم لا يضيف أسطرًا فيقابل كل سطر نظيره —
     *  يُجمع فرق الأطوال للسطور الكاملة قبل المؤشر (تدقيق الدفعة ج). */
    fun renumberWithSelection(text: String, sel: TextRange): Pair<String, TextRange> {
        val ren = renumberLists(text)
        if (ren == text) return text to sel
        val a = text.split('\n')
        val b = ren.split('\n')
        if (a.size != b.size) {
            val s = sel.start.coerceIn(0, ren.length)
            val e = sel.end.coerceIn(0, ren.length)
            return ren to TextRange(s, e)
        }
        fun shiftFor(pos: Int): Int {
            var shift = 0
            var off = 0
            for (i in a.indices) {
                if (off >= pos) break
                if (off + a[i].length < pos) shift += b[i].length - a[i].length
                off += a[i].length + 1
            }
            return shift
        }
        val ns = (sel.start + shiftFor(sel.start)).coerceIn(0, ren.length)
        val ne = (sel.end + shiftFor(sel.end)).coerceIn(ns, ren.length)
        return ren to TextRange(ns, ne)
    }

    /** Enter ذكية: تكمل عنصر القائمة/الاقتباس، والعنصر الفارغ يخرج منها. null = سلوك عادي. */
    fun smartEnter(oldText: String, oldSel: TextRange, newText: String, newSel: TextRange): TextFieldValue? {
        if (!newSel.collapsed || !oldSel.collapsed) return null
        if (newSel.start != oldSel.start + 1) return null
        if (newSel.start <= 0 || newText[newSel.start - 1] != '\n') return null
        if (newText.length != oldText.length + 1) return null
        val insAt = newSel.start - 1
        val prevEnd = insAt - 1
        val ls = if (prevEnd < 0) 0 else oldText.lastIndexOf('\n', prevEnd).let { if (it < 0) 0 else it + 1 }
        val prevLine = oldText.substring(ls, (prevEnd + 1).coerceAtMost(oldText.length))
        // مهمة [ ]/[x]
        var m = Regex("^(\\s*[-*+] \\[[ x]\\] )(.*)$").matchEntire(prevLine)
        if (m != null) {
            val marker = m.groupValues[1]
            return if (m.groupValues[2].isBlank()) {
                val nt = oldText.substring(0, ls) + "\n" + newText.substring(insAt + 1)
                TextFieldValue(nt, TextRange(ls + 1))
            } else {
                val indent = Regex("^\\s*").find(prevLine)?.value ?: ""
                val bullet = Regex("^\\s*([-*+])").find(prevLine)?.groupValues?.get(1) ?: "-"
                val nt = newText.substring(0, insAt + 1) + indent + bullet + " [ ] " + newText.substring(insAt + 1)
                TextFieldValue(nt, TextRange(insAt + 1 + indent.length + bullet.length + 5))
            }
        }
        // مرقمة
        m = Regex("^(\\s*\\d+\\. )(.*)$").matchEntire(prevLine)
        if (m != null) {
            return if (m.groupValues[2].isBlank()) {
                val nt = oldText.substring(0, ls) + "\n" + newText.substring(insAt + 1)
                TextFieldValue(renumberLists(nt), TextRange(ls + 1))
            } else {
                val indent = Regex("^\\s*").find(prevLine)?.value ?: ""
                val num = Regex("\\d+").find(prevLine)?.value?.toIntOrNull() ?: 0
                val nm = "$indent${num + 1}. "
                val nt = newText.substring(0, insAt + 1) + nm + newText.substring(insAt + 1)
                TextFieldValue(renumberLists(nt), TextRange(insAt + 1 + nm.length))
            }
        }
        // نقطية
        m = Regex("^(\\s*[-*+] )(.*)$").matchEntire(prevLine)
        if (m != null) {
            return if (m.groupValues[2].isBlank()) {
                val nt = oldText.substring(0, ls) + "\n" + newText.substring(insAt + 1)
                TextFieldValue(nt, TextRange(ls + 1))
            } else {
                val marker = m.groupValues[1]
                val nt = newText.substring(0, insAt + 1) + marker + newText.substring(insAt + 1)
                TextFieldValue(nt, TextRange(insAt + 1 + marker.length))
            }
        }
        // اقتباس
        m = Regex("^((>+)\\s?)(.*)$").matchEntire(prevLine)
        if (m != null) {
            return if (m.groupValues[3].isBlank()) {
                val nt = oldText.substring(0, ls) + "\n" + newText.substring(insAt + 1)
                TextFieldValue(nt, TextRange(ls + 1))
            } else {
                val marker = m.groupValues[1]
                val nt = newText.substring(0, insAt + 1) + marker + newText.substring(insAt + 1)
                TextFieldValue(nt, TextRange(insAt + 1 + marker.length))
            }
        }
        return null
    }

    // ===== الدفعة ج: أوامر / + #وسم + [[رابط]] + لصق الروابط =====

    data class NoteLink(val id: Long, val title: String)

    /** مشغّل / — الشرطة مسبوقة ببداية/مسافة، وبعدها بلا مسافات، وليست // */
    fun slashTrigger(linePrefix: String): Pair<Int, String>? {
        val i = linePrefix.lastIndexOf('/')
        if (i < 0) return null
        if (i > 0 && !linePrefix[i - 1].isWhitespace()) return null
        if (i + 1 < linePrefix.length && linePrefix[i + 1] == '/') return null
        val q = linePrefix.substring(i + 1)
        if (q.any { it.isWhitespace() }) return null
        return i to q
    }

    /** مشغّل #وسم — مسبوق ببداية/مسافة وبلا مسافات أو أقواس بعده. */
    fun tagTrigger(linePrefix: String): Pair<Int, String>? {
        val i = linePrefix.lastIndexOf('#')
        if (i < 0) return null
        if (i > 0 && !linePrefix[i - 1].isWhitespace()) return null
        val q = linePrefix.substring(i + 1)
        if (q.any { it.isWhitespace() || it == '#' || it == '[' || it == ']' }) return null
        return i to q
    }

    /** مشغّل [[رابط ملاحظة — بلا إغلاق بعد. */
    fun noteLinkTrigger(linePrefix: String): Pair<Int, String>? {
        val i = linePrefix.lastIndexOf("[[")
        if (i < 0) return null
        val q = linePrefix.substring(i + 2)
        if (q.contains("]]") || q.any { it == '[' }) return null
        return i to q
    }

    /** استبدال [from, to) بإدخال — والمؤشر بعده. */
    fun replaceRange(text: String, from: Int, to: Int, insert: String): Pair<String, TextRange> {
        val f = from.coerceIn(0, text.length)
        val t = to.coerceIn(f, text.length)
        val nt = text.substring(0, f) + insert + text.substring(t)
        val ns = (f + insert.length).coerceAtMost(nt.length)
        return nt to TextRange(ns, ns)
    }

    private val URL_RE = Regex("^https?://\\S+$")

    /** لصق رابط فوق تحديد → [نص](رابط). null = لصق عادي. */
    fun pasteUrl(oldText: String, oldSel: TextRange, newText: String): TextFieldValue? {
        if (oldSel.collapsed) return null
        if (newText.length <= oldText.length) return null
        var pre = 0
        while (pre < oldText.length && pre < newText.length && oldText[pre] == newText[pre]) pre++
        var suf = 0
        while (suf < oldText.length - pre && suf < newText.length - pre &&
            oldText[oldText.length - 1 - suf] == newText[newText.length - 1 - suf]
        ) suf++
        if (pre > oldSel.start || oldText.length - suf < oldSel.end) return null
        val inserted = newText.substring(pre, newText.length - suf)
        if (!URL_RE.matches(inserted)) return null
        val (nt, ns) = wrapLink(oldText, oldSel, inserted)
        return TextFieldValue(nt, ns)
    }

    // ===== الدفعة د: نموذج كتل المعاينة + HTML =====

    sealed interface PreviewBlock {
        data class Heading(val level: Int, val text: String) : PreviewBlock
        data class Quote(val text: String) : PreviewBlock
        data object Divider : PreviewBlock
        data class Bullet(val indent: Int, val text: String) : PreviewBlock
        data class Ordered(val indent: Int, val num: Int, val text: String) : PreviewBlock
        data class Task(val indent: Int, val checked: Boolean, val text: String, val lineStart: Int) : PreviewBlock
        data class Code(val text: String) : PreviewBlock
        data class Table(val rows: List<List<String>>) : PreviewBlock
        data class Para(val text: String) : PreviewBlock
    }

    /** يقطع النص لكتل عرض: عناوين/اقتباس/فاصل/قوائم/مهام/كود/جداول/فقرات. */
    fun parseBlocks(text: String): List<PreviewBlock> {
        val lines = text.split('\n')
        val out = mutableListOf<PreviewBlock>()
        var off = 0
        val lineStarts = mutableListOf<Int>()
        lines.forEach { l -> lineStarts.add(off); off += l.length + 1 }
        var i = 0
        var inCode = false
        val codeBuf = StringBuilder()
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (inCode) {
                    out.add(PreviewBlock.Code(codeBuf.toString().trimEnd('\n')))
                    codeBuf.clear()
                    inCode = false
                } else {
                    inCode = true
                }
                i++
                continue
            }
            if (inCode) {
                codeBuf.append(line).append('\n')
                i++
                continue
            }
            if (trimmed.isEmpty()) {
                i++
                continue
            }
            // جدول: سطور متتالية تبدأ بـ |
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                val rows = mutableListOf<List<String>>()
                while (i < lines.size) {
                    val t = lines[i].trim()
                    if (!(t.startsWith("|") && t.endsWith("|"))) break
                    if (!t.replace("|", "").replace("-", "").replace(":", "").replace(" ", "").isEmpty()) {
                        rows.add(t.removePrefix("|").removeSuffix("|").split("|").map { it.trim() })
                    }
                    i++
                }
                if (rows.isNotEmpty()) out.add(PreviewBlock.Table(rows))
                continue
            }
            val h = Regex("^(#{1,3})\\s+(.*)$").matchEntire(line)
            if (h != null) {
                out.add(PreviewBlock.Heading(h.groupValues[1].length, h.groupValues[2].trim()))
                i++
                continue
            }
            if (trimmed.matches(Regex("(\\*{3,}|-{3,}|_{3,})"))) {
                out.add(PreviewBlock.Divider)
                i++
                continue
            }
            val q = Regex("^(>+)\\s?(.*)$").matchEntire(line)
            if (q != null) {
                out.add(PreviewBlock.Quote(q.groupValues[2]))
                i++
                continue
            }
            val task = Regex("^(\\s*)[-*+] \\[[ x]\\] (.*)$").matchEntire(line)
            if (task != null) {
                val checked = line.contains("[x]")
                out.add(PreviewBlock.Task(task.groupValues[1].length, checked, task.groupValues[2], lineStarts[i]))
                i++
                continue
            }
            val ol = Regex("^(\\s*)(\\d+)\\. (.*)$").matchEntire(line)
            if (ol != null) {
                out.add(PreviewBlock.Ordered(ol.groupValues[1].length, ol.groupValues[2].toIntOrNull() ?: 0, ol.groupValues[3]))
                i++
                continue
            }
            val bl = Regex("^(\\s*)[-*+] (.*)$").matchEntire(line)
            if (bl != null) {
                out.add(PreviewBlock.Bullet(bl.groupValues[1].length, bl.groupValues[2]))
                i++
                continue
            }
            // فقرة: ادمج السطور المتتالية العادية
            val para = StringBuilder(line.trim())
            i++
            while (i < lines.size) {
                val nx = lines[i]
                if (nx.isBlank() || isBlockStart(nx)) break
                para.append(' ').append(nx.trim())
                i++
            }
            out.add(PreviewBlock.Para(para.toString()))
        }
        if (inCode && codeBuf.isNotEmpty()) out.add(PreviewBlock.Code(codeBuf.toString().trimEnd('\n')))
        return out
    }

    private fun isBlockStart(line: String): Boolean {
        val t = line.trim()
        if (t.isEmpty()) return true
        if (t.startsWith("```") || t.matches(Regex("(\\*{3,}|-{3,}|_{3,})"))) return true
        if (Regex("^(#{1,3})\\s+").containsMatchIn(line)) return true
        if (t.startsWith(">")) return true
        if (t.startsWith("|") && t.endsWith("|")) return true
        if (Regex("^\\s*((\\d+)\\. |[-*+] )").containsMatchIn(line)) return true
        return false
    }

    /** قلب مهمة نصية [ ]/[x] عند بداية سطرها — null إن لم يكن سطر مهمة. */
    fun flipTaskLine(text: String, lineStart: Int): String? {
        var le = text.indexOf('\n', lineStart)
        if (le < 0) le = text.length
        val line = text.substring(lineStart.coerceIn(0, text.length), le)
        val m = Regex("^(\\s*[-*+] )\\[[ x]\\](.*)$").matchEntire(line) ?: return null
        val flipped = if (line.contains("[x]")) {
            m.groupValues[1] + "[ ]" + m.groupValues[2]
        } else {
            m.groupValues[1] + "[x]" + m.groupValues[2]
        }
        return text.substring(0, lineStart) + flipped + text.substring(le)
    }

    private fun escHtml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /** inline (عريض/مائل/خط/كود/تمييز/روابط) → HTML. */
    fun inlineHtml(s: String): String {
        var t = escHtml(s)
        t = t.replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
        t = t.replace(Regex("~~(.+?)~~"), "<s>$1</s>")
        t = t.replace(Regex("`(.+?)`"), "<code>$1</code>")
        t = t.replace(Regex("==(.+?)=="), "<mark>$1</mark>")
        // مائل مفرد بعد العريض — وخارج <b>/<code> فقط ليوافق المعاينة (تدقيق الدفعة د)
        val italicRe = Regex("(?<!\\w)_(?!_)(.+?)(?<!_)_(?!\\w)")
        t = Regex("(<b>.*?</b>|<code>.*?</code>)").split(t).mapIndexed { i, part ->
            if (i % 2 == 1) part else part.replace(italicRe, "<i>$1</i>")
        }.joinToString("")
        t = t.replace(Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)"), "<a href=\"$2\">$1</a>")
        t = t.replace(Regex("\\[\\[([^\\]]+)\\]\\]"), "<b>$1</b>")
        return t
    }

    /** مستند HTML كامل (RTL تلقائي لكل فقرة) للمشاركة. */
    fun markdownToHtml(title: String, body: String): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        sb.append("<style>body{font-family:sans-serif;line-height:1.7;padding:16px;max-width:800px;margin:auto}")
        sb.append("pre{background:#f0f0f0;padding:12px;border-radius:8px;overflow-x:auto}")
        sb.append("code{font-family:monospace}blockquote{border-inline-start:3px solid #888;margin:8px 0;padding:4px 12px;color:#555}")
        sb.append("table{border-collapse:collapse;margin:8px 0}td,th{border:1px solid #aaa;padding:4px 10px}")
        sb.append("mark{background:#ffeb3b}</style></head><body dir=\"auto\">")
        if (title.isNotBlank()) sb.append("<h1 dir=\"auto\">").append(inlineHtml(title)).append("</h1>")
        var inUl = false
        var inOl = false
        fun closeLists() {
            if (inUl) { sb.append("</ul>"); inUl = false }
            if (inOl) { sb.append("</ol>"); inOl = false }
        }
        for (b in parseBlocks(body)) {
            when (b) {
                is PreviewBlock.Heading -> {
                    closeLists()
                    sb.append("<h").append(b.level + 1).append(" dir=\"auto\">").append(inlineHtml(b.text)).append("</h").append(b.level + 1).append(">")
                }
                is PreviewBlock.Quote -> {
                    closeLists()
                    sb.append("<blockquote dir=\"auto\">").append(inlineHtml(b.text)).append("</blockquote>")
                }
                is PreviewBlock.Divider -> {
                    closeLists()
                    sb.append("<hr>")
                }
                is PreviewBlock.Bullet -> {
                    if (inOl) { sb.append("</ol>"); inOl = false }
                    if (!inUl) { sb.append("<ul>"); inUl = true }
                    sb.append("<li dir=\"auto\">").append(inlineHtml(b.text)).append("</li>")
                }
                is PreviewBlock.Ordered -> {
                    if (inUl) { sb.append("</ul>"); inUl = false }
                    if (!inOl) { sb.append("<ol>"); inOl = true }
                    sb.append("<li dir=\"auto\">").append(inlineHtml(b.text)).append("</li>")
                }
                is PreviewBlock.Task -> {
                    closeLists()
                    val box = if (b.checked) "☑" else "☐"
                    sb.append("<p dir=\"auto\">").append(box).append(' ').append(inlineHtml(b.text)).append("</p>")
                }
                is PreviewBlock.Code -> {
                    closeLists()
                    sb.append("<pre><code>").append(escHtml(b.text)).append("</code></pre>")
                }
                is PreviewBlock.Table -> {
                    closeLists()
                    sb.append("<table>")
                    b.rows.forEachIndexed { ri, row ->
                        sb.append("<tr>")
                        row.forEach { c ->
                            sb.append(if (ri == 0) "<th>" else "<td>").append(inlineHtml(c))
                                .append(if (ri == 0) "</th>" else "</td>")
                        }
                        sb.append("</tr>")
                    }
                    sb.append("</table>")
                }
                is PreviewBlock.Para -> {
                    closeLists()
                    sb.append("<p dir=\"auto\">").append(inlineHtml(b.text)).append("</p>")
                }
            }
        }
        closeLists()
        sb.append("</body></html>")
        return sb.toString()
    }

    // ===== عرض فوري (WYSIWYG): إخفاء علامات التنسيق في العرض فقط =====
    // التخزين يبقى Markdown نصًا عاديًا؛ العرض يسقط العلامات المغلقة ويُبقي الأنماط.
    // كل محرف مطوي يُستهلك مرة واحدة (إظهار أو تخطٍّ) فيتطابق العرض مع الجدولين دائمًا.
    // العلامات اليتيمة (بلا إغلاق) تبقى ظاهرة عمدًا حتى يمكن تحريرها.

    data class WysiwygSpan(val start: Int, val end: Int, val style: SpanStyle)

    class WysiwygResult(
        val text: String,
        val spans: List<WysiwygSpan>,
        val srcLen: Int,
        val toFolded: IntArray,
        val toDisplay: IntArray
    )

    fun renderWysiwyg(folded: String, base: SpanStyle, dim: Color): WysiwygResult {
        val out = StringBuilder()
        val spans = mutableListOf<WysiwygSpan>()
        val toFolded = ArrayList<Int>()
        val toDisplay = IntArray(folded.length + 1)
        var d = 0
        fun skip(o: Int) { toDisplay[o] = d }
        fun emit(o: Int, c: Char) { toDisplay[o] = d; toFolded.add(o); out.append(c); d++ }

        // محلل inline مطابِق لشروط parseInlineSpans — لكنه يسقط العلامات
        // المغلقة (حتى الفارغة) ويُظهر اليتيمة كما هي.
        fun inline(s: String, abs: Int, st: SpanStyle) {
            var i = 0
            while (i < s.length) {
                val o = abs + i
                // عريض: **نص**
                if (s.startsWith("**", i)) {
                    val close = s.indexOf("**", i + 2)
                    if (close >= i + 2) {
                        skip(o); skip(o + 1)
                        val s0 = d
                        for (k in i + 2 until close) emit(abs + k, s[k])
                        if (d > s0) spans.add(WysiwygSpan(s0, d, st.copy(fontWeight = FontWeight.Bold)))
                        skip(abs + close); skip(abs + close + 1)
                        i = close + 2
                        continue
                    }
                }
                // يتوسطه خط: ~~نص~~
                if (s.startsWith("~~", i)) {
                    val close = s.indexOf("~~", i + 2)
                    if (close >= i + 2) {
                        skip(o); skip(o + 1)
                        val s0 = d
                        for (k in i + 2 until close) emit(abs + k, s[k])
                        if (d > s0) spans.add(WysiwygSpan(s0, d, st.copy(textDecoration = TextDecoration.LineThrough)))
                        skip(abs + close); skip(abs + close + 1)
                        i = close + 2
                        continue
                    }
                }
                // تمييز: ==نص==
                if (s.startsWith("==", i)) {
                    val close = s.indexOf("==", i + 2)
                    if (close >= i + 2) {
                        skip(o); skip(o + 1)
                        val s0 = d
                        for (k in i + 2 until close) emit(abs + k, s[k])
                        if (d > s0) spans.add(
                            WysiwygSpan(s0, d, st.copy(background = Color(0xFFFFEB3B).copy(alpha = 0.45f)))
                        )
                        skip(abs + close); skip(abs + close + 1)
                        i = close + 2
                        continue
                    }
                }
                // مائل: _نص_ (نفس شروط الحدّية حتى لا يكسر snake_case)
                if (s[i] == '_' && !s.startsWith("__", i) && isOpenUnderscore(s, i)) {
                    val close = findCloseUnderscore(s, i + 1)
                    if (close >= i + 1) {
                        skip(o)
                        val s0 = d
                        for (k in i + 1 until close) emit(abs + k, s[k])
                        if (d > s0) spans.add(WysiwygSpan(s0, d, st.copy(fontStyle = FontStyle.Italic)))
                        skip(abs + close)
                        i = close + 1
                        continue
                    }
                }
                // كود: `نص`
                if (s[i] == '`') {
                    val close = s.indexOf('`', i + 1)
                    if (close >= i + 1) {
                        skip(o)
                        val s0 = d
                        for (k in i + 1 until close) emit(abs + k, s[k])
                        if (d > s0) spans.add(
                            WysiwygSpan(
                                s0, d, st.copy(
                                    fontFamily = FontFamily.Monospace,
                                    background = st.color.copy(alpha = 0.12f)
                                )
                            )
                        )
                        skip(abs + close)
                        i = close + 1
                        continue
                    }
                }
                // رابط: [نص](رابط) — يُعرض النص بلون الرابط وتُخفى الأقواس
                if (s[i] == '[') {
                    val rb = s.indexOf(']', i + 1)
                    if (rb > i + 1 && rb + 1 < s.length && s[rb + 1] == '(') {
                        val rp = s.indexOf(')', rb + 2)
                        if (rp > rb + 2) {
                            skip(o)
                            val s0 = d
                            for (k in i + 1 until rb) emit(abs + k, s[k])
                            if (d > s0) spans.add(
                                WysiwygSpan(
                                    s0, d, st.copy(
                                        color = Color(0xFF42A5F5),
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            )
                            for (k in rb..rp) skip(abs + k)
                            i = rp + 1
                            continue
                        }
                    }
                }
                emit(o, s[i])
                i++
            }
        }

        // سطر واحد بمستوى الكتلة — نفس بنية appendBlockLine:
        // بادئتا العنوان والاقتباس تُخفَيان، وعلامة القائمة تبقى خافتة.
        fun blockLine(line: String, lineStart: Int) {
            // فاصل: --- أو *** أو ___
            if (line.trim().matches(Regex("(\\*{3,}|-{3,}|_{3,})"))) {
                val s0 = d
                for (k in line.indices) emit(lineStart + k, line[k])
                spans.add(WysiwygSpan(s0, d, base.copy(background = dim.copy(alpha = 0.25f))))
                return
            }
            // عنوان: #/##/### — البادئة تُخفى والمحتوى مكبَّر عريض
            val h = Regex("^(#{1,3})\\s(.*)$").matchEntire(line)
            if (h != null) {
                val prefixLen = h.groupValues[1].length + 1
                for (k in 0 until prefixLen) skip(lineStart + k)
                val scale = when (h.groupValues[1].length) { 1 -> 1.35f; 2 -> 1.2f; else -> 1.1f }
                val fs = base.fontSize
                val hs = if (fs.isUnspecified) base.copy(fontWeight = FontWeight.Bold)
                    else base.copy(fontWeight = FontWeight.Bold, fontSize = fs * scale)
                inline(h.groupValues[2], lineStart + prefixLen, hs)
                return
            }
            // اقتباس: > — العلامة ومسافة واحدة بعدها تُخفَيان
            val q = Regex("^(>+)\\s?(.*)$").matchEntire(line)
            if (q != null) {
                val run = q.groupValues[1].length
                var skipLen = run
                if (line.length > run && line[run].isWhitespace()) skipLen = run + 1
                for (k in 0 until skipLen) skip(lineStart + k)
                inline(line.substring(skipLen), lineStart + skipLen, base)
                return
            }
            // علامة قائمة: - / 1. / - [ ] — تبقى ظاهرة خافتة
            val m = Regex("^(\\s*((\\d+)\\. |[-*+] (\\[[ x]\\])? ?))").find(line)
            if (m != null && m.groupValues[1].isNotBlank()) {
                val mark = m.groupValues[1]
                val s0 = d
                for (k in mark.indices) emit(lineStart + k, mark[k])
                spans.add(WysiwygSpan(s0, d, SpanStyle(color = dim)))
                inline(line.substring(mark.length), lineStart + mark.length, base)
                return
            }
            inline(line, lineStart, base)
        }

        var off = 0
        val lines = folded.split('\n')
        lines.forEachIndexed { li, line ->
            blockLine(line, off)
            off += line.length
            if (li != lines.lastIndex) { emit(off, '\n'); off++ }
        }
        toDisplay[folded.length] = d
        return WysiwygResult(out.toString(), spans, folded.length, toFolded.toIntArray(), toDisplay)
    }
}
