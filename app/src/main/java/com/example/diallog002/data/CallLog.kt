package com.example.diallog002.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,
    val speakingTime: Long, // in milliseconds
    val listeningTime: Long, // in milliseconds
    val timestamp: Date,
    val favorite: Boolean = false // Indicates if the contact is a favorite
)