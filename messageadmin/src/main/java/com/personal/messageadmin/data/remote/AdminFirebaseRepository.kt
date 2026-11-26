package com.personal.messageadmin.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.personal.messageadmin.data.model.AdminSmsAlert
import com.personal.messageadmin.data.model.SenderDeviceStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminFirebaseRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance("https://inamglink-default-rtdb.firebaseio.com")
    }

    fun isAuthenticated(): Boolean = auth.currentUser != null

    suspend fun ensureAuthenticated(): Result<Unit> {
        if (auth.currentUser != null) {
            return Result.success(Unit)
        }
        return try {
            auth.signInWithEmailAndPassword("device_admin@inamglink.app", "Messageadmin123!").await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                auth.createUserWithEmailAndPassword("device_admin@inamglink.app", "Messageadmin123!").await()
                Result.success(Unit)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    fun getAlertsFlow(): Flow<List<AdminSmsAlert>> = callbackFlow {
        val alertsRef = database.getReference("alerts").orderByChild("timestamp")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<AdminSmsAlert>()
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                    val deviceId = child.child("deviceId").getValue(String::class.java) ?: ""
                    val deviceName = child.child("deviceName").getValue(String::class.java) ?: ""
                    val sender = child.child("sender").getValue(String::class.java) ?: "Unknown"
                    val appName = child.child("appName").getValue(String::class.java) ?: "SMS"
                    val message = child.child("message").getValue(String::class.java) ?: ""
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val synced = child.child("synced").getValue(Boolean::class.java) ?: true
                    val syncedAt = child.child("syncedAt").getValue(Long::class.java) ?: 0L
                    val read = child.child("read").getValue(Boolean::class.java) ?: false
                    val isTestAlert = child.child("isTestAlert").getValue(Boolean::class.java) ?: false

                    list.add(
                        AdminSmsAlert(
                            id = id,
                            deviceId = deviceId,
                            deviceName = deviceName,
                            sender = sender,
                            appName = appName,
                            message = message,
                            timestamp = timestamp,
                            synced = synced,
                            syncedAt = syncedAt,
                            read = read,
                            isTestAlert = isTestAlert
                        )
                    )
                }
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        alertsRef.addValueEventListener(listener)
        awaitClose { alertsRef.removeEventListener(listener) }
    }

    fun getDevicesListFlow(): Flow<List<SenderDeviceStatus>> = callbackFlow {
        val devicesRef = database.getReference("devices")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<SenderDeviceStatus>()
                for (child in snapshot.children) {
                    if (child.key == "admin" || child.key == "sender") continue
                    val devId = child.child("deviceId").getValue(String::class.java) ?: child.key ?: ""
                    val devName = child.child("deviceName").getValue(String::class.java) ?: devId
                    val status = child.child("status").getValue(String::class.java) ?: "offline"
                    val lastSeen = child.child("last_seen").getValue(Long::class.java) ?: 0L
                    val lastSync = child.child("last_sync").getValue(Long::class.java) ?: 0L

                    list.add(
                        SenderDeviceStatus(
                            deviceId = devId,
                            deviceName = devName,
                            status = status,
                            lastSeen = lastSeen,
                            lastSync = lastSync
                        )
                    )
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore
            }
        }

        devicesRef.addValueEventListener(listener)
        awaitClose { devicesRef.removeEventListener(listener) }
    }

    suspend fun markAlertAsRead(alertId: String): Result<Unit> {
        if (alertId.isEmpty()) return Result.success(Unit)
        return try {
            database.getReference("alerts").child(alertId).child("read").setValue(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAlert(alertId: String): Result<Unit> {
        if (alertId.isEmpty()) return Result.success(Unit)
        return try {
            database.getReference("alerts").child(alertId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllAlerts(): Result<Unit> {
        return try {
            database.getReference("alerts").removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> {
        ensureAuthenticated()
        return try {
            database.getReference("devices/admin/fcm_token").setValue(token).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
