package com.example.diallog002

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diallog002.data.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var timeRangeSpinner: Spinner
    private lateinit var contactSpinner: Spinner
    private lateinit var datePickerButton: Button
    private lateinit var generateAnalyticsButton: Button
    
    // Analytics display views
    private lateinit var analyticsContainer: LinearLayout
    private lateinit var summaryCard: LinearLayout
    private lateinit var totalCallsText: TextView
    private lateinit var totalDurationText: TextView
    private lateinit var talkListenRatioText: TextView
    private lateinit var averageCallText: TextView
    private lateinit var periodText: TextView
    
    // Detailed results
    private lateinit var contactAnalyticsRecycler: RecyclerView
    private lateinit var contactAnalyticsAdapter: ContactAnalyticsAdapter
    
    // Service and data
    private lateinit var analyticsService: CallAnalyticsService
    private var contactNames = mutableListOf<String>()
    private var selectedDate = Date()
    private var selectedTimeRange = TimeRange.DAY
    private var selectedContact: String? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)
        
        analyticsService = CallAnalyticsService(this)
        
        initializeViews()
        setupSpinners()
        loadContacts()
    }
    
    private fun initializeViews() {
        // Control views
        timeRangeSpinner = findViewById(R.id.time_range_spinner)
        contactSpinner = findViewById(R.id.contact_spinner)
        datePickerButton = findViewById(R.id.date_picker_button)
        generateAnalyticsButton = findViewById(R.id.generate_analytics_button)
        
        // Analytics display views
        analyticsContainer = findViewById(R.id.analytics_container)
        summaryCard = findViewById(R.id.summary_card)
        totalCallsText = findViewById(R.id.total_calls_text)
        totalDurationText = findViewById(R.id.total_duration_text)
        talkListenRatioText = findViewById(R.id.talk_listen_ratio_text)
        averageCallText = findViewById(R.id.average_call_text)
        periodText = findViewById(R.id.period_text)
        
        contactAnalyticsRecycler = findViewById(R.id.contact_analytics_recycler)
        
        // Setup RecyclerView for detailed analytics
        contactAnalyticsRecycler.layoutManager = LinearLayoutManager(this)
        contactAnalyticsAdapter = ContactAnalyticsAdapter(emptyList()) { result ->
            showDetailedAnalytics(result)
        }
        contactAnalyticsRecycler.adapter = contactAnalyticsAdapter
        
        // Setup click listeners
        datePickerButton.setOnClickListener { showDatePicker() }
        generateAnalyticsButton.setOnClickListener { generateAnalytics() }
        
        // Update date button text
        updateDateButtonText()
    }
    
    private fun setupSpinners() {
        // Time range spinner
        val timeRanges = arrayOf("Day", "Week", "Month", "Year", "All Time")
        val timeRangeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeRanges)
        timeRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeRangeSpinner.adapter = timeRangeAdapter
        
        timeRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTimeRange = when (position) {
                    0 -> TimeRange.DAY
                    1 -> TimeRange.WEEK
                    2 -> TimeRange.MONTH
                    3 -> TimeRange.YEAR
                    4 -> TimeRange.ALL_TIME
                    else -> TimeRange.DAY
                }
                
                // Hide date picker for ALL_TIME
                datePickerButton.visibility = if (selectedTimeRange == TimeRange.ALL_TIME) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    
    private fun loadContacts() {
        coroutineScope.launch {
            try {
                val callLogDao = AppDatabase.getInstance(this@AnalyticsActivity).callLogDao()
                val contactCounts = callLogDao.getContactCallCounts()
                contactNames.clear()
                contactNames.add("All Contacts")
                contactNames.addAll(contactCounts.map { it.contactName })
                
                val contactAdapter = ArrayAdapter(this@AnalyticsActivity, android.R.layout.simple_spinner_item, contactNames)
                contactAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                contactSpinner.adapter = contactAdapter
                
                contactSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedContact = if (position == 0) null else contactNames[position]
                    }
                    
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
                
                Log.d("AnalyticsActivity", "Loaded ${contactNames.size - 1} contacts for analytics")
            } catch (e: Exception) {
                Log.e("AnalyticsActivity", "Error loading contacts", e)
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { time = selectedDate }
        
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            updateDateButtonText()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            .show()
    }
    
    private fun updateDateButtonText() {
        datePickerButton.text = displayDateFormat.format(selectedDate)
    }
    
    private fun generateAnalytics() {
        coroutineScope.launch {
            try {
                showLoading(true)
                
                if (selectedContact != null) {
                    // Single contact analytics
                    val result = analyticsService.getContactAnalytics(
                        selectedContact!!, 
                        selectedTimeRange, 
                        selectedDate
                    )
                    
                    if (result != null) {
                        displaySingleContactAnalytics(result)
                    } else {
                        showNoDataMessage()
                    }
                } else {
                    // Global or all contacts analytics
                    val globalResult = analyticsService.getGlobalAnalytics(selectedTimeRange, selectedDate)
                    val allContactsResults = analyticsService.getAllContactsAnalytics(selectedTimeRange, selectedDate)
                    
                    displayGlobalAnalytics(globalResult, allContactsResults)
                }
                
                showLoading(false)
            } catch (e: Exception) {
                Log.e("AnalyticsActivity", "Error generating analytics", e)
                showLoading(false)
                showErrorMessage(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    private fun displaySingleContactAnalytics(result: AnalyticsResult) {
        analyticsContainer.visibility = View.VISIBLE
        
        // Update summary card
        periodText.text = "${result.contactName} - ${result.timeRange.name.lowercase().replaceFirstChar { it.uppercase() }} (${result.period})"
        totalCallsText.text = "${result.totalCalls} calls"
        totalDurationText.text = result.getTotalDurationFormatted()
        talkListenRatioText.text = result.getTalkListenRatioFormatted()
        averageCallText.text = "Avg: ${result.getAverageCallDurationFormatted()}"
        
        // Show individual call logs in adapter
        contactAnalyticsAdapter.updateResults(listOf(result))
        
        // Add communication style analysis if we have data
        if (result.totalCalls > 0) {
            showCommunicationStyleOption(result)
        }
    }
    
    private fun displayGlobalAnalytics(globalResult: AnalyticsResult?, contactResults: List<AnalyticsResult>) {
        analyticsContainer.visibility = View.VISIBLE
        
        if (globalResult != null) {
            // Update summary card with global stats
            periodText.text = "All Contacts - ${globalResult.timeRange.name.lowercase().replaceFirstChar { it.uppercase() }} (${globalResult.period})"
            totalCallsText.text = "${globalResult.totalCalls} calls"
            totalDurationText.text = globalResult.getTotalDurationFormatted()
            talkListenRatioText.text = globalResult.getTalkListenRatioFormatted()
            averageCallText.text = "Avg: ${globalResult.getAverageCallDurationFormatted()}"
            
            // Show per-contact breakdown
            contactAnalyticsAdapter.updateResults(contactResults.sortedByDescending { it.totalCalls })
        } else {
            showNoDataMessage()
        }
    }
    
    private fun showDetailedAnalytics(result: AnalyticsResult) {
        // Show detailed analytics in a dialog or expand view
        val details = buildString {
            append("${result.contactName ?: "All Contacts"}\n")
            append("Period: ${result.period}\n")
            append("Total Calls: ${result.totalCalls}\n")
            append("Speaking Time: ${result.getTotalSpeakingTimeFormatted()}\n")
            append("Listening Time: ${result.getTotalListeningTimeFormatted()}\n")
            append("Total Duration: ${result.getTotalDurationFormatted()}\n")
            append("Talk/Listen Ratio: ${result.getTalkListenRatioFormatted()}\n")
            append("Average Call: ${result.getAverageCallDurationFormatted()}")
        }
        
        Toast.makeText(this, details, Toast.LENGTH_LONG).show()
    }
    
    private fun showLoading(show: Boolean) {
        generateAnalyticsButton.isEnabled = !show
        generateAnalyticsButton.text = if (show) "Analyzing..." else "Generate Analytics"
    }
    
    private fun showNoDataMessage() {
        analyticsContainer.visibility = View.VISIBLE
        periodText.text = "No data found for the selected criteria"
        totalCallsText.text = "0 calls"
        totalDurationText.text = "0s"
        talkListenRatioText.text = "No data"
        averageCallText.text = "No data"
        contactAnalyticsAdapter.updateResults(emptyList())
    }
    
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
    }
    
    private fun showCommunicationStyleOption(result: AnalyticsResult) {
        // Create a button or show a dialog to analyze communication style
        val style = CommunicationStyleEvaluator.evaluateStyle(result.averageTalkRatio)
        
        Toast.makeText(this, 
            "${style.emoji} Communication Style: ${style.category}\nTap to learn more!", 
            Toast.LENGTH_LONG).show()
        
        // After 2 seconds, show option to view detailed analysis
        Handler(Looper.getMainLooper()).postDelayed({
            android.app.AlertDialog.Builder(this)
                .setTitle("${style.emoji} ${style.category} Style Detected")
                .setMessage("Based on your ${String.format("%.1f", result.averageTalkRatio)}% talking ratio, you have a ${style.category.lowercase()} communication style.\n\nWould you like to see a detailed analysis?")
                .setPositiveButton("View Analysis") { _, _ ->
                    val intent = Intent(this, CommunicationStyleActivity::class.java)
                    intent.putExtra("talk_percentage", result.averageTalkRatio)
                    intent.putExtra("contact_name", result.contactName)
                    startActivity(intent)
                }
                .setNegativeButton("Maybe Later", null)
                .show()
        }, 2000)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}

// Adapter for displaying contact analytics results
class ContactAnalyticsAdapter(
    private var results: List<AnalyticsResult>,
    private val onItemClick: (AnalyticsResult) -> Unit
) : RecyclerView.Adapter<ContactAnalyticsAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactNameText: TextView = itemView.findViewById(R.id.contact_name_text)
        val callCountText: TextView = itemView.findViewById(R.id.call_count_text)
        val durationText: TextView = itemView.findViewById(R.id.duration_text)
        val ratioText: TextView = itemView.findViewById(R.id.ratio_text)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_analytics, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        
        holder.contactNameText.text = result.contactName ?: "All Contacts"
        holder.callCountText.text = "${result.totalCalls} calls"
        holder.durationText.text = result.getTotalDurationFormatted()
        holder.ratioText.text = result.getTalkListenRatioFormatted()
        
        holder.itemView.setOnClickListener {
            onItemClick(result)
        }
    }
    
    override fun getItemCount() = results.size
    
    fun updateResults(newResults: List<AnalyticsResult>) {
        results = newResults
        notifyDataSetChanged()
    }
}
