package com.nota.notes.ui

import com.nota.notes.R

// P3-C3: بحث الإعدادات — فهرس عناوين وكلمات مفتاحية (ar+en) بلا قيم حساسة.
// القاعدة: الفهرس يحوي العناوين والأسماء فقط — لا تُفهرس أبدًا قيم الإعدادات
// (مفاتيح AI، كلمات مرور، عبارات الاسترداد). المطابقة نصية خالصة (JVM).
enum class SettingsRoute { ROOT, SECURITY, APPEARANCE, EXPERIENCE, AI, NOTIF, ADVANCED }

data class SettingSearchEntry(
    val route: SettingsRoute,
    val titleRes: Int,
    val keywords: String
)

// جدول التوجيه: كل قسم فرعي → مساره. الكلمات ar+en معًا ليعمل البحث بأي لغة.
fun defaultSettingsSearchEntries(): List<SettingSearchEntry> = listOf(
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.security_title, "الأمان security pin بصمة biometric قفل lock"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.changepin_title, "تغيير PIN change pin كلمة مرور password"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.reset_pin_title, "إعادة ضبط PIN reset forgot نسيت"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.bio_title, "بصمة biometric بصمات fingerprint"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.reckey_status_on, "مفتاح الاسترداد recovery key كلمات words"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.devcred_title, "قفل الجهاز device credential جهاز device"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.autolock_title, "القفل التلقائي auto lock مهلة timeout"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.quickmode_title, "الفتح السريع quick mode تجاوز"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.shots_title, "لقطات الشاشة screenshots منع block"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.shred_title, "الإتلاف الآمن secure delete shred مسح"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.privacy_title, "الخصوصية privacy تصدير export حذف حساب delete بيانات data"),
    SettingSearchEntry(SettingsRoute.SECURITY, R.string.sec_status_title, "حالة الأمان security status أمان"),
    SettingSearchEntry(SettingsRoute.APPEARANCE, R.string.theme_title, "الوضع المظهر theme فاتح داكن light dark"),
    SettingSearchEntry(SettingsRoute.APPEARANCE, R.string.app_theme_title, "الثيمات الألوان colors themes هوية"),
    SettingSearchEntry(SettingsRoute.APPEARANCE, R.string.lang_title, "اللغة language لغة عربي english"),
    SettingSearchEntry(SettingsRoute.APPEARANCE, R.string.font_title, "الخط font حجم size"),
    SettingSearchEntry(SettingsRoute.APPEARANCE, R.string.defcolor_title, "اللون الافتراضي default color لون"),
    SettingSearchEntry(SettingsRoute.EXPERIENCE, R.string.navbar_customize_row, "شريط التنقل nav bar تخصيص customize"),
    SettingSearchEntry(SettingsRoute.EXPERIENCE, R.string.welcome_sec, "الترحيب welcome تحية اسم name"),
    SettingSearchEntry(SettingsRoute.EXPERIENCE, R.string.write_title, "المحرر الكتابة editor كتابة"),
    SettingSearchEntry(SettingsRoute.EXPERIENCE, R.string.fx_title, "التأثيرات effects اهتزاز animation حركة"),
    SettingSearchEntry(SettingsRoute.EXPERIENCE, R.string.autosave_title, "الحفظ التلقائي autosave حفظ save"),
    SettingSearchEntry(SettingsRoute.EXPERIENCE, R.string.carddates_title, "التواريخ dates تواريخ بطاقات"),
    SettingSearchEntry(SettingsRoute.AI, R.string.ai_title, "الذكاء الاصطناعي ai نموذج model مفتاح endpoint خادم"),
    SettingSearchEntry(SettingsRoute.NOTIF, R.string.health_title, "التنبيهات الإشعارات notifications صحة health منبه alarm بطارية battery"),
    SettingSearchEntry(SettingsRoute.ADVANCED, R.string.adv_access_log, "سجل الوصول access log محاولات attempts"),
    SettingSearchEntry(SettingsRoute.ADVANCED, R.string.adv_version, "الإصدار version نسخة about")
)

// مطابقة خالصة: استعلام أقصر من حرفين → فارغ (يمنع ضجيج حرف واحد).
// تُرجع الإدخالات المطابقة بلا تكرار، مرتبة بتسلسل الفهرس (الأهم أولًا).
fun searchSettings(
    query: String,
    entries: List<SettingSearchEntry> = defaultSettingsSearchEntries()
): List<SettingSearchEntry> {
    val q = query.trim().lowercase()
    if (q.length < 2) return emptyList()
    val out = LinkedHashMap<Pair<SettingsRoute, Int>, SettingSearchEntry>()
    for (e in entries) {
        if (e.keywords.lowercase().contains(q)) out[e.route to e.titleRes] = e
    }
    return out.values.toList()
}
