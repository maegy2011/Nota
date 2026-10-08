package com.nota.notes.data

import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom

// الإتلاف الآمن للملفات قبل حذفها (shredding) — اختياري من الإعدادات
// المستويات حسب المعمول به وأفضل الممارسات:
// - SINGLE: تمريرة عشوائية واحدة — كافية عمليًا على التخزين الحديث (NIST SP 800-88 Clear)
// - DOD: ثلاث تمريرات DoD 5220.22-M (00 ثم FF ثم عشوائي)
// - SCHNEIER: سبع تمريرات (00 ثم FF ثم 5 عشوائية)
// أمانة معلنة: ذاكرة الفلاش (wear-leveling) قد تُبقي نسخًا فيزيائية خارج سيطرتنا —
// لكن ملفات نوتا مشفرة أصلًا (AES-256-GCM) والقاعدة SQLCipher — فالحذف مع الإتلاف دفاع متعدد الطبقات.
// Gutmann35 مستبعد عمدًا: مصمم لأقراص السبعينات وخرافة على التخزين الحديث.
object SecureDelete {
    private const val CHUNK = 64 * 1024
    private val random = SecureRandom()

    // تمريرات كل طريقة: null = عشوائي، وإلا بايت ثابت
    private fun passesFor(method: String): List<Byte?> = when (method) {
        SettingsStore.SHRED_DOD -> listOf(0x00.toByte(), 0xFF.toByte(), null)
        SettingsStore.SHRED_SCHNEIER -> listOf(0x00.toByte(), 0xFF.toByte(), null, null, null, null, null)
        else -> listOf(null) // SHRED_SINGLE الافتراضي
    }

    // يُتلف محتوى الملف ثم يحذفه — يرجع true إذا اختفى الملف
    fun shred(file: File, method: String = SettingsStore.SHRED_SINGLE): Boolean {
        if (!file.exists()) return true
        runCatching {
            val len = file.length()
            if (len > 0) {
                RandomAccessFile(file, "rw").use { raf ->
                    val buf = ByteArray(CHUNK)
                    for (pass in passesFor(method)) {
                        raf.seek(0)
                        var left = len
                        if (pass == null) {
                            while (left > 0) {
                                val n = minOf(left, CHUNK.toLong()).toInt()
                                random.nextBytes(buf)
                                raf.write(buf, 0, n)
                                left -= n
                            }
                        } else {
                            buf.fill(pass)
                            while (left > 0) {
                                val n = minOf(left, CHUNK.toLong()).toInt()
                                raf.write(buf, 0, n)
                                left -= n
                            }
                        }
                        raf.fd.sync()
                    }
                }
            }
        }
        runCatching { file.delete() }
        return !file.exists()
    }

    // نص عشوائي بنفس الطول لطمس صفوف القاعدة قبل حذفها (أفضل جهد — WAL والقاعدة مشفرة أصلًا)
    fun maskText(text: String): String {
        if (text.isEmpty()) return text
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return CharArray(text.length) { alphabet[random.nextInt(alphabet.length)] }.concatToString()
    }
}
