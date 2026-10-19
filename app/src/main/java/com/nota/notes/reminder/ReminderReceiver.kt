package com.nota.notes.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nota.notes.MainActivity
import com.nota.notes.R
import com.nota.notes.data.DbProvider
import com.nota.notes.security.PassphraseStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// انطلاق التذكير: يعرض الإشعار ثم يقدّم الموعد المتكرر ويجدول التالي
// المقفلة لا تكشف محتواها أبدًا — نص عام بلا عنوان ولا متن
// سكوب المستقبل مملوك: يُلغى في finally مع finish — فلا كوروتين يتيم بعد انتهاء البث
private fun ownedScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action
        if (action != ReminderScheduler.ACTION_REMIND && action != ReminderScheduler.ACTION_SNOOZE) return
        val noteId = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)
        if (noteId <= 0) return
        // زر الغفوة: يُجدوَل منبه مستقل بعد 10 دقائق — بلا مساس بالقاعدة
        if (action == ReminderScheduler.ACTION_SNOOZE) {
            ReminderScheduler.snooze(ctx.applicationContext, noteId)
            // إخفاء الإشعار الحالي فورًا — البديل يظهر بعد 10 دقائق
            runCatching {
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager)
                    ?.cancel(NOTIF_TAG, (noteId % Int.MAX_VALUE).toInt())
            }
            return
        }
        // انطلاق غفوة سابقة: عرض فقط — بلا تقديم ولا إعادة جدولة (القادم الأصلي محفوظ)
        if (intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZED, false)) {
            val finS = goAsync()
            val scS = ownedScope()
            scS.launch {
                try {
                    val app = ctx.applicationContext
                    val store = PassphraseStore(app)
                    val db = DbProvider.open(app, store.getOrCreateDbPassphrase())
                    val note = db.notes().byId(noteId)
                    if (note == null || note.deletedAt != null) return@launch
                    val acctS = runCatching { com.nota.notes.data.AccountStore(app).currentAccountId() }.getOrNull()
                    if (!(note.ownerAccountId == null || note.ownerAccountId == acctS)) return@launch
                    val appLocked = runCatching { store.readPinHash() }.getOrNull() != null
                    showNotification(app, noteId, note.title, note.body, note.locked || appLocked)
                } finally {
                    finS.finish(); scS.cancel()
                }
            }
            return
        }
        val fin = goAsync()
        val sc = ownedScope()
        sc.launch {
            try {
                val app = ctx.applicationContext
                val store = PassphraseStore(app)
                val db = DbProvider.open(app, store.getOrCreateDbPassphrase())
                val dao = db.notes()
                val note = dao.byId(noteId)
                if (note == null || note.deletedAt != null || note.reminderAt == null) return@launch
                // فصل الحسابات: انطلاق يخص حسابًا آخر يُتجاهل بصمت — ويُستأنف عند عودة صاحبه (rescheduleAll)
                val acct = runCatching { com.nota.notes.data.AccountStore(app).currentAccountId() }.getOrNull()
                if (!(note.ownerAccountId == null || note.ownerAccountId == acct)) return@launch
                // المتكرر يتقدم أولًا حتى لا يضيع التالي لو تأخر الجهاز نائمًا
                if (note.reminderRepeat != ReminderScheduler.REPEAT_NONE) {
                    val spec = com.nota.notes.data.ReminderSpec(
                        at = note.reminderAt, repeat = note.reminderRepeat,
                        beforeMin = note.reminderBeforeMin, interval = note.repeatInterval,
                        days = note.repeatDays, monthMode = note.repeatMonthMode,
                        end = note.repeatEnd, endValue = note.repeatEndValue
                    )
                    // العدّاد: كل انطلاق يُنقص المتبقي — وعند النفاد ينتهي التذكير بعد هذا الإشعار
                    val left = if (spec.end == com.nota.notes.data.ReminderSpec.END_COUNT) spec.endValue - 1 else spec.endValue
                    val next = ReminderScheduler.nextRepeat(spec, note.reminderAt)
                    val finished = (spec.end == com.nota.notes.data.ReminderSpec.END_COUNT && left <= 0) ||
                        (spec.end == com.nota.notes.data.ReminderSpec.END_UNTIL && next > spec.endValue)
                    if (finished) {
                        dao.update(note.copy(reminderAt = null, reminderRepeat = ReminderScheduler.REPEAT_NONE,
                            reminderBeforeMin = 0L, repeatInterval = 1, repeatDays = "",
                            repeatMonthMode = com.nota.notes.data.ReminderSpec.MONTH_SAME,
                            repeatEnd = com.nota.notes.data.ReminderSpec.END_FOREVER, repeatEndValue = 0L))
                        ReminderScheduler.cancel(app, noteId)
                    } else {
                        dao.update(note.copy(reminderAt = next, repeatEndValue = left))
                        ReminderScheduler.schedule(app, noteId, next, note.reminderBeforeMin)
                    }
                }
                // الخصوصية: قفل التطبيق مفعّل = كل الإشعارات عامّة (حتى غير المقفلة) — المحتوى لا يتسرب من الشاشة المقفلة
                val appLocked = runCatching { store.readPinHash() }.getOrNull() != null
                showNotification(app, noteId, note.title, note.body, note.locked || appLocked)
            } finally {
                fin.finish(); sc.cancel()
            }
        }
    }

    // ملاحظة مقفلة (أو قفل التطبيق مفعّل): عنوان ونص عامّان — المحتوى لا يظهر على الشاشة المقفلة أبدًا
    private fun showNotification(ctx: Context, noteId: Long, title: String, body: String, locked: Boolean) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = android.app.NotificationChannel(
            ReminderScheduler.CHANNEL,
            ctx.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = ctx.getString(R.string.notif_channel_desc) }
        mgr.createNotificationChannel(channel)
        val open = PendingIntent.getActivity(
            ctx, (noteId % Int.MAX_VALUE).toInt(),
            Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_NOTE, noteId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nTitle = if (locked) ctx.getString(R.string.reminder_locked_title)
            else title.ifBlank { ctx.getString(R.string.untitled) }
        val nText = if (locked) ctx.getString(R.string.reminder_locked_text)
            else body.take(140).ifBlank { ctx.getString(R.string.reminder_empty_body) }
        // الغفوة من الإشعار نفسه: بث مباشر للمستقبِل — يُؤجل 10 دقائق ويُبقي القادم الأصلي كما هو
        val snooze = ReminderScheduler.snoozePending(ctx, noteId)
        val notif = NotificationCompat.Builder(ctx, ReminderScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_notif_bell)
            // P2-B: SECRET دائمًا (لا شرطيًا) — لا يُعرض أي جزء من الإشعار على شاشة القفل
            // أبدًا، بغض النظر عن إعداد "show sensitive content" في النظام. تطبيق خصوصية.
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentTitle(nTitle)
            .setContentText(nText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(nText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(R.drawable.ic_notif_bell, ctx.getString(R.string.snooze_10m), snooze)
            .build()
        mgr.notify(NOTIF_TAG, (noteId % Int.MAX_VALUE).toInt(), notif)
    }

    companion object {
        private const val NOTIF_TAG = "nota_reminder"
    }
}

// الإقلاع: المنبهات تموت مع إعادة التشغيل — فتُعاد جدولتها كلها هنا
class BootReceiver : BroadcastReceiver() {
    // الإقلاع وحده لا يكفي: التخزين المشفر مغلق قبل أول فتح قفل — فيُعاد عند USER_UNLOCKED أيضًا
    // بدونه لا منبهات بعد إعادة التشغيل حتى فتح التطبيق (فجوة صامتة كانت موجودة)
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_USER_UNLOCKED) return
        val fin = goAsync()
        val scB = ownedScope()
        scB.launch {
            try {
                ReminderScheduler.rescheduleAll(ctx.applicationContext)
                // السياجات تموت مع الإقلاع أيضًا — فتُعاد كلها هنا (27 سبتمبر 2026)
                runCatching { com.nota.notes.geo.GeoScheduler.rescheduleAll(ctx.applicationContext) }
                // النسخ المجدول منبه واحد: يُعاد ضبطه بعد كل إقلاع (27 سبتمبر 2026)
                runCatching {
                    val freq = com.nota.notes.data.SettingsStore(ctx.applicationContext).cloudFreq.first()
                    com.nota.notes.backup.BackupScheduler.schedule(ctx.applicationContext, freq)
                }
            } finally {
                fin.finish()
            }
        }
    }
}
