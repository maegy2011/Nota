package com.nota.notes.billing

// نتيجة عملية شراء — sealed لتغطية كل الحالات بلا استثناء صامت
sealed interface BillingResult {
    data class Success(val purchaseToken: String?) : BillingResult
    data object Cancelled : BillingResult
    data class Failed(val reason: String) : BillingResult
    data object Unavailable : BillingResult
}
