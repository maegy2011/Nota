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

    init {
        viewModelScope.launch {
            _pinHash.value = store.readPinHash()
            _pinMin6Ok.value = store.readPinMin6()
            _pinCardDismissed.value = store.pinCardDismissed()
            _rootWarnDismissed.value = store.rootWarnDismissed()
            _loaded.value = true
        }
    }

    fun dismissRootWarn() = viewModelScope.launch {
        store.dismissRootWarn()
        _rootWarnDismissed.value = true
    }

    fun dismissPinCard() = viewModelScope.launch {
        store.dismissPinCard()
        _pinCardDismissed.value = true
    }

    // نتيجة التحقق: نجاح | غلط | مقفل مؤقتًا (ضد التخمين) — تُستهلك من كوروتين
    sealed interface PinResult {
        data object Success : PinResult
        data object Wrong : PinResult
        data class Locked(val untilMs: Long) : PinResult
    }

    suspend fun verify(pin: String): PinResult {
        val now = System.currentTimeMillis()
        val locked = store.pinLockUntil()
        if (locked > now) return PinResult.Locked(locked)
        val expected = _pinHash.value ?: return PinResult.Wrong
        return if (auth.unlockWithPin(pin, expected)) {
            store.resetPinFails()
            PinResult.Success
        } else {
            val until = store.notePinFail(now)
            if (until > now) PinResult.Locked(until) else PinResult.Wrong
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
    fun changePin(pin: String, onDone: () -> Unit) = viewModelScope.launch {
        val salt = store.getOrCreatePinSalt()
        val hash = PinHash.hash(pin, salt)
        store.savePinHashFull(hash)
        store.savePinMin6(pin.length >= 6)
        _pinMin6Ok.value = pin.length >= 6
        onDone()
    }

    // يُستهلك مرة واحدة عند أول تركيب بعد الإنشاء — الدخول المباشر بعد التعيين، والقفل الدائم بعدها
    fun consumeFreshSetup(): Boolean {
        val v = _freshSetup.value
        _freshSetup.value = false
        return v
    }


}
