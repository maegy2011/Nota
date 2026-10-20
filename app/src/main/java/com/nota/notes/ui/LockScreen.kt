package com.nota.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nota.notes.R

// Fallback chain (implemented in phases):
// PIN fails → biometric (P3-A1) → recovery key (P3-A2)
//   → device credential (P3-A3) → full app reset (P0-3, always available)
@Composable
fun LockScreen(
    showBiometric: Boolean,
    onBiometric: () -> Unit,
    onPin: (String) -> Unit,
    pinError: String? = null,
    bioError: String? = null,
    lockUntilMs: Long = 0L,
    onLockExpired: () -> Unit = {},
    // البصمة مقفلة مؤقتًا (نظام) — تُخفى خلية البصمة ويبقى PIN
    bioLockedOut: Boolean = false,
    // إسقاط تحذير الروت نهائيًا (مخزن) — والجلسة محلية أدناه
    rootNeverDismissed: Boolean = false,
    onRootNeverShow: () -> Unit = {},
    // مسارات الاسترداد (مراجعة أكتوبر 2026 — Commit 1): الشيت يعرض المتاح فقط
    authVm: AuthViewModel,
    hasRecoveryKey: Boolean = false,
    deviceCredEnabled: Boolean = false,
    onRecovered: () -> Unit = {},
    // UX القفل (Commit 3): حالة التسجيل + تنبيه استباقي + وميض نجاح
    bioEnrolled: Boolean = true,
    prelockWarn: Boolean = false,
    successFlash: Boolean = false
) {
    var pin by remember { mutableStateOf("") }
    // حالات الاسترداد مرفوعة للأعلى — معالج لوحة المفاتيح الفيزيائية يحتاجها (Commit 5أ)
    var showRecovery by remember { mutableStateOf(false) }
    var recoverNewPin by remember { mutableStateOf(false) }
    var recoverErr by remember { mutableStateOf<String?>(null) }
    val recScope = rememberCoroutineScope()
    val genericFail = stringResource(R.string.recover_generic_fail)
    // قفل اتجاه الشاشة عموديًا طوال عرض القفل — يُستعاد السابق عند الخروج
    val ctx = LocalContext.current
    val activity = ctx as? android.app.Activity
    DisposableEffect(Unit) {
        val prev = activity?.requestedOrientation
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { if (prev != null) activity?.requestedOrientation = prev }
    }
    // إسقاط تحذير الروت لهذه الجلسة فقط — الخيار الدائم عبر الزر الثاني
    var rootSessionDismissed by remember { mutableStateOf(false) }
    // تحذير الجذر مرة واحدة (IO) — غير حاجب
    var rooted by remember { mutableStateOf<Boolean?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        rooted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.nota.notes.security.RootCheck.isRooted()
        }
    }
    // عدّاد القفل المؤقت — يتحدث كل ثانية وينتهي تلقائيًا
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(lockUntilMs) {
        while (lockUntilMs > System.currentTimeMillis()) {
            kotlinx.coroutines.delay(1000)
            nowMs = System.currentTimeMillis()
        }
        if (lockUntilMs > 0) onLockExpired()
    }
    val lockedRemain = ((lockUntilMs - nowMs) / 1000).toInt().coerceAtLeast(0)
    val isLocked = lockUntilMs > nowMs
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // لوحة مفاتيح فيزيائية (Commit 5أ): أرقام إدخال + مسح + Enter فتح + Escape استرداد — على الحاوية الجذرية
        fun typeDigit(d: String): Boolean {
            if (pin.length < 8 && pin.all(Char::isDigit)) { pin += d; return true }
            return false
        }
        Column(
            Modifier.fillMaxSize().imePadding().padding(24.dp)
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (e.key) {
                        Key.Zero, Key.NumPad0 -> typeDigit("0")
                        Key.One, Key.NumPad1 -> typeDigit("1")
                        Key.Two, Key.NumPad2 -> typeDigit("2")
                        Key.Three, Key.NumPad3 -> typeDigit("3")
                        Key.Four, Key.NumPad4 -> typeDigit("4")
                        Key.Five, Key.NumPad5 -> typeDigit("5")
                        Key.Six, Key.NumPad6 -> typeDigit("6")
                        Key.Seven, Key.NumPad7 -> typeDigit("7")
                        Key.Eight, Key.NumPad8 -> typeDigit("8")
                        Key.Nine, Key.NumPad9 -> typeDigit("9")
                        Key.Backspace -> if (pin.isNotEmpty()) { pin = pin.dropLast(1); true } else false
                        Key.Enter, Key.NumPadEnter -> if (pin.isNotEmpty() && !isLocked) { onPin(pin); true } else false
                        Key.Escape -> { showRecovery = true; recoverErr = null; recoverNewPin = false; true }
                        else -> false
                    }
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        BrandHeader(size = 96.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.locked_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        // سطر تمهيدي ديناميكي: مقفل مؤقتًا / PIN فقط / الوضع العادي
        val subtitleRes = when {
            isLocked -> R.string.locked_sub_throttled
            !showBiometric -> R.string.locked_sub_pinonly
            else -> R.string.locked_sub
        }
        Text(
            stringResource(subtitleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        Spacer(Modifier.height(24.dp))
        // اللوحة الداخلية: الإرسال يدوي بزر الفتح (منعًا للتخمين) — خلية البصمة
        // نُقلت لزر عريض مستقل أدناه (Commit 3: أوضح + حالات متاح/معطل/مقفل)
        val shakeX = remember { androidx.compose.animation.core.Animatable(0f) }
        androidx.compose.runtime.LaunchedEffect(pinError) {
            if (pinError != null) {
                // وميض بصري للخطأ: اهتزاز أفقي قصير (Commit 3د) + اهتزاز لمسي مميز عند الضغط
                repeat(2) {
                    shakeX.animateTo(10f, animationSpec = androidx.compose.animation.core.tween(60))
                    shakeX.animateTo(-10f, animationSpec = androidx.compose.animation.core.tween(60))
                }
                shakeX.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(60))
            }
        }
        // الـ PIN أرقام LTR دائمًا حتى داخل واجهة RTL (داخل PinPad)
        androidx.compose.foundation.layout.Box(
            Modifier.graphicsLayer { translationX = shakeX.value }
        ) {
            PinPad(
                pin = pin,
                onPin = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                showBio = false
            )
        }
        // منطقة حالة موحدة بين اللوحة والزر (Commit 3أ): رسالة واحدة بأولوية —
        // درع > خطأ PIN > خطأ بصمة > تنبيه استباقي > تلميح 8 > فارغ — fade قصير 150ms
        val statusKey = when {
            isLocked -> "locked"
            pinError != null -> "pin"
            bioError != null -> "bio"
            prelockWarn -> "pre"
            pin.length >= 8 -> "hint"
            else -> "empty"
        }
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().heightIn(min = 30.dp).padding(top = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Assertive }
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = statusKey,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) togetherWith
                        androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                },
                label = "lockStatus"
            ) { key ->
                when (key) {
                    "locked" -> Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.pin_locked_fmt, lockedRemain),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(10.dp)
                        )
                    }
                    "pin" -> Text(
                        pinError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "bio" -> Text(
                        bioError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "pre" -> Text(
                        stringResource(R.string.lock_prelock_warn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "hint" -> Text(
                        stringResource(R.string.pin_hint_max8),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    else -> Spacer(Modifier.height(4.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onPin(pin) }, enabled = pin.isNotEmpty() && !isLocked, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.unlock_pin))
        }
        // زر البصمة العريض (Commit 3ب): بعرض Unlock تقريبًا، 56dp — بحالات متاح/معطل/مقفل مؤقتًا
        if (showBiometric) {
            val bioReady = !bioLockedOut && bioEnrolled
            androidx.compose.material3.OutlinedButton(
                onClick = onBiometric,
                enabled = bioReady,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(stringResource(R.string.bio_open_wide))
                }
            }
            if (bioLockedOut) {
                Text(
                    stringResource(R.string.bio_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            } else if (!bioEnrolled) {
                Text(
                    stringResource(R.string.bio_notenrolled_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
        // وميض النجاح (Commit 3هـ): طبقة خضراء خفيفة 300ms قبل الانتقال + اهتزاز تأكيد — بلا صوت
        if (successFlash) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            )
        }
        // رابط «نسيت PIN؟» — ظاهر دائمًا حتى أثناء قفل الدرع (مراجعة أكتوبر 2026)
        TextButton(onClick = { showRecovery = true; recoverErr = null; recoverNewPin = false }) {
            Text(
                stringResource(R.string.forgot_pin_link),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val act = activity as? androidx.fragment.app.FragmentActivity
        RecoverySheet(
            visible = showRecovery,
            onDismiss = { showRecovery = false; recoverNewPin = false; recoverErr = null },
            canBio = act != null,
            bioEnrolled = showBiometric && bioEnrolled,
            hasRecoveryKey = hasRecoveryKey,
            deviceCredEnabled = deviceCredEnabled,
            onBio = {
                act?.let { a ->
                    authVm.promptBiometric(
                        a,
                        // نجاح البصمة هنا ≠ تصفير الدرع — التصفير عند تأكيد PIN الجديد فقط (changePin)
                        onOk = { recoverNewPin = true; recoverErr = null },
                        onErr = { recoverErr = ctx.getString(R.string.recover_generic_fail) }
                    )
                }
            },
            onRecoveryKey = { key ->
                recScope.launch {
                    when (val r = authVm.verifyRecoveryKey(key)) {
                        is AuthViewModel.RecoveryResult.Success -> { recoverNewPin = true; recoverErr = null }
                        is AuthViewModel.RecoveryResult.Wrong -> recoverErr = genericFail
                        is AuthViewModel.RecoveryResult.Locked -> {
                            val s = ((r.untilMs - System.currentTimeMillis() + 999) / 1000).toInt()
                            recoverErr = ctx.getString(R.string.pin_locked_fmt, s)
                        }
                    }
                }
            },
            recoveryError = recoverErr,
            onDeviceCred = {
                act?.let { a ->
                    authVm.promptDeviceCredential(
                        a,
                        onOk = { recoverNewPin = true; recoverErr = null },
                        onErr = { if (it.isNotBlank()) recoverErr = it else recoverErr = genericFail }
                    )
                }
            },
            onFactoryReset = { authVm.factoryReset {} },
            newPinContent = if (recoverNewPin) {
                {
                    Text(
                        stringResource(R.string.recover_newpin_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    SetPinScreen(onDone = { np ->
                        authVm.changePin(np) {
                            showRecovery = false; recoverNewPin = false; onRecovered()
                        }
                    })
                }
            } else null
        )
        if (rooted == true && !rootSessionDismissed && !rootNeverDismissed) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.root_warn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        TextButton(onClick = { rootSessionDismissed = true }) {
                            Text(stringResource(R.string.root_ack))
                        }
                        TextButton(onClick = onRootNeverShow) {
                            Text(stringResource(R.string.root_never))
                        }
                    }
                }
            }
        }
            }
        }
    }
}

@Composable
private fun LocalTextStyleLtr() = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Start)
