package com.personal.messagesender.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.personal.messagesender.data.local.SyncPreferences
import com.personal.messagesender.data.remote.FirebaseSenderRepository
import java.util.concurrent.TimeUnit

class PendingSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val prefs = SyncPreferences(applicationContext)
    private val repository = FirebaseSenderRepository()

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        if (!prefs.isSyncEnabled) {
            return androidx.work.ListenableWorker.Result.success()
        }

        val authResult = repository.ensureAuthenticated(applicationContext)
        if (authResult.isFailure) {
            return androidx.work.ListenableWorker.Result.retry()
        }

        val pendingList = prefs.getPendingAlerts()
        if (pendingList.isEmpty()) {
            return androidx.work.ListenableWorker.Result.success()
        }

        var hasFailure = false
        for (alert in pendingList) {
            val syncResult = repository.syncAlert(applicationContext, alert)
            if (syncResult.isSuccess) {
                prefs.removePendingAlert(alert.id)
                prefs.lastSyncTimestamp = System.currentTimeMillis()
            } else {
                hasFailure = true
            }
        }

        return if (hasFailure) {
            androidx.work.ListenableWorker.Result.retry()
        } else {
            androidx.work.ListenableWorker.Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "pending_sms_sync_worker"

        fun enqueueWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<PendingSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
