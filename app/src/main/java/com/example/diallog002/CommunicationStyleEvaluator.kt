package com.example.diallog002

data class CommunicationStyle(
    val category: String,
    val description: String,
    val characteristics: List<String>,
    val advice: String,
    val color: String,
    val emoji: String
)

object CommunicationStyleEvaluator {
    
    /**
     * Evaluates communication style based on talk/listen ratio
     * @param talkPercentage The percentage of time spent talking (0-100)
     * @return CommunicationStyle object with analysis
     */
    fun evaluateStyle(talkPercentage: Double): CommunicationStyle {
        return when {
            talkPercentage >= 80 -> CommunicationStyle(
                category = "Highly Impositive",
                description = "Extremely dominant communication pattern",
                characteristics = listOf(
                    "Dominates conversations",
                    "Rarely lets others speak",
                    "May appear overwhelming",
                    "Strong presence but poor listening"
                ),
                advice = "Practice active listening. Pause to ask questions and wait for responses.",
                color = "#D32F2F", // Dark Red
                emoji = "🗣️"
            )
            
            talkPercentage >= 70 -> CommunicationStyle(
                category = "Impositive",
                description = "Dominant and controlling communication",
                characteristics = listOf(
                    "Controls conversation flow",
                    "Confident speaker",
                    "May interrupt others",
                    "Strong opinions, limited input seeking"
                ),
                advice = "Balance talking with listening. Ask open-ended questions more often.",
                color = "#F44336", // Red
                emoji = "📢"
            )
            
            talkPercentage >= 60 -> CommunicationStyle(
                category = "Assertive",
                description = "Confident and balanced communication",
                characteristics = listOf(
                    "Clear communication",
                    "Confident in expressing views",
                    "Good leadership qualities",
                    "Respects others' input"
                ),
                advice = "Great balance! Continue being confident while staying open to others.",
                color = "#FF9800", // Orange
                emoji = "💪"
            )
            
            talkPercentage >= 50 -> CommunicationStyle(
                category = "Balanced",
                description = "Ideal communication equilibrium",
                characteristics = listOf(
                    "Equal give-and-take",
                    "Good listener and speaker",
                    "Collaborative approach",
                    "Builds rapport effectively"
                ),
                advice = "Perfect balance! You're an excellent communicator. Keep it up!",
                color = "#4CAF50", // Green
                emoji = "⚖️"
            )
            
            talkPercentage >= 40 -> CommunicationStyle(
                category = "Listening-Focused",
                description = "Thoughtful and considerate communication",
                characteristics = listOf(
                    "Excellent listener",
                    "Thoughtful responses",
                    "Values others' opinions",
                    "Supportive communicator"
                ),
                advice = "Great listening skills! Don't be afraid to share your thoughts more often.",
                color = "#2196F3", // Blue
                emoji = "👂"
            )
            
            talkPercentage >= 30 -> CommunicationStyle(
                category = "Passive",
                description = "Reserved and cautious communication",
                characteristics = listOf(
                    "Prefers listening to speaking",
                    "Cautious with opinions",
                    "Values harmony",
                    "May miss opportunities to contribute"
                ),
                advice = "Your listening skills are valuable. Try to speak up more and share your insights.",
                color = "#9C27B0", // Purple
                emoji = "🤐"
            )
            
            talkPercentage >= 20 -> CommunicationStyle(
                category = "Highly Passive",
                description = "Very reserved communication pattern",
                characteristics = listOf(
                    "Minimal verbal participation",
                    "Avoids conflict",
                    "May seem disengaged",
                    "Others may not know your thoughts"
                ),
                advice = "Practice expressing yourself more. Your voice matters and others want to hear it.",
                color = "#673AB7", // Deep Purple
                emoji = "😶"
            )
            
            else -> CommunicationStyle(
                category = "Silent Observer",
                description = "Extremely quiet communication pattern",
                characteristics = listOf(
                    "Almost entirely listening",
                    "May appear withdrawn",
                    "Deep processing style",
                    "Others may misinterpret silence"
                ),
                advice = "Consider that silence can be misunderstood. Try to engage verbally more often.",
                color = "#424242", // Dark Gray
                emoji = "🤫"
            )
        }
    }
    
    /**
     * Get all communication styles for display in a grid
     */
    fun getAllStyles(): List<CommunicationStyle> {
        return listOf(
            evaluateStyle(85.0), // Highly Impositive
            evaluateStyle(75.0), // Impositive
            evaluateStyle(65.0), // Assertive
            evaluateStyle(55.0), // Balanced
            evaluateStyle(45.0), // Listening-Focused
            evaluateStyle(35.0), // Passive
            evaluateStyle(25.0), // Highly Passive
            evaluateStyle(15.0)  // Silent Observer
        )
    }
    
    /**
     * Get style category based on percentage ranges
     */
    fun getStyleRange(category: String): String {
        return when (category) {
            "Highly Impositive" -> "80-100% talking"
            "Impositive" -> "70-79% talking"
            "Assertive" -> "60-69% talking"
            "Balanced" -> "50-59% talking"
            "Listening-Focused" -> "40-49% talking"
            "Passive" -> "30-39% talking"
            "Highly Passive" -> "20-29% talking"
            "Silent Observer" -> "0-19% talking"
            else -> "Unknown range"
        }
    }
    
    /**
     * Get contextual advice for improving communication
     */
    fun getContextualAdvice(talkPercentage: Double, context: String = "general"): String {
        val baseStyle = evaluateStyle(talkPercentage)
        
        return when (context) {
            "professional" -> when {
                talkPercentage >= 70 -> "In professional settings, ensure you're allowing space for team input and feedback."
                talkPercentage <= 30 -> "In meetings, make sure to contribute your expertise and ideas more actively."
                else -> baseStyle.advice
            }
            "personal" -> when {
                talkPercentage >= 70 -> "In relationships, remember that listening builds deeper connections."
                talkPercentage <= 30 -> "Share your feelings and thoughts more openly with loved ones."
                else -> baseStyle.advice
            }
            "sales" -> when {
                talkPercentage >= 60 -> "Great for presentations! Remember the 80/20 rule: let customers talk 80% of the time."
                talkPercentage <= 40 -> "You're an excellent listener! Now practice confidently presenting your solutions."
                else -> baseStyle.advice
            }
            else -> baseStyle.advice
        }
    }
}
