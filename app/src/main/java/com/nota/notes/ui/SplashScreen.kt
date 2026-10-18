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

// بوابة مشتركة بين Activity (نظام Splash) وCompose — الخيار: System + Compose (مهمة 1، 18 أكتوبر 2026)
// النظام يعرض الشعار ثابتًا على نفس الخلفية حتى جاهزية التهيئة، ثم Compose بحركة 350ms فقط
// (System Splash لا يدعم خط Amiri ولا التوقيع السفلي — لذا Compose ضروري لا مكرر)
object SplashGate {
    @Volatile var keepOnScreen: Boolean = true
}

@Composable
fun LuxurySplashScreen(
    onAnimationDone: () -> Unit = {},
    showLoading: Boolean = false // مؤشر تحميل بعد 1000ms تهيئة (مهمة 3) — الخلفية والشعار يبقيان ظاهرين
) {
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
    // نفس خلفية الثيم تمامًا (Theme.kt — مهمة 4: مطابقة مؤكدة) — الانتقال استمرار لا انقطاع
    val splashBackground = if (isDark) Color(0xFF16130E) else Color(0xFFFFFBF3)
    val appNameColor = if (isDark) Color(0xFFF2CE6B) else Color(0xFF000000)
    // توقيع WCAG AA 4.5:1 (مهمة 2، محسوب يدويًا): نهار أسود alpha .7 ≈ 8.4:1 ✓ — ليل #C9B88A (ذهبي الثيم) alpha .7 ≈ 5.2:1 ✓
    // (#6B5F4C القديم ليلًا كان 1.6:1 فقط حتى بكامل الشفافية 2.97:1 — مرفوض)
    val copyrightColor = if (isDark) Color(0xFFC9B88A) else Color(0xFF000000)
    val spinnerColor = if (isDark) Color(0xFFF2CE6B) else Color(0xFF7A5900)
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
            // مؤشر التهيئة الطويلة فقط — بلا نص جديد (مهمة 3)
            if (showLoading) {
                Spacer(Modifier.height(24.dp))
                androidx.compose.material3.CircularProgressIndicator(color = spinnerColor)
            }
        }
        // توقيع سفلي — يظهر مع الشعار في نفس اللحظة (نفس alpha)، بلا تأخير إضافي — alpha .7 لضمان 4.5:1 (مهمة 2)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .alpha(0.7f * alpha)
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
