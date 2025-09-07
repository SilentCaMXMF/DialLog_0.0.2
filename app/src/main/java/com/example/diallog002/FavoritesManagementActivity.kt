package com.example.diallog002

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import kotlinx.coroutines.*

class FavoritesManagementActivity : AppCompatActivity() {
    
    // UI Components
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchButton: ImageButton
    private lateinit var favoritesCountText: TextView
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var allContactsRecyclerView: RecyclerView
    private lateinit var noFavoritesText: TextView
    private lateinit var noContactsText: TextView
    private lateinit var addAllButton: Button
    private lateinit var removeAllButton: Button
    private lateinit var sortSpinner: Spinner
    
    // Adapters
    private lateinit var favoritesManagementAdapter: FavoritesManagementAdapter
    private lateinit var contactsSelectionAdapter: ContactsSelectionAdapter
    
    // Data
    private var favoriteContacts = mutableListOf<Contact>()
    private var allContacts = mutableListOf<Contact>()
    private var filteredContacts = mutableListOf<Contact>()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites_management)
        
        setupActionBar()
        initializeViews()
        setupRecyclerViews()
        setupSearchFunctionality()
        setupButtons()
        loadContacts()
    }
    
    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Manage Favorites"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun initializeViews() {
        searchEditText = findViewById(R.id.search_edit_text)
        clearSearchButton = findViewById(R.id.clear_search_button)
        favoritesCountText = findViewById(R.id.favorites_count_text)
        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view)
        allContactsRecyclerView = findViewById(R.id.all_contacts_recycler_view)
        noFavoritesText = findViewById(R.id.no_favorites_text)
        noContactsText = findViewById(R.id.no_contacts_text)
        addAllButton = findViewById(R.id.add_all_button)
        removeAllButton = findViewById(R.id.remove_all_button)
        sortSpinner = findViewById(R.id.sort_spinner)
    }
    
    private fun setupRecyclerViews() {
        // Favorites RecyclerView with drag to reorder and swipe to remove
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        favoritesManagementAdapter = FavoritesManagementAdapter(
            initialContacts = favoriteContacts,
            onContactClick = { contact -> showFavoriteContactOptions(contact) },
            onCallClick = { contact -> callContact(contact) },
            onRemoveClick = { contact -> removeFavorite(contact) }
        )
        favoritesRecyclerView.adapter = favoritesManagementAdapter
        
        // Add swipe-to-remove functionality
        val itemTouchHelper = ItemTouchHelper(FavoriteItemTouchCallback())
        itemTouchHelper.attachToRecyclerView(favoritesRecyclerView)
        
        // All Contacts RecyclerView
        allContactsRecyclerView.layoutManager = LinearLayoutManager(this)
        contactsSelectionAdapter = ContactsSelectionAdapter(
            initialContacts = filteredContacts,
            onContactClick = { contact -> addToFavorites(contact) },
            onBulkSelectionChanged = { count -> updateBulkSelectionUI(count) }
        )
        allContactsRecyclerView.adapter = contactsSelectionAdapter
    }
    
    private fun setupSearchFunctionality() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterContacts(s.toString())
            }
        })
        
        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
        }
    }
    
    private fun setupButtons() {
        addAllButton.setOnClickListener { addAllFilteredToFavorites() }
        removeAllButton.setOnClickListener { removeAllFavorites() }
        
        // Sort spinner
        val sortOptions = arrayOf("Name A-Z", "Name Z-A", "Recently Added", "Most Calls")
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = sortAdapter
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortFavorites(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun loadContacts() {
        Log.d("FavoritesManagement", "loadContacts: Starting to load contacts")
        
        // Check permissions first
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("FavoritesManagement", "loadContacts: READ_CONTACTS permission not granted")
            showError("Contacts permission is required to manage favorites")
            return
        }
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d("FavoritesManagement", "loadContacts: Calling ContactManager.loadContacts")
                val loadedContacts = ContactManager.loadContacts(this@FavoritesManagementActivity)
                Log.d("FavoritesManagement", "loadContacts: ContactManager returned ${loadedContacts.size} contacts")
                
                // Switch back to Main thread for UI updates
                withContext(Dispatchers.Main) {
                    allContacts.clear()
                    allContacts.addAll(loadedContacts)
                    
                    // Separate favorites from non-favorites
                    favoriteContacts.clear()
                    favoriteContacts.addAll(allContacts.filter { it.isFavorite })
                    
                    filteredContacts.clear()
                    filteredContacts.addAll(allContacts.filter { !it.isFavorite })
                    
                    Log.d("FavoritesManagement", "loadContacts: Separated into ${favoriteContacts.size} favorites and ${filteredContacts.size} non-favorites")
                    
                    updateUI()
                    
                    if (allContacts.isEmpty()) {
                        showError("No contacts found. Please check your contacts app and permissions.")
                    } else {
                        Log.d("FavoritesManagement", "Loaded ${allContacts.size} total contacts, ${favoriteContacts.size} favorites")
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoritesManagement", "Error loading contacts", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to load contacts: ${e.message}")
                }
            }
        }
    }
    
    private fun updateUI() {
        Log.d("FavoritesManagement", "updateUI: Updating UI with ${favoriteContacts.size} favorites and ${filteredContacts.size} filtered contacts")
        
        // Update favorites section
        try {
            favoritesManagementAdapter.updateContacts(favoriteContacts)
            favoritesCountText.text = "${favoriteContacts.size} favorite contacts"
            
            if (favoriteContacts.isEmpty()) {
                noFavoritesText.visibility = View.VISIBLE
                favoritesRecyclerView.visibility = View.GONE
                removeAllButton.isEnabled = false
                Log.d("FavoritesManagement", "updateUI: Showing empty favorites state")
            } else {
                noFavoritesText.visibility = View.GONE
                favoritesRecyclerView.visibility = View.VISIBLE
                removeAllButton.isEnabled = true
                Log.d("FavoritesManagement", "updateUI: Showing ${favoriteContacts.size} favorites")
            }
        } catch (e: Exception) {
            Log.e("FavoritesManagement", "Error updating favorites UI", e)
        }
        
        // Update all contacts section
        try {
            Log.d("FavoritesManagement", "updateUI: Updating contacts adapter with ${filteredContacts.size} contacts")
            contactsSelectionAdapter.updateContacts(filteredContacts)
            
            if (filteredContacts.isEmpty()) {
                noContactsText.visibility = View.VISIBLE
                allContactsRecyclerView.visibility = View.GONE
                addAllButton.isEnabled = false
                Log.d("FavoritesManagement", "updateUI: Showing empty contacts state")
                
                // Update the message based on why it's empty
                if (allContacts.isEmpty()) {
                    noContactsText.text = "No contacts found. Please check your contacts app and permissions."
                } else if (allContacts.all { it.isFavorite }) {
                    noContactsText.text = "All contacts are already in favorites!"
                } else {
                    noContactsText.text = "No contacts match your search."
                }
            } else {
                noContactsText.visibility = View.GONE
                allContactsRecyclerView.visibility = View.VISIBLE
                addAllButton.isEnabled = true
                Log.d("FavoritesManagement", "updateUI: Showing ${filteredContacts.size} non-favorite contacts")
            }
        } catch (e: Exception) {
            Log.e("FavoritesManagement", "Error updating contacts UI", e)
        }
        
        Log.d("FavoritesManagement", "updateUI: UI update completed")
    }
    
    private fun filterContacts(query: String) {
        filteredContacts.clear()
        if (query.isEmpty()) {
            filteredContacts.addAll(allContacts.filter { !it.isFavorite })
        } else {
            filteredContacts.addAll(allContacts.filter { contact ->
                !contact.isFavorite && (
                    contact.name.contains(query, ignoreCase = true) ||
                    contact.phoneNumber.contains(query, ignoreCase = true)
                )
            })
        }
        updateUI()
    }
    
    private fun sortFavorites(sortType: Int) {
        when (sortType) {
            0 -> favoriteContacts.sortBy { it.name } // A-Z
            1 -> favoriteContacts.sortByDescending { it.name } // Z-A
            2 -> favoriteContacts.reverse() // Recently added (reverse current order)
            3 -> {
                // Sort by most calls - would need call count data
                // For now, just sort by name
                favoriteContacts.sortBy { it.name }
            }
        }
        favoritesManagementAdapter.updateContacts(favoriteContacts)
    }
    
    private fun addToFavorites(contact: Contact) {
        coroutineScope.launch {
            try {
                val success = ContactManager.toggleFavorite(this@FavoritesManagementActivity, contact.id)
                if (success) {
                    contact.isFavorite = true
                    favoriteContacts.add(contact)
                    filteredContacts.remove(contact)
                    updateUI()
                    showSuccess("${contact.name} added to favorites")
                } else {
                    showError("Failed to add ${contact.name} to favorites")
                }
            } catch (e: Exception) {
                Log.e("FavoritesManagement", "Error adding favorite", e)
                showError("Failed to add favorite: ${e.message}")
            }
        }
    }
    
    private fun removeFavorite(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Remove from Favorites?")
            .setMessage("Remove ${contact.name} from favorites? Future calls will no longer be tracked automatically.")
            .setPositiveButton("Remove") { _, _ ->
                coroutineScope.launch {
                    try {
                        val success = ContactManager.toggleFavorite(this@FavoritesManagementActivity, contact.id)
                        if (success) {
                            contact.isFavorite = false
                            favoriteContacts.remove(contact)
                            filteredContacts.add(0, contact)
                            updateUI()
                            showSuccess("${contact.name} removed from favorites")
                        } else {
                            showError("Failed to remove ${contact.name} from favorites")
                        }
                    } catch (e: Exception) {
                        Log.e("FavoritesManagement", "Error removing favorite", e)
                        showError("Failed to remove favorite: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addAllFilteredToFavorites() {
        if (filteredContacts.isEmpty()) return
        
        AlertDialog.Builder(this)
            .setTitle("Add All to Favorites?")
            .setMessage("Add all ${filteredContacts.size} contacts to favorites? This will enable automatic call tracking for all of them.")
            .setPositiveButton("Add All") { _, _ ->
                coroutineScope.launch {
                    var successCount = 0
                    val contactsToAdd = filteredContacts.toList()
                    
                    for (contact in contactsToAdd) {
                        try {
                            val success = ContactManager.toggleFavorite(this@FavoritesManagementActivity, contact.id)
                            if (success) {
                                contact.isFavorite = true
                                favoriteContacts.add(contact)
                                filteredContacts.remove(contact)
                                successCount++
                            }
                        } catch (e: Exception) {
                            Log.e("FavoritesManagement", "Error adding ${contact.name} to favorites", e)
                        }
                    }
                    
                    updateUI()
                    showSuccess("Added $successCount contacts to favorites")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun removeAllFavorites() {
        if (favoriteContacts.isEmpty()) return
        
        AlertDialog.Builder(this)
            .setTitle("Remove All Favorites?")
            .setMessage("Remove all ${favoriteContacts.size} contacts from favorites? This will disable automatic call tracking for all of them.")
            .setPositiveButton("Remove All") { _, _ ->
                coroutineScope.launch {
                    var successCount = 0
                    val contactsToRemove = favoriteContacts.toList()
                    
                    for (contact in contactsToRemove) {
                        try {
                            val success = ContactManager.toggleFavorite(this@FavoritesManagementActivity, contact.id)
                            if (success) {
                                contact.isFavorite = false
                                favoriteContacts.remove(contact)
                                filteredContacts.add(0, contact)
                                successCount++
                            }
                        } catch (e: Exception) {
                            Log.e("FavoritesManagement", "Error removing ${contact.name} from favorites", e)
                        }
                    }
                    
                    updateUI()
                    showSuccess("Removed $successCount contacts from favorites")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun callContact(contact: Contact) {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = android.net.Uri.parse("tel:${contact.phoneNumber}")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("FavoritesManagement", "Error making call", e)
            showError("Failed to make call")
        }
    }
    
    private fun showFavoriteContactOptions(contact: Contact) {
        val options = arrayOf(
            "View Call History",
            "View Analytics",
            "Call Contact",
            "Remove from Favorites"
        )
        
        AlertDialog.Builder(this)
            .setTitle(contact.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, CallHistoryActivity::class.java)
                        intent.putExtra("contact_name", contact.name)
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(this, AnalyticsActivity::class.java)
                        intent.putExtra("contact_name", contact.name)
                        startActivity(intent)
                    }
                    2 -> callContact(contact)
                    3 -> removeFavorite(contact)
                }
            }
            .show()
    }
    
    private fun updateBulkSelectionUI(selectedCount: Int) {
        addAllButton.text = if (selectedCount > 0) {
            "Add Selected ($selectedCount)"
        } else {
            "Add All"
        }
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.favorites_management_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_import_favorites -> {
                // Future feature: import from other sources
                Toast.makeText(this, "Import feature coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_export_favorites -> {
                // Future feature: export favorites list
                Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadContacts() // Refresh data when returning to this activity
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
    
    // Inner class for swipe-to-remove functionality
    inner class FavoriteItemTouchCallback : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            
            // Reorder the list
            val contact = favoriteContacts.removeAt(fromPosition)
            favoriteContacts.add(toPosition, contact)
            favoritesManagementAdapter.notifyItemMoved(fromPosition, toPosition)
            
            return true
        }
        
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val contact = favoriteContacts[position]
            removeFavorite(contact)
        }
    }
}
