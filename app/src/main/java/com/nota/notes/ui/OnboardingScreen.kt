package com.nota.notes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nota.notes.R
import kotlinx.coroutines.launch

// شاشات التعريف عند أول تشغيل: 6 صفحات — تُعرض مرة واحدة ثم تُحفظ
// 1 ترحيب/قيمة  2 مميزات  3 وظائف  4 حالات استخدام  5 أمان  6 اشتراك
private const val TOTAL_PAGES = 6
private const val SUBSCRIPTION_PAGE = 5

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { TOTAL_PAGES })
    val scope = rememberCoroutineScope()
    val currentPage = pager.currentPage
    val isLast = currentPage == TOTAL_PAGES - 1

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(Modifier.fillMaxSize().weight(1f)) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true
                ) { i ->
                    when (i) {
                        0 -> WelcomePage(onDone)
                        1 -> FeaturesPage()
                        2 -> FunctionsPage(onDone)
                        3 -> UseCasesPage()
                        4 -> SecurityPage()
                        else -> SubscriptionPage()
                    }
                }
            }
            // نقاط التقدم — توسيط
            Row(
                Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(TOTAL_PAGES) { i ->
                    Text(
                        if (i == currentPage) "● " else "○ ",
                        color = if (i == currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            // أزرار التنقل — arrows بدلاً من نص
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentPage > 0) {
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage(currentPage - 1) } }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ob_prev)
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                if (!isLast) {
                    TextButton(onClick = onDone) {
                        Text(stringResource(R.string.ob_skip))
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                if (!isLast) {
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage(currentPage + 1) } }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.ob_next)
                        )
                    }
                } else {
                    Button(onClick = onDone) {
                        Text(stringResource(R.string.ob_start))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandBadge(Icons.Filled.Description, size = 104.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.ob_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.ob_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onDone) {
            Text(stringResource(R.string.ob_cta_start))
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FeaturesPage() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.ob_features_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        FeatureRow(Icons.Filled.Lock, stringResource(R.string.ob_feat1))
        FeatureRow(Icons.Filled.Search, stringResource(R.string.ob_feat2))
        FeatureRow(Icons.Filled.Brush, stringResource(R.string.ob_feat3))
        FeatureRow(Icons.Filled.PushPin, stringResource(R.string.ob_feat4))
    }
}

@Composable
private fun StepRow(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun FunctionsPage(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.ob_functions_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        StepRow(stringResource(R.string.ob_func1))
        StepRow(stringResource(R.string.ob_func2))
        StepRow(stringResource(R.string.ob_func3))
        StepRow(stringResource(R.string.ob_func4))
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onDone) {
            Text(stringResource(R.string.ob_cta_try))
        }
    }
}

@Composable
private fun UseCasesPage() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.ob_usecases_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        StepRow(stringResource(R.string.ob_uc1))
        StepRow(stringResource(R.string.ob_uc2))
        StepRow(stringResource(R.string.ob_uc3))
        StepRow(stringResource(R.string.ob_uc4))
    }
}

@Composable
private fun SecurityPage() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandBadge(Icons.Filled.Shield, size = 104.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.ob_security_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.ob_security_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubscriptionPage() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.ob_sub_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        SectionCard(stringResource(R.string.acc_free_name), top = 0.dp) {
            Text(
                stringResource(R.string.acc_free_price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        SectionCard(stringResource(R.string.acc_pro_name), top = 0.dp) {
            Text(
                stringResource(R.string.acc_pro_items),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                stringResource(R.string.acc_pro_monthly),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.acc_pro_yearly),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(
                R.string.ob_sub_trial_note,
                com.nota.notes.data.SettingsStore.TRIAL_DAYS,
                com.nota.notes.data.SettingsStore.TRIAL_DISCOUNT_PCT
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.trial_discount_note, com.nota.notes.data.SettingsStore.TRIAL_DISCOUNT_PCT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}