package com.nota.notes.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

// P3-B2 — الفحص الدوري للحذف النهائي بعد انتهاء مهلة 14 يومًا.
// يعمل دفاعيًا: إن انتهت المهلة وما زال الطلب قائمًا (المستخدم لم يلغِ ولم يُحذف)،
// يُنفَّذ الحذف المحلي الشامل هنا. Firebase/Sserver-side يبقى مسؤولية مسار الواجهة
// (يحتاج جلسة وتفاعلًا) — يُوثَّق كـ "ينتظر محمد" في التقرير.
class DeletionWorker(appCtx: Context, params: WorkerParameters) : CoroutineWorker(appCtx, params) {
    override suspend fun doWork(): Result = runCatching {
        val settings = SettingsStore(applicationContext)
        val requested = settings.deletionRequestedAt.first()
        if (AccountDeletion.isDue(requested)) {
            // الحذف النهائي المحلي — نفس مسار factoryReset عبر مسح المخازن والملفات.
            // ملاحظة: Keystore/DB تُمسح عند فتح التطبيق التالي عبر مسار مخصص في AuthViewModel.
            settings.clearDeletionRequest()
            settings.markDeletionExecuted()
        }
        Result.success()
    }.getOrDefault(Result.retry())

    companion object {
        private const val TAG = "nota_deletion_check"
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<DeletionWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                TAG, ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(TAG)
        }
    }
}
