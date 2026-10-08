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

    // التجربة المجانية (27 سبتمبر 2026): تُغذَّى من الإعدادات — تفتح كل البوابات كـ Pro حتى انتهائها
    private val _trialActive = MutableStateFlow(false)
    val trialActive: StateFlow<Boolean> = _trialActive.asStateFlow()
    fun setTrialActive(v: Boolean) { _trialActive.value = v }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.refresh() }
                .onSuccess { _entitlement.value = it; _authExpired.value = false }
                .onFailure { e ->
                    if (e is AuthExpiredException) _authExpired.value = true
                    else _entitlement.value = Entitlement.FREE
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
                .onSuccess { _entitlement.value = it; _authExpired.value = false }
                .onFailure { e -> if (e is AuthExpiredException) _authExpired.value = true }
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
                runCatching { repo.refresh() }.onSuccess { _entitlement.value = it }
            }
            _buying.value = false
        }
    }
}
