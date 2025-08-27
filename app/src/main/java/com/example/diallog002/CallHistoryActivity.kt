package com.example.diallog002

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diallog002.data.CallLog
import com.example.diallog002.data.CallLogManager
import kotlinx.coroutines.*

class CallHistoryActivity : AppCompatActivity() {
    private lateinit var callHistoryRecyclerView: RecyclerView
    private lateinit var callHistoryAdapter: CallHistoryAdapter
    private var callLogs = mutableListOf<CallLog>()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_history)
        
        initializeViews()
        loadCallHistory()
    }
    
    private fun initializeViews() {
        callHistoryRecyclerView = findViewById(R.id.call_history_recycler)
        
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
