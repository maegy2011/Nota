package com.nota.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// شرائح الملاحظات الثمانية (المواصفة المنقحة): يُخزَّن رقم الـ slot لا قيمة الـ hex —
// فأي مراجعة مستقبلية للوحة لن تُتلف الملاحظات المحفوظة. الترتيب ثابت أبدًا لسلامة المخزن.
data class NoteColor(val light: Color, val dark: Color, val onLight: Color, val onDark: Color)

val NotePalette = listOf(
    // 0 = المحايد Neutral: يتبع سطح الثيم — عنصر شكلي لا يُستخدم كخلفية مباشرة
    NoteColor(Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent),
    NoteColor(Color(0xFFFFF1CC), Color(0xFF3D2E00), Color(0xFF3D2E00), Color(0xFFFFE7A8)), // كهرماني Amber
    NoteColor(Color(0xFFDCF5E0), Color(0xFF10331C), Color(0xFF0B2210), Color(0xFFC3EFC7)), // نعناعي Mint
    NoteColor(Color(0xFFFFE0EC), Color(0xFF4A2135), Color(0xFF3E0022), Color(0xFFFFD8E6)), // وردي Rose
    NoteColor(Color(0xFFDCEBFF), Color(0xFF0E2A45), Color(0xFF001C38), Color(0xFFCFE4FF)), // سماوي Sky
    NoteColor(Color(0xFFEADDFF), Color(0xFF2B2140), Color(0xFF22005D), Color(0xFFE9DDFF)), // لافندر Lavender
    NoteColor(Color(0xFFFFE2DE), Color(0xFF4A2420), Color(0xFF410E0B), Color(0xFFFFD5CF)), // مرجاني Coral
    NoteColor(Color(0xFFF3E8D8), Color(0xFF33291C), Color(0xFF2A2113), Color(0xFFEFE0C8))  // رملي Sand
)

// ليل/نهار التطبيق (وليس النظام) — يوفّره App.kt من إعداد الثيم حتى تتبع البطاقات الداكنة الوضع الليلي
val LocalNotaDark = compositionLocalOf { false }

private fun valid(index: Int) = index in 1 until NotePalette.size

@Composable
@ReadOnlyComposable
fun noteCardBg(index: Int): Color {
    if (!valid(index)) return MaterialTheme.colorScheme.surface
    val c = NotePalette[index]
    return if (LocalNotaDark.current) c.dark else c.light
}

@Composable
@ReadOnlyComposable
fun noteCardOn(index: Int): Color {
    if (!valid(index)) return MaterialTheme.colorScheme.onSurface
    val c = NotePalette[index]
    return if (LocalNotaDark.current) c.onDark else c.onLight
}

fun noteCardSub(on: Color): Color = on.copy(alpha = 0.72f)

// شريط اختيار اللون — لمس 48dp موحد مع نقاط الفلاتر (غلاف خارجي + عينة داخلية)
@Composable
fun ColorPickerRow(selected: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
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
                            width = if (isSel) 3.dp else 1.dp,
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
