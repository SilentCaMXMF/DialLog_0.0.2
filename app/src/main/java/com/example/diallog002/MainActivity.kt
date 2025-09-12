package com.example.diallog002

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
    private lateinit var statusTextView: TextView
    private lateinit var manageFavoritesButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var viewAnalyticsButton: Button
    
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
        statusTextView = findViewById(R.id.status_text)
        manageFavoritesButton = findViewById(R.id.manage_favorites_button)
        viewHistoryButton = findViewById(R.id.view_history_button)
        viewAnalyticsButton = findViewById(R.id.view_analytics_button)
        
        // Set up button click listeners
        manageFavoritesButton.setOnClickListener {
            // Navigate to FavoritesManagementActivity to manage favorites
            val intent = Intent(this, FavoritesManagementActivity::class.java)
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
        Log.d("MainActivity", "loadFavoriteContacts: Starting to load favorite numbers")
        coroutineScope.launch(Dispatchers.Main) {
            try {
                // Load favorite phone numbers instead of contacts
                val favoriteNumbers = ContactManager.getFavoriteNumbers(this@MainActivity)
                Log.d("MainActivity", "loadFavoriteContacts: Found ${favoriteNumbers.size} favorite numbers")
                
                favoriteNumbers.forEach { favoriteNumber ->
                    Log.d("MainActivity", "loadFavoriteContacts: Favorite number: ${favoriteNumber.contactName} - ${favoriteNumber.phoneNumber}")
                }
                
                // Update status based on favorite numbers count
                val statusMessage = if (favoriteNumbers.isEmpty()) {
                    "📱 No favorite phone numbers. Add favorites to start automatic call tracking."
                } else {
                    "🎯 Auto-tracking ${favoriteNumbers.size} favorite phone numbers"
                }
                updateStatus(statusMessage)
                
                Log.d("MainActivity", "loadFavoriteContacts: Completed loading ${favoriteNumbers.size} favorite numbers for auto-tracking")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading favorite numbers", e)
                updateStatus("Error loading favorites: ${e.message}")
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
                val favoriteNumber = findFavoriteContactByPhoneNumber(phoneNumber)
                if (favoriteNumber != null) {
                    Log.d("MainActivity", "✅ Incoming call from FAVORITE: ${favoriteNumber.contactName} (${favoriteNumber.phoneNumber})")
                    updateStatus("📞 Incoming call from favorite: ${favoriteNumber.contactName}")
                } else {
                    Log.d("MainActivity", "❌ Incoming call from NON-FAVORITE: $phoneNumber")
                    updateStatus("📞 Incoming call (not tracked)")
                }
            }
            CallStateMonitor.CallState.OFFHOOK -> {
                Log.d("MainActivity", "Processing OFFHOOK state (call answered/active)")
                val favoriteNumber = findFavoriteContactByPhoneNumber(phoneNumber)
                if (favoriteNumber != null) {
                    Log.d("MainActivity", "✅ STARTING TRACKING for favorite: ${favoriteNumber.contactName} (${favoriteNumber.phoneNumber})")
                    try {
                        callTracker.startTracking(favoriteNumber.contactName, favoriteNumber.phoneNumber)
                        updateStatus("🎯 Auto-tracking call with: ${favoriteNumber.contactName}")
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
    
    private fun findFavoriteContactByPhoneNumber(phoneNumber: String?): FavoriteNumber? {
        Log.d("MainActivity", "findFavoriteContactByPhoneNumber: Looking for $phoneNumber")
        if (phoneNumber == null) {
            Log.d("MainActivity", "findFavoriteContactByPhoneNumber: phoneNumber is null")
            return null
        }
        
        val cleanPhoneNumber = phoneNumber.replace("\\s".toRegex(), "")
        Log.d("MainActivity", "findFavoriteContactByPhoneNumber: Clean phone number: $cleanPhoneNumber")
        
        val favoriteNumber = ContactManager.findFavoriteByPhoneNumber(this, phoneNumber)
        
        if (favoriteNumber != null) {
            Log.d("MainActivity", "findFavoriteContactByPhoneNumber: FOUND MATCH: ${favoriteNumber.contactName} - ${favoriteNumber.phoneNumber}")
        } else {
            Log.d("MainActivity", "findFavoriteContactByPhoneNumber: NO MATCH FOUND")
        }
        
        return favoriteNumber
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
        Log.d("MainActivity", "testFavorites: Testing favorites system")
        
        // Test the SharedPreferences system
        val currentFavorites = ContactManager.getFavoriteContactIds(this)
        Log.d("MainActivity", "testFavorites: Current SharedPreferences favorites: $currentFavorites")
        
        updateStatus("🔧 Testing favorites system - check logs")
        
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
                // Create multiple realistic test call logs with different dates
                val testCallLogs = listOf(
                    CallLog(
                        contactName = "Alice Johnson",
                        phoneNumber = "+1234567890",
                        speakingTime = 45000L, // 45 seconds
                        listeningTime = 30000L, // 30 seconds
                        totalDuration = 75000L, // 1 minute 15 seconds
                        timestamp = Date(System.currentTimeMillis() - 86400000) // 1 day ago
                    ),
                    CallLog(
                        contactName = "Bob Smith",
                        phoneNumber = "+1987654321",
                        speakingTime = 120000L, // 2 minutes
                        listeningTime = 180000L, // 3 minutes
                        totalDuration = 300000L, // 5 minutes
                        timestamp = Date(System.currentTimeMillis() - 172800000) // 2 days ago
                    ),
                    CallLog(
                        contactName = "Alice Johnson",
                        phoneNumber = "+1234567890",
                        speakingTime = 90000L, // 1.5 minutes
                        listeningTime = 60000L, // 1 minute
                        totalDuration = 150000L, // 2.5 minutes
                        timestamp = Date(System.currentTimeMillis() - 259200000) // 3 days ago
                    ),
                    CallLog(
                        contactName = "Carol Davis",
                        phoneNumber = "+1555123456",
                        speakingTime = 60000L, // 1 minute
                        listeningTime = 120000L, // 2 minutes
                        totalDuration = 180000L, // 3 minutes
                        timestamp = Date(System.currentTimeMillis() - 345600000) // 4 days ago
                    ),
                    CallLog(
                        contactName = "Bob Smith",
                        phoneNumber = "+1987654321",
                        speakingTime = 200000L, // 3.33 minutes
                        listeningTime = 100000L, // 1.67 minutes
                        totalDuration = 300000L, // 5 minutes
                        timestamp = Date() // Now
                    )
                )
                
                for (testLog in testCallLogs) {
                    CallLogManager.addCallLog(testLog)
                    delay(100) // Small delay between inserts
                }
                
                updateStatus("Created ${testCallLogs.size} test call logs successfully!")
                
                // Wait a moment and then navigate to history to see them
                delay(1000)
                val intent = Intent(this@MainActivity, CallHistoryActivity::class.java)
                startActivity(intent)
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Error creating test call logs", e)
                updateStatus("Error creating test call logs: ${e.message}")
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
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                showSettingsDialog()
                true
            }
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showSettingsDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Settings")
            .setMessage("DialLog Settings\n\n" +
                       "Audio-based talk/listen measurement is active.\n" +
                       "Environment noise is calibrated automatically at call start.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showAboutDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("About DialLog")
            .setMessage("DialLog v3.0.0\n\n" +
                       "Automatic call tracking for your favorite contacts.\n\n" +
                       "Features:\n" +
                       "• Real-time audio analysis for accurate talk/listen tracking\n" +
                       "• Automatic environment noise adaptation\n" +
                       "• Comprehensive favorites management\n" +
                       "• Communication analytics and insights\n" +
                       "• Direct calling from favorites list")
            .setPositiveButton("OK", null)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        callStateMonitor.stopListening()
        callTracker.cleanup()
        coroutineScope.cancel()
    }
}
