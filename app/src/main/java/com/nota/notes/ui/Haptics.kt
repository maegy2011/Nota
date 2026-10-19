package com.nota.notes.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// لمسات اهتزازية (27 سبتمبر 2026): تأكيد لمسي للأفعال — بلا إذن (اهتزازات قصيرة فقط)
// 28 سبتمبر 2026: مفتاح عام enabled + أنماط أقوى (فتح/إغلاق/سحب) — يُطفأ من إعداد
// "التأثيرات والحركة" فيحترم كل مستهلك في التطبيق بلا تعديل موضعي.
object Haptics {
    // يُضبط من SettingsViewModel عبر App — false = صمت تام
    @Volatile
    var enabled: Boolean = true

    private fun vibrator(ctx: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    // نمط مركّب: نبضتان/ثلاث خفيفة — يوسّع الإحساس بلا إزعاج
    private fun pattern(ctx: Context, timings: LongArray, amps: IntArray) {
        if (!enabled) return
        val v = vibrator(ctx) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
            else @Suppress("DEPRECATION") v.vibrate(timings, -1)
        }
    }

    // نقرة خفيفة: تنقل وأزرار
    fun tick(ctx: Context) {
        if (!enabled) return
        val v = vibrator(ctx) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            else v.vibrate(VibrationEffect.createOneShot(12, 80))
        }
    }

    // تأكيد: إرسال/حفظ/بدء
    fun confirm(ctx: Context) {
        if (!enabled) return
        val v = vibrator(ctx) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            else v.vibrate(VibrationEffect.createOneShot(25, 120))
        }
    }

    // فتح قائمة دائرية: نبضة صاعدة (نقرتان تتدرجان) — إحساس خروج من المركز
    fun open(ctx: Context) = pattern(ctx, longArrayOf(0, 14, 45, 20), intArrayOf(0, 70, 0, 130))

    // إغلاق: نبضة هابطة معكوسة
    fun close(ctx: Context) = pattern(ctx, longArrayOf(0, 20, 40, 12), intArrayOf(0, 130, 0, 60))

    // الإمساك بالسحب: نقرة واحدة — تأكيد أن السحب قد بدأ
    fun grab(ctx: Context) = tick(ctx)

    // انتهاء السحب/الاستقرار: نبضة خفيفة واحدة
    fun release(ctx: Context) = pattern(ctx, longArrayOf(0, 18), intArrayOf(0, 110))

    // رفض (مراجعة أكتوبر 2026 — Commit 3): نمط مميز للأخطاء — نبضة طويلة واحدة،
    // مختلف عن نقرة الضغطة العادية (TextHandleMove في اللوحة)
    fun reject(ctx: Context) = pattern(ctx, longArrayOf(0, 90), intArrayOf(0, 200))
}
