package com.example.diallog002.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.*

// Date converter for Room database
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Entity(tableName = "call_logs")
@TypeConverters(DateConverter::class)
data class CallLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val contactName: String,
    val phoneNumber: String,
    val speakingTime: Long, // in milliseconds
    val listeningTime: Long, // in milliseconds
    val totalDuration: Long, // in milliseconds
    val timestamp: Date,
    val callDate: String = getDateString(timestamp), // YYYY-MM-DD format for easy querying
    val callMonth: String = getMonthString(timestamp), // YYYY-MM format
    val callYear: String = getYearString(timestamp), // YYYY format
    val talkListenRatio: Double = calculateRatio(speakingTime, listeningTime)
) {
    companion object {
        private fun getDateString(date: Date): String {
            val calendar = Calendar.getInstance().apply { time = date }
            return String.format("%04d-%02d-%02d", 
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        
        private fun getMonthString(date: Date): String {
            val calendar = Calendar.getInstance().apply { time = date }
            return String.format("%04d-%02d", 
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1
            )
        }
        
        private fun getYearString(date: Date): String {
            val calendar = Calendar.getInstance().apply { time = date }
            return calendar.get(Calendar.YEAR).toString()
        }
        
        private fun calculateRatio(speakingTime: Long, listeningTime: Long): Double {
            val total = speakingTime + listeningTime
            return if (total > 0) (speakingTime.toDouble() / total.toDouble()) * 100.0 else 0.0
        }
    }
    
    // Utility functions
    fun getSpeakingTimeSeconds(): Double = speakingTime / 1000.0
    fun getListeningTimeSeconds(): Double = listeningTime / 1000.0
    fun getTotalDurationSeconds(): Double = totalDuration / 1000.0
    fun getTalkListenRatioFormatted(): String = String.format("%.1f%% talk / %.1f%% listen", 
        talkListenRatio, 100.0 - talkListenRatio)
}
