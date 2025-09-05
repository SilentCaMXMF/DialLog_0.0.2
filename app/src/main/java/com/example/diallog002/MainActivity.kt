package com.example.diallog002

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var mainFavoritesAdapter: MainFavoritesAdapter
    private lateinit var statusTextView: TextView
    private lateinit var manageFavoritesButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var viewAnalyticsButton: Button
    private lateinit var favoritesCountText: TextView
    private lateinit var emptyFavoritesText: TextView
    
    private lateinit var callStateMonitor: CallStateMonitor
    private lateinit var callTracker: CallTracker
    private var favoriteContacts = mutableListOf<Contact>()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d("MainActivity", "All permissions granted")
            loadFavoriteContacts()
            startCallMonitoring()
        } else {
            Log.e("MainActivity", "Some permissions were denied")
            updateStatus("⚠️ Permissions required for call tracking")
            // Still try to load favorites since READ_CONTACTS might be granted
            tryLoadFavorites()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeCallTracking()
        initializeViews()
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
        favoritesRecyclerView = findViewById(R.id.favorites_recycler)
        statusTextView = findViewById(R.id.status_text)
        manageFavoritesButton = findViewById(R.id.manage_favorites_button)
        viewHistoryButton = findViewById(R.id.view_history_button)
        viewAnalyticsButton = findViewById(R.id.view_analytics_button)
        favoritesCountText = findViewById(R.id.favorites_count)
        emptyFavoritesText = findViewById(R.id.empty_favorites_text)
        
        // Set up Favorites RecyclerView - showing current auto-tracked contacts
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        mainFavoritesAdapter = MainFavoritesAdapter(
            onFavoriteClick = { contact ->
                // Show options for this favorite contact (view history, analytics, etc.)
                Log.d("MainActivity", "Favorite contact options: ${contact.name}")
                showFavoriteContactOptions(contact)
            },
            onCallClick = { contact ->
                // Direct call to this favorite contact
                Log.d("MainActivity", "Calling favorite contact: ${contact.name}")
                callFavoriteContact(contact)
            }
        )
        favoritesRecyclerView.adapter = mainFavoritesAdapter
        Log.d("MainActivity", "Main favorites adapter set with ${favoriteContacts.size} contacts")
        
        // Set up button click listeners
        manageFavoritesButton.setOnClickListener {
            // Navigate to FavoritesActivity to manage favorites
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }
        
        viewHistoryButton.setOnClickListener {
            // Navigate to CallHistoryActivity
            val intent = Intent(this, CallHistoryActivity::class.java)
            startActivity(intent)
        }
        
        viewAnalyticsButton.setOnClickListener {
            // Navigate to AnalyticsActivity
            val intent = Intent(this, AnalyticsActivity::class.java)
            startActivity(intent)
        }
        
        // Add long click listener for communication styles guide
        viewAnalyticsButton.setOnLongClickListener {
            val intent = Intent(this, CommunicationStyleActivity::class.java)
            startActivity(intent)
            true
        }
        
        // Test call logging button (keep for development)
        findViewById<Button>(R.id.test_call_logging_button).setOnClickListener {
            testFavorites() // Changed to test favorites instead
        }
        
        updateStatusDisplay()
    }
    
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE
        )
        
        // Check if READ_CONTACTS is already granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // If contacts permission is already granted, load favorites immediately
            loadFavoriteContacts()
        }
        
        requestPermissionLauncher.launch(permissions)
    }
    
    private fun tryLoadFavorites() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            loadFavoriteContacts()
        }
    }
    
    private fun loadFavoriteContacts() {
        Log.d("MainActivity", "loadFavoriteContacts: Starting to load favorites")
        coroutineScope.launch(Dispatchers.Main) {
            try {
                val allContacts = ContactManager.loadContacts(this@MainActivity)
                Log.d("MainActivity", "loadFavoriteContacts: Loaded ${allContacts.size} total contacts")
                
                val favoriteIds = ContactManager.getFavoriteContactIds(this@MainActivity)
                Log.d("MainActivity", "loadFavoriteContacts: Found ${favoriteIds.size} favorite IDs: $favoriteIds")
                
                favoriteContacts.clear()
                favoriteContacts.addAll(allContacts.filter { it.isFavorite })
                
                Log.d("MainActivity", "loadFavoriteContacts: Filtered ${favoriteContacts.size} favorite contacts")
                favoriteContacts.forEach { contact ->
                    Log.d("MainActivity", "loadFavoriteContacts: Favorite contact: ${contact.name} (${contact.id}) - ${contact.phoneNumber}")
                }
                
                mainFavoritesAdapter.updateFavorites(favoriteContacts)
                
                // Update favorites count display
                favoritesCountText.text = "${favoriteContacts.size} contacts"
                
                // Handle empty state visibility
                if (favoriteContacts.isEmpty()) {
                    favoritesRecyclerView.visibility = android.view.View.GONE
                    emptyFavoritesText.visibility = android.view.View.VISIBLE
                    Log.d("MainActivity", "loadFavoriteContacts: Showing empty state")
                } else {
                    favoritesRecyclerView.visibility = android.view.View.VISIBLE
                    emptyFavoritesText.visibility = android.view.View.GONE
                    Log.d("MainActivity", "loadFavoriteContacts: Showing ${favoriteContacts.size} favorites in RecyclerView")
                }
                
                val statusMessage = if (favoriteContacts.isEmpty()) {
                    "📱 No favorite contacts. Add favorites to start automatic call tracking."
                } else {
                    "🎯 Auto-tracking ${favoriteContacts.size} favorite contacts"
                }
                updateStatus(statusMessage)
                
                Log.d("MainActivity", "loadFavoriteContacts: Completed loading ${favoriteContacts.size} favorite contacts for auto-tracking")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading favorite contacts", e)
                updateStatus("Error loading contacts: ${e.message}")
            }
        }
    }
    
    private fun startCallMonitoring() {
        Log.d("MainActivity", "startCallMonitoring: Starting call state monitoring")
        try {
            callStateMonitor.startListening()
            Log.d("MainActivity", "startCallMonitoring: Successfully started call monitoring")
        } catch (e: Exception) {
            Log.e("MainActivity", "startCallMonitoring: Error starting call monitoring", e)
        }
    }
    
    private fun handleCallStateChange(callState: CallStateMonitor.CallState, phoneNumber: String?) {
        Log.d("MainActivity", "=== HANDLE CALL STATE CHANGE ===")
        Log.d("MainActivity", "Call state: $callState")
        Log.d("MainActivity", "Phone number: $phoneNumber")
        
        when (callState) {
            CallStateMonitor.CallState.RINGING -> {
                Log.d("MainActivity", "Processing RINGING state")
                val contact = findFavoriteContactByPhoneNumber(phoneNumber)
                if (contact != null) {
                    Log.d("MainActivity", "✅ Incoming call from FAVORITE: ${contact.name} (${contact.phoneNumber})")
                    updateStatus("📞 Incoming call from favorite: ${contact.name}")
                } else {
                    Log.d("MainActivity", "❌ Incoming call from NON-FAVORITE: $phoneNumber")
                    updateStatus("📞 Incoming call (not tracked)")
                }
            }
            CallStateMonitor.CallState.OFFHOOK -> {
                Log.d("MainActivity", "Processing OFFHOOK state (call answered/active)")
                val contact = findFavoriteContactByPhoneNumber(phoneNumber)
                if (contact != null) {
                    Log.d("MainActivity", "✅ STARTING TRACKING for favorite: ${contact.name} (${contact.phoneNumber})")
                    try {
                        callTracker.startTracking(contact.name, contact.phoneNumber)
                        updateStatus("🎯 Auto-tracking call with: ${contact.name}")
                        Log.d("MainActivity", "Call tracking started successfully")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error starting call tracking", e)
                        updateStatus("❌ Error starting call tracking")
                    }
                } else {
                    Log.d("MainActivity", "❌ Call with NON-FAVORITE, not tracking: $phoneNumber")
                    updateStatus("📞 Call in progress (not tracked)")
                }
            }
            CallStateMonitor.CallState.IDLE -> {
                Log.d("MainActivity", "Processing IDLE state (call ended)")
                if (callTracker.isCurrentlyTracking()) {
                    Log.d("MainActivity", "✅ STOPPING call tracking - call was being tracked")
                    try {
                        callTracker.stopTracking()
                        updateStatus("✅ Call tracked and saved")
                        Log.d("MainActivity", "Call tracking stopped successfully")
                        // Refresh to show updated counts
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadFavoriteContacts()
                        }, 1000)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error stopping call tracking", e)
                        updateStatus("❌ Error saving call data")
                    }
                } else {
                    Log.d("MainActivity", "❌ No call was being tracked")
                    updateStatus("📱 Ready for next call")
                }
            }
        }
        Log.d("MainActivity", "=== END HANDLE CALL STATE CHANGE ===")
    }
    
    private fun findFavoriteContactByPhoneNumber(phoneNumber: String?): Contact? {
        Log.d("MainActivity", "findFavoriteContactByPhoneNumber: Looking for $phoneNumber")
        if (phoneNumber == null) {
            Log.d("MainActivity", "findFavoriteContactByPhoneNumber: phoneNumber is null")
            return null
        }
        
        Log.d("MainActivity", "findFavoriteContactByPhoneNumber: Searching among ${favoriteContacts.size} favorite contacts")
        val cleanPhoneNumber = phoneNumber.replace("\\s".toRegex(), "")
        Log.d("MainActivity", "findFavoriteContactByPhoneNumber: Clean phone number: $cleanPhoneNumber")
        
        favoriteContacts.forEach { contact ->
            val cleanContactNumber = contact.phoneNumber.replace("\\s".toRegex(), "")
            Log.d("MainActivity", "findFavoriteContactByPhoneNumber: Comparing with ${contact.name}: $cleanContactNumber")
        }
        
        val foundContact = favoriteContacts.find { contact ->
            contact.phoneNumber.replace("\\s".toRegex(), "") == cleanPhoneNumber
        }
        
        if (foundContact != null) {
            Log.d("MainActivity", "findFavoriteContactByPhoneNumber: FOUND MATCH: ${foundContact.name}")
        } else {
            Log.d("MainActivity", "findFavoriteContactByPhoneNumber: NO MATCH FOUND")
        }
        
        return foundContact
    }
    
    private fun callFavoriteContact(contact: Contact) {
        Log.d("MainActivity", "callFavoriteContact: Attempting to call ${contact.name} at ${contact.phoneNumber}")
        
        // Check if we have CALL_PHONE permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("MainActivity", "callFavoriteContact: CALL_PHONE permission not granted")
            updateStatus("⚠️ Phone permission required to make calls")
            return
        }
        
        try {
            val phoneNumber = contact.phoneNumber
            Log.d("MainActivity", "callFavoriteContact: Initiating call to $phoneNumber")
            
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = android.net.Uri.parse("tel:$phoneNumber")
            
            startActivity(callIntent)
            updateStatus("📞 Calling ${contact.name}...")
            
            Log.d("MainActivity", "callFavoriteContact: Call intent started successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "callFavoriteContact: Error making call", e)
            updateStatus("❌ Error making call to ${contact.name}")
        }
    }
    
    private fun showFavoriteContactOptions(contact: Contact) {
        // Show options for this favorite contact (view history, analytics, etc.)
        val options = arrayOf(
            "View Call History",
            "View Analytics", 
            "Remove from Favorites"
        )
        
        android.app.AlertDialog.Builder(this)
            .setTitle(contact.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // View call history for this contact
                        val intent = Intent(this, CallHistoryActivity::class.java)
                        intent.putExtra("contact_name", contact.name)
                        startActivity(intent)
                    }
                    1 -> {
                        // View analytics for this contact
                        val intent = Intent(this, AnalyticsActivity::class.java)
                        intent.putExtra("contact_name", contact.name)
                        startActivity(intent)
                    }
                    2 -> {
                        // Remove from favorites
                        removeFavoriteContact(contact)
                    }
                }
            }
            .show()
    }
    
    private fun removeFavoriteContact(contact: Contact) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Remove from Favorites?")
            .setMessage("Remove ${contact.name} from favorites? Future calls will no longer be tracked automatically.")
            .setPositiveButton("Remove") { _, _ ->
                ContactManager.toggleFavorite(this, contact.id)
                loadFavoriteContacts() // Refresh the list
                updateStatus("${contact.name} removed from favorites")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun testFavorites() {
        Log.d("MainActivity", "testFavorites: Starting favorites test")
        
        // First test basic UI updates
        favoritesCountText.text = "TEST: 99 contacts"
        favoritesCountText.setBackgroundColor(android.graphics.Color.YELLOW)
        
        // Test by directly adding a hardcoded contact to the adapter
        val testContact = Contact(
            id = "test_123",
            name = "Test Favorite Contact",
            phoneNumber = "+1234567890",
            isFavorite = true
        )
        
        Log.d("MainActivity", "testFavorites: Adding hardcoded test contact to adapter")
        favoriteContacts.clear()
        favoriteContacts.add(testContact)
        
        Log.d("MainActivity", "testFavorites: Calling adapter updateFavorites with ${favoriteContacts.size} contacts")
        mainFavoritesAdapter.updateFavorites(favoriteContacts)
        
        // Force RecyclerView to be visible and try different approaches
        Log.d("MainActivity", "testFavorites: Making RecyclerView visible")
        favoritesRecyclerView.visibility = android.view.View.VISIBLE
        favoritesRecyclerView.setBackgroundColor(android.graphics.Color.RED) // Make it obvious
        emptyFavoritesText.visibility = android.view.View.GONE
        
        // Force refresh
        mainFavoritesAdapter.notifyDataSetChanged()
        favoritesRecyclerView.invalidate()
        favoritesRecyclerView.requestLayout()
        
        Log.d("MainActivity", "testFavorites: RecyclerView childCount = ${favoritesRecyclerView.childCount}")
        Log.d("MainActivity", "testFavorites: RecyclerView adapter itemCount = ${mainFavoritesAdapter.itemCount}")
        
        updateStatus("🔧 Test contact added - RecyclerView should be RED")
        
        // Also test the SharedPreferences system
        val currentFavorites = ContactManager.getFavoriteContactIds(this)
        Log.d("MainActivity", "testFavorites: Current SharedPreferences favorites: $currentFavorites")
        
        // Try to add a real contact as favorite if available
        try {
            val allContacts = ContactManager.loadContacts(this)
            if (allContacts.isNotEmpty()) {
                val firstContact = allContacts.first()
                Log.d("MainActivity", "testFavorites: Adding real contact as favorite: ${firstContact.name} (${firstContact.id})")
                ContactManager.toggleFavorite(this, firstContact.id)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "testFavorites: Error testing with real contacts", e)
        }
        
        Log.d("MainActivity", "testFavorites: Test completed")
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
        // Refresh favorites to update counts
        loadFavoriteContacts()
    }
    
    private fun updateStatus(message: String) {
        statusTextView.text = message
        Log.d("MainActivity", message)
    }
    
    private fun updateStatusDisplay() {
        val statusMessage = if (favoriteContacts.isEmpty()) {
            "📱 No favorites selected - Add contacts to start auto-tracking"
        } else {
            "🎯 Auto-tracking enabled for ${favoriteContacts.size} favorite contacts"
        }
        updateStatus(statusMessage)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh favorites list when returning to this activity
        Log.d("MainActivity", "onResume: Refreshing favorites list")
        tryLoadFavorites()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        callStateMonitor.stopListening()
        callTracker.cleanup()
        coroutineScope.cancel()
    }
}
