package com.nota.notes.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nota.notes.R

// الثيمات (قرار 26 سبتمبر 2026): التطبيق كامل — 2 مجاني + 10 مدفوعة من لوحات coolors
// الدافئ يستعيد مخططي Theme.kt الأصليين — بطاقات الملاحظات الملونة تبقى كما هي (محتوى لا هوية)
data class NotaAppTheme(
    val id: String,
    val nameRes: Int,
    val free: Boolean,
    val light: ColorScheme?,
    val dark: ColorScheme?
)


internal val Theme_warm = NotaAppTheme(
    id = "warm", nameRes = R.string.theme_warm, free = true,
    light = null, dark = null
)

internal val Theme_paper = NotaAppTheme(
    id = "paper", nameRes = R.string.theme_paper, free = true,
    light =
    lightColorScheme(
        primary = Color(0xFF44546A), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E4F7), onPrimaryContainer = Color(0xFF0D1D33),
        secondary = Color(0xFF555F71), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD9E2F3), onSecondaryContainer = Color(0xFF111C2E),
        tertiary = Color(0xFF6C5B40), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF3DFBB), onTertiaryContainer = Color(0xFF231A08),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFAF9F6), onBackground = Color(0xFF191C22),
        surface = Color(0xFFFAF9F6), onSurface = Color(0xFF191C22),
        surfaceVariant = Color(0xFFE1E2E8), onSurfaceVariant = Color(0xFF44474F),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F3F7),
        surfaceContainer = Color(0xFFEEEEEF), surfaceContainerHigh = Color(0xFFE8E9EE),
        surfaceContainerHighest = Color(0xFFE1E2E8),
        inverseSurface = Color(0xFF2E3138), inverseOnSurface = Color(0xFFF1F0F4),
        inversePrimary = Color(0xFFA9C7EC),
        outline = Color(0xFF74777F), outlineVariant = Color(0xFFC4C6D0),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFFA9C7EC), onPrimary = Color(0xFF0B1D33),
        primaryContainer = Color(0xFF2C4A6B), onPrimaryContainer = Color(0xFFD6E4F7),
        secondary = Color(0xFFBDC7DC), onSecondary = Color(0xFF263141),
        secondaryContainer = Color(0xFF3A4558), onSecondaryContainer = Color(0xFFD9E2F3),
        tertiary = Color(0xFFD7C2A0), onTertiary = Color(0xFF3A2E18),
        tertiaryContainer = Color(0xFF4A3D28), onTertiaryContainer = Color(0xFFF3DFBB),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF101418), onBackground = Color(0xFFE2E2E9),
        surface = Color(0xFF101418), onSurface = Color(0xFFE2E2E9),
        surfaceVariant = Color(0xFF44474F), onSurfaceVariant = Color(0xFFC4C6D0),
        surfaceContainerLowest = Color(0xFF0B0E13), surfaceContainerLow = Color(0xFF141920),
        surfaceContainer = Color(0xFF1A1F26), surfaceContainerHigh = Color(0xFF242B35),
        surfaceContainerHighest = Color(0xFF2F3642),
        inverseSurface = Color(0xFFE2E2E9), inverseOnSurface = Color(0xFF2E3138),
        inversePrimary = Color(0xFF44546A),
        outline = Color(0xFF8E9099), outlineVariant = Color(0xFF44474F),
        scrim = Color.Black
    )
)
internal val Theme_royalgold = NotaAppTheme(
    id = "royalgold", nameRes = R.string.theme_royalgold, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF00509D), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD500), onPrimaryContainer = Color(0xFF00296B),
        secondary = Color(0xFF003F88), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD8E4F5), onSecondaryContainer = Color(0xFF00296B),
        tertiary = Color(0xFF8A6D00), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFE08A), onTertiaryContainer = Color(0xFF00296B),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBF9F2), onBackground = Color(0xFF101A33),
        surface = Color(0xFFFBF9F2), onSurface = Color(0xFF101A33),
        surfaceVariant = Color(0xFFE4E9F2), onSurfaceVariant = Color(0xFF3E4A63),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF4F6FB),
        surfaceContainer = Color(0xFFEFF3F9), surfaceContainerHigh = Color(0xFFE7EDF6),
        surfaceContainerHighest = Color(0xFFDFE6F2),
        inverseSurface = Color(0xFF00296B), inverseOnSurface = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFFDC500),
        outline = Color(0xFF6E7A94), outlineVariant = Color(0xFFC2CCDE),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFFFFD500), onPrimary = Color(0xFF00296B),
        primaryContainer = Color(0xFF003F88), onPrimaryContainer = Color(0xFFFFD500),
        secondary = Color(0xFFFDC500), onSecondary = Color(0xFF00296B),
        secondaryContainer = Color(0xFF00509D), onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFFFFD500), onTertiary = Color(0xFF00296B),
        tertiaryContainer = Color(0xFF5C4A00), onTertiaryContainer = Color(0xFFFFD500),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF001A4D), onBackground = Color(0xFFE8EEF9),
        surface = Color(0xFF001A4D), onSurface = Color(0xFFE8EEF9),
        surfaceVariant = Color(0xFF1F3C73), onSurfaceVariant = Color(0xFFC9D6EF),
        surfaceContainerLowest = Color(0xFF000F33), surfaceContainerLow = Color(0xFF001540),
        surfaceContainer = Color(0xFF001C52), surfaceContainerHigh = Color(0xFF0A2A63),
        surfaceContainerHighest = Color(0xFF14306F),
        inverseSurface = Color(0xFFE8EEF9), inverseOnSurface = Color(0xFF00296B),
        inversePrimary = Color(0xFF00509D),
        outline = Color(0xFF8FA0C2), outlineVariant = Color(0xFF1F3C73),
        scrim = Color.Black
    )
)

