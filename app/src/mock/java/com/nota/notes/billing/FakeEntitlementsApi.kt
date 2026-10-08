package com.nota.notes.billing

import javax.inject.Inject
import kotlinx.coroutines.delay

// نكهة mock فقط — يرد Pro كاملًا بلا سيرفر (الشراء الوهمي يفتح المزايا فورًا للتجربة)
class FakeEntitlementsApi @Inject constructor() : EntitlementsApi {
    override suspend fun getEntitlements(accountId: String): Entitlement {
        delay(200) // محاكاة زمن الشبكة — بلا runBlocking
        return Entitlement(
            pro = true,
            features = listOf(Entitlement.FEATURE_FOLDERS),
            expiresAt = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L
        )
    }
}
