package com.example.diallog002

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.diallog002.data.CallLog
import com.example.diallog002.data.CallLogManager
import kotlinx.coroutines.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var startStopButton: Button
    private lateinit var playButton: FloatingActionButton
    private lateinit var statusTextView: TextView
    private lateinit var currentCallTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchButton: Button
    private lateinit var viewHistoryButton: Button
    
    private lateinit var callStateMonitor: CallStateMonitor
    private lateinit var callTracker: CallTracker
    private var contacts = mutableListOf<Contact>()
    private var allContacts = mutableListOf<Contact>() // Keep original list for search
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d("MainActivity", "All permissions granted")
            loadContacts()
            startCallMonitoring()
        } else {
            Log.e("MainActivity", "Some permissions were denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeCallTracking()
        initializeViews()
        setupSearchFunctionality()
        requestPermissions()
    }
    
    private fun initializeCallTracking() {
        CallLogManager.initialize(this)
        callStateMonitor = CallStateMonitor(this) { callState, phoneNumber ->
            handleCallStateChange(callState, phoneNumber)
        }
        callTracker = CallTracker(this) { callLog ->
            onCallLogUpdated(callLog)
        }
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        startStopButton = findViewById(R.id.select_contact_button)
        playButton = findViewById(R.id.fabStartCall)
        statusTextView = findViewById(R.id.status_text)
        currentCallTextView = findViewById(R.id.current_call_text)
        searchEditText = findViewById(R.id.search_edit_text)
        clearSearchButton = findViewById(R.id.clear_search_button)
        viewHistoryButton = findViewById(R.id.view_history_button)
        
        // Set up Contacts RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ContactsAdapter(
            contacts = contacts,
            onContactClick = { contact: Contact ->
                // Handle contact click - could show call history
                Log.d("MainActivity", "Contact clicked: ${contact.name}")
            },
            onFavoriteToggle = { contact: Contact ->
                toggleFavorite(contact)
            }
        )
        recyclerView.adapter = adapter
        
        // Set up button click listeners
        startStopButton.setOnClickListener {
            if (callTracker.isCurrentlyTracking()) {
                stopCallTracking()
            } else {
                // Show contact selection dialog
                showContactSelectionDialog()
            }
        }
        
        playButton.setOnClickListener {
            if (callTracker.isCurrentlyTracking()) {
                stopCallTracking()
            } else {
                // Show contact selection dialog
                showContactSelectionDialog()
            }
        }
        
        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
            filterContacts("")
        }
        
        viewHistoryButton.setOnClickListener {
            // Navigate to CallHistoryActivity
            val intent = Intent(this, CallHistoryActivity::class.java)
            startActivity(intent)
        }
        
        // Test call logging button
        findViewById<Button>(R.id.test_call_logging_button).setOnClickListener {
            testCallLogging()
        }
        
        updateUI()
    }
    
    private fun setupSearchFunctionality() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterContacts(s.toString())
            }
        })
    }
    
    private fun filterContacts(query: String) {
        if (query.isEmpty()) {
            contacts.clear()
            contacts.addAll(allContacts)
        } else {
            contacts.clear()
            contacts.addAll(allContacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                contact.phoneNumber.contains(query, ignoreCase = true)
            })
        }
        adapter.updateContacts(contacts)
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_CALL_LOG
        )
        
        requestPermissionLauncher.launch(permissions)
    }
    
    private fun loadContacts() {
        coroutineScope.launch {
            try {
                allContacts = ContactManager.loadContacts(this@MainActivity).toMutableList()
                contacts.clear()
                contacts.addAll(allContacts)
                adapter.updateContacts(contacts)
                Log.d("MainActivity", "Loaded ${contacts.size} contacts")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading contacts", e)
            }
        }
    }
    
    private fun startCallMonitoring() {
        callStateMonitor.startListening()
        Log.d("MainActivity", "Started call monitoring")
    }
    
    private fun handleCallStateChange(callState: CallStateMonitor.CallState, phoneNumber: String?) {
        when (callState) {
            CallStateMonitor.CallState.RINGING -> {
                Log.d("MainActivity", "Call ringing from: $phoneNumber")
                updateStatus("Incoming call from: $phoneNumber")
            }
            CallStateMonitor.CallState.OFFHOOK -> {
                Log.d("MainActivity", "Call answered from: $phoneNumber")
                if (phoneNumber != null && callStateMonitor.isCallFromFavoriteContact(phoneNumber)) {
                    val contact = findContactByPhoneNumber(phoneNumber)
                    if (contact != null) {
                        startCallTracking(contact.name, contact.phoneNumber)
                        updateStatus("Tracking call with: ${contact.name}")
                    }
                } else {
                    updateStatus("Call in progress (not from favorite contact)")
                }
            }
            CallStateMonitor.CallState.IDLE -> {
                Log.d("MainActivity", "Call ended")
                if (callTracker.isCurrentlyTracking()) {
                    stopCallTracking()
                }
                updateStatus("No active call")
            }
        }
    }
    
    private fun findContactByPhoneNumber(phoneNumber: String): Contact? {
        return contacts.find { contact ->
            contact.phoneNumber.replace("\\s".toRegex(), "") == 
            phoneNumber.replace("\\s".toRegex(), "")
        }
    }
    
    private fun startCallTracking(contactName: String, phoneNumber: String) {
        callTracker.startTracking(contactName, phoneNumber)
        updateUI()
        updateStatus("Tracking call with: $contactName")
    }
    
    private fun stopCallTracking() {
        callTracker.stopTracking()
        updateUI()
        updateStatus("Call tracking stopped")
    }
    
    private fun toggleFavorite(contact: Contact) {
        val newFavoriteState = ContactManager.toggleFavorite(this, contact.id)
        contact.isFavorite = newFavoriteState
        adapter.updateContacts(contacts)
        
        val message = if (newFavoriteState) "Added to favorites" else "Removed from favorites"
        updateStatus("${contact.name}: $message")
    }
    
    private fun showContactSelectionDialog() {
        // For now, just show a simple message
        // In a real app, you'd show a dialog with contact selection
        updateStatus("Please select a contact to track manually")
    }
    
    private fun testCallLogging() {
        coroutineScope.launch {
            try {
                // Create a test call log
                val testCallLog = CallLog(
                    contactName = "Test Contact",
                    phoneNumber = "+1234567890",
                    speakingTime = 30000L, // 30 seconds
                    listeningTime = 45000L, // 45 seconds
                    totalDuration = 75000L, // 1 minute 15 seconds
                    timestamp = Date()
                )
                
                CallLogManager.addCallLog(testCallLog)
                updateStatus("Test call log created successfully!")
                
                // Wait a moment and then navigate to history to see it
                delay(1000)
                val intent = Intent(this@MainActivity, CallHistoryActivity::class.java)
                startActivity(intent)
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Error creating test call log", e)
                updateStatus("Error creating test call log: ${e.message}")
            }
        }
    }
    
    private fun onCallLogUpdated(callLog: CallLog) {
        updateStatus("Call logged: ${callLog.contactName} - Speaking: ${callLog.speakingTime}ms, Listening: ${callLog.listeningTime}ms")
        updateUI()
    }
    
    private fun updateStatus(message: String) {
        statusTextView.text = message
        Log.d("MainActivity", message)
    }
    
    private fun updateUI() {
        if (callTracker.isCurrentlyTracking()) {
            startStopButton.text = "Stop Tracking"
            playButton.setImageResource(android.R.drawable.ic_media_pause)
            currentCallTextView.text = "Tracking: ${callTracker.getCurrentContactName()}"
        } else {
            startStopButton.text = "Start Tracking"
            playButton.setImageResource(android.R.drawable.ic_media_play)
            currentCallTextView.text = "No active tracking"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        callStateMonitor.stopListening()
        callTracker.cleanup()
        coroutineScope.cancel()
    }
}
