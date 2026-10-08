package com.nota.notes.backup

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nota.notes.MainActivity
import com.nota.notes.R
import com.nota.notes.data.AccountStore
import com.nota.notes.data.BackupCodec
import com.nota.notes.data.DbHolder
import com.nota.notes.data.DriveBackup
import com.nota.notes.data.NotesRepository
import com.nota.notes.data.SettingsStore
import com.nota.notes.security.PassphraseStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// النسخ السحابي المجدول (27 سبتمبر 2026): يومي/أسبوعي فجرًا عبر منبه غير دقيق (بلا إذن منبهات دقيقة) + إشعار بالنتيجة
// يُعاد الجدولة بعد كل انطلاق + عند الإقلاع/فتح القفل + عند تغيير الإعداد — فلا يتوقف أبدًا
object BackupScheduler {
    const val ACTION_BACKUP = "com.nota.notes.SCHEDULED_BACKUP"
    const val CHANNEL = "nota_backup"
    private const val REQ = 19001

    fun schedule(ctx: Context, freq: String) {
        cancel(ctx)
        if (freq != SettingsStore.FREQ_DAILY && freq != SettingsStore.FREQ_WEEKLY) return
        val mgr = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val at = nextRun(freq)
        runCatching {
            mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx))
        }
    }

    fun cancel(ctx: Context) {
        runCatching {
            (ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pending(ctx))
        }
    }

    // 3:07 فجرًا التالي (أو نفس الموعد بعد 7 أيام للأسبوعي) — بعيدًا عن ازدحام رأس الساعة
    private fun nextRun(freq: String): Long {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 7)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var at = c.timeInMillis
        if (at <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, if (freq == SettingsStore.FREQ_WEEKLY) 7 else 1)
            at = c.timeInMillis
        }
        return at
    }

    private fun pending(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, REQ,
            Intent(ctx, BackupReceiver::class.java).setAction(ACTION_BACKUP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class BackupReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != BackupScheduler.ACTION_BACKUP) return
        val fin = goAsync()
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        sc.launch {
            try {
                runBackup(ctx.applicationContext)
            } finally {
                fin.finish(); sc.cancel()
            }
        }
    }

    private suspend fun runBackup(app: Context) {
        val settings = SettingsStore(app)
        val freq = runCatching { settings.cloudFreq.first() }.getOrDefault(SettingsStore.FREQ_OFF)
        // أعد الجدولة دائمًا أولًا حتى لو تخطينا — فالموعد القادم لا يضيع أبدًا
        BackupScheduler.schedule(app, freq)
        if (freq == SettingsStore.FREQ_OFF) return
        // بلا حساب درايف مسجل = إشعار يطلب الدخول (بلا محاولة رفع)
        val acct = runCatching { AccountStore(app).currentAccountId() }.getOrNull()
        if (acct == null) {
            notify(app, app.getString(R.string.backup_sched_noacct))
            return
        }
        try {
            val store = PassphraseStore(app)
            val holder = DbHolder(app, store)
            val accounts = AccountStore(app)
            val repo = NotesRepository(holder, app, accounts)
            val json = BackupCodec.encode(repo.backupNotes())
            DriveBackup(app).upload(json)
            withContext(Dispatchers.IO) { settings.setCloudLast(System.currentTimeMillis()) }
            notify(app, app.getString(R.string.backup_sched_ok))
        } catch (e: DriveBackup.ConsentNeeded) {
            notify(app, app.getString(R.string.backup_sched_consent))
        } catch (e: Exception) {
            notify(app, app.getString(R.string.backup_sched_fail))
        }
    }

    private fun notify(app: Context, text: String) {
        val mgr = app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            mgr.createNotificationChannel(
                android.app.NotificationChannel(
                    BackupScheduler.CHANNEL,
                    app.getString(R.string.backup_sched_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val open = PendingIntent.getActivity(
            app, 19002,
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(app, BackupScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_notif_bell)
            .setContentTitle(app.getString(R.string.backup_sched_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { mgr.notify("nota_backup_sched", 19001, n) }
    }
}
