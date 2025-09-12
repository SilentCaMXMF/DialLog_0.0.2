package com.example.diallog002

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoriteNumbersAdapter(
    private var favoriteNumbers: MutableList<FavoriteNumber>,
    private val onFavoriteClick: (FavoriteNumber) -> Unit,
    private val onCallClick: (FavoriteNumber) -> Unit,
    private val onRemoveClick: (FavoriteNumber) -> Unit
) : RecyclerView.Adapter<FavoriteNumbersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contact_name)
        val phoneNumber: TextView = itemView.findViewById(R.id.phone_number)
        val callButton: ImageButton = itemView.findViewById(R.id.call_button)
        val removeButton: ImageButton = itemView.findViewById(R.id.remove_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_number, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favoriteNumber = favoriteNumbers[position]
        
        holder.contactName.text = favoriteNumber.contactName
        holder.phoneNumber.text = favoriteNumber.phoneNumber
        
        // Set click listeners
        holder.itemView.setOnClickListener {
            onFavoriteClick(favoriteNumber)
        }
        
        holder.callButton.setOnClickListener {
            onCallClick(favoriteNumber)
        }
        
        holder.removeButton.setOnClickListener {
            onRemoveClick(favoriteNumber)
        }
    }

    override fun getItemCount(): Int = favoriteNumbers.size

    fun updateFavoriteNumbers(newFavoriteNumbers: List<FavoriteNumber>) {
        favoriteNumbers.clear()
        favoriteNumbers.addAll(newFavoriteNumbers)
        notifyDataSetChanged()
    }
}