package com.personal.messagesender.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.messagesender.data.local.SyncPreferences
import com.personal.messagesender.data.model.SmsAlert
import com.personal.messagesender.data.network.NetworkMonitor
import com.personal.messagesender.data.remote.FirebaseSenderRepository
import com.personal.messagesender.service.SmsSyncForegroundService
import com.personal.messagesender.worker.PendingSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SenderViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SyncPreferences(application)
    private val repository = FirebaseSenderRepository()
    private val networkMonitor = NetworkMonitor(application)

    private val _isOnline = MutableStateFlow(networkMonitor.isCurrentlyConnected())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _isSyncEnabled = MutableStateFlow(prefs.isSyncEnabled)
    val isSyncEnabled: StateFlow<Boolean> = _isSyncEnabled.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(prefs.lastSyncTimestamp)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _pendingCount = MutableStateFlow(prefs.getPendingAlerts().size)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online || _isFirebaseConnected.value
                if (online) {
                    autoConnectFirebase()
                }
            }
        }
        if (prefs.isSyncEnabled) {
            SmsSyncForegroundService.startService(application)
        }
        autoConnectFirebase()
        refreshState()
    }

    fun refreshState() {
        _isSyncEnabled.value = prefs.isSyncEnabled
        _lastSyncTimestamp.value = prefs.lastSyncTimestamp
        _pendingCount.value = prefs.getPendingAlerts().size
        autoConnectFirebase()
    }

    private fun autoConnectFirebase() {
        viewModelScope.launch {
            val res = repository.ensureAuthenticated(getApplication())
            if (res.isSuccess) {
                _isFirebaseConnected.value = true
                _isOnline.value = true
                _errorMessage.value = null
            } else {
                _isFirebaseConnected.value = false
                _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Firebase auto connection failed"
            }
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        prefs.isSyncEnabled = enabled
        _isSyncEnabled.value = enabled
        if (enabled) {
            SmsSyncForegroundService.startService(getApplication())
            _statusMessage.value = "SMS Alert Synchronization Enabled (Foreground Service Active)"
            autoConnectFirebase()
        } else {
            SmsSyncForegroundService.stopService(getApplication())
            _statusMessage.value = "SMS Alert Synchronization Disabled"
        }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            val testAlert = SmsAlert(
                id = UUID.randomUUID().toString(),
                sender = "+1-555-0199 (Test)",
                appName = "Test App",
                message = "This is a test notification payload.",
                timestamp = System.currentTimeMillis(),
                synced = false,
                isTestAlert = true
            )

            if (_isOnline.value) {
                val syncResult = repository.syncAlert(getApplication(), testAlert)
                if (syncResult.isSuccess) {
                    prefs.lastSyncTimestamp = System.currentTimeMillis()
                    _statusMessage.value = "Test Alert Synchronized Successfully!"
                    _errorMessage.value = null
                } else {
                    val err = syncResult.exceptionOrNull()?.localizedMessage ?: "Failed to sync test alert"
                    _errorMessage.value = err
                    prefs.addPendingAlert(testAlert)
                    PendingSyncWorker.enqueueWork(getApplication())
                }
            } else {
                prefs.addPendingAlert(testAlert)
                PendingSyncWorker.enqueueWork(getApplication())
                _statusMessage.value = "Offline: Test Alert queued for retry"
            }
            refreshState()
        }
    }

    fun retryPendingNow() {
        viewModelScope.launch {
            val pendingList = prefs.getPendingAlerts()
            if (pendingList.isEmpty()) {
                _statusMessage.value = "No pending alerts to retry."
                return@launch
            }

            val authRes = repository.ensureAuthenticated(getApplication())
            if (authRes.isFailure) {
                _errorMessage.value = "Retry failed: Firebase connection error."
                return@launch
            }

            PendingSyncWorker.enqueueWork(getApplication())
            _statusMessage.value = "Pending sync retry scheduled with WorkManager."
            refreshState()
        }
    }
}
