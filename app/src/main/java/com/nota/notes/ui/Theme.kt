package com.nota.notes.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// مواصفة الألوان المنقحة (20 سبتمبر 2026): ورق دافئ محايد + بذرة كهرمانية — نفس الـ tokens للعربية والإنجليزية
// معروضان للثيمات (قرار 26 سبتمبر 2026): الدافئ هو الثيم الافتراضي المجاني
internal val WarmDarkScheme = darkColorScheme(
    primary = Color(0xFFF2BF48),
    onPrimary = Color(0xFF412D00),
    primaryContainer = Color(0xFF5E4200),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = Color(0xFFD7C4A0),
    onSecondary = Color(0xFF3A2F17),
    secondaryContainer = Color(0xFF52452C),
    onSecondaryContainer = Color(0xFFF4E0BB),
    tertiary = Color(0xFFB3CFAB),
    onTertiary = Color(0xFF1F361D),
    tertiaryContainer = Color(0xFF354D33),
    onTertiaryContainer = Color(0xFFCEEBC6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF16130E),
    onBackground = Color(0xFFE9E1D5),
    surface = Color(0xFF16130E),
    onSurface = Color(0xFFE9E1D5),
    surfaceVariant = Color(0xFF4E4639),
    onSurfaceVariant = Color(0xFFD1C5B4),
    surfaceContainerLowest = Color(0xFF100E09),
    surfaceContainerLow = Color(0xFF1F1B16),
    surfaceContainer = Color(0xFF231F1A),
    surfaceContainerHigh = Color(0xFF2E2923),
    surfaceContainerHighest = Color(0xFF39342E),
    inverseSurface = Color(0xFFE9E1D5),
    inverseOnSurface = Color(0xFF34302A),
    inversePrimary = Color(0xFF7A5900),
    outline = Color(0xFF9A8F80),
    outlineVariant = Color(0xFF4E4639),
    scrim = Color.Black
)

internal val WarmLightScheme = lightColorScheme(
    primary = Color(0xFF7A5900),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDF9E),
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF6B5E3F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E0BB),
    onSecondaryContainer = Color(0xFF241A04),
    tertiary = Color(0xFF4C6547),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCEEBC6),
    onTertiaryContainer = Color(0xFF0A2009),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBF3),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFFBF3),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFEDE1CF),
    onSurfaceVariant = Color(0xFF4E4639),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF3E6),
    surfaceContainer = Color(0xFFF5EDE0),
    surfaceContainerHigh = Color(0xFFEFE7DA),
    surfaceContainerHighest = Color(0xFFE9E1D5),
    inverseSurface = Color(0xFF34302A),
    inverseOnSurface = Color(0xFFF8EFE2),
    inversePrimary = Color(0xFFF2BF48),
    outline = Color(0xFF7F7667),
    outlineVariant = Color(0xFFD1C5B4),
    scrim = Color.Black
)

// زوايا عصرية انسيابية (M3 Expressive) — بطاقات وأزرار وحوارات أكثر نعومة في كل الشاشات
private val NotaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// خطوط التطبيق: العناوين بخط مميز (أميري عربي / DM Serif إنجليزي) والنصوص بتجوال
// العربية بنفس الوزن الاسمي تبدو أثقل — تُنزَل درجة وزن واحدة عندما تكون العربية نشطة
private fun stepDown(w: FontWeight): FontWeight = when (w) {
    FontWeight.Bold -> FontWeight.Medium
    FontWeight.SemiBold -> FontWeight.Medium
    FontWeight.Medium -> FontWeight.Normal
    else -> w
}

private fun notaTypography(head: FontFamily, body: FontFamily, arabic: Boolean): Typography {
    val d = Typography()
    fun bodyOf(s: androidx.compose.ui.text.TextStyle) =
        s.copy(fontFamily = body, fontWeight = if (arabic) stepDown(s.fontWeight ?: FontWeight.Normal) else s.fontWeight)
    fun headOf(s: androidx.compose.ui.text.TextStyle) =
        s.copy(fontFamily = head, fontWeight = if (arabic) stepDown(s.fontWeight ?: FontWeight.Normal) else s.fontWeight)
    return Typography(
        displayLarge = headOf(d.displayLarge),
        displayMedium = headOf(d.displayMedium),
        displaySmall = headOf(d.displaySmall),
        headlineLarge = headOf(d.headlineLarge),
        headlineMedium = headOf(d.headlineMedium),
        headlineSmall = headOf(d.headlineSmall),
        titleLarge = headOf(d.titleLarge),
        titleMedium = headOf(d.titleMedium),
        titleSmall = headOf(d.titleSmall),
        bodyLarge = bodyOf(d.bodyLarge),
        bodyMedium = bodyOf(d.bodyMedium),
        bodySmall = bodyOf(d.bodySmall),
        labelLarge = bodyOf(d.labelLarge),
        labelMedium = bodyOf(d.labelMedium),
        labelSmall = bodyOf(d.labelSmall)
    )
}

@Composable
fun NotaTheme(
    dark: Boolean = isSystemInDarkTheme(),
    head: FontFamily = AmiriFamily,
    body: FontFamily = TajawalFamily,
    arabic: Boolean = true,
    themeId: String = "warm",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = notaThemeFor(themeId).colorScheme(dark),
        typography = notaTypography(head, body, arabic),
        shapes = NotaShapes,
        content = content
    )
}
