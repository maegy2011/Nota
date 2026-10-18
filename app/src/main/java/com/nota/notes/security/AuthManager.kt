package com.nota.notes.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthManager(private val ctx: Context) {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked
    var lastActive: Long = System.currentTimeMillis()

    /** مهلة القفل التلقائي — تُزامَن من SettingsStore (القيمة -1 تعني: لا قفل تلقائي أبدًا). */
    var autoLockMs: Long = 30_000L

    fun touch() { lastActive = System.currentTimeMillis() }

    /** يرجع true إذا ما زال مفتوحًا. مع NEVER (-1) لا يُقفل أبدًا بالمهلة. */
    fun checkAutoLock(): Boolean {
        if (autoLockMs < 0) return _unlocked.value
        if (_unlocked.value && System.currentTimeMillis() - lastActive > autoLockMs) {
            _unlocked.value = false
        }
        return _unlocked.value
    }

    fun canBiometric(): Boolean {
        val m = BiometricManager.from(ctx)
        return m.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun biometric(activity: FragmentActivity, onOk: () -> Unit, onErr: (String) -> Unit, onLockout: () -> Unit = {}) {
        // ربط حيوي حقيقي: بلا CryptoObject صالح لا تُعرض المطالبة أصلًا (fail-closed).
        // بعد إضافة بصمة جديدة يُبطل Keystore المفتاح → init ترمي → نرفض قبل العرض.
        val crypto = try {
            androidx.biometric.BiometricPrompt.CryptoObject(KeyManager.getBioChallengeCipher())
        } catch (e: Exception) {
            onErr("")
            return
        }
        val executor = ContextCompat.getMainExecutor(ctx)
        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) {
                    // نجاح بلا CryptoObject = نجاح مزيف — يُرفض كفشل تحقق عادي
                    if (r.cryptoObject == null) { onErr(""); return }
                    _unlocked.value = true; touch(); onOk()
                }
                // القفل المؤقت للبصمة (محاولات كثيرة) مسار مستقل — تُخفى البصمة ويُعرض PIN فقط
                override fun onAuthenticationError(c: Int, s: CharSequence) {
                    if (c == BiometricPrompt.ERROR_LOCKOUT || c == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) onLockout()
                    else onErr(s.toString())
                }
                override fun onAuthenticationFailed() { onErr("") }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Nota")
            .setSubtitle(ctx.getString(ctx.resources.getIdentifier("bio_sub", "string", ctx.packageName)))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(ctx.getString(ctx.resources.getIdentifier("bio_use_pin", "string", ctx.packageName)))
            .build()
        prompt.authenticate(info, crypto)
    }

    fun unlockWithPin(pin: String, expectedHash: String): Boolean {
        // PIN يُقارن بـ PBKDF2-HMAC-SHA256 (310k دورة) في DataStore — لا plain
        val ok = PinHash.verify(pin, expectedHash)
        if (ok) {
            _unlocked.value = true; touch()
            // PIN صحيح = مالك شرعي: جدّد مفتاح التحدي إن أُبطل ببصمة جديدة شرعية (18 أكتوبر 2026)
            runCatching { KeyManager.refreshBioChallengeIfInvalidated() }
        }
        return ok
    }

    fun lock() { _unlocked.value = false }
}
