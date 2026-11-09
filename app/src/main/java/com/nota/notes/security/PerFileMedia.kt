package com.nota.notes.security

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// P3-D1/D3: ظرف ملفات الوسائط بمفتاح لكل ملف + ملف المفتاح الجانبي.
// صيغة الملف: NOTAM2\n<iv b64>\n<ct b64>  (مشفر بمفتاح الملف الخاص وحده)
// صيغة المفتاح الجانبي (<name>.key): NOTAK1\n<iv b64>\n<wrapped b64>  (مفتاح الملف مغلّف بالمفتاح الرئيسي)
// قرار التخزين (موثق — بديل عن صفوف DB): المفاتيح المغلّفة ملفات جانبية في نفس المجلد الخاص
// (filesDir — نفس حماية DB)، مشفرة بالمفتاح الرئيسي. المزايا: بلا ترحيل Room، بلا إغلاق DB،
// وحذف المفتاح = crypto-shred طبيعي. الخاصية الأمنية مطابقة للـ DB (السرية من المفتاح الرئيسي لا من المكان).
// التوافق الخلفي: ملفات NOTAM1 القديمة تُقرأ عبر legacyDecrypt وتُرحَّل بكسل عند أول لمس ناجح.
// حذف مفتاح ملف = فقدان أبدي لذلك الملف — هذا هو الهدف (crypto-shred)، لا عيب.
object PerFileMedia {
    const val HEADER2 = "NOTAM2"
    const val KEY_HEADER = "NOTAK1"
    const val KEY_SUFFIX = ".key"
    private const val LEGACY_HEADER = "NOTAM1"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private val random = SecureRandom()
    private val b64e: Base64.Encoder = Base64.getEncoder()
    private val b64d: Base64.Decoder = Base64.getDecoder()

    data class Sealed(val fileBytes: ByteArray, val keyBytes: ByteArray)

