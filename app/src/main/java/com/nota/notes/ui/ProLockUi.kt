package com.nota.notes.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nota.notes.R

// قاعدة القفل المرئي (27 سبتمبر 2026 — بأمر محمد): المحتوى المدفوع يُعرض دائمًا،
// وكل خيار مدفوع معطّل وبجواره قفل — الضغطة تقود للترقية بدل الإخفاء

// بانر علوي موحد للشاشات المقفلة: قفل + شرح + زر ترقية
@Composable
fun ProLockedBanner(onUpgrade: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.pro_only),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = onUpgrade) {
                Text(stringResource(R.string.pro_upgrade_btn))
            }
        }
    }
}

// زر أيقونة مقفول: الأيقونة الأصلية باهتة + شارة قفل — الضغطة تقود للترقية
@Composable
fun LockedIconButton(
    icon: ImageVector,
    desc: String,
    onLocked: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(onClick = onLocked) {
        androidx.compose.foundation.layout.Box {
            Icon(icon, contentDescription = desc, tint = tint.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp).align(Alignment.BottomEnd)
            )
        }
    }
}
