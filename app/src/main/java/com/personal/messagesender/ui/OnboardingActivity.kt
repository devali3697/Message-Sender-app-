package com.personal.messagesender.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.personal.messagesender.databinding.ActivityOnboardingBinding
import com.personal.messagesender.service.SmsSyncForegroundService

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            Toast.makeText(this, "SMS Permission Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SMS Permission is required for SIM alerts.", Toast.LENGTH_SHORT).show()
        }
        updateStatusUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateStatusUi()
    }

    override fun onResume() {
        super.onResume()
        updateStatusUi()
    }

    private fun setupListeners() {
        binding.btnGrantSms.setOnClickListener {
            requestSmsPermissions()
        }

        binding.btnGrantNotifAccess.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open Notification Settings", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCompleteOnboarding.setOnClickListener {
            if (!hasSmsPermission()) {
                Toast.makeText(this, "Please grant SMS permission first.", Toast.LENGTH_SHORT).show()
                requestSmsPermissions()
                return@setOnClickListener
            }

            if (!isNotificationListenerGranted()) {
                Toast.makeText(this, "Please enable Notification Access for WhatsApp.", Toast.LENGTH_SHORT).show()
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    // Ignore
                }
                return@setOnClickListener
            }

            // Start service and finish onboarding
            SmsSyncForegroundService.startService(this)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun requestSmsPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        smsPermissionLauncher.launch(permissions.toTypedArray())
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

    private fun updateStatusUi() {
        val smsOk = hasSmsPermission()
        if (smsOk) {
            binding.tvStep1Status.text = "GRANTED"
            binding.tvStep1Status.setTextColor(Color.parseColor("#2E7D32"))
            binding.tvStep1Status.setBackgroundColor(Color.parseColor("#E8F5E9"))
            binding.btnGrantSms.text = "Permission Granted ✓"
            binding.btnGrantSms.isEnabled = false
        } else {
            binding.tvStep1Status.text = "REQUIRED"
            binding.tvStep1Status.setTextColor(Color.parseColor("#C62828"))
            binding.tvStep1Status.setBackgroundColor(Color.parseColor("#FFEBEE"))
            binding.btnGrantSms.text = "1. Grant SMS Permission"
            binding.btnGrantSms.isEnabled = true
        }

        val notifOk = isNotificationListenerGranted()
        if (notifOk) {
            binding.tvStep2Status.text = "GRANTED"
            binding.tvStep2Status.setTextColor(Color.parseColor("#2E7D32"))
            binding.tvStep2Status.setBackgroundColor(Color.parseColor("#E8F5E9"))
            binding.btnGrantNotifAccess.text = "Notification Access Enabled ✓"
            binding.btnGrantNotifAccess.isEnabled = false
        } else {
            binding.tvStep2Status.text = "REQUIRED"
            binding.tvStep2Status.setTextColor(Color.parseColor("#C62828"))
            binding.tvStep2Status.setBackgroundColor(Color.parseColor("#FFEBEE"))
            binding.btnGrantNotifAccess.text = "2. Enable Notification Access"
            binding.btnGrantNotifAccess.isEnabled = true
        }

        if (smsOk && notifOk) {
            binding.btnCompleteOnboarding.text = "Complete Setup & Launch Dashboard"
            binding.btnCompleteOnboarding.setBackgroundColor(Color.parseColor("#2E7D32"))
        } else {
            binding.btnCompleteOnboarding.text = "Continue Setup"
        }
    }
}
