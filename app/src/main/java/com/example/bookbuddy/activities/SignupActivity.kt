package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookbuddy.R
import com.example.bookbuddy.models.User
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var roleSpinner: Spinner
    private lateinit var adminCodeInput: TextInputEditText
    private lateinit var signupButton: Button
    private lateinit var loginLink: TextView
    private lateinit var progressBar: ProgressBar

    // Secret code for librarian registration
    private val LIBRARIAN_SECRET_CODE = "LIB2024"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_signup)

            // Initialize Firebase with try-catch
            try {
                auth = FirebaseAuth.getInstance()
                db = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Toast.makeText(this, "Firebase initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Initialize views with try-catch
            try {
                initializeViews()
                setupRoleSpinner()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load UI: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Set click listeners with try-catch
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
            nameInput = findViewById(R.id.nameInput)
            emailInput = findViewById(R.id.emailInput)
            passwordInput = findViewById(R.id.passwordInput)
            roleSpinner = findViewById(R.id.roleSpinner)
            adminCodeInput = findViewById(R.id.adminCodeInput)
            signupButton = findViewById(R.id.signupButton)
            loginLink = findViewById(R.id.loginLink)
            progressBar = findViewById(R.id.progressBar)

            // Initially hide admin code input
            adminCodeInput.visibility = View.GONE
        } catch (e: Exception) {
            throw Exception("View initialization failed: ${e.message}")
        }
    }

    private fun setupRoleSpinner() {
        try {
            // Role options
            val roles = arrayOf("Member", "Librarian")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            roleSpinner.adapter = adapter

            // Show/hide admin code based on role selection
            roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    try {
                        // Show admin code input only when "Librarian" is selected
                        adminCodeInput.visibility = if (position == 1) View.VISIBLE else View.GONE
                    } catch (e: Exception) {
                        // Ignore spinner errors
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    try {
                        adminCodeInput.visibility = View.GONE
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to setup role selection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setClickListeners() {
        try {
            signupButton.setOnClickListener {
                signupUser()
            }

            loginLink.setOnClickListener {
                try {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open login", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up buttons", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signupUser() {
        try {
            // Get input values with null safety
            val name = try { nameInput.text.toString().trim() } catch (e: Exception) { "" }
            val email = try { emailInput.text.toString().trim() } catch (e: Exception) { "" }
            val password = try { passwordInput.text.toString().trim() } catch (e: Exception) { "" }
            val selectedRole = try { roleSpinner.selectedItem.toString() } catch (e: Exception) { "Member" }
            val adminCode = try { adminCodeInput.text.toString().trim() } catch (e: Exception) { "" }

            // Validate inputs with try-catch
            try {
                if (name.isEmpty()) {
                    nameInput.error = "Name is required"
                    nameInput.requestFocus()
                    return
                }

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

                if (password.length < 6) {
                    passwordInput.error = "Password must be at least 6 characters"
                    passwordInput.requestFocus()
                    return
                }

                // Validate email format
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInput.error = "Please enter a valid email"
                    emailInput.requestFocus()
                    return
                }

                // Validate librarian secret code if librarian role selected
                if (selectedRole == "Librarian") {
                    if (adminCode.isEmpty()) {
                        adminCodeInput.error = "Admin code is required for librarian registration"
                        adminCodeInput.requestFocus()
                        return
                    }
                    if (adminCode != LIBRARIAN_SECRET_CODE) {
                        adminCodeInput.error = "Invalid admin code"
                        adminCodeInput.requestFocus()
                        return
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Validation error", Toast.LENGTH_SHORT).show()
                return
            }

            // Show loading
            showLoading(true)

            // Create user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    try {
                        if (task.isSuccessful) {
                            val userId = try {
                                auth.currentUser?.uid ?: return@addOnCompleteListener
                            } catch (e: Exception) {
                                showLoading(false)
                                Toast.makeText(this, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                                return@addOnCompleteListener
                            }

                            // Determine role (Member or Librarian)
                            val finalRole = if (selectedRole == "Librarian" && adminCode == LIBRARIAN_SECRET_CODE) {
                                "librarian"
                            } else {
                                "member"
                            }

                            // Create user profile in Firestore
                            val user = try {
                                User(
                                    id = userId,
                                    name = name,
                                    email = email,
                                    role = finalRole
                                )
                            } catch (e: Exception) {
                                showLoading(false)
                                Toast.makeText(this, "Failed to create user profile", Toast.LENGTH_SHORT).show()
                                return@addOnCompleteListener
                            }

                            // Save to Firestore
                            db.collection("users").document(userId)
                                .set(user)
                                .addOnCompleteListener { dbTask ->
                                    try {
                                        showLoading(false)

                                        if (dbTask.isSuccessful) {
                                            val roleMessage = if (finalRole == "librarian")
                                                "Librarian account created!"
                                            else
                                                "Member account created!"

                                            Toast.makeText(this, roleMessage, Toast.LENGTH_SHORT).show()

                                            // Navigate to Main Activity
                                            try {
                                                startActivity(Intent(this, MainActivity::class.java))
                                                finish()
                                            } catch (e: Exception) {
                                                Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val errorMsg = dbTask.exception?.message ?: "Failed to save user data"
                                            Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        showLoading(false)
                                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    try {
                                        showLoading(false)
                                        Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } catch (ex: Exception) {
                                        // Ignore
                                    }
                                }
                        } else {
                            showLoading(false)
                            val errorMsg = task.exception?.message ?: "Signup failed"
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
                        Toast.makeText(this, "Authentication error: ${e.message}", Toast.LENGTH_LONG).show()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Signup error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        try {
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                signupButton.isEnabled = false
                signupButton.alpha = 0.5f
                loginLink.isEnabled = false
                loginLink.alpha = 0.5f
                roleSpinner.isEnabled = false
                roleSpinner.alpha = 0.5f
            } else {
                progressBar.visibility = View.GONE
                signupButton.isEnabled = true
                signupButton.alpha = 1.0f
                loginLink.isEnabled = true
                loginLink.alpha = 1.0f
                roleSpinner.isEnabled = true
                roleSpinner.alpha = 1.0f
            }
        } catch (e: Exception) {
            // Ignore UI errors during loading state
        }
    }
}