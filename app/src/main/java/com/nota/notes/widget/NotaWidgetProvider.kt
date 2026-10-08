package com.nota.notes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.nota.notes.MainActivity
import com.nota.notes.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// ويدجت نوتا 4×2: آخر 3 عناوين من الكاش + زر إضافة يفتح محررًا جديدًا بعد القفل
// بلا updatePeriod (صفر) — التحديث يُدفع من التطبيق عند الحفظ/الفتح فقط
class NotaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        // سكوب مملوك يُلغى مع finish — كالمستقبلات (قاعدة ملكية النطاق)
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        sc.launch {
            try {
                val titles = runCatching { WidgetUpdater.snapshot(context) }.getOrDefault(emptyList())
                ids.forEach { id -> mgr.updateAppWidget(id, buildViews(context, titles)) }
            } finally {
                pending.finish(); sc.cancel()
            }
        }
    }

    private fun buildViews(ctx: Context, titles: List<String>): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.nota_widget)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        v.setOnClickPendingIntent(R.id.widget_root, open)
        v.setOnClickPendingIntent(R.id.widget_header, open)
        val add = PendingIntent.getActivity(
            ctx, 1,
            Intent(ctx, MainActivity::class.java)
                .setAction(ACTION_NEW_NOTE)
                .putExtra(MainActivity.EXTRA_NEW_NOTE, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        v.setOnClickPendingIntent(R.id.widget_add, add)
        val slots = intArrayOf(R.id.widget_t1, R.id.widget_t2, R.id.widget_t3)
        slots.forEachIndexed { i, id ->
            if (i < titles.size) {
                v.setViewVisibility(id, View.VISIBLE)
                v.setTextViewText(id, titles[i])
            } else {
                v.setViewVisibility(id, View.GONE)
            }
        }
        v.setViewVisibility(R.id.widget_empty, if (titles.isEmpty()) View.VISIBLE else View.GONE)
        return v
    }

    companion object {
        const val ACTION_NEW_NOTE = "com.nota.notes.action.NEW_NOTE"
    }
}
