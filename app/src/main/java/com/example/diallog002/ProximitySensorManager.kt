package com.example.diallog002

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log

class ProximitySensorManager(
    private val context: Context,
    private val onProximityChanged: (isNear: Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var isSensorRegistered = false

    // Some proximity sensors report distance, others binary (near/far).
    // Typically, a value < proximitySensor.maximumRange (often < 5cm) means "near".
    // Many sensors report 0.0 for near and a larger value (e.g., 5.0 or maxRange) for far.
    private var nearValue: Float = 0.0f // Default to 0 for near, common for many sensors

    init {
        if (proximitySensor == null) {
            Log.w("ProximitySensor", "No proximity sensor found on this device.")
        } else {
            // Some sensors might report a small non-zero value for "near"
            // If maximumRange is small (e.g., 5cm), anything less than it is near.
            // If it's a binary sensor, it might report 0.0 for near.
            // It's often safer to check if the value is less than a certain small threshold
            // or less than its maximum range if that max range is small (like 1-10cm).
            // For simplicity here, we'll assume a value close to 0 means near.
            // A more robust approach might be to check if value < proximitySensor.maximumRange
            // and proximitySensor.maximumRange is small (e.g., < 10)
            if (proximitySensor.maximumRange < 10.0f) { // Heuristic for typical "ear" proximity sensors
                nearValue =
                    proximitySensor.maximumRange / 2 // Or some other fraction, or just check < maxRange
            }
            Log.i(
                "ProximitySensor",
                "Proximity sensor found: ${proximitySensor.name}, Max Range: ${proximitySensor.maximumRange}, Resolution: ${proximitySensor.resolution}"
            )
        }
    }

    fun startListening() {
        if (proximitySensor != null && !isSensorRegistered) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
            isSensorRegistered = true
            Log.d("ProximitySensor", "Proximity sensor listener registered.")
        }
    }

    fun stopListening() {
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
            Log.d("ProximitySensor", "Proximity sensor listener unregistered.")
            // Reset to a "not near" state if needed when not listening
            // onProximityChanged(false) // Or based on last known state / default
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            // The threshold for "near" can vary.
            // Many sensors report 0.0 for near and a larger value (e.g., 5cm or sensor's max range) for far.
            // A common check is if distance < proximitySensor.maximumRange and if that max range is small (e.g. 5-10cm)
            // Or simply if distance is very small (e.g. < 1 or < 5 depending on the sensor units)
            val isCurrentlyNear =
                if (proximitySensor != null && proximitySensor.maximumRange <= 5.0f) {
                    distance < proximitySensor.maximumRange // Typical for binary or short-range sensors
                } else {
                    distance <= 5.0f // A general fallback for sensors that might report cm
                    // and don't have a very small maximumRange (e.g. if maxRange is huge, this is safer)
                }

            // Log.d("ProximitySensor", "Proximity distance: $distance, Is Near: $isCurrentlyNear")
            onProximityChanged(isCurrentlyNear)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Can be used to handle changes in sensor accuracy if needed
        Log.d("ProximitySensor", "Accuracy changed for ${sensor?.name}: $accuracy")
    }

    // Optional: Helper to check if the screen is off (often due to proximity during a call)
    // This is an indirect way and might not always correlate perfectly.
    fun isScreenOff(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !powerManager.isInteractive // isInteractive is false if screen is off
    }
}