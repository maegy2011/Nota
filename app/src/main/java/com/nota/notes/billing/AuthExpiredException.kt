package com.nota.notes.billing

import java.io.IOException

// جلسة الدخول انتهت على الجهاز (لا توكن مخزن صالح ولا تجديد من Firebase) —
// الحل إعادة الدخول، لا الصمت: تُرمى صراحةً لتظهر الواجهة طلب إعادة الدخول
// بدل التعليق على loading أو السقوط الصامت للـ FREE (درس 27 سبتمبر 2026).
class AuthExpiredException : IOException("auth session expired")
