package com.nota.notes.billing

// فحص السلامة — جوجل تشهد أن التطبيق أصلي على جهاز سليم (Play Integrity لاحقًا)
interface IntegrityCheck {
    // true = التطبيق سليم، false = مرفوض (نسخة معدّلة/جهاز مخترق)
    suspend fun check(): Boolean
}
