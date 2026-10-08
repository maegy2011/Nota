package com.nota.notes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nota.notes.R
import java.util.Calendar

// رسالة الترحيب عند فتح التطبيق (27 سبتمبر 2026): تحية بالوقت + الاسم —
// فارغ الاسم = حقل تسجيل داخل الحوار نفسه — وزر إخفاء دائم لمن لا يريدها
@Composable
fun WelcomeDialog(
    username: String,
    onSaveName: (String) -> Unit,
    onEnter: () -> Unit,
    onHideForever: () -> Unit
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val morning = hour in 4..11
    var draft by remember(username) { mutableStateOf(username) }
    AlertDialog(
        onDismissRequest = onEnter,
        title = {
            Text(
                if (username.isNotBlank()) {
                    if (morning) stringResource(R.string.welcome_named_morning, username)
                    else stringResource(R.string.welcome_named_evening, username)
                } else {
                    if (morning) stringResource(R.string.welcome_morning)
                    else stringResource(R.string.welcome_evening)
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.welcome_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (username.isBlank()) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text(stringResource(R.string.username_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (draft.isNotBlank() && draft != username) onSaveName(draft)
                onEnter()
            }) {
                Text(stringResource(R.string.welcome_enter))
            }
        },
        dismissButton = {
            TextButton(onClick = onHideForever) {
                Text(stringResource(R.string.welcome_hide))
            }
        }
    )
}
