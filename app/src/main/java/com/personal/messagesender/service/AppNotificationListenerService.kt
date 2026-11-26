package com.personal.messagesender.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.personal.messagesender.data.local.SyncPreferences
import com.personal.messagesender.data.model.SmsAlert
import com.personal.messagesender.data.network.NetworkMonitor
import com.personal.messagesender.data.remote.FirebaseSenderRepository
import com.personal.messagesender.worker.PendingSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AppNotificationListenerService : NotificationListenerService() {

    private val repository by lazy { FirebaseSenderRepository() }
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val recentNotificationCache = ConcurrentHashMap<String, Long>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        // Ignore our own app and system UI layout bars
        if (packageName == applicationContext.packageName) return
        if (packageName == "android" || packageName == "com.android.systemui") return

        val prefs = SyncPreferences(applicationContext)
        if (!prefs.isSyncEnabled) return

        val notification = sbn.notification ?: return
        val appLabel = getAppNameFromPackage(packageName)

        val (parsedTitle, parsedText) = parseNotificationContent(notification)

        val sender = if (parsedTitle.isNotEmpty()) parsedTitle else appLabel
        val messageText = parsedText

        if (sender.isEmpty() && messageText.isEmpty()) return

        val now = System.currentTimeMillis()
        val dedupeKey = "${packageName}_${sender}_${messageText}"

        val lastSeen = recentNotificationCache[dedupeKey]
        if (lastSeen != null && (now - lastSeen) < 3000L) {
            return
        }
        recentNotificationCache[dedupeKey] = now

        if (recentNotificationCache.size > 150) {
            recentNotificationCache.entries.removeIf { (now - it.value) > 10000L }
        }

        val timestamp = if (sbn.postTime > 0) sbn.postTime else now

        val alert = SmsAlert(
            id = UUID.randomUUID().toString(),
            sender = sender.take(60),
            appName = appLabel,
            message = messageText,
            timestamp = timestamp,
            synced = false,
            isTestAlert = false
        )

        val networkMonitor = NetworkMonitor(applicationContext)

        scope.launch {
            try {
                if (networkMonitor.isCurrentlyConnected()) {
                    val result = repository.syncAlert(applicationContext, alert)
                    if (result.isSuccess) {
                        prefs.lastSyncTimestamp = System.currentTimeMillis()
                    } else {
                        prefs.addPendingAlert(alert)
                        PendingSyncWorker.enqueueWork(applicationContext)
                    }
                } else {
                    prefs.addPendingAlert(alert)
                    PendingSyncWorker.enqueueWork(applicationContext)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                prefs.addPendingAlert(alert)
                PendingSyncWorker.enqueueWork(applicationContext)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun parseNotificationContent(notification: Notification): Pair<String, String> {
        val extras = notification.extras ?: return Pair("", "")

        var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""

        if (text.isEmpty()) {
            text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
        }

        try {
            val messagesParcelable = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (!messagesParcelable.isNullOrEmpty()) {
                val lastMsg = messagesParcelable.lastOrNull()
                if (lastMsg is Bundle) {
                    val msgText = lastMsg.getCharSequence("text")?.toString()?.trim()
                    val msgSender = lastMsg.getCharSequence("sender")?.toString()?.trim()
                    if (!msgText.isNullOrEmpty()) {
                        text = msgText
                    }
                    if (!msgSender.isNullOrEmpty() && (title.isEmpty() || title == "WhatsApp")) {
                        title = msgSender
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        if (text.isEmpty()) {
            text = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""
        }

        if (text.isEmpty() && notification.tickerText != null) {
            text = notification.tickerText.toString().trim()
        }

        return Pair(title, text)
    }

    private fun getAppNameFromPackage(pkg: String): String {
        return try {
            val pm = applicationContext.packageManager
            val ai = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            when {
                pkg.contains("whatsapp") -> "WhatsApp"
                pkg.contains("telegram") -> "Telegram"
                pkg.contains("facebook") || pkg.contains("orca") -> "Messenger"
                else -> pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }
    }
}
