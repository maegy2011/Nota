package com.nota.notes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

import androidx.lifecycle.lifecycleScope
import com.nota.notes.data.SettingsStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var settingsStore: SettingsStore

    // إشارة زر + في الويدجت — تُمرر لـ App فيفتح محررًا جديدًا بعد القفل
    private var newNoteSignal by mutableIntStateOf(0)
    // الربط العميق من إشعار التذكير — تُمرر لـ App فيفتح الملاحظة بعد القفل
    private var openNoteId by androidx.compose.runtime.mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // شاشة النظام الرسمية (مهمة 1، 18 أكتوبر 2026 — الخيار: System + Compose):
        // النظام يعرض الشعار ثابتًا على نفس الخلفية حتى جاهزية التهيئة الحقيقية (SplashGate.keepOnScreen)
        // ثم Compose بحركة 350ms فقط — بلا "2x Splash" مرئي لتماثل الخلفيتين
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this)
            .setKeepOnScreenCondition { com.nota.notes.ui.SplashGate.keepOnScreen }
        if (isNewNoteIntent()) newNoteSignal++
        intent?.getLongExtra(EXTRA_OPEN_NOTE, -1L)?.takeIf { it > 0 }?.let { openNoteId = it }
        // شريطا النظام يتبعان الثيم المختار — لا يبقيان داكنين في الوضع الفاتح
        lifecycleScope.launch {
            settingsStore.theme.collect { mode ->
                val night = when (mode) {
                    "light" -> false
                    "dark" -> true
                    else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
                // أسطح دافئة لا سوداء صافية (تلطخ OLED) — مطابقة لتوكنز المواصفة
                val bar = if (night) 0xFF16130E.toInt() else 0xFFFFFBF3.toInt()
                window.statusBarColor = bar
                window.navigationBarColor = bar
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !night
                    isAppearanceLightNavigationBars = !night
                }
            }
        }
        // خصوصية: المنع افتراضيًا (بلا سكرينشوت ولا ظهور في التطبيقات الأخيرة) — والمستخدم يسمح من الإعدادات
        lifecycleScope.launch {
            settingsStore.screenshotsAllowed.collect { allowed ->
                if (allowed) window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                else window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SECURE,
                    android.view.WindowManager.LayoutParams.FLAG_SECURE
                )
            }
        }
        setContent { App(newNoteSignal = newNoteSignal, openNoteId = openNoteId) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isNewNoteIntent()) newNoteSignal++
        intent.getLongExtra(EXTRA_OPEN_NOTE, -1L).takeIf { it > 0 }?.let { openNoteId = it }
    }

    private fun isNewNoteIntent(): Boolean =
        intent?.getBooleanExtra(EXTRA_NEW_NOTE, false) == true ||
            intent?.action == com.nota.notes.widget.NotaWidgetProvider.ACTION_NEW_NOTE

    companion object {
        const val EXTRA_NEW_NOTE = "extra_new_note"
        const val EXTRA_OPEN_NOTE = "extra_open_note"
    }
}
