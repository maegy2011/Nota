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

// شاشة بداية فاخرة: خلفية تتكيف مع الوضع (فاتحة للنهار، داكنة للليل) + شارة ذهبية + اسم + سطر — تظهر بعد سباش النظام مباشرة
@Composable
fun LuxurySplashScreen() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "splashScale"
    )
    val isDark = isSystemInDarkTheme()
    val splashBackground = if (isDark) Color(0xFF16130E) else Color(0xFFFAF9F6)
    val appNameColor = if (isDark) Color(0xFFF2CE6B) else Color(0xFF000000)
    val taglineColor = if (isDark) Color(0xFFC9B88A) else Color(0xFF000000)
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
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = taglineColor
            )
        }
        // توقيع سفلي خافت
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .alpha(alpha * 0.8f)
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
