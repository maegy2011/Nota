package com.nota.notes.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.FileProvider
import com.nota.notes.R
import com.nota.notes.data.AccountDeletion
import com.nota.notes.data.GdprExportRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// P3-B — قسم الخصوصية (GDPR): تصدير ZIP + حذف الحساب بمهلة + opt-in التقارير/التحليلات.
// يُضمَّن في SettingsScreen — منطق نقي في GdprExport/AccountDeletion (يُختبر JVM)،
// والتشغيل الجهازي (ZIP/مشاركة/Firebase) هنا ويُوثَّق "ينتظر محمد" في التقرير.
@Composable
fun PrivacySection(
    authVm: AuthViewModel,
    crashOpt: Boolean,
    analyticsOpt: Boolean,
    onCrashOpt: (Boolean) -> Unit,
    onAnalyticsOpt: (Boolean) -> Unit,
    isPro: Boolean = true,
    exportRunner: GdprExportRunner? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val deletionAt by authVm.deletionAt.collectAsState()

    SectionCard(stringResource(R.string.privacy_title)) {
        // ── P3-B1: تصدير كل البيانات ──
        var exporting by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0f) }
        var exportMsg by remember { mutableStateOf<String?>(null) }
        // 6د: كلمة مرور اختيارية لتشفير التصدير — فارغة = ZIP مكشوفة (السلوك السابق).
        var exportPw by remember { mutableStateOf("") }
        Text(
            stringResource(R.string.privacy_export_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (exporting) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
        exportMsg?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        OutlinedTextField(
            value = exportPw,
            onValueChange = { exportPw = it.take(64); exportMsg = null },
            label = { Text(stringResource(R.string.privacy_export_pw_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        OutlinedButton(
            onClick = {
                val runner = exportRunner ?: return@OutlinedButton
                val pw = exportPw.takeIf { it.isNotBlank() }
                scope.launch {
                    exporting = true; progress = 0f; exportMsg = null
                    try {
                        val file = withContext(Dispatchers.IO) {
                            runner.export({ p -> scope.launch { progress = p } }, pw)
                        }
                        if (runner.isOversize(file)) {
                            exportMsg = ctx.getString(R.string.privacy_oversize_warn)
                        }
                        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
                        ctx.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = if (file.name.endsWith(".enc")) "application/octet-stream" else "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },
                            ctx.getString(R.string.privacy_export_btn)
                        ))
                        if (exportMsg == null) exportMsg = ctx.getString(R.string.privacy_export_ok)
                    } catch (e: Exception) {
                        exportMsg = ctx.getString(R.string.privacy_export_fail)
                    }
                    exporting = false; authVm.touch()
                }
            },
            enabled = !exporting && exportRunner != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(if (exporting) R.string.privacy_exporting else R.string.privacy_export_btn))
        }

        // ── P3-B2: حذف الحساب — بطاقة معلقة أو زر الحذف ──
        if (AccountDeletion.isPending(deletionAt)) {
            val days = AccountDeletion.remainingDays(deletionAt)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        ctx.getString(R.string.privacy_pending_fmt, days),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { authVm.cancelDeletion { authVm.touch() } }) {
                            Text(stringResource(R.string.privacy_cancel_delete))
                        }
                    }
                }
            }
        } else {
            var showD1 by remember { mutableStateOf(false) }
            var showD2 by remember { mutableStateOf(false) }
            var confirmText by remember { mutableStateOf("") }
            var delMsg by remember { mutableStateOf<String?>(null) }
            Text(
                stringResource(R.string.privacy_delete_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            delMsg?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            OutlinedButton(
                onClick = { showD1 = true; delMsg = null; confirmText = "" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.privacy_delete_btn),
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (showD1) {
                AlertDialog(
                    onDismissRequest = { showD1 = false },
                    title = { Text(stringResource(R.string.privacy_del_d1_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.privacy_del_d1_msg))
                            if (isPro) {
                                Text(
                                    stringResource(R.string.privacy_del_pro_warn),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showD1 = false; showD2 = true }) {
                            Text(stringResource(R.string.recover_wipe_next))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showD1 = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
            if (showD2) {
                AlertDialog(
                    onDismissRequest = { showD2 = false },
                    title = { Text(stringResource(R.string.privacy_del_d2_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.privacy_del_d2_hint))
                            OutlinedTextField(
                                value = confirmText,
                                onValueChange = { confirmText = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (!AccountDeletion.isConfirmValid(confirmText)) {
                                    delMsg = ctx.getString(R.string.privacy_del_d2_hint)
                                    return@TextButton
                                }
                                showD2 = false
                                scope.launch {
                                    // 1) Firebase (يحتاج جلسة وشبكة) — الفشل لا يوقف المحلي.
                                    val fbOk = withContext(Dispatchers.IO) {
                                        authVm.performFirebaseDelete()
                                    }
                                    // 2) السحابة server-side غير مدعومة (موثق) — رسالة واضحة.
                                    // 3) تسجيل الطلب + مهلة 14 يومًا (الحذف المحلي النهائي عند انتهائها).
                                    authVm.requestDeletion { authVm.touch() }
                                    delMsg = if (fbOk) ctx.getString(R.string.privacy_deleted_local)
                                    else ctx.getString(R.string.privacy_cloud_note)
                                }
                            },
                            enabled = AccountDeletion.isConfirmValid(confirmText)
                        ) {
                            Text(
                                stringResource(R.string.privacy_del_confirm),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showD2 = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
        }

        // ── P3-B3: opt-in صريح — معطّل افتراضيًا ──
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.privacy_crash_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.privacy_crash_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = crashOpt, onCheckedChange = onCrashOpt)
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.privacy_analytics_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.privacy_analytics_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = analyticsOpt, onCheckedChange = onAnalyticsOpt)
        }
        Text(
            stringResource(R.string.privacy_ai_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
