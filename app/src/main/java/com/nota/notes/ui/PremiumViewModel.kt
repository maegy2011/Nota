package com.nota.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nota.notes.billing.AuthExpiredException
import com.nota.notes.billing.BillingGateway
import com.nota.notes.billing.BillingResult
import com.nota.notes.billing.Entitlement
import com.nota.notes.billing.EntitlementsRepo
import com.nota.notes.billing.hasFeature
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// المرحلة 1أ: مصدر الاستحقاق — "من أين جاء Pro؟" يُعرض في شاشة الحساب.
// SERVER = pro من السيرفر عبر force، CACHED = pro من الكاش عبر refresh، TRIAL = تجربة نشطة.
enum class EntitlementSource {
    SERVER,
    TRIAL,
    CACHED,
    AUTH_EXPIRED,
    NONE
}

// حالة الترقية للـ Paywall — بلا runBlocking، كل شيء معلّق
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val repo: EntitlementsRepo,
    private val gateway: BillingGateway
) : ViewModel() {
    private val _entitlement = MutableStateFlow<Entitlement?>(null)
    val entitlement: StateFlow<Entitlement?> = _entitlement.asStateFlow()

    private val _buying = MutableStateFlow(false)
    val buying: StateFlow<Boolean> = _buying.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    // الجلسة منتهية على الجهاز: تُعرض الواجهة طلب إعادة الدخول بدل السقوط الصامت للـ FREE
    private val _authExpired = MutableStateFlow(false)
    val authExpired: StateFlow<Boolean> = _authExpired.asStateFlow()

    private val _lastResult = MutableStateFlow<BillingResult?>(null)
    val lastResult: StateFlow<BillingResult?> = _lastResult.asStateFlow()

    // 5ب: التفعيل مؤجل — الشراء نجح لكن إبلاغ السيرفر فشل (يُعرض مرة واحدة ثم يُستهلَك)
    private val _purchaseDeferred = MutableStateFlow(false)
    val purchaseDeferred: StateFlow<Boolean> = _purchaseDeferred.asStateFlow()
    fun clearPurchaseDeferred() { _purchaseDeferred.value = false }

    // M4 (المرحلة 0أ-3): تُمسَح بعد عرضها للمستخدم — تُستهلَك مرة واحدة فقط
    fun clearLastResult() { _lastResult.value = null }

    // المرحلة 1أ: مصدر Pro الحالي — يُحدَّث مع كل refresh/forceRefresh/setTrialActive
    private val _source = MutableStateFlow(EntitlementSource.NONE)
    val source: StateFlow<EntitlementSource> = _source.asStateFlow()

    // يحسب المصدر من الحالة الحالية — التجربة أولًا (تغلب على الكل).
    // fromServer=true فقط في forceRefresh (تجاوز الكاش) — وإلا CACHED.
    private fun resolveSource(fromServer: Boolean): EntitlementSource = when {
        _trialActive.value -> EntitlementSource.TRIAL
        _entitlement.value?.pro == true -> if (fromServer) EntitlementSource.SERVER else EntitlementSource.CACHED
        else -> EntitlementSource.NONE
    }

    // 6ج: trial سيرفري يغلب المحلي — إن استُهلكت التجربة على السيرفر، لا تجربة محلية أبدًا.
    // تُستدعى بعد كل استحقاق جديد وقبل أي تفعيل محلي (fail-closed للتجربة فقط، لا للشراء).
    private fun applyServerTrial(ent: Entitlement) {
        if (ent.trialUsed && _trialActive.value) {
            _trialActive.value = false
            _source.value = resolveSource(fromServer = false)
        }
    }

    // المصدر الوحيد للتعريف، بلا تكرار لمنطق OR في أي مكان آخر.
    fun isProNow(): Boolean = _trialActive.value || _entitlement.value?.pro == true

    // اسم مستعار واضح: النسخ الاحتياطي يستخدم نفس تعريف isPro تمامًا (T3 مغلق — التجربة تفتح ncPro).
    fun isProForBackup(): Boolean = isPro()

    // التجربة المجانية (27 سبتمبر 2026): تُغذَّى من الإعدادات — تفتح كل البوابات كـ Pro حتى انتهائها
    private val _trialActive = MutableStateFlow(false)
    val trialActive: StateFlow<Boolean> = _trialActive.asStateFlow()
    fun setTrialActive(v: Boolean) {
        // 6ج: لا تجربة محلية إن حسمها السيرفر مستهلكة — يُحترم trialUsed فوق رغبة الواجهة.
        _trialActive.value = if (v && _entitlement.value?.trialUsed == true) false else v
        // المرحلة 1أ: بدء التجربة = TRIAL، انتهاؤها = إعادة الحساب من الاستحقاق المخزن
        _source.value = resolveSource(fromServer = false)
    }

    init {
        refresh()
        // 5ب: استعادة تلقائية للمشتريات عند بدء التطبيق — فارغة في mock (FakeBilling ترد [])
        // فلا تُحدِث أي شبكة إلا بوجود مشتريات حقيقية تُبلَّغ أولًا
        viewModelScope.launch {
            val purchases = runCatching { gateway.queryPurchases() }.getOrDefault(emptyList())
            if (purchases.isNotEmpty()) {
                val ok = runCatching {
                    purchases.forEach { gateway.reportPurchase(it.purchaseToken, it.productId).getOrThrow() }
                }.isSuccess
                if (ok) runCatching { repo.refresh(force = true) }.onSuccess { _entitlement.value = it }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.refresh() }
                .onSuccess { _entitlement.value = it; _authExpired.value = false; applyServerTrial(it); _source.value = resolveSource(fromServer = false) }
                .onFailure { e ->
                    if (e is AuthExpiredException) { _authExpired.value = true; _source.value = EntitlementSource.AUTH_EXPIRED }
                    else { _entitlement.value = Entitlement.FREE; _source.value = resolveSource(fromServer = false) }
                }
        }
    }

    // تحديث الحالة من السيرفر متجاهلًا الكاش — بعد الدفع يحتاج المشتري هذا طبيعيًا.
    // عند الفشل يُبقَى آخر استحقاق معروف (لا يُسقَط للـ FREE ظلمًا في الأوفلاين).
    fun forceRefresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            runCatching { repo.refresh(force = true) }
                .onSuccess { _entitlement.value = it; _authExpired.value = false; applyServerTrial(it); _source.value = resolveSource(fromServer = true) }
                .onFailure { e -> if (e is AuthExpiredException) { _authExpired.value = true; _source.value = EntitlementSource.AUTH_EXPIRED } }
            _refreshing.value = false
        }
    }

    fun canUseFolders(): Boolean {
        if (_trialActive.value) return true
        val e = _entitlement.value ?: return false
        return e.pro || hasFeature(e.features, Entitlement.FEATURE_FOLDERS)
    }

    // بوابة PRO العامة: كل مزايا ما بعد إغلاق المجانية (وسوم/تذكير/PDF/قفل خاص/مفضلة/تثبيت/ويدجت/سحابة/خلفيات/وسائط)
    fun isPro(): Boolean = _trialActive.value || _entitlement.value?.pro == true

    // الموقع المرفق ميزة مدفوعة — pro أو علم location من السيرفر
    fun canUseLocation(): Boolean {
        if (_trialActive.value) return true
        val e = _entitlement.value ?: return false
        return e.pro || hasFeature(e.features, Entitlement.FEATURE_LOCATION)
    }

    fun purchase(productId: String) {
        if (_buying.value) return
        viewModelScope.launch {
            _buying.value = true
            val r = runCatching { gateway.launchPurchase(productId) }
                .getOrElse { BillingResult.Failed(it.message ?: "error") }
            _lastResult.value = r
            if (r is BillingResult.Success) {
                // 5ب: إبلاغ السيرفر بالإيصال قبل الاعتماد — بلا إيصال مُبلَّغ لا Pro سيرفري
                val token = r.purchaseToken
                val reported = if (!token.isNullOrBlank()) {
                    runCatching { gateway.reportPurchase(token, productId).getOrThrow() }.isSuccess
                } else false
                if (reported) {
                    // C3 (المرحلة 0أ-2): force وإلا قد يعيد كاش FREE طازج بعد دفع ناجح
                    runCatching { repo.refresh(force = true) }.onSuccess { _entitlement.value = it }
                } else {
                    // الدفع تم لكن التفعيل مؤجل — يُعرض للمستخدم مرة واحدة (snackbar مؤجل)
                    _purchaseDeferred.value = true
                }
            }
            _buying.value = false
        }
    }
}
