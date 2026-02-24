package com.example.bookbuddy.activities.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.bookbuddy.activities.member.MemberMainActivity
import com.example.bookbuddy.activities.librarian.LibrarianMainActivity
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
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var roleSpinner: Spinner
    private lateinit var adminCodeInput: TextInputEditText
    private lateinit var signupButton: Button
    private lateinit var loginLink: TextView
    private lateinit var progressBar: ProgressBar

    private val LIBRARIAN_SECRET_CODE = "LIB2024"
    private var selectedRole = "member"
    private val TAG = "SignupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_signup)

            initializeFirebase()
            initializeViews()
            setupRoleSpinner()
            setClickListeners()
            checkCurrentUser()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "App error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeFirebase() {
        try {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}", e)
            Toast.makeText(this, "Firebase initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
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

            adminCodeInput.visibility = View.GONE
            Log.d(TAG, "Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            Toast.makeText(this, "UI initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupRoleSpinner() {
        try {
            val roles = arrayOf("Member", "Librarian")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            roleSpinner.adapter = adapter

            roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    try {
                        selectedRole = if (position == 1) "librarian" else "member"
                        adminCodeInput.visibility = if (position == 1) View.VISIBLE else View.GONE
                        adminCodeInput.error = null
                        Log.d(TAG, "Role selected: $selectedRole")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in role selection: ${e.message}", e)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    adminCodeInput.visibility = View.GONE
                    selectedRole = "member"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up role spinner: ${e.message}", e)
            Toast.makeText(this, "Failed to setup role selection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setClickListeners() {
        try {
            signupButton.setOnClickListener {
                try {
                    signupUser()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in signup click: ${e.message}", e)
                    Toast.makeText(this, "Signup error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            }

            loginLink.setOnClickListener {
                try {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to login: ${e.message}", e)
                    Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting click listeners: ${e.message}", e)
            Toast.makeText(this, "Failed to setup buttons", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCurrentUser() {
        try {
            if (auth.currentUser != null) {
                Log.d(TAG, "User already logged in, navigating based on role")
                navigateBasedOnRole()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking current user: ${e.message}", e)
        }
    }

    private fun signupUser() {
        try {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            val adminCode = adminCodeInput.text.toString().trim()

            if (!validateInputs(name, email, password, confirmPassword, adminCode)) return

            showLoading(true)
            Log.d(TAG, "Attempting to create user with email: $email")

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    try {
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                Log.d(TAG, "User created in Auth with ID: $userId")
                                createUserInFirestore(userId, name, email)
                            } else {
                                showLoading(false)
                                Log.e(TAG, "User ID is null after successful auth")
                                Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showLoading(false)
                            val errorMsg = task.exception?.message ?: "Signup failed"
                            Log.e(TAG, "Auth signup failed: $errorMsg")
                            handleSignupError(task.exception?.message)
                        }
                    } catch (e: Exception) {
                        showLoading(false)
                        Log.e(TAG, "Error in signup completion handler: ${e.message}", e)
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Log.e(TAG, "Auth signup failure listener: ${e.message}", e)
                    Toast.makeText(this, "Authentication error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Log.e(TAG, "Unexpected error in signupUser: ${e.message}", e)
            Toast.makeText(this, "Signup error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(name: String, email: String, password: String,
                               confirmPassword: String, adminCode: String): Boolean {
        try {
            if (name.isEmpty()) {
                nameInput.error = "Name is required"
                nameInput.requestFocus()
                return false
            }

            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                emailInput.requestFocus()
                return false
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Invalid email"
                emailInput.requestFocus()
                return false
            }

            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                passwordInput.requestFocus()
                return false
            }

            if (password.length < 6) {
                passwordInput.error = "Password must be at least 6 characters"
                passwordInput.requestFocus()
                return false
            }

            if (confirmPassword.isEmpty()) {
                confirmPasswordInput.error = "Please confirm password"
                confirmPasswordInput.requestFocus()
                return false
            }

            if (password != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                confirmPasswordInput.requestFocus()
                return false
            }

            if (selectedRole == "librarian") {
                if (adminCode.isEmpty()) {
                    adminCodeInput.error = "Secret code required"
                    adminCodeInput.requestFocus()
                    return false
                }
                if (adminCode != LIBRARIAN_SECRET_CODE) {
                    adminCodeInput.error = "Invalid secret code"
                    adminCodeInput.requestFocus()
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in validation: ${e.message}", e)
            Toast.makeText(this, "Validation error", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun createUserInFirestore(userId: String, name: String, email: String) {
        try {
            val user = User(
                id = userId,
                name = name,
                email = email,
                role = selectedRole
            )

            Log.d(TAG, "Saving user to Firestore: $user")

            db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener {
                    try {
                        showLoading(false)
                        val message = if (selectedRole == "librarian")
                            "Librarian account created!" else "Member account created!"
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        Log.d(TAG, "User saved to Firestore successfully")

                        if (selectedRole == "librarian") {
                            Log.d(TAG, "Navigating to LibrarianMainActivity")
                            navigateToLibrarianMain()
                        } else {
                            Log.d(TAG, "Navigating to MemberMainActivity")
                            navigateToMemberMain()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in success handler: ${e.message}", e)
                        Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Log.e(TAG, "Firestore save failed: ${e.message}", e)
                    Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()

                    try {
                        // Still try to navigate even if Firestore fails
                        if (selectedRole == "librarian") {
                            navigateToLibrarianMain()
                        } else {
                            navigateToMemberMain()
                        }
                    } catch (navError: Exception) {
                        Log.e(TAG, "Navigation after Firestore failure also failed: ${navError.message}")
                    }

                    auth.signOut()
                }
        } catch (e: Exception) {
            showLoading(false)
            Log.e(TAG, "Error in createUserInFirestore: ${e.message}", e)
            Toast.makeText(this, "Error creating user profile: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignupError(errorMessage: String?) {
        try {
            when {
                errorMessage?.contains("email", ignoreCase = true) == true &&
                        errorMessage.contains("already", ignoreCase = true) -> {
                    emailInput.error = "Email already registered"
                    emailInput.requestFocus()
                }
                errorMessage?.contains("weak", ignoreCase = true) == true -> {
                    passwordInput.error = "Password is too weak"
                    passwordInput.requestFocus()
                }
                errorMessage?.contains("network", ignoreCase = true) == true -> {
                    Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(this, errorMessage ?: "Signup failed", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling signup error: ${e.message}", e)
            Toast.makeText(this, "An unexpected error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateBasedOnRole() {
        try {
            val userId = auth.currentUser?.uid ?: run {
                Log.e(TAG, "No user ID found in navigateBasedOnRole")
                navigateToMemberMain()
                return
            }

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        val user = document.toObject(User::class.java)
                        if (user?.role == "librarian") {
                            Log.d(TAG, "Role check: librarian, navigating to LibrarianMainActivity")
                            navigateToLibrarianMain()
                        } else {
                            Log.d(TAG, "Role check: member, navigating to MemberMainActivity")
                            navigateToMemberMain()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user document: ${e.message}", e)
                        navigateToMemberMain()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to fetch user role: ${e.message}", e)
                    navigateToMemberMain()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in navigateBasedOnRole: ${e.message}", e)
            navigateToMemberMain()
        }
    }

    private fun navigateToMemberMain() {
        try {
            Log.d(TAG, "Attempting to navigate to MemberMainActivity")
            val intent = Intent(this, MemberMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Log.d(TAG, "Successfully navigated to MemberMainActivity")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to MemberMainActivity: ${e.message}", e)
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToLibrarianMain() {
        try {
            Log.d(TAG, "Attempting to navigate to LibrarianMainActivity")
            val intent = Intent(this, LibrarianMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Log.d(TAG, "Successfully navigated to LibrarianMainActivity")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to LibrarianMainActivity: ${e.message}", e)
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Log.e(TAG, "Error in showLoading: ${e.message}", e)
        }
    }
}