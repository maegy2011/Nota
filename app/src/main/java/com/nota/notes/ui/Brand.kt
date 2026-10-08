package com.nota.notes.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nota.notes.R

// الهوية البصرية: تدرج ذهبي ثابت في كل الأوضاع فوقه الأبيض — تباين مضمون ليلًا ونهارًا
@Composable
@ReadOnlyComposable
fun goldGradient(): Brush = Brush.linearGradient(
    listOf(Color(0xFFF2CE6B), Color(0xFFE8C15A), Color(0xFFC9962E))
)

// شارة دائرية متدرجة لأيقونات المزايا (التعريف والقفل)
@Composable
fun BrandBadge(icon: ImageVector, contentDesc: String? = null, size: Dp = 96.dp) {
    Box(
        Modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(goldGradient()),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription = contentDesc, tint = Color.White,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

// شعار نوتا: الأيقونة الدائرية بخلفيتها الخاصة (عن التطبيق/القفل)
@Composable
fun BrandHeader(size: Dp = 84.dp) {
    Box(
        Modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(R.drawable.ic_nota_badge), contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}
