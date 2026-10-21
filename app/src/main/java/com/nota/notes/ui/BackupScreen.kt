package com.nota.notes.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import com.nota.notes.data.SettingsStore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nota.notes.R
import com.nota.notes.data.BackupManager
import com.nota.notes.data.CloudPointer
import com.nota.notes.data.CloudQuota
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// نسخ حساس (مستوى بنكي): EXTRA_IS_SENSITIVE (33+) يحجب النص عن اقتراحات لوحات المفاتيح
// ومعاينات النظام — لعبارة الاسترداد وتفاصيل الأخطاء. دونها أي تطبيق أمامي قد يلتقطها.
fun copySensitive(ctx: android.content.Context, text: String) {
    try {
        val cm = ctx.getSystemService(android.content.ClipboardManager::class.java) ?: return
        val clip = android.content.ClipData.newPlainText("nota", text)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            clip.description.extras = android.os.PersistableBundle().apply {
                putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        cm.setPrimaryClip(clip)
        // P3-A2: تعمية تلقائية بعد دقيقة — لصق مفتاح الاسترداد ثم نسيانه في الحافظة
        // خطر صامت. يُمسح أي محتوى لاحق أيضًا — موثق في نص التنبيه (reckey_copied).
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { cm.clearPrimaryClip() }
            }, 60_000L)
        }
    } catch (e: Exception) {
        // الفشل الصامت هنا مقصود — النسخ راحة لا مسار أمان
    }
}

// شاشة النسخ الاحتياطي الموحدة (من القائمة الجانبية فقط — بلا ارتباط بالإعدادات):
// Drive اليومي + سحابة نوتا + مفتاح السحابة (عرض/حفظ ملف/استيراد) + ملف .nota المشفر
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onSignInNeeded: () -> Unit,
    notesVm: NotesViewModel,
    authVm: AuthViewModel,
    vm: CloudBackupViewModel = hiltViewModel(),
    ncvm: NotaCloudViewModel = hiltViewModel(),
    premiumVm: PremiumViewModel = hiltViewModel(),
    // القفل المرئي (27 سبتمبر 2026): المحتوى معروض — كل إجراء مدفوع معطّل مع قفل
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    val signedIn by vm.signedIn.collectAsStateWithLifecycle()
    val last by vm.lastBackup.collectAsStateWithLifecycle()
    val freq by vm.autoFreq.collectAsStateWithLifecycle()
    val working by vm.working.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val consent by vm.consentIntent.collectAsStateWithLifecycle()
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.retryConsented()
    }
    val ncQuota by ncvm.quota.collectAsStateWithLifecycle()
    val ncPointer by ncvm.pointer.collectAsStateWithLifecycle()
    val ncWorking by ncvm.working.collectAsStateWithLifecycle()
    val ncMsg by ncvm.message.collectAsStateWithLifecycle()
    val ncDetail by ncvm.detail.collectAsStateWithLifecycle()
    // ncPro excludes trial TEMPORARILY — media upload to cloud requires trust beyond trial.
    // Temporary exception per product decision (Phase 0) — Phase 1 closes it: Trial will open ncPro.
    // See: product decision documented in nota-phase0-status.md
    // المرحلة 1ب-2 (T3 مغلق بقرار محمد): Trial يفتح ncPro الآن — نفس تعريف isPro الموحد.
    val ncPro = premiumVm.isPro()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(signedIn) {
        if (signedIn) ncvm.refresh()
    }
    val pendingConsent = consent
    LaunchedEffect(pendingConsent) {
        if (pendingConsent != null) {
            consentLauncher.launch(pendingConsent)
            vm.consumeConsent()
        }
    }
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    // P3-C2: طي الطرق + روابط سفلية + شيت الباقات
    var showLog by remember { mutableStateOf(false) }
    var driveAdv by remember { mutableStateOf(false) }
    var proSheet by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            // داخل DrawerShell: الهيدر المثبت من الغلاف — بلا شريط داخلي (27 سبتمبر 2026)
            contentWindowInsets = WindowInsets(0)
        ) { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.backup_screen_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // القفل المرئي: البانر يفتح شيت «تعرّف على الباقات» بدل قفل مباشر (P3-C2)
                if (locked) {
                    ProLockedBanner { proSheet = true }
                }
                if (proSheet) {
                    ModalBottomSheet(onDismissRequest = { proSheet = false }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                stringResource(R.string.bk_pro_title),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                stringResource(R.string.bk_pro_msg),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Button(onClick = { proSheet = false; onLocked() }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.paywall_upgrade))
                            }
                            TextButton(onClick = { proSheet = false }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }
                if (!signedIn) {
                    SectionCard(stringResource(R.string.cloud_sec_status), top = 0.dp) {
                        Text(
                            stringResource(R.string.cloud_signin_needed),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(onClick = onSignInNeeded, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.cloud_signin_btn))
                        }
                    }
                } else {
                    // P3-C2: بطاقة ملخص + 3 طرق مصغرة قابلة للطي + روابط سفلية
                    BackupSummaryCard(
                        last, ncPointer?.updatedAt ?: 0L, fmt,
                        working || ncWorking, { vm.backupNow() }, locked, onLocked
                    )
                    MethodSection(stringResource(R.string.bk_method_drive)) {
                        DriveCard(vm, last, freq, working, msg, fmt, locked, onLocked, advOpen = driveAdv, onAdvToggle = { driveAdv = !driveAdv })
                    }
                    MethodSection(stringResource(R.string.bk_method_ncloud)) {
                        NotaCloudCard(ncvm, ncQuota, ncPointer, ncWorking, ncMsg, ncDetail, ncPro, working, fmt, clipboard, locked, onLocked)
                        if (ncPro) {
                            CloudKeyCard(ncvm, ncWorking, working, clipboard, locked, onLocked)
                        }
                    }
                    MethodSection(stringResource(R.string.bk_method_file)) {
                        FileBackupCard(notesVm, authVm, fmt, locked, onLocked)
                    }
                    // روابط سفلية: سجل الاستعادة + إعدادات Drive المتقدمة
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showLog = !showLog }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.bk_links_log))
                        }
                        TextButton(onClick = { driveAdv = !driveAdv }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.bk_links_adv))
                        }
                    }
                    if (showLog) {
                        RestoreLogCard(notesVm, authVm, fmt, locked, onLocked)
                    }
                }
            }
        }
    }
}

