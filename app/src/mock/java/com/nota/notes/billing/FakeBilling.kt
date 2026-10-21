package com.nota.notes.billing

import javax.inject.Inject
import kotlinx.coroutines.delay

// نكهة mock فقط — الشراء ينجح فورًا بلا جوجل (صفر تكلفة، لا تُنشر)
class FakeBilling @Inject constructor() : BillingGateway {
    override suspend fun launchPurchase(productId: String): BillingResult {
        delay(300) // محاكاة زمن الشبكة — بلا runBlocking
        return if (productId in BillingGateway.ALL_PRODUCTS) {
            BillingResult.Success(purchaseToken = "fake-$productId-${System.currentTimeMillis()}")
        } else {
            BillingResult.Failed("unknown product: $productId")
        }
    }

    // 5أ: لا مشتريات سابقة في mock — تُستخدم فقط لاختبار مسار الاستعادة بلا عناصر
    override suspend fun queryPurchases(): List<PurchaseInfo> = emptyList()

    // 5أ: محاكاة نجاح الإبلاغ — السيرفر الحقيقي (POST /v1/purchases) غير موجود بعد
    override suspend fun reportPurchase(purchaseToken: String, productId: String): Result<Unit> =
        Result.success(Unit)
}
