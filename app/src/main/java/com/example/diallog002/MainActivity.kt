package com.example.diallog002

import android.Manifest
import android.content.Intent
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.diallog002.data.CallLog
import com.example.diallog002.data.CallLogManager

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var startCallButton: FloatingActionButton

    private var isTracking = false
    private var speakingTime = 0L
    private var listeningTime = 0L
    private lateinit var audioManager: AudioManager
    private lateinit var audioRecord: AudioRecord
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        startCallButton = findViewById(R.id.fabStartCall)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ContactsAdapter(mutableListOf()) // Empty list for now
        recyclerView.adapter = adapter

        // Request necessary permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE
            ),
            1
        )

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Initialize AudioRecord
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

        // Set up FAB click listener
        startCallButton.setOnClickListener {
            // TODO: Implement call tracking functionality
        }

        // Button to select a contact and start tracking
        findViewById<Button>(R.id.select_contact_button).setOnClickListener {
            if (!isTracking) {
                startTracking()
            } else {
                stopTracking()
            }
        }

        // Log call logs
        val logs = CallLogManager.getCallLogs()
        logs.forEach {
            Log.d("CallLog", "Contact: ${it.contactName}, Speaking: ${it.speakingTime}, Listening: ${it.listeningTime}")
        }
    }

    private fun startTracking() {
        isTracking = true
        speakingTime = 0L
        listeningTime = 0L

        // Start monitoring microphone and speaker
        audioRecord.startRecording()
        handler.post(trackAudio)
    }

    private fun stopTracking() {
        isTracking = false
        audioRecord.stop()
        handler.removeCallbacks(trackAudio)

        // Log the speaking and listening time
        val contactName = "Selected Contact" // Replace with the actual contact name
        val callLog = CallLog(
            contactName = contactName,
            speakingTime = speakingTime,
            listeningTime = listeningTime,
            timestamp = java.util.Date()
        )
        CallLogManager.addCallLog(callLog)

        Log.d("MainActivity", "Call Log: $callLog")
    }

    private val trackAudio = object : Runnable {
        override fun run() {
            if (!isTracking) return

            val buffer = ShortArray(1024)
            val read = audioRecord.read(buffer, 0, buffer.size)

            // Check if the microphone is active (user is speaking)
            val isSpeaking = buffer.any { it > 2000 } // Threshold for voice detection
            if (isSpeaking) {
                speakingTime += 100 // Increment speaking time
            }

            // Check if the speaker is active (user is listening)
            val isListening = audioManager.isSpeakerphoneOn || audioManager.isMusicActive
            if (isListening) {
                listeningTime += 100 // Increment listening time
            }

            handler.postDelayed(this, 100) // Check every 100ms
        }
    }
}
