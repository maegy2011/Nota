package com.nota.notes.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.nota.notes.R
import kotlinx.coroutines.launch

// تحدي فتح عنصر مقفل (ملاحظة/مجلد): PIN أو بصمة — يُستخدم من القوائم وتفاصيل المجلد.
// يعيد استخدام AuthViewModel (درع التخمين سارٍ هنا أيضًا — لا باب خلفي).
@Composable
fun LockChallenge(
    title: String,
    authVm: AuthViewModel,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var bioError by remember { mutableStateOf<String?>(null) }
    val bioAvailable = authVm.canBiometric() && activity != null

    fun submit() {
        if (pin.isEmpty()) return
        scope.launch {
            when (authVm.verify(pin)) {
                is AuthViewModel.PinResult.Success -> { authVm.touch(); onUnlocked() }
                is AuthViewModel.PinResult.Wrong -> error = ctx.getString(R.string.pin_wrong)
                is AuthViewModel.PinResult.Locked -> {
                    val s = ((authVm.lockRemaining() + 999) / 1000).toInt()
                    error = ctx.getString(R.string.pin_locked_fmt, s)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                // العنوان سطر واحد مقصوص — الطويل لا يغيّر حجم الحوار (قرار 26 سبتمبر 2026)
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.unlock_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // لوحة داخلية واحدة: البصمة داخلها + الإرسال يدوي بزر الفتح
                PinPad(
                    pin = pin,
                    onPin = { if (it.length <= 8 && it.all(Char::isDigit)) { pin = it; error = null } },
                    showBio = bioAvailable,
                    onBio = {
                        activity?.let { a ->
                            authVm.promptBiometric(
                                a,
                                onOk = { authVm.biometricOk(); authVm.touch(); onUnlocked() },
                                onErr = { if (it.isNotBlank()) bioError = it }
                            )
                        }
                    }
                )
                if (error != null) {
                    Text(
                        error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (bioError != null) {
                    Text(
                        bioError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = pin.isNotEmpty()) {
                Text(stringResource(R.string.unlock_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
