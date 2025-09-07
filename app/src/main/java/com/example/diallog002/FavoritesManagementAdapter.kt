package com.example.diallog002

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoritesManagementAdapter(
    initialContacts: MutableList<Contact>,
    private val onContactClick: (Contact) -> Unit,
    private val onCallClick: (Contact) -> Unit,
    private val onRemoveClick: (Contact) -> Unit
) : RecyclerView.Adapter<FavoritesManagementAdapter.ViewHolder>() {
    
    // Create our own copy of the contacts list to avoid reference issues
    private var contacts: MutableList<Contact> = initialContacts.toMutableList()
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contact_name)
        val phoneNumber: TextView = itemView.findViewById(R.id.phone_number)
        val callButton: ImageButton = itemView.findViewById(R.id.call_button)
        val removeButton: ImageButton = itemView.findViewById(R.id.remove_button)
        val dragHandle: View = itemView.findViewById(R.id.drag_handle)
        val statusIndicator: TextView = itemView.findViewById(R.id.status_indicator)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_management, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        
        holder.contactName.text = contact.name
        holder.phoneNumber.text = contact.phoneNumber
        holder.statusIndicator.text = "🎯 Auto-tracked"
        
        // Set click listeners
        holder.itemView.setOnClickListener { onContactClick(contact) }
        holder.callButton.setOnClickListener { onCallClick(contact) }
        holder.removeButton.setOnClickListener { onRemoveClick(contact) }
        
        // Add visual feedback for different states
        holder.itemView.setBackgroundResource(R.drawable.favorite_management_background)
    }
    
    override fun getItemCount(): Int = contacts.size
    
    fun updateContacts(newContacts: List<Contact>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }
    
    fun moveItem(fromPosition: Int, toPosition: Int) {
        val contact = contacts.removeAt(fromPosition)
        contacts.add(toPosition, contact)
        notifyItemMoved(fromPosition, toPosition)
    }
    
    fun removeItem(position: Int) {
        contacts.removeAt(position)
        notifyItemRemoved(position)
    }
}
