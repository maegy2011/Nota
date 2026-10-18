package com.nota.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nota.notes.R

@Composable
fun LockScreen(
    showBiometric: Boolean,
    onBiometric: () -> Unit,
    onPin: (String) -> Unit,
    pinError: String? = null,
    bioError: String? = null,
    lockUntilMs: Long = 0L,
    onLockExpired: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
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
        Column(
            Modifier.fillMaxSize().imePadding().padding(24.dp),
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
        Text(
            stringResource(R.string.locked_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        Spacer(Modifier.height(24.dp))
        // لوحة داخلية واحدة: البصمة داخلها + الإرسال يدوي بزر الفتح (منعًا للتخمين)
        if (bioError != null) {
            Text(
                bioError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Assertive }
            )
        }
        // الـ PIN أرقام LTR دائمًا حتى داخل واجهة RTL (داخل PinPad)
        PinPad(
            pin = pin,
            onPin = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
            showBio = showBiometric,
            onBio = onBiometric
        )
        if (pinError != null) {
            Text(
                pinError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Assertive }
            )
        }
        if (isLocked) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text(
                    stringResource(R.string.pin_locked_fmt, lockedRemain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onPin(pin) }, enabled = pin.isNotEmpty() && !isLocked, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.unlock_pin))
        }
        if (rooted == true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text(
                    stringResource(R.string.root_warn),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }
            }
        }
    }
}

@Composable
private fun LocalTextStyleLtr() = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Start)
