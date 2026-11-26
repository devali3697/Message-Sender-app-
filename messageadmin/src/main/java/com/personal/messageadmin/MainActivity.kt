package com.personal.messageadmin

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.personal.messageadmin.data.model.SenderDeviceStatus
import com.personal.messageadmin.databinding.ActivityMainBinding
import com.personal.messageadmin.ui.adapter.AlertAdapter
import com.personal.messageadmin.ui.viewmodel.AdminViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var adapter: AlertAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        requestNotificationPermission()
    }

    private fun setupRecyclerView() {
        adapter = AlertAdapter(
            onAlertClick = { alert ->
                viewModel.markAsRead(alert)
            },
            onDeleteClick = { alert ->
                viewModel.deleteAlert(alert)
                Toast.makeText(this, "Alert deleted", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvAlerts.layoutManager = LinearLayoutManager(this)
        binding.rvAlerts.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }

        binding.chipAllDevices.setOnClickListener {
            viewModel.selectDeviceFilter(null)
        }

        binding.btnClearAllAlerts.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Alert History")
                .setMessage("Are you sure you want to delete all synchronized alert records?")
                .setPositiveButton("Clear All") { _, _ ->
                    viewModel.deleteAllAlerts()
                    Toast.makeText(this, "All alert history cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.alertsList.collect { list ->
                        adapter.submitList(list)
                        if (list.isEmpty() && !viewModel.isLoading.value) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.rvAlerts.visibility = View.GONE
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.rvAlerts.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.devicesList.collect { devices ->
                        updateDeviceChips(devices)
                        updateSelectedDeviceStatus(devices, viewModel.selectedDeviceId.value)
                    }
                }

                launch {
                    viewModel.selectedDeviceId.collect { selectedId ->
                        updateSelectedDeviceStatus(viewModel.devicesList.value, selectedId)
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.isRefreshing.collect { refreshing ->
                        binding.swipeRefreshLayout.isRefreshing = refreshing
                    }
                }

                launch {
                    viewModel.errorMessage.collect { err ->
                        if (!err.isNullOrEmpty()) {
                            binding.tvErrorBanner.text = "Error: $err"
                            binding.tvErrorBanner.visibility = View.VISIBLE
                        } else {
                            binding.tvErrorBanner.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun updateDeviceChips(devices: List<SenderDeviceStatus>) {
        val currentCount = binding.cgDevices.childCount
        // Keep the first "All Devices" chip
        if (currentCount > 1) {
            binding.cgDevices.removeViews(1, currentCount - 1)
        }

        devices.forEach { dev ->
            val chip = Chip(this).apply {
                val statusTag = if (dev.status == "online") "🟢 ONLINE" else "🔴 OFFLINE"
                text = "${dev.deviceName} ($statusTag)"
                isCheckable = true
                isChecked = viewModel.selectedDeviceId.value == dev.deviceId
                setOnClickListener {
                    viewModel.selectDeviceFilter(dev.deviceId)
                }
            }
            binding.cgDevices.addView(chip)
        }
    }

    private fun updateSelectedDeviceStatus(devices: List<SenderDeviceStatus>, selectedId: String?) {
        if (selectedId.isNullOrEmpty()) {
            val onlineCount = devices.count { it.status == "online" }
            binding.tvSenderDeviceStatus.text = "Devices Online: $onlineCount / ${devices.size}"
            binding.tvSenderDeviceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            binding.tvLastSeenTime.text = "Filtering: All Sender Devices"
        } else {
            val targetDev = devices.find { it.deviceId == selectedId }
            if (targetDev != null) {
                val isOnline = targetDev.status == "online"
                binding.tvSenderDeviceStatus.text = "${targetDev.deviceName}: ${if (isOnline) "ONLINE" else "OFFLINE"}"
                binding.tvSenderDeviceStatus.setTextColor(
                    ContextCompat.getColor(this, if (isOnline) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
                )

                val lastSeenStr = formatTime(targetDev.lastSeen)
                val lastSyncStr = formatTime(targetDev.lastSync)
                binding.tvLastSeenTime.text = "Last Seen: $lastSeenStr | Last Sync: $lastSyncStr"
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Never"
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
