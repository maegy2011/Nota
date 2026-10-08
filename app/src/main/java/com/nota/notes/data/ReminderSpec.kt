package com.nota.notes.data

// قاعدة التكرار المخصص للتذكير — كما في تطبيقات التذكير الكبرى:
// تكرار بسيط (يومي/أسبوعي/شهري/سنوي كل فترة) أو مخصص (أيام معينة، نمط شهري، نهاية)
// تُخزن في أعمدة الملاحظة (v12) وتُنسخ مع النسخة الاحتياطية — التوافق محفوظ بقيم افتراضية
data class ReminderSpec(
    val at: Long? = null,
    val repeat: String = "none", // none|daily|weekly|monthly|yearly
    val beforeMin: Long = 0L,
    val interval: Int = 1, // كل N (يوم/أسبوع/شهر/سنة) — بحد 1..99
    val days: String = "", // أيام الأسبوع CSV للأسبوعي (1=الأحد..7=السبت) — فاضي = نفس اليوم
    val monthMode: String = MONTH_SAME, // same|nth — الشهري: نفس اليوم أو نفس الترتيب (الثلاثاء الرابع)
    val end: String = END_FOREVER, // forever|until|count
    val endValue: Long = 0L // until: تاريخ بالمللي | count: الأحداث المتبقية (تتناقص كل انطلاق)
) {
    companion object {
        const val MONTH_SAME = "same"
        const val MONTH_NTH = "nth"
        const val END_FOREVER = "forever"
        const val END_UNTIL = "until"
        const val END_COUNT = "count"

    }

    // هل القاعدة مخصصة فعلًا (تستحق وصفًا مفصلًا) أم تكرار بسيط؟ — دالة عضوية (لا استيراد خاص)
    fun isCustom(): Boolean =
        interval > 1 || days.isNotBlank() || monthMode != MONTH_SAME || end != END_FOREVER
}
