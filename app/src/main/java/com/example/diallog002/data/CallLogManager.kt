package com.example.diallog002.data

object CallLogManager {
    private val callLogs = mutableListOf<CallLog>()

    fun addCallLog(callLog: CallLog) {
        callLogs.add(callLog)
    }

    fun getCallLogs(): List<CallLog> {
        return callLogs
    }
}