package com.personal.messageadmin.data.model

data class SenderDeviceStatus(
    val deviceId: String = "",
    val deviceName: String = "",
    val status: String = "offline",
    val lastSeen: Long = 0L,
    val lastSync: Long = 0L
)
