package com.example.diallog002.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CallLogManager {
    private var database: AppDatabase? = null
    
    fun initialize(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
        }
    }
    
    suspend fun addCallLog(callLog: CallLog) = withContext(Dispatchers.IO) {
        database?.callLogDao()?.insert(callLog)
    }
    
    suspend fun getAllCallLogs(): List<CallLog> = withContext(Dispatchers.IO) {
        database?.callLogDao()?.getAllCallLogs() ?: emptyList()
    }
    
    suspend fun getCallLogsByContact(contactName: String): List<CallLog> = withContext(Dispatchers.IO) {
        database?.callLogDao()?.getCallLogsByContact(contactName) ?: emptyList()
    }
    
    suspend fun getMostRecentCallPerContact(): List<CallLog> = withContext(Dispatchers.IO) {
        database?.callLogDao()?.getMostRecentCallPerContact() ?: emptyList()
    }
    
    suspend fun deleteCallLog(callLog: CallLog) = withContext(Dispatchers.IO) {
        database?.callLogDao()?.delete(callLog)
    }
    
    suspend fun deleteAllCallLogs() = withContext(Dispatchers.IO) {
        database?.callLogDao()?.deleteAll()
    }
}