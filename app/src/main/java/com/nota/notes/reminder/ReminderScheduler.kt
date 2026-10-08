package com.nota.notes.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nota.notes.security.PassphraseStore
import com.nota.notes.data.DbProvider
import java.util.Calendar

// جدولة التنبيهات بالمنبه الدقيق — منبه واحد لكل ملاحظة (يُستبدل عند التعديل)
// التكرار يُحسب عند الانطلاق: الموعد يتقدم في القاعدة ويُجدوَل التالي — بلا Worker دائم
object ReminderScheduler {
    const val ACTION_REMIND = "com.nota.notes.REMIND"
    const val ACTION_SNOOZE = "com.nota.notes.SNOOZE"
    const val EXTRA_NOTE_ID = "noteId"
    const val EXTRA_SNOOZED = "snoozed" // انطلاق غفوة: عرض فقط بلا تقديم القاعدة
    const val CHANNEL = "nota_reminders"
    const val SNOOZE_MIN = 10L // الغفوة: 10 دقائق — منبه مستقل لا يمس القاعدة الأصلية

    const val REPEAT_NONE = "none"
    const val REPEAT_DAILY = "daily"
    const val REPEAT_WEEKLY = "weekly"
    const val REPEAT_MONTHLY = "monthly"
    const val REPEAT_YEARLY = "yearly"

    // وقت الانطلاق الفعلي = الموعد ناقص الإشعار المسبق — والماضي يُنطلق فورًا إن بقي الموعد مستقبلًا
    fun fireTime(reminderAt: Long, beforeMin: Long): Long {
        val t = reminderAt - beforeMin * 60_000L
        return if (t <= System.currentTimeMillis() && reminderAt > System.currentTimeMillis())
            System.currentTimeMillis() + 1_000L else t
    }

