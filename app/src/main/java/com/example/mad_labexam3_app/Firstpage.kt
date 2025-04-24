package com.example.madnew

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_labexam3_app.Onbording1
import com.example.mad_labexam3_app.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Use Handler to delay the screen transition
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an Intent to navigate to Onboarding1 activity
            val intent = Intent(this, Onbording1::class.java)

            // Start the new activity
            startActivity(intent)

            // Optional: finish this activity so user can't go back
            finish()
        }, 3000) // 3000 milliseconds = 3 seconds
    }
}