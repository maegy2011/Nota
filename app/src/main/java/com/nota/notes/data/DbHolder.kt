package com.nota.notes.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// تهيئة DB معلّقة على IO — لا runBlocking على الرئيسي أبدًا (كان سبب ANR عند الفتح)
@Singleton
class DbHolder @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val store: com.nota.notes.security.PassphraseStore
) {
    private val mutex = Mutex()
    @Volatile private var db: NoteDatabase? = null
    // عطل «file is not a database»: المفتاح لا يطابق الملف — نحجر الملف ونبدأ جديدًا بدل حلقة التعطل
    private val _corruptionFound = MutableStateFlow(false)
    val corruptionFound = _corruptionFound.asStateFlow()
    fun dismissCorruption() { _corruptionFound.value = false }

    suspend fun get(): NoteDatabase = db ?: mutex.withLock {
        db ?: try {
            withContext(Dispatchers.IO) {
                // ملف يتيم: قاعدة بلا مفتاح مخزن — لا مفتاح سيفتحها أبدًا — حجر قبل توليد مفتاح ضدها
                if (!store.hasPassphrase() && ctx.getDatabasePath("nota.db").let { it.exists() && it.length() > 0 }) {
                    quarantine()
                    _corruptionFound.value = true
                }
                val pass = store.getOrCreateDbPassphrase()
                try {
                    DbProvider.open(ctx, pass).also {
                        // أمان ذاكرة SQLCipher: بلا نسخ عادية للمفتاح في الذاكرة
                        runCatching { it.openHelper.writableDatabase.execSQL("PRAGMA cipher_memory_security = ON") }
                    }
                } finally {
                    java.util.Arrays.fill(pass, 0) // مسح الـ passphrase من الذاكرة فور الفتح
                }
            }.also { db = it }
        } catch (e: Exception) {
            // محاولة واحدة فقط: حجر الملفات المقروءة ثم فتح جديد — فشل الجديد يُرمى (لا حلقة)
            withContext(Dispatchers.IO) { quarantine() }
            _corruptionFound.value = true
            withContext(Dispatchers.IO) {
                val pass2 = store.getOrCreateDbPassphrase()
                try {
                    DbProvider.open(ctx, pass2).also {
                        runCatching { it.openHelper.writableDatabase.execSQL("PRAGMA cipher_memory_security = ON") }
                    }
                } finally {
                    java.util.Arrays.fill(pass2, 0)
                }
            }.also { db = it }
        }
    }

    // نقل nota.db وتوابعها (wal/shm/journal) جانبًا بتاريخ — لا حذف أبدًا (قد تُنقذ خبيرًا)
    private fun quarantine() {
        val ts = System.currentTimeMillis()
        listOf("nota.db", "nota.db-wal", "nota.db-shm", "nota.db-journal").forEach { name ->
            runCatching {
                val f = ctx.getDatabasePath(name)
                if (f.exists()) f.renameTo(java.io.File(f.parent, "nota-unreadable-$ts-$name"))
            }
        }
    }

    suspend fun dao(): NoteDao = get().notes()

    // P0-3 (أكتوبر 2026): إغلاق الاتصال قبل مسح الملفات — ملحق فقط، لا يمس منطق الفتح.
    suspend fun close() = mutex.withLock {
        runCatching { db?.close() }
        db = null
    }

    // P1-A (أكتوبر 2026): إغلاق DB عند القفل لمسح الذاكرة — لا يمس منطق الفتح.
    // أي استدعاء لاحق لـ get()/dao() يعيد الفتح تلقائيًا (المستودعات تطلب dao طازجًا كل مرة).
    suspend fun closeDatabase() = close()

    // للاختبار: هل الاتصال مفتوح حاليًا؟ — يُستخدم للتحقق من أن lock() أغلق فعلًا.
    fun isOpen(): Boolean = db != null

    suspend fun getOrOpen(): NoteDatabase = get()

    suspend fun folderDao(): FolderDao = get().folders()

    suspend fun tagDao(): TagDao = get().tags()

    suspend fun contentDao(): ContentDao = get().content()
}