// P3-C2: بطاقة ملخص الحالة — أحدث نسخة (Drive/سحابة) + زر «نسخ الآن»
@Composable
private fun BackupSummaryCard(
    lastDrive: Long,
    lastCloud: Long,
    fmt: SimpleDateFormat,
    busy: Boolean,
    onBackupNow: () -> Unit,
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    val best = maxOf(lastDrive, lastCloud)
    SectionCard(stringResource(R.string.bk_sum_title), top = 0.dp, locked = locked) {
        Text(
            if (best > 0) stringResource(
                R.string.bk_sum_fmt,
                fmt.format(Date(best)),
                stringResource(if (lastDrive >= lastCloud) R.string.bk_log_src_drive else R.string.bk_log_src_ncloud)
            )
            else stringResource(R.string.bk_sum_none),
            style = MaterialTheme.typography.titleMedium
        )
        Button(
            onClick = onBackupNow,
            enabled = !busy && !locked,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.cloud_backup_now))
        }
    }
}

// P3-C2: قسم طريقة قابل للطي — مفتوح افتراضيًا (الطي يخفي الحقول مؤقتًا)
@Composable
private fun MethodSection(title: String, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf(true) }
    Row(
        Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { open = !open }) { Text(if (open) "−" else "+") }
    }
    if (open) content()
}

