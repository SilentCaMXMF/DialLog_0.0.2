package com.example.diallog002

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diallog002.data.CallLog
import com.example.diallog002.data.CallLogManager
import com.example.diallog002.data.CallAnalyticsService
import com.example.diallog002.data.TimeRange
import kotlinx.coroutines.*

class CallHistoryActivity : AppCompatActivity() {
    private lateinit var callHistoryRecyclerView: RecyclerView
    private lateinit var callHistoryAdapter: CallHistoryAdapter
    private lateinit var analyticsButton: Button
    private lateinit var analyticsService: CallAnalyticsService
    private lateinit var totalCallsStat: TextView
    private lateinit var totalDurationStat: TextView
    private lateinit var avgRatioStat: TextView
    private var callLogs = mutableListOf<CallLog>()
    private var selectedContactName: String? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_history)
        
        analyticsService = CallAnalyticsService(this)
        
        // Check if we're viewing history for a specific contact
        selectedContactName = intent.getStringExtra("contact_name")
        
        initializeViews()
        loadCallHistory()
        showQuickAnalytics()
    }
    
    private fun initializeViews() {
        callHistoryRecyclerView = findViewById(R.id.call_history_recycler)
        analyticsButton = findViewById(R.id.analytics_button)
        totalCallsStat = findViewById(R.id.total_calls_stat)
        totalDurationStat = findViewById(R.id.total_duration_stat)
        avgRatioStat = findViewById(R.id.avg_ratio_stat)
        
        // Set up Call History RecyclerView
        callHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        callHistoryAdapter = CallHistoryAdapter(
            callLogs = callLogs,
            onCallLogClick = { callLog ->
                // Handle call log click - show detailed options
                Log.d("CallHistoryActivity", "Call log clicked: ${callLog.contactName}")
                showCallLogOptions(callLog)
            },
            onDeleteClick = { callLog ->
                // Handle delete call log
                confirmDeleteCallLog(callLog)
            }
        )
        callHistoryRecyclerView.adapter = callHistoryAdapter
        
        // Set up analytics button
        analyticsButton.setOnClickListener {
            val intent = Intent(this, AnalyticsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadCallHistory() {
        coroutineScope.launch {
            try {
                callLogs = if (selectedContactName != null) {
                    // Load all calls for the specific contact
                    CallLogManager.getCallLogsByContact(selectedContactName!!).toMutableList()
                } else {
                    // Load most recent call per contact (overview mode)
                    CallLogManager.getMostRecentCallPerContact().toMutableList()
                }
                
                callHistoryAdapter.updateCallLogs(callLogs)
                
                val title = if (selectedContactName != null) {
                    "${callLogs.size} calls with $selectedContactName"
                } else {
                    "${callLogs.size} contacts with call history"
                }
                
                supportActionBar?.title = title
                Log.d("CallHistoryActivity", "Loaded $title")
                
            } catch (e: Exception) {
                Log.e("CallHistoryActivity", "Error loading call history", e)
            }
        }
    }
    
    private fun showCallLogOptions(callLog: CallLog) {
        val options = if (selectedContactName != null) {
            arrayOf(
                "View Details",
                "Delete This Call",
                "Delete All Calls for ${callLog.contactName}",
                "View Analytics for ${callLog.contactName}"
            )
        } else {
            arrayOf(
                "View All Calls",
                "View Analytics",
                "Delete All Calls for ${callLog.contactName}"
            )
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle("${callLog.contactName} - ${formatDuration(callLog.totalDuration)}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (selectedContactName != null) {
                            showCallLogDetails(callLog)
                        } else {
                            // View all calls for this contact
                            val intent = Intent(this, CallHistoryActivity::class.java)
                            intent.putExtra("contact_name", callLog.contactName)
                            startActivity(intent)
                        }
                    }
                    1 -> {
                        if (selectedContactName != null) {
                            confirmDeleteCallLog(callLog)
                        } else {
                            // View analytics
                            val intent = Intent(this, AnalyticsActivity::class.java)
                            intent.putExtra("contact_name", callLog.contactName)
                            startActivity(intent)
                        }
                    }
                    2 -> {
                        confirmDeleteAllCallsForContact(callLog.contactName)
                    }
                    3 -> {
                        // View analytics (only available in contact-specific mode)
                        val intent = Intent(this, AnalyticsActivity::class.java)
                        intent.putExtra("contact_name", callLog.contactName)
                        startActivity(intent)
                    }
                }
            }
            .show()
    }
    
    private fun showCallLogDetails(callLog: CallLog) {
        val speakingSeconds = callLog.speakingTime / 1000
        val listeningSeconds = callLog.listeningTime / 1000
        val totalSeconds = callLog.totalDuration / 1000
        val talkRatio = if (totalSeconds > 0) (speakingSeconds.toFloat() / totalSeconds * 100).toInt() else 0
        
        val details = """
            Contact: ${callLog.contactName}
            Date: ${java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault()).format(callLog.timestamp)}
            
            Speaking Time: ${speakingSeconds}s
            Listening Time: ${listeningSeconds}s
            Total Duration: ${totalSeconds}s
            
            Talk/Listen Ratio: ${talkRatio}%/${100-talkRatio}%
            
            Phone: ${callLog.phoneNumber}
        """.trimIndent()
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Call Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .setNeutralButton("Delete This Call") { _, _ ->
                confirmDeleteCallLog(callLog)
            }
            .show()
    }
    
    private fun confirmDeleteCallLog(callLog: CallLog) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Call Record?")
            .setMessage("Delete this call record with ${callLog.contactName}?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCallLog(callLog)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun confirmDeleteAllCallsForContact(contactName: String) {
        coroutineScope.launch {
            val callCount = CallLogManager.getCallLogsCountByContact(contactName)
            
            android.app.AlertDialog.Builder(this@CallHistoryActivity)
                .setTitle("Delete All Calls?")
                .setMessage("Delete all $callCount call records with $contactName?\n\nThis will reset all analytics for this contact.\n\nThis action cannot be undone.")
                .setPositiveButton("Delete All") { _, _ ->
                    deleteAllCallsForContact(contactName)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    private fun deleteCallLog(callLog: CallLog) {
        coroutineScope.launch {
            try {
                CallLogManager.deleteCallLogById(callLog.id)
                loadCallHistory() // Reload the list
                
                android.widget.Toast.makeText(
                    this@CallHistoryActivity,
                    "Call record deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                Log.d("CallHistoryActivity", "Deleted call log: ${callLog.id}")
            } catch (e: Exception) {
                Log.e("CallHistoryActivity", "Error deleting call log", e)
                android.widget.Toast.makeText(
                    this@CallHistoryActivity,
                    "Error deleting call record",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun deleteAllCallsForContact(contactName: String) {
        coroutineScope.launch {
            try {
                CallLogManager.deleteCallLogsByContact(contactName)
                loadCallHistory() // Reload the list
                
                android.widget.Toast.makeText(
                    this@CallHistoryActivity,
                    "All call records for $contactName deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                Log.d("CallHistoryActivity", "Deleted all call logs for: $contactName")
                
                // If we were viewing this contact's history, go back to overview
                if (selectedContactName == contactName) {
                    finish()
                }
                
            } catch (e: Exception) {
                Log.e("CallHistoryActivity", "Error deleting call logs for contact", e)
                android.widget.Toast.makeText(
                    this@CallHistoryActivity,
                    "Error deleting call records",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) "${minutes}m ${remainingSeconds}s" else "${seconds}s"
    }
    
    private fun showQuickAnalytics() {
        coroutineScope.launch {
            try {
                Log.d("CallHistoryActivity", "Starting showQuickAnalytics...")
                // Get all-time analytics to show in the quick stats
                val allTimeAnalytics = analyticsService.getGlobalAnalytics(TimeRange.ALL_TIME)
                Log.d("CallHistoryActivity", "Analytics result: $allTimeAnalytics")
                
                allTimeAnalytics?.let { analytics ->
                    Log.d("CallHistoryActivity", "All Time: ${analytics.totalCalls} calls, ${analytics.getTalkListenRatioFormatted()}")
                    
                    // Update UI elements on the main thread
                    runOnUiThread {
                        totalCallsStat.text = analytics.totalCalls.toString()
                        
                        val totalMinutes = (analytics.totalDurationMs / 1000 / 60).toInt()
                        totalDurationStat.text = if (totalMinutes > 0) "${totalMinutes}min" else "${(analytics.totalDurationMs / 1000).toInt()}s"
                        
                        val talkPercentage = if (analytics.totalDurationMs > 0) {
                            ((analytics.totalSpeakingTimeMs.toDouble() / analytics.totalDurationMs.toDouble()) * 100).toInt()
                        } else {
                            0
                        }
                        avgRatioStat.text = "${talkPercentage}%"
                    }
                } ?: run {
                    // No analytics data available
                    Log.d("CallHistoryActivity", "No analytics data available")
                    runOnUiThread {
                        totalCallsStat.text = "0"
                        totalDurationStat.text = "0min"
                        avgRatioStat.text = "0%"
                    }
                }
                
            } catch (e: Exception) {
                Log.e("CallHistoryActivity", "Error loading quick analytics", e)
                runOnUiThread {
                    totalCallsStat.text = "Error"
                    totalDurationStat.text = "Error"
                    avgRatioStat.text = "Error"
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload call history when returning to this activity
        loadCallHistory()
        // Also refresh the quick analytics
        showQuickAnalytics()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
