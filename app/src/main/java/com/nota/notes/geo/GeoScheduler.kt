package com.nota.notes.geo

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.nota.notes.MainActivity
import com.nota.notes.R
import com.nota.notes.data.AccountStore
import com.nota.notes.data.DbProvider
import com.nota.notes.security.PassphraseStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// تذكير الموقع (27 سبتمبر 2026): سياج دخول واحد لكل ملاحظة — عند الدخول إشعار بعنوان الملاحظة
// البطارية: السياج نظامي (مدمج في خدمات الموقع) — بلا Worker ولا تتبع مستمر
object GeoScheduler {
    const val ACTION_GEO = "com.nota.notes.GEOFENCE"
    const val CHANNEL = "nota_geofence"
    const val EXTRA_NOTE_ID = "noteId"
    const val MIN_RADIUS = 100f
    const val MAX_RADIUS = 2000f

    fun reqId(noteId: Long) = "nota_geo_$noteId"

    fun hasPermission(ctx: Context): Boolean =
        ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    // التسجيل: دخول فقط (ENTER) — انتهاء الصلاحية مفتوح، ويُستبدل عند التعديل
    fun add(ctx: Context, noteId: Long, lat: Double, lng: Double, radiusM: Float) {
        if (!hasPermission(ctx)) return
        try {
            val client = LocationServices.getGeofencingClient(ctx.applicationContext)
            val fence = Geofence.Builder()
                .setRequestId(reqId(noteId))
                .setCircularRegion(lat, lng, radiusM.coerceIn(MIN_RADIUS, MAX_RADIUS))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
            val req = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(fence)
                .build()
            client.addGeofences(req, pending(ctx, noteId))
        } catch (e: SecurityException) { /* بلا إذن: تُحفظ الإحداثيات ويُسجَّل عند منح الإذن */ }
    }

    fun remove(ctx: Context, noteId: Long) {
        runCatching {
            LocationServices.getGeofencingClient(ctx.applicationContext)
                .removeGeofences(listOf(reqId(noteId)))
        }
    }

    // إعادة التسجيل بعد الإقلاع/فتح القفل: كل السياجات النشطة للحساب الحالي
    suspend fun rescheduleAll(ctx: Context) {
        runCatching {
            val app = ctx.applicationContext
            val store = PassphraseStore(app)
            val db = DbProvider.open(app, store.getOrCreateDbPassphrase())
            val acct = runCatching { AccountStore(app).currentAccountId() }.getOrNull()
            db.notes().geofencedOnce(acct).forEach { n ->
                val la = n.geoLat ?: return@forEach
                val ln = n.geoLng ?: return@forEach
                add(app, n.id, la, ln, n.geoRadiusM)
            }
        }
    }

    private fun pending(ctx: Context, noteId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            // لا تستخدم نطاقًا ضيقًا مثل % 10000: معرفان يفصل بينهما 10000 كانا سيتشاركان نفس الطلب.
            ctx, (noteId % Int.MAX_VALUE).toInt(),
            Intent(ctx, GeoReceiver::class.java).setAction(ACTION_GEO)
                .putExtra(EXTRA_NOTE_ID, noteId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class GeoReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != GeoScheduler.ACTION_GEO) return
        val event = runCatching { GeofencingEvent.fromIntent(intent) }.getOrNull() ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val noteId = intent.getLongExtra(GeoScheduler.EXTRA_NOTE_ID, -1L)
        if (noteId <= 0) return
        val fin = goAsync()
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        sc.launch {
            try {
                val app = ctx.applicationContext
                val store = PassphraseStore(app)
                val db = DbProvider.open(app, store.getOrCreateDbPassphrase())
                val note = db.notes().byId(noteId) ?: return@launch
                // المحذوفة/المؤرشفة بلا سياج منطقيًا — لكن الإبقاء أبسط: المحذوفة تُتجاهل، والمؤرشفة تُنبَّه (تُفتح من سياقها كالتذكير الزمني)
                if (note.deletedAt != null) return@launch
                if (note.geoLat == null || note.geoLng == null) return@launch
                val acct = runCatching { AccountStore(app).currentAccountId() }.getOrNull()
                if (!(note.ownerAccountId == null || note.ownerAccountId == acct)) return@launch
                val appLocked = runCatching { store.readPinHash() }.getOrNull() != null
                showNotification(app, noteId, note.title, note.geoName, note.locked || appLocked)
            } finally {
                fin.finish(); sc.cancel()
            }
        }
    }

    private fun showNotification(ctx: Context, noteId: Long, title: String, geoName: String, locked: Boolean) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            mgr.createNotificationChannel(
                android.app.NotificationChannel(
                    GeoScheduler.CHANNEL,
                    ctx.getString(R.string.geo_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = ctx.getString(R.string.geo_channel_desc) }
            )
        }
        val open = PendingIntent.getActivity(
            ctx, (noteId % Int.MAX_VALUE).toInt(),
            Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_NOTE, noteId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (locked) ctx.getString(R.string.geo_locked_text)
        else if (geoName.isNotBlank()) ctx.getString(R.string.geo_arrived_fmt, geoName)
        else ctx.getString(R.string.geo_arrived)
        val n = NotificationCompat.Builder(ctx, GeoScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_notif_bell)
            .setContentTitle(if (locked) ctx.getString(R.string.reminder_locked_title) else title.ifBlank { ctx.getString(R.string.untitled) })
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { mgr.notify("nota_geo", (noteId % Int.MAX_VALUE).toInt(), n) }
    }
}
