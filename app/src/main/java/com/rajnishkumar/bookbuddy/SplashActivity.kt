package com.rajnishkumar.bookbuddy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay for 2 seconds to show the splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 2000)
    }

    private fun checkUserStatus() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User is signed in, check their role and go to the appropriate dashboard
            val database = FirebaseDatabase.getInstance().reference
            database.child("users").child(currentUser.uid).get()
                .addOnSuccessListener { snapshot ->
                    val role = snapshot.child("role").getValue(String::class.java) ?: "member"
                    if (role == "librarian") {
                        startActivity(Intent(this, LibrarianDashboard::class.java))
                    } else {
                        startActivity(Intent(this, MemberDashboard::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    // Fallback to AuthActivity if data fetch fails
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                }
        } else {
            // No user signed in, go to AuthActivity
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
}