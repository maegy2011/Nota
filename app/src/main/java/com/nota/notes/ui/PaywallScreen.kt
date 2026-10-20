package com.nota.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nota.notes.R
import com.nota.notes.billing.BillingGateway
import com.nota.notes.billing.BillingResult

// بوابة الدفع — Surface إجباري (قاعدة النص الأسود: كل شاشة جديدة بـ Surface يمنح لون المحتوى الصحيح)
// تُعرض عند مدخل أي ميزة مدفوعة بلا صلاحية — المجلدات أولها
@Composable
fun PaywallScreen(
    buying: Boolean,
    onUpgrade: (String) -> Unit,
    onBack: () -> Unit,
    signedIn: Boolean = true,
    signingIn: Boolean = false,
    onGoogleSignIn: (() -> Unit)? = null,
    refreshing: Boolean = false,
    onRefreshStatus: (() -> Unit)? = null,
    // التجربة المجانية (27 سبتمبر 2026): بانر المتبقي + سطر الخصم — الأسعار النهائية من متجر Play
    trialActive: Boolean = false,
    trialLeftMs: Long = 0L,
    discountPct: Int = com.nota.notes.data.SettingsStore.TRIAL_DISCOUNT_PCT,
    // M4 (المرحلة 0أ-3): نتيجة آخر شراء — تُعرض مرة واحدة ثم تُستهلَك
    lastResult: BillingResult? = null,
    onPurchaseSuccess: () -> Unit = {},
    onResultConsumed: () -> Unit = {}
) {
    val purchaseHost = remember { SnackbarHostState() }
    PurchaseResultEffect(
        lastResult = lastResult,
        host = purchaseHost,
        successMsg = stringResource(R.string.purchase_success),
        unavailableMsg = stringResource(R.string.purchase_unavailable),
        failedFmt = stringResource(R.string.purchase_failed_fmt),
        onSuccess = onPurchaseSuccess,
        onConsumed = onResultConsumed
    )
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
      Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader(size = 64.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.paywall_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            // التجربة النشطة: بانر المتبقي — والخصم يُذكر تحت بطاقة Pro
            if (trialActive) {
                val days = (trialLeftMs / 86_400_000L).toInt()
                val hours = ((trialLeftMs % 86_400_000L) / 3_600_000L).toInt()
                SectionCard(stringResource(R.string.trial_active_title), top = 0.dp) {
                    Text(
                        if (days >= 1) stringResource(R.string.trial_left_days, days)
                        else stringResource(R.string.trial_left_hours, hours.coerceAtLeast(1)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            // الدخول أولًا: الترقية تُربط بحساب مسجل — ثم بطاقة Pro بنفس لغة شاشة الحساب
            val signInLaunch = onGoogleSignIn
            if (!signedIn && signInLaunch != null) {
                if (signingIn) {
                    CircularProgressIndicator()
                } else {
                    OutlinedButton(
                        onClick = signInLaunch,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.signin_google_btn))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            SectionCard(stringResource(R.string.acc_pro_name), top = 0.dp) {
                Text(
                    stringResource(R.string.acc_pro_items),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (buying) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onUpgrade(BillingGateway.PRODUCT_PRO_MONTHLY) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.acc_pro_monthly))
                        }
                        Button(
                            onClick = { onUpgrade(BillingGateway.PRODUCT_PRO_YEARLY) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.acc_pro_yearly))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // عرض الافتتاح: خصم ثابت على السنوية لفترة محدودة بعد التجربة — يُفعَّل من عروض متجر Play
            Text(
                stringResource(R.string.trial_discount_note, discountPct),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.acc_free_name) + " • " + stringResource(R.string.acc_free_price),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.paywall_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            // زر تحديث الحالة: سؤال السيرفر الحي متجاهلًا الكاش ثم إعادة تقييم البوابة فورًا
            if (onRefreshStatus != null) {
                Spacer(Modifier.height(4.dp))
                if (refreshing) {
                    CircularProgressIndicator()
                } else {
                    TextButton(onClick = onRefreshStatus) {
                        Text(stringResource(R.string.paywall_refresh))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back_cd))
            }
        }
        SnackbarHost(
            purchaseHost,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
      }
    }
}

// عرض التجربة أول استخدام فقط (27 سبتمبر 2026): 7 أيام Pro + خصم — قبول يبدأ العداد، ورفض لا يعيد العرض
@Composable
fun TrialOfferDialog(
    onStart: () -> Unit,
    onDecline: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(stringResource(R.string.trial_offer_title)) },
        text = {
            Text(
                stringResource(
                    R.string.trial_offer_msg,
                    com.nota.notes.data.SettingsStore.TRIAL_DAYS,
                    com.nota.notes.data.SettingsStore.TRIAL_DISCOUNT_PCT
                )
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onStart) {
                Text(stringResource(R.string.trial_start_btn))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDecline) {
                Text(stringResource(R.string.trial_decline))
            }
        }
    )
}

// M4 (المرحلة 0أ-3): يعرض نتيجة آخر شراء مرة واحدة — النجاح/الفشل/المتجر بتنبيه، والإلغاء يُستهلَك بصمت
@Composable
internal fun PurchaseResultEffect(
    lastResult: BillingResult?,
    host: SnackbarHostState,
    successMsg: String,
    unavailableMsg: String,
    failedFmt: String,
    onSuccess: () -> Unit = {},
    onConsumed: () -> Unit
) {
    LaunchedEffect(lastResult) {
        when (lastResult) {
            null -> {}
            is BillingResult.Success -> {
                onSuccess()
                host.showSnackbar(successMsg)
                onConsumed()
            }
            BillingResult.Unavailable -> {
                host.showSnackbar(unavailableMsg)
                onConsumed()
            }
            is BillingResult.Failed -> {
                host.showSnackbar(failedFmt.format(lastResult.reason))
                onConsumed()
            }
            BillingResult.Cancelled -> {
                // المستخدم ألغى بنفسه — بلا تنبيه، وتُستهلَك حتى لا تتراكم
                onConsumed()
            }
        }
    }
}
