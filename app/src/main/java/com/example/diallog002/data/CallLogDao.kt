package com.example.diallog002.data

import androidx.room.*

// Data class for aggregated analytics
data class ContactAnalytics(
    val contactName: String,
    val totalCalls: Int,
    val totalSpeakingTime: Long,
    val totalListeningTime: Long,
    val totalDuration: Long,
    val averageTalkRatio: Double
)

data class TimeRangeAnalytics(
    val totalCalls: Int,
    val totalSpeakingTime: Long,
    val totalListeningTime: Long,
    val totalDuration: Long,
    val averageTalkRatio: Double
)

data class ContactCount(
    val contactName: String,
    val callCount: Int
)

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

    // Time-range queries
    @Query("SELECT * FROM call_logs WHERE callDate = :date ORDER BY timestamp DESC")
    suspend fun getCallLogsByDate(date: String): List<CallLog>

    @Query("SELECT * FROM call_logs WHERE callMonth = :month ORDER BY timestamp DESC")
    suspend fun getCallLogsByMonth(month: String): List<CallLog>

    @Query("SELECT * FROM call_logs WHERE callYear = :year ORDER BY timestamp DESC")
    suspend fun getCallLogsByYear(year: String): List<CallLog>

    // Contact-specific time-range queries
    @Query("SELECT * FROM call_logs WHERE contactName = :contactName AND callDate = :date ORDER BY timestamp DESC")
    suspend fun getCallLogsByContactAndDate(contactName: String, date: String): List<CallLog>

    @Query("SELECT * FROM call_logs WHERE contactName = :contactName AND callMonth = :month ORDER BY timestamp DESC")
    suspend fun getCallLogsByContactAndMonth(contactName: String, month: String): List<CallLog>

    @Query("SELECT * FROM call_logs WHERE contactName = :contactName AND callYear = :year ORDER BY timestamp DESC")
    suspend fun getCallLogsByContactAndYear(contactName: String, year: String): List<CallLog>

    // Aggregated analytics queries
    @Query("""
        SELECT 
            contactName,
            COUNT(*) as totalCalls,
            SUM(speakingTime) as totalSpeakingTime,
            SUM(listeningTime) as totalListeningTime,
            SUM(totalDuration) as totalDuration,
            AVG(talkListenRatio) as averageTalkRatio
        FROM call_logs 
        WHERE contactName = :contactName
        GROUP BY contactName
    """)
    suspend fun getContactAnalytics(contactName: String): ContactAnalytics?

    @Query("""
        SELECT 
            contactName,
            COUNT(*) as totalCalls,
            SUM(speakingTime) as totalSpeakingTime,
            SUM(listeningTime) as totalListeningTime,
            SUM(totalDuration) as totalDuration,
            AVG(talkListenRatio) as averageTalkRatio
        FROM call_logs 
        WHERE contactName = :contactName AND callDate = :date
        GROUP BY contactName
    """)
    suspend fun getContactAnalyticsByDate(contactName: String, date: String): ContactAnalytics?

    @Query("""
        SELECT 
            contactName,
            COUNT(*) as totalCalls,
            SUM(speakingTime) as totalSpeakingTime,
            SUM(listeningTime) as totalListeningTime,
            SUM(totalDuration) as totalDuration,
            AVG(talkListenRatio) as averageTalkRatio
        FROM call_logs 
        WHERE contactName = :contactName AND callMonth = :month
        GROUP BY contactName
    """)
    suspend fun getContactAnalyticsByMonth(contactName: String, month: String): ContactAnalytics?

    @Query("""
        SELECT 
            contactName,
            COUNT(*) as totalCalls,
            SUM(speakingTime) as totalSpeakingTime,
            SUM(listeningTime) as totalListeningTime,
            SUM(totalDuration) as totalDuration,
            AVG(talkListenRatio) as averageTalkRatio
        FROM call_logs 
        WHERE contactName = :contactName AND callYear = :year
        GROUP BY contactName
    """)
    suspend fun getContactAnalyticsByYear(contactName: String, year: String): ContactAnalytics?

    // Global time-range analytics
    @Query("""
        SELECT 
            COUNT(*) as totalCalls,
            SUM(speakingTime) as totalSpeakingTime,
            SUM(listeningTime) as totalListeningTime,
            SUM(totalDuration) as totalDuration,
            AVG(talkListenRatio) as averageTalkRatio
        FROM call_logs 
        WHERE callDate = :date
    """)
    suspend fun getDayAnalytics(date: String): TimeRangeAnalytics?

    @Query("""
        SELECT 
            COUNT(*) as totalCalls,
            SUM(speakingTime) as totalSpeakingTime,
            SUM(listeningTime) as totalListeningTime,
            SUM(totalDuration) as totalDuration,
            AVG(talkListenRatio) as averageTalkRatio
        FROM call_logs 
        WHERE callMonth = :month
    """)
    suspend fun getMonthAnalytics(month: String): TimeRangeAnalytics?

    @Query("""
        SELECT 
            COUNT(*) as totalCalls,
            SUM(speakingTime) as totalSpeakingTime,
            SUM(listeningTime) as totalListeningTime,
            SUM(totalDuration) as totalDuration,
            AVG(talkListenRatio) as averageTalkRatio
        FROM call_logs
        WHERE callYear = :year
    """)
    suspend fun getYearAnalytics(year: String): TimeRangeAnalytics?

    @Query("""
        SELECT 
            COUNT(*) as totalCalls,
            SUM(speakingTime) as totalSpeakingTime,
            SUM(listeningTime) as totalListeningTime,
            SUM(totalDuration) as totalDuration,
            AVG(talkListenRatio) as averageTalkRatio
        FROM call_logs
    """)
    suspend fun getAllTimeAnalytics(): TimeRangeAnalytics?

    // Get all unique contacts with call counts
    @Query("""
        SELECT contactName, COUNT(*) as callCount
        FROM call_logs 
        GROUP BY contactName 
        ORDER BY callCount DESC
    """)
    suspend fun getContactCallCounts(): List<ContactCount>

    @Delete
    suspend fun delete(callLog: CallLog)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()
}
