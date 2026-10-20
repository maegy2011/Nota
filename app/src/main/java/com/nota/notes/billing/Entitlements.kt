package com.nota.notes.billing

// الاستحقاق كما يرده السيرفر: ماذا يملك هذا الحساب؟
// القاعدة: لا isPremium محفوظ محليًا أبدًا — يُسأل من السيرفر مع كاش قصير للأوفلاين
data class Entitlement(
    val pro: Boolean = false,
    val features: List<String> = emptyList(),
    val expiresAt: Long? = null
) {
    companion object {
        const val FEATURE_FOLDERS = "folders"
        const val FEATURE_LOCATION = "location"
        // كل مزايا ما بعد إغلاق المجانية (قرار 20 سبتمبر 2026): مدفوعة — تُفتح بـ pro من السيرفر
        const val FEATURE_TAGS = "tags"
        const val FEATURE_REMINDERS = "reminders"
        const val FEATURE_PDF = "pdf"
        const val FEATURE_LOCK = "lock"
        const val FEATURE_FAVORITES = "favorites"
        const val FEATURE_PIN = "pin"
        const val FEATURE_WIDGET = "widget"
        const val FEATURE_CLOUD = "cloud"
        const val FEATURE_BACKGROUNDS = "backgrounds"
        const val FEATURE_MEDIA = "media"
        const val FEATURE_SHRED = "shred"
        val FREE = Entitlement(pro = false)
    }
}

// أسماء المزايا المعروفة — أولها المجلدات
fun hasFeature(features: List<String>, name: String): Boolean = features.contains(name)

// واجهة سؤال الاستحقاق — التطبيق يسأل GET /v1/entitlements (سيرفر VPS لاحقًا)
interface EntitlementsApi {
    suspend fun getEntitlements(accountId: String): Entitlement
}

// بوابة PRO: السيرفر يرد pro:true لأي حساب له صف استحقاق ساري (isProRow) — المدفوعة كلها تُفتح به
fun isPro(e: Entitlement?): Boolean = e?.pro == true
