package com.nota.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nota.notes.R
import com.nota.notes.security.RecoveryKey
import kotlinx.coroutines.launch

// إنشاء مفتاح الاسترداد والتحقق من حفظه (مراجعة أكتوبر 2026 — Commit 2):
// عرض مرة واحدة + نسخ (تعمية تلقائية مسؤولية النظام) + كويز 3 كلمات إلزامي —
// لا تخطي للتحقق: بلا نجاح لا يُعتمد المفتاح. يُخزَّن hash فقط.
@Composable
fun RecoveryKeySetup(
    authVm: AuthViewModel,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val words = remember { RecoveryKey.generate() }
    var phase by remember { mutableStateOf(0) } // 0 عرض، 1 كويز
    var copied by remember { mutableStateOf(false) }
    val quizIdx = remember { RecoveryKey.quizIndices() }
    var answers by remember { mutableStateOf(listOf("", "", "")) }
    var quizErr by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (phase == 0) {
            Text(
                stringResource(R.string.reckey_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Text(
                    stringResource(R.string.reckey_warn),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    words.mapIndexed { i, w -> "${i + 1}. $w" }.joinToString("   "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        copySensitive(ctx, words.joinToString(" "))
                        copied = true
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.reckey_copy)) }
                Button(onClick = { phase = 1; quizErr = false }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.reckey_verify_go))
                }
            }
            if (copied) {
                Text(
                    stringResource(R.string.reckey_copied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (onBack != null) {
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.back_cd))
                }
            }
        } else {
            Text(
                stringResource(R.string.reckey_quiz_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.reckey_quiz_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
            )
            quizIdx.forEachIndexed { qi, wi ->
                OutlinedTextField(
                    value = answers[qi],
                    onValueChange = { v ->
                        answers = answers.toMutableList().also { it[qi] = v.trim().lowercase() }
                        quizErr = false
                    },
                    label = { Text(stringResource(R.string.reckey_word_fmt, wi + 1)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            if (quizErr) {
                Text(
                    stringResource(R.string.reckey_quiz_fail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val ok = quizIdx.indices.all { answers[it] == words[quizIdx[it]] }
                    if (!ok) { quizErr = true; return@Button }
                    saving = true
                    authVm.saveRecoveryKey(words.joinToString(" ")) { saving = false; onDone() }
                },
                enabled = answers.all { it.isNotBlank() } && !saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.reckey_confirm)) }
            TextButton(onClick = { phase = 0; answers = listOf("", "", ""); quizErr = false }) {
                Text(stringResource(R.string.reckey_reshow))
            }
        }
        scope // إبقاء النطاق حيًا للاستدعاءات المستقبلية
    }
}

// التحقق من حفظ مفتاح الاسترداد (الإدارة — Commit 2): إدخال العبارة كاملة والتحقق من hash.
// انحراف موثق عن البرومبت (عرض 3 كلمات): مستحيل بلا تخزين الكلمات — والتحقق الكامل أدق.
// يُحتسب الفشل في الدرع (نفس عدّاد التخمين) — رسالة عامة فقط.
@Composable
fun RecVerifyDialog(authVm: AuthViewModel, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reckey_verify_btn)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.reckey_verify_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it; result = null },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                val r = result
                if (r != null) {
                    Text(
                        stringResource(if (r) R.string.reckey_ok else R.string.recover_generic_fail),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (r) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                            .semantics { liveRegion = LiveRegionMode.Assertive }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    busy = true
                    scope.launch {
                        result = authVm.verifyRecoveryKey(draft) is AuthViewModel.RecoveryResult.Success
                        busy = false
                    }
                },
                enabled = draft.isNotBlank() && !busy
            ) { Text(stringResource(R.string.verify_btn)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
