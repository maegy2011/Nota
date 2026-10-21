package com.nota.notes.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nota.notes.data.AccountStore
import com.nota.notes.data.BackupCodec
import com.nota.notes.data.DriveBackup
import com.nota.notes.data.NotesRepository
import com.nota.notes.data.SettingsStore
import com.nota.notes.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// النسخ السحابي: نسخ/استعادة يدوية + تلقائي يومي صامت — الحالة للشاشة والتلقائي يُستدعى من App بعد القفل
@HiltViewModel
class CloudBackupViewModel @Inject constructor(
    private val drive: DriveBackup,
    private val repo: NotesRepository,
    private val settings: SettingsStore,
    private val accounts: AccountStore,
    @ApplicationContext private val ctx: Context
) : ViewModel() {
    val signedIn: StateFlow<Boolean> =
        accounts.accountId.map { it != null }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val lastBackup: StateFlow<Long> =
        settings.cloudLast.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    // Phase 4أ: حجم آخر نسخة للبطاقة العلوية
    val sizeLast: StateFlow<Long> =
        settings.sizeLast.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val autoEnabled: StateFlow<Boolean> =
        settings.cloudAuto.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    // تكرار النسخ المجدول off|daily|weekly (27 سبتمبر 2026) — يزامن مفتاح التلقائي القديم ويجدول المنبه
    val autoFreq: StateFlow<String> =
        settings.cloudFreq.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.nota.notes.data.SettingsStore.FREQ_OFF)
    fun setFreq(v: String) = viewModelScope.launch {
        runCatching {
            settings.setCloudFreq(v)
            settings.setCloudAuto(v != com.nota.notes.data.SettingsStore.FREQ_OFF)
            if (v == com.nota.notes.data.SettingsStore.FREQ_OFF)
                com.nota.notes.backup.BackupScheduler.cancel(ctx)
            else com.nota.notes.backup.BackupScheduler.schedule(ctx, v)
        }
    }

    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // intent موافقة Drive المعلق — تفتحه الشاشة بـ launcher ثم تُعيد العملية
    private val _consentIntent = MutableStateFlow<Intent?>(null)
    val consentIntent: StateFlow<Intent?> = _consentIntent.asStateFlow()
    fun consumeConsent(): Intent? = _consentIntent.value.also { _consentIntent.value = null }

    private var retryAfterConsent: (suspend () -> Unit)? = null
    private var consentTries = 0 // سقف الموافقة: رفضان متتاليان = رسالة بدل حلقة طلبات لا تنتهي
    fun retryConsented() {
        val op = retryAfterConsent ?: return
        retryAfterConsent = null
        viewModelScope.launch { op() }
    }

    fun setAuto(v: Boolean) = viewModelScope.launch { runCatching { settings.setCloudAuto(v) } }

    // بصمة المحتوى لتخطي الرفع المكرر — توفير بطارية وبيانات
    private fun sha256(s: String): String {
        val d = java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    // كلمة مرور Drive الثابتة — تُحفظ مشفرة بمفتاح الجهاز (الحماية من قراءة السيرفر لا من الجهاز)
    // الجهاز يملك الملاحظات أصلًا، فتخزينها مشفرة لا يُضعف أمن الجهاز — ويمنع قراءة جوجل (27 سبتمبر 2026)
    private fun lockPw(plain: String): String {
        val key = com.nota.notes.security.KeyManager.getOrCreateDrivePwKey()
        val c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        c.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
        val iv = android.util.Base64.encodeToString(c.iv, android.util.Base64.NO_WRAP)
        val enc = android.util.Base64.encodeToString(c.doFinal(plain.toByteArray()), android.util.Base64.NO_WRAP)
        return "$iv.$enc"
    }
    private fun unlockPw(stored: String): String {
        val parts = stored.split(".")
        require(parts.size == 2) { "bad stored pw" }
        val key = com.nota.notes.security.KeyManager.getOrCreateDrivePwKey()
        val c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        c.init(javax.crypto.Cipher.DECRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(128,
            android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP)))
        return String(c.doFinal(android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)))
    }
    val drivePwSet: StateFlow<Boolean> =
        settings.drivePwEnc.map { it != null }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    // P1-B fix 2 (defense in depth): حد 8 + قوة في الـ backend أيضًا — الـ UI تمنع أولًا،
    // وهنا يُرفض أي مسار يتجاوزها (الفشل رسالة لا حفظ صامت). الفك بلا حد عمدًا (توافق خلفي).
    fun setDrivePassword(pw: String) = viewModelScope.launch {
        if (pw.length < com.nota.notes.data.BackupManager.MIN_PASSWORD_LEN ||
            !com.nota.notes.data.BackupManager.isStrongPassword(pw)
        ) {
            _message.value = "err:drive-pw-weak"
            return@launch
        }
        runCatching {
            settings.setDrivePwEnc(lockPw(pw))
            settings.setDrivePwNext(nextConfirmAt())
            _message.value = null
        }
    }
    fun clearDrivePassword() = viewModelScope.launch { runCatching { settings.setDrivePwEnc(null) } }
    private suspend fun storedDrivePassword(): String? = runCatching {
        settings.drivePwEnc.first()?.let { unlockPw(it) }
    }.getOrNull()
    suspend fun checkDrivePassword(pw: String): Boolean = storedDrivePassword() == pw
    // موعد التأكيد التالي — عشوائي 3..7 أيام (بحد طلب المستخدم: لا يتجاوز أسبوعًا)
    private fun nextConfirmAt(): Long =
        System.currentTimeMillis() + (3 + kotlin.random.Random.nextInt(5)) * 24L * 60 * 60 * 1000
    suspend fun confirmDueNow(): Boolean =
        storedDrivePassword() != null && System.currentTimeMillis() >= settings.drivePwNext.first()
    fun markPwConfirmed() = viewModelScope.launch { runCatching { settings.setDrivePwNext(nextConfirmAt()) } }
    fun snoozePwConfirm() = viewModelScope.launch {
        runCatching { settings.setDrivePwNext(System.currentTimeMillis() + 24L * 60 * 60 * 1000) }
    }

    // عتبة الانكماش (27 سبتمبر 2026): الجديدة أقل من نصف السابقة (وبحد أدنى 10) = إيقاف + سؤال يدويًا، وتخطٍّ صامت تلقائيًا
    private val _shrinkWarn = MutableStateFlow<Int?>(null) // نسبة الباقي % عند التحذير
    val shrinkWarn: StateFlow<Int?> = _shrinkWarn.asStateFlow()
    private var shrinkPending: Pair<String, String>? = null // json + hash بانتظار التأكيد
    private var shrinkPendingCount: Int = 0
    fun clearShrink() { _shrinkWarn.value = null; shrinkPending = null }
    fun confirmShrink() {
        val (json, hash) = shrinkPending ?: return
        if (_working.value) return
        viewModelScope.launch {
            _working.value = true
            try {
                val pw = storedDrivePassword() ?: throw IllegalStateException("no-drive-pw")
                drive.upload(com.nota.notes.data.BackupManager.encryptEnvelope(json, pw), settings.cloudKeep.first())
                settings.setCloudHash(hash)
                settings.setCloudCount(shrinkPendingCount)
                settings.setCloudLast(System.currentTimeMillis())
                clearShrink()
                _message.value = "ok-backup"
            } catch (e: DriveBackup.ConsentNeeded) {
                retryAfterConsent = { confirmShrink() }
                _consentIntent.value = e.intent
            } catch (e: Exception) {
                _message.value = "err:${e.message}"
            } finally {
                _working.value = false
            }
        }
    }

    // حارس الرفع (27 سبتمبر 2026): فارغة والسحابة فيها بيانات = تخطٍّ (لا مسح لسليمة) — ومكررة بلا تغيير = تخطٍّ
    // البصمة للمحتوى المكشوف (الغلاف مُملَّح فيختلف كل مرة) — تُمرَّر جاهزة من النداء المشفِّر
    private suspend fun guardedUpload(envelope: String, plainJson: String, plainHash: String, noteCount: Int, silent: Boolean): Boolean {
        val hash = plainHash
        if (hash == settings.cloudHash.first()) return false // بلا تغيير
        if (noteCount == 0 && drive.hasBackup()) {
            if (!silent) _message.value = "err:empty-guard"
            return false
        }
        val lastCount = settings.cloudCount.first()
        if (!silent && lastCount >= 10 && noteCount * 2 < lastCount) {
            shrinkPending = plainJson to plainHash
            shrinkPendingCount = noteCount
            _shrinkWarn.value = (noteCount * 100 / lastCount)
            return false
        }
        if (silent && lastCount >= 10 && noteCount * 2 < lastCount) return false // التلقائي لا يرفع المنكمشة أبدًا
        drive.upload(envelope, settings.cloudKeep.first())
        settings.setCloudHash(hash)
        settings.setCloudCount(noteCount)
        settings.setCloudLast(System.currentTimeMillis())
        runCatching { settings.setSizeLast(plainJson.toByteArray().size.toLong()) }
        return true
    }

    // عدد نسخ Drive المحتفظ بها 1..7 — الأقدم يُستبدل عند التجاوز (27 سبتمبر 2026)
    val keepCount: StateFlow<Int> =
        settings.cloudKeep.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.nota.notes.data.SettingsStore.DEFAULT_CLOUD_KEEP)
    fun setKeepCount(v: Int) = viewModelScope.launch { runCatching { settings.setCloudKeep(v) } }

    fun backupNow() {
        if (_working.value) return
        consentTries = 0
        viewModelScope.launch {
            var op: suspend () -> Unit = {}
            op = {
                _working.value = true
                _message.value = null
                try {
                    val pw = storedDrivePassword() ?: throw IllegalStateException("no-drive-pw")
                    val notes = withContext(Dispatchers.IO) { repo.backupNotes() }
                    val json = withContext(Dispatchers.IO) {
                        BackupCodec.encode(notes, repo.backupFolders(), repo.appVersion())
                    }
                    val envelope = withContext(Dispatchers.IO) {
                        com.nota.notes.data.BackupManager.encryptEnvelope(json, pw)
                    }
                    val uploaded = guardedUpload(envelope, json, sha256(json), notes.size, silent = false)
                    _message.value = when {
                        uploaded -> "ok-backup"
                        _message.value != null -> _message.value
                        _shrinkWarn.value != null -> null // حوار الانكماش هو الرسالة — بلا سطر مضلل
                        else -> "ok-backup-same"
                    }
                } catch (e: DriveBackup.ConsentNeeded) {
                    // رفض الموافقة مرتين = توقف برسالة — لا حلقة طلبات أبدًا
                    if (++consentTries < 3) {
                        retryAfterConsent = op
                        _consentIntent.value = e.intent
                    } else {
                        retryAfterConsent = null
                        _message.value = "err:consent-denied"
                    }
                } catch (e: Exception) {
                    _message.value = "err:${e.message}"
                } finally {
                    _working.value = false
                }
            }
            op()
        }
    }

    // معاينة نسخة Drive قبل الدمج (27 سبتمبر 2026): التنزيل يعرض البطاقة، والتأكيد يدمج
    data class CloudPreview(val meta: com.nota.notes.data.BackupMeta, val stale: Boolean)
    private val _preview = MutableStateFlow<CloudPreview?>(null)
    val preview: StateFlow<CloudPreview?> = _preview.asStateFlow()
    private var previewJson: String? = null
    fun clearPreview() { _preview.value = null; previewJson = null }
    // الغلاف لا يُفك بالمخزنة (نُسيت/جهاز جديد) — إدخال يدوي لمرة واحدة
    private val _pwNeeded = MutableStateFlow(false)
    val pwNeeded: StateFlow<Boolean> = _pwNeeded.asStateFlow()
    private var pwNeededRaw: String? = null
    fun clearPwNeeded() { _pwNeeded.value = false; pwNeededRaw = null }
    fun restoreWithPassword(pw: String) {
        val raw = pwNeededRaw ?: return
        if (_working.value) return
        viewModelScope.launch {
            _working.value = true
            _message.value = null
            try {
                val json = withContext(Dispatchers.IO) {
                    com.nota.notes.data.BackupManager.decryptEnvelope(raw, pw)
                }
                val meta = withContext(Dispatchers.IO) { BackupCodec.preview(json) }
                val stale = meta.createdAt > 0 && meta.createdAt < repo.newestUpdate()
                clearPwNeeded()
                previewJson = json
                _preview.value = CloudPreview(meta, stale)
            } catch (e: Exception) {
                _message.value = "err:drive-pw-wrong"
            } finally {
                _working.value = false
            }
        }
    }

    fun restoreNow() {
        if (_working.value) return
        consentTries = 0
        viewModelScope.launch {
            var op: suspend () -> Unit = {}
            op = {
                _working.value = true
                _message.value = null
                try {
                    val (raw, _) = drive.downloadLatest()
                    // نسخة قديمة مكشوفة (قبل التشفير) — تُقرأ مباشرة كترحيل لمرة واحدة
                    if (!com.nota.notes.data.BackupManager.isEnvelope(raw)) {
                        val meta = withContext(Dispatchers.IO) { BackupCodec.preview(raw) }
                        val stale = meta.createdAt > 0 && meta.createdAt < repo.newestUpdate()
                        previewJson = raw
                        _preview.value = CloudPreview(meta, stale)
                    } else {
                        val pw = storedDrivePassword()
                        val json = runCatching {
                            withContext(Dispatchers.IO) {
                                com.nota.notes.data.BackupManager.decryptEnvelope(raw, pw ?: throw IllegalStateException("no-drive-pw"))
                            }
                        }.getOrNull()
                        if (json != null) {
                            val meta = withContext(Dispatchers.IO) { BackupCodec.preview(json) }
                            val stale = meta.createdAt > 0 && meta.createdAt < repo.newestUpdate()
                            previewJson = json
                            _preview.value = CloudPreview(meta, stale)
                        } else {
                            pwNeededRaw = raw
                            _pwNeeded.value = true
                        }
                    }
                } catch (e: DriveBackup.ConsentNeeded) {
                    // رفض الموافقة مرتين = توقف برسالة — لا حلقة طلبات أبدًا
                    if (++consentTries < 3) {
                        retryAfterConsent = op
                        _consentIntent.value = e.intent
                    } else {
                        retryAfterConsent = null
                        _message.value = "err:consent-denied"
                    }
                } catch (e: Exception) {
                    _message.value = "err:${e.message}"
                } finally {
                    _working.value = false
                }
            }
            op()
        }
    }

    // تأكيد دمج معاينة Drive — نسخة أمان أولًا ثم دمج + إعادة جدولة
    fun confirmRestore() {
        val json = previewJson ?: return
        if (_working.value) return
        viewModelScope.launch {
            _working.value = true
            _message.value = null
            try {
                repo.safetySnapshot()
                val r = repo.restoreMerge(BackupCodec.decode(json), BackupCodec.foldersOf(json))
                runCatching {
                    repo.logRestore(com.nota.notes.data.RestoreRecord(src = "drive", added = r.added, skipped = r.skipped, addedIds = r.addedIds, conflicts = r.conflicts))
                }
                runCatching { ReminderScheduler.rescheduleAll(ctx) }
                clearPreview()
                _message.value = "ok-restore:${r.added}:${r.skipped}"
            } catch (e: Exception) {
                _message.value = "err:${e.message}"
            } finally {
                _working.value = false
            }
        }
    }

    // Phase 4ج: حذف نسخة Drive مفردة + حذف الكل بتأكيد مزدوج (الواجهة تؤكد، هنا التنفيذ فقط)
    fun deleteDriveById(id: String) {
        if (_working.value) return
        viewModelScope.launch {
            _working.value = true
            try { drive.deleteById(id); _message.value = "ok-backup" } catch (e: Exception) { _message.value = "err:${e.message}" } finally { _working.value = false }
        }
    }
    fun deleteAllDrive() {
        if (_working.value) return
        viewModelScope.launch {
            _working.value = true
            try { drive.deleteAll(); _message.value = "ok-backup" } catch (e: Exception) { _message.value = "err:${e.message}" } finally { _working.value = false }
        }
    }
    suspend fun listDriveVersions(): List<DriveBackup.RemoteVersion> = runCatching { drive.listVersions() }.getOrDefault(emptyList())

    // التلقائي اليومي الصامت: يُستدعى من App بعد كل فتح قفل — يتخطى بصمت بلا شبكة/دخول/موافقة
    fun autoBackupIfDue() {
        viewModelScope.launch {
            if (!settings.cloudAuto.first()) return@launch
            if (System.currentTimeMillis() - settings.cloudLast.first() < 24L * 60 * 60 * 1000) return@launch
            if (accounts.currentAccountId() == null) return@launch
            runCatching {
                val pw = storedDrivePassword() ?: return@runCatching // بلا كلمة = تخطٍّ صامت
                val notes = withContext(Dispatchers.IO) { repo.backupNotes() }
                val json = withContext(Dispatchers.IO) {
                    BackupCodec.encode(notes, repo.backupFolders(), repo.appVersion())
                }
                val envelope = withContext(Dispatchers.IO) {
                    com.nota.notes.data.BackupManager.encryptEnvelope(json, pw)
                }
                guardedUpload(envelope, json, sha256(json), notes.size, silent = true)
            }
        }
    }
}
