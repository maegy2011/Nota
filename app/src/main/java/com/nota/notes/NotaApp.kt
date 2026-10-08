package com.nota.notes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class NotaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // نظافة البقايا: أي ملف نسخة (.nota) في الكاش يُمسح عند كل تشغيل —
        // النسخة تُشارك فور إنشائها، وبقاؤها نسخة إضافية بلا داعٍ (تقليل البيانات المخزنة)
        Thread {
            try {
                cacheDir.listFiles { f -> f.extension == "nota" }?.forEach { it.delete() }
                File(cacheDir, "backups").listFiles { f -> f.extension == "nota" }?.forEach { it.delete() }
                // النسخ المفكوكة للعارض/المشغل (shared_tmp) — بقاياها نصوص صافية تُمسح عند كل تشغيل
                File(cacheDir, "shared_tmp").listFiles()?.forEach { it.delete() }
            } catch (e: Exception) { /* أفضل جهد */ }
        }.start()
    }
}
