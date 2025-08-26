package com.example.diallog002

import android.content.ContentResolver
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class ContactSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_selection)

        val contactListView = findViewById<ListView>(R.id.contact_list_view)
        val contacts = getContactList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, contacts)
        contactListView.adapter = adapter

        // Handle contact selection
        contactListView.setOnItemClickListener { _, _, position, _ ->
            val selectedContact = contacts[position]
            // Pass the selected contact to the next activity
            // Start tracking logic here
        }
    }

    private fun getContactList(): List<String> {
        val contacts = mutableListOf<String>()
        val resolver: ContentResolver = contentResolver
        val cursor: Cursor? = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                contacts.add(name)
            }
        }
        return contacts
    }
}