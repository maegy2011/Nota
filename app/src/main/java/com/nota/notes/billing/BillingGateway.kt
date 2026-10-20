package com.nota.notes.billing

// بوابة الفوترة — التطبيق يبني ضد هذه الواجهة فقط، لا ضد جوجل مباشرة.
// النسخة الحقيقية (Play Billing) تُحقن في نكهة play لاحقًا، والوهمية في mock فقط.
interface BillingGateway {
    // إطلاق تدفق الشراء لمنتج — تُرجع النتيجة بلا رمي استثناء
    suspend fun launchPurchase(productId: String): BillingResult

    companion object {
        const val PRODUCT_PRO_MONTHLY = "nota_pro_monthly"
        const val PRODUCT_PRO_YEARLY = "nota_pro_yearly"
        const val PRODUCT_LIFETIME = "nota_lifetime"
        // حزم التخزين الإضافية: للمشتركين Pro فأعلى — تُجمع فوق سعة الباقة (السيرفر يجمع storage_* تلقائيًا)
        const val PRODUCT_STORAGE_5G_MONTHLY = "nota_storage_5g_monthly"
        const val PRODUCT_STORAGE_5G_YEARLY = "nota_storage_5g_yearly"
        const val PRODUCT_STORAGE_10G_MONTHLY = "nota_storage_10g_monthly"
        const val PRODUCT_STORAGE_10G_YEARLY = "nota_storage_10g_yearly"
        val ALL_PRODUCTS = listOf(PRODUCT_PRO_MONTHLY, PRODUCT_PRO_YEARLY, PRODUCT_LIFETIME,
            PRODUCT_STORAGE_5G_MONTHLY, PRODUCT_STORAGE_5G_YEARLY,
            PRODUCT_STORAGE_10G_MONTHLY, PRODUCT_STORAGE_10G_YEARLY)
    }
}
