package com.nota.notes.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nota.notes.R

@Composable
fun SetPinScreen(onDone: (String) -> Unit) {
    // مرحلتان بنفس اللوحة الواحدة: إدخال ثم تأكيد — بلا حقلين مكررين
    var step by remember { mutableStateOf(0) } // 0 إدخال، 1 تأكيد
    var first by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val mismatch = stringResource(R.string.pin_mismatch)
    val short = stringResource(R.string.pin_short)
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
        if (error != null) {
            Text(
                error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (step == 0) {
                    if (current.length < 4) error = short
                    else { first = current; current = ""; step = 1; error = null }
                } else {
                    error = when {
                        current.length < 4 -> short
                        current != first -> mismatch
                        else -> null
                    }
                    if (error == null) onDone(current)
                }
            },
            enabled = current.length >= 4,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(if (step == 0) R.string.verify_btn else R.string.setpin_done)) }
    }
    }
}
