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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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

// تغيير PIN على 3 خطوات: زر → تحقق من الحالي → إدخال الجديد (SetPinScreen) → حفظ
@Composable
fun ChangePinBlock(authVm: AuthViewModel) {
    var step by remember { mutableIntStateOf(0) } // 0 زر، 1 تحقق، 2 جديد، 3 تم
    var current by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var lockMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    when (step) {
        0 -> Button(
            onClick = { step = 1; authVm.touch() },
            modifier = Modifier.padding(top = 8.dp)
        ) { Text(stringResource(R.string.changepin_open)) }
        1 -> Column(Modifier.padding(top = 8.dp)) {
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
                )
            }
            if (lockMsg != null) {
                Text(
                    lockMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
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
            authVm.changePin(newPin) { step = 3 }
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
