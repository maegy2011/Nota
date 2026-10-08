package com.nota.notes.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.accountStore by preferencesDataStore("nota_account")

// المرحلة 0 — الهوية والحساب: accountId(uid) + email + idToken + lastSignIn + pendingRegister
// ملاحظة خصوصية: هذا المخزن للهوية فقط — الملاحظات تبقى مشفرة محليًا كما هي
@Singleton
class AccountStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val K_ACCOUNT_ID = stringPreferencesKey("account_id")
    private val K_EMAIL = stringPreferencesKey("email")
    private val K_ID_TOKEN = stringPreferencesKey("id_token")
    private val K_LAST_SIGNIN = longPreferencesKey("last_sign_in")
    private val K_PENDING_REGISTER = booleanPreferencesKey("pending_register")

    val accountId: Flow<String?> = ctx.accountStore.data.map { it[K_ACCOUNT_ID] }
    val email: Flow<String?> = ctx.accountStore.data.map { it[K_EMAIL] }
    val idToken: Flow<String?> = ctx.accountStore.data.map { it[K_ID_TOKEN] }
    val lastSignIn: Flow<Long?> = ctx.accountStore.data.map { it[K_LAST_SIGNIN] }
    val pendingRegister: Flow<Boolean> = ctx.accountStore.data.map { it[K_PENDING_REGISTER] ?: false }

    suspend fun set(accountId: String, idToken: String?, email: String?) {
        ctx.accountStore.edit {
            it[K_ACCOUNT_ID] = accountId
            if (idToken == null) it.remove(K_ID_TOKEN) else it[K_ID_TOKEN] = idToken
            if (email == null) it.remove(K_EMAIL) else it[K_EMAIL] = email
            it[K_LAST_SIGNIN] = System.currentTimeMillis()
        }
    }

    suspend fun setPendingRegister(pending: Boolean) {
        ctx.accountStore.edit {
            if (pending) it[K_PENDING_REGISTER] = true else it.remove(K_PENDING_REGISTER)
        }
    }

    suspend fun clear() {
        ctx.accountStore.edit {
            it.remove(K_ACCOUNT_ID)
            it.remove(K_EMAIL)
            it.remove(K_ID_TOKEN)
            it.remove(K_LAST_SIGNIN)
            it.remove(K_PENDING_REGISTER)
        }
    }

    suspend fun currentAccountId(): String? = accountId.first()

    suspend fun currentIdToken(): String? {
        val raw = idToken.first() ?: return null
        // مختوم: فك — مكشوف قديم (JWT): استخدم مرة واحدة وأعد ختمه ذاتيًا
        if (!raw.startsWith("V1:")) {
            if (!com.nota.notes.security.TokenVault.looksLikeJwt(raw)) return null
            runCatching { updateToken(raw) }
            return raw
        }
        return runCatching { com.nota.notes.security.TokenVault.unseal(raw) }.getOrNull()
    }

    suspend fun updateToken(token: String) {
        val sealed = runCatching { com.nota.notes.security.TokenVault.seal(token) }.getOrNull() ?: return
        ctx.accountStore.edit { it[K_ID_TOKEN] = sealed }
    }
}
