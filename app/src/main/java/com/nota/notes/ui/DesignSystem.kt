package com.nota.notes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nota.notes.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── لغة تصميم نوتا الموحدة Premium Warm (22 سبتمبر 2026) ───
// القواعد الثابتة لكل الشاشات المعاد تصميمها:
// - الزوايا من ثيم NotaTheme (small 12 / medium 20 / large 28 / XL 36) — البطاقات medium افتراضيًا
// - بطاقات الأقسام: سطح surfaceContainer + عنوان titleSmall بلون primary فوقها
// - صفوف التنقل: عنوان bodyLarge + وصف bodySmall + سهم + شارة قفل للمدفوعة
// - الحقول: زوايا 20dp صريحة (الافتراضي 4dp مرفوض)
// - اللمس: 48dp للأهداف المخصصة (نقاط الألوان/الفلاتر بغلاف خارجي)
// - الحشو: 16dp للشاشات — 20dp للشاشات الممررة الطويلة — 24dp لشاشات التمركز (قفل/PIN)
// - الأيقونات الاتجاهية: AutoMirrored دائمًا (RTL أولًا)

// تنسيق الحجم: 100M / 5G — رقم واحد عشري يكفي — مشترك بين شاشتي السحابة والحساب
internal fun fmtBytes(b: Long): String {
    if (b < 1024L * 1024L) return "%d KB".format(b / 1024L)
    val mb = b.toDouble() / (1024.0 * 1024.0)
    if (mb < 1024.0) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}

// بطاقة قسم: عنوان مميز + محتوى على سطح موحد — تُستخدم في الإعدادات والمتقدمة وكل قوائم الإدارة
// locked=true تعرض قفلًا بجوار العنوان (القفل المرئي 27 سبتمبر 2026)
@Composable
fun SectionCard(title: String, top: Dp = 20.dp, locked: Boolean = false, content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = top, bottom = 8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (locked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
    }
}

// حوار تأكيد موحد قبل أي إجراء أو تغيير (بأمر محمد 27 سبتمبر 2026): عنوان + رسالة + تأكيد/إلغاء بلغتي العرض
// danger=true يلوّن زر التأكيد بلون الخطأ (حذف/إتلاف) — وإلا بلون الأساسي
@Composable
fun ConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.confirm_title),
    confirmLabel: String = stringResource(R.string.confirm_do),
    danger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// صف تنقل موحد: عنوان + وصف اختياري + شارة قفل + سهم — لمس 48dp ضمنيًا (12dp عموديًا + ارتفاع السطر)
@Composable
fun NavRow(title: String, sub: String = "", locked: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (locked) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).padding(start = 6.dp)
                    )
                }
            }
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
