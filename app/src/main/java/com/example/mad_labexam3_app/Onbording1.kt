package com.example.madnew

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_labexam3_app.R

class Onbording1 : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onbording1)

        val btnNavigate: Button = findViewById(R.id.button2)
        btnNavigate.setOnClickListener {
            val intent = Intent(this, Onbording2::class.java)
            startActivity(intent)
        }

        val btnNavigate1: TextView = findViewById(R.id.tvSkip)
        btnNavigate1.setOnClickListener {
            val intent = Intent(this, Onbording3::class.java)
            startActivity(intent)
        }

    }
}