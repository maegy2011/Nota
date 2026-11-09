package com.nota.notes.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nota.notes.R
import com.nota.notes.billing.ServerRegister
import com.nota.notes.data.AccountStore
import com.nota.notes.data.DriveBackup
import com.nota.notes.data.NotesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "GoogleAuth"

// المرحلة 0 — دخول جوجل الحقيقي: GoogleSignIn → Firebase credential → AccountStore + ربط الملاحظات + تسجيل السيرفر.
// بلا مفاتيح في الكود: web client id مولّد من google-services.json (default_web_client_id).
// الشبكة على IO فقط، وفشلها لا يمنع الدخول المحلي — يُعاد لاحقًا عبر pendingRegister.
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val accounts: AccountStore,
    private val notes: NotesRepository,
    private val registrar: ServerRegister
) {
    fun googleClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(ctx.getString(R.string.default_web_client_id))
            .requestEmail()
            // النسخ السحابي: مجلد التطبيق الخاص فقط (drive.appdata) — لا يرى المستخدم ملفاته ولا نرى سواها
            .requestScopes(Scope(DriveBackup.SCOPE_APPDATA))
            .build()
        return GoogleSignIn.getClient(ctx, gso)
    }

    sealed interface SignInResult {
        data object Success : SignInResult
        data object ServerPending : SignInResult // دخل محليًا — تسجيل السيرفر سيُعاد لاحقًا
        data class Failed(val message: String?) : SignInResult
    }

    suspend fun completeSignIn(idToken: String, email: String?): SignInResult = withContext(Dispatchers.IO) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = try {
            FirebaseAuth.getInstance().signInWithCredential(credential).await()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sign-in failed: ${e.message}")
            return@withContext SignInResult.Failed(e.message)
        }
        val user = authResult.user
        val uid = user?.uid
        if (uid.isNullOrBlank()) {
            return@withContext SignInResult.Failed("empty uid")
        }
        // توكن طازج للتحقق المستقبلي في السيرفر (المرحلة 3) — أفضل جهد، لا يمنع الدخول
        val freshToken = try {
            user.getIdToken(false).await()?.token
        } catch (e: Exception) {
            Log.w(TAG, "getIdToken best-effort failed: ${e.message}")
            null
        }
        val finalEmail = email ?: user.email
        accounts.set(uid, freshToken, finalEmail)
        // صورة الحساب: تُحمَّل مرة واحدة لملف داخلي (للأفاتار أوفلاين) — فشلها لا يمنع الدخول
        runCatching {
            val url = user.photoUrl?.toString()
            if (!url.isNullOrBlank()) {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.inputStream.use { ins ->
                    java.io.File(ctx.filesDir, "account_photo.jpg").outputStream().use { ins.copyTo(it) }
                }
            }
        }.onFailure { Log.w(TAG, "account photo fetch failed: ${it.message}") }
        // ربط الملاحظات المحلية اليتيمة بالحساب — تبقى مشفرة على الجهاز كما هي
        runCatching { notes.claimLocalNotes(uid) }
            .onFailure { Log.w(TAG, "claimLocalNotes failed: ${it.message}") }
        // تسجيل السيرفر: فشله لا يمنع الدخول — يُعلَّم pending ويُعاد لاحقًا
        val registered = registerServer(uid)
        if (!registered) {
            accounts.setPendingRegister(true)
            Log.w(TAG, "server register failed — local sign-in kept, will retry")
            return@withContext SignInResult.ServerPending
        }
        accounts.setPendingRegister(false)
        // العائد تستأنف منبهاته فورًا — بلا انتظار فتح قفل قادم
        runCatching { com.nota.notes.reminder.ReminderScheduler.rescheduleAll(ctx) }
        Log.i(TAG, "sign-in complete, uid registered")
        SignInResult.Success
    }

    // إعادة محاولة تسجيل السيرفر لاحقًا (عند بدء التطبيق وحساب موجود + pending)
    suspend fun retryPendingRegister(): Boolean = withContext(Dispatchers.IO) {
        val uid = accounts.currentAccountId() ?: return@withContext true
        val pending = try {
            accounts.pendingRegister.first()
        } catch (e: Exception) {
            return@withContext false
        }
        if (!pending) return@withContext true
        if (registerServer(uid)) {
            accounts.setPendingRegister(false)
            Log.i(TAG, "pending server register retried OK")
            true
        } else {
            false
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        // الخروج يمسح الحساب والتوكن فقط — الملاحظات المحلية تبقى كما هي
        runCatching { googleClient().signOut().await() }
            .onFailure { Log.w(TAG, "google signOut failed: ${it.message}") }
        runCatching { FirebaseAuth.getInstance().signOut() }
        // المعرف يُلتقط قبل المسح — فالإلغاء بنطاق حساب الخارج لا المحلية فقط
        val uid = accounts.currentAccountId()
        accounts.clear()
        // صورة الحساب تُمسح مع الخروج — لا أثر لحساب خارج
        runCatching { java.io.File(ctx.filesDir, "account_photo.jpg").delete() }
        // منبهات الخارج تُلغى فورًا — فلا إيقاظ ولا إشعار في جلسة حساب آخر
        runCatching { com.nota.notes.reminder.ReminderScheduler.cancelAll(ctx, uid) }
        Log.i(TAG, "signed out — local notes kept")
    }

    // التسجيل عبر ServerRegister الإيدمبوتنت ({accountId} فقط — أي حقل زائد يرفضه السيرفر)
    private suspend fun registerServer(uid: String): Boolean {
        val ok = registrar.ensureRegistered(uid)
        if (!ok) Log.w(TAG, "registerServer failed — will retry later")
        return ok
    }
}
