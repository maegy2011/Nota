package com.nota.notes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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

// شاشات التعريف عند أول تشغيل: 4 صفحات تمرير — تُعرض مرة واحدة ثم تُحفظ
private data class IntroPage(val icon: ImageVector, val title: Int, val body: Int)

private val PAGES = listOf(
    IntroPage(Icons.Filled.Lock, R.string.ob1_title, R.string.ob1_body),
    IntroPage(Icons.Filled.Face, R.string.ob2_title, R.string.ob2_body),
    IntroPage(Icons.Filled.Search, R.string.ob3_title, R.string.ob3_body),
    IntroPage(Icons.Filled.Share, R.string.ob4_title, R.string.ob4_body)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.size - 1
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxSize().weight(1f)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { i ->
                val p = PAGES[i]
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
            }
        }
        // نقاط التقدم
        Row(
            Modifier.padding(vertical = 16.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(PAGES.size) { i ->
                Text(
                    if (i == pager.currentPage) "● " else "○ ",
                    color = if (i == pager.currentPage) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Row(
            Modifier.padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!last) {
                TextButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.ob_skip))
                }
                Button(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.ob_next)) }
            } else {
                Button(onClick = onDone, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.ob_start))
                }
            }
        }
        }
    }
}
