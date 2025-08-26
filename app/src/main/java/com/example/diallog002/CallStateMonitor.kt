package com.example.diallog002

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback // For API 31+
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*

// Inside CallStateMonitor.kt

// ... (other imports)
import android.os.PowerManager // If you use the isScreenOff check

class CallStateMonitor(
    private val context: Context,
    private val onCallStateChanged: (isActive: Boolean, isNearEar: Boolean) -> Unit // Modified callback
) {
    // ... (telephonyManager, phoneStateListener, telephonyCallback)

    private var isCallActive = false
    private var isNearEar = false // Track proximity state

    private val proximityManager = ProximitySensorManager(context) { currentlyNear ->
        val oldNearState = isNearEar
        isNearEar = currentlyNear
        Log.d("CallStateMonitor", "Proximity changed. Is Near Ear: $isNearEar")

        if (isCallActive) { // Only update mic monitoring if call is active
            if (isNearEar && !oldNearState) { // Moved to ear
                Log.i("CallStateMonitor", "Phone moved to ear during active call.")
                // Potentially (re)start microphone monitoring if it was paused
                if (shouldMonitorMic()) { // Add a new combined condition
                    startMicrophoneMonitoring()
                }
            } else if (!isNearEar && oldNearState) { // Moved away from ear
                Log.i("CallStateMonitor", "Phone moved away from ear during active call.")
                // Pause or stop microphone monitoring
                stopMicrophoneMonitoring("Paused due to proximity change (away from ear)")
            }
            // Notify listener about the combined state
            onCallStateChanged(isCallActive, isNearEar)
        }
    }

    private fun handleCallState(state: Int) {
        val previousCallStateActive = isCallActive
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d("CallStateMonitor", "Call is Active (Off-hook)")
                isCallActive = true
                proximityManager.startListening() // Start listening to proximity when call is active
                // Initial check after call becomes active
                // The proximity sensor might take a moment to give its first reading.
                // We will rely on its callback to set isNearEar.
                // Microphone monitoring will start/stop based on shouldMonitorMic()
                if (shouldMonitorMic()){
                    startMicrophoneMonitoring()
                } else {
                    // Ensure it's stopped if conditions aren't met (e.g., not near ear at call start)
                    stopMicrophoneMonitoring("Call active, but not near ear initially or mic disabled")
                }
            }
            TelephonyManager.CALL_STATE_IDLE, TelephonyManager.CALL_STATE_RINGING -> {
                Log.d("CallStateMonitor", "Call is Idle or Ringing")
                isCallActive = false
                proximityManager.stopListening() // Stop proximity when call ends
                isNearEar = false // Reset proximity state
                stopMicrophoneMonitoring("Call ended")
            }
        }
        if (isCallActive != previousCallStateActive) { // If active state changed
            onCallStateChanged(isCallActive, isNearEar) // Notify about call active state (proximity might update shortly)
        }
    }

    // Helper to decide if mic monitoring should run
    private fun shouldMonitorMic(): Boolean {
        // Add any other conditions here if needed (e.g., a global enable/disable setting for the feature)
        return isCallActive && isNearEar
    }


    fun startListeningToCallState() { // Renamed for clarity
        // ... (existing telephonyManager.listen/registerTelephonyCallback)
    }

    fun stopListeningToCallState() { // Renamed for clarity
        // ... (existing telephonyManager.listen/unregisterTelephonyCallback)
        proximityManager.stopListening() // Ensure proximity also stops
        stopMicrophoneMonitoring("Listener stopped")
    }

    // --- Microphone Monitoring Part ---
    // ... (audioRecord, isMonitoringMic, sampleRate, etc. remain largely the same)

    private fun startMicrophoneMonitoring() {
        if (isMonitoringMic) {
            Log.d("MicMonitor", "Mic monitoring already active.")
            return
        }
        if (!shouldMonitorMic()) { // Double check conditions
            Log.d("MicMonitor", "Conditions not met for mic monitoring (Call Active: $isCallActive, Near Ear: $isNearEar).")
            stopMicrophoneMonitoring("Conditions not met at start attempt") // Ensure it's stopped if it shouldn't run
            return
        }
        // ... (rest of the existing startMicrophoneMonitoring logic: permission checks, AudioRecord setup)
        Log.i("MicMonitor", "Starting microphone monitoring because call is active AND phone is near ear.")
        // (The actual AudioRecord start and coroutine launch is inside the existing method)
    }

    // Modify stopMicrophoneMonitoring to accept a reason for logging
    private fun stopMicrophoneMonitoring(reason: String) {
        if (!isMonitoringMic && job == null) { // If already fully stopped
            // Log.d("MicMonitor", "Mic monitoring already stopped ($reason).")
            return
        }
        Log.i("MicMonitor", "Stopping microphone monitoring. Reason: $reason")
        // ... (rest of the existing stopMicrophoneMonitoring logic: job.cancel, audioRecord.stop/release)
    }

    // ... (The rest of CallStateMonitor: handleCallState, microphone monitoring coroutine, etc.)
}