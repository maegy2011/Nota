package com.nota.notes.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nota.notes.R

@Composable
fun SetPinScreen(onDone: (String) -> Unit, authVm: AuthViewModel? = null) {
    // مرحلتان بنفس اللوحة الواحدة: إدخال ثم تأكيد — بلا حقلين مكررين
    // + خطوة استرداد ثانية عند الإعداد الأول (مراجعة أكتوبر 2026 — Commit 2)
    var step by remember { mutableStateOf(0) } // 0 إدخال، 1 تأكيد، 2 استرداد
    var first by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var recMode by remember { mutableStateOf(0) } // 0 اختيار، 1 إنشاء مفتاح
    var riskAck by remember { mutableStateOf(false) }
    val mismatch = stringResource(R.string.pin_mismatch)
    val short = stringResource(R.string.pin_short)
    val ctx = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.setpin_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            stringResource(R.string.setpin_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        // تحذير لا استرداد عند الإعداد الأول — يُقرأ قبل الإدخال
        if (step == 0) {
            Text(
                stringResource(R.string.setpin_norecover_warn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        BrandHeader(size = 72.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(if (step == 0) R.string.setpin_first else R.string.setpin_confirm),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        // اللوحة الواحدة لكلا المرحلتين — بلا كيبورد نظام
        PinPad(
            pin = current,
            onPin = { if (it.length <= 8 && it.all(Char::isDigit)) { current = it; error = null } }
        )
        // تلميح الحد الأقصى: 8 أرقام — يذكّر بالسقف بلا كشف قواعد عند الفشل
        Text(
            stringResource(R.string.pin_hint_max8),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(
                error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Assertive }
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (step == 0) {
                    if (current.length < 6) error = short
                    else { first = current; current = ""; step = 1; error = null }
                } else {
                    error = when {
                        current.length < 6 -> short
                        current != first -> mismatch
                        else -> null
                    }
                    if (error == null) {
                        Haptics.confirm(ctx)
                        // الإعداد الأول (authVm موجود) → خطوة الاسترداد؛ التغيير من الإعدادات → حفظ مباشر
                        if (authVm != null) { step = 2 } else { onDone(current) }
                    }
                }
            },
            enabled = current.length >= 6,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(if (step == 0) R.string.verify_btn else R.string.setpin_done)) }
    }
    }
    // خطوة الاسترداد الثانية (Commit 2): بصمة / مفتاح استرداد / إقرار مخاطرة — تُعرض فوق نفس الشاشة
    if (step == 2 && authVm != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.reckey_choice_title)) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (recMode == 0) {
                        val bioOk = authVm.canBiometric()
                        Text(
                            if (bioOk) stringResource(R.string.reckey_choice_bio_ok)
                            else stringResource(R.string.reckey_choice_bio_no),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        androidx.compose.material3.OutlinedButton(
                            onClick = { onDone(first) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.reckey_choice_bio_btn)) }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { recMode = 1 },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) { Text(stringResource(R.string.reckey_choice_key_btn)) }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(checked = riskAck, onCheckedChange = { riskAck = it })
                            Text(
                                stringResource(R.string.reckey_choice_risk),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    } else {
                        RecoveryKeySetup(authVm = authVm, onDone = { onDone(first) }, onBack = { recMode = 0 })
                    }
                }
            },
            confirmButton = {
                if (recMode == 0) {
                    androidx.compose.material3.TextButton(onClick = { onDone(first) }, enabled = riskAck) {
                        Text(stringResource(R.string.reckey_choice_skip))
                    }
                }
            },
            dismissButton = {}
        )
    }
}
