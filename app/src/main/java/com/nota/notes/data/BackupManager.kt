package com.nota.notes.data

import android.content.Context
import android.util.Base64
import com.nota.notes.security.KeyManager
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object BackupManager {
    // ترويسة واحدة للكل (27 سبتمبر 2026): NOTA + سطر النوع — والصيغ القديمة NOTA1/2/3 محذوفة نهائيًا
    private const val HEADER = "NOTA"
    private const val KIND_PW = "PW" // مشفرة بكلمة مرور (PBKDF2 + AES-GCM)
    private const val KIND_KS = "KS" // مشفرة بمفتاح الجهاز (نسخ الأمان الداخلية فقط)
    const val ITERS = 600_000

    // نسخ احتياطي مشفر: JSON الملاحظات → AES-GCM بمفتاح Keystore **مستقل عن مفتاح قاعدة البيانات** → ملف .nota
    fun export(ctx: Context, json: String): File {
        val key = KeyManager.getOrCreateBackupKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val enc = cipher.doFinal(json.toByteArray())
        val out = File(ctx.cacheDir, "nota-${System.currentTimeMillis()}.nota")
        val body = HEADER + "\n" + KIND_KS + "\n" +
            Base64.encodeToString(iv, Base64.NO_WRAP) + "\n" +
            Base64.encodeToString(enc, Base64.NO_WRAP)
        atomicWrite(out, body)
        return out
    }

    // نسخة بمفتاح الجهاز في مجلد مخصص (نسخ الأمان التلقائية) — نفس الصيغة الموحدة
    fun export(dir: File, json: String): File {
        dir.mkdirs()
        val key = KeyManager.getOrCreateBackupKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val body = HEADER + "\n" + KIND_KS + "\n" +
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + "\n" +
            Base64.encodeToString(cipher.doFinal(json.toByteArray()), Base64.NO_WRAP)
        return File(dir, "nota-${System.currentTimeMillis()}.nota").also { atomicWrite(it, body) }
    }

    fun import(ctx: Context, file: File, jsonOut: (String) -> Unit, onError: (String) -> Unit) {
        try {
            val lines = file.readText().split("\n")
            if (lines.size == 4 && lines[0] == HEADER && lines[1] == KIND_KS)
                jsonOut(decrypt(KeyManager.getOrCreateBackupKey(), lines[2], lines[3]))
            else { onError("ملف النسخة تالف"); return }
        } catch (e: Exception) { onError("فشل الاستعادة: كلمة السر أو الملف غلط") }
    }

    // كتابة ذرية (27 سبتمبر 2026): ملف مؤقت + إعادة تسمية — لا ملف نصف مكتوب أبدًا
    private fun atomicWrite(file: File, text: String) {
        val tmp = File(file.parent, "${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            runCatching { tmp.delete() }
            file.writeText(text) // احتياطي نادر (عبر الأنظمة) — أفضل من الفشل الصامت
        }
    }

    private fun decrypt(key: javax.crypto.SecretKey, ivB64: String, encB64: String): String {
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        val enc = Base64.decode(encB64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(enc))
    }

    private const val GCM_TAG_BITS = 128

    // نوع النسخة من محتواها — قبل طلب كلمة المرور
    enum class BackupKind { PASSWORD, KEYSTORE, CORRUPT }

    fun peek(file: File): BackupKind = try {
        val lines = file.readText().split("\n")
        when {
            lines.size == 6 && lines[0] == HEADER && lines[1] == KIND_PW -> BackupKind.PASSWORD
            lines.size == 4 && lines[0] == HEADER && lines[1] == KIND_KS -> BackupKind.KEYSTORE
            else -> BackupKind.CORRUPT
        }
    } catch (e: Exception) { BackupKind.CORRUPT }

    // P1-B (أكتوبر 2026): حد أدنى 8 أحرف + فحص قوة في الـ backend — أي مسار (UI/اختبار/مستقبلي)
    // يمر من هنا. فك التشفير أدناه بلا فحص عمدًا (توافق خلفي مع ملفات قديمة بكلمة 4 أحرف).
    const val MIN_PASSWORD_LEN = 8

    fun isStrongPassword(password: String): Boolean {
        val lower = password.lowercase()
        // كلمات شائعة مرفوضة (الفحص على الصيغة الصغرى)
        val common = setOf(
            "password", "12345678", "qwertyui", "admin123", "letmein1",
            "welcome1", "abc12345", "passw0rd", "iloveyou", "monkey12",
            "dragon12", "master12", "shadow12", "sunshine", "princess",
            "football", "baseball", "11111111", "00000000", "123456789",
            "aaaaaaaa", "abcdefgh", "87654321"
        )
        if (lower in common) return false
        // حرف واحد مكرر: "aaaaaaaa"
        if (password.toSet().size == 1) return false
        // متتاليات واضحة: "12345678" / "abcdefgh" (صعودًا أو هبوطًا)
        if (isSequential(lower)) return false
        return true
    }

    private fun isSequential(s: String): Boolean {
        if (s.length < 2) return false
        val ascending = s.zipWithNext().all { (a, b) -> b.code == a.code + 1 }
        val descending = s.zipWithNext().all { (a, b) -> b.code == a.code - 1 }
        return ascending || descending
    }

    // غلاف النقل المشفر (27 سبتمبر 2026): نفس صيغة الملف كنص — لرفع Drive المشفر بكلمة المرور الثابتة
    fun encryptEnvelope(json: String, password: String): String {
        require(password.length >= MIN_PASSWORD_LEN) {
            "password must be at least $MIN_PASSWORD_LEN characters"
        }
        require(isStrongPassword(password)) { "password is too weak" }
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt, ITERS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return HEADER + "\n" + KIND_PW + "\n" + ITERS + "\n" +
            Base64.encodeToString(salt, Base64.NO_WRAP) + "\n" +
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + "\n" +
            Base64.encodeToString(cipher.doFinal(json.toByteArray()), Base64.NO_WRAP)
    }

    // فك الغلاف — يرمي عند كلمة غلط أو نص تالف (الـ GCM يكشف العبث)
    // P1-B: بلا فحص حد أدنى عمدًا — ملف قديم بكلمة 4 أحرف يجب أن يُستعاد (توافق خلفي).
    fun decryptEnvelope(text: String, password: String): String {
        require(password.isNotEmpty()) { "password required" }
        val lines = text.split("\n")
        if (lines.size != 6 || lines[0] != HEADER || lines[1] != KIND_PW) throw IllegalArgumentException("bad format")
        val iters = lines[2].toIntOrNull()?.coerceIn(100_000, 2_000_000)
            ?: throw IllegalArgumentException("bad iters")
        return decrypt(deriveKey(password, Base64.decode(lines[3], Base64.NO_WRAP), iters), lines[4], lines[5])
    }

    // هل هذا النص غلاف مشفر أم JSON قديم مكشوف؟ — لمسار ترحيل نسخ Drive القديمة
    fun isEnvelope(text: String): Boolean {
        val i = text.indexOf('\n')
        return i > 0 && text.substring(0, i) == HEADER
    }

    // تصدير مشفر بكلمة مرور: مفتاح يُشتق بـ PBKDF2 (ملح عشوائي + 600 ألف دورة) —
    // الملف وحده بلا قيمة، وكلمة المرور لا تُحفظ في أي مكان
    fun exportWithPassword(dir: File, json: String, password: String): File {
        dir.mkdirs() // مجلد backups داخل الكاش الخاص — FileProvider يشارك ملف النسخة فقط
        return File(dir, "nota-backup-${System.currentTimeMillis()}.nota").also { atomicWrite(it, encryptEnvelope(json, password)) }
    }

    // فك التشفير بكلمة المرور — يرمي استثناءً عند كلمة غلط أو ملف تالف (الـ GCM يكشف العبث)
    fun decryptWithPassword(file: File, password: String): String {
        return decryptEnvelope(file.readText(), password)
    }

    private fun deriveKey(password: String, salt: ByteArray, iters: Int = ITERS): javax.crypto.SecretKey {
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iters, 256)
        val tmp = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        spec.clearPassword()
        return javax.crypto.spec.SecretKeySpec(tmp.encoded, "AES")
    }
}
