package com.example.diallog002

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LogAdapter(private val logEntries: List<LogEntry>, private val context: Context) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewPhoneNumber: TextView = view.findViewById(R.id.textViewPhoneNumber)
        val textViewCallDuration: TextView = view.findViewById(R.id.textViewCallDuration)
        val textViewListeningTime: TextView = view.findViewById(R.id.textViewListeningTime)
        val textViewSpeakingTime: TextView = view.findViewById(R.id.textViewSpeakingTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.log_entry_item, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val logEntry = logEntries[position]
        holder.textViewPhoneNumber.text = context.getString(R.string.phone_number_label, logEntry.phoneNumber)
        holder.textViewCallDuration.text = context.getString(R.string.call_duration_label, logEntry.callDuration)
        holder.textViewListeningTime.text = context.getString(R.string.listening_time_label, logEntry.listeningTime)
        holder.textViewSpeakingTime.text = context.getString(R.string.speaking_time_label, logEntry.speakingTime)
    }

    override fun getItemCount(): Int {
        return logEntries.size
    }

    fun updateLogs(logEntries: MutableList<LogEntry>) {

    }
}
