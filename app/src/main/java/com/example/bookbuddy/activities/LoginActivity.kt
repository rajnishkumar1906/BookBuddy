package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookbuddy.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var signupLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_login)

            try {
                auth = FirebaseAuth.getInstance()
            } catch (e: Exception) {
                Toast.makeText(this, "Firebase initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Check if user is already logged in
            try {
                if (auth.currentUser != null) {
                    navigateToMain()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error checking login status", Toast.LENGTH_SHORT).show()
            }

            // Initialize views with try-catch
            try {
                initializeViews()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load UI: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Set click listeners
            try {
                setClickListeners()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to setup buttons", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "App error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            emailInput = findViewById(R.id.emailInput)
            passwordInput = findViewById(R.id.passwordInput)
            loginButton = findViewById(R.id.loginButton)
            googleSignInButton = findViewById(R.id.googleSignInButton)
            signupLink = findViewById(R.id.signupLink)
            forgotPasswordLink = findViewById(R.id.forgotPasswordLink)
            progressBar = findViewById(R.id.progressBar)
        } catch (e: Exception) {
            throw Exception("View initialization failed: ${e.message}")
        }
    }

    private fun setClickListeners() {
        try {
            loginButton.setOnClickListener {
                loginUser()
            }

            googleSignInButton.setOnClickListener {
                try {
                    Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // Ignore toast errors
                }
            }

            signupLink.setOnClickListener {
                try {
                    startActivity(Intent(this, SignupActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open signup", Toast.LENGTH_SHORT).show()
                }
            }

            forgotPasswordLink.setOnClickListener {
                try {
                    Toast.makeText(this, "Password reset coming soon!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // Ignore toast errors
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up buttons", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser() {
        try {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate inputs
            try {
                if (email.isEmpty()) {
                    emailInput.error = "Email is required"
                    emailInput.requestFocus()
                    return
                }

                if (password.isEmpty()) {
                    passwordInput.error = "Password is required"
                    passwordInput.requestFocus()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Validation error", Toast.LENGTH_SHORT).show()
                return
            }

            // Show progress bar and disable button
            showLoading(true)

            // Sign in with Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    try {
                        showLoading(false)

                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Login successful! Welcome back!",
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateToMain()
                        } else {
                            val errorMsg = task.exception?.message ?: "Login failed"
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        showLoading(false)
                        Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        try {
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                loginButton.isEnabled = false
                loginButton.alpha = 0.5f
                googleSignInButton.isEnabled = false
                googleSignInButton.alpha = 0.5f
                signupLink.isEnabled = false
                signupLink.alpha = 0.5f
                forgotPasswordLink.isEnabled = false
                forgotPasswordLink.alpha = 0.5f
            } else {
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true
                loginButton.alpha = 1.0f
                googleSignInButton.isEnabled = true
                googleSignInButton.alpha = 1.0f
                signupLink.isEnabled = true
                signupLink.alpha = 1.0f
                forgotPasswordLink.isEnabled = true
                forgotPasswordLink.alpha = 1.0f
            }
        } catch (e: Exception) {
            // Ignore UI errors during loading state
        }
    }

    private fun navigateToMain() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}