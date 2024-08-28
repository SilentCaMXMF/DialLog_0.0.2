package com.example.diallog002.ui.theme

import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallLoggingService : Service() {

    private var callStartTime: Long = 0
    private var recorder: MediaRecorder? = null
    private var listeningTime: Long = 0
    private var speakingTime: Long = 0
    private var phoneNumber: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("state")) {
            "RINGING" -> {
                callStartTime = System.currentTimeMillis()
                phoneNumber = intent.getStringExtra("phoneNumber")
                startListening()
            }
            "OFFHOOK" -> startSpeaking()
            "IDLE" -> {
                val callDuration = System.currentTimeMillis() - callStartTime
                stopRecording()
                saveLogToFile(phoneNumber, callDuration, listeningTime, speakingTime)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startListening() {
        listeningTime = System.currentTimeMillis()
    }

    private fun startSpeaking() {
        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile("/dev/null")
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("CallLoggingService", "Error starting MediaRecorder", e)
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("CallLoggingService", "Error stopping MediaRecorder", e)
        } finally {
            recorder = null
            speakingTime = System.currentTimeMillis() - listeningTime
        }
    }

    private fun saveLogToFile(phoneNumber: String?, callDuration: Long, listeningTime: Long, speakingTime: Long) {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault())
        val currentDate = sdf.format(Date())
        val fileName = "CallLog_${currentDate}.txt"
        val contactDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), phoneNumber ?: "Unknown")

        try {
            contactDir.mkdirs() // Ensure the directory for this contact exists
            val logFile = File(contactDir, fileName)
            FileWriter(logFile).use { writer ->
                writer.write("PhoneNumber: $phoneNumber\n")
                writer.write("CallDuration: $callDuration\n")
                writer.write("ListeningTime: $listeningTime\n")
                writer.write("SpeakingTime: $speakingTime\n")
            }
            Log.d("CallLogger", "Log saved to file: ${logFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("CallLogger", "Error saving log to file", e)
        }
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
