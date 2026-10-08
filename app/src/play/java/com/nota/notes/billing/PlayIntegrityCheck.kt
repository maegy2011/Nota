package com.nota.notes.billing

import javax.inject.Inject

// نكهة play — stub: بلا Play Integrity بعد (مستحيل بلا حساب مطور)، يرد سليمًا محليًا
// التحقق الحقيقي سيكون على السيرفر عند الإطلاق
class PlayIntegrityCheck @Inject constructor() : IntegrityCheck {
    override suspend fun check(): Boolean = true
}
