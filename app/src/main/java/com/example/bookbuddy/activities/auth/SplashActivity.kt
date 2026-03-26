package com.example.bookbuddy.activities.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookbuddy.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_splash)

            // Delay for 2 seconds then move to LoginActivity
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error starting app: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }, 2000)

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback - go directly to LoginActivity if splash fails
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}