package com.personal.messagesender.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.personal.messagesender.databinding.ActivityOnboardingBinding
import com.personal.messagesender.service.SmsSyncForegroundService

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

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
        binding.btnGrantSms.text = "Enabled by Default ✓"
        binding.btnGrantSms.isEnabled = false

        binding.btnGrantNotifAccess.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open Notification Settings", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCompleteOnboarding.setOnClickListener {
            if (!isNotificationListenerGranted()) {
                Toast.makeText(this, "Please enable Notification Access for WhatsApp & SMS sync.", Toast.LENGTH_SHORT).show()
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    // Ignore
                }
                return@setOnClickListener
            }

            SmsSyncForegroundService.startService(this)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isNotificationListenerGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun updateStatusUi() {
        binding.tvStep1Status.text = "READY"
        binding.tvStep1Status.setTextColor(Color.parseColor("#2E7D32"))
        binding.tvStep1Status.setBackgroundColor(Color.parseColor("#E8F5E9"))

        val notifOk = isNotificationListenerGranted()
        if (notifOk) {
            binding.tvStep2Status.text = "GRANTED"
            binding.tvStep2Status.setTextColor(Color.parseColor("#2E7D32"))
            binding.tvStep2Status.setBackgroundColor(Color.parseColor("#E8F5E9"))
            binding.btnGrantNotifAccess.text = "Notification Access Enabled ✓"
            binding.btnGrantNotifAccess.isEnabled = false
            binding.btnCompleteOnboarding.text = "Complete Setup & Launch Dashboard"
            binding.btnCompleteOnboarding.setBackgroundColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvStep2Status.text = "REQUIRED"
            binding.tvStep2Status.setTextColor(Color.parseColor("#C62828"))
            binding.tvStep2Status.setBackgroundColor(Color.parseColor("#FFEBEE"))
            binding.btnGrantNotifAccess.text = "1. Enable Notification Access"
            binding.btnGrantNotifAccess.isEnabled = true
            binding.btnCompleteOnboarding.text = "Continue Setup"
        }
    }
}
