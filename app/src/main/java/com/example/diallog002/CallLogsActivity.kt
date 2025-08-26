package com.example.diallog002

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diallog002.data.AppDatabase
import com.example.diallog002.data.CallLog
import kotlinx.coroutines.launch

class CallLogsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CallLogsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_logs)

        recyclerView = findViewById(R.id.rvCallLogs)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CallLogsAdapter(mutableListOf()) { callLog, isFavorite ->
            updateFavoriteStatus(callLog, isFavorite)
        }
        recyclerView.adapter = adapter

        loadCallLogs()
    }

    private fun loadCallLogs() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            val callLogs = db.callLogDao().getAllCallLogs()
            adapter.updateCallLogs(callLogs)
        }
    }

    private fun updateFavoriteStatus(callLog: CallLog, isFavorite: Boolean) {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.callLogDao().updateFavoriteStatus(callLog.id, isFavorite)
            loadCallLogs()
        }
    }
}