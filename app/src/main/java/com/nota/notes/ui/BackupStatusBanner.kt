package com.nota.notes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nota.notes.R
import kotlinx.coroutines.delay

// Phase 2د: بانر حالة مضمّن لكل بطاقات النسخ — Idle = لا شيء، Running = شريط تقدم،
// Success/Failed = نتيجة + زر إعادة عند الفشل — يختفي تلقائيًا بعد 10 ثوانٍ.
@Composable
fun BackupOpBanner(
    running: Boolean,
    pct: Int?,
    successText: String?,
    failText: String?,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit = {}
) {
    if (!running && successText == null && failText == null) return
    // الاختفاء التلقائي بعد 10 ثوانٍ للنجاح/الفشل فقط — الجاري يبقى حتى ينتهي.
    if (!running && (successText != null || failText != null)) {
        LaunchedEffect(successText, failText) {
            delay(10_000)
            onDismiss()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                failText != null -> MaterialTheme.colorScheme.errorContainer
                successText != null -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            when {
                running -> {
                    Text(
                        if (pct != null) stringResource(R.string.backup_status_running_fmt, pct)
                        else stringResource(R.string.backup_status_running),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { (pct ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                successText != null -> {
                    Text(successText, style = MaterialTheme.typography.bodyMedium)
                }
                failText != null -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            failText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        if (onRetry != null) {
                            TextButton(onClick = onRetry) {
                                Text(stringResource(R.string.rec_retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Phase 2ج: حوار الحجم الكبير (Drive) — حوار تأكيد لا رفض.
@Composable
fun SizeWarnDialog(
    sizeText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_too_large_fmt, sizeText)) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.confirm_do)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// Phase 2ج: حوار الحصة (سحابة نوتا) — تحذير 50% حوار، وتجاوز رفض برسالة (لا حوار).
@Composable
fun QuotaWarnDialog(
    sizeText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quota_warn_fmt, sizeText)) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.confirm_do)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
