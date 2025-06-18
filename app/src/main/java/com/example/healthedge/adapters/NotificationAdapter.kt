package com.example.healthedge.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.healthedge.R
import com.example.healthedge.models.Notification
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.notificationMessage)
        val dateText: TextView = view.findViewById(R.id.notificationDate)
        val unreadIndicator: View = view.findViewById(R.id.unreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.messageText.text = notification.message
        
        try {
            // Parse ISO 8601 date format
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(notification.created_at)
            
            // Format for display
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            holder.dateText.text = date?.let { outputFormat.format(it) } ?: notification.created_at
        } catch (e: Exception) {
            // Fallback to raw date string if parsing fails
            holder.dateText.text = notification.created_at
        }

        holder.unreadIndicator.visibility = if (notification.is_read == 0) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener {
            onNotificationClick(notification)
        }
    }

    override fun getItemCount() = notifications.size
} 