package com.personal.messagesender.data.model

import java.util.UUID

data class SmsAlert(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String = "",
    val deviceName: String = "",
    val sender: String = "",
    val appName: String = "SMS",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val syncedAt: Long = 0L,
    val isTestAlert: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "sender" to sender,
            "appName" to appName,
            "message" to message,
            "timestamp" to timestamp,
            "synced" to synced,
            "syncedAt" to syncedAt,
            "isTestAlert" to isTestAlert
        )
    }
}
