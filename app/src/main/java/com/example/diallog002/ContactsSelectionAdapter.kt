package com.example.diallog002

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactsSelectionAdapter(
    initialContacts: MutableList<Contact>,
    private val onContactClick: (Contact) -> Unit,
    private val onBulkSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<ContactsSelectionAdapter.ViewHolder>() {
    
    // Create our own copy of the contacts list to avoid reference issues
    private var contacts: MutableList<Contact> = initialContacts.toMutableList()
    
    private var selectedContacts = mutableSetOf<Contact>()
    private var isBulkSelectionMode = false
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contact_name)
        val phoneNumber: TextView = itemView.findViewById(R.id.phone_number)
        val addButton: ImageButton = itemView.findViewById(R.id.add_button)
        val selectionCheckbox: CheckBox = itemView.findViewById(R.id.selection_checkbox)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_selection, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        
        holder.contactName.text = contact.name
        holder.phoneNumber.text = contact.phoneNumber
        
        // Handle selection mode
        if (isBulkSelectionMode) {
            holder.selectionCheckbox.visibility = View.VISIBLE
            holder.addButton.visibility = View.GONE
            holder.selectionCheckbox.isChecked = selectedContacts.contains(contact)
        } else {
            holder.selectionCheckbox.visibility = View.GONE
            holder.addButton.visibility = View.VISIBLE
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener {
            if (isBulkSelectionMode) {
                toggleSelection(contact, holder.selectionCheckbox)
            } else {
                onContactClick(contact)
            }
        }
        
        holder.addButton.setOnClickListener { onContactClick(contact) }
        
        holder.selectionCheckbox.setOnClickListener {
            toggleSelection(contact, holder.selectionCheckbox)
        }
        
        // Long click to enter bulk selection mode
        holder.itemView.setOnLongClickListener {
            if (!isBulkSelectionMode) {
                enableBulkSelectionMode()
                toggleSelection(contact, holder.selectionCheckbox)
            }
            true
        }
    }
    
    private fun toggleSelection(contact: Contact, checkbox: CheckBox) {
        if (selectedContacts.contains(contact)) {
            selectedContacts.remove(contact)
            checkbox.isChecked = false
        } else {
            selectedContacts.add(contact)
            checkbox.isChecked = true
        }
        
        onBulkSelectionChanged(selectedContacts.size)
        
        // Exit bulk selection mode if no items selected
        if (selectedContacts.isEmpty()) {
            disableBulkSelectionMode()
        }
    }
    
    private fun enableBulkSelectionMode() {
        isBulkSelectionMode = true
        notifyDataSetChanged()
    }
    
    private fun disableBulkSelectionMode() {
        isBulkSelectionMode = false
        selectedContacts.clear()
        notifyDataSetChanged()
        onBulkSelectionChanged(0)
    }
    
    override fun getItemCount(): Int = contacts.size
    
    fun updateContacts(newContacts: List<Contact>) {
        contacts.clear()
        contacts.addAll(newContacts)
        
        // Clear selections when contacts change
        selectedContacts.clear()
        if (isBulkSelectionMode) {
            disableBulkSelectionMode()
        }
        
        notifyDataSetChanged()
    }
    
    fun getSelectedContacts(): List<Contact> = selectedContacts.toList()
    
    fun clearSelections() {
        selectedContacts.clear()
        disableBulkSelectionMode()
    }
    
    fun selectAll() {
        selectedContacts.clear()
        selectedContacts.addAll(contacts)
        isBulkSelectionMode = true
        notifyDataSetChanged()
        onBulkSelectionChanged(selectedContacts.size)
    }
}
