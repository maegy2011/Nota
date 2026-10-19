package com.nota.notes.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.nota.notes.R
import com.nota.notes.data.BackupManager
import com.nota.notes.data.SettingsStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// شاشة الإعدادات المتقدمة: لون افتراضي + حجم خط المحرر + تواريخ البطاقات + تغيير PIN
// حالة ضمانات التنبيه: إشعار النظام + منبه دقيق + استثناء البطارية — تُحسب لحظة العرض
// وتُحدَّث بالضغطة على إصلاح
data class AlarmHealth(val notif: Boolean, val exact: Boolean, val battery: Boolean) {
    fun ok() = notif && exact && battery
}

fun readAlarmHealth(ctx: android.content.Context): AlarmHealth {
    val notif = androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    val mgr = ctx.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
    val exact = android.os.Build.VERSION.SDK_INT < 31 || mgr == null || mgr.canScheduleExactAlarms()
    val pm = ctx.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
    val battery = pm == null || pm.isIgnoringBatteryOptimizations(ctx.packageName)
    return AlarmHealth(notif, exact, battery)
}

// ملاحظة (23 سبتمبر 2026): شاشة AdvancedSettingsScreen المدمجة حُذفت — أقسامها انتقلت إلى SettingsScreen
// الباقي هنا: AlarmHealth + readAlarmHealth + HealthLine + FontOption + ChangePinBlock (مشتركة مع الإعدادات)

// سطر حالة ضمان: ✓ خضراء أو زر إصلاح يفتح الإعداد
@Composable
fun HealthLine(ok: Boolean, label: String, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (ok) "✓ $label" else "✗ $label",
            style = MaterialTheme.typography.bodyMedium,
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        if (!ok) {
            TextButton(onClick = onFix) { Text(stringResource(R.string.health_fix)) }
        }
    }
}

