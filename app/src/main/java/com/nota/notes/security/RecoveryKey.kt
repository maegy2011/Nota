package com.nota.notes.security

import java.security.SecureRandom

/**
 * مفتاح الاسترداد — P3-A2 (أكتوبر 2026).
 *
 * قرار معماري موثق: 12 كلمة من قائمة BIP39 الإنجليزية الرسمية (2048 كلمة،
 * `Bip39English.kt`) — معيار موجود، سهل النسخ، عابر للغات. 12×11 = 132 بت
 * إنتروبي. تُخزَّن hash فقط (PBKDF2-HMAC-SHA256، 310k دورة، نفس PinHash،
 * salt مستقل) — لا تُخزَّن الكلمات نفسها أبدًا. لهذا يستحيل "عرض 3 كلمات
 * للتحقق" لاحقًا: التحقق (إعدادًا وإدارةً واستردادًا) بإدخال العبارة كاملة
 * مقابل الـ hash. مفاتيح الجيل السابق (قائمة 256 كلمة) ما زالت تعمل —
 * التحقق hash فقط ولا يشترط الانتماء للقائمة.
 *
 * UX الإدخال: حقل واحد كبير (12 كلمة مفصولة بمسافة) — قرار موثق: حقل واحد
 * أسهل للصق من مدير كلمات المرور من 12 حقلًا. الفشل يُحتسب في الدرع بعد
 * إدخال 12 كلمة كاملة — لا بعد كل كلمة. لا كشف جزئي: رسالة عامة فقط.
 */
object RecoveryKey {
    // القائمة الرسمية الكاملة — التوليد الجديد منها حصرًا
    private val WORDS: List<String> = BIP39_ENGLISH

    init {
        check(WORDS.size == 2048) { "BIP39 English list must hold exactly 2048 words" }
        check(WORDS.distinct().size == WORDS.size) { "wordlist words must be unique" }
    }

    fun generate(): List<String> {
        val rnd = SecureRandom()
        return List(12) { WORDS[rnd.nextInt(WORDS.size)] }
    }

    fun normalize(input: String): String =
        input.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ")

    fun wordCount(normalized: String): Int =
        if (normalized.isBlank()) 0 else normalized.split(" ").size

    /** 3 فهارس عشوائية مميزة للتحقق من الحفظ */
    fun quizIndices(): List<Int> {
        val rnd = SecureRandom()
        return (0 until 12).shuffled(rnd).take(3).sorted()
    }
}
