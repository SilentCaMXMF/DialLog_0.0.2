package com.example.diallog002

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import java.util.*

class CallStateMonitor(
    private val context: Context,
    private val onCallStateChanged: (CallState, String?) -> Unit
) {
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var currentCallNumber: String? = null
    private var isCallActive = false
    
    enum class CallState {
        IDLE, RINGING, OFFHOOK
    }
    
    fun startListening() {
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                Log.d("CallStateMonitor", "=== CALL STATE CHANGED ===")
                Log.d("CallStateMonitor", "Raw state: $state")
                Log.d("CallStateMonitor", "Phone number: $phoneNumber")
                Log.d("CallStateMonitor", "Current call number: $currentCallNumber")
                Log.d("CallStateMonitor", "Is call active: $isCallActive")
                
                val callState = when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                        Log.d("CallStateMonitor", "State: IDLE")
                        if (isCallActive) {
                            Log.d("CallStateMonitor", "Call ended - was active")
                            isCallActive = false
                            val endingCallNumber = currentCallNumber
                            currentCallNumber = null
                            Log.d("CallStateMonitor", "Ending call from: $endingCallNumber")
                        } else {
                            Log.d("CallStateMonitor", "State IDLE - no active call")
                        }
                        CallState.IDLE
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d("CallStateMonitor", "State: RINGING from $phoneNumber")
                        currentCallNumber = phoneNumber
                        
                        // Check if it's a favorite contact
                        val isFavorite = isCallFromFavoriteContact(phoneNumber)
                        Log.d("CallStateMonitor", "Is favorite contact: $isFavorite")
                        
                        CallState.RINGING
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        Log.d("CallStateMonitor", "State: OFFHOOK (call answered/outgoing)")
                        Log.d("CallStateMonitor", "Phone number from parameter: $phoneNumber")
                        Log.d("CallStateMonitor", "Using current call number: $currentCallNumber")
                        
                        isCallActive = true
                        // Use current call number if phone number parameter is null
                        val activeCallNumber = phoneNumber ?: currentCallNumber
                        currentCallNumber = activeCallNumber
                        
                        Log.d("CallStateMonitor", "Active call number set to: $activeCallNumber")
                        
                        // Check if it's a favorite contact
                        val isFavorite = isCallFromFavoriteContact(activeCallNumber)
                        Log.d("CallStateMonitor", "Is favorite contact: $isFavorite")
                        
                        CallState.OFFHOOK
                    }
                    else -> {
                        Log.d("CallStateMonitor", "Unknown state: $state")
                        CallState.IDLE
                    }
                }
                
                Log.d("CallStateMonitor", "Calling onCallStateChanged with: $callState, $currentCallNumber")
                onCallStateChanged(callState, currentCallNumber)
                Log.d("CallStateMonitor", "=== END CALL STATE CHANGE ===")
            }
        }
        
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        Log.d("CallStateMonitor", "Started listening to call state changes")
    }
    
    fun stopListening() {
        phoneStateListener?.let { listener ->
            telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
        }
        phoneStateListener = null
        telephonyManager = null
        Log.d("CallStateMonitor", "Stopped listening to call state changes")
    }
    
    fun isCallFromFavoriteContact(phoneNumber: String?): Boolean {
        if (phoneNumber == null) {
            Log.d("CallStateMonitor", "isCallFromFavoriteContact: phoneNumber is null")
            return false
        }
        
        Log.d("CallStateMonitor", "isCallFromFavoriteContact: Checking if $phoneNumber is a favorite")
        
        val favoriteContactIds = ContactManager.getFavoriteContactIds(context)
        Log.d("CallStateMonitor", "isCallFromFavoriteContact: Found ${favoriteContactIds.size} favorite IDs")
        
        // Optimize by only loading favorite contacts, not all contacts
        val favoriteContacts = ContactManager.getFavoriteContacts(context)
        Log.d("CallStateMonitor", "isCallFromFavoriteContact: Loaded ${favoriteContacts.size} favorite contacts")
        
        val cleanPhoneNumber = phoneNumber.replace("\\s".toRegex(), "")
        Log.d("CallStateMonitor", "isCallFromFavoriteContact: Clean phone number: $cleanPhoneNumber")
        
        val isFavorite = favoriteContacts.any { contact ->
            val cleanContactNumber = contact.phoneNumber.replace("\\s".toRegex(), "")
            val matches = cleanContactNumber == cleanPhoneNumber
            if (matches) {
                Log.d("CallStateMonitor", "isCallFromFavoriteContact: MATCH found with ${contact.name} (${contact.phoneNumber})")
            }
            matches
        }
        
        Log.d("CallStateMonitor", "isCallFromFavoriteContact: Result = $isFavorite")
        return isFavorite
    }
}