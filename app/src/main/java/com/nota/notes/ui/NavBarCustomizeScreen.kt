package com.nota.notes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.nota.notes.R
import com.nota.notes.data.SettingsStore

// شاشة تخصيص الشريط السفلي — Pro فقط (قرار 26 سبتمبر 2026): اختيار الخانات الخمس + ترتيبها + مفتاح FAB
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBarCustomizeScreen(
    slots: List<String>,
    fabVisible: Boolean,
    onSlots: (List<String>) -> Unit,
    onFab: (Boolean) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    // القفل المرئي (27 سبتمبر 2026): العرض للجميع — التعديل مدفوع
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    // قاعدة طريق الإضافة: لا إضافة في الشريط → FAB إجباري ومفتاحه معطّل
    val fabForced = !slots.contains(SettingsStore.NAV_ADD)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // الهيدر المثبت من DrawerShell — بلا شريط داخلي (27 سبتمبر 2026)
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(
                    stringResource(R.string.navbar_customize_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                if (locked) {
                    ProLockedBanner(onLocked)
                }
                slots.take(5).forEachIndexed { i, key ->
                    SlotRow(
                        index = i,
                        key = key,
                        usedElsewhere = slots.filterIndexed { j, _ -> j != i }.toSet(),
                        locked = locked,
                        onLocked = onLocked,
                        onPick = { picked ->
                            onSlots(slots.toMutableList().also { it[i] = picked })
                        },
                        onUp = if (i > 0) ({ if (locked) onLocked() else onSlots(slots.toMutableList().also { val t = it[i - 1]; it[i - 1] = it[i]; it[i] = t }) }) else null,
                        onDown = if (i < slots.size - 1) ({ if (locked) onLocked() else onSlots(slots.toMutableList().also { val t = it[i + 1]; it[i + 1] = it[i]; it[i] = t }) }) else null
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.navbar_fab_title), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (fabForced) stringResource(R.string.navbar_fab_forced) else stringResource(R.string.navbar_fab_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = fabVisible || fabForced,
                        enabled = !fabForced && !locked,
                        onCheckedChange = { if (locked) onLocked() else onFab(it) }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { if (locked) onLocked() else onReset() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.navbar_reset))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotRow(
    index: Int,
    key: String,
    usedElsewhere: Set<String>,
    onPick: (String) -> Unit,
    onUp: (() -> Unit)?,
    onDown: (() -> Unit)?,
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val options = SettingsStore.NAV_ELIGIBLE.filter { it == key || it !in usedElsewhere }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (locked) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (locked) onLocked() else expanded = it }, modifier = Modifier.weight(1f)) {
            val meta = slotMeta(key)
            TextField(
                value = stringResource(meta.label),
                onValueChange = {},
                readOnly = true,
                enabled = !locked,
                label = { Text(stringResource(R.string.navbar_slot_fmt, index + 1)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    val m = slotMeta(opt)
                    DropdownMenuItem(
                        text = { Text(stringResource(m.label)) },
                        leadingIcon = {
                            if (m.icon != null) Icon(m.icon, contentDescription = null)
                            else if (m.drawable != null) Icon(painterResource(m.drawable), contentDescription = null)
                        },
                        onClick = { onPick(opt); expanded = false }
                    )
                }
            }
        }
        IconButton(onClick = { onUp?.invoke() }, enabled = onUp != null) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = null)
        }
        IconButton(onClick = { onDown?.invoke() }, enabled = onDown != null) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = null)
        }
    }
}
