package com.example.diallog002.data

import android.content.Context
import java.util.*
import java.text.SimpleDateFormat

enum class TimeRange {
    DAY, WEEK, MONTH, YEAR, ALL_TIME
}

data class AnalyticsResult(
    val contactName: String? = null,
    val timeRange: TimeRange,
    val period: String, // e.g., "2025-01-15", "2025-01", "2025"
    val totalCalls: Int,
    val totalSpeakingTimeMs: Long,
    val totalListeningTimeMs: Long,
    val totalDurationMs: Long,
    val averageTalkRatio: Double,
    val averageCallDuration: Double,
    val callLogs: List<CallLog> = emptyList()
) {
    // Utility functions for formatted display
    fun getTotalSpeakingTimeFormatted(): String = formatTime(totalSpeakingTimeMs)
    fun getTotalListeningTimeFormatted(): String = formatTime(totalListeningTimeMs)
    fun getTotalDurationFormatted(): String = formatTime(totalDurationMs)
    fun getAverageCallDurationFormatted(): String = formatTime(averageCallDuration.toLong())
    
    fun getTalkListenRatioFormatted(): String = 
        String.format("%.1f%% talk / %.1f%% listen", 
            averageTalkRatio, 100.0 - averageTalkRatio)
    
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }
}

class CallAnalyticsService(private val context: Context) {
    private val callLogDao = AppDatabase.getInstance(context).callLogDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    
    /**
     * Get analytics for a specific contact in a given time range
     */
    suspend fun getContactAnalytics(
        contactName: String,
        timeRange: TimeRange,
        specificDate: Date? = null
    ): AnalyticsResult? {
        val date = specificDate ?: Date()
        
        return when (timeRange) {
            TimeRange.DAY -> {
                val dateStr = dateFormat.format(date)
                val analytics = callLogDao.getContactAnalyticsByDate(contactName, dateStr)
                val callLogs = callLogDao.getCallLogsByContactAndDate(contactName, dateStr)
                analytics?.toAnalyticsResult(contactName, TimeRange.DAY, dateStr, callLogs)
            }
            
            TimeRange.WEEK -> {
                // For week, we'll get the past 7 days from the specified date
                val weekLogs = getWeekCallLogs(contactName, date)
                createWeekAnalytics(contactName, weekLogs, date)
            }
            
            TimeRange.MONTH -> {
                val monthStr = monthFormat.format(date)
                val analytics = callLogDao.getContactAnalyticsByMonth(contactName, monthStr)
                val callLogs = callLogDao.getCallLogsByContactAndMonth(contactName, monthStr)
                analytics?.toAnalyticsResult(contactName, TimeRange.MONTH, monthStr, callLogs)
            }
            
            TimeRange.YEAR -> {
                val yearStr = yearFormat.format(date)
                val analytics = callLogDao.getContactAnalyticsByYear(contactName, yearStr)
                val callLogs = callLogDao.getCallLogsByContactAndYear(contactName, yearStr)
                analytics?.toAnalyticsResult(contactName, TimeRange.YEAR, yearStr, callLogs)
            }
            
            TimeRange.ALL_TIME -> {
                val analytics = callLogDao.getContactAnalytics(contactName)
                val callLogs = callLogDao.getCallLogsByContact(contactName)
                analytics?.toAnalyticsResult(contactName, TimeRange.ALL_TIME, "all-time", callLogs)
            }
        }
    }
    
