package com.nota.notes.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// صورة الحساب: ملف داخلي واحد يُحمَّل عند الدخول ويُمسح عند الخروج — عرض أوفلاين دائمًا
fun accountPhotoFile(ctx: Context): File = File(ctx.filesDir, "account_photo.jpg")

// أفاتار الحساب الموحد: صورة المستخدم الدائرية — أو الحرف الأول — أو أيقونة عامة لغير المسجل
// يُستخدم في الرئيسية وصف الإعدادات وشاشة الحساب — نفس الشكل في كل مكان
@Composable
fun AccountAvatar(
    accountId: String?,
    label: String? = null,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current.applicationContext
    var bmp by remember(accountId) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(accountId) {
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                val f = accountPhotoFile(ctx)
                if (accountId != null && f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            }.getOrNull()
        }
    }
    val bit = bmp
    if (bit != null) {
        Image(
            bitmap = bit.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.size(size).clip(CircleShape)
        )
    } else if (!label.isNullOrBlank()) {
        Box(
            modifier = modifier.size(size).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label.trim().first().uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    } else {
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(size)
        )
    }
}