// 1) نسخة Drive اليومية: الحالة + نسخ/استعادة + مفتاح التلقائي
@Composable
private fun DriveCard(
    vm: CloudBackupViewModel,
    last: Long,
    freq: String,
    working: Boolean,
    msg: String?,
    fmt: SimpleDateFormat,
    locked: Boolean = false,
    onLocked: () -> Unit = {},
    // P3-C2: التكرار/عدد النسخ/كلمة المرور مطوية في «إعدادات متقدمة»
    advOpen: Boolean = false,
    onAdvToggle: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val keep by vm.keepCount.collectAsStateWithLifecycle()
    val cloudPreview by vm.preview.collectAsStateWithLifecycle()
    val shrinkPct by vm.shrinkWarn.collectAsStateWithLifecycle()
    val pwSet by vm.drivePwSet.collectAsStateWithLifecycle()
    val pwNeeded by vm.pwNeeded.collectAsStateWithLifecycle()
    var newPw by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var newPw2 by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var manualPw by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    SectionCard(stringResource(R.string.cloud_sec_status), top = 0.dp, locked = locked) {
        Text(
            if (last > 0) stringResource(R.string.cloud_last_fmt, fmt.format(Date(last)))
            else stringResource(R.string.cloud_never),
            style = MaterialTheme.typography.titleMedium
        )
        val m = msg
        if (m != null) {
            Text(
                text = when {
                    m == "ok-backup" -> stringResource(R.string.cloud_ok_backup)
                    m == "ok-backup-same" -> stringResource(R.string.cloud_ok_backup_same)
                    m == "err:empty-guard" -> stringResource(R.string.cloud_empty_guard)
                    m.startsWith("ok-restore:") -> {
                        val parts = m.removePrefix("ok-restore:").split(":")
                        stringResource(
                            R.string.cloud_ok_restore,
                            parts.getOrNull(0)?.toIntOrNull() ?: 0,
                            parts.getOrNull(1)?.toIntOrNull() ?: 0
                        )
                    }
                    m == "err:no-drive-pw" -> stringResource(R.string.cloud_no_drive_pw)
                    m == "err:drive-pw-weak" -> stringResource(R.string.nota_password_weak)
                    m == "err:drive-pw-wrong" -> stringResource(R.string.cloud_drive_pw_wrong)
                    m == "err:verify-failed" -> stringResource(R.string.cloud_verify_failed)
                    m == "err:consent-denied" -> stringResource(R.string.cloud_consent_denied)
                    m == "err:no-backup" -> stringResource(R.string.cloud_no_backup)
                    else -> stringResource(R.string.cloud_failed)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (m.startsWith("err") || m == "err") MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
    SectionCard(stringResource(R.string.cloud_sec_actions), locked = locked) {
        Button(onClick = { vm.backupNow() }, enabled = !working && !locked, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (working) R.string.cloud_working else R.string.cloud_backup_now))
        }
        OutlinedButton(onClick = { vm.restoreNow() }, enabled = !working && !locked, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cloud_restore))
        }
    }
    // P3-C2: التكرار وعدد النسخ وكلمة المرور في المتقدمة — الزر الرئيسي ظاهر دائمًا
    TextButton(onClick = onAdvToggle, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(if (advOpen) R.string.bk_adv_hide else R.string.bk_adv_show))
    }
    if (advOpen) {
        // تكرار النسخ المجدول (27 سبتمبر 2026): إيقاف/يومي فجرًا/أسبوعي — الاختيار يجدول المنبه فورًا + إشعار بالنتيجة

        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(stringResource(R.string.cloud_auto), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.cloud_auto_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FreqOption(value = SettingsStore.FREQ_OFF, label = stringResource(R.string.cloud_freq_off), selected = freq, onPick = { v -> vm.setFreq(v) }, locked = locked, onLocked = onLocked)
            FreqOption(value = SettingsStore.FREQ_DAILY, label = stringResource(R.string.cloud_freq_daily), selected = freq, onPick = { v -> vm.setFreq(v) }, locked = locked, onLocked = onLocked)
            FreqOption(value = SettingsStore.FREQ_WEEKLY, label = stringResource(R.string.cloud_freq_weekly), selected = freq, onPick = { v -> vm.setFreq(v) }, locked = locked, onLocked = onLocked)
        }
        // عدد النسخ المحتفظ بها 1..7 — الأقدم يُستبدل عند التجاوز (27 سبتمبر 2026)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.cloud_keep_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.cloud_keep_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { vm.setKeepCount(keep - 1) }, enabled = !working && keep > 1 && !locked) { Text("−") }
            Text(
                stringResource(R.string.cloud_keep_fmt, keep),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            TextButton(onClick = { vm.setKeepCount(keep + 1) }, enabled = !working && keep < SettingsStore.MAX_CLOUD_KEEP && !locked) { Text("+") }
        }
    // كلمة مرور Drive الثابتة (27 سبتمبر 2026): تُشفر بها كل النسخ تلقائيًا — وتُطلب أسبوعيًا للتأكيد
    SectionCard(stringResource(R.string.cloud_pw_title), locked = locked) {
        if (!pwSet) {
            Text(
                stringResource(R.string.cloud_pw_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                enabled = !locked,
                    shape = RoundedCornerShape(20.dp),
                    value = newPw, onValueChange = { newPw = it },
                    label = { Text(stringResource(R.string.cloud_pw_hint)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                OutlinedTextField(
                enabled = !locked,
                    shape = RoundedCornerShape(20.dp),
                    value = newPw2, onValueChange = { newPw2 = it },
                    label = { Text(stringResource(R.string.cloud_pw_hint2)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
            // P1-B fix 2: حد أدنى 8 أحرف + فحص قوة (BackupManager.isStrongPassword — بلا تكرار) + نفس مؤشر القوة.
            val driveStrong = newPw.length >= com.nota.notes.data.BackupManager.MIN_PASSWORD_LEN && com.nota.notes.data.BackupManager.isStrongPassword(newPw)
            val driveErr = when {
                newPw.isNotEmpty() && newPw.length < com.nota.notes.data.BackupManager.MIN_PASSWORD_LEN -> stringResource(R.string.nota_password_too_short)
                newPw.length >= com.nota.notes.data.BackupManager.MIN_PASSWORD_LEN && !com.nota.notes.data.BackupManager.isStrongPassword(newPw) -> stringResource(R.string.nota_password_weak)
                newPw2.isNotEmpty() && newPw != newPw2 -> stringResource(R.string.bk_pw_mismatch)
                else -> null
            }
            if (driveErr != null) {
                Text(
                    driveErr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (newPw.isNotEmpty()) {
                PasswordStrengthIndicator(level = com.nota.notes.data.BackupPassword.strengthLevel(newPw))
            }
            Text(
                stringResource(R.string.nota_password_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            OutlinedButton(
                onClick = {
                    val sug = com.nota.notes.data.BackupPassword.generateDicewarePassword()
                    newPw = sug; newPw2 = sug
                    copySensitive(ctx, sug)
                },
                enabled = !working && !locked,
                modifier = Modifier.padding(top = 4.dp)
            ) { Text(stringResource(R.string.nota_password_suggest)) }
            Button(
                onClick = { vm.setDrivePassword(newPw); newPw = ""; newPw2 = "" },
                enabled = !working && newPw.length >= com.nota.notes.data.BackupManager.MIN_PASSWORD_LEN && driveStrong && newPw == newPw2 && !locked,
                modifier = Modifier.padding(top = 8.dp)
            ) { Text(stringResource(R.string.cloud_pw_save)) }
        } else {
            Text(
                stringResource(R.string.cloud_pw_set),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedButton(onClick = { vm.clearDrivePassword() }, enabled = !working) {
                Text(stringResource(R.string.cloud_pw_remove))
            }
        }
        // فك يدوي: الغلاف لا يُفك بالمخزنة (جهاز جديد/نسيان) — إدخال لمرة واحدة للاستعادة
        if (pwNeeded) {
            Text(
                stringResource(R.string.cloud_pw_needed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                enabled = !locked,
                    shape = RoundedCornerShape(20.dp),
                    value = manualPw, onValueChange = { manualPw = it },
                    label = { Text(stringResource(R.string.cloud_pw_manual_hint)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
            Button(
                onClick = { vm.restoreWithPassword(manualPw); manualPw = "" },
                enabled = !working && manualPw.isNotEmpty() && !locked,
                modifier = Modifier.padding(top = 8.dp)
            ) { Text(stringResource(R.string.cloud_pw_manual_btn)) }
        }
    }
    }
    // سؤال الانكماش: الجديدة أصغر من نصف السابقة — متابعة أم إلغاء (27 سبتمبر 2026)
    shrinkPct?.let { pct ->
        AlertDialog(
            onDismissRequest = { vm.clearShrink() },
            title = { Text(stringResource(R.string.cloud_shrink_title)) },
            text = { Text(stringResource(R.string.cloud_shrink_msg, pct)) },
            confirmButton = {
                TextButton(onClick = { vm.confirmShrink() }) {
                    Text(stringResource(R.string.cloud_shrink_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.clearShrink() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    // معاينة نسخة Drive قبل الدمج — نفس حوار نسخة الملف + تحذير القدم (27 سبتمبر 2026)
    // P3-3ج: اختيار جزئي — checkbox لكل ملاحظة (الكل محددة افتراضيًا).
    cloudPreview?.let { pv ->
        val pvNotes = remember(pv) { vm.previewNotes() }
        var sel by remember(pv) { mutableStateOf(pvNotes.map { it.id }.toSet()) }
        AlertDialog(
            onDismissRequest = { vm.clearPreview() },
            title = { Text(stringResource(R.string.bk_preview_title)) },
            text = {
                Column {
                    Text(
                        stringResource(
                            R.string.bk_preview_msg,
                            pv.meta.count,
                            if (pv.meta.createdAt > 0) fmt.format(Date(pv.meta.createdAt)) else "—"
                        )
                    )
                    if (pv.stale) {
                        Text(
                            stringResource(R.string.bk_preview_stale),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (pvNotes.isNotEmpty()) {
                        BackupNoteSelectionList(
                            notes = pvNotes, selected = sel,
                            onToggle = { id -> sel = if (id in sel) sel - id else sel + id },
                            onSelectAll = { sel = pvNotes.map { it.id }.toSet() },
                            onDeselectAll = { sel = emptySet() }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { val s = sel; vm.confirmRestore(s) }) {
                    Text(stringResource(R.string.bk_preview_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.clearPreview() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// 2) سحابة نوتا: الحصة + آخر نسخة + نسخ/استعادة + رسائل واضحة
@Composable
private fun NotaCloudCard(
    ncvm: NotaCloudViewModel,
    ncQuota: CloudQuota?,
    ncPointer: CloudPointer?,
    ncWorking: Boolean,
    ncMsg: String?,
    ncDetail: String?,
    ncPro: Boolean,
    working: Boolean,
    fmt: SimpleDateFormat,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    val ctx = LocalContext.current
    SectionCard(stringResource(R.string.ncloud_title), locked = locked) {
        Text(
            stringResource(R.string.ncloud_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val q = ncQuota
        if (q == null) {
            Text(
                stringResource(R.string.ncloud_loading),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            val frac = if (q.quotaBytes > 0) (q.usedBytes.toFloat() / q.quotaBytes.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
            Text(
                stringResource(R.string.ncloud_used_fmt, fmtBytes(q.usedBytes), fmtBytes(q.quotaBytes)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        val np = ncPointer
        Text(
            if (np != null && np.updatedAt > 0) stringResource(R.string.ncloud_last_fmt, fmt.format(Date(np.updatedAt)))
            else stringResource(R.string.ncloud_never),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = {
            ncvm.backupNow(ncPro)
        }, enabled = !ncWorking && !working && !locked, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (ncWorking) R.string.cloud_working else R.string.ncloud_backup_now))
        }
        if (!ncPro) {
            Text(
                stringResource(R.string.ncloud_media_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        OutlinedButton(onClick = {
            ncvm.restoreNow()
        }, enabled = !ncWorking && !working && !locked, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ncloud_restore))
        }
        val nm = ncMsg
        if (nm != null) {
            val isErr = nm.startsWith("nc-err") || nm == "nc-err"
            Text(
                text = when {
                    nm == "nc-ok-backup" -> stringResource(R.string.ncloud_ok_backup)
                    nm.startsWith("nc-ok-restore:") -> {
                        val parts = nm.removePrefix("nc-ok-restore:").split(":")
                        stringResource(
                            R.string.ncloud_ok_restore,
                            parts.getOrNull(0)?.toIntOrNull() ?: 0,
                            parts.getOrNull(1)?.toIntOrNull() ?: 0
                        )
                    }
                    nm == "nc-err-quota" -> stringResource(R.string.ncloud_err_quota)
                    // المرحلة 1ج: رفض رفع الوسائط لغير المشتركين (فحص داخل الـ VM)
                    nm == "nc-err-pro-required" -> stringResource(R.string.ncloud_err_pro_required)
                    nm == "nc-err-nokey" -> stringResource(R.string.ncloud_err_nokey)
                    nm == "nc-err-nokey-upload" -> stringResource(R.string.ncloud_err_nokey_upload)
                    nm == "nc-err-decrypt" -> stringResource(R.string.ncloud_err_decrypt)
                    nm == "nc-err-nobackup" -> stringResource(R.string.ncloud_err_nobackup)
                    nm == "nc-err:AUTH_EXPIRED" -> stringResource(R.string.ncloud_err_auth)
                    else -> stringResource(R.string.ncloud_failed)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isErr) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            // P0-2 (أكتوبر 2026): بطاقة توجيه عند رفض الرفع بلا مفتاح — الزر في بطاقة المفتاح أدناه.
            if (nm == "nc-err-nokey-upload") {
                Text(
                    stringResource(R.string.ncloud_nokey_guide),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            val ndParts = ncDetail?.takeIf { it.startsWith("media:") }?.split(":")
            val ndMedia = ndParts?.getOrNull(1)?.toIntOrNull() ?: 0
            if (ndMedia > 0) {
                Text(
                    stringResource(
                        if (ndParts?.getOrNull(3) == "backup") R.string.ncloud_media_nokey_backup_fmt
                        else R.string.ncloud_media_nokey_fmt,
                        ndMedia
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            val nd = ncDetail
            if (isErr && nd != null) {
                SelectionContainer(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = nd,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { copySensitive(ctx, nd) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(stringResource(R.string.ncloud_copy))
                }
            }
        }
    }
}

// 3) مفتاح السحابة الخاص: عرض + حفظ في ملف + نسخ + استيراد — داخل الشاشة لا خارجها
@Composable
private fun CloudKeyCard(
    ncvm: NotaCloudViewModel,
    ncWorking: Boolean,
    working: Boolean,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val keyScope = rememberCoroutineScope()
    var cloudPhrase by remember { mutableStateOf<String?>(null) }
    var importText by remember { mutableStateOf("") }
    var importMsg by remember { mutableStateOf<String?>(null) }
    var saveMsg by remember { mutableStateOf<String?>(null) }
    // حفظ العبارة في ملف نصي يختاره المستخدم — التنزيل الحقيقي للمفتاح خارج الجهاز
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val phrase = cloudPhrase
        if (uri != null && phrase != null) {
            keyScope.launch(Dispatchers.IO) {
                val ok = runCatching {
                    ctx.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(phrase.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("no-stream")
                }.isSuccess
                withContext(Dispatchers.Main) {
                    saveMsg = if (ok) "ok" else "bad"
                }
            }
        }
    }
    // كشف المفتاح: تعمية دائمة + عين تكشف 30 ثانية (قرار 26 سبتمبر 2026)
    var revealed by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(0) }
    // استيراد المفتاح من ملف نصي — بجانب اللصق اليدوي (قرار 26 سبتمبر 2026)
    val importFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        keyScope.launch {
            val tt = runCatching {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?.trim()?.take(500)
                }
            }.getOrNull()
            if (tt.isNullOrBlank()) { importMsg = "bad"; return@launch }
            val ok = ncvm.importCloudKey(tt)
            importMsg = if (ok) "ok" else "bad"
            if (ok) { importText = ""; cloudPhrase = ncvm.exportCloudKey(); saveMsg = null }
        }
    }
    // حماية المفتاح: منع اللقطات داخل البطاقة حتى مع سماح المستخدم العام — يُستعاد السابق عند الخروج
    DisposableEffect(Unit) {
        val win = (ctx as? android.app.Activity)?.window
        if (win == null) return@DisposableEffect onDispose {}
        val had = (win.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE) != 0
        win.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!had) win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
    // عدّاد الإظهار: 30 ثانية ثم تعمية تلقائية — مفتاح جديد يعيد العدّاد
    LaunchedEffect(revealed, cloudPhrase) {
        if (revealed) {
            for (i in 30 downTo 1) { secondsLeft = i; delay(1000) }
            revealed = false
        }
    }
    SectionCard(stringResource(R.string.ncloud_key_title), locked = locked) {
        Text(
            stringResource(R.string.ncloud_key_msg),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
        )
        Text(
            stringResource(R.string.ncloud_key_noshots),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            stringResource(R.string.ncloud_key_view),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { keyScope.launch { cloudPhrase = ncvm.exportCloudKey(); saveMsg = null } },
                enabled = !ncWorking && !working && !locked,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.ncloud_key_show)) }
            OutlinedButton(
                onClick = {
                    keyScope.launch {
                        cloudPhrase = ncvm.createCloudKey()
                        importMsg = null; saveMsg = null
                    }
                },
                enabled = !ncWorking && !working && !locked,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.ncloud_key_new)) }
        }
        val phrase = cloudPhrase
        if (phrase != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    if (revealed) {
                        SelectionContainer {
                            Text(
                                text = phrase,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // تعمية دائمة: النص موجود لكن غير مقروء ولا يُنسخ بالتحديد
                        Text(
                            text = phrase,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.blur(14.dp)
                        )
                    }
                }
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (revealed) R.string.ncloud_key_hide else R.string.ncloud_key_reveal
                        ),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (revealed) {
                Text(
                    stringResource(R.string.ncloud_key_visible_fmt, secondsLeft),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                OutlinedButton(
                    onClick = { copySensitive(ctx, phrase) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.ncloud_key_copy)) }
                OutlinedButton(
                    onClick = { saveMsg = null; saveLauncher.launch("nota-cloud-key.txt") },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.ncloud_key_save)) }
            }
            if (saveMsg == "ok") {
                Text(
                    stringResource(R.string.ncloud_key_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (saveMsg == "bad") {
                Text(
                    stringResource(R.string.ncloud_key_save_bad),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Text(
            stringResource(R.string.ncloud_key_import_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        OutlinedTextField(
                enabled = !locked,
            value = importText,
            onValueChange = { importText = it.take(200); importMsg = null },
            label = { Text(stringResource(R.string.ncloud_key_import_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            OutlinedButton(
                onClick = {
                    val st = importText
                    keyScope.launch {
                        val ok = ncvm.importCloudKey(st)
                        importMsg = if (ok) "ok" else "bad"
                        if (ok) { importText = ""; cloudPhrase = ncvm.exportCloudKey() }
                    }
                },
                enabled = !ncWorking && !working && importText.isNotBlank() && !locked,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.ncloud_key_import)) }
            OutlinedButton(
                onClick = { importFileLauncher.launch("text/plain") },
                enabled = !ncWorking && !working && !locked,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.ncloud_key_import_file)) }
        }
        if (importMsg == "ok") {
            Text(
                stringResource(R.string.ncloud_key_import_ok),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else if (importMsg == "bad") {
            Text(
                stringResource(R.string.ncloud_key_import_bad),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
// P3-3ج: قائمة اختيار الملاحظات للاستعادة الجزئية — checkbox لكل ملاحظة + الكل/إلغاء + عدّاد.
@Composable
private fun BackupNoteSelectionList(
    notes: List<com.nota.notes.data.BackupNote>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.restore_selected_fmt, selected.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSelectAll) { Text(stringResource(R.string.restore_select_all)) }
            TextButton(onClick = onDeselectAll) { Text(stringResource(R.string.restore_deselect_all)) }
        }
        Column(
            Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())
        ) {
            notes.forEach { n ->
                Row(
                    Modifier.fillMaxWidth().clickable { onToggle(n.id) }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = n.id in selected, onCheckedChange = { onToggle(n.id) })
                    Column(Modifier.weight(1f)) {
                        Text(
                            n.title.ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        Text(
                            n.body.take(60),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
// 5) سجل عمليات الاستعادة: كل دمج (وقته/مصدره/عدده) + تراجع بالنقل للسلة — قابل للاستعادة 30 يومًا (27 سبتمبر 2026)
@Composable
private fun RestoreLogCard(vm: NotesViewModel, authVm: AuthViewModel, fmt: java.text.SimpleDateFormat, locked: Boolean = false, onLocked: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val log by vm.restoreLog.collectAsStateWithLifecycle(initialValue = emptyList())
    var undoMsg by remember { mutableStateOf<String?>(null) }
    var undoBusy by remember { mutableStateOf(false) }
    // P3-3ب: حالة حل التعارضات (تمنع النقر المزدوج أثناء الكتابة).
    var resolving by remember { mutableStateOf(false) }
    SectionCard(stringResource(R.string.bk_log_title), locked = locked) {
        if (log.isEmpty()) {
            Text(
                stringResource(R.string.bk_log_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            log.forEach { rec ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                R.string.bk_log_fmt,
                                fmt.format(java.util.Date(rec.at)),
                                when (rec.src) {
                                    "drive" -> stringResource(R.string.bk_log_src_drive)
                                    "ncloud" -> stringResource(R.string.bk_log_src_ncloud)
                                    else -> stringResource(R.string.bk_log_src_file)
                                },
                                rec.added,
                                rec.skipped
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (rec.undone) {
                            Text(
                                stringResource(R.string.bk_undone),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (!rec.undone && rec.added > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    undoBusy = true
                                    val n = withContext(Dispatchers.IO) { vm.undoRestore(rec.id) }
                                    undoMsg = ctx.getString(R.string.bk_undo_fmt, n)
                                    undoBusy = false; authVm.touch()
                                }
                            },
                            enabled = !undoBusy && !locked
                        ) { Text(stringResource(R.string.bk_undo)) }
                    }
                }
                // P3-3ب: بطاقة التعارضات — الافتراضي المحفوظ (محلي) + قرار المستخدم لكل تعارض.
                if (rec.conflicts.isNotEmpty() && !rec.undone) {
                    var showConflicts by remember(rec.id) { mutableStateOf(false) }
                    TextButton(
                        onClick = { showConflicts = !showConflicts },
                        enabled = !resolving && !locked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.restore_conflicts_fmt, rec.conflicts.size))
                    }
                    if (showConflicts) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.conflict_apply_all),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        resolving = true
                                        rec.conflicts.forEach { c -> withContext(Dispatchers.IO) { vm.resolveKeepLocal(rec.id, c.id) } }
                                        resolving = false; authVm.touch()
                                    }
                                },
                                enabled = !resolving && !locked
                            ) { Text(stringResource(R.string.conflict_keep_local)) }
                        }
                        rec.conflicts.forEach { c ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(
                                    stringResource(
                                        R.string.conflict_detail_fmt,
                                        c.localTitle.ifBlank { "—" },
                                        c.remoteTitle.ifBlank { "—" }
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    fmt.format(java.util.Date(c.localUpdatedAt)) + " ← → " + fmt.format(java.util.Date(c.remoteUpdatedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                resolving = true
                                                withContext(Dispatchers.IO) { vm.resolveKeepLocal(rec.id, c.id) }
                                                resolving = false; authVm.touch()
                                            }
                                        },
                                        enabled = !resolving && !locked
                                    ) { Text(stringResource(R.string.conflict_keep_local)) }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                resolving = true
                                                withContext(Dispatchers.IO) { vm.resolveTakeRemote(rec.id, c.id) }
                                                resolving = false; authVm.touch()
                                            }
                                        },
                                        enabled = !resolving && !locked
                                    ) { Text(stringResource(R.string.conflict_take_remote)) }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                resolving = true
                                                withContext(Dispatchers.IO) { vm.resolveKeepBoth(rec.id, c.id) }
                                                resolving = false; authVm.touch()
                                            }
                                        },
                                        enabled = !resolving && !locked
                                    ) { Text(stringResource(R.string.conflict_keep_both)) }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
            }
        }
        if (undoMsg != null) {
            Text(
                undoMsg!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
// P1-B: مؤشر قوة كلمة مرور .nota — ضعيفة/متوسطة/قوية/ممتازة حسب الطول والتنوع والقوائم الشائعة.
@Composable
private fun PasswordStrengthIndicator(level: Int) {
    val label = stringResource(
        when (level) {
            1 -> R.string.nota_password_strength_medium
            2 -> R.string.nota_password_strength_strong
            3 -> R.string.nota_password_strength_excellent
            else -> R.string.nota_password_strength_weak
        }
    )
    val color = when (level) {
        1 -> MaterialTheme.colorScheme.error
        2 -> MaterialTheme.colorScheme.primary
        3 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        LinearProgressIndicator(
            progress = { (level + 1) / 4f },
            color = color,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
// 4) ملف .nota المشفر بكلمة مرور: تصدير/مشاركة + استعادة بالدمج — نُقل من الإعدادات المتقدمة (قرار 23 سبتمبر 2026)
@Composable
private fun FileBackupCard(vm: NotesViewModel, authVm: AuthViewModel, fmt: java.text.SimpleDateFormat, locked: Boolean = false, onLocked: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var expPw by remember { mutableStateOf("") }
    var expPw2 by remember { mutableStateOf("") }
    var expMsg by remember { mutableStateOf<String?>(null) }
    var resPw by remember { mutableStateOf("") }
    var resMsg by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    // معاينة النسخة قبل الدمج (27 سبتمبر 2026): json + ملف + بطاقة — التأكيد يستعيد، والإلغاء يمسح المؤقت
    var previewRestore by remember { mutableStateOf<Triple<String, File?, com.nota.notes.data.BackupMeta>?>(null) }
    var previewStale by remember { mutableStateOf(false) }
    // تنظيف احترازي: عند استبدال المعاينة أو مغادرة التركيب (مثل تدوير الشاشة)، احذف الملف المؤقت السابق.
    DisposableEffect(previewRestore) {
        val tmp = previewRestore?.second
        onDispose { tmp?.delete() }
    }

    suspend fun previewJson(json: String, tmp: File?) {
        val meta = try {
            withContext(Dispatchers.IO) { vm.previewBackupJson(json) }
        } catch (e: Exception) {
            resMsg = ctx.getString(R.string.bk_file_bad)
            tmp?.delete()
            null
        }
        if (meta != null) {
            previewStale = withContext(Dispatchers.IO) { vm.isPreviewStale(meta) }
            previewRestore = Triple(json, tmp, meta)
        }
    }

    // P3-3ج: selectedIds = null تعني الكل؛ غيرها تُستعاد المحددة فقط.
    fun restoreJson(json: String, tmp: File?, selectedIds: Set<String>? = null) {
        scope.launch {
            busy = true
            try {
                val r = withContext(Dispatchers.IO) { vm.restoreBackupJson(json, "file", selectedIds) }
                resMsg = ctx.getString(R.string.bk_restored_fmt, r.added, r.skipped)
                resPw = ""; pendingRestore = null
            } catch (e: Exception) {
                resMsg = ctx.getString(R.string.bk_file_bad)
            }
            tmp?.delete()
            busy = false; authVm.touch()
        }
    }

    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true; resMsg = null; pendingRestore = null
            val tmp = withContext(Dispatchers.IO) {
                val f = File(ctx.cacheDir, "restore.nota")
                ctx.contentResolver.openInputStream(uri)?.use { ins ->
                    f.outputStream().use { ins.copyTo(it) }
                }
                f
            }
            when (BackupManager.peek(tmp)) {
                BackupManager.BackupKind.PASSWORD -> pendingRestore = tmp
                BackupManager.BackupKind.KEYSTORE -> {
                    var err: String? = null
                    var done: String? = null
                    withContext(Dispatchers.IO) {
                        BackupManager.import(ctx, tmp, { done = it }, { err = it })
                    }
                    if (done != null) previewJson(done, tmp)
                    else { resMsg = err ?: ctx.getString(R.string.bk_file_bad); tmp.delete() }
                }
                BackupManager.BackupKind.CORRUPT -> {
                    resMsg = ctx.getString(R.string.bk_file_bad); tmp.delete()
                }
            }
            busy = false; authVm.touch()
        }
    }

    // P3-C2: التصدير بطاقة مستقلة — حقلا كلمة مرور للتصدير فقط
    SectionCard(stringResource(R.string.bk_exp_title), locked = locked) {
        Text(
            stringResource(R.string.backup_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
                enabled = !locked,
                shape = RoundedCornerShape(20.dp),
                value = expPw, onValueChange = { expPw = it; expMsg = null },
                label = { Text(stringResource(R.string.bk_pw_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                enabled = !locked,
                shape = RoundedCornerShape(20.dp),
                value = expPw2, onValueChange = { expPw2 = it; expMsg = null },
                label = { Text(stringResource(R.string.bk_pw2_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
        // P1-B: حد أدنى 8 أحرف + فحص قوة — يطابق BackupManager.encryptEnvelope (الـ backend يرفض أيضًا).
        val expStrong = expPw.length >= 8 && com.nota.notes.data.BackupManager.isStrongPassword(expPw)
        val expErr = when {
            expPw.isNotEmpty() && expPw.length < 8 -> ctx.getString(R.string.nota_password_too_short)
            expPw.length >= 8 && !com.nota.notes.data.BackupManager.isStrongPassword(expPw) -> ctx.getString(R.string.nota_password_weak)
            expPw2.isNotEmpty() && expPw != expPw2 -> ctx.getString(R.string.bk_pw_mismatch)
            else -> null
        }
        Text(
            expErr ?: expMsg ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = if (expErr != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (expPw.isNotEmpty()) {
            PasswordStrengthIndicator(level = com.nota.notes.data.BackupPassword.strengthLevel(expPw))
        }
        Text(
            stringResource(R.string.nota_password_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        OutlinedButton(
            onClick = {
                val sug = com.nota.notes.data.BackupPassword.generateDicewarePassword()
                expPw = sug; expPw2 = sug
                copySensitive(ctx, sug)
                expMsg = ctx.getString(R.string.nota_password_copied)
            },
            enabled = !busy && !locked,
            modifier = Modifier.padding(top = 4.dp)
        ) { Text(stringResource(R.string.nota_password_suggest)) }
        Button(
            onClick = {
                scope.launch {
                    busy = true; expMsg = null
                    try {
                        val json = withContext(Dispatchers.IO) { vm.buildBackupJson() }
                        val file = withContext(Dispatchers.IO) {
                            BackupManager.exportWithPassword(File(ctx.cacheDir, "backups"), json, expPw)
                        }
                        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
                        ctx.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "application/octet-stream"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },
                            ctx.getString(R.string.bk_share_title)
                        ))
                        expPw = ""; expPw2 = ""
                        expMsg = ctx.getString(R.string.bk_exp_ok)
                    } catch (e: Exception) {
                        expMsg = ctx.getString(R.string.bk_failed)
                    }
                    busy = false; authVm.touch()
                }
            },
            enabled = !busy && expPw.length >= 8 && expStrong && expPw == expPw2 && !locked,
            modifier = Modifier.padding(top = 8.dp)
        ) { Text(stringResource(R.string.bk_exp_btn)) }
        }
        // P3-C2: الاستعادة بطاقة مستقلة بحقل كلمة مرور مستقل — لا يجتمع حقلا التصدير والاستعادة معًا
        SectionCard(stringResource(R.string.bk_res_title), locked = locked) {
        Button(
            onClick = { pickRestore.launch("*/*"); authVm.touch() },
            enabled = !busy && !locked,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.bk_restore_pick)) }
        if (pendingRestore != null) {
            Text(
                stringResource(R.string.bk_encrypted_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                enabled = !locked,
                    shape = RoundedCornerShape(20.dp),
                    value = resPw, onValueChange = { resPw = it; resMsg = null },
                    label = { Text(stringResource(R.string.bk_res_pw_hint)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
            Button(
                onClick = {
                    val tmp = pendingRestore ?: return@Button
                    scope.launch {
                        busy = true
                        try {
                            val json = withContext(Dispatchers.IO) {
                                BackupManager.decryptWithPassword(tmp, resPw)
                            }
                            busy = false
                            previewJson(json, tmp)
                            return@launch
                        } catch (e: Exception) {
                            resMsg = ctx.getString(R.string.bk_pw_wrong)
                            busy = false
                        }
                    }
                },
                enabled = !busy && resPw.isNotEmpty() && !locked,
                modifier = Modifier.padding(top = 8.dp)
            ) { Text(stringResource(R.string.bk_res_btn)) }
        }
        // حوار معاينة النسخة: العدد والتاريخ ثم دمج مؤكد — بلا مسح لشيء (27 سبتمبر 2026)
        // P3-3ج: اختيار جزئي — checkbox لكل ملاحظة (الكل محددة افتراضيًا).
        previewRestore?.let { (json, tmp, meta) ->
            val pvNotes = remember(json) { vm.previewNotes(json) }
            var sel by remember(json) { mutableStateOf(pvNotes.map { it.id }.toSet()) }
            AlertDialog(
                onDismissRequest = { tmp?.delete(); previewRestore = null },
                title = { Text(stringResource(R.string.bk_preview_title)) },
                text = {
                    Column {
                        Text(
                            stringResource(
                                R.string.bk_preview_msg,
                                meta.count,
                                if (meta.createdAt > 0) fmt.format(java.util.Date(meta.createdAt)) else "—"
                            )
                        )
                        if (previewStale) {
                            Text(
                                stringResource(R.string.bk_preview_stale),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        if (pvNotes.isNotEmpty()) {
                            BackupNoteSelectionList(
                                notes = pvNotes, selected = sel,
                                onToggle = { id -> sel = if (id in sel) sel - id else sel + id },
                                onSelectAll = { sel = pvNotes.map { it.id }.toSet() },
                                onDeselectAll = { sel = emptySet() }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { val j = json; val t = tmp; val s = sel; previewRestore = null; restoreJson(j, t, s) }) {
                        Text(stringResource(R.string.bk_preview_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tmp?.delete(); previewRestore = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        if (resMsg != null) {
            Text(
                resMsg!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// خيار تكرار النسخ المجدول: راديو + تسمية — صف كامل نقّار (27 سبتمبر 2026)
@Composable
private fun FreqOption(value: String, label: String, selected: String, onPick: (String) -> Unit, locked: Boolean = false, onLocked: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = (selected == value), onClick = { if (locked) onLocked() else onPick(value) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = (selected == value), onClick = { if (locked) onLocked() else onPick(value) })
        Text(label, modifier = Modifier.padding(start = 8.dp))
        if (locked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp).size(14.dp)
            )
        }
    }
}
