package com.nota.notes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nota.notes.R

// شاشة "عن التطبيق": التعريف + حقوق الملكية الفكرية — نصوص من الموارد (عربي/إنجليزي)
// عنصر واحد من السجل: عنوان الإصدار بتاريخه ثم نقاطه — الأحدث أولًا
@Composable
private fun ChangelogEntry(title: String, items: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
        textAlign = TextAlign.Start
    )
    Text(
        items,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
    )
}

@Composable
fun AboutScreen() {
    val ctx = LocalContext.current
    var logOpen by remember { mutableStateOf(false) }
    val version = try {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        pi.versionName ?: ""
    } catch (e: Exception) {
        ""
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        BrandHeader(size = 96.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            stringResource(R.string.about_tag),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        // شارة الإصدار الذهبية — رقم النسخة على سطح مميز بدل سطر باهت
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                stringResource(R.string.about_version_fmt, version),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        // سجل التغييرات التراكمي: مطوي افتراضيًا — يُفتح عند الرغبة بدل التمرير الطويل
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clickable { logOpen = !logOpen }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.about_whatsnew),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                if (logOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (logOpen) {
        ChangelogEntry(stringResource(R.string.about_v123_0_title), stringResource(R.string.about_v123_0_items))
        ChangelogEntry(stringResource(R.string.about_v122_0_title), stringResource(R.string.about_v122_0_items))
        ChangelogEntry(stringResource(R.string.about_v118_0_title), stringResource(R.string.about_v118_0_items))
        ChangelogEntry(stringResource(R.string.about_v116_0_title), stringResource(R.string.about_v116_0_items))
        ChangelogEntry(stringResource(R.string.about_v115_0_title), stringResource(R.string.about_v115_0_items))
        ChangelogEntry(stringResource(R.string.about_v114_0_title), stringResource(R.string.about_v114_0_items))
        ChangelogEntry(stringResource(R.string.about_v113_0_title), stringResource(R.string.about_v113_0_items))
        ChangelogEntry(stringResource(R.string.about_v111_0_title), stringResource(R.string.about_v111_0_items))
        ChangelogEntry(stringResource(R.string.about_v110_0_title), stringResource(R.string.about_v110_0_items))
        ChangelogEntry(stringResource(R.string.about_v190_title), stringResource(R.string.about_v190_items))
        ChangelogEntry(stringResource(R.string.about_v180_title), stringResource(R.string.about_v180_items))
        ChangelogEntry(stringResource(R.string.about_v170_title), stringResource(R.string.about_v170_items))
        ChangelogEntry(stringResource(R.string.about_v160_title), stringResource(R.string.about_v160_items))
        ChangelogEntry(stringResource(R.string.about_v150_title), stringResource(R.string.about_v150_items))
        ChangelogEntry(stringResource(R.string.about_v140_title), stringResource(R.string.about_v140_items))
        ChangelogEntry(stringResource(R.string.about_v130_title), stringResource(R.string.about_v130_items))
        ChangelogEntry(stringResource(R.string.about_v120_title), stringResource(R.string.about_v120_items))
        ChangelogEntry(stringResource(R.string.about_v113_title), stringResource(R.string.about_v113_items))
        ChangelogEntry(stringResource(R.string.about_v112_title), stringResource(R.string.about_v112_items))
        ChangelogEntry(stringResource(R.string.about_v111_title), stringResource(R.string.about_v111_items))
        ChangelogEntry(stringResource(R.string.about_v1100_title), stringResource(R.string.about_v1100_items))
        ChangelogEntry(stringResource(R.string.about_v110_title), stringResource(R.string.about_v110_items))
        ChangelogEntry(stringResource(R.string.about_v109_title), stringResource(R.string.about_v109_items))
        ChangelogEntry(stringResource(R.string.about_v108_title), stringResource(R.string.about_v108_items))
        ChangelogEntry(stringResource(R.string.about_v107_title), stringResource(R.string.about_v107_items))
        ChangelogEntry(stringResource(R.string.about_v106_title), stringResource(R.string.about_v106_items))
        ChangelogEntry(stringResource(R.string.about_v105_title), stringResource(R.string.about_v105_items))
        ChangelogEntry(stringResource(R.string.about_v104_title), stringResource(R.string.about_v104_items))
        ChangelogEntry(stringResource(R.string.about_v103_title), stringResource(R.string.about_v103_items))
        ChangelogEntry(stringResource(R.string.about_v102_title), stringResource(R.string.about_v102_items))
        ChangelogEntry(stringResource(R.string.about_v101_title), stringResource(R.string.about_v101_items))
        ChangelogEntry(stringResource(R.string.about_v100_title), stringResource(R.string.about_v100_items))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.about_p1),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.about_p2),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            stringResource(R.string.about_p3),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.about_dev),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            stringResource(R.string.about_site),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.about_copy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        }
    }
}
