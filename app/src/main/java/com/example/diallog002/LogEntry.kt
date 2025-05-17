package com.example.diallog002

data class LogEntry(
    val phoneNumber: String,
    val callDuration: Long,
    val listeningTime: Long,
    val speakingTime: Long
)

fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
