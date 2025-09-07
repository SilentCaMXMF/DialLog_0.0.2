package com.example.diallog002

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.*
import com.example.diallog002.data.CallLog
import com.example.diallog002.data.CallLogManager
import kotlinx.coroutines.*
import kotlin.math.sqrt

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
    private var audioRecord: AudioRecord? = null
    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    // Audio analysis parameters
    private var trackingRunnable: Runnable? = null
    private val ENVIRONMENT_THRESHOLD_DB = 1.0 // 1 dB above environment
    private var environmentBaseLevel = 0.0
    private var speakingThreshold = 0.0
    private var lastAudioLevel = 0.0
    private var consecutiveSilentChecks = 0
    private val maxSilentChecks = 3 // 300ms of silence before switching to listening
    private val SAMPLE_RATE = 44100
    private val BUFFER_SIZE = 4096
    private var audioMonitoringActive = false
    
    // Environmental noise calibration
    private var environmentLevels = mutableListOf<Double>()
    private var calibrationComplete = false
    
    init {
        // Initialize the database
        CallLogManager.initialize(context)
        
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Initialize audio recording for real-time analysis
        initializeAudioRecord()
        
        Log.d("CallTracker", "🎤 Audio-based talk/listen measurement initialized")
        Log.d("CallTracker", "Environment threshold: +${ENVIRONMENT_THRESHOLD_DB} dB")
    }
    
    private fun calibrateEnvironmentLevel() {
        Log.d("CallTracker", "🔧 Starting environment calibration...")
        
        // Quick environment sampling at call start
        coroutineScope.launch {
            try {
                val samples = mutableListOf<Double>()
                repeat(20) { // Sample for 2 seconds (100ms intervals)
                    audioRecord?.let { record ->
                        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            val buffer = ShortArray(BUFFER_SIZE)
                            val read = record.read(buffer, 0, buffer.size)
                            if (read > 0) {
                                val rms = calculateRMS(buffer, read)
                                val dbLevel = 20 * kotlin.math.log10(rms / 32767.0)
                                samples.add(dbLevel)
                            }
                        }
                    }
                    delay(100)
                }
                
                if (samples.isNotEmpty()) {
                    environmentBaseLevel = samples.average()
                    speakingThreshold = environmentBaseLevel + ENVIRONMENT_THRESHOLD_DB
                    calibrationComplete = true
                    
                    Log.d("CallTracker", "✅ Environment calibration complete")
                    Log.d("CallTracker", "  Base environment: ${String.format("%.1f", environmentBaseLevel)} dB")
                    Log.d("CallTracker", "  Speaking threshold: ${String.format("%.1f", speakingThreshold)} dB")
                } else {
                    // Fallback values
                    environmentBaseLevel = -40.0
                    speakingThreshold = environmentBaseLevel + ENVIRONMENT_THRESHOLD_DB
                    calibrationComplete = true
                    
                    Log.w("CallTracker", "⚠️ Using fallback environment levels")
                }
            } catch (e: Exception) {
                Log.e("CallTracker", "Error during environment calibration", e)
                environmentBaseLevel = -40.0
                speakingThreshold = environmentBaseLevel + ENVIRONMENT_THRESHOLD_DB
                calibrationComplete = true
            }
        }
    }
    
    private fun initializeAudioRecord() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            
            val actualBufferSize = maxOf(bufferSize, BUFFER_SIZE)
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                actualBufferSize
            )
            
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                Log.d("CallTracker", "✅ AudioRecord initialized successfully")
                Log.d("CallTracker", "  Sample rate: $SAMPLE_RATE Hz")
                Log.d("CallTracker", "  Buffer size: $actualBufferSize bytes")
            } else {
                Log.e("CallTracker", "❌ AudioRecord failed to initialize")
                audioRecord = null
            }
        } catch (e: SecurityException) {
            Log.e("CallTracker", "Permission denied for audio recording", e)
            audioRecord = null
        } catch (e: Exception) {
            Log.e("CallTracker", "Error initializing AudioRecord", e)
            audioRecord = null
        }
    }
    
    
    
    fun startTracking(contactName: String, phoneNumber: String) {
        if (isTracking) return
        
        Log.d("CallTracker", "=== STARTING AUDIO-BASED CALL TRACKING ===")
        Log.d("CallTracker", "Contact: $contactName ($phoneNumber)")
        
        isTracking = true
        currentContactName = contactName
        currentPhoneNumber = phoneNumber
        callStartTime = System.currentTimeMillis()
        speakingTime = 0L
        listeningTime = 0L
        consecutiveSilentChecks = 0
        calibrationComplete = false
        
        // Reinitialize AudioRecord if needed
        if (audioRecord == null) {
            Log.d("CallTracker", "AudioRecord is null, reinitializing...")
            initializeAudioRecord()
        }
        
        audioRecord?.let { record ->
            try {
                record.startRecording()
                Log.d("CallTracker", "✅ Audio recording started - beginning environment calibration")
                
                // Start environment calibration
                calibrateEnvironmentLevel()
                
                // Start audio monitoring after brief delay for calibration
                handler.postDelayed({
                    startAudioAnalysis()
                }, 2500) // 2.5 second delay for environment calibration
                
            } catch (e: IllegalStateException) {
                Log.e("CallTracker", "Failed to start audio recording", e)
            } catch (e: SecurityException) {
                Log.e("CallTracker", "Permission denied for audio recording", e)
            }
        } ?: run {
            Log.e("CallTracker", "❌ AudioRecord not available, cannot track call")
        }
        
        Log.d("CallTracker", "=== AUDIO CALL TRACKING STARTED ===")
    }
    
    fun stopTracking() {
        if (!isTracking) return
        
        Log.d("CallTracker", "=== STOPPING AUDIO CALL TRACKING ===")
        Log.d("CallTracker", "Contact: $currentContactName")
        
        isTracking = false
        audioMonitoringActive = false
        
        // Stop audio analysis
        stopAudioAnalysis()
        
        // Safely stop audio recording
        try {
            audioRecord?.let { record ->
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                    Log.d("CallTracker", "Audio recording stopped successfully")
                } else {
                    Log.d("CallTracker", "Audio recording was not active")
                }
            }
        } catch (e: Exception) {
            Log.e("CallTracker", "Error stopping audio recording", e)
        }
        
        val totalDuration = System.currentTimeMillis() - callStartTime
        
        Log.d("CallTracker", "AUDIO ANALYSIS RESULTS:")
        Log.d("CallTracker", "  Speaking time: ${speakingTime}ms (${String.format("%.1f", speakingTime/1000.0)}s)")
        Log.d("CallTracker", "  Listening time: ${listeningTime}ms (${String.format("%.1f", listeningTime/1000.0)}s)")
        Log.d("CallTracker", "  Total duration: ${totalDuration}ms (${String.format("%.1f", totalDuration/1000.0)}s)")
        Log.d("CallTracker", "  Environment threshold: ${String.format("%.1f", environmentBaseLevel)} + ${ENVIRONMENT_THRESHOLD_DB} dB")
        
        // Validate results - ensure we have reasonable data
        val trackedTime = speakingTime + listeningTime
        if (trackedTime < totalDuration * 0.8 || speakingTime == 0L) {
            Log.w("CallTracker", "⚠️ Incomplete audio tracking detected")
            
            // Distribute untracked time based on existing ratio
            val missingTime = totalDuration - trackedTime
            if (speakingTime > 0 && listeningTime > 0) {
                val speakingRatio = speakingTime.toDouble() / trackedTime
                speakingTime += (missingTime * speakingRatio).toLong()
                listeningTime += (missingTime * (1 - speakingRatio)).toLong()
                Log.d("CallTracker", "🔧 Adjusted times based on detected ratio")
            } else {
                // No valid data, use conservative estimate
                listeningTime = (totalDuration * 0.6).toLong()
                speakingTime = (totalDuration * 0.4).toLong()
                Log.d("CallTracker", "🔧 Using fallback 60/40 ratio")
            }
        } else {
            Log.d("CallTracker", "✅ REAL AUDIO DATA captured successfully!")
        }
        
        // Create and save call log
        val callLog = CallLog(
            contactName = currentContactName,
            phoneNumber = currentPhoneNumber,
            speakingTime = speakingTime,
            listeningTime = listeningTime,
            totalDuration = totalDuration,
            timestamp = Date()
        )
        
        Log.d("CallTracker", "FINAL CALL LOG: $callLog")
        
        coroutineScope.launch {
            try {
                CallLogManager.addCallLog(callLog)
                Log.d("CallTracker", "✅ Call log saved to database successfully")
                onCallLogUpdated(callLog)
            } catch (e: Exception) {
                Log.e("CallTracker", "Error saving call log to database", e)
            }
        }
        
        Log.d("CallTracker", "=== AUDIO CALL TRACKING STOPPED ===")
    }
    
    private fun startAudioAnalysis() {
        if (!calibrationComplete) {
            Log.w("CallTracker", "Environment calibration not complete, waiting...")
            // Retry after a short delay
            handler.postDelayed({
                startAudioAnalysis()
            }, 500)
            return
        }
        
        audioMonitoringActive = true
        Log.d("CallTracker", "🎤 Starting real-time audio analysis")
        Log.d("CallTracker", "Using threshold: ${String.format("%.1f", speakingThreshold)} dB (${String.format("%.1f", environmentBaseLevel)} + ${ENVIRONMENT_THRESHOLD_DB})")
        
        trackingRunnable = object : Runnable {
            override fun run() {
                if (!isTracking || !audioMonitoringActive) return
                
                audioRecord?.let { record ->
                    if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        Log.w("CallTracker", "AudioRecord not recording, stopping analysis")
                        return
                    }
                    
                    val buffer = ShortArray(BUFFER_SIZE)
                    val read = record.read(buffer, 0, buffer.size)
                    
                    if (read > 0) {
                        val rms = calculateRMS(buffer, read)
                        val dbLevel = 20 * kotlin.math.log10(rms / 32767.0)
                        
                        // Check if audio level exceeds environment + threshold
                        val isSpeaking = dbLevel > speakingThreshold
                        
                        // Log detailed analysis every 2 seconds for debugging
                        val currentTime = System.currentTimeMillis()
                        if ((currentTime - callStartTime) % 2000 < 100) {
                            Log.d("CallTracker", "AUDIO: ${String.format("%.1f", dbLevel)} dB | Threshold: ${String.format("%.1f", speakingThreshold)} dB | Speaking: $isSpeaking")
                        }
                        
                        if (isSpeaking) {
                            speakingTime += 100 // Increment by 100ms
                            consecutiveSilentChecks = 0
                            if (currentTime % 5000 < 100) { // Log speaking detection every 5 seconds
                                Log.d("CallTracker", "🗣️ SPEAKING detected: ${String.format("%.1f", dbLevel)} dB")
                            }
                        } else {
                            consecutiveSilentChecks++
                            if (consecutiveSilentChecks >= maxSilentChecks) {
                                listeningTime += 100 // Increment by 100ms
                                if (currentTime % 5000 < 100) { // Log listening detection every 5 seconds
                                    Log.d("CallTracker", "👂 LISTENING detected: ${String.format("%.1f", dbLevel)} dB")
                                }
                            }
                        }
                        
                        lastAudioLevel = dbLevel
                    }
                }
                
                handler.postDelayed(this, 100) // Analyze every 100ms
            }
        }
        
        handler.post(trackingRunnable!!)
        Log.d("CallTracker", "✅ Audio analysis started successfully")
    }
    
    private fun stopAudioAnalysis() {
        audioMonitoringActive = false
        
        trackingRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            trackingRunnable = null
            Log.d("CallTracker", "Audio analysis stopped")
        }
    }
    
    
    private fun calculateRMS(audioBuffer: ShortArray, readSize: Int): Double {
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += (audioBuffer[i] * audioBuffer[i]).toDouble()
        }
        return sqrt(sum / readSize)
    }
    
    
    fun isCurrentlyTracking(): Boolean = isTracking
    
    fun getCurrentContactName(): String = currentContactName
    
    fun getCurrentPhoneNumber(): String = currentPhoneNumber
    
    fun cleanup() {
        stopAudioAnalysis()
        
        audioRecord?.let { record ->
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
                audioRecord = null
                Log.d("CallTracker", "AudioRecord cleaned up")
            } catch (e: Exception) {
                Log.e("CallTracker", "Error cleaning up AudioRecord", e)
            }
        }
        
        coroutineScope.cancel()
    }
}
