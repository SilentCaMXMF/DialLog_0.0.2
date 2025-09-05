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
    
    private var trackAudioRunnable: Runnable? = null
    private var speechThreshold = 1000.0 // Will be updated from calibration
    private var backgroundNoise = 100.0 // Will be updated from calibration
    private var lastAudioLevel = 0.0
    private var consecutiveSilentChecks = 0
    private val maxSilentChecks = 5 // 500ms of silence before considering it listening
    
    init {
        // Initialize the database
        CallLogManager.initialize(context)
        
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Load calibrated parameters
        loadCalibrationSettings()
        
        initializeAudioRecord()
    }
    
    private fun loadCalibrationSettings() {
        val prefs = context.getSharedPreferences("mic_calibration", Context.MODE_PRIVATE)
        
        if (prefs.getBoolean("calibration_completed", false)) {
            speechThreshold = prefs.getFloat("speaking_threshold", 1000.0f).toDouble()
            backgroundNoise = prefs.getFloat("background_noise_level", 100.0f).toDouble()
            
            val isSkipped = prefs.getBoolean("calibration_skipped", false)
            val calibrationTime = prefs.getLong("calibration_timestamp", 0)
            val calibrationDate = prefs.getString("calibration_date", "Unknown")
            
            Log.d("CallTracker", "=== CALIBRATION SETTINGS LOADED ===")
            Log.d("CallTracker", "  Speech threshold: $speechThreshold")
            Log.d("CallTracker", "  Background noise: $backgroundNoise")
            Log.d("CallTracker", "  Was skipped: $isSkipped")
            Log.d("CallTracker", "  Calibration date: $calibrationDate")
            Log.d("CallTracker", "  Calibration time: $calibrationTime")
            Log.d("CallTracker", "=== END CALIBRATION SETTINGS ===")
        } else {
            Log.d("CallTracker", "⚠️ NO CALIBRATION DATA FOUND, using defaults")
            Log.d("CallTracker", "  Default speech threshold: $speechThreshold")
            Log.d("CallTracker", "  Default background noise: $backgroundNoise")
        }
    }
    
    private fun initializeAudioRecord() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                44100,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            
            if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                Log.e("CallTracker", "Invalid buffer size for AudioRecord")
                return
            }
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("CallTracker", "AudioRecord not initialized properly")
                audioRecord = null
            } else {
                Log.d("CallTracker", "AudioRecord initialized successfully")
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
        
        Log.d("CallTracker", "=== STARTING CALL TRACKING ===")
        Log.d("CallTracker", "Contact: $contactName ($phoneNumber)")
        Log.d("CallTracker", "Current calibration - Speech: $speechThreshold, Background: $backgroundNoise")
        
        isTracking = true
        currentContactName = contactName
        currentPhoneNumber = phoneNumber
        callStartTime = System.currentTimeMillis()
        speakingTime = 0L
        listeningTime = 0L
        consecutiveSilentChecks = 0
        
        // Reinitialize AudioRecord if needed
        if (audioRecord == null) {
            Log.d("CallTracker", "AudioRecord is null, reinitializing...")
            initializeAudioRecord()
        }
        
        audioRecord?.let { record ->
            Log.d("CallTracker", "AudioRecord state: ${record.state}, recording state: ${record.recordingState}")
            try {
                record.startRecording()
                startAudioMonitoring()
                Log.d("CallTracker", "✅ Audio recording started successfully - using REAL audio analysis")
            } catch (e: IllegalStateException) {
                Log.e("CallTracker", "Failed to start audio recording", e)
                // Continue with basic duration tracking even without audio analysis
                startBasicTimeTracking()
            } catch (e: SecurityException) {
                Log.e("CallTracker", "Permission denied for audio recording", e)
                startBasicTimeTracking()
            }
        } ?: run {
            Log.w("CallTracker", "⚠️ AudioRecord not available, using basic time tracking")
            startBasicTimeTracking()
        }
        
        Log.d("CallTracker", "=== CALL TRACKING STARTED ===")
    }
    
    fun stopTracking() {
        if (!isTracking) return
        
        Log.d("CallTracker", "=== STOPPING CALL TRACKING ===")
        Log.d("CallTracker", "Contact: $currentContactName")
        
        isTracking = false
        
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
        
        stopAudioMonitoring()
        
        val totalDuration = System.currentTimeMillis() - callStartTime
        
        Log.d("CallTracker", "RAW TRACKING RESULTS:")
        Log.d("CallTracker", "  Speaking time: ${speakingTime}ms (${speakingTime/1000}s)")
        Log.d("CallTracker", "  Listening time: ${listeningTime}ms (${listeningTime/1000}s)")
        Log.d("CallTracker", "  Total duration: ${totalDuration}ms (${totalDuration/1000}s)")
        
        // If we have no audio tracking data, estimate based on call duration
        if (speakingTime == 0L && listeningTime == 0L && totalDuration > 0) {
            // Rough estimation: assume 60% listening, 40% speaking for normal conversation
            listeningTime = (totalDuration * 0.6).toLong()
            speakingTime = (totalDuration * 0.4).toLong()
            Log.d("CallTracker", "⚠️ NO AUDIO DATA - Using estimated talk/listen ratio")
            Log.d("CallTracker", "  Estimated speaking: ${speakingTime}ms")
            Log.d("CallTracker", "  Estimated listening: ${listeningTime}ms")
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
        
        Log.d("CallTracker", "=== CALL TRACKING STOPPED ===")
    }
    
    private fun startAudioMonitoring() {
        trackAudioRunnable = object : Runnable {
            override fun run() {
                if (!isTracking) return
                
                audioRecord?.let { record ->
                    if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        Log.w("CallTracker", "AudioRecord not recording, switching to basic tracking")
                        startBasicTimeTracking()
                        return
                    }
                    
                    val buffer = ShortArray(1024)
                    val read = record.read(buffer, 0, buffer.size)
                    
                    if (read > 0) {
                        // Calculate RMS (Root Mean Square) for better audio level detection
                        val rms = calculateRMS(buffer, read)
                        
                        // Use a more sensitive threshold calculation
                        // The speechThreshold from calibration is already optimized, so use it more directly
                        val effectiveThreshold = backgroundNoise + (speechThreshold - backgroundNoise) * 0.3
                        val isSpeaking = rms > effectiveThreshold
                        
                        // Log every 1 second (every 10 iterations) for debugging
                        val currentTime = System.currentTimeMillis()
                        if (currentTime % 1000 < 200) { // Log roughly every second
                            Log.d("CallTracker", "AUDIO ANALYSIS: RMS=$rms, threshold=$effectiveThreshold, backgroundNoise=$backgroundNoise, speechThreshold=$speechThreshold, isSpeaking=$isSpeaking")
                        }
                        
                        if (isSpeaking) {
                            speakingTime += 100 // Increment speaking time by 100ms
                            consecutiveSilentChecks = 0
                            Log.v("CallTracker", "✨ SPEAKING: RMS=$rms > threshold=$effectiveThreshold")
                        } else {
                            consecutiveSilentChecks++
                            if (consecutiveSilentChecks >= maxSilentChecks) {
                                listeningTime += 100 // Increment listening time by 100ms
                                Log.v("CallTracker", "👂 LISTENING: RMS=$rms < threshold=$effectiveThreshold")
                            }
                        }
                        
                        lastAudioLevel = rms
                    }
                }
                
                handler.postDelayed(this, 100) // Check every 100ms
            }
        }
        
        handler.post(trackAudioRunnable!!)
    }
    
    private fun calculateRMS(audioBuffer: ShortArray, readSize: Int): Double {
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += (audioBuffer[i] * audioBuffer[i]).toDouble()
        }
        return sqrt(sum / readSize)
    }
    
    private fun startBasicTimeTracking() {
        // Fallback method that estimates talk/listen ratio without audio analysis
        trackAudioRunnable = object : Runnable {
            override fun run() {
                if (!isTracking) return
                
                // Simple time-based estimation
                // This is a basic fallback - in reality you might want to use other signals
                // like proximity sensor, screen state, etc.
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - callStartTime
                
                // Estimate based on typical conversation patterns
                // This is very basic and should be improved with actual audio analysis
                if (elapsed % 3000 < 1200) { // Assume speaking 40% of time
                    speakingTime += 100
                } else { // Assume listening 60% of time
                    listeningTime += 100
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