    fun schedule(ctx: Context, noteId: Long, reminderAt: Long, beforeMin: Long) {
        val fire = fireTime(reminderAt, beforeMin)
        if (fire <= System.currentTimeMillis() && reminderAt <= System.currentTimeMillis()) return
        val mgr = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pending(ctx, noteId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !mgr.canScheduleExactAlarms()) {
            mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pi)
        } else {
            try {
                mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pi)
            } catch (e: SecurityException) {
                mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pi)
            }
        }
    }

    // إلغاء كل المنبهات — عند الخروج من الحساب (منبهات الخارج لا تنطلق في جلسة غيره)
    // العودة تُعيد الجدولة (rescheduleAll) فيستأنف صاحبه دون فقد
    // acct صريح — لأن الخروج يمسح الحساب قبل الإلغاء فلا يُقرأ من المخزن بعده
    suspend fun cancelAll(ctx: Context, acct: String?) {
        runCatching {
            val store = PassphraseStore(ctx.applicationContext)
            val db = DbProvider.open(ctx.applicationContext, store.getOrCreateDbPassphrase())
            db.notes().allOnce(acct)
                .filter { it.deletedAt == null && it.reminderAt != null }
                .forEach { n -> cancel(ctx, n.id) }
        }
    }

    fun cancel(ctx: Context, noteId: Long) {
        val mgr = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        mgr.cancel(pending(ctx, noteId))
        mgr.cancel(snoozePending(ctx, noteId))
        mgr.cancel(snoozedFirePending(ctx, noteId))
    }

    // الغفوة: منبه مستقل (طلب مختلف) حتى لا يستبدل القادم الأصلي — يُجدوَل بعد 10 دقائق لمرة واحدة
    // انطلاقه يحمل علم snoozed فيُعرض فقط ولا يُقدَّم التكرار — فلا حلقة غفوة أبدًا
    fun snooze(ctx: Context, noteId: Long) {
        val mgr = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val at = System.currentTimeMillis() + SNOOZE_MIN * 60_000L
        val pi = snoozedFirePending(ctx, noteId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !mgr.canScheduleExactAlarms()) {
            mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            try {
                mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } catch (e: SecurityException) {
                mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        }
    }

    fun snoozePending(ctx: Context, noteId: Long): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_SNOOZE)
            .putExtra(EXTRA_NOTE_ID, noteId)
        // طلب مميز (xor) — لا يتصادم مع منبه القاعدة الأصلي أبدًا
        return PendingIntent.getBroadcast(
            ctx, ((noteId % Int.MAX_VALUE).toInt() xor 0x5A5A5A5A), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // انطلاق الغفوة: نفس الطلب المميز لكن بفعل REMIND وعلم snoozed — فيُعرض فقط
    fun snoozedFirePending(ctx: Context, noteId: Long): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .putExtra(EXTRA_NOTE_ID, noteId)
            .putExtra(EXTRA_SNOOZED, true)
        return PendingIntent.getBroadcast(
            ctx, ((noteId % Int.MAX_VALUE).toInt() xor 0x5A5A5A5A), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pending(ctx: Context, noteId: Long): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .putExtra(EXTRA_NOTE_ID, noteId)
        return PendingIntent.getBroadcast(
            ctx, (noteId % Int.MAX_VALUE).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // الموعد التالي للتكرار — يتقدم من القديم حتى يتجاوز الآن (بحد أمان 3700 قفزة ≈ 10 سنوات يوميًا)
    fun nextRepeat(reminderAt: Long, repeat: String): Long =
        nextRepeat(com.nota.notes.data.ReminderSpec(at = reminderAt, repeat = repeat), reminderAt)

    // خطوة واحدة للأمام بعد لحظة معينة — للّحاق التدريجي بالفائت دون قفز الساعة
    fun stepOnce(spec: com.nota.notes.data.ReminderSpec, after: Long): Long =
        nextRepeat(spec.copy(at = after), after, anchor = after)

    // التكرار المخصص: فترة + أيام أسبوع + نمط شهري — من لحظة الأساس حتى تجاوز المرساة (الآن افتراضيًا)
    fun nextRepeat(spec: com.nota.notes.data.ReminderSpec, from: Long, anchor: Long = System.currentTimeMillis()): Long {
        val at = spec.at ?: from
        if (spec.repeat == REPEAT_NONE) return at
        val step = spec.interval.coerceIn(1, 99)
        return when (spec.repeat) {
            REPEAT_DAILY -> advanceSimple(at, Calendar.DAY_OF_YEAR, step, anchor)
            REPEAT_WEEKLY -> nextWeekly(at, step, parseDays(spec.days), anchor)
            REPEAT_MONTHLY -> if (spec.monthMode == com.nota.notes.data.ReminderSpec.MONTH_NTH)
                advanceMonthlyNth(at, step, anchor) else advanceMonthlySame(at, step, anchor)
            REPEAT_YEARLY -> advanceSimple(at, Calendar.YEAR, step, anchor)
            else -> at
        }
    }

    private fun advanceSimple(at: Long, field: Int, step: Int, anchor: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = at }
        var guard = 0
        while (cal.timeInMillis <= anchor && guard++ < 3700) cal.add(field, step)
        return cal.timeInMillis
    }

    // الأسبوعي: بلا أيام مختارة = كل N أسابيع — وبأيام = أقرب يوم قادم من القائمة
    private fun nextWeekly(at: Long, step: Int, days: Set<Int>, anchor: Long): Long {
        if (days.isEmpty()) return advanceSimple(at, Calendar.WEEK_OF_YEAR, step, anchor)
        // ملاحظة: الفاصل step مع أيام محددة يُبسَّط لأسبوعي (1) — الأيام نفسها هي القاعدة
        return nextWeekday(at, days, anchor)
    }

    // أيام الأسبوع المختارة: أقرب يوم قادم من القائمة (1=الأحد..7=السبت) — مع الحفاظ على الوقت
    private fun nextWeekday(at: Long, days: Set<Int>, anchor: Long): Long {
        val base = Calendar.getInstance().apply { timeInMillis = at }
        val probe = (base.clone() as Calendar)
        var guard = 0
        // ابدأ من اليوم التالي لتفادي البقاء على نفس اللحظة الماضية
        do {
            probe.add(Calendar.DAY_OF_YEAR, 1)
            if (days.contains(probe.get(Calendar.DAY_OF_WEEK))) {
                probe.set(Calendar.HOUR_OF_DAY, base.get(Calendar.HOUR_OF_DAY))
                probe.set(Calendar.MINUTE, base.get(Calendar.MINUTE))
                probe.set(Calendar.SECOND, 0)
                probe.set(Calendar.MILLISECOND, 0)
                if (probe.timeInMillis > anchor) return probe.timeInMillis
            }
        } while (guard++ < 3700)
        return advanceSimple(at, Calendar.WEEK_OF_YEAR, 1, anchor)
    }

    // الشهري بنفس اليوم: مع التثبيت لآخر الشهر عند تجاوز الأيام (31 ← 30/28)
    private fun advanceMonthlySame(at: Long, step: Int, anchor: Long): Long {
        val base = Calendar.getInstance().apply { timeInMillis = at }
        val wantDay = base.get(Calendar.DAY_OF_MONTH)
        val probe = (base.clone() as Calendar)
        var guard = 0
        do {
            probe.add(Calendar.MONTH, step)
            probe.set(Calendar.DAY_OF_MONTH, wantDay.coerceAtMost(probe.getActualMaximum(Calendar.DAY_OF_MONTH)))
        } while (probe.timeInMillis <= anchor && guard++ < 1200)
        return probe.timeInMillis
    }

    // الشهري بنفس الترتيب: الثلاثاء الرابع مثلًا — يُعاد حسابه كل شهر متخطَّى
    private fun advanceMonthlyNth(at: Long, step: Int, anchor: Long): Long {
        val base = Calendar.getInstance().apply { timeInMillis = at }
        val weekOfMonth = base.get(Calendar.DAY_OF_WEEK_IN_MONTH)
        val weekday = base.get(Calendar.DAY_OF_WEEK)
        val probe = (base.clone() as Calendar)
        var guard = 0
        do {
            probe.add(Calendar.MONTH, step)
            probe.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfMonth)
            probe.set(Calendar.DAY_OF_WEEK, weekday)
        } while (probe.timeInMillis <= anchor && guard++ < 1200)
        return probe.timeInMillis
    }

    // تحليل أيام الأسبوع CSV — القيم خارج 1..7 تُتجاهل
    fun parseDays(csv: String): Set<Int> =
        csv.split(",").mapNotNull { it.trim().toIntOrNull()?.takeIf { d -> d in 1..7 } }.toSet()

    // إعادة جدولة كل المستقبلية — عند الإقلاع وبعد الاستعادة (المنبهات لا تنجو من إعادة التشغيل)
    // إصلاح: المتكرر الذي فات موعده أثناء الإغلاق يتقدم لقادمه بدل ضياعه للأبد
    // فصل الحسابات: منبهات حساب آخر لا تُجدوَل في جلسة الحساب الحالي — ولا تنطلق له إشعارات
    suspend fun rescheduleAll(ctx: Context) {
        runCatching {
            val store = PassphraseStore(ctx.applicationContext)
            val db = DbProvider.open(ctx.applicationContext, store.getOrCreateDbPassphrase())
            val dao = db.notes()
            val acct = runCatching { com.nota.notes.data.AccountStore(ctx.applicationContext).currentAccountId() }.getOrNull()
            val now = System.currentTimeMillis()
            dao.allOnce(acct).filter { it.deletedAt == null && it.reminderAt != null }.forEach { n ->
                // قراءة آمنة بلا !! — المصفاة تضمن عدم الـ null لكن النوع يبقى nullable
                val firstAt: Long = n.reminderAt ?: return@forEach
                if (firstAt > now) {
                    schedule(ctx.applicationContext, n.id, firstAt, n.reminderBeforeMin)
                } else if (n.reminderRepeat != REPEAT_NONE) {
                    // فائت متكرر: تقدّم حدثًا حدثًا حتى تجاوز الآن — بلا إغراق بإشعارات فائتة، وبلا تنقيص العدّاد
                    // (الفائت لم يُعرض أصلًا — فيبقى عدد الأحداث كما وعده المستخدم)
                    val spec = com.nota.notes.data.ReminderSpec(at = n.reminderAt, repeat = n.reminderRepeat,
                        beforeMin = n.reminderBeforeMin, interval = n.repeatInterval, days = n.repeatDays,
                        monthMode = n.repeatMonthMode, end = n.repeatEnd, endValue = n.repeatEndValue)
                    var occ: Long = firstAt
                    var guard = 0
                    while (occ <= now && guard++ < 3700) {
                        val nx = stepOnce(spec, occ)
                        if (nx <= occ) break // حماية من الجمود
                        occ = nx
                        if (spec.end == com.nota.notes.data.ReminderSpec.END_UNTIL && occ > spec.endValue) break
                    }
                    val finished = spec.end == com.nota.notes.data.ReminderSpec.END_UNTIL && occ > spec.endValue
                    if (finished) {
                        dao.update(n.copy(reminderAt = null, reminderRepeat = REPEAT_NONE, reminderBeforeMin = 0L,
                            repeatInterval = 1, repeatDays = "", repeatMonthMode = com.nota.notes.data.ReminderSpec.MONTH_SAME,
                            repeatEnd = com.nota.notes.data.ReminderSpec.END_FOREVER, repeatEndValue = 0L))
                    } else {
                        dao.update(n.copy(reminderAt = occ))
                        schedule(ctx.applicationContext, n.id, occ, n.reminderBeforeMin)
                    }
                }
                // الفائت غير المتكرر يُترك سجلًا بلا منبه — كما كان
            }
        }
    }
}
