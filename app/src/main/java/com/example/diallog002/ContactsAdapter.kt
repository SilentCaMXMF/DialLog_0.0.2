package com.example.diallog002

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(
    private var contacts: List<Contact>,
    private val onContactClick: (Contact) -> Unit,
    private val onFavoriteToggle: (Contact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.contact_name)
        val phoneTextView: TextView = itemView.findViewById(R.id.contact_phone)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        
        holder.nameTextView.text = contact.name
        holder.phoneTextView.text = contact.phoneNumber
        
        // Set favorite icon
        holder.favoriteIcon.setImageResource(
            if (contact.isFavorite) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
        
        holder.itemView.setOnClickListener {
            onContactClick(contact)
        }
        
        holder.favoriteIcon.setOnClickListener {
            onFavoriteToggle(contact)
        }
    }

    override fun getItemCount() = contacts.size

    fun updateContacts(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}
