package com.personal.messageadmin.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.personal.messageadmin.data.local.AdminPreferences
import com.personal.messageadmin.data.model.AdminSmsAlert
import com.personal.messageadmin.data.model.SenderDeviceStatus
import com.personal.messageadmin.data.remote.AdminFirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminFirebaseRepository()
    private val prefs = AdminPreferences(application)

    private val _rawAlertsList = MutableStateFlow<List<AdminSmsAlert>>(emptyList())
    private val _devicesList = MutableStateFlow<List<SenderDeviceStatus>>(emptyList())
    val devicesList: StateFlow<List<SenderDeviceStatus>> = _devicesList.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    private val _filteredAlertsList = MutableStateFlow<List<AdminSmsAlert>>(emptyList())
    val alertsList: StateFlow<List<AdminSmsAlert>> = _filteredAlertsList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_rawAlertsList, _selectedDeviceId) { alerts, selectedId ->
                if (selectedId.isNullOrEmpty()) {
                    alerts
                } else {
                    alerts.filter { it.deviceId == selectedId }
                }
            }.collect { filtered ->
                _filteredAlertsList.value = filtered
            }
        }
        autoConnectAndObserve()
    }

    fun selectDeviceFilter(deviceId: String?) {
        _selectedDeviceId.value = deviceId
    }

    fun autoConnectAndObserve() {
        viewModelScope.launch {
            _isLoading.value = true
            val authRes = repository.ensureAuthenticated()
            if (authRes.isSuccess) {
                _isFirebaseConnected.value = true
                _errorMessage.value = null
                startObservingData()
                retrieveFcmToken()
            } else {
                _isFirebaseConnected.value = false
                _errorMessage.value = authRes.exceptionOrNull()?.localizedMessage ?: "Firebase connection failed"
                _isLoading.value = false
            }
        }
    }

    private fun startObservingData() {
        viewModelScope.launch {
            launch {
                repository.getAlertsFlow()
                    .catch { e ->
                        _errorMessage.value = "Failed to load alerts: ${e.localizedMessage}"
                        _isLoading.value = false
                        _isRefreshing.value = false
                    }
                    .collect { list ->
                        _rawAlertsList.value = list
                        _isLoading.value = false
                        _isRefreshing.value = false
                    }
            }

            launch {
                repository.getDevicesListFlow()
                    .catch { e ->
                        // Ignore
                    }
                    .collect { devices ->
                        _devicesList.value = devices
                    }
            }
        }
    }

    private fun retrieveFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (!token.isNullOrEmpty()) {
                    prefs.fcmToken = token
                    viewModelScope.launch {
                        repository.updateFcmToken(token)
                    }
                }
            }
        }
    }

    fun refreshData() {
        _isRefreshing.value = true
        autoConnectAndObserve()
    }

    fun markAsRead(alert: AdminSmsAlert) {
        if (alert.read) return
        viewModelScope.launch {
            repository.markAlertAsRead(alert.id)
        }
    }

    fun deleteAlert(alert: AdminSmsAlert) {
        viewModelScope.launch {
            repository.deleteAlert(alert.id)
        }
    }

    fun deleteAllAlerts() {
        viewModelScope.launch {
            repository.deleteAllAlerts()
        }
    }
}
