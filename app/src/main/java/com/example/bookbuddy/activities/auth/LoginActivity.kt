package com.example.bookbuddy.activities.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var signupLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeFirebase()
        initializeViews()
        checkCurrentUser()
        setClickListeners()
    }

    private fun initializeFirebase() {
        try {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_firebase_init, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        googleSignInButton = findViewById(R.id.googleSignInButton)
        signupLink = findViewById(R.id.signupLink)
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun checkCurrentUser() {
        if (auth.currentUser != null) {
            navigateBasedOnRole()
        }
    }

    private fun setClickListeners() {
        loginButton.setOnClickListener { loginUser() }

        googleSignInButton.setOnClickListener {
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }

        signupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        forgotPasswordLink.setOnClickListener {
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validateInputs(email, password)) return

        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                    navigateBasedOnRole()
                } else {
                    handleLoginError(task.exception?.message)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, getString(R.string.login_failed, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        when {
            TextUtils.isEmpty(email) -> {
                emailInput.error = getString(R.string.error_email_required)
                emailInput.requestFocus()
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailInput.error = getString(R.string.error_email_invalid)
                emailInput.requestFocus()
                return false
            }
            TextUtils.isEmpty(password) -> {
                passwordInput.error = getString(R.string.error_password_required)
                passwordInput.requestFocus()
                return false
            }
        }
        return true
    }

    private fun handleLoginError(errorMessage: String?) {
        when {
            errorMessage?.contains("password", ignoreCase = true) == true -> {
                passwordInput.error = getString(R.string.error_password_incorrect)
                passwordInput.requestFocus()
            }
            errorMessage?.contains("email", ignoreCase = true) == true -> {
                emailInput.error = getString(R.string.error_email_not_found)
                emailInput.requestFocus()
            }
            else -> {
                Toast.makeText(this, errorMessage ?: getString(R.string.error_login_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateBasedOnRole() {
        val userId = auth.currentUser?.uid ?: run {
            navigateToMemberMain()
            return
        }

        showLoading(true)

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user?.role == "librarian") {
                        navigateToLibrarianMain()
                    } else {
                        navigateToMemberMain()
                    }
                } else {
                    createUserDocumentAndNavigate(userId)
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, R.string.error_role_default, Toast.LENGTH_SHORT).show()
                navigateToMemberMain()
            }
    }

    private fun createUserDocumentAndNavigate(userId: String) {
        val user = User(
            id = userId,
            name = auth.currentUser?.displayName ?: "User",
            email = auth.currentUser?.email ?: "",
            role = "member"
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.welcome_new_user, Toast.LENGTH_SHORT).show()
                navigateToMemberMain()
            }
            .addOnFailureListener {
                navigateToMemberMain()
            }
    }

    private fun navigateToMemberMain() {
        val intent = Intent(this, MemberMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLibrarianMain() {
        val intent = Intent(this, LibrarianMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !isLoading
        loginButton.alpha = if (isLoading) 0.5f else 1.0f
        googleSignInButton.isEnabled = !isLoading
        googleSignInButton.alpha = if (isLoading) 0.5f else 1.0f
        signupLink.isEnabled = !isLoading
        forgotPasswordLink.isEnabled = !isLoading
        emailInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading
    }
}