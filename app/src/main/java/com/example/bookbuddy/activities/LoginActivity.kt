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
import com.example.bookbuddy.models.User
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // UI Components
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

            // Initialize Firebase
            try {
                auth = FirebaseAuth.getInstance()
                db = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Toast.makeText(this, "Firebase initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Check if user is already logged in
            try {
                if (auth.currentUser != null) {
                    // User is already logged in, navigate to appropriate dashboard
                    navigateBasedOnRole()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error checking login status", Toast.LENGTH_SHORT).show()
            }

            // Initialize views
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

                // Basic email validation
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInput.error = "Please enter a valid email"
                    emailInput.requestFocus()
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
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Login successful! Welcome back!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Navigate based on user role
                            navigateBasedOnRole()
                        } else {
                            showLoading(false)
                            val errorMsg = task.exception?.message ?: "Login failed"

                            // Handle specific error messages
                            when {
                                errorMsg.contains("password", ignoreCase = true) -> {
                                    passwordInput.error = "Incorrect password"
                                    passwordInput.requestFocus()
                                }
                                errorMsg.contains("email", ignoreCase = true) -> {
                                    emailInput.error = "Email not found"
                                    emailInput.requestFocus()
                                }
                                else -> {
                                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            }
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

    /**
     * Navigate to appropriate dashboard based on user role
     */
    private fun navigateBasedOnRole() {
        try {
            val userId = auth.currentUser?.uid

            if (userId == null) {
                // Fallback to member dashboard if no user ID
                navigateToMemberDashboard()
                return
            }

            showLoading(true)

            // Fetch user role from Firestore
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    showLoading(false)

                    if (document.exists()) {
                        val user = document.toObject(User::class.java)

                        if (user?.role == "librarian") {
                            // Librarian dashboard
                            navigateToLibrarianDashboard()
                        } else {
                            // Member dashboard (default)
                            navigateToMemberDashboard()
                        }
                    } else {
                        // User document doesn't exist - create it
                        createUserDocumentAndNavigate(userId)
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)

                    // Log the error but still navigate (default to member)
                    android.util.Log.e("LoginActivity", "Error fetching role: ${e.message}")

                    // Default to member dashboard
                    Toast.makeText(
                        this,
                        "Using default access. Role not found.",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToMemberDashboard()
                }
        } catch (e: Exception) {
            showLoading(false)
            android.util.Log.e("LoginActivity", "Navigation error: ${e.message}")
            navigateToMemberDashboard() // Fallback
        }
    }

    /**
     * Create user document if it doesn't exist and navigate
     */
    private fun createUserDocumentAndNavigate(userId: String) {
        try {
            // Create new user document with member role
            val user = User(
                id = userId,
                name = auth.currentUser?.displayName ?: "User",
                email = auth.currentUser?.email ?: "",
                role = "member"  // Default role
            )

            db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "Welcome! Your account is set up.", Toast.LENGTH_SHORT).show()
                    navigateToMemberDashboard()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("LoginActivity", "Error creating user: ${e.message}")
                    navigateToMemberDashboard() // Navigate anyway
                }
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Error in createUserDocumentAndNavigate: ${e.message}")
            navigateToMemberDashboard()
        }
    }

    /**
     * Navigate to Member Dashboard
     */
    private fun navigateToMemberDashboard() {
        try {
            val intent = Intent(this, MemberDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Navigation to member dashboard failed: ${e.message}")
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Navigate to Librarian Dashboard
     */
    private fun navigateToLibrarianDashboard() {
        try {
            val intent = Intent(this, LibrarianDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Navigation to librarian dashboard failed: ${e.message}")

            // Fallback to member dashboard
            Toast.makeText(this, "Error accessing librarian panel. Using member view.", Toast.LENGTH_SHORT).show()
            navigateToMemberDashboard()
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
                emailInput.isEnabled = false
                passwordInput.isEnabled = false
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
                emailInput.isEnabled = true
                passwordInput.isEnabled = true
            }
        } catch (e: Exception) {
            // Ignore UI errors during loading state
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in when activity starts
        try {
            if (auth.currentUser != null) {
                navigateBasedOnRole()
            }
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Error in onStart: ${e.message}")
        }
    }
}