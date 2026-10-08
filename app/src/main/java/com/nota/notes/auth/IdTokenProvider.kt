package com.nota.notes.auth

import com.google.firebase.auth.FirebaseAuth
import com.nota.notes.data.AccountStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// موفر توكن Firebase ID لكل عملاء السيرفر — بلا دورات تبعية (يعتمد على AccountStore فقط).
// النمط: المخزن أولًا (بلا شبكة)، وعند 401 تجديد قسري واحد ثم إعادة المحاولة.
@Singleton
class IdTokenProvider @Inject constructor(private val accounts: AccountStore) {
    suspend fun current(): String? = withContext(Dispatchers.IO) {
        runCatching { accounts.currentIdToken() }.getOrNull()
    }

    // تجديد قسري من Firebase + حفظ فوري — null إن لا دخول حالي
    suspend fun refresh(): String? = withContext(Dispatchers.IO) {
        try {
            val t = FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
            if (t.isNullOrBlank()) return@withContext null
            runCatching { accounts.updateToken(t) }
            t
        } catch (e: Exception) {
            null
        }
    }
}
