package com.nota.notes.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nota.notes.R

// خطوط مدمجة داخل التطبيق (SIL Open Font License — تعمل دون إنترنت):
// أميري (نسخي مميز) للعناوين العربية، تجوال للنصوص، DM Serif للعناوين الإنجليزية
val AmiriFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)
val TajawalFamily = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)
val DmSerifFamily = FontFamily(
    Font(R.font.dmserif_regular, FontWeight.Normal)
)
