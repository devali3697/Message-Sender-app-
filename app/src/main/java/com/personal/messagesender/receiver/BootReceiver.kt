package com.personal.messagesender.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.personal.messagesender.data.local.SyncPreferences
import com.personal.messagesender.service.SmsSyncForegroundService
import com.personal.messagesender.worker.PendingSyncWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = SyncPreferences(context)
            if (prefs.isSyncEnabled) {
                // Automatically start background service after boot
                SmsSyncForegroundService.startService(context)

                // Enqueue any pending alerts retry
                val pendingList = prefs.getPendingAlerts()
                if (pendingList.isNotEmpty()) {
                    PendingSyncWorker.enqueueWork(context)
                }
            }
        }
    }
}
