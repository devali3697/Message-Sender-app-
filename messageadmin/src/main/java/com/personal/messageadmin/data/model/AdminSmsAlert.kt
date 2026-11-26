package com.personal.messageadmin.data.model

data class AdminSmsAlert(
    val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val sender: String = "",
    val appName: String = "SMS",
    val message: String = "",
    val timestamp: Long = 0L,
    val synced: Boolean = true,
    val syncedAt: Long = 0L,
    val read: Boolean = false,
    val isTestAlert: Boolean = false
)
