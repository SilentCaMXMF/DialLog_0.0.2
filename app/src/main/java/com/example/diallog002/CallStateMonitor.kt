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
                val callState = when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (isCallActive) {
                            Log.d("CallStateMonitor", "Call ended")
                            isCallActive = false
                            currentCallNumber = null
                        }
                        CallState.IDLE
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d("CallStateMonitor", "Call ringing from: $phoneNumber")
                        currentCallNumber = phoneNumber
                        CallState.RINGING
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        Log.d("CallStateMonitor", "Call answered from: $phoneNumber")
                        isCallActive = true
                        currentCallNumber = phoneNumber
                        CallState.OFFHOOK
                    }
                    else -> CallState.IDLE
                }
                
                onCallStateChanged(callState, currentCallNumber)
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
        if (phoneNumber == null) return false
        
        val favoriteContactIds = ContactManager.getFavoriteContactIds(context)
        val contacts = ContactManager.loadContacts(context)
        
        return contacts.any { contact ->
            contact.phoneNumber.replace("\\s".toRegex(), "") == phoneNumber.replace("\\s".toRegex(), "") &&
            favoriteContactIds.contains(contact.id)
        }
    }
}