package com.personal.messagesender.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.personal.messagesender.data.model.SmsAlert
import com.personal.messagesender.util.DeviceUtils
import kotlinx.coroutines.tasks.await

class FirebaseSenderRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance("https://inamglink-default-rtdb.firebaseio.com")
    }

    fun isAuthenticated(): Boolean = auth.currentUser != null

    suspend fun ensureAuthenticated(context: Context): Result<Unit> {
        if (auth.currentUser != null) {
            setupPresence(context)
            return Result.success(Unit)
        }
        return try {
            auth.signInWithEmailAndPassword("device_sender@inamglink.app", "Messagesender123!").await()
            setupPresence(context)
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                auth.createUserWithEmailAndPassword("device_sender@inamglink.app", "Messagesender123!").await()
                setupPresence(context)
                Result.success(Unit)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    fun setupPresence(context: Context) {
        if (!isAuthenticated()) return
        try {
            val deviceId = DeviceUtils.getDeviceId(context)
            val deviceName = DeviceUtils.getDeviceName()

            val deviceRef = database.getReference("devices").child(deviceId)
            val statusRef = deviceRef.child("status")
            val lastSeenRef = deviceRef.child("last_seen")

            deviceRef.child("deviceId").setValue(deviceId)
            deviceRef.child("deviceName").setValue(deviceName)

            val connectedRef = database.getReference(".info/connected")
            connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        statusRef.onDisconnect().setValue("offline")
                        lastSeenRef.onDisconnect().setValue(ServerValue.TIMESTAMP)

                        statusRef.setValue("online")
                        lastSeenRef.setValue(ServerValue.TIMESTAMP)
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    // Ignored
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncAlert(context: Context, alert: SmsAlert): Result<Unit> {
        val authResult = ensureAuthenticated(context)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: IllegalStateException("Firebase auto-auth failed"))
        }

        return try {
            val deviceId = DeviceUtils.getDeviceId(context)
            val deviceName = DeviceUtils.getDeviceName()

            val alertRef = database.getReference("alerts").child(alert.id)
            val updatedAlert = alert.copy(
                deviceId = deviceId,
                deviceName = deviceName,
                synced = true,
                syncedAt = System.currentTimeMillis()
            )
            alertRef.setValue(updatedAlert.toMap()).await()

            // Update sender device last sync timestamp
            val deviceRef = database.getReference("devices").child(deviceId)
            deviceRef.child("last_sync").setValue(ServerValue.TIMESTAMP)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
