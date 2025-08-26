package com.example.diallog002

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.diallog002.data.CallLog

class CallLogsAdapter(
    private var callLogs: MutableList<CallLog>,
    private val onFavoriteChanged: (CallLog, Boolean) -> Unit
) : RecyclerView.Adapter<CallLogsAdapter.CallLogViewHolder>() {

    fun updateCallLogs(newCallLogs: List<CallLog>) {
        callLogs.clear()
        callLogs.addAll(newCallLogs)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call_log, parent, false)
        return CallLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val callLog = callLogs[position]
        holder.bind(callLog)
    }

    override fun getItemCount(): Int = callLogs.size

    inner class CallLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContactName: TextView = itemView.findViewById(R.id.tvContactName)
        private val tvSpeakingTime: TextView = itemView.findViewById(R.id.tvSpeakingTime)
        private val tvListeningTime: TextView = itemView.findViewById(R.id.tvListeningTime)
        private val cbFavorite: CheckBox = itemView.findViewById(R.id.cbFavorite)

        fun bind(callLog: CallLog) {
            tvContactName.text = callLog.contactName
            tvSpeakingTime.text = "Speaking: ${callLog.speakingTime / 1000}s"
            tvListeningTime.text = "Listening: ${callLog.listeningTime / 1000}s"
            cbFavorite.isChecked = callLog.favorite

            cbFavorite.setOnCheckedChangeListener { _, isChecked ->
                onFavoriteChanged(callLog, isChecked)
            }
        }
    }
}