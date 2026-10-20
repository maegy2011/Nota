package com.nota.notes.billing

import javax.inject.Inject

// نكهة play — stub حقيقي الشكل: بلا Play Billing بعد (يُربط عند الإطلاق)، يرد غير متاح
class PlayBillingGateway @Inject constructor() : BillingGateway {
    override suspend fun launchPurchase(productId: String): BillingResult =
        BillingResult.Unavailable
}
