package com.example.diallog002

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diallog002.ui.theme.CallLoggingService
import java.io.File

data class Contact(val name: String, val phoneNumber: String)

class MainActivity : AppCompatActivity() {

    private lateinit var logAdapter: LogAdapter
    private lateinit var recyclerViewLogs: RecyclerView
    private lateinit var recyclerViewContacts: RecyclerView
    private var contactLogs: MutableMap<Contact, MutableList<LogEntry>> = mutableMapOf()
    private var selectedContact: Contact? = null
    private var isLogging = false
    private lateinit var contactsAdapter: ContactsAdapter // Fixed declaration type


    private val requestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerViewLogs = findViewById(R.id.recyclerViewLogs)
        recyclerViewLogs.layoutManager = LinearLayoutManager(this)
        logAdapter = LogAdapter(mutableListOf(), this)
        recyclerViewLogs.adapter = logAdapter

        recyclerViewContacts = findViewById(R.id.recyclerViewContacts)
        recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        contactsAdapter = ContactsAdapter(mutableListOf(), this::selectContact) // Initialize properly
        recyclerViewContacts.adapter = contactsAdapter

        loadContacts() // Load contacts here

        val buttonStartLogging: Button = findViewById(R.id.buttonStartLogging)
        buttonStartLogging.setOnClickListener {
            if (!isLogging) {
                if (checkPermissions()) {
                    startLogging()
                } else {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show()
                }
            } else {
                stopLogging()
            }
            isLogging = !isLogging
            updateLoggingButton()
        }

        findViewById<Button>(R.id.buttonRefresh).setOnClickListener {
            loadLogs()
            contactsAdapter.updateContacts(contactLogs.keys.toList())
        }

        findViewById<Button>(R.id.buttonCheckLastLog).setOnClickListener {
            showLastLogEntry()
        }

        requestPermissionsIfNeeded()
    }

    private fun loadContacts() {
        val contactsList = mutableListOf<Contact>()
        val contentResolver = contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                contactsList.add(Contact(name, phoneNumber))
            }
        }

        contactsAdapter.updateContacts(contactsList)
    }

    private fun showLastLogEntry() {
        selectedContact?.let { contact ->
            val logs = contactLogs[contact]
            if (!logs.isNullOrEmpty()) {
                val lastLogEntry = logs.last()
                val message = getString(
                    R.string.log_entry_format,
                    lastLogEntry.phoneNumber,
                    lastLogEntry.callDuration,
                    lastLogEntry.listeningTime,
                    lastLogEntry.speakingTime
                )
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, R.string.no_logs_for_contact, Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, R.string.no_contact_selected, Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )
        return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestPermissionsIfNeeded() {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, requiredPermissions, requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted, proceed with logging or load contacts
                loadContacts()
            } else {
                Toast.makeText(this, R.string.permissions_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLoggingButton() {
        val buttonStartLogging: Button = findViewById(R.id.buttonStartLogging)
        buttonStartLogging.text = if (isLogging) getString(R.string.stop_logging) else getString(R.string.start_logging)
    }

    private fun loadLogs() {
        val logsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (logsDir == null || !logsDir.exists()) {
            Toast.makeText(this, R.string.logs_dir_not_found, Toast.LENGTH_SHORT).show()
            return
        }

        contactLogs.clear()

        logsDir.listFiles()?.forEach { file ->
            if (file.extension == "txt") {
                val logEntry = parseLogFile(file)
                val contact = Contact(logEntry.phoneNumber, logEntry.phoneNumber) // Replace with actual contact matching logic
                if (!contactLogs.containsKey(contact)) {
                    contactLogs[contact] = mutableListOf()
                }
                contactLogs[contact]?.add(logEntry)
            }
        }

        selectedContact?.let {
            logAdapter.updateLogs(contactLogs[it] ?: mutableListOf())
        }
    }

    private fun selectContact(contact: Contact) {
        selectedContact = contact
        logAdapter.updateLogs(contactLogs[contact] ?: mutableListOf())
    }

    private fun startLogging() {
        val intent = Intent(this, CallLoggingService::class.java)
        startService(intent)
    }

    private fun stopLogging() {
        val intent = Intent(this, CallLoggingService::class.java)
        stopService(intent)
    }

    private fun parseLogFile(file: File): LogEntry {
        var phoneNumber = "Unknown"
        var callDuration: Long = 0
        var listeningTime: Long = 0
        var speakingTime: Long = 0

        try {
            file.forEachLine { line ->
                when {
                    line.startsWith("PhoneNumber:") -> {
                        phoneNumber = line.substringAfter("PhoneNumber:").trim()
                    }
                    line.startsWith("CallDuration:") -> {
                        callDuration = line.substringAfter("CallDuration:").trim().toLong()
                    }
                    line.startsWith("ListeningTime:") -> {
                        listeningTime = line.substringAfter("ListeningTime:").trim().toLong()
                    }
                    line.startsWith("SpeakingTime:") -> {
                        speakingTime = line.substringAfter("SpeakingTime:").trim().toLong()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle parsing exception, maybe use default values or notify user
        }

        return LogEntry(phoneNumber, callDuration, listeningTime, speakingTime)
    }
}