internal val Theme_cocoarose = NotaAppTheme(
    id = "cocoarose", nameRes = R.string.theme_cocoarose, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF573C27), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFADC6), onPrimaryContainer = Color(0xFF4A0E28),
        secondary = Color(0xFFA98360), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFDEED9), onSecondaryContainer = Color(0xFF3A2A12),
        tertiary = Color(0xFFE34989), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E6), onTertiaryContainer = Color(0xFF4A0E28),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBF7), onBackground = Color(0xFF221A14),
        surface = Color(0xFFFFFBF7), onSurface = Color(0xFF221A14),
        surfaceVariant = Color(0xFFEDE0D4), onSurfaceVariant = Color(0xFF52453A),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFAF4EC),
        surfaceContainer = Color(0xFFF6EFE5), surfaceContainerHigh = Color(0xFFF2E9DC),
        surfaceContainerHighest = Color(0xFFEDE0D4),
        inverseSurface = Color(0xFF3A2E24), inverseOnSurface = Color(0xFFFFEDE4),
        inversePrimary = Color(0xFFFFADC6),
        outline = Color(0xFF84766A), outlineVariant = Color(0xFFD6C3B2),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFFFFADC6), onPrimary = Color(0xFF4A0E28),
        primaryContainer = Color(0xFF7A2A4A), onPrimaryContainer = Color(0xFFFFD8E6),
        secondary = Color(0xFFD9B48F), onSecondary = Color(0xFF3A2A12),
        secondaryContainer = Color(0xFF573C27), onSecondaryContainer = Color(0xFFFDEED9),
        tertiary = Color(0xFFE34989), onTertiary = Color(0xFF3A0718),
        tertiaryContainer = Color(0xFF8F2050), onTertiaryContainer = Color(0xFFFFD8E6),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF1E1410), onBackground = Color(0xFFF2E4D8),
        surface = Color(0xFF1E1410), onSurface = Color(0xFFF2E4D8),
        surfaceVariant = Color(0xFF52453A), onSurfaceVariant = Color(0xFFD6C3B2),
        surfaceContainerLowest = Color(0xFF150E0A), surfaceContainerLow = Color(0xFF241914),
        surfaceContainer = Color(0xFF2A1E18), surfaceContainerHigh = Color(0xFF352820),
        surfaceContainerHighest = Color(0xFF42332A),
        inverseSurface = Color(0xFFF2E4D8), inverseOnSurface = Color(0xFF3A2E24),
        inversePrimary = Color(0xFF573C27),
        outline = Color(0xFFA08D7D), outlineVariant = Color(0xFF52453A),
        scrim = Color.Black
    )
)

