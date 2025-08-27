package com.example.diallog002

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log

object ContactManager {
    private const val PREFS_NAME = "ContactFavorites"
    private const val KEY_FAVORITES = "favorite_contact_ids"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun loadContacts(context: Context): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver
        
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(0)
                val name = it.getString(1)
                val phoneNumber = it.getString(2)
                
                val isFavorite = isFavorite(context, id)
                contacts.add(Contact(id, name, phoneNumber, isFavorite))
            }
        }
        
        return contacts
    }
    
    fun toggleFavorite(context: Context, contactId: String): Boolean {
        val prefs = getSharedPreferences(context)
        val favoriteIds = getFavoriteContactIds(context).toMutableSet()
        
        val newFavoriteState = if (favoriteIds.contains(contactId)) {
            favoriteIds.remove(contactId)
            false
        } else {
            favoriteIds.add(contactId)
            true
        }
        
        // Save to SharedPreferences
        val editor = prefs.edit()
        editor.putStringSet(KEY_FAVORITES, favoriteIds)
        editor.apply()
        
        Log.d("ContactManager", "Favorite toggled for contact $contactId: $newFavoriteState")
        return newFavoriteState
    }
    
    fun isFavorite(context: Context, contactId: String): Boolean {
        return getFavoriteContactIds(context).contains(contactId)
    }
    
    fun getFavoriteContacts(context: Context): List<Contact> {
        val favoriteIds = getFavoriteContactIds(context)
        val allContacts = loadContacts(context)
        return allContacts.filter { it.id in favoriteIds }
    }
    
    fun getFavoriteContactIds(context: Context): Set<String> {
        val prefs = getSharedPreferences(context)
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }
    
    fun clearAllFavorites(context: Context) {
        val prefs = getSharedPreferences(context)
        val editor = prefs.edit()
        editor.remove(KEY_FAVORITES)
        editor.apply()
        Log.d("ContactManager", "All favorites cleared")
    }
}
