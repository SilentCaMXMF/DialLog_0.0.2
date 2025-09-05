package com.example.diallog002

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlin.math.sqrt

class MicCalibrationActivity : AppCompatActivity() {
    
    private lateinit var titleText: TextView
    private lateinit var instructionText: TextView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var volumeIndicator: ProgressBar
    private lateinit var startButton: Button
    private lateinit var skipButton: Button
    private lateinit var completeButton: Button
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var calibrationStep = CalibrationStep.WELCOME
    
    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    // Calibration data
    private var silenceLevel = 0.0
    private var speakingLevels = mutableListOf<Double>()
    private var backgroundNoise = 0.0
    private val calibrationDuration = 8000L // 8 seconds per test
    private var isRecalibration = false
    
    enum class CalibrationStep {
        WELCOME, SILENCE_TEST, SPEAKING_TEST, ANALYSIS, COMPLETE
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mic_calibration)
        
        // Check if this is a recalibration
        isRecalibration = intent.getBooleanExtra("is_recalibration", false)
        
        initializeViews()
        setupCalibration()
    }
    
    private fun initializeViews() {
        titleText = findViewById(R.id.calibration_title)
        instructionText = findViewById(R.id.instruction_text)
        statusText = findViewById(R.id.status_text)
        progressBar = findViewById(R.id.calibration_progress)
        volumeIndicator = findViewById(R.id.volume_indicator)
        startButton = findViewById(R.id.start_button)
        skipButton = findViewById(R.id.skip_button)
        completeButton = findViewById(R.id.complete_button)
        
        startButton.setOnClickListener { startCalibrationStep() }
        skipButton.setOnClickListener { skipCalibration() }
        completeButton.setOnClickListener { completeCalibration() }
        
        completeButton.visibility = Button.GONE
    }
    
    private fun setupCalibration() {
        updateUIForStep(CalibrationStep.WELCOME)
    }
    
    private fun updateUIForStep(step: CalibrationStep) {
        calibrationStep = step
        
        when (step) {
            CalibrationStep.WELCOME -> {
                if (isRecalibration) {
                    titleText.text = "🔄 Recalibrate Microphone"
                    instructionText.text = """
                        Recalibrating your microphone settings
                        
                        This will update your calibration for:
                        • Changed environment (quieter/louder space)
                        • Different speaking volume preferences
                        • New microphone or device settings
                        • Improved call tracking accuracy
                        
                        The process takes about 30 seconds and will replace your previous calibration.
                        
                        No audio is stored or transmitted during this process.
                    """.trimIndent()
                    statusText.text = "Ready to update calibration"
                    startButton.text = "Start Recalibration"
                } else {
                    titleText.text = "🎤 Microphone Calibration"
                    instructionText.text = """
                        Welcome to DialLog! 
                        
                        To provide accurate call tracking, we need to calibrate your microphone for your voice and device.
                        
                        This will take about 30 seconds and will help us:
                        • Detect when you're speaking vs listening
                        • Optimize for your voice characteristics  
                        • Account for your device's microphone sensitivity
                        
                        The calibration is completely private - no audio is stored or transmitted.
                    """.trimIndent()
                    statusText.text = "Ready to start calibration"
                    startButton.text = "Start Calibration"
                }
                startButton.visibility = Button.VISIBLE
                skipButton.visibility = if (isRecalibration) Button.GONE else Button.VISIBLE
                progressBar.progress = 0
                volumeIndicator.progress = 0
            }
            
            CalibrationStep.SILENCE_TEST -> {
                titleText.text = "🔇 Background Noise Test"
                instructionText.text = """
                    Step 1: Background Noise Detection
                    
                    Please stay quiet for 8 seconds while we measure your environment's background noise level.
                    
                    This helps us distinguish between silence and actual speech.
                """.trimIndent()
                statusText.text = "Stay silent... measuring background noise"
                startButton.visibility = Button.GONE
                skipButton.visibility = Button.GONE
                progressBar.progress = 25
            }
            
            CalibrationStep.SPEAKING_TEST -> {
                titleText.text = "🗣️ Voice Calibration"
                instructionText.text = """
                    Step 2: Voice Level Detection
                    
                    Please speak normally for 8 seconds. You can:
                    • Count from 1 to 20
                    • Recite the alphabet
                    • Talk about your day
                    
                    Speak at your normal conversation volume.
                """.trimIndent()
                statusText.text = "Speak now... analyzing your voice"
                progressBar.progress = 50
            }
            
            CalibrationStep.ANALYSIS -> {
                titleText.text = "📊 Analyzing Results"
                instructionText.text = """
                    Processing your calibration data...
                    
                    We're calculating optimal detection thresholds for your:
                    • Voice volume and characteristics
                    • Device microphone sensitivity
                    • Environmental noise levels
                """.trimIndent()
                statusText.text = "Processing calibration data..."
                progressBar.progress = 75
            }
            
            CalibrationStep.COMPLETE -> {
                val silenceThreshold = backgroundNoise + (backgroundNoise * 0.5)
                val speakingThreshold = speakingLevels.average()
                
                if (isRecalibration) {
                    titleText.text = "✅ Recalibration Complete!"
                    instructionText.text = """
                        Your microphone has been successfully recalibrated!
                        
                        Updated settings:
                        • Background noise: ${String.format("%.0f", backgroundNoise)}
                        • Speaking threshold: ${String.format("%.0f", speakingThreshold)}
                        • Sensitivity: ${if (speakingThreshold > 2000) "High" else "Standard"}
                        
                        Call tracking accuracy has been improved for your current environment.
                    """.trimIndent()
                    statusText.text = "Calibration updated successfully!"
                    completeButton.text = "Done"
                } else {
                    titleText.text = "✅ Calibration Complete!"
                    instructionText.text = """
                        Calibration successful! 
                        
                        Your personalized settings:
                        • Background noise: ${String.format("%.0f", backgroundNoise)}
                        • Speaking threshold: ${String.format("%.0f", speakingThreshold)}
                        • Sensitivity: ${if (speakingThreshold > 2000) "High" else "Standard"}
                        
                        DialLog is now optimized for your voice and device!
                    """.trimIndent()
                    statusText.text = "Ready to start tracking calls!"
                    completeButton.text = "Start Using DialLog"
                }
                
                progressBar.progress = 100
                completeButton.visibility = Button.VISIBLE
                skipButton.visibility = Button.GONE
            }
            
        }
    }
    
    private fun startCalibrationStep() {
        when (calibrationStep) {
            CalibrationStep.WELCOME -> startSilenceTest()
            else -> { /* Already in progress */ }
        }
    }
    
    private fun startSilenceTest() {
        updateUIForStep(CalibrationStep.SILENCE_TEST)
        startAudioRecording { audioLevel ->
            // Update volume indicator during silence test
            volumeIndicator.progress = (audioLevel / 50).toInt().coerceIn(0, 100)
            backgroundNoise = audioLevel.coerceAtLeast(backgroundNoise)
        }
        
        // After 8 seconds, move to speaking test
        handler.postDelayed({
            stopAudioRecording()
            startSpeakingTest()
        }, calibrationDuration)
    }
    
    private fun startSpeakingTest() {
        updateUIForStep(CalibrationStep.SPEAKING_TEST)
        speakingLevels.clear()
        
        startAudioRecording { audioLevel ->
            // Update volume indicator and collect speaking data
            volumeIndicator.progress = (audioLevel / 50).toInt().coerceIn(0, 100)
            if (audioLevel > backgroundNoise * 1.5) { // Only count levels significantly above background
                speakingLevels.add(audioLevel)
            }
        }
        
        // After 8 seconds, analyze results
        handler.postDelayed({
            stopAudioRecording()
            analyzeResults()
        }, calibrationDuration)
    }
    
    private fun analyzeResults() {
        updateUIForStep(CalibrationStep.ANALYSIS)
        
        // Simulate analysis time
        handler.postDelayed({
            saveCalibrationResults()
            updateUIForStep(CalibrationStep.COMPLETE)
        }, 2000)
    }
    
    private fun startAudioRecording(onAudioLevel: (Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != 
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            statusText.text = "⚠️ Microphone permission required"
            return
        }
        
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            audioRecord?.startRecording()
            isRecording = true
            
            // Start monitoring audio levels
            coroutineScope.launch {
                val buffer = ShortArray(1024)
                while (isRecording && audioRecord != null) {
                    val read = audioRecord!!.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val rms = calculateRMS(buffer, read)
                        withContext(Dispatchers.Main) {
                            onAudioLevel(rms)
                        }
                    }
                    delay(100) // Update every 100ms
                }
            }
            
        } catch (e: Exception) {
            Log.e("MicCalibration", "Error starting audio recording", e)
            statusText.text = "❌ Error accessing microphone"
        }
    }
    
    private fun stopAudioRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        volumeIndicator.progress = 0
    }
    
    private fun calculateRMS(audioBuffer: ShortArray, readSize: Int): Double {
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += (audioBuffer[i] * audioBuffer[i]).toDouble()
        }
        return sqrt(sum / readSize)
    }
    
    private fun saveCalibrationResults() {
        val prefs = getSharedPreferences("mic_calibration", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        val averageSpeaking = if (speakingLevels.isNotEmpty()) speakingLevels.average() else 1500.0
        val optimizedThreshold = (backgroundNoise + (averageSpeaking - backgroundNoise) * 0.3).coerceAtLeast(500.0)
        
        val currentTime = System.currentTimeMillis()
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
        val calibrationDate = dateFormat.format(java.util.Date(currentTime))
        
        editor.putBoolean("calibration_completed", true)
        editor.putBoolean("calibration_skipped", false) // Clear skip flag if recalibrating
        editor.putFloat("background_noise_level", backgroundNoise.toFloat())
        editor.putFloat("speaking_threshold", optimizedThreshold.toFloat())
        editor.putFloat("average_speaking_level", averageSpeaking.toFloat())
        editor.putLong("calibration_timestamp", currentTime)
        editor.putString("calibration_date", calibrationDate)
        
        editor.apply()
        
        Log.d("MicCalibration", "Calibration saved: background=$backgroundNoise, threshold=$optimizedThreshold, speaking=$averageSpeaking, date=$calibrationDate")
    }
    
    private fun skipCalibration() {
        // Save default values
        val prefs = getSharedPreferences("mic_calibration", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        val currentTime = System.currentTimeMillis()
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
        val calibrationDate = dateFormat.format(java.util.Date(currentTime))
        
        editor.putBoolean("calibration_completed", true)
        editor.putBoolean("calibration_skipped", true)
        editor.putFloat("background_noise_level", 100.0f)
        editor.putFloat("speaking_threshold", 1000.0f) // Default threshold
        editor.putFloat("average_speaking_level", 2000.0f)
        editor.putLong("calibration_timestamp", currentTime)
        editor.putString("calibration_date", "$calibrationDate (skipped)")
        
        editor.apply()
        
        completeCalibration()
    }
    
    private fun completeCalibration() {
        if (isRecalibration) {
            // If this is a recalibration, just finish and return to previous activity
            finish()
        } else {
            // If this is initial calibration, navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAudioRecording()
        coroutineScope.cancel()
    }
}