internal val Theme_forest = NotaAppTheme(
    id = "forest", nameRes = R.string.theme_forest, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF386641), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA7C957), onPrimaryContainer = Color(0xFF10200F),
        secondary = Color(0xFF6A994E), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD9EAD3), onSecondaryContainer = Color(0xFF10200F),
        tertiary = Color(0xFFBC4749), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDAD8), onTertiaryContainer = Color(0xFF40090A),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBF8EF), onBackground = Color(0xFF1A1F18),
        surface = Color(0xFFFBF8EF), onSurface = Color(0xFF1A1F18),
        surfaceVariant = Color(0xFFDEE5D8), onSurfaceVariant = Color(0xFF414941),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F4EC),
        surfaceContainer = Color(0xFFEEF0E6), surfaceContainerHigh = Color(0xFFE8EBDF),
        surfaceContainerHighest = Color(0xFFDEE5D8),
        inverseSurface = Color(0xFF2C322B), inverseOnSurface = Color(0xFFF0F2EA),
        inversePrimary = Color(0xFFA7C957),
        outline = Color(0xFF72796F), outlineVariant = Color(0xFFC2C9BB),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFFA7C957), onPrimary = Color(0xFF10200F),
        primaryContainer = Color(0xFF2A4A2E), onPrimaryContainer = Color(0xFFD9EAD3),
        secondary = Color(0xFF9BD180), onSecondary = Color(0xFF0F2A12),
        secondaryContainer = Color(0xFF386641), onSecondaryContainer = Color(0xFFF2E8CF),
        tertiary = Color(0xFFF49B9D), onTertiary = Color(0xFF40090A),
        tertiaryContainer = Color(0xFF713335), onTertiaryContainer = Color(0xFFFFDAD8),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0E150E), onBackground = Color(0xFFE0E4DA),
        surface = Color(0xFF0E150E), onSurface = Color(0xFFE0E4DA),
        surfaceVariant = Color(0xFF414941), onSurfaceVariant = Color(0xFFC2C9BB),
        surfaceContainerLowest = Color(0xFF090D09), surfaceContainerLow = Color(0xFF131A13),
        surfaceContainer = Color(0xFF181F18), surfaceContainerHigh = Color(0xFF222A22),
        surfaceContainerHighest = Color(0xFF2D352D),
        inverseSurface = Color(0xFFE0E4DA), inverseOnSurface = Color(0xFF2C322B),
        inversePrimary = Color(0xFF386641),
        outline = Color(0xFF8D938A), outlineVariant = Color(0xFF414941),
        scrim = Color.Black
    )
)

internal val Theme_rosepastel = NotaAppTheme(
    id = "rosepastel", nameRes = R.string.theme_rosepastel, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF9D8189), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFCAD4), onPrimaryContainer = Color(0xFF421524),
        secondary = Color(0xFF7A6A72), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFE5D9), onSecondaryContainer = Color(0xFF3A2A1E),
        tertiary = Color(0xFFD16A82), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDCE3), onTertiaryContainer = Color(0xFF421524),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFAFA), onBackground = Color(0xFF211A1D),
        surface = Color(0xFFFFFAFA), onSurface = Color(0xFF211A1D),
        surfaceVariant = Color(0xFFEADFE2), onSurfaceVariant = Color(0xFF504348),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFAF3F5),
        surfaceContainer = Color(0xFFF6EEF1), surfaceContainerHigh = Color(0xFFF2E8EC),
        surfaceContainerHighest = Color(0xFFEADFE2),
        inverseSurface = Color(0xFF372E32), inverseOnSurface = Color(0xFFFFECF1),
        inversePrimary = Color(0xFFF4ACB7),
        outline = Color(0xFF81747A), outlineVariant = Color(0xFFD5C2C9),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFFF4ACB7), onPrimary = Color(0xFF421524),
        primaryContainer = Color(0xFF6E3A48), onPrimaryContainer = Color(0xFFFFDCE3),
        secondary = Color(0xFFD8C3CB), onSecondary = Color(0xFF3A2A32),
        secondaryContainer = Color(0xFF4E3A42), onSecondaryContainer = Color(0xFFFFE5D9),
        tertiary = Color(0xFFFFCAD4), onTertiary = Color(0xFF421524),
        tertiaryContainer = Color(0xFF6E3A48), onTertiaryContainer = Color(0xFFFFDCE3),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF1A1416), onBackground = Color(0xFFF0E3E7),
        surface = Color(0xFF1A1416), onSurface = Color(0xFFF0E3E7),
        surfaceVariant = Color(0xFF504348), onSurfaceVariant = Color(0xFFD5C2C9),
        surfaceContainerLowest = Color(0xFF120D0F), surfaceContainerLow = Color(0xFF20181B),
        surfaceContainer = Color(0xFF261D20), surfaceContainerHigh = Color(0xFF31272B),
        surfaceContainerHighest = Color(0xFF3D3237),
        inverseSurface = Color(0xFFF0E3E7), inverseOnSurface = Color(0xFF372E32),
        inversePrimary = Color(0xFF9D8189),
        outline = Color(0xFF9C8E94), outlineVariant = Color(0xFF504348),
        scrim = Color.Black
    )
)

