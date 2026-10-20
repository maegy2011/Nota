package com.nota.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import kotlinx.coroutines.launch

/**
 * سلسلة fallback للاسترداد (موثقة بأمر مراجعة أكتوبر 2026):
 * PIN fails → biometric (if enrolled) → recovery key (if set)
 * → device credential (if enabled) → full app reset (last resort).
 * الدرع لا يُصفَّر عبر أي من هذه المسارات — محاولات إعادة الضبط الفاشلة
 * تُحتسب في عدّاد التخمين نفسه.
 */
// مسارات الاسترداد المتاحة فقط — لا خيار «قريبًا» ولا «غير متاح» (نمط compose-lock-screens:
// بطاقات tonal، نصوص دافئة، لا أحمر عاري، البصمة داخل اللوحة)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverySheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    canBio: Boolean,
    bioEnrolled: Boolean,
    hasRecoveryKey: Boolean,
    deviceCredEnabled: Boolean,
    onBio: () -> Unit,
    onRecoveryKey: (String) -> Unit,
    recoveryError: String?,
    onDeviceCred: () -> Unit,
    onFactoryReset: () -> Unit,
    newPinContent: @Composable (() -> Unit)? = null
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val clipCtx = LocalContext.current
    var showKeyInput by remember { mutableStateOf(false) }
    var keyDraft by remember { mutableStateOf("") }
    var wipeStep by remember { mutableStateOf(0) } // 0 لا شيء، 1 حوار أول، 2 تأكيد كتابي
    var wipeWord by remember { mutableStateOf("") }
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // مقياس خط 200% (Commit 5د): تمرير داخلي — بلا overflow على الشاشات الصغيرة
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (newPinContent != null) {
                newPinContent()
                return@ModalBottomSheet
            }
            Text(
                stringResource(R.string.recover_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }
            )
            Spacer(Modifier.height(16.dp))
            if (canBio) {
                RecoveryOption(
                    icon = { Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.recover_bio),
                    sub = if (!bioEnrolled) stringResource(R.string.recover_bio_unenrolled) else null,
                    enabled = bioEnrolled,
                    onClick = onBio
                )
            }
            if (hasRecoveryKey) {
                if (!showKeyInput) {
                    RecoveryOption(
                        icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = stringResource(R.string.recover_key),
                        sub = null,
                        enabled = true,
                        onClick = { showKeyInput = true }
                    )
                } else {
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        label = { Text(stringResource(R.string.recover_key_hint)) },
                        singleLine = false,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (recoveryError != null) {
                        Text(
                            recoveryError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                                .semantics { liveRegion = LiveRegionMode.Assertive }
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRecoveryKey(keyDraft.trim()) }, enabled = keyDraft.isNotBlank(), modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.verify_btn))
                        }
                        TextButton(onClick = {
                            runCatching {
                                val cm = clipCtx.getSystemService(android.content.ClipboardManager::class.java)
                                val t = cm?.primaryClip?.getItemAt(0)?.coerceToText(clipCtx)?.toString()
                                if (!t.isNullOrBlank()) keyDraft = t
                            }
                        }) {
                            Text(stringResource(R.string.recover_paste))
                        }
                        TextButton(onClick = { showKeyInput = false; keyDraft = "" }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
            if (deviceCredEnabled) {
                RecoveryOption(
                    icon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.recover_device),
                    sub = null,
                    enabled = true,
                    onClick = onDeviceCred
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            stringResource(R.string.recover_wipe),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        stringResource(R.string.recover_wipe_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    OutlinedButton(onClick = { wipeStep = 1 }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.recover_wipe))
                    }
                }
            }
        }
    }
    if (wipeStep == 1) {
        AlertDialog(
            onDismissRequest = { wipeStep = 0 },
            title = { Text(stringResource(R.string.recover_wipe_confirm1)) },
            text = { Text(stringResource(R.string.recover_wipe_confirm1_msg)) },
            confirmButton = { TextButton(onClick = { wipeStep = 2 }) { Text(stringResource(R.string.recover_wipe_next)) } },
            dismissButton = { TextButton(onClick = { wipeStep = 0 }) { Text(stringResource(R.string.cancel)) } }
        )
    } else if (wipeStep == 2) {
        val expect = stringResource(R.string.recover_wipe_word)
        AlertDialog(
            onDismissRequest = { wipeStep = 0; wipeWord = "" },
            title = { Text(stringResource(R.string.recover_wipe_confirm2)) },
            text = {
                Column {
                    Text(stringResource(R.string.recover_wipe_confirm2_msg, expect))
                    OutlinedTextField(
                        value = wipeWord,
                        onValueChange = { wipeWord = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { wipeStep = 0; wipeWord = ""; close(); onFactoryReset() },
                    enabled = wipeWord.trim() == expect
                ) { Text(stringResource(R.string.recover_wipe_confirm)) }
            },
            dismissButton = { TextButton(onClick = { wipeStep = 0; wipeWord = "" }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun RecoveryOption(
    icon: @Composable () -> Unit,
    title: String,
    sub: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = if (enabled) onClick else { {} }
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            icon()
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (sub != null) {
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!enabled) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
