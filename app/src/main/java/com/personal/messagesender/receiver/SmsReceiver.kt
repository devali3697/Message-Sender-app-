package com.personal.messagesender.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.personal.messagesender.data.local.SyncPreferences
import com.personal.messagesender.data.network.NetworkMonitor
import com.personal.messagesender.data.privacy.SmsPrivacyFilter
import com.personal.messagesender.data.remote.FirebaseSenderRepository
import com.personal.messagesender.service.SmsSyncForegroundService
import com.personal.messagesender.worker.PendingSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = SyncPreferences(context)
        if (!prefs.isSyncEnabled) return

        try {
            SmsSyncForegroundService.startService(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val alert = SmsPrivacyFilter.extractAlert(intent) ?: return

        val repository = FirebaseSenderRepository()
        val networkMonitor = NetworkMonitor(context)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (networkMonitor.isCurrentlyConnected()) {
                    val result = repository.syncAlert(context, alert)
                    if (result.isSuccess) {
                        prefs.lastSyncTimestamp = System.currentTimeMillis()
                    } else {
                        prefs.addPendingAlert(alert)
                        PendingSyncWorker.enqueueWork(context)
                    }
                } else {
                    prefs.addPendingAlert(alert)
                    PendingSyncWorker.enqueueWork(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                prefs.addPendingAlert(alert)
                PendingSyncWorker.enqueueWork(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
