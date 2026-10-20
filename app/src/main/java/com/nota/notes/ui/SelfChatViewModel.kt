package com.nota.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nota.notes.data.DbHolder
import com.nota.notes.data.SelfMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// عارض محادثة النفس (27 سبتمبر 2026): تدفق الرسائل + إرسال/حذف/مسح — عبر DbHolder مثل المستودعات
@HiltViewModel
class SelfChatViewModel @Inject constructor(
    private val holder: DbHolder
) : ViewModel() {
    private val _messages = MutableStateFlow<List<SelfMessage>>(emptyList())
    val messages: StateFlow<List<SelfMessage>> = _messages.asStateFlow()

    init { reload() }

    // P1-A (أكتوبر 2026): إعادة الاشتراك بعد إعادة فتح DB — إغلاق DB أثناء القفل يُميت التدفق.
    fun reload() {
        viewModelScope.launch {
            // P3-D4: مفتاح الطبقة العمودية — null عند تعذر Keystore (عرض legacy فقط، بلا كسر)
            val key = runCatching { com.nota.notes.security.SelfMessageCrypto.deviceKey() }.getOrNull()
            runCatching { holder.get().selfMessages().observe() }.getOrNull()?.collect { list ->
                _messages.value = list.map { it.copy(text = com.nota.notes.security.SelfMessageCrypto.decryptOrLegacy(key, it.text)) }
                runCatching { migrateLegacyRows(key, list) }
            }
        }
    }

    // P3-D4: ترحيل كسول — الصفوف القديمة العادية تُختم عند أول قراءة (idempotent، ولا يُعيد الإطلاق)
    private suspend fun migrateLegacyRows(key: javax.crypto.SecretKey?, list: List<SelfMessage>) {
        if (key == null) return
        val dao = holder.get().selfMessages()
        list.filter { !com.nota.notes.security.SelfMessageCrypto.isSealed(it.text) }.forEach { m ->
            runCatching {
                val sealed = com.nota.notes.security.SelfMessageCrypto.seal(key, m.text)
                dao.update(m.id, sealed)
            }
        }
    }

    // P1-A: تُنادى عند القفل — تمسح نصوص المحادثة من الذاكرة.
    fun clearSensitiveData() { _messages.value = emptyList() }

    // P3-D4: الختم بمفتاح الطبقة العمودية — السقوط للعادي عند تعذر Keystore (SQLCipher يبقى الضابط الأساسي)
    private fun sealOrPlain(t: String): String {
        val key = runCatching { com.nota.notes.security.SelfMessageCrypto.deviceKey() }.getOrNull() ?: return t
        return runCatching { com.nota.notes.security.SelfMessageCrypto.seal(key, t) }.getOrDefault(t)
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || t.length > 4000) return
        viewModelScope.launch {
            runCatching { holder.get().selfMessages().insert(SelfMessage(text = sealOrPlain(t))) }
        }
    }

    fun edit(id: Long, text: String) {
        val t = text.trim()
        if (t.isEmpty() || t.length > 4000) return
        viewModelScope.launch { runCatching { holder.get().selfMessages().update(id, sealOrPlain(t)) } }
    }

    // تحويل رسالة لملاحظة جديدة في القائمة الرئيسية — ثم تُفتح من هناك (27 سبتمبر 2026)
    fun forwardToNote(text: String, onDone: () -> Unit = {}) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                holder.get().notes().insert(
                    com.nota.notes.data.Note(title = "", body = t, createdAt = now, updatedAt = now)
                )
            }
            onDone()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { runCatching { holder.get().selfMessages().delete(id) } }
    }

    fun clear() {
        viewModelScope.launch { runCatching { holder.get().selfMessages().clear() } }
    }
}
