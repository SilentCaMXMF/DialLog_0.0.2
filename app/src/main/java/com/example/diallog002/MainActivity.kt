package com.example.diallog002

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var startCallButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        startCallButton = findViewById(R.id.fabStartCall)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ContactsAdapter(mutableListOf()) // Empty list for now
        recyclerView.adapter = adapter

        // Set up FAB click listener
        startCallButton.setOnClickListener {
            // TODO: Implement call tracking functionality
        }
    }
}
