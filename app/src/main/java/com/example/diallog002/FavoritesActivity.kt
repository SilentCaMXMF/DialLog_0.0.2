package com.example.diallog002

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.util.*

class FavoritesActivity : AppCompatActivity() {
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchButton: Button
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var allContactsRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var allContactsAdapter: ContactsAdapter
    
    private var favoriteContacts = mutableListOf<Contact>()
    private var allContacts = mutableListOf<Contact>()
    private var filteredContacts = mutableListOf<Contact>()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        
        initializeViews()
        setupSearchFunctionality()
        loadContacts()
    }
    
    private fun initializeViews() {
        searchEditText = findViewById(R.id.search_edit_text)
        clearSearchButton = findViewById(R.id.clear_search_button)
        favoritesRecyclerView = findViewById(R.id.favorites_recycler)
        allContactsRecyclerView = findViewById(R.id.all_contacts_recycler)
        
        // Setup Favorites RecyclerView
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        favoritesAdapter = FavoritesAdapter(mutableListOf()) { contact ->
            removeFavorite(contact)
        }
        favoritesRecyclerView.adapter = favoritesAdapter
        Log.d("FavoritesActivity", "FavoritesAdapter initialized")
        
        // Setup All Contacts RecyclerView
        allContactsRecyclerView.layoutManager = LinearLayoutManager(this)
        allContactsAdapter = ContactsAdapter(
            contacts = filteredContacts,
            onContactClick = { contact ->
                addToFavorites(contact)
            },
            onFavoriteToggle = { contact ->
                if (contact.isFavorite) {
                    removeFavorite(contact)
                } else {
                    addToFavorites(contact)
                }
            }
        )
        allContactsRecyclerView.adapter = allContactsAdapter
        
        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
            filterContacts("")
        }
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
        allContactsAdapter.updateContacts(filteredContacts)
    }
    
    private fun loadContacts() {
        coroutineScope.launch {
            try {
                allContacts = ContactManager.loadContacts(this@FavoritesActivity).toMutableList()
                
                // Separate favorites from non-favorites
                favoriteContacts.clear()
                favoriteContacts.addAll(allContacts.filter { it.isFavorite })
                
                filteredContacts.clear()
                filteredContacts.addAll(allContacts.filter { !it.isFavorite })
                
                favoritesAdapter.updateFavorites(favoriteContacts)
                allContactsAdapter.updateContacts(filteredContacts)
                
                Log.d("FavoritesActivity", "Loaded ${allContacts.size} total contacts, ${favoriteContacts.size} favorites")
            } catch (e: Exception) {
                Log.e("FavoritesActivity", "Error loading contacts", e)
                showError("Failed to load contacts: ${e.message}")
            }
        }
    }
    
    private fun addToFavorites(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Add to Favorites")
            .setMessage("Add ${contact.name} to favorites? All calls with this contact will be automatically tracked and analyzed.")
            .setPositiveButton("Add") { _, _ ->
                coroutineScope.launch {
                    try {
                        val newFavoriteState = ContactManager.toggleFavorite(this@FavoritesActivity, contact.id)
                        if (newFavoriteState) {
                            contact.isFavorite = true
                            favoriteContacts.add(contact)
                            filteredContacts.remove(contact)
                            
                            favoritesAdapter.updateFavorites(favoriteContacts)
                            allContactsAdapter.updateContacts(filteredContacts)
                            
                            showSuccess("${contact.name} added to favorites")
                            Log.d("FavoritesActivity", "Added ${contact.name} to favorites for automatic call tracking")
                        } else {
                            showError("Failed to add ${contact.name} to favorites")
                        }
                    } catch (e: Exception) {
                        Log.e("FavoritesActivity", "Error adding favorite", e)
                        showError("Failed to add favorite: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun removeFavorite(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Remove from Favorites")
            .setMessage("Remove ${contact.name} from favorites? Future calls with this contact will no longer be tracked automatically. Past call history will be preserved.")
            .setPositiveButton("Remove") { _, _ ->
                coroutineScope.launch {
                    try {
                        val newFavoriteState = ContactManager.toggleFavorite(this@FavoritesActivity, contact.id)
                        if (!newFavoriteState) {
                            contact.isFavorite = false
                            favoriteContacts.remove(contact)
                            filteredContacts.add(0, contact) // Add to top of all contacts list
                            
                            favoritesAdapter.updateFavorites(favoriteContacts)
                            allContactsAdapter.updateContacts(filteredContacts)
                            
                            showSuccess("${contact.name} removed from favorites")
                            Log.d("FavoritesActivity", "Removed ${contact.name} from favorites, call tracking disabled")
                        } else {
                            showError("Failed to remove ${contact.name} from favorites")
                        }
                    } catch (e: Exception) {
                        Log.e("FavoritesActivity", "Error removing favorite", e)
                        showError("Failed to remove favorite: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("FavoritesActivity", "onResume: Refreshing contacts")
        loadContacts()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}

// Adapter for favorite contacts with remove functionality
class FavoritesAdapter(
    private var favorites: MutableList<Contact>,
    private val onRemoveClick: (Contact) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactNameText: TextView = itemView.findViewById(R.id.contact_name)
        val phoneNumberText: TextView = itemView.findViewById(R.id.phone_number)
        val removeButton: ImageButton = itemView.findViewById(R.id.remove_favorite_button)
        val trackingIndicator: TextView = itemView.findViewById(R.id.tracking_indicator)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_contact, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = favorites[position]
        
        Log.d("FavoritesAdapter", "onBindViewHolder: Binding contact at position $position: ${contact.name} (${contact.id})")
        
        holder.contactNameText.text = contact.name
        holder.phoneNumberText.text = contact.phoneNumber
        holder.trackingIndicator.text = "🎯 Auto-tracked"
        
        holder.removeButton.setOnClickListener {
            Log.d("FavoritesAdapter", "Remove button clicked for: ${contact.name}")
            onRemoveClick(contact)
        }
        
        // Set different background for favorites
        holder.itemView.setBackgroundResource(R.drawable.favorite_contact_background)
    }
    
    override fun getItemCount() = favorites.size
    
    fun updateFavorites(newFavorites: List<Contact>) {
        Log.d("FavoritesAdapter", "updateFavorites: Updating with ${newFavorites.size} favorites")
        newFavorites.forEach { contact ->
            Log.d("FavoritesAdapter", "updateFavorites: ${contact.name} (${contact.id}) - ${contact.phoneNumber}")
        }
        
        // Ensure UI updates happen on main thread
        Handler(Looper.getMainLooper()).post {
            favorites.clear()
            favorites.addAll(newFavorites)
            Log.d("FavoritesAdapter", "updateFavorites: favorites list size after update = ${favorites.size}")
            
            notifyDataSetChanged()
            
            Handler(Looper.getMainLooper()).post {
                Log.d("FavoritesAdapter", "updateFavorites: After notifyDataSetChanged, itemCount = ${itemCount}")
            }
        }
    }
}
