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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
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

// شاشات التعريف عند أول تشغيل: 4 صفحات تمرير + صفحة اشتراك أخيرة — تُعرض مرة واحدة ثم تُحفظ
private data class IntroPage(val icon: ImageVector, val title: Int, val body: Int)

private val INTRO_PAGES = listOf(
    IntroPage(Icons.Filled.Lock, R.string.ob1_title, R.string.ob1_body),
    IntroPage(Icons.Filled.Fingerprint, R.string.ob2_title, R.string.ob2_body),
    IntroPage(Icons.Filled.Folder, R.string.ob3_title, R.string.ob3_body),
    IntroPage(Icons.Filled.Backup, R.string.ob4_title, R.string.ob4_body)
)

// إجمالي الصفحات = 4 تعريف + 1 اشتراك
private const val TOTAL_PAGES = 5
private const val SUBSCRIPTION_PAGE = 4

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
                    if (i < INTRO_PAGES.size) {
                        val p = INTRO_PAGES[i]
                        Column(
                            Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BrandBadge(p.icon, size = 104.dp)
                            Spacer(Modifier.height(24.dp))
                            Text(
                                stringResource(p.title),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(p.body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // صفحة الاشتراك (الأخيرة)
                        SubscriptionPage()
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
                // السابق
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
                // تخطي (نص) في صفحات التعريف
                if (!isLast) {
                    TextButton(onClick = onDone) {
                        Text(stringResource(R.string.ob_skip))
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                // التالي أو ابدأ
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