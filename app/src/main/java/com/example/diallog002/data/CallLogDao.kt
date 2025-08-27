package com.example.diallog002.data

import androidx.room.*

@Dao
interface CallLogDao {
    @Insert
    suspend fun insert(callLog: CallLog)

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllCallLogs(): List<CallLog>

    @Query("SELECT * FROM call_logs WHERE contactName = :contactName ORDER BY timestamp DESC")
    suspend fun getCallLogsByContact(contactName: String): List<CallLog>

    @Query("""
        SELECT * FROM call_logs c1 
        WHERE timestamp = (
            SELECT MAX(timestamp) 
            FROM call_logs c2 
            WHERE c2.contactName = c1.contactName
        )
        ORDER BY timestamp DESC
    """)
    suspend fun getMostRecentCallPerContact(): List<CallLog>

    @Delete
    suspend fun delete(callLog: CallLog)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()
}