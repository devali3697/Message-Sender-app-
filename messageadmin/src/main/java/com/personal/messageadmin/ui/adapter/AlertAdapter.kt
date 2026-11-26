package com.personal.messageadmin.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personal.messageadmin.data.model.AdminSmsAlert
import com.personal.messageadmin.databinding.ItemAlertBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertAdapter(
    private val onAlertClick: (AdminSmsAlert) -> Unit,
    private val onDeleteClick: (AdminSmsAlert) -> Unit
) : ListAdapter<AdminSmsAlert, AlertAdapter.AlertViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlertViewHolder(
        private val binding: ItemAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: AdminSmsAlert) {
            binding.tvAppNameBadge.text = alert.appName.ifEmpty { "SMS" }
            binding.tvAppNameBadge.setBackgroundColor(getAppColor(alert.appName))

            binding.tvSenderPhone.text = alert.sender.ifEmpty { "Unknown" }
            binding.tvMessageContent.text = if (alert.message.isNotEmpty()) alert.message else "(No message body)"
            binding.tvTimestamp.text = formatTimestamp(alert.timestamp)

            if (!alert.read) {
                binding.tvUnreadBadge.visibility = View.VISIBLE
                binding.cardAlert.setCardBackgroundColor(Color.parseColor("#F0F7FF"))
            } else {
                binding.tvUnreadBadge.visibility = View.GONE
                binding.cardAlert.setCardBackgroundColor(Color.WHITE)
            }

            if (alert.isTestAlert) {
                binding.tvTestBadge.visibility = View.VISIBLE
            } else {
                binding.tvTestBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onAlertClick(alert)
            }

            binding.btnDeleteAlert.setOnClickListener {
                onDeleteClick(alert)
            }
        }

        private fun getAppColor(appName: String): Int {
            val lower = appName.lowercase()
            return when {
                lower.contains("whatsapp") -> Color.parseColor("#2E7D32")
                lower.contains("telegram") -> Color.parseColor("#0288D1")
                lower.contains("sms") || lower.contains("message") -> Color.parseColor("#1565C0")
                else -> Color.parseColor("#6A1B9A")
            }
        }

        private fun formatTimestamp(time: Long): String {
            if (time <= 0L) return "Unknown Time"
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(time))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AdminSmsAlert>() {
        override fun areItemsTheSame(oldItem: AdminSmsAlert, newItem: AdminSmsAlert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AdminSmsAlert, newItem: AdminSmsAlert): Boolean {
            return oldItem == newItem
        }
    }
}