    /**
     * Get global analytics for a given time range (all contacts combined)
     */
    suspend fun getGlobalAnalytics(
        timeRange: TimeRange,
        specificDate: Date? = null
    ): AnalyticsResult? {
        val date = specificDate ?: Date()
        
        return when (timeRange) {
            TimeRange.DAY -> {
                val dateStr = dateFormat.format(date)
                val analytics = callLogDao.getDayAnalytics(dateStr)
                val callLogs = callLogDao.getCallLogsByDate(dateStr)
                analytics?.toGlobalAnalyticsResult(TimeRange.DAY, dateStr, callLogs)
            }
            
            TimeRange.WEEK -> {
                val weekLogs = getWeekCallLogsGlobal(date)
                createWeekAnalyticsGlobal(weekLogs, date)
            }
            
            TimeRange.MONTH -> {
                val monthStr = monthFormat.format(date)
                val analytics = callLogDao.getMonthAnalytics(monthStr)
                val callLogs = callLogDao.getCallLogsByMonth(monthStr)
                analytics?.toGlobalAnalyticsResult(TimeRange.MONTH, monthStr, callLogs)
            }
            
            TimeRange.YEAR -> {
                val yearStr = yearFormat.format(date)
                val analytics = callLogDao.getYearAnalytics(yearStr)
                val callLogs = callLogDao.getCallLogsByYear(yearStr)
                analytics?.toGlobalAnalyticsResult(TimeRange.YEAR, yearStr, callLogs)
            }
            
            TimeRange.ALL_TIME -> {
                val analytics = callLogDao.getAllTimeAnalytics()
                val callLogs = callLogDao.getAllCallLogs()
                analytics?.toGlobalAnalyticsResult(TimeRange.ALL_TIME, "all-time", callLogs)
            }
        }
    }
    
    /**
     * Get analytics for multiple contacts
     */
    suspend fun getMultiContactAnalytics(
        contactNames: List<String>,
        timeRange: TimeRange,
        specificDate: Date? = null
    ): List<AnalyticsResult> {
        return contactNames.mapNotNull { contactName ->
            getContactAnalytics(contactName, timeRange, specificDate)
        }
    }
    
    /**
     * Get analytics for all contacts that have calls in the specified time range
     */
    suspend fun getAllContactsAnalytics(
        timeRange: TimeRange,
        specificDate: Date? = null
    ): List<AnalyticsResult> {
        val contactCounts = callLogDao.getContactCallCounts()
        val contactNames = contactCounts.map { it.contactName }
        
        return getMultiContactAnalytics(contactNames, timeRange, specificDate)
    }
    
    /**
     * Get a summary of the most and least talkative contacts
     */
    suspend fun getContactRankings(): Map<String, List<String>> {
        val allContacts = getAllContactsAnalytics(TimeRange.ALL_TIME)
        
        val mostTalkative = allContacts
            .sortedByDescending { it.averageTalkRatio }
            .take(5)
            .map { "${it.contactName}: ${String.format("%.1f%%", it.averageTalkRatio)}" }
        
        val bestListeners = allContacts
            .sortedBy { it.averageTalkRatio }
            .take(5)
            .map { "${it.contactName}: ${String.format("%.1f%%", 100.0 - it.averageTalkRatio)} listening" }
        
        return mapOf(
            "mostTalkative" to mostTalkative,
            "bestListeners" to bestListeners
        )
    }
    
