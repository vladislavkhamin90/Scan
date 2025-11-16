package com.example.qr_scan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun start(view: View){
        val pass = findViewById<EditText>(R.id.ed_pass)
        val prev = findViewById<TextView>(R.id.previev_text)
        if(pass.text.toString() == "2468") {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }else{
            prev.text = "Введён неправильный пароль\nПовторите попытку"
        }
    }
}