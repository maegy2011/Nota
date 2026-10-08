package com.nota.notes.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nota.notes.R

// مراجعة الصلاحيات عند أول تثبيت (27 سبتمبر 2026): تُعرض مرة واحدة بعد التعريف —
// كل صف: لماذا يلزم الإذن + الحالة الحية + زر منح/إصلاح. الرفض لا يمنع الدخول؛
// كل ميزة تتحقق من إذنها لحظة الاستخدام وتوجّه المستخدم للإعدادات عند الحاجة.
@Composable
fun PermissionsReviewScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val appCtx = ctx.applicationContext
    val owner = LocalLifecycleOwner.current
    // عدّاد يُجبِر إعادة قراءة الحالات عند العودة من الإعدادات
    var resumeTick by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    @Suppress("UNUSED_EXPRESSION")
    resumeTick // تُقرأ عمدًا لإعادة التركيب عند الاستئناف

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resumeTick++ }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }

    fun openAppSettings() {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    fun openExactAlarm() {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    fun openBattery() {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    val notifOk = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(appCtx, android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    val mgr = appCtx.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
    val exactOk = Build.VERSION.SDK_INT < 31 || mgr == null || mgr.canScheduleExactAlarms()
    val pm = appCtx.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
    val batteryOk = pm == null || pm.isIgnoringBatteryOptimizations(appCtx.packageName)
    val locOk = ContextCompat.checkSelfPermission(appCtx, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    val micOk = ContextCompat.checkSelfPermission(appCtx, android.Manifest.permission.RECORD_AUDIO) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.perms_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.perms_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            PermRow(
                title = stringResource(R.string.perm_notif_title),
                sub = stringResource(R.string.perm_notif_sub),
                ok = notifOk,
                actionLabel = stringResource(if (notifOk) R.string.perm_granted else R.string.perm_grant),
                onAction = {
                    if (Build.VERSION.SDK_INT >= 33 && !notifOk)
                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            )
            PermRow(
                title = stringResource(R.string.perm_exact_title),
                sub = stringResource(R.string.perm_exact_sub),
                ok = exactOk,
                actionLabel = stringResource(if (exactOk) R.string.perm_granted else R.string.perm_fix),
                onAction = { if (!exactOk) openExactAlarm() }
            )
            PermRow(
                title = stringResource(R.string.perm_battery_title),
                sub = stringResource(R.string.perm_battery_sub),
                ok = batteryOk,
                actionLabel = stringResource(if (batteryOk) R.string.perm_granted else R.string.perm_fix),
                onAction = { if (!batteryOk) openBattery() }
            )
            PermRow(
                title = stringResource(R.string.perm_loc_title),
                sub = stringResource(R.string.perm_loc_sub),
                ok = locOk,
                actionLabel = stringResource(if (locOk) R.string.perm_granted else R.string.perm_grant),
                onAction = {
                    if (!locOk) locLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
            PermRow(
                title = stringResource(R.string.perm_mic_title),
                sub = stringResource(R.string.perm_mic_sub),
                ok = micOk,
                actionLabel = stringResource(if (micOk) R.string.perm_granted else R.string.perm_grant),
                onAction = { if (!micOk) micLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.perms_done))
            }
            TextButton(onClick = { openAppSettings() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.perm_open_settings))
            }
        }
    }
}

@Composable
private fun PermRow(title: String, sub: String, ok: Boolean, actionLabel: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                (if (ok) "✓ " else "✗ ") + title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onAction, enabled = !ok) { Text(actionLabel) }
    }
}
