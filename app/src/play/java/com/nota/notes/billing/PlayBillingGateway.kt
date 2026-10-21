package com.nota.notes.billing

import javax.inject.Inject

// نكهة play — stub حقيقي الشكل: بلا Play Billing بعد (يُربط عند الإطلاق)، يرد غير متاح
class PlayBillingGateway @Inject constructor() : BillingGateway {
    override suspend fun launchPurchase(productId: String): BillingResult =
        BillingResult.Unavailable

    // 5أ: لا استعادة قبل ربط العميل الحقيقي — تُنفَّذ مع BillingClient (المرحلة 5ج)
    override suspend fun queryPurchases(): List<PurchaseInfo> = emptyList()

    // 5أ: الإبلاغ مستحيل بلا إيصال حقيقي — يُفعَّل مع POST /v1/purchases (backend)
    override suspend fun reportPurchase(purchaseToken: String, productId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("play billing not wired"))

    // TODO: Implement real BillingClient.
    // Requires:
    // 1. dependency: com.android.billingclient:billing:7.0.0
    // 2. BillingClient initialization with PurchasesUpdatedListener
    // 3. launchBillingFlow with productId
    // 4. Handle onPurchasesUpdated → return Success(token)
    // See: https://developer.android.com/google/play/billing/integrate
}
