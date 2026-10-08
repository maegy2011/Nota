package com.nota.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─── لغة نوتا الفاخرة Luxury Gold (25 سبتمبر 2026) ───
// ذهبي ثابت ليلًا ونهارًا فوقه بني داكن — تباين مضمون في كل الأوضاع

val LuxuryGold = Color(0xFFF2CE6B)
val LuxuryOnGold = Color(0xFF3A2B00)

// شريط ذهبي رئيسي: تدرج العلامة + عنوان أبيض + سطر فرعي — زوايا 20dp كبطاقة فاخرة
@Composable
fun GoldHero(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(goldGradient())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 1
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}
