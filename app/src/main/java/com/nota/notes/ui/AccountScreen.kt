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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.nota.notes.R
import com.nota.notes.billing.BillingGateway

// شاشة الحساب المنفصلة: الهوية + الباقة الحالية والتجديد + الباقات والأسعار — بنمط DesignSystem
// الدخول من القائمة الجانبية فقط (قرار 23 سبتمبر 2026) — بلا ارتباط بالإعدادات ولا أفاتار الرئيسية
@Composable
fun AccountScreen(
    accountVm: AccountViewModel,
    premiumVm: PremiumViewModel,
    ncvm: NotaCloudViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val id by accountVm.accountId.collectAsStateWithLifecycle()
    val mail by accountVm.email.collectAsStateWithLifecycle()
    val busy by accountVm.signingIn.collectAsStateWithLifecycle()
    val err by accountVm.error.collectAsStateWithLifecycle()
    val pending by accountVm.serverPending.collectAsStateWithLifecycle()
    val entitlement by premiumVm.entitlement.collectAsStateWithLifecycle()
    val buying by premiumVm.buying.collectAsStateWithLifecycle()
    val refreshing by premiumVm.refreshing.collectAsStateWithLifecycle()
    // انتهاء الجلسة: يُعرض طلب إعادة الدخول بدل التعليق على loading أو الـ FREE الصامت
    val ncAuthExpired by ncvm.authExpired.collectAsStateWithLifecycle()
    val premiumAuthExpired by premiumVm.authExpired.collectAsStateWithLifecycle()
    val sessionExpired = ncAuthExpired || premiumAuthExpired
    // تأكيد قبل تسجيل الخروج (بأمر محمد 27 سبتمبر 2026)
    var confirmSignout by remember { mutableStateOf(false) }
    if (confirmSignout) {
        ConfirmDialog(
            message = stringResource(R.string.confirm_signout_msg),
            confirmLabel = stringResource(R.string.confirm_signout_btn),
            onConfirm = { confirmSignout = false; accountVm.signOut(); onBack() },
            onDismiss = { confirmSignout = false }
        )
    }
    // T4 (المرحلة 0أ-1): القرار المركزي من premiumVm.isPro() يشمل التجربة — بلا دالة حرة
    // تُجمَع trialOn هنا ليعيد التركيب نفسه عند بدء/انتهاء التجربة
    val trialOn by premiumVm.trialActive.collectAsStateWithLifecycle()
    // المرحلة 1أ: مصدر Pro للعرض — "من أين جاء؟" (اشتراك ساري vs تجربة مجانية)
    val proSource by premiumVm.source.collectAsStateWithLifecycle()
    val pro = remember(trialOn, entitlement) { premiumVm.isPro() }
    val trialOnly = trialOn && entitlement?.pro != true
    val planLabel = stringResource(
        if (trialOnly) R.string.acc_plan_pro_trial
        else if (pro) R.string.acc_plan_pro
        else R.string.acc_plan_free
    )
    // M4 (المرحلة 0أ-3): شاشة الحساب فيها أزرار شراء — تعرض نتيجة الشراء هنا أيضًا
    val lastResult by premiumVm.lastResult.collectAsStateWithLifecycle()
    val purchaseHost = remember { SnackbarHostState() }
    PurchaseResultEffect(
        lastResult = lastResult,
        host = purchaseHost,
        successMsg = stringResource(R.string.purchase_success),
        unavailableMsg = stringResource(R.string.purchase_unavailable),
        failedFmt = stringResource(R.string.purchase_failed_fmt),
        onSuccess = { premiumVm.forceRefresh() },
        onConsumed = { premiumVm.clearLastResult() }
    )
    val ncQuota by ncvm.quota.collectAsStateWithLifecycle()
    // حالة التخزين: تُحدث مع تبدل الحساب — جزء من بيانات الاشتراك
    LaunchedEffect(id) {
        if (id != null) ncvm.refresh()
    }
    val fmt = rememberDateFmt()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // الهوية: الأفاتار (صورة المستخدم أو حرفه) + البريد + شارة الباقة
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccountAvatar(accountId = id, label = mail ?: id, size = 64.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (mail.isNullOrBlank()) stringResource(R.string.acc_signed_out) else mail!!,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            planLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        // المرحلة 1أ: سطر المصدر تحت الشارة — يظهر فقط لمشتركي Pro
                        if (pro) {
                            Text(
                                stringResource(
                                    if (proSource == EntitlementSource.TRIAL) R.string.pro_source_trial
                                    else R.string.pro_source_server
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
            val currentId = id
            if (currentId == null) {
                // غير مسجل: الدخول أولًا — الباقات تُشترى بحساب مسجل فقط
                SectionCard(stringResource(R.string.account_title), top = 20.dp) {
                    Text(
                        stringResource(R.string.signin_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val launcher = rememberGoogleSignInLauncher(accountVm)
                    Button(
                        onClick = { launcher.launch(accountVm.signInIntent()) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.signin_google_btn))
                    }
                    if (err != null) {
                        Text(
                            stringResource(R.string.signin_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                // الباقة الحالية: الاسم + الحالة + التجديد/الإلغاء — تُدار عبر Google Play
                SectionCard(stringResource(R.string.acc_current)) {
                    Text(
                        planLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                    val exp = entitlement?.expiresAt
                    Text(
                        when {
                            !pro -> stringResource(R.string.acc_forever)
                            exp != null && exp > 0L -> stringResource(
                                R.string.acc_until_fmt,
                                fmt.format(java.util.Date(exp))
                            )
                            else -> stringResource(R.string.acc_active)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (pro) {
                        Text(
                            stringResource(R.string.acc_manage_play),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (pending) {
                        Text(
                            stringResource(R.string.signin_offline_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (sessionExpired) {
                        Text(
                            stringResource(R.string.ncloud_err_auth),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (refreshing) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp).size(24.dp))
                    } else {
                        TextButton(onClick = { premiumVm.forceRefresh() }) {
                            Text(stringResource(R.string.acc_refresh))
                        }
                    }
                }
                // حالة التخزين السحابي: الحصة المستخدمة حسب الباقة — المجانية 100 ميجا وPro خمسة 5G
                SectionCard(stringResource(R.string.acc_storage)) {
                    val q = ncQuota
                    if (q == null) {
                        if (sessionExpired) {
                            Text(
                                stringResource(R.string.ncloud_err_auth),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                stringResource(R.string.ncloud_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val frac = if (q.quotaBytes > 0) (q.usedBytes.toFloat() / q.quotaBytes.toFloat()).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        )
                        Text(
                            stringResource(R.string.ncloud_used_fmt, fmtBytes(q.usedBytes), fmtBytes(q.quotaBytes)),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (pro) stringResource(R.string.acc_storage_pro_note)
                            else stringResource(R.string.acc_storage_free_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            // الباقات والأسعار: بطاقات حديثة — المجانية دائمًا + Pro مميزة بنقاط مفصلة وأسعار بارزة
            PlansSection(
                currentId = currentId,
                pro = pro,
                buying = buying,
                onMonthly = { premiumVm.purchase(BillingGateway.PRODUCT_PRO_MONTHLY) },
                onYearly = { premiumVm.purchase(BillingGateway.PRODUCT_PRO_YEARLY) }
            )
            // حزم التخزين الإضافية: للمشتركين Pro فأعلى — تُجمع فوق سعة الباقة (5G أساس Pro)
            SectionCard(stringResource(R.string.acc_packs)) {
                if (!pro) {
                    Text(
                        stringResource(R.string.acc_packs_pro_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                PackRow(
                    name = stringResource(R.string.acc_pack_5g),
                    monthly = stringResource(R.string.acc_pack_5g_m),
                    yearly = stringResource(R.string.acc_pack_5g_y),
                    enabled = currentId != null && pro && !buying,
                    onMonthly = { premiumVm.purchase(BillingGateway.PRODUCT_STORAGE_5G_MONTHLY) },
                    onYearly = { premiumVm.purchase(BillingGateway.PRODUCT_STORAGE_5G_YEARLY) }
                )
                PackRow(
                    name = stringResource(R.string.acc_pack_10g),
                    monthly = stringResource(R.string.acc_pack_10g_m),
                    yearly = stringResource(R.string.acc_pack_10g_y),
                    enabled = currentId != null && pro && !buying,
                    onMonthly = { premiumVm.purchase(BillingGateway.PRODUCT_STORAGE_10G_MONTHLY) },
                    onYearly = { premiumVm.purchase(BillingGateway.PRODUCT_STORAGE_10G_YEARLY) }
                )
            }
            if (currentId != null) {
                // الخروج: يُخفي الملاحظات عن الجهاز وتبقى مشفرة — تعود بالدخول مجددًا
                SectionCard(stringResource(R.string.account_title), top = 20.dp) {
                    OutlinedButton(
                        onClick = { confirmSignout = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.signout_btn))
                    }
                    Text(
                        stringResource(R.string.signout_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        SnackbarHost(
            purchaseHost,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
      }
    }
}

// قسم الباقات الحديث: بطاقة مجانية + بطاقة Pro مميزة بنقاط وأزرار أسعار — تُستخدم هنا وفي البوابة
@Composable
internal fun PlansSection(
    currentId: String?,
    pro: Boolean,
    buying: Boolean,
    onMonthly: () -> Unit,
    onYearly: () -> Unit
) {
    SectionCard(stringResource(R.string.acc_plans)) {
        // المجانية: بطاقة هادئة بنقاطها
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.acc_free_name), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.acc_free_price),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
                stringResource(R.string.acc_free_items).split("\n").forEach { PlanBullet(it) }
            }
        }
        // Pro: بطاقة مميزة بإطار بارز + شارة + نقاط مسهبة + زرّا سعر
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.acc_pro_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    if (pro) {
                        Text(
                            stringResource(R.string.acc_plan_pro),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                stringResource(R.string.acc_pro_items).split("\n").forEach {
                    PlanBullet(it, MaterialTheme.colorScheme.onPrimaryContainer)
                }
                if (currentId == null) {
                    Text(
                        stringResource(R.string.acc_signin_first),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onMonthly, enabled = currentId != null && !buying && !pro, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.acc_pro_monthly))
                    }
                    Button(onClick = onYearly, enabled = currentId != null && !buying && !pro, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.acc_pro_yearly))
                    }
                }
                Text(
                    stringResource(R.string.paywall_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}

// نقطة ميزة: ✓ + نص — تُبنى من أسطر acc_*_items (تُجرد • إن وجدت)
@Composable
private fun PlanBullet(raw: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val text = raw.trim().removePrefix("•").trim()
    if (text.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(
            "✓ ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

// صف حزمة تخزين: الاسم + زر شهري/سنوي — الشراء للمشترك Pro المسجل فقط
@Composable
private fun PackRow(
    name: String,
    monthly: String,
    yearly: String,
    enabled: Boolean,
    onMonthly: () -> Unit,
    onYearly: () -> Unit
) {
    Text(
        name,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp)
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onMonthly, enabled = enabled, modifier = Modifier.weight(1f)) {
            Text(monthly)
        }
        OutlinedButton(onClick = onYearly, enabled = enabled, modifier = Modifier.weight(1f)) {
            Text(yearly)
        }
    }
}

@Composable
private fun rememberDateFmt(): java.text.SimpleDateFormat {
    return androidx.compose.runtime.remember {
        java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
    }
}
