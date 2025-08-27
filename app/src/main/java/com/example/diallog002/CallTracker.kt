package com.example.diallog002

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.*
import com.example.diallog002.data.CallLog
import com.example.diallog002.data.CallLogManager
import kotlinx.coroutines.*

class CallTracker(
    private val context: Context,
    private val onCallLogUpdated: (CallLog) -> Unit
) {
    private var isTracking = false
    private var callStartTime: Long = 0
    private var speakingTime = 0L
    private var listeningTime = 0L
    private var currentContactName = ""
    private var currentPhoneNumber = ""
    
    private lateinit var audioManager: AudioManager
    private lateinit var audioRecord: AudioRecord
    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    private var trackAudioRunnable: Runnable? = null
    
    init {
        // Initialize the database
        CallLogManager.initialize(context)
        
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT
        )
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
    }
    
    fun startTracking(contactName: String, phoneNumber: String) {
        if (isTracking) return
        
        Log.d("CallTracker", "Starting call tracking for: $contactName ($phoneNumber)")
        
        isTracking = true
        currentContactName = contactName
        currentPhoneNumber = phoneNumber
        callStartTime = System.currentTimeMillis()
        speakingTime = 0L
        listeningTime = 0L
        
        audioRecord.startRecording()
        startAudioMonitoring()
    }
    
    fun stopTracking() {
        if (!isTracking) return
        
        Log.d("CallTracker", "Stopping call tracking for: $currentContactName")
        
        isTracking = false
        audioRecord.stop()
        stopAudioMonitoring()
        
        // Create and save call log
        val callLog = CallLog(
            contactName = currentContactName,
            phoneNumber = currentPhoneNumber,
            speakingTime = speakingTime,
            listeningTime = listeningTime,
            totalDuration = System.currentTimeMillis() - callStartTime,
            timestamp = Date()
        )
        
        Log.d("CallTracker", "Call log created: $callLog")
        
        coroutineScope.launch {
            try {
                CallLogManager.addCallLog(callLog)
                Log.d("CallTracker", "Call log saved to database successfully")
                onCallLogUpdated(callLog)
            } catch (e: Exception) {
                Log.e("CallTracker", "Error saving call log to database", e)
            }
        }
    }
    
    private fun startAudioMonitoring() {
        trackAudioRunnable = object : Runnable {
            override fun run() {
                if (!isTracking) return
                
                val buffer = ShortArray(1024)
                val read = audioRecord.read(buffer, 0, buffer.size)
                
                if (read > 0) {
                    // Check if the microphone is active (user is speaking)
                    val isSpeaking = buffer.any { it > 2000 } // Threshold for voice detection
                    if (isSpeaking) {
                        speakingTime += 100 // Increment speaking time by 100ms
                    }
                    
                    // Check if the speaker is active (user is listening)
                    val isListening = audioManager.isSpeakerphoneOn || audioManager.isMusicActive
                    if (isListening) {
                        listeningTime += 100 // Increment listening time by 100ms
                    }
                }
                
                handler.postDelayed(this, 100) // Check every 100ms
            }
        }
        
        handler.post(trackAudioRunnable!!)
    }
    
    private fun stopAudioMonitoring() {
        trackAudioRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            trackAudioRunnable = null
        }
    }
    
    fun isCurrentlyTracking(): Boolean = isTracking
    
    fun getCurrentContactName(): String = currentContactName
    
    fun getCurrentPhoneNumber(): String = currentPhoneNumber
    
    fun cleanup() {
        coroutineScope.cancel()
    }
}
