package com.personal.messagesender.data.privacy

import android.content.Intent
import android.provider.Telephony
import com.personal.messagesender.data.model.SmsAlert
import java.util.UUID

object SmsPrivacyFilter {

    fun extractAlert(intent: Intent): SmsAlert? {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return null

        val firstMessage = messages[0]
        val rawSender = firstMessage.originatingAddress ?: "Unknown Sender"
        val timestamp = if (firstMessage.timestampMillis > 0) firstMessage.timestampMillis else System.currentTimeMillis()

        val fullBody = messages.joinToString(separator = "") { it.displayMessageBody ?: it.messageBody ?: "" }.trim()

        return SmsAlert(
            id = UUID.randomUUID().toString(),
            sender = rawSender.trim().take(50),
            appName = "SMS (SIM)",
            message = fullBody,
            timestamp = timestamp,
            synced = false,
            syncedAt = 0L,
            isTestAlert = false
        )
    }
}
