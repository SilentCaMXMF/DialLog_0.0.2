package com.example.diallog002

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
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
    private var callLogs = mutableListOf<CallLog>()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_history)
        
        analyticsService = CallAnalyticsService(this)
        
        initializeViews()
        loadCallHistory()
        showQuickAnalytics()
    }
    
    private fun initializeViews() {
        callHistoryRecyclerView = findViewById(R.id.call_history_recycler)
        analyticsButton = findViewById(R.id.analytics_button)
        
        // Set up Call History RecyclerView
        callHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        callHistoryAdapter = CallHistoryAdapter(
            callLogs = callLogs,
            onCallLogClick = { callLog ->
                // Handle call log click - could show detailed history
                Log.d("CallHistoryActivity", "Call log clicked: ${callLog.contactName}")
                showCallLogDetails(callLog)
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
                callLogs = CallLogManager.getMostRecentCallPerContact().toMutableList()
                callHistoryAdapter.updateCallLogs(callLogs)
                Log.d("CallHistoryActivity", "Loaded ${callLogs.size} call history items")
            } catch (e: Exception) {
                Log.e("CallHistoryActivity", "Error loading call history", e)
            }
        }
    }
    
    private fun showCallLogDetails(callLog: CallLog) {
        // For now, just log the details
        // In a real app, you could show a dialog or navigate to a detail activity
        Log.d("CallHistoryActivity", "Call Details: ${callLog.contactName} - Speaking: ${callLog.speakingTime}ms, Listening: ${callLog.listeningTime}ms, Total: ${callLog.totalDuration}ms")
    }
    
    private fun showQuickAnalytics() {
        coroutineScope.launch {
            try {
                // Get today's analytics as a quick preview
                val todayAnalytics = analyticsService.getGlobalAnalytics(TimeRange.DAY)
                val allTimeAnalytics = analyticsService.getGlobalAnalytics(TimeRange.ALL_TIME)
                
                todayAnalytics?.let {
                    Log.d("CallHistoryActivity", "Today: ${it.totalCalls} calls, ${it.getTalkListenRatioFormatted()}")
                }
                
                allTimeAnalytics?.let {
                    Log.d("CallHistoryActivity", "All Time: ${it.totalCalls} calls, ${it.getTalkListenRatioFormatted()}")
                }
                
            } catch (e: Exception) {
                Log.e("CallHistoryActivity", "Error loading quick analytics", e)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload call history when returning to this activity
        loadCallHistory()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