    // ختم بايتات صافية بمفتاح ملف جديد — خالص بلا أندرويد (يُختبر على JVM بمفتاح برمجي)
    fun sealNew(master: SecretKey, plain: ByteArray): Sealed {
        val fk = PerFileKeys.generate()
        try {
            val fiv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, fk, GCMParameterSpec(GCM_TAG_BITS, fiv))
            val body = "$HEADER2\n${b64e.encodeToString(fiv)}\n${b64e.encodeToString(c.doFinal(plain))}"
            val w = PerFileKeys.wrap(master, fk)
            val keyBody = "$KEY_HEADER\n${b64e.encodeToString(w.iv)}\n${b64e.encodeToString(w.ct)}"
            wipe(w.iv); wipe(w.ct)
            return Sealed(body.toByteArray(Charsets.UTF_8), keyBody.toByteArray(Charsets.UTF_8))
        } finally {
            runCatching { fk.destroy() }
        }
    }

    // قراءة المفتاح الجانبي — null عند أي فشل (بلا كشف)
    fun parseKeyFile(master: SecretKey, keyBytes: ByteArray): SecretKey? = runCatching {
        val lines = String(keyBytes, Charsets.UTF_8).split("\n")
        if (lines.size != 3 || lines[0] != KEY_HEADER) return null
        PerFileKeys.unwrap(master, PerFileKeys.Wrapped(b64d.decode(lines[1]), b64d.decode(lines[2])))
    }.getOrNull()

    // فك ظرف NOTAM2 بمفتاح ملف — null عند أي فشل
    fun decryptWithKey(fileKey: SecretKey, blob: ByteArray): ByteArray? = runCatching {
        val lines = String(blob, Charsets.UTF_8).split("\n")
        if (lines.size != 3 || lines[0] != HEADER2) return null
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, fileKey, GCMParameterSpec(GCM_TAG_BITS, b64d.decode(lines[1])))
        c.doFinal(b64d.decode(lines[2]))
    }.getOrNull()

    fun isPerFile(blob: ByteArray): Boolean =
        blob.size > HEADER2.length + 1 &&
            blob[HEADER2.length] == '\n'.code.toByte() &&
            String(blob, 0, HEADER2.length, Charsets.UTF_8) == HEADER2

    fun isLegacyEnvelope(blob: ByteArray): Boolean =
        blob.size > LEGACY_HEADER.length + 1 &&
            blob[LEGACY_HEADER.length] == '\n'.code.toByte() &&
            String(blob, 0, LEGACY_HEADER.length, Charsets.UTF_8) == LEGACY_HEADER

    fun isKeySidecar(name: String): Boolean = name.endsWith(KEY_SUFFIX)

    // --- مسارات الملفات (على الجهاز: master من Keystore عبر KeyManager) ---
    fun masterKey(): SecretKey = KeyManager.getOrCreateMediaKey()

    fun keyFileFor(file: File): File = File(file.parent, file.name + KEY_SUFFIX)

    // كتابة ملف جديد بمفتاح خاص — false عند الفشل (المتصل يسقط للمسار القديم)
    fun writeNew(file: File, plain: ByteArray, master: SecretKey = masterKey()): Boolean = runCatching {
        val s = sealNew(master, plain)
        try {
            file.parentFile?.mkdirs()
            keyFileFor(file).writeBytes(s.keyBytes)
            file.writeBytes(s.fileBytes)
            true
        } finally {
            wipe(s.fileBytes); wipe(s.keyBytes)
        }
    }.getOrDefault(false)

    // فك شامل: مفتاح خاص أولًا، ثم legacy — null عند الغياب/التلف.
    // master=null (تعذر Keystore) يعني: الملفات الجديدة غير قابلة للفك، والقديمة عبر legacy فقط.
    fun readAll(
        file: File,
        master: SecretKey? = runCatching { masterKey() }.getOrNull(),
        legacyDecrypt: (File) -> ByteArray? = { MediaCrypto.decryptFile(it) }
    ): ByteArray? = runCatching {
        if (!file.exists()) return null
        val blob = file.readBytes()
        if (isPerFile(blob)) {
            if (master == null) return null
            val kf = keyFileFor(file)
            if (!kf.exists()) return null // مفتاح محذوف (crypto-shred) = غير قابل للفك — وهذا هو الهدف
            val fk = parseKeyFile(master, kf.readBytes()) ?: return null
            try {
                decryptWithKey(fk, blob)
            } finally {
                runCatching { fk.destroy() }
            }
        } else {
            val plain = legacyDecrypt(file)
            // ترحيل كسول: قديم → مفتاح خاص عند أول قراءة ناجحة (بلا إغلاق DB — المفاتيح ملفات جانبية)
            if (plain != null && master != null) runCatching {
                if (!isPerFile(file.readBytes())) writeNew(file, plain, master)
            }
            plain
        }
    }.getOrNull()

    // ختم ملف عادي (كاميرا مباشرة/مستعاد سحابيًا) — مفتاح خاص إن أمكن، وإلا المسار القديم
    fun sealPlain(file: File, master: SecretKey? = runCatching { masterKey() }.getOrNull()) {
        runCatching {
            if (!file.exists()) return@runCatching
            val raw = file.readBytes()
            if (raw.isEmpty() || isPerFile(raw)) return@runCatching
            if (isLegacyEnvelope(raw)) {
                // ظرف قديم: يُفك بالمشترك ثم يُعاد بمفتاح خاص (ترحيل)
                val plain = MediaCrypto.decryptFile(file) ?: return@runCatching
                try {
                    if (master != null) {
                        if (!writeNew(file, plain, master)) MediaCrypto.sealIfPlain(file)
                    }
                } finally {
                    wipe(plain)
                }
                return@runCatching
            }
            // ملف عادي تمامًا
            if (master != null) {
                if (!writeNew(file, raw, master)) MediaCrypto.sealIfPlain(file)
            } else {
                MediaCrypto.sealIfPlain(file)
            }
        }
    }

    // Crypto-shred (P3-D3): حذف مفتاح الملف أولًا ثم الملف — أي بقايا فيزيائية (wear-leveling) غير قابلة للفك
    fun cryptoShred(file: File) {
        runCatching { keyFileFor(file).delete() }
        runCatching { file.delete() }
    }

    fun wipe(b: ByteArray) {
        java.util.Arrays.fill(b, 0)
    }
}
