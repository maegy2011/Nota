package com.nota.notes.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nota.notes.data.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(private val store: SettingsStore) : ViewModel() {
    val theme = store.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val appTheme = store.appTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "warm")
    val lang = store.lang.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val biometricEnabled = store.biometricEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoLockMs = store.autoLockMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30_000L)
    val sortOrder = store.sortOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.SORT_UPDATED_DESC)

    fun setTheme(v: String) = viewModelScope.launch { store.setTheme(v) }
    fun setAppTheme(v: String) = viewModelScope.launch { store.setAppTheme(v) }

    fun setBiometricEnabled(v: Boolean) = viewModelScope.launch { store.setBiometricEnabled(v) }

    fun setAutoLockMs(v: Long) = viewModelScope.launch { store.setAutoLockMs(v) }

    fun setSortOrder(v: String) = viewModelScope.launch { store.setSortOrder(v) }

    val defaultColor = store.defaultColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val editorFont = store.editorFont.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.FONT_NORMAL)
    val cardDates = store.cardDates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autosave = store.autosave.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    // التأثيرات والحركة (28 سبتمبر 2026): مفتاح واحد يحكم الاهتزاز اللمسي وأنيميشن القوائم
    val effectsOn = store.effects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    fun setEffects(v: Boolean) = viewModelScope.launch { store.setEffects(v) }
    // "مستخدمة مؤخرًا" في شيت الإضافة (28 سبتمبر 2026): يخشيب بين الجلسات
    val sheetRecent = store.sheetRecent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun setSheetRecent(v: List<String>) = viewModelScope.launch { store.setSheetRecent(v) }
    // قوالب المستخدم (28 سبتمبر 2026): يحفظها من نص ملاحظة الحالي ويعيد إدراجها في أي ملاحظة
    val tplCustom = store.tplCustom.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun setTplCustom(v: List<Pair<String, String>>) = viewModelScope.launch { store.setTplCustom(v) }
    val onboardingDone = store.onboardingDone.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val screenshotsAllowed = store.screenshotsAllowed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setScreenshotsAllowed(v: Boolean) = viewModelScope.launch { store.setScreenshotsAllowed(v) }

    fun setOnboardingDone() = viewModelScope.launch { store.setOnboardingDone() }
    // مراجعة الصلاحيات عند أول تثبيت (27 سبتمبر 2026): تُعرض مرة واحدة بعد التعريف وقبل PIN
    val permsReviewed = store.permsReviewed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    fun setPermsReviewed() = viewModelScope.launch { store.setPermsReviewed() }

    fun setDefaultColor(v: Int) = viewModelScope.launch { store.setDefaultColor(v) }
    fun setEditorFont(v: String) = viewModelScope.launch { store.setEditorFont(v) }
    fun setCardDates(v: Boolean) = viewModelScope.launch { store.setCardDates(v) }
    fun setAutosave(v: Boolean) = viewModelScope.launch { store.setAutosave(v) }
    // الإتلاف الآمن (قرار 23 سبتمبر 2026): اختياري معطّل افتراضيًا — Pro فقط عند العرض
    val shredEnabled = store.shredEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val shredMethod = store.shredMethod.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.SHRED_SINGLE)
    // شريط التنقل السفلي + زر FAB (قرار 26 سبتمبر 2026): التغيير للمشترك فقط — تُفرض البوابة عند النداء
    val navSlots = store.navSlots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.DEFAULT_NAV_SLOTS)
    val fabVisible = store.fabVisible.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    fun setNavSlots(v: List<String>) = viewModelScope.launch { store.setNavSlots(v) }
    fun setFabVisible(v: Boolean) = viewModelScope.launch { store.setFabVisible(v) }
    fun setShredEnabled(v: Boolean) = viewModelScope.launch { store.setShredEnabled(v) }
    fun setShredMethod(v: String) = viewModelScope.launch { store.setShredMethod(v) }
    // P3-B3: opt-in صريح — معطّل افتراضيًا
    val crashOptIn = store.crashOptIn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val analyticsOptIn = store.analyticsOptIn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setCrashOptIn(v: Boolean) = viewModelScope.launch { store.setCrashOptIn(v) }
    fun setAnalyticsOptIn(v: Boolean) = viewModelScope.launch { store.setAnalyticsOptIn(v) }
    // ضمان التنبيه: عرض طلب استثناء البطارية مرة واحدة فقط
    val batteryPromptShown = store.batteryPromptShown.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setBatteryPromptShown() = viewModelScope.launch { store.setBatteryPromptShown() }
    // الترحيب والسرعة (27 سبتمبر 2026): الاسم + تفعيل الرسالة + الوضع السريع
    val username = store.username.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val welcomeEnabled = store.welcomeEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val quickMode = store.quickMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setUsername(v: String) = viewModelScope.launch { store.setUsername(v) }
    fun setWelcomeEnabled(v: Boolean) = viewModelScope.launch { store.setWelcomeEnabled(v) }
    fun setQuickMode(v: Boolean) = viewModelScope.launch { store.setQuickMode(v) }
    // التجربة المجانية (27 سبتمبر 2026): تُعرض أول استخدام فقط — والقبول يفتح Pro لمدة ثابتة
    val trialOffered = store.trialOffered.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val trialStart = store.trialStart.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    fun dismissTrialOffer() = viewModelScope.launch { store.dismissTrialOffer() }
    fun startTrial() = viewModelScope.launch { store.startTrial() }
    // الذكاء الاصطناعي BYOK (27 سبتمبر 2026): المفتاح يُختم بـ TokenVault قبل التخزين ولا يُقرأ مكشوفًا إلا عند الاستدعاء
    val aiEndpoint = store.aiEndpoint.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.nota.notes.data.AiRewriteService.DEFAULT_ENDPOINT)
    val aiModel = store.aiModel.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.nota.notes.data.AiRewriteService.DEFAULT_MODEL)
    val aiKeySet = store.aiKeySealed.map { it != null }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    // P0-1 (أكتوبر 2026): runCatching — require الـ https في المخزن يرمي، والانهيار هنا = تعليق الإعدادات.
    fun setAiEndpoint(v: String) = viewModelScope.launch { runCatching { store.setAiEndpoint(v) } }
    fun setAiModel(v: String) = viewModelScope.launch { store.setAiModel(v) }
    fun setAiKey(plain: String) = viewModelScope.launch {
        val t = plain.trim()
        if (t.isEmpty()) store.setAiKeySealed(null)
        else store.setAiKeySealed(com.nota.notes.security.TokenVault.seal(t))
    }
    suspend fun aiKeyPlain(): String? = store.aiKeySealed.first()?.let { com.nota.notes.security.TokenVault.unseal(it) }
    // اختبار الاتصال (تحسين 27 سبتمبر 2026): طلب ping مصغر — ناجح = المفتاح والخادم والنموذج سليمة
    fun testAi(onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val key = aiKeyPlain()
        if (key.isNullOrBlank()) { onDone(false); return@launch }
        val r = com.nota.notes.data.AiRewriteService.complete(
            aiEndpoint.first(), key, aiModel.first(),
            com.nota.notes.data.AiRewriteService.PING_PROMPT,
            com.nota.notes.data.AiRewriteService.PING_PROMPT
        )
        onDone(r.isSuccess)
    }

    // اللغات السبع عشرة (27 سبتمبر 2026): الوسم = كود اللغة نفسه — والافتراضي يتبع الجهاز
    fun setLang(v: String) = viewModelScope.launch {
        store.setLang(v)
        AppCompatDelegate.setApplicationLocales(
            when (v) {
                "ar", "en", "ru", "hi", "es", "it", "ja", "zh", "fr", "tr", "ms", "de", "ur",
                "nl", "pl", "pt", "uk" ->
                    LocaleListCompat.forLanguageTags(v)
                else -> LocaleListCompat.getEmptyLocaleList()
            }
        )
    }
}