internal val Theme_candy = NotaAppTheme(
    id = "candy", nameRes = R.string.theme_candy, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF9A5FA6), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFC8DD), onPrimaryContainer = Color(0xFF45173E),
        secondary = Color(0xFF5E7FA6), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFBDE0FE), onSecondaryContainer = Color(0xFF0E2A45),
        tertiary = Color(0xFFC26D94), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFAFCC), onTertiaryContainer = Color(0xFF45173E),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFE), onBackground = Color(0xFF211A24),
        surface = Color(0xFFFFFBFE), onSurface = Color(0xFF211A24),
        surfaceVariant = Color(0xFFE9DFEE), onSurfaceVariant = Color(0xFF4E444F),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF9F2FB),
        surfaceContainer = Color(0xFFF5EDF8), surfaceContainerHigh = Color(0xFFF0E7F4),
        surfaceContainerHighest = Color(0xFFE9DFEE),
        inverseSurface = Color(0xFF362F39), inverseOnSurface = Color(0xFFFFEDF6),
        inversePrimary = Color(0xFFCDB4DB),
        outline = Color(0xFF7F7480), outlineVariant = Color(0xFFD3C2D8),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFFE0A9CC), onPrimary = Color(0xFF45173E),
        primaryContainer = Color(0xFF6B3A63), onPrimaryContainer = Color(0xFFFFD9E8),
        secondary = Color(0xFFA2D2FF), onSecondary = Color(0xFF0E2A45),
        secondaryContainer = Color(0xFF3E4E6B), onSecondaryContainer = Color(0xFFBDE0FE),
        tertiary = Color(0xFFFFAFCC), onTertiary = Color(0xFF45173E),
        tertiaryContainer = Color(0xFF6B3A52), onTertiaryContainer = Color(0xFFFFD9E8),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF181220), onBackground = Color(0xFFEEE1F2),
        surface = Color(0xFF181220), onSurface = Color(0xFFEEE1F2),
        surfaceVariant = Color(0xFF4E444F), onSurfaceVariant = Color(0xFFD3C2D8),
        surfaceContainerLowest = Color(0xFF110C17), surfaceContainerLow = Color(0xFF1D1625),
        surfaceContainer = Color(0xFF241B2D), surfaceContainerHigh = Color(0xFF2F2439),
        surfaceContainerHighest = Color(0xFF3B2E46),
        inverseSurface = Color(0xFFEEE1F2), inverseOnSurface = Color(0xFF362F39),
        inversePrimary = Color(0xFF9A5FA6),
        outline = Color(0xFF9A8EA0), outlineVariant = Color(0xFF4E444F),
        scrim = Color.Black
    )
)