@Composable
fun FontOption(value: String, label: String, selected: String, onPick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = (selected == value), onClick = { onPick(value) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = (selected == value), onClick = { onPick(value) })
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

// تغيير PIN على 3 خطوات: زر → تحقق من الحالي (أو استرداد بالبصمة) → إدخال الجديد (SetPinScreen) → حفظ
@Composable
fun ChangePinBlock(authVm: AuthViewModel) {
    var step by remember { mutableIntStateOf(0) } // 0 زر، 1 تحقق، 2 جديد، 3 تم
    var current by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var lockMsg by remember { mutableStateOf<String?>(null) }
    var recoverMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity
    val bioAvailable = authVm.canBiometric() && activity != null
    // بطاقة توصية لمرة واحدة: PIN قصير (< 6) أو مجهول الطول (مستخدم قديم) ولم تُسقط بعد
    val pinMin6Ok by authVm.pinMin6Ok.collectAsState()
    val pinCardDismissed by authVm.pinCardDismissed.collectAsState()
    val showUpgradeCard = step == 0 && pinMin6Ok != true && !pinCardDismissed
    when (step) {
        0 -> Column(Modifier.padding(top = 8.dp)) {
            if (showUpgradeCard) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            stringResource(R.string.pin_upgrade_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            stringResource(R.string.pin_upgrade_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            TextButton(onClick = { step = 1; authVm.touch() }) {
                                Text(stringResource(R.string.pin_upgrade_now))
                            }
                            TextButton(onClick = { authVm.dismissPinCard() }) {
                                Text(stringResource(R.string.pin_upgrade_later))
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { step = 1; authVm.touch() }
            ) { Text(stringResource(R.string.changepin_open)) }
        }
        1 -> Column(Modifier.padding(top = 8.dp)) {
            // «نسيت PIN الحالي؟» قبل الإدخال — نفس شيت الاسترداد (مراجعة أكتوبر 2026)
            var showRecovery by remember { mutableStateOf(false) }
            var recoverNewPin by remember { mutableStateOf(false) }
            var recoverErr by remember { mutableStateOf<String?>(null) }
            TextButton(onClick = { showRecovery = true; recoverErr = null; recoverNewPin = false }) {
                Text(
                    stringResource(R.string.forgot_current_pin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val enrolled = authVm.auth.isBioEnrolled()
            RecoverySheet(
                visible = showRecovery,
                onDismiss = { showRecovery = false; recoverNewPin = false; recoverErr = null },
                canBio = bioAvailable,
                bioEnrolled = enrolled,
                hasRecoveryKey = false,
                deviceCredEnabled = false,
                onBio = {
                    activity?.let { a ->
                        authVm.promptBiometric(
                            a,
                            onOk = { recoverNewPin = true; recoverErr = null },
                            onErr = { recoverErr = ctx.getString(R.string.recover_generic_fail) }
                        )
                    }
                },
                onRecoveryKey = {},
                recoveryError = recoverErr,
                onDeviceCred = {},
                onFactoryReset = { authVm.factoryReset {} },
                newPinContent = if (recoverNewPin) {
                    {
                        Text(
                            stringResource(R.string.recover_newpin_title),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        SetPinScreen(onDone = { np ->
                            authVm.changePin(np) {
                                showRecovery = false; recoverNewPin = false
                                step = 3; current = ""; error = false; lockMsg = null
                            }
                        })
                    }
                } else null
            )
            // اللوحة الواحدة هنا أيضًا — بلا كيبورد نظام + زر تحقق يدوي
            PinPad(
                pin = current,
                onPin = { if (it.length <= 8 && it.all(Char::isDigit)) { current = it; error = false; lockMsg = null } }
            )
            if (error) {
                Text(
                    stringResource(R.string.curpin_wrong),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }
            if (lockMsg != null) {
                Text(
                    lockMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }
            // مسار استرداد: زر البصمة ظاهر دائمًا — معطّل مع نص واضح إن غير مسجّلة (مراجعة أكتوبر 2026)
            // تمييز «غير مسجّلة» (حالة إعداد) عن «فشل التحقق» (خطأ) — لا كشف تفاصيل
            if (activity != null) {
                TextButton(
                    onClick = {
                        activity.let { a ->
                            authVm.promptBiometric(
                                a,
                                onOk = {
                                    authVm.biometricOk()
                                    step = 2; current = ""; error = false; lockMsg = null; recoverMsg = null
                                },
                                onErr = { recoverMsg = ctx.getString(R.string.pin_recover_fail) }
                            )
                        }
                    },
                    enabled = enrolled,
                    modifier = Modifier.padding(top = 4.dp)
                ) { Text(stringResource(R.string.pin_recover_forgot)) }
                if (!enrolled) {
                    Text(
                        stringResource(R.string.recover_bio_unenrolled),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (recoverMsg != null) {
                Text(
                    recoverMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }
            // التحقق هنا يُحسب ضمن درع التخمين أيضًا — لا باب خلفي
            Button(
                onClick = {
                    scope.launch {
                        when (authVm.verify(current)) {
                            is AuthViewModel.PinResult.Success -> {
                                step = 2; current = ""; error = false; lockMsg = null
                            }
                            is AuthViewModel.PinResult.Wrong -> error = true
                            is AuthViewModel.PinResult.Locked -> {
                                val s = ((authVm.lockRemaining() + 999) / 1000).toInt()
                                lockMsg = ctx.getString(R.string.pin_locked_fmt, s)
                            }
                        }
                        authVm.touch()
                    }
                },
                enabled = current.length >= 4,
                modifier = Modifier.padding(top = 8.dp)
            ) { Text(stringResource(R.string.verify_btn)) }
        }
        2 -> SetPinScreen(onDone = { newPin ->
            authVm.changePin(newPin) { Haptics.confirm(ctx); step = 3 }
        })
        else -> {
            Text(
                stringResource(R.string.pin_changed),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = { step = 0; authVm.touch() }) { Text(stringResource(R.string.changepin_again)) }
        }
    }
}
