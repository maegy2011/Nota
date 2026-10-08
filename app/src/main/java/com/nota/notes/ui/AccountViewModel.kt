package com.nota.notes.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.nota.notes.auth.GoogleAuthManager
import com.nota.notes.data.AccountStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// المرحلة 0 — حالة الحساب للدخول/الخروج — بلا runBlocking، كل شيء معلّق
@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val appCtx: Context,
    private val accounts: AccountStore,
    private val auth: GoogleAuthManager
) : ViewModel() {
    val accountId: StateFlow<String?> =
        accounts.accountId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val email: StateFlow<String?> =
        accounts.email.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val serverPending: StateFlow<Boolean> =
        accounts.pendingRegister.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun signInIntent(): Intent = auth.googleClient().signInIntent

    fun signInCancelled() {
        _signingIn.value = false
    }

    init {
        // إعادة محاولة تسجيل السيرفر المعلق من جلسة سابقة — على IO، وبصمت
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { auth.retryPendingRegister() }
        }
    }

    // نتيجة intent الدخول من GoogleSignInClient — تُستدعى من الـ launcher في الواجهة
    fun handleSignInIntent(data: Intent?) {
        if (_signingIn.value) return
        viewModelScope.launch {
            _signingIn.value = true
            _error.value = null
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val gsa = try {
                task.getResult(ApiException::class.java)
            } catch (e: ApiException) {
                _error.value = "google:${e.statusCode}"
                _signingIn.value = false
                return@launch
            }
            val token = gsa?.idToken
            if (token.isNullOrBlank()) {
                _error.value = "empty-token"
                _signingIn.value = false
                return@launch
            }
            when (val r = auth.completeSignIn(token, gsa.email)) {
                is GoogleAuthManager.SignInResult.Success,
                is GoogleAuthManager.SignInResult.ServerPending -> {
                    // الدخول المحلي تم في الحالتين — الـ pending يُعاد تلقائيًا لاحقًا
                    _error.value = null
                    // صورة المستخدم: تُحمَّل من حساب جوجل لملف داخلي — الأفاتار يعرض أوفلاين دائمًا
                    val photo = gsa.photoUrl?.toString()
                    if (!photo.isNullOrBlank()) {
                        viewModelScope.launch(Dispatchers.IO) {
                            runCatching {
                                java.net.URL(photo).openStream().use { inp ->
                                    accountPhotoFile(appCtx).outputStream().use { out -> inp.copyTo(out) }
                                }
                            }
                        }
                    }
                }
                is GoogleAuthManager.SignInResult.Failed -> _error.value = r.message
                // حدة الـ when: أي حالة مستقبلية تُعامل كفشل ظاهر — لا صمت أبدًا
                else -> _error.value = "unknown-result"
            }
            _signingIn.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { auth.signOut() }
            runCatching { accountPhotoFile(appCtx).delete() }
            _error.value = null
        }
    }
}