internal val Theme_ocean = NotaAppTheme(
    id = "ocean", nameRes = R.string.theme_ocean, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF023047), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF8ECAE6), onPrimaryContainer = Color(0xFF032A3E),
        secondary = Color(0xFF219EBC), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFC9E9F5), onSecondaryContainer = Color(0xFF032A3E),
        tertiary = Color(0xFFC26A00), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFB703), onTertiaryContainer = Color(0xFF3A2200),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFAFCFE), onBackground = Color(0xFF141B22),
        surface = Color(0xFFFAFCFE), onSurface = Color(0xFF141B22),
        surfaceVariant = Color(0xFFDCE4EB), onSurfaceVariant = Color(0xFF40484F),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF1F5F9),
        surfaceContainer = Color(0xFFEDF2F7), surfaceContainerHigh = Color(0xFFE7EEF5),
        surfaceContainerHighest = Color(0xFFDCE4EB),
        inverseSurface = Color(0xFF293241), inverseOnSurface = Color(0xFFEAF3FA),
        inversePrimary = Color(0xFF8ECAE6),
        outline = Color(0xFF70787F), outlineVariant = Color(0xFFBFC9D4),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFF8ECAE6), onPrimary = Color(0xFF032A3E),
        primaryContainer = Color(0xFF165A75), onPrimaryContainer = Color(0xFFC9E9F5),
        secondary = Color(0xFF63D2E8), onSecondary = Color(0xFF032A3E),
        secondaryContainer = Color(0xFF023047), onSecondaryContainer = Color(0xFF8ECAE6),
        tertiary = Color(0xFFFFB703), onTertiary = Color(0xFF3A2200),
        tertiaryContainer = Color(0xFF7A4C00), onTertiaryContainer = Color(0xFFFFD97A),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF08131C), onBackground = Color(0xFFDCE9F2),
        surface = Color(0xFF08131C), onSurface = Color(0xFFDCE9F2),
        surfaceVariant = Color(0xFF40484F), onSurfaceVariant = Color(0xFFBFC9D4),
        surfaceContainerLowest = Color(0xFF040B11), surfaceContainerLow = Color(0xFF0C171F),
        surfaceContainer = Color(0xFF111C25), surfaceContainerHigh = Color(0xFF1B2834),
        surfaceContainerHighest = Color(0xFF26333F),
        inverseSurface = Color(0xFFDCE9F2), inverseOnSurface = Color(0xFF293241),
        inversePrimary = Color(0xFF023047),
        outline = Color(0xFF89939B), outlineVariant = Color(0xFF40484F),
        scrim = Color.Black
    )
)

internal val Theme_sage = NotaAppTheme(
    id = "sage", nameRes = R.string.theme_sage, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF3A5A40), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA3B18A), onPrimaryContainer = Color(0xFF0E1F12),
        secondary = Color(0xFF588157), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDAD7CD), onSecondaryContainer = Color(0xFF23281E),
        tertiary = Color(0xFF6B705C), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFDCE0CF), onTertiaryContainer = Color(0xFF23281E),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFAFAF6), onBackground = Color(0xFF191C17),
        surface = Color(0xFFFAFAF6), onSurface = Color(0xFF191C17),
        surfaceVariant = Color(0xFFDDDCD2), onSurfaceVariant = Color(0xFF41473D),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF2F2EC),
        surfaceContainer = Color(0xFFEEEDE5), surfaceContainerHigh = Color(0xFFE8E7DE),
        surfaceContainerHighest = Color(0xFFDDDCD2),
        inverseSurface = Color(0xFF2B3129), inverseOnSurface = Color(0xFFEDEEE8),
        inversePrimary = Color(0xFFA3B18A),
        outline = Color(0xFF6F7568), outlineVariant = Color(0xFFC1C2B4),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFFA3B18A), onPrimary = Color(0xFF0E1F12),
        primaryContainer = Color(0xFF2A3F2E), onPrimaryContainer = Color(0xFFDCE0CF),
        secondary = Color(0xFF8FC098), onSecondary = Color(0xFF0E2A14),
        secondaryContainer = Color(0xFF3A5A40), onSecondaryContainer = Color(0xFFDAD7CD),
        tertiary = Color(0xFFC1C2B4), onTertiary = Color(0xFF23281E),
        tertiaryContainer = Color(0xFF41473D), onTertiaryContainer = Color(0xFFDDDCD2),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0F120E), onBackground = Color(0xFFE0E2D8),
        surface = Color(0xFF0F120E), onSurface = Color(0xFFE0E2D8),
        surfaceVariant = Color(0xFF41473D), onSurfaceVariant = Color(0xFFC1C2B4),
        surfaceContainerLowest = Color(0xFF0A0C09), surfaceContainerLow = Color(0xFF141713),
        surfaceContainer = Color(0xFF191D17), surfaceContainerHigh = Color(0xFF232822),
        surfaceContainerHighest = Color(0xFF2E332C),
        inverseSurface = Color(0xFFE0E2D8), inverseOnSurface = Color(0xFF2B3129),
        inversePrimary = Color(0xFF3A5A40),
        outline = Color(0xFF8B9184), outlineVariant = Color(0xFF41473D),
        scrim = Color.Black
    )
)

