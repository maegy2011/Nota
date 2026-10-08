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

    init {
        viewModelScope.launch {
            runCatching { holder.get().selfMessages().observe() }.getOrNull()?.collect { _messages.value = it }
        }
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || t.length > 4000) return
        viewModelScope.launch {
            runCatching { holder.get().selfMessages().insert(SelfMessage(text = t)) }
        }
    }

    fun edit(id: Long, text: String) {
        val t = text.trim()
        if (t.isEmpty() || t.length > 4000) return
        viewModelScope.launch { runCatching { holder.get().selfMessages().update(id, t) } }
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
