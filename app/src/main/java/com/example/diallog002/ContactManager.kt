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
        Log.d("ContactManager", "loadContacts: Starting to load all contacts")
        
        // Check permissions first
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("ContactManager", "loadContacts: READ_CONTACTS permission not granted")
            return emptyList()
        }
        
        val contacts = mutableListOf<Contact>()
        val contactsMap = mutableMapOf<String, Contact>() // To avoid duplicates
        val contentResolver: ContentResolver = context.contentResolver
        
        // Get favorite IDs first for reference
        val favoriteIds = getFavoriteContactIds(context)
        Log.d("ContactManager", "loadContacts: Will mark ${favoriteIds.size} contacts as favorites")
        
        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} IS NOT NULL",
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                var contactCount = 0
                var favoriteCount = 0
                val idColumnIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameColumnIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneColumnIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                if (idColumnIndex < 0 || nameColumnIndex < 0 || phoneColumnIndex < 0) {
                    Log.e("ContactManager", "loadContacts: Invalid column indices")
                    return emptyList()
                }
                
                while (it.moveToNext()) {
                    try {
                        val id = it.getString(idColumnIndex)
                        val name = it.getString(nameColumnIndex)
                        val phoneNumber = it.getString(phoneColumnIndex)
                        
                        // Skip contacts with null or empty data
                        if (id.isNullOrEmpty() || name.isNullOrEmpty() || phoneNumber.isNullOrEmpty()) {
                            continue
                        }
                        
                        // Use contact ID as unique key to avoid duplicates
                        val uniqueKey = "${id}_${phoneNumber.replace("\\s".toRegex(), "")}"
                        if (!contactsMap.containsKey(uniqueKey)) {
                            val isFavorite = favoriteIds.contains(id)
                            val contact = Contact(id, name, phoneNumber, isFavorite)
                            contactsMap[uniqueKey] = contact
                            
                            contactCount++
                            if (isFavorite) {
                                favoriteCount++
                                Log.d("ContactManager", "loadContacts: Found favorite contact: $name (ID: $id)")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ContactManager", "loadContacts: Error processing contact row", e)
                        continue
                    }
                }
                
                contacts.addAll(contactsMap.values)
                Log.d("ContactManager", "loadContacts: Processed $contactCount unique contacts, $favoriteCount marked as favorites")
            } ?: run {
                Log.e("ContactManager", "loadContacts: Cursor is null, no contacts found")
            }
        } catch (e: Exception) {
            Log.e("ContactManager", "loadContacts: Error querying contacts", e)
            return emptyList()
        }
        
        Log.d("ContactManager", "loadContacts: Returning ${contacts.size} contacts")
        return contacts.sortedBy { it.name.lowercase() }
    }
    
    fun toggleFavorite(context: Context, contactId: String): Boolean {
        val prefs = getSharedPreferences(context)
        val favoriteIds = getFavoriteContactIds(context).toMutableSet()
        
        Log.d("ContactManager", "toggleFavorite: Current favorite IDs before toggle: $favoriteIds")
        Log.d("ContactManager", "toggleFavorite: Toggling contact ID: $contactId")
        
        val newFavoriteState = if (favoriteIds.contains(contactId)) {
            favoriteIds.remove(contactId)
            false
        } else {
            favoriteIds.add(contactId)
            true
        }
        
        Log.d("ContactManager", "toggleFavorite: Favorite IDs after toggle: $favoriteIds")
        Log.d("ContactManager", "toggleFavorite: New favorite state: $newFavoriteState")
        
        // Save to SharedPreferences
        val editor = prefs.edit()
        editor.putStringSet(KEY_FAVORITES, favoriteIds)
        val saveResult = editor.commit() // Use commit() instead of apply() to get immediate result
        
        Log.d("ContactManager", "toggleFavorite: SharedPreferences save result: $saveResult")
        
        // Verify the save worked
        val verifyIds = getFavoriteContactIds(context)
        Log.d("ContactManager", "toggleFavorite: Verified favorite IDs after save: $verifyIds")
        
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
        val favoriteIds = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        Log.d("ContactManager", "getFavoriteContactIds: Retrieved ${favoriteIds.size} favorite IDs: $favoriteIds")
        return favoriteIds
    }
    
    fun clearAllFavorites(context: Context) {
        val prefs = getSharedPreferences(context)
        val editor = prefs.edit()
        editor.remove(KEY_FAVORITES)
        editor.apply()
        Log.d("ContactManager", "All favorites cleared")
    }
}
