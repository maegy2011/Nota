package com.nota.notes.billing

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// نكهة mock فقط — هذا الملف غير موجود في مخرجات play أصلًا (source set منفصل)
@Module
@InstallIn(SingletonComponent::class)
abstract class MockBillingModule {
    @Binds abstract fun billing(g: FakeBilling): BillingGateway
    @Binds abstract fun integrity(c: FakeIntegrity): IntegrityCheck
    @Binds abstract fun entitlements(a: FakeEntitlementsApi): EntitlementsApi
}
