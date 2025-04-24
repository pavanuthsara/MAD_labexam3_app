package com.example.madnew

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_labexam3_app.R

class Onbording2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onbording2)

        val btnNavigate: Button = findViewById(R.id.button2)
        btnNavigate.setOnClickListener {
            val intent = Intent(this, Onbording3::class.java)
            startActivity(intent)
        }

    }
}