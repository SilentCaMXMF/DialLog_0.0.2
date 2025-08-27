package com.example.diallog002.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val contactName: String,
    val phoneNumber: String,
    val speakingTime: Long,
    val listeningTime: Long,
    val totalDuration: Long,
    val timestamp: Date
)