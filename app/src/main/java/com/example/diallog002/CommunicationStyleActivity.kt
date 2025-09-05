package com.example.diallog002

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CommunicationStyleActivity : AppCompatActivity() {
    
    private lateinit var styleGridRecycler: RecyclerView
    private lateinit var styleGridAdapter: CommunicationStyleAdapter
    private lateinit var headerText: TextView
    private lateinit var explanationText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication_style)
        
        initializeViews()
        setupRecyclerView()
        
        // Check if we received a specific ratio to highlight
        val userTalkPercentage = intent.getDoubleExtra("talk_percentage", -1.0)
        val contactName = intent.getStringExtra("contact_name")
        
        if (userTalkPercentage >= 0) {
            highlightUserStyle(userTalkPercentage, contactName)
        }
    }
    
    private fun initializeViews() {
        styleGridRecycler = findViewById(R.id.style_grid_recycler)
        headerText = findViewById(R.id.header_text)
        explanationText = findViewById(R.id.explanation_text)
    }
    
    private fun setupRecyclerView() {
        styleGridRecycler.layoutManager = GridLayoutManager(this, 2) // 2 columns
        
        val styles = CommunicationStyleEvaluator.getAllStyles()
        styleGridAdapter = CommunicationStyleAdapter(styles) { style ->
            showStyleDetails(style)
        }
        styleGridRecycler.adapter = styleGridAdapter
    }
    
    private fun highlightUserStyle(talkPercentage: Double, contactName: String?) {
        val userStyle = CommunicationStyleEvaluator.evaluateStyle(talkPercentage)
        val contact = contactName ?: "Your"
        
        headerText.text = "$contact Communication Style: ${userStyle.emoji} ${userStyle.category}"
        explanationText.text = "Based on ${String.format("%.1f", talkPercentage)}% talking time\\n\\n${userStyle.description}\\n\\n💡 ${userStyle.advice}"
        
        // Update adapter to highlight user's style
        styleGridAdapter.highlightStyle(userStyle.category)
    }
    
    private fun showStyleDetails(style: CommunicationStyle) {
        val details = buildString {
            append("${style.emoji} ${style.category}\\n\\n")
            append("${style.description}\\n\\n")
            append("📊 Range: ${CommunicationStyleEvaluator.getStyleRange(style.category)}\\n\\n")
            append("🎯 Characteristics:\\n")
            style.characteristics.forEach { characteristic ->
                append("• $characteristic\\n")
            }
            append("\\n💡 Advice:\\n${style.advice}")
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle("${style.emoji} ${style.category}")
            .setMessage(details)
            .setPositiveButton("Got it", null)
            .show()
    }
}

// Adapter for the communication styles grid
class CommunicationStyleAdapter(
    private val styles: List<CommunicationStyle>,
    private val onStyleClick: (CommunicationStyle) -> Unit
) : RecyclerView.Adapter<CommunicationStyleAdapter.ViewHolder>() {
    
    private var highlightedCategory: String? = null
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiText: TextView = itemView.findViewById(R.id.style_emoji)
        val categoryText: TextView = itemView.findViewById(R.id.style_category)
        val rangeText: TextView = itemView.findViewById(R.id.style_range)
        val descriptionText: TextView = itemView.findViewById(R.id.style_description)
        val cardView: View = itemView.findViewById(R.id.style_card)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_communication_style, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val style = styles[position]
        
        holder.emojiText.text = style.emoji
        holder.categoryText.text = style.category
        holder.rangeText.text = CommunicationStyleEvaluator.getStyleRange(style.category)
        holder.descriptionText.text = style.description
        
        // Set background color based on style
        try {
            holder.cardView.setBackgroundColor(Color.parseColor(style.color + "20")) // 20% opacity
        } catch (e: Exception) {
            // Fallback to default color if parsing fails
            holder.cardView.setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        
        // Highlight user's current style
        if (style.category == highlightedCategory) {
            holder.cardView.setBackgroundColor(Color.parseColor(style.color + "40")) // 40% opacity for highlight
            holder.categoryText.textSize = 16f
            holder.categoryText.setTextColor(Color.parseColor(style.color))
        } else {
            holder.categoryText.textSize = 14f
            holder.categoryText.setTextColor(Color.parseColor("#212121"))
        }
        
        holder.itemView.setOnClickListener {
            onStyleClick(style)
        }
    }
    
    override fun getItemCount() = styles.size
    
    fun highlightStyle(category: String) {
        highlightedCategory = category
        notifyDataSetChanged()
    }
}
