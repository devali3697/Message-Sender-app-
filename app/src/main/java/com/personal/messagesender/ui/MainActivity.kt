package com.personal.messagesender.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.personal.messagesender.databinding.ActivityMainBinding
import com.personal.messagesender.ui.viewmodel.SenderViewModel
import com.personal.messagesender.util.DateTimeUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SenderViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        if (smsGranted) {
            Toast.makeText(this, "SMS permissions granted successfully.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SMS permission is required for SMS synchronization.", Toast.LENGTH_LONG).show()
        }
        updatePermissionUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Automatic Onboarding Check
        if (!hasSmsPermission() || !isNotificationListenerGranted()) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setupListeners()
        observeViewModel()
        updatePermissionUi()
    }

    override fun onResume() {
        super.onResume()
        if (!hasSmsPermission() || !isNotificationListenerGranted()) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        viewModel.refreshState()
        updatePermissionUi()
    }

    private fun setupListeners() {
        binding.switchSync.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasSmsPermission()) {
                binding.switchSync.isChecked = false
                Toast.makeText(this, "Please grant SMS permission first.", Toast.LENGTH_SHORT).show()
                requestSmsPermissions()
            } else {
                viewModel.setSyncEnabled(isChecked)
            }
        }

        binding.btnRequestPermissions.setOnClickListener {
            requestSmsPermissions()
        }

        binding.btnGrantNotificationAccess.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open Notification Settings", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSendTest.setOnClickListener {
            viewModel.sendTestNotification()
        }

        binding.btnRetryPending.setOnClickListener {
            viewModel.retryPendingNow()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isOnline.collect { isOnline ->
                        binding.tvInternetStatus.text = if (isOnline) "Internet: Connected" else "Internet: Offline"
                        binding.tvInternetStatus.setTextColor(
                            ContextCompat.getColor(this@MainActivity, if (isOnline) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
                        )
                    }
                }

                launch {
                    viewModel.isFirebaseConnected.collect { isConnected ->
                        if (isConnected) {
                            binding.tvFirebaseStatus.text = "Firebase: Connected"
                            binding.tvFirebaseStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                        } else {
                            binding.tvFirebaseStatus.text = "Firebase: Connecting..."
                            binding.tvFirebaseStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_orange_dark))
                        }
                    }
                }

                launch {
                    viewModel.isSyncEnabled.collect { enabled ->
                        if (binding.switchSync.isChecked != enabled) {
                            binding.switchSync.isChecked = enabled
                        }
                    }
                }

                launch {
                    viewModel.lastSyncTimestamp.collect { timestamp ->
                        binding.tvLastSyncTime.text = "Last Successful Sync: ${DateTimeUtils.formatFormattedDateTime(timestamp)}"
                    }
                }

                launch {
                    viewModel.pendingCount.collect { count ->
                        binding.tvPendingCount.text = "Pending Events: $count"
                        binding.btnRetryPending.isEnabled = count > 0
                    }
                }

                launch {
                    viewModel.statusMessage.collect { msg ->
                        if (!msg.isNullOrEmpty()) {
                            binding.tvStatusMessage.text = msg
                            binding.tvStatusMessage.visibility = View.VISIBLE
                        } else {
                            binding.tvStatusMessage.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collect { error ->
                        if (!error.isNullOrEmpty()) {
                            binding.tvErrorMessage.text = "Error: $error"
                            binding.tvErrorMessage.visibility = View.VISIBLE
                        } else {
                            binding.tvErrorMessage.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isNotificationListenerGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun requestSmsPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun updatePermissionUi() {
        val hasSms = hasSmsPermission()
        if (hasSms) {
            binding.tvPermissionStatus.text = "SMS Permission: Granted"
            binding.tvPermissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            binding.btnRequestPermissions.visibility = View.GONE
        } else {
            binding.tvPermissionStatus.text = "SMS Permission: Missing"
            binding.tvPermissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.btnRequestPermissions.visibility = View.VISIBLE
        }

        val hasNotifListener = isNotificationListenerGranted()
        if (hasNotifListener) {
            binding.tvNotificationListenerStatus.text = "Notification Access (WhatsApp/Apps): Granted"
            binding.tvNotificationListenerStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            binding.btnGrantNotificationAccess.visibility = View.GONE
        } else {
            binding.tvNotificationListenerStatus.text = "Notification Access (WhatsApp/Apps): Missing"
            binding.tvNotificationListenerStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.btnGrantNotificationAccess.visibility = View.VISIBLE
        }
    }
}
