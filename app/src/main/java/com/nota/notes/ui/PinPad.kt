package com.nota.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nota.notes.R

// لوحة أرقام داخلية واحدة لكل شاشات PIN — بلا كيبورد نظام (خصوصية: لا تسجيل خارجي)
// الإرسال يدوي دائمًا من الزر الأب — اللوحة إدخال فقط (منعًا للتخمين التلقائي)
@Composable
fun PinDots(pin: String, maxLen: Int = 8, contentDesc: String = "") {
    // النقاط LTR دائمًا حتى داخل RTL — وصف ديناميكي لقارئ الشاشة (فارغ/N من 8)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) { contentDescription = contentDesc }
        ) {
            repeat(maxLen) { i ->
                val filled = i < pin.length
                Box(
                    Modifier
                        .padding(horizontal = 5.dp)
                        .size(if (filled) 14.dp else 12.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

@Composable
fun PinPad(
    pin: String,
    onPin: (String) -> Unit,
    maxLen: Int = 8,
    // البصمة زر داخل اللوحة (الخانة اليسرى بالصف الأخير) — لا زر خارجي منفصل
    showBio: Boolean = false,
    onBio: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    // احترام إعداد الاهتزاز العام — يُقرأ لحظة الضغط بلا حاجة لإعادة تركيب
    fun tap(type: HapticFeedbackType = HapticFeedbackType.TextHandleMove) {
        if (Haptics.enabled) haptic.performHapticFeedback(type)
    }
    // حجم تكيفي: الشاشات القصيرة (< 620dp طولًا) مفاتيح 64dp بدل 72dp
    val keySize = if (LocalConfiguration.current.screenHeightDp < 620) 64.dp else 72.dp
    val bioCd = stringResource(R.string.unlock_bio_cd)
    // الأرقام LTR دائمًا
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // وصف النقاط: فارغ أو N من الحد — يُمرر من موارد الشاشة الأب
            val dotsDesc = if (pin.isEmpty()) stringResource(R.string.pindots_empty)
            else stringResource(R.string.pindots_filled_fmt, pin.length, maxLen)
            PinDots(pin = pin, maxLen = maxLen, contentDesc = dotsDesc)
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
            chunks@ for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    for (col in 0..2) {
                        val d = keys[row * 3 + col]
                        PinKey(label = d, size = keySize) {
                            tap()
                            if (pin.length < maxLen && pin.all(Char::isDigit)) onPin(pin + d)
                        }
                    }
                }
            }
            // الصف الأخير: [بصمة أو فراغ] + 0 + مسح
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                if (showBio) {
                    PinKeyIcon(
                        contentDesc = bioCd,
                        size = keySize,
                        onClick = { tap(HapticFeedbackType.LongPress); onBio() }
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                } else {
                    Box(Modifier.size(72.dp))
                }
                PinKey(label = "0", size = keySize) {
                    tap()
                    if (pin.length < maxLen) onPin(pin + "0")
                }
                PinKeyIcon(
                    contentDesc = stringResource(R.string.clear_search_cd),
                    size = keySize,
                    onClick = {
                        tap()
                        if (pin.isNotEmpty()) onPin(pin.dropLast(1))
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
private fun PinKey(label: String, size: Dp = 72.dp, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(size)
            .semantics { contentDescription = label }
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun PinKeyIcon(contentDesc: String, size: Dp = 72.dp, onClick: () -> Unit, icon: @Composable () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(size)
            .semantics { contentDescription = contentDesc }
    ) {
        icon()
    }
}