    // Helper functions
    private suspend fun getWeekCallLogs(contactName: String, endDate: Date): List<CallLog> {
        val calendar = Calendar.getInstance().apply { time = endDate }
        val logs = mutableListOf<CallLog>()
        
        repeat(7) {
            val dateStr = dateFormat.format(calendar.time)
            logs.addAll(callLogDao.getCallLogsByContactAndDate(contactName, dateStr))
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        return logs
    }
    
    private suspend fun getWeekCallLogsGlobal(endDate: Date): List<CallLog> {
        val calendar = Calendar.getInstance().apply { time = endDate }
        val logs = mutableListOf<CallLog>()
        
        repeat(7) {
            val dateStr = dateFormat.format(calendar.time)
            logs.addAll(callLogDao.getCallLogsByDate(dateStr))
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        return logs
    }
    
    private fun createWeekAnalytics(contactName: String, callLogs: List<CallLog>, endDate: Date): AnalyticsResult {
        val totalCalls = callLogs.size
        val totalSpeaking = callLogs.sumOf { it.speakingTime }
        val totalListening = callLogs.sumOf { it.listeningTime }
        val totalDuration = callLogs.sumOf { it.totalDuration }
        val avgTalkRatio = if (callLogs.isNotEmpty()) callLogs.map { it.talkListenRatio }.average() else 0.0
        val avgCallDuration = if (totalCalls > 0) totalDuration.toDouble() / totalCalls else 0.0
        
        val weekStart = Calendar.getInstance().apply { 
            time = endDate
            add(Calendar.DAY_OF_MONTH, -6)
        }
        val weekPeriod = "${dateFormat.format(weekStart.time)} to ${dateFormat.format(endDate)}"
        
        return AnalyticsResult(
            contactName = contactName,
            timeRange = TimeRange.WEEK,
            period = weekPeriod,
            totalCalls = totalCalls,
            totalSpeakingTimeMs = totalSpeaking,
            totalListeningTimeMs = totalListening,
            totalDurationMs = totalDuration,
            averageTalkRatio = avgTalkRatio,
            averageCallDuration = avgCallDuration,
            callLogs = callLogs
        )
    }
    
    private fun createWeekAnalyticsGlobal(callLogs: List<CallLog>, endDate: Date): AnalyticsResult {
        val totalCalls = callLogs.size
        val totalSpeaking = callLogs.sumOf { it.speakingTime }
        val totalListening = callLogs.sumOf { it.listeningTime }
        val totalDuration = callLogs.sumOf { it.totalDuration }
        val avgTalkRatio = if (callLogs.isNotEmpty()) callLogs.map { it.talkListenRatio }.average() else 0.0
        val avgCallDuration = if (totalCalls > 0) totalDuration.toDouble() / totalCalls else 0.0
        
        val weekStart = Calendar.getInstance().apply { 
            time = endDate
            add(Calendar.DAY_OF_MONTH, -6)
        }
        val weekPeriod = "${dateFormat.format(weekStart.time)} to ${dateFormat.format(endDate)}"
        
        return AnalyticsResult(
            contactName = null,
            timeRange = TimeRange.WEEK,
            period = weekPeriod,
            totalCalls = totalCalls,
            totalSpeakingTimeMs = totalSpeaking,
            totalListeningTimeMs = totalListening,
            totalDurationMs = totalDuration,
            averageTalkRatio = avgTalkRatio,
            averageCallDuration = avgCallDuration,
            callLogs = callLogs
        )
    }
}

// Extension functions for converting DAO results to AnalyticsResult
private fun ContactAnalytics.toAnalyticsResult(
    contactName: String,
    timeRange: TimeRange,
    period: String,
    callLogs: List<CallLog>
): AnalyticsResult {
    val avgCallDuration = if (totalCalls > 0) totalDuration.toDouble() / totalCalls else 0.0
    
    return AnalyticsResult(
        contactName = contactName,
        timeRange = timeRange,
        period = period,
        totalCalls = totalCalls,
        totalSpeakingTimeMs = totalSpeakingTime,
        totalListeningTimeMs = totalListeningTime,
        totalDurationMs = totalDuration,
        averageTalkRatio = averageTalkRatio,
        averageCallDuration = avgCallDuration,
        callLogs = callLogs
    )
}

private fun TimeRangeAnalytics.toGlobalAnalyticsResult(
    timeRange: TimeRange,
    period: String,
    callLogs: List<CallLog>
): AnalyticsResult {
    val avgCallDuration = if (totalCalls > 0) totalDuration.toDouble() / totalCalls else 0.0
    
    return AnalyticsResult(
        contactName = null,
        timeRange = timeRange,
        period = period,
        totalCalls = totalCalls,
        totalSpeakingTimeMs = totalSpeakingTime,
        totalListeningTimeMs = totalListeningTime,
        totalDurationMs = totalDuration,
        averageTalkRatio = averageTalkRatio,
        averageCallDuration = avgCallDuration,
        callLogs = callLogs
    )
}
