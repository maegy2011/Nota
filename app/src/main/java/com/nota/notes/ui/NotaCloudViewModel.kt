package com.nota.notes.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nota.notes.billing.EntitlementsRepo
import com.nota.notes.data.AccountStore
import com.nota.notes.data.BackupCodec
import com.nota.notes.data.CloudHttpException
import com.nota.notes.data.NotaCloudApi
import com.nota.notes.data.NotesRepository
import com.nota.notes.data.CloudPointer
import com.nota.notes.data.CloudQuota
import com.nota.notes.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// سحابة نوتا (سيرفرنا): حصة مرئية + نسخ/استعادة يدوية لآخر نسخة — الأوفلاين المحلي لا يمسه شيء
@HiltViewModel
class NotaCloudViewModel @Inject constructor(
    private val api: NotaCloudApi,
    private val repo: NotesRepository,
    private val accounts: AccountStore,
    private val cloudKeys: com.nota.notes.security.CloudKeyStore,
    private val settings: com.nota.notes.data.SettingsStore,
    // المرحلة 1ج: فحص Pro داخل الـ VM (defense in depth) — من نفس مصادر premiumVm.isPro().
    // بلا حقن PremiumViewModel عمدًا: نسخة VM منفصلة ستملك trialActive=false دائمًا فتكسر التجربة.
    private val entitlements: EntitlementsRepo,
    @ApplicationContext private val ctx: Context
) : ViewModel() {
    private val _quota = MutableStateFlow<CloudQuota?>(null)
    val quota: StateFlow<CloudQuota?> = _quota.asStateFlow()

    private val _pointer = MutableStateFlow<CloudPointer?>(null)
    val pointer: StateFlow<CloudPointer?> = _pointer.asStateFlow()

    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // الجلسة منتهية على الجهاز: الواجهة تعرض طلب إعادة الدخول بدل التعليق على loading
    private val _authExpired = MutableStateFlow(false)
    val authExpired: StateFlow<Boolean> = _authExpired.asStateFlow()

    // تفاصيل الخطأ الخام للتشخيص عن بعد (رموز بلا بيانات شخصية) — تُنسخ للمطور عند طلبها
    private val _detail = MutableStateFlow<String?>(null)
    val detail: StateFlow<String?> = _detail.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val acct = accounts.currentAccountId() ?: return@launch
            runCatching { _quota.value = api.getQuota(acct) }
                .onSuccess { _authExpired.value = false }
                .onFailure { e ->
                    if ((e as? CloudHttpException)?.code == "AUTH_EXPIRED") _authExpired.value = true
                }
            runCatching { _pointer.value = api.getPointer(acct, "backup_latest") }
        }
    }

    // المرحلة 1ج: نسخة متزامنة-المصادر من premiumVm.isPro() — تجربة نشطة أو pro من الكاش.
    // الكاش مربوط بالحساب (FREE بلا حساب) فيطابق تعريف البوابة الموحدة.
    private suspend fun isProNow(): Boolean {
        if (com.nota.notes.data.SettingsStore.isTrialActive(settings.trialStart.first())) return true
        return entitlements.cached.first().pro
    }

    // النسخ لسحابة نوتا: النص دائمًا + الوسائط (صور/صوت/رسوم/خلفيات) للمشتركين فقط —
    // الوسائط مشفرة بمفتاح المستخدم الخاص وحده (CloudKeyStore) — صفر معرفة.
    // PIN الشاشة قفل فقط ولا يدخل هنا إطلاقًا.
    // P0-2 (أكتوبر 2026): بلا مفتاح سحابة = رفض كامل قبل أي شبكة — لا نص عادي على السيرفر أبدًا.
    fun backupNow(allowMedia: Boolean = false) {
        if (_working.value) return
        viewModelScope.launch {
            _working.value = true
            _message.value = null
            _detail.value = null
            try {
                // المرحلة 1ج: الوسائط للمشتركين فقط — فحص داخل الـ VM لا في الـ UI فقط.
                // Trial يفتح الكل (T3 مغلق): isProNow() يشمل التجربة النشطة.
                if (allowMedia && !isProNow()) {
                    _message.value = "nc-err-pro-required"
                    _detail.value = "pro:required:media"
                    return@launch
                }
                val acct = accounts.currentAccountId() ?: throw IllegalStateException("no-account")
                val key = withContext(Dispatchers.IO) { cloudKeys.getEntropy() }
                if (key == null) {
                    _message.value = "nc-err-nokey-upload"
                    _detail.value = "nokey:upload-refused"
                    return@launch
                }
                try {
                    val notes = withContext(Dispatchers.IO) { repo.backupNotes() }
                    val withMedia = if (allowMedia) {
                        withContext(Dispatchers.IO) { attachMediaShas(acct, notes, key) }
                    } else notes
                    val json = withContext(Dispatchers.IO) { BackupCodec.encode(withMedia) }
                    // المبدأ: النص أيضًا مشفر بمفتاح المستخدم — السيرفر لا يرى شيئًا إطلاقًا.
                    // لا مسار عادي هنا — بلا مفتاح لا رفع أصلًا (البوابة أعلاه).
                    val bytes = withContext(Dispatchers.IO) {
                        com.nota.notes.security.CloudMediaCrypto.encryptKey(
                            key, json.toByteArray(Charsets.UTF_8)
                        )
                    }
                    val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                    api.uploadBlob(acct, sha, bytes, "application/octet-stream")
                    api.setPointer(acct, "backup_latest", sha)
                    _message.value = "nc-ok-backup"
                    runCatching { settings.setCloudLast(System.currentTimeMillis()) }
                    refresh()
                } finally {
                    com.nota.notes.security.CloudMediaCrypto.wipe(key)
                }
            } catch (e: CloudHttpException) {
                _detail.value = "${e.javaClass.simpleName}:${e.code}"
                _message.value = if (e.code == "QUOTA_EXCEEDED") "nc-err-quota" else "nc-err:${e.code}"
            } catch (e: Exception) {
                _detail.value = "${e.javaClass.simpleName}:${e.message}"
                _message.value = "nc-err:${e.message}"
            } finally {
                _working.value = false
            }
        }
    }

    // رفع الوسائط مشفرة بمفتاح المستخدم الخاص أولًا ثم حقن البصمات — فشل ملف واحد لا يُسقط النسخة
    private suspend fun attachMediaShas(
        acct: String,
        notes: List<com.nota.notes.data.BackupNote>,
        key: ByteArray
    ): List<com.nota.notes.data.BackupNote> {
        val shaOf = mutableMapOf<String, String>()
        suspend fun push(name: String, bytes: ByteArray, mime: String) {
            if (shaOf.containsKey(name)) return
            runCatching {
                val sha = sha256Of(bytes)
                api.uploadBlob(acct, sha, bytes, mime)
                shaOf[name] = sha
            }
        }
        notes.forEach { n ->
            n.attachments.forEach { a ->
                val f = com.nota.notes.data.NoteMediaFiles.file(ctx, a.file)
                if (f.exists() && f.length() > 0) {
                    // تُفك محليًا ثم تُعاد للسحابة بمفتاح المستخدم الخاص — المفكوك لا يغادر الذاكرة أبدًا
                    // P3-D1: readAll يفك ملفات المفتاح-الخاص والقديمة معًا
                    val plain = runCatching { com.nota.notes.security.PerFileMedia.readAll(f) }.getOrNull()
                    if (plain != null) runCatching {
                        val cloud = com.nota.notes.security.CloudMediaCrypto.encryptKey(key, plain)
                        java.util.Arrays.fill(plain, 0)
                        push(a.file, cloud, mimeFor(a.kind))
                    }
                }
            }
            n.background?.let { bg ->
                val f = com.nota.notes.data.NoteBgFiles.file(ctx, bg)
                if (f.exists() && f.length() > 0) {
                    val plain = runCatching { com.nota.notes.security.PerFileMedia.readAll(f) }.getOrNull()
                    if (plain != null) runCatching {
                        val cloud = com.nota.notes.security.CloudMediaCrypto.encryptKey(key, plain)
                        java.util.Arrays.fill(plain, 0)
                        push(bg, cloud, "image/jpeg")
                    }
                }
            }
        }
        if (shaOf.isEmpty()) return notes
        return notes.map { n ->
            n.copy(
                backgroundSha = n.background?.let { shaOf[it] } ?: n.backgroundSha,
                attachments = n.attachments.map { a ->
                    a.copy(sha = shaOf[a.file] ?: a.sha)
                }
            )
        }
    }

    // تنزيل الوسائط الناقصة وفكها بمفتاح الـ PIN ثم ختمها محليًا — الفشل يُتجاهل فيُستعاد النص وحده
    private suspend fun fetchMediaBlobs(acct: String, notes: List<com.nota.notes.data.BackupNote>, key: ByteArray) {
        notes.forEach { n ->
            n.attachments.forEach { a ->
                val sha = a.sha ?: return@forEach
                val f = com.nota.notes.data.NoteMediaFiles.file(ctx, a.file)
                if (!f.exists()) {
                    // جهاز جديد: المجلد غير موجود أصلًا — بدونه تفشل الكتابة بصمت (كان سبب ضياع المستعاد)
                    runCatching {
                        val cloud = api.downloadBlob(acct, sha)
                        // مفتاح غلط = null — تُتجاهل بصمت والنص محفوظ
                        val plain = com.nota.notes.security.CloudMediaCrypto.decryptKey(key, cloud)
                            ?: return@runCatching
                        f.parentFile?.mkdirs()
                        f.writeBytes(plain)
                        java.util.Arrays.fill(plain, 0)
                        // تُختم فورًا لتبقى مشفرة على الجهاز (التشفير أثناء الراحة دائم)
                        // P3-D1: ختم بمفتاح خاص إن أمكن
                        com.nota.notes.security.PerFileMedia.sealPlain(f)
                    }
                }
            }
            val bgSha = n.backgroundSha
            val bg = n.background
            if (bgSha != null && bg != null) {
                val f = com.nota.notes.data.NoteBgFiles.file(ctx, bg)
                if (!f.exists()) {
                    runCatching {
                        val cloud = api.downloadBlob(acct, bgSha)
                        val plain = com.nota.notes.security.CloudMediaCrypto.decryptKey(key, cloud)
                            ?: return@runCatching
                        f.parentFile?.mkdirs()
                        f.writeBytes(plain)
                        java.util.Arrays.fill(plain, 0)
                        com.nota.notes.security.PerFileMedia.sealPlain(f)
                    }
                }
            }
        }
    }

    private fun sha256Of(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun mimeFor(kind: String): String = when (kind) {
        com.nota.notes.data.NoteAttachment.KIND_AUDIO -> "audio/mp4"
        com.nota.notes.data.NoteAttachment.KIND_DRAWING -> "image/png"
        else -> "image/jpeg"
    }

    // التلقائي اليومي الصامت: يُستدعى من App بعد كل فتح قفل — للمشتركين يشمل الوسائط
    // (المفتاح على الجهاز فلا حوار)، يتخطى بصمت بلا شبكة/دخول/إعداد
    fun autoBackupIfDue(allowMedia: Boolean = false) {
        if (_working.value) return
        viewModelScope.launch {
            runCatching {
                if (!settings.cloudAuto.first()) return@launch
                if (System.currentTimeMillis() - settings.cloudLast.first() < 24L * 60 * 60 * 1000) return@launch
                if (accounts.currentAccountId() == null) return@launch
                // P0-2: بلا مفتاح = تخطٍّ صامت — backupNow سيرفض برسالة مرئية، والتلقائي لا يُزعج.
                if (withContext(Dispatchers.IO) { cloudKeys.getEntropy() } == null) return@launch
                backupNow(allowMedia)
            }
        }
    }

    fun restoreNow() {
        // المرحلة 1ج — قرار صريح: بلا فحص Pro عمدًا — المستخدم يستعيد بياناته دائمًا حتى بعد downgrade.
        if (_working.value) return
        viewModelScope.launch {
            _working.value = true
            _message.value = null
            _detail.value = null
            try {
                val acct = accounts.currentAccountId() ?: throw IllegalStateException("no-account")
                val p = api.getPointer(acct, "backup_latest") ?: run {
                    _message.value = "nc-err-nobackup"
                    return@launch
                }
                val bytes = api.downloadBlob(acct, p.sha256)
                // ظرف مشفر: يُفك بمفتاح المستخدم — بلا مفتاح لا استعادة أصلًا (صفر معرفة)
                // نص قديم عادي: يُقبل للتوافق مع نسخ ما قبل التشفير
                val CC = com.nota.notes.security.CloudMediaCrypto
                val json = if (CC.isEnvelope(bytes)) {
                    val k = withContext(Dispatchers.IO) { cloudKeys.getEntropy() }
                    if (k == null) {
                        _message.value = "nc-err-nokey"
                        return@launch
                    }
                    try {
                        val plain = withContext(Dispatchers.IO) { CC.decryptKey(k, bytes) }
                        if (plain == null) {
                            _message.value = "nc-err-decrypt"
                            return@launch
                        }
                        try { plain.toString(Charsets.UTF_8) }
                        finally { java.util.Arrays.fill(plain, 0) }
                    } finally { CC.wipe(k) }
                } else bytes.toString(Charsets.UTF_8)
                val decoded = BackupCodec.decode(json)
                val mediaCount = decoded.sumOf { it.attachments.count { a -> a.sha != null } } +
                    decoded.count { it.backgroundSha != null }
                // وسائط بلا مفتاح على هذا الجهاز تُتجاهل بصمت مشروح — النص يُستعاد دائمًا
                // (المفتاح مع المستخدم: عبارة الاسترداد تُستورد من قسم مفتاح السحابة)
                val key = if (mediaCount > 0) withContext(Dispatchers.IO) { cloudKeys.getEntropy() } else null
                if (mediaCount > 0 && key != null) {
                    try { withContext(Dispatchers.IO) { fetchMediaBlobs(acct, decoded, key) } }
                    finally { com.nota.notes.security.CloudMediaCrypto.wipe(key) }
                } else if (mediaCount > 0) {
                    _detail.value = "media:$mediaCount:nokey"
                }
                val r = repo.restoreMerge(decoded)
                runCatching {
                    repo.logRestore(com.nota.notes.data.RestoreRecord(src = "ncloud", added = r.added, skipped = r.skipped, addedIds = r.addedIds))
                }
                runCatching { ReminderScheduler.rescheduleAll(ctx) }
                _message.value = "nc-ok-restore:${r.added}:${r.skipped}"
                refresh()
            } catch (e: CloudHttpException) {
                _detail.value = "${e.javaClass.simpleName}:${e.code}"
                _message.value = "nc-err:${e.code}"
            } catch (e: Exception) {
                _detail.value = "${e.javaClass.simpleName}:${e.message}"
                _message.value = "nc-err:${e.message}"
            } finally {
                _working.value = false
            }
        }
    }

    // عبارة الاسترداد للعرض مرة واحدة (قسم مفتاح السحابة) — null إن لا مفتاح بعد
    suspend fun exportCloudKey(): String? = withContext(Dispatchers.IO) {
        val raw = cloudKeys.getEntropy() ?: return@withContext null
        try { cloudKeys.exportPhrase(raw) }
        finally { com.nota.notes.security.CloudMediaCrypto.wipe(raw) }
    }

    // توليد أول مفتاح ثم عرض عبارته — يُستدعى من زر "إنشاء مفتاح جديد"
    suspend fun createCloudKey(): String = withContext(Dispatchers.IO) {
        val raw = cloudKeys.getOrCreateEntropy()
        try { cloudKeys.exportPhrase(raw) }
        finally { com.nota.notes.security.CloudMediaCrypto.wipe(raw) }
    }

    // استيراد عبارة مستخدم موجودة (جهاز جديد/بعد حذف) — false إن تالفة
    suspend fun importCloudKey(s: String): Boolean = withContext(Dispatchers.IO) {
        val raw = cloudKeys.importPhrase(s) ?: return@withContext false
        try {
            cloudKeys.storeEntropy(raw)
            true
        } finally { com.nota.notes.security.CloudMediaCrypto.wipe(raw) }
    }
}
