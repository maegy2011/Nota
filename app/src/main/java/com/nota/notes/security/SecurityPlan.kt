package com.nota.notes.security

// V1: SQLCipher + Keystore + Biometric/PIN + auto-lock 30s
// مبدأ مؤسس (23 سبتمبر 2026 — بأمر محمد): المعرفة الصفرية — التشفير وفك التشفير على جهاز
// المستخدم حصرًا بمفتاحه الخاص؛ السيرفر مخزن أعمى لا يرى بايتًا مفكوكًا أبدًا (نصًا ووسائط).
// PIN الشاشة قفل فقط ولا يدخل في أي تشفير سحابي.
// ترقية 22 سبتمبر 2026 (كود فقط): PBKDF2 بـ 310k دورة (OWASP) + تشفير ملفات الوسائط AES-GCM بمفتاح
// مستقل + مسح passphrase من الذاكرة + cipher_memory_security + عارض خارجي بنسخة مؤقتة فقط
// V2 (مدفوع): E2EE sync zero-knowledge — blobs مشفرة فقط على السيرفر
object SecurityPlan
