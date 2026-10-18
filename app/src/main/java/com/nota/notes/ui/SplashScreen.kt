package com.nota.notes.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.nota.notes.R
import kotlinx.coroutines.delay

// شاشة بداية 2026: نافذة حركة واحدة 350ms (تكبير 0.92→1 + تلاشي متزامن) — بلا سطر تعريفي، بلا تأخير مصطنع
// الخلفية مطابقة لخلفية الثيم (0xFFFFFBF3 نهارًا / 0x16130E ليلًا) لانتقال سلس إلى Onboarding
private const val SPLASH_ANIM_MS = 350

@Composable
fun LuxurySplashScreen(onAnimationDone: () -> Unit = {}) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        // إشارة انتهاء الحركة فقط (350ms) — ليست تأخيرًا مصطنعًا للعرض
        delay(SPLASH_ANIM_MS.toLong())
        onAnimationDone()
    }
    // نافذة زمنية واحدة: التكبير والتلاشي متزامنان
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = SPLASH_ANIM_MS, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = tween(durationMillis = SPLASH_ANIM_MS, easing = FastOutSlowInEasing),
        label = "splashScale"
    )
    val isDark = isSystemInDarkTheme()
    // نفس خلفية الثيم تمامًا (Theme.kt) — الانتقال إلى Onboarding استمرار لا انقطاع
    val splashBackground = if (isDark) Color(0xFF16130E) else Color(0xFFFFFBF3)
    val appNameColor = if (isDark) Color(0xFFF2CE6B) else Color(0xFF000000)
    // ذهبي النهار للشعار ثابت (أصل العلامة ic_nota_badge) — اسم التطبيق أسود يحقق تباينًا كاملًا
    val copyrightColor = if (isDark) Color(0xFF6B5F4C) else Color(0xFF000000)
    Box(
        Modifier
            .fillMaxSize()
            .background(splashBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha)
        ) {
            Box(Modifier.scale(scale)) {
                BrandHeader(size = 112.dp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                fontFamily = AmiriFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                color = appNameColor
            )
        }
        // توقيع سفلي مخفف — يظهر مع الشعار في نفس اللحظة (نفس alpha)، بلا تأخير إضافي
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .alpha(0.5f * alpha)
        ) {
            Text(
                // حقوق الملكية الفكرية: اسم التطبيق + السنة — بأمر محمد 27 سبتمبر 2026
                text = "© 2026 " + stringResource(R.string.app_name) + " — جميع الحقوق محفوظة",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = copyrightColor,
                modifier = Modifier.paddingFromBaseline(bottom = 48.dp)
            )
        }
    }
}
