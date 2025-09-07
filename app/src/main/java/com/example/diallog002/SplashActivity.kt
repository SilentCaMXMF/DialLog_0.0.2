package com.example.diallog002

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var logoText: TextView
    private lateinit var taglineText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var loadingText: TextView
    private lateinit var dot1: TextView
    private lateinit var dot2: TextView
    private lateinit var dot3: TextView

    private val SPLASH_DURATION = 3000L // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        initializeViews()
        startAnimations()
        startMainActivity()
    }
    
    private fun initializeViews() {
        logoText = findViewById(R.id.logo_text)
        taglineText = findViewById(R.id.tagline_text)
        subtitleText = findViewById(R.id.subtitle_text)
        loadingText = findViewById(R.id.loading_text)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
    }
    
    private fun startAnimations() {
        // Logo fade in and scale
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_enter_animation)
        logoText.startAnimation(logoAnimation)
        
        // Tagline slide in from bottom with delay
        Handler(Looper.getMainLooper()).postDelayed({
            val taglineAnimation = AnimationUtils.loadAnimation(this, R.anim.tagline_enter_animation)
            taglineText.startAnimation(taglineAnimation)
            taglineText.alpha = 1f
        }, 800)
        
        // Subtitle fade in with delay
        Handler(Looper.getMainLooper()).postDelayed({
            val subtitleAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation)
            subtitleText.startAnimation(subtitleAnimation)
            subtitleText.alpha = 1f
        }, 1400)
        
        // Dot animations with staggered timing
        Handler(Looper.getMainLooper()).postDelayed({
            val dotAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation)
            dot1.startAnimation(dotAnimation)
            dot1.alpha = 1f
        }, 1800)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val dotAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation)
            dot2.startAnimation(dotAnimation)
            dot2.alpha = 1f
        }, 2000)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val dotAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation)
            dot3.startAnimation(dotAnimation)
            dot3.alpha = 1f
        }, 2200)
        
        // Loading text pulse animation
        Handler(Looper.getMainLooper()).postDelayed({
            val loadingAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
            loadingText.startAnimation(loadingAnimation)
            loadingText.alpha = 1f
        }, 2400)
    }
    
    private fun startMainActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("SplashActivity", "Starting MainActivity")
            val intent = Intent(this, MainActivity::class.java)
            
            startActivity(intent)
            
            // Add smooth transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            
            finish()
        }, SPLASH_DURATION)
    }
    
    override fun onBackPressed() {
        // Disable back button during splash
    }
}
