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

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // UI Components
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var roleSpinner: Spinner
    private lateinit var adminCodeInput: TextInputEditText
    private lateinit var signupButton: Button
    private lateinit var loginLink: TextView
    private lateinit var progressBar: ProgressBar

    // Secret code for librarian registration (in production, move to secure location)
    private val LIBRARIAN_SECRET_CODE = "LIB2024" // You can change this

    // Selected role
    private var selectedRole = "member" // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_signup)

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
                    // User already logged in, go to appropriate dashboard
                    navigateBasedOnRole()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error checking login status", Toast.LENGTH_SHORT).show()
            }

            // Initialize views
            try {
                initializeViews()
                setupRoleSpinner()
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
            nameInput = findViewById(R.id.nameInput)
            emailInput = findViewById(R.id.emailInput)
            passwordInput = findViewById(R.id.passwordInput)
            confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
            roleSpinner = findViewById(R.id.roleSpinner)
            adminCodeInput = findViewById(R.id.adminCodeInput)
            signupButton = findViewById(R.id.signupButton)
            loginLink = findViewById(R.id.loginLink)
            progressBar = findViewById(R.id.progressBar)

            // Initially hide admin code input
            adminCodeInput.visibility = View.GONE
            adminCodeInput.hint = "Enter Librarian Secret Code"
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
                        // Update selected role
                        selectedRole = if (position == 1) "librarian" else "member"

                        // Show admin code input only when "Librarian" is selected
                        adminCodeInput.visibility = if (position == 1) View.VISIBLE else View.GONE

                        // Clear any previous errors
                        adminCodeInput.error = null
                    } catch (e: Exception) {
                        // Ignore spinner errors
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    try {
                        adminCodeInput.visibility = View.GONE
                        selectedRole = "member"
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
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
            val confirmPassword = try { confirmPasswordInput.text.toString().trim() } catch (e: Exception) { "" }
            val adminCode = try { adminCodeInput.text.toString().trim() } catch (e: Exception) { "" }

            // Validate inputs
            try {
                // Name validation
                if (name.isEmpty()) {
                    nameInput.error = "Name is required"
                    nameInput.requestFocus()
                    return
                }

                if (name.length < 2) {
                    nameInput.error = "Name must be at least 2 characters"
                    nameInput.requestFocus()
                    return
                }

                // Email validation
                if (email.isEmpty()) {
                    emailInput.error = "Email is required"
                    emailInput.requestFocus()
                    return
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInput.error = "Please enter a valid email"
                    emailInput.requestFocus()
                    return
                }

                // Password validation
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

                // Confirm password validation
                if (confirmPassword.isEmpty()) {
                    confirmPasswordInput.error = "Please confirm your password"
                    confirmPasswordInput.requestFocus()
                    return
                }

                if (password != confirmPassword) {
                    confirmPasswordInput.error = "Passwords do not match"
                    confirmPasswordInput.requestFocus()
                    return
                }

                // Validate librarian secret code if librarian role selected
                if (selectedRole == "librarian") {
                    if (adminCode.isEmpty()) {
                        adminCodeInput.error = "Secret code is required for librarian registration"
                        adminCodeInput.requestFocus()
                        return
                    }
                    if (adminCode != LIBRARIAN_SECRET_CODE) {
                        adminCodeInput.error = "Invalid secret code"
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
                                auth.currentUser?.uid ?: run {
                                    showLoading(false)
                                    Toast.makeText(this, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                                    return@addOnCompleteListener
                                }
                            } catch (e: Exception) {
                                showLoading(false)
                                Toast.makeText(this, "Error getting user ID", Toast.LENGTH_SHORT).show()
                                return@addOnCompleteListener
                            }

                            // Create user profile in Firestore
                            val user = try {
                                User(
                                    id = userId,
                                    name = name,
                                    email = email,
                                    role = selectedRole  // Use the selected role
                                )
                            } catch (e: Exception) {
                                showLoading(false)
                                Toast.makeText(this, "Failed to create user profile", Toast.LENGTH_SHORT).show()
                                return@addOnCompleteListener
                            }

                            // Save to Firestore
                            db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener {
                                    try {
                                        showLoading(false)

                                        // Show success message based on role
                                        val successMessage = if (selectedRole == "librarian") {
                                            "Librarian account created successfully!"
                                        } else {
                                            "Member account created successfully!"
                                        }
                                        Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show()

                                        // Navigate based on role
                                        if (selectedRole == "librarian") {
                                            navigateToLibrarianDashboard()
                                        } else {
                                            navigateToMemberDashboard()
                                        }

                                    } catch (e: Exception) {
                                        showLoading(false)
                                        Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    try {
                                        showLoading(false)
                                        Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()

                                        // Even if Firestore fails, user is created in Auth
                                        // Log the user out to prevent inconsistency
                                        auth.signOut()
                                    } catch (ex: Exception) {
                                        // Ignore
                                    }
                                }
                        } else {
                            showLoading(false)
                            val errorMsg = task.exception?.message ?: "Signup failed"

                            // Handle specific Firebase errors
                            when {
                                errorMsg.contains("email", ignoreCase = true) &&
                                        errorMsg.contains("already", ignoreCase = true) -> {
                                    emailInput.error = "Email already registered"
                                    emailInput.requestFocus()
                                }
                                errorMsg.contains("weak", ignoreCase = true) -> {
                                    passwordInput.error = "Password is too weak"
                                    passwordInput.requestFocus()
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
            android.util.Log.e("SignupActivity", "Navigation to member dashboard failed: ${e.message}")
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
            android.util.Log.e("SignupActivity", "Navigation to librarian dashboard failed: ${e.message}")

            // Fallback to member dashboard
            Toast.makeText(this, "Error accessing librarian panel. Using member view.", Toast.LENGTH_SHORT).show()
            navigateToMemberDashboard()
        }
    }

    /**
     * Navigate based on role (if already logged in)
     */
    private fun navigateBasedOnRole() {
        try {
            val userId = auth.currentUser?.uid ?: return

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        if (user?.role == "librarian") {
                            navigateToLibrarianDashboard()
                        } else {
                            navigateToMemberDashboard()
                        }
                    } else {
                        navigateToMemberDashboard()
                    }
                }
                .addOnFailureListener {
                    navigateToMemberDashboard()
                }
        } catch (e: Exception) {
            navigateToMemberDashboard()
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
                nameInput.isEnabled = false
                emailInput.isEnabled = false
                passwordInput.isEnabled = false
                confirmPasswordInput.isEnabled = false
                adminCodeInput.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                signupButton.isEnabled = true
                signupButton.alpha = 1.0f
                loginLink.isEnabled = true
                loginLink.alpha = 1.0f
                roleSpinner.isEnabled = true
                roleSpinner.alpha = 1.0f
                nameInput.isEnabled = true
                emailInput.isEnabled = true
                passwordInput.isEnabled = true
                confirmPasswordInput.isEnabled = true
                adminCodeInput.isEnabled = true
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
            android.util.Log.e("SignupActivity", "Error in onStart: ${e.message}")
        }
    }
}