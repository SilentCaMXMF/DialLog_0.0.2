package com.example.diallog002

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Dedicated adapter for MainActivity favorites display
class MainFavoritesAdapter(
    private val onFavoriteClick: (Contact) -> Unit,
    private val onCallClick: (Contact) -> Unit
) : RecyclerView.Adapter<MainFavoritesAdapter.ViewHolder>() {
    
    private var favorites: MutableList<Contact> = mutableListOf()
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactNameText: TextView = itemView.findViewById(R.id.contact_name)
        val phoneNumberText: TextView = itemView.findViewById(R.id.phone_number)
        val trackingIndicator: TextView = itemView.findViewById(R.id.tracking_indicator)
        val removeButton: ImageButton = itemView.findViewById(R.id.remove_favorite_button)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("MainFavoritesAdapter", "onCreateViewHolder: Creating view holder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_contact, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = favorites[position]
        
        Log.d("MainFavoritesAdapter", "onBindViewHolder: Binding contact at position $position: ${contact.name} (${contact.id})")
        
        holder.contactNameText.text = contact.name
        holder.phoneNumberText.text = contact.phoneNumber
        holder.trackingIndicator.text = "🎯 Tap to call • Long press for options"
        
        // Handle click on the whole item - direct call
        holder.itemView.setOnClickListener {
            Log.d("MainFavoritesAdapter", "Contact item clicked for CALLING: ${contact.name}")
            onCallClick(contact)
        }
        
        // Handle long click on the whole item - show options
        holder.itemView.setOnLongClickListener {
            Log.d("MainFavoritesAdapter", "Contact item long-clicked for OPTIONS: ${contact.name}")
            onFavoriteClick(contact)
            true
        }
        
        // Handle remove button click - show options
        holder.removeButton.setOnClickListener {
            Log.d("MainFavoritesAdapter", "Remove button clicked for: ${contact.name}")
            onFavoriteClick(contact)
        }
        
        // Set different background for favorites
        holder.itemView.setBackgroundResource(R.drawable.favorite_contact_background)
    }
    
    override fun getItemCount(): Int {
        Log.d("MainFavoritesAdapter", "getItemCount: Returning ${favorites.size} items")
        return favorites.size
    }
    
    fun updateFavorites(newFavorites: List<Contact>) {
        Log.d("MainFavoritesAdapter", "updateFavorites: Updating with ${newFavorites.size} favorites")
        newFavorites.forEach { contact ->
            Log.d("MainFavoritesAdapter", "updateFavorites: ${contact.name} (${contact.id}) - ${contact.phoneNumber}")
        }
        
        // Clear and update on main thread
        Handler(Looper.getMainLooper()).post {
            favorites.clear()
            favorites.addAll(newFavorites)
            
            Log.d("MainFavoritesAdapter", "updateFavorites: favorites list size after update = ${favorites.size}")
            Log.d("MainFavoritesAdapter", "updateFavorites: About to call notifyDataSetChanged")
            
            notifyDataSetChanged()
            
            // Verify the update worked
            Handler(Looper.getMainLooper()).post {
                Log.d("MainFavoritesAdapter", "updateFavorites: After notifyDataSetChanged, itemCount = ${itemCount}")
                Log.d("MainFavoritesAdapter", "updateFavorites: favorites.size = ${favorites.size}")
            }
        }
    }
}
