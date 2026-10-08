package com.nota.notes.billing

import javax.inject.Inject

// نكهة mock فقط — دائمًا سليم (Play Integrity الحقيقي مستحيل بلا حساب مطور)
class FakeIntegrity @Inject constructor() : IntegrityCheck {
    override suspend fun check(): Boolean = true
}
