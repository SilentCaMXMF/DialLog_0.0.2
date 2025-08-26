package com.example.diallog002.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CallLogDao {
    @Insert
    suspend fun insertCallLog(callLog: CallLog)

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllCallLogs(): List<CallLog>

    @Query("SELECT * FROM call_logs WHERE favorite = 1 ORDER BY contactName ASC")
    suspend fun getFavoriteContacts(): List<CallLog>

    @Query("UPDATE call_logs SET favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)
}