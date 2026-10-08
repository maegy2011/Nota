package com.nota.notes.billing

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// نكهة play — روابط الإنتاج الحية (سؤال السيرفر)؛ لا Fake هنا أبدًا
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayBillingModule {
    @Binds abstract fun billing(g: PlayBillingGateway): BillingGateway
    @Binds abstract fun integrity(c: PlayIntegrityCheck): IntegrityCheck
    @Binds abstract fun entitlements(a: HttpEntitlementsApi): EntitlementsApi
}
