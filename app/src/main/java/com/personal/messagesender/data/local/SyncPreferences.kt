package com.personal.messagesender.data.local

import android.content.Context
import android.content.SharedPreferences
import com.personal.messagesender.data.model.SmsAlert
import org.json.JSONArray
import org.json.JSONObject

class SyncPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_SYNC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_ENABLED, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    @Synchronized
    fun getPendingAlerts(): List<SmsAlert> {
        val jsonStr = prefs.getString(KEY_PENDING_ALERTS, "[]") ?: "[]"
        val list = mutableListOf<SmsAlert>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SmsAlert(
                        id = obj.optString("id"),
                        sender = obj.optString("sender"),
                        appName = obj.optString("appName", "SMS"),
                        message = obj.optString("message"),
                        timestamp = obj.optLong("timestamp"),
                        synced = obj.optBoolean("synced", false),
                        syncedAt = obj.optLong("syncedAt", 0L),
                        isTestAlert = obj.optBoolean("isTestAlert", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @Synchronized
    fun addPendingAlert(alert: SmsAlert) {
        val currentList = getPendingAlerts().toMutableList()
        // Deduplication check: Avoid duplicate alerts with same sender, app, & message within 5s
        val isDuplicate = currentList.any { existing ->
            existing.sender == alert.sender &&
            existing.appName == alert.appName &&
            existing.message == alert.message &&
            Math.abs(existing.timestamp - alert.timestamp) < 5000L
        }
        if (!isDuplicate) {
            currentList.add(alert)
            savePendingAlerts(currentList)
        }
    }

    @Synchronized
    fun removePendingAlert(alertId: String) {
        val currentList = getPendingAlerts().filterNot { it.id == alertId }
        savePendingAlerts(currentList)
    }

    @Synchronized
    fun clearPendingAlerts() {
        prefs.edit().remove(KEY_PENDING_ALERTS).apply()
    }

    private fun savePendingAlerts(list: List<SmsAlert>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("sender", item.sender)
                put("appName", item.appName)
                put("message", item.message)
                put("timestamp", item.timestamp)
                put("synced", item.synced)
                put("syncedAt", item.syncedAt)
                put("isTestAlert", item.isTestAlert)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PENDING_ALERTS, array.toString()).apply()
    }

    companion object {
        private const val PREF_NAME = "message_sender_prefs"
        private const val KEY_SYNC_ENABLED = "key_sync_enabled"
        private const val KEY_LAST_SYNC_TIME = "key_last_sync_time"
        private const val KEY_PENDING_ALERTS = "key_pending_alerts"
    }
}
