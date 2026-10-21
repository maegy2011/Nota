package com.nota.notes.data

// P3-B2 — حذف الحساب: منطق نقي يُختبر على JVM (بلا Context).
// فترة السماح 14 يومًا — خلالها يُعرض "حسابك في طريقه للحذف — X أيام متبقية" + زر إلغاء.
// بعدها الحذف النهائي عبر WorkManager (فحص دوري).
// ملاحظة backend موثقة: endpoint حذف الحساب (DELETE /v1/account) غير موجود سيرفريًا بعد —
// التطبيق يستدعيه best-effort من DeletionWorker (6أ: NotaCloudApi.deleteAccount) ولا يتوقف على فشله.
// يُطبَّق المحلي + Firebase فقط، والسحابة "تنتظر backend".
object AccountDeletion {
    const val GRACE_MS: Long = 14L * 24 * 3600 * 1000
    const val CONFIRM_PHRASE_AR = "احذف حسابي"

    // الأيام المتبقية (تقريب لأعلى) — 0 تعني انتهت المهلة.
    fun remainingDays(requestedAt: Long, now: Long = System.currentTimeMillis()): Int {
        if (requestedAt <= 0L) return 0
        val left = requestedAt + GRACE_MS - now
        if (left <= 0L) return 0
        return ((left + 24 * 3600 * 1000 - 1) / (24 * 3600 * 1000)).toInt().coerceAtMost(14)
    }

    fun isDue(requestedAt: Long, now: Long = System.currentTimeMillis()): Boolean =
        requestedAt > 0L && now >= requestedAt + GRACE_MS

    fun isPending(requestedAt: Long, now: Long = System.currentTimeMillis()): Boolean =
        requestedAt > 0L && !isDue(requestedAt, now)

    // التحقق من عبارة التأكيد — مطابقة حرفية بعد trim (لا تساهل: حذف حساب قرار مصيري).
    fun isConfirmValid(input: String): Boolean =
        input.trim() == CONFIRM_PHRASE_AR
}