internal val Theme_steel = NotaAppTheme(
    id = "steel", nameRes = R.string.theme_steel, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF3D5A80), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF98C1D9), onPrimaryContainer = Color(0xFF0A2740),
        secondary = Color(0xFF5B6B7F), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE0FBFC), onSecondaryContainer = Color(0xFF0A2E38),
        tertiary = Color(0xFFC25A33), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9CC), onTertiaryContainer = Color(0xFF3E1300),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFAFCFD), onBackground = Color(0xFF16202B),
        surface = Color(0xFFFAFCFD), onSurface = Color(0xFF16202B),
        surfaceVariant = Color(0xFFDDE3EA), onSurfaceVariant = Color(0xFF42474E),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF1F5F8),
        surfaceContainer = Color(0xFFEDEFF4), surfaceContainerHigh = Color(0xFFE7EBF1),
        surfaceContainerHighest = Color(0xFFDDE3EA),
        inverseSurface = Color(0xFF293241), inverseOnSurface = Color(0xFFEAF1F8),
        inversePrimary = Color(0xFF98C1D9),
        outline = Color(0xFF71787F), outlineVariant = Color(0xFFC0C8D2),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFF98C1D9), onPrimary = Color(0xFF0A2740),
        primaryContainer = Color(0xFF2B4A66), onPrimaryContainer = Color(0xFFD5E9F5),
        secondary = Color(0xFFB9C9DA), onSecondary = Color(0xFF232F3D),
        secondaryContainer = Color(0xFF3D5A80), onSecondaryContainer = Color(0xFFE0FBFC),
        tertiary = Color(0xFFEE6C4D), onTertiary = Color(0xFF3E1300),
        tertiaryContainer = Color(0xFF7A3A20), onTertiaryContainer = Color(0xFFFFD9CC),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0D141D), onBackground = Color(0xFFDFE6EE),
        surface = Color(0xFF0D141D), onSurface = Color(0xFFDFE6EE),
        surfaceVariant = Color(0xFF42474E), onSurfaceVariant = Color(0xFFC0C8D2),
        surfaceContainerLowest = Color(0xFF080D13), surfaceContainerLow = Color(0xFF111822),
        surfaceContainer = Color(0xFF161E28), surfaceContainerHigh = Color(0xFF212A37),
        surfaceContainerHighest = Color(0xFF2C3543),
        inverseSurface = Color(0xFFDFE6EE), inverseOnSurface = Color(0xFF293241),
        inversePrimary = Color(0xFF3D5A80),
        outline = Color(0xFF8B939C), outlineVariant = Color(0xFF42474E),
        scrim = Color.Black
    )
)

internal val Theme_stone = NotaAppTheme(
    id = "stone", nameRes = R.string.theme_stone, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF6B5D4F), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6CCC2), onPrimaryContainer = Color(0xFF241B12),
        secondary = Color(0xFF7A6F63), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF5EBE0), onSecondaryContainer = Color(0xFF2A231B),
        tertiary = Color(0xFF8A6F5C), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE3D5CA), onTertiaryContainer = Color(0xFF2A1D12),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFDFCF9), onBackground = Color(0xFF1C1B18),
        surface = Color(0xFFFDFCF9), onSurface = Color(0xFF1C1B18),
        surfaceVariant = Color(0xFFE4E0D8), onSurfaceVariant = Color(0xFF4A4640),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF7F4EE),
        surfaceContainer = Color(0xFFF3EFE8), surfaceContainerHigh = Color(0xFFEEE9E0),
        surfaceContainerHighest = Color(0xFFE4E0D8),
        inverseSurface = Color(0xFF33302B), inverseOnSurface = Color(0xFFF3EFE8),
        inversePrimary = Color(0xFFD6CCC2),
        outline = Color(0xFF7A756B), outlineVariant = Color(0xFFCBC5B8),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFFD5BDAF), onPrimary = Color(0xFF2A1D12),
        primaryContainer = Color(0xFF4E4237), onPrimaryContainer = Color(0xFFE3D5CA),
        secondary = Color(0xFFCFC0B1), onSecondary = Color(0xFF2A231B),
        secondaryContainer = Color(0xFF3E362D), onSecondaryContainer = Color(0xFFF5EBE0),
        tertiary = Color(0xFFE3D5CA), onTertiary = Color(0xFF2A1D12),
        tertiaryContainer = Color(0xFF4E4237), onTertiaryContainer = Color(0xFFE3D5CA),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF141210), onBackground = Color(0xFFE9E2D8),
        surface = Color(0xFF141210), onSurface = Color(0xFFE9E2D8),
        surfaceVariant = Color(0xFF4A4640), onSurfaceVariant = Color(0xFFCBC5B8),
        surfaceContainerLowest = Color(0xFF0D0C0A), surfaceContainerLow = Color(0xFF191614),
        surfaceContainer = Color(0xFF1E1A17), surfaceContainerHigh = Color(0xFF292420),
        surfaceContainerHighest = Color(0xFF35302A),
        inverseSurface = Color(0xFFE9E2D8), inverseOnSurface = Color(0xFF33302B),
        inversePrimary = Color(0xFF6B5D4F),
        outline = Color(0xFF978F83), outlineVariant = Color(0xFF4A4640),
        scrim = Color.Black
    )
)

