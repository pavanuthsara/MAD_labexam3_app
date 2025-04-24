package com.example.madnew

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_labexam3_app.Onbording2
import com.example.mad_labexam3_app.Onbording3

class Onbording1 : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onbording1)

        val btnNavigate: Button = findViewById(R.id.button2)
        btnNavigate.setOnClickListener {
            val intent = Intent(this, Onbording2::class.java)
            startActivity(intent)
        }

        val btnNavigate1: ImageView = findViewById(R.id.ivIllustration)
        btnNavigate1.setOnClickListener {
            val intent = Intent(this, Onbording3::class.java)
            startActivity(intent)
        }

    }
}