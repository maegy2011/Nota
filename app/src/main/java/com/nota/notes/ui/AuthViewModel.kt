package com.nota.notes.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nota.notes.security.AuthManager
import com.nota.notes.security.PassphraseStore
import com.nota.notes.security.PinHash
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    val auth: AuthManager,
    private val store: PassphraseStore
) : ViewModel() {
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded
    private val _pinHash = MutableStateFlow<String?>(null)
    val pinHash: StateFlow<String?> = _pinHash
    // تتبع طول PIN: true = 6+، false = قصير، null = مستخدم قديم (مجهول) — لبطاقة التوصية لمرة واحدة
    private val _pinMin6Ok = MutableStateFlow<Boolean?>(null)
    val pinMin6Ok: StateFlow<Boolean?> = _pinMin6Ok
    private val _pinCardDismissed = MutableStateFlow(false)
    val pinCardDismissed: StateFlow<Boolean> = _pinCardDismissed
    private val _rootWarnDismissed = MutableStateFlow(false)
    val rootWarnDismissed: StateFlow<Boolean> = _rootWarnDismissed
    // مسارات الاسترداد (مراجعة أكتوبر 2026 — Commit 2): null = غير مُعد (ترحيل آمن)
    private val _hasRecoveryKey = MutableStateFlow(false)
    val hasRecoveryKey: StateFlow<Boolean> = _hasRecoveryKey
    private val _deviceCredEnabled = MutableStateFlow(false)
    val deviceCredEnabled: StateFlow<Boolean> = _deviceCredEnabled
    private val _migrationVisible = MutableStateFlow(false)
    val migrationVisible: StateFlow<Boolean> = _migrationVisible
    private val _lastOk = MutableStateFlow(0L)
    val lastUnlockOk: StateFlow<Long> = _lastOk
    private val _failTotal = MutableStateFlow(0)
    val failTotal: StateFlow<Int> = _failTotal

    init {
        viewModelScope.launch {
            _pinHash.value = store.readPinHash()
            _pinMin6Ok.value = store.readPinMin6()
            _pinCardDismissed.value = store.pinCardDismissed()
            _rootWarnDismissed.value = store.rootWarnDismissed()
            _hasRecoveryKey.value = store.readRecoveryHash() != null
            _deviceCredEnabled.value = store.deviceCredEnabled()
            _lastOk.value = store.lastUnlockOk()
            _failTotal.value = store.failTotal()
            refreshMigration()
            _loaded.value = true
        }
    }

    /** بطاقة الترحيل: بلا مفتاح استرداد + لم تُسقط + انقضى التأجيل (تظهر حتى مع بصمة — البصمة وحدها لا تكفي) */
    suspend fun refreshMigration() {
        val hasRec = store.readRecoveryHash() != null
        _hasRecoveryKey.value = hasRec
        _migrationVisible.value = !hasRec && !store.migrationDismissed() &&
            System.currentTimeMillis() >= store.migrationLaterAt()
    }

    fun dismissMigration() = viewModelScope.launch {
        store.dismissMigration()
        _migrationVisible.value = false
    }

    fun snoozeMigration() = viewModelScope.launch {
        store.snoozeMigration()
        _migrationVisible.value = false
    }

    // حفظ hash مفتاح الاسترداد (لا كلمات أبدًا) — يُخفى البطاقة فور الإعداد
    fun saveRecoveryKey(normalized: String, onDone: () -> Unit) = viewModelScope.launch {
        val salt = store.getOrCreateRecSalt()
        store.saveRecoveryHash(com.nota.notes.security.PinHash.hash(normalized, salt))
        _hasRecoveryKey.value = true
        _migrationVisible.value = false
        onDone()
    }

    sealed interface RecoveryResult {
        data object Success : RecoveryResult
        data object Wrong : RecoveryResult
        data class Locked(val untilMs: Long) : RecoveryResult
    }

    /** التحقق من مفتاح الاسترداد: الفشل بعد 12 كلمة كاملة يُحتسب في الدرع — رسالة عامة فقط */
    suspend fun verifyRecoveryKey(input: String): RecoveryResult {
        val now = System.currentTimeMillis()
        val locked = store.pinLockUntil()
        if (locked > now) return RecoveryResult.Locked(locked)
        val norm = com.nota.notes.security.RecoveryKey.normalize(input)
        if (com.nota.notes.security.RecoveryKey.wordCount(norm) != 12) return RecoveryResult.Wrong
        val expected = store.readRecoveryHash() ?: return RecoveryResult.Wrong
        return if (com.nota.notes.security.PinHash.verify(norm, expected)) {
            RecoveryResult.Success
        } else {
            store.recordFailTotal()
            val until = store.notePinFail(now)
            if (until > now) RecoveryResult.Locked(until) else RecoveryResult.Wrong
        }
    }

    fun setDeviceCred(v: Boolean) = viewModelScope.launch {
        store.setDeviceCredEnabled(v)
        _deviceCredEnabled.value = v
    }

    fun promptDeviceCredential(activity: FragmentActivity, onOk: () -> Unit, onErr: (String) -> Unit) {
        auth.deviceCredential(activity, onOk, onErr)
    }

    /** فتح ناجح: توقيت + تصفير درع — يُنادى مع كل verify ناجح */
    fun recordUnlockOk() = viewModelScope.launch {
        store.recordUnlockOk()
        _lastOk.value = System.currentTimeMillis()
    }

    fun dismissRootWarn() = viewModelScope.launch {
        store.dismissRootWarn()
        _rootWarnDismissed.value = true
    }

    fun dismissPinCard() = viewModelScope.launch {
        store.dismissPinCard()
        _pinCardDismissed.value = true
    }

    // نتيجة التحقق: نجاح | غلط (+عدد الفشل للتنبيه الاستباقي) | مقفل مؤقتًا — تُستهلك من كوروتين
    sealed interface PinResult {
        data object Success : PinResult
        data class Wrong(val fails: Int) : PinResult
        data class Locked(val untilMs: Long) : PinResult
    }

    suspend fun verify(pin: String): PinResult {
        val now = System.currentTimeMillis()
        val locked = store.pinLockUntil()
        if (locked > now) return PinResult.Locked(locked)
        val expected = _pinHash.value ?: return PinResult.Wrong
        return if (auth.unlockWithPin(pin, expected)) {
            store.resetPinFails()
            store.recordUnlockOk()
            _lastOk.value = now
            PinResult.Success
        } else {
            store.recordFailTotal()
            _failTotal.value = _failTotal.value + 1
            val until = store.notePinFail(now)
            if (until > now) PinResult.Locked(until) else PinResult.Wrong(store.pinFailCount())
        }
    }

    // فتح بيومتري ناجح = مالك شرعي — يُصفّر عداد التخمين
    fun biometricOk() = viewModelScope.launch { store.resetPinFails() }

    suspend fun lockRemaining(): Long =
        (store.pinLockUntil() - System.currentTimeMillis()).coerceAtLeast(0L)

    fun canBiometric(): Boolean = auth.canBiometric()

    fun promptBiometric(activity: FragmentActivity, onOk: () -> Unit, onErr: (String) -> Unit, onLockout: () -> Unit = {}) {
        auth.biometric(activity, onOk, onErr, onLockout)
    }

    fun touch() = auth.touch()

    private val _freshSetup = MutableStateFlow(false)

    fun setupPin(pin: String, onDone: () -> Unit) = viewModelScope.launch {
        val salt = store.getOrCreatePinSalt()
        val hash = PinHash.hash(pin, salt)
        store.savePinHashFull(hash)
        store.savePinMin6(pin.length >= 6)
        _pinMin6Ok.value = pin.length >= 6
        _pinHash.value = store.readPinHash()
        auth.unlockWithPin(pin, _pinHash.value ?: return@launch)
        _freshSetup.value = true
        onDone()
    }

    // ترقية صامتة: PIN قديم (SHA-256) يُعاد تشفيره بالصيغة الجديدة بعد أول فتح ناجح — بلا أي خطوة من المستخدم
    fun upgradePinIfLegacy(pin: String) = viewModelScope.launch {
        val cur = _pinHash.value ?: return@launch
        if (PinHash.isLegacy(cur) && PinHash.verify(pin, cur)) {
            val salt = store.getOrCreatePinSalt()
            val upgraded = PinHash.hash(pin, salt)
            store.savePinHashFull(upgraded)
            _pinHash.value = store.readPinHash()
        }
    }

    // تغيير الرقم السري من الإعدادات المتقدمة — يتحقق من الحالي أولًا في الشاشة، وهنا يُحفظ الجديد فقط
    // بلا freshSetup حتى لا يُفتح التطبيق مباشرة بعد التغيير
    // (مراجعة أكتوبر 2026 — إغلاق ثغرة الدرع المشترك 2د): تأكيد PIN جديد = مالك شرعي،
    // فيُصفَّر عدّاد شاشة القفل هنا — لا عند نجاح البصمة وحدها.
    fun changePin(pin: String, onDone: () -> Unit) = viewModelScope.launch {
        val salt = store.getOrCreatePinSalt()
        val hash = PinHash.hash(pin, salt)
        store.savePinHashFull(hash)
        store.savePinMin6(pin.length >= 6)
        _pinMin6Ok.value = pin.length >= 6
        store.resetPinFails()
        onDone()
    }

    // إعادة ضبط التطبيق من شاشة القفل (الخيار الأخير): مسح PIN والدرع + حذف قاعدة
    // البيانات المشفرة — لا رجعة. يُنادى بعد التأكيد المزدوج في RecoverySheet.
    fun factoryReset(onDone: () -> Unit) = viewModelScope.launch {
        store.clearAllLockData()
        runCatching {
            val f = auth.appContext.getDatabasePath("nota.db")
            if (f.exists()) f.delete()
            auth.appContext.getDatabasePath("nota.db-shm").delete()
            auth.appContext.getDatabasePath("nota.db-wal").delete()
        }
        _pinHash.value = null
        _pinMin6Ok.value = null
        _pinCardDismissed.value = false
        onDone()
    }

    // يُستهلك مرة واحدة عند أول تركيب بعد الإنشاء — الدخول المباشر بعد التعيين، والقفل الدائم بعدها
    fun consumeFreshSetup(): Boolean {
        val v = _freshSetup.value
        _freshSetup.value = false
        return v
    }


}