internal val Theme_onyx = NotaAppTheme(
    id = "onyx", nameRes = R.string.theme_onyx, free = false,
    light =
    lightColorScheme(
        primary = Color(0xFF14213D), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFCA311), onPrimaryContainer = Color(0xFF241500),
        secondary = Color(0xFF5C6470), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE5E5E5), onSecondaryContainer = Color(0xFF1A1D22),
        tertiary = Color(0xFF9A6200), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD88A), onTertiaryContainer = Color(0xFF241500),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFCFCFC), onBackground = Color(0xFF000000),
        surface = Color(0xFFFCFCFC), onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFE2E4E8), onSurfaceVariant = Color(0xFF2A2E35),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF4F5F7),
        surfaceContainer = Color(0xFFEFF1F4), surfaceContainerHigh = Color(0xFFE9EBEF),
        surfaceContainerHighest = Color(0xFFE2E4E8),
        inverseSurface = Color(0xFF000000), inverseOnSurface = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFFCA311),
        outline = Color(0xFF3A3F47), outlineVariant = Color(0xFFC4C8D0),
        scrim = Color.Black
    ),
    dark =
    darkColorScheme(
        primary = Color(0xFFFCA311), onPrimary = Color(0xFF241500),
        primaryContainer = Color(0xFF5C4309), onPrimaryContainer = Color(0xFFFFD88A),
        secondary = Color(0xFFE5E5E5), onSecondary = Color(0xFF1A1D22),
        secondaryContainer = Color(0xFF3A4356), onSecondaryContainer = Color(0xFFE5E5E5),
        tertiary = Color(0xFFFCA311), onTertiary = Color(0xFF241500),
        tertiaryContainer = Color(0xFF5C4309), onTertiaryContainer = Color(0xFFFFD88A),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000), onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF2A2E35), onSurfaceVariant = Color(0xFFC4C8D0),
        surfaceContainerLowest = Color(0xFF000000), surfaceContainerLow = Color(0xFF0A0A0C),
        surfaceContainer = Color(0xFF101014), surfaceContainerHigh = Color(0xFF1A1A1F),
        surfaceContainerHighest = Color(0xFF26262C),
        inverseSurface = Color(0xFFFFFFFF), inverseOnSurface = Color(0xFF000000),
        inversePrimary = Color(0xFF14213D),
        outline = Color(0xFFE5E5E5), outlineVariant = Color(0xFF3A3F47),
        scrim = Color.Black
    )
)

val NotaThemes: List<NotaAppTheme> = listOf(
    Theme_warm, Theme_paper, Theme_royalgold, Theme_cocoarose, Theme_forest, Theme_rosepastel, Theme_candy, Theme_ocean, Theme_sage, Theme_steel, Theme_stone, Theme_onyx
)

fun notaThemeFor(id: String): NotaAppTheme = NotaThemes.firstOrNull { it.id == id } ?: Theme_warm

@Composable
fun NotaAppTheme.colorScheme(dark: Boolean): ColorScheme {
    if (id == "warm") return if (dark) WarmDarkScheme else WarmLightScheme
    return (if (dark) this.dark else this.light) ?: (if (dark) WarmDarkScheme else WarmLightScheme)
}
