package com.example.diallog002

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.diallog002.data.CallLog
import java.text.SimpleDateFormat
import java.util.*

class CallHistoryAdapter(
    private var callLogs: List<CallLog>,
    private val onCallLogClick: (CallLog) -> Unit,
    private val onDeleteClick: (CallLog) -> Unit
) : RecyclerView.Adapter<CallHistoryAdapter.CallHistoryViewHolder>() {

    class CallHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contact_name_history)
        val callDate: TextView = itemView.findViewById(R.id.call_date)
        val speakingTime: TextView = itemView.findViewById(R.id.speaking_time_history)
        val listeningTime: TextView = itemView.findViewById(R.id.listening_time_history)
        val totalDuration: TextView = itemView.findViewById(R.id.total_duration_history)
        val deleteAction: View? = itemView.findViewById(R.id.delete_action)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_history, parent, false)
        return CallHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallHistoryViewHolder, position: Int) {
        val callLog = callLogs[position]
        
        holder.contactName.text = callLog.contactName
        
        // Format the date
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.callDate.text = dateFormat.format(callLog.timestamp)
        
        // Format time durations
        holder.speakingTime.text = formatDuration(callLog.speakingTime)
        holder.listeningTime.text = formatDuration(callLog.listeningTime)
        holder.totalDuration.text = "Total: ${formatDuration(callLog.totalDuration)}"
        
        holder.itemView.setOnClickListener {
            onCallLogClick(callLog)
        }
        
        holder.deleteAction?.setOnClickListener {
            onDeleteClick(callLog)
        }
    }

    override fun getItemCount() = callLogs.size

    fun updateCallLogs(newCallLogs: List<CallLog>) {
        callLogs = newCallLogs
        notifyDataSetChanged()
    }

    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return if (minutes > 0) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${remainingSeconds}s"
        }
    }
}
