package com.rajnishkumar.bookbuddy.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.ui.dashboard.LibrarianDashboard
import com.rajnishkumar.bookbuddy.ui.dashboard.MemberDashboard
import com.rajnishkumar.bookbuddy.R
import android.view.animation.AnimationUtils
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

import com.rajnishkumar.bookbuddy.common.Constants
import com.rajnishkumar.bookbuddy.utils.NotificationHelper

class LoginFragment : Fragment() {

    private val TAG = "LoginFragmentLogger"
    private lateinit var tvStatus: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: View
    private lateinit var btnGoogleLogin: View
    private lateinit var tvGoToSignup: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var cardView: View
    private lateinit var ivLogo: View
    private lateinit var tvTitle: View
    private lateinit var tilEmail: View
    private lateinit var tilPassword: View

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var credentialManager: CredentialManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        Log.d(TAG, "🟢 LoginFragment view created")

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        credentialManager = CredentialManager.create(requireContext())

        tvStatus = view.findViewById(R.id.tvStatus)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        btnGoogleLogin = view.findViewById(R.id.btnGoogleLogin)
        tvGoToSignup = view.findViewById(R.id.tvGoToSignup)
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword)
        cardView = view.findViewById(R.id.cardView)
        ivLogo = view.findViewById(R.id.ivLogo)
        tvTitle = view.findViewById(R.id.tvTitle)
        tilEmail = view.findViewById(R.id.tilEmail)
        tilPassword = view.findViewById(R.id.tilPassword)

        btnLogin.setOnClickListener { loginUser() }
        btnGoogleLogin.setOnClickListener { signInWithGoogle() }
        tvForgotPassword.setOnClickListener {
            (activity as? AuthActivity)?.loadFragment(ForgotPasswordFragment())
        }
        tvGoToSignup.setOnClickListener {
            Log.d(TAG, "👉 Navigating to SignupFragment")
            (activity as? AuthActivity)?.loadFragment(SignupFragment())
        }

        startEntranceAnimations()
        return view
    }

    private fun signInWithGoogle() {
        // Generate a simple SHA-256 hashed nonce for security
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(Constants.GOOGLE_SERVER_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "🚀 Launching Google Sign-In account picker...")
                val result = credentialManager.getCredential(
                    context = requireActivity(),
                    request = request
                )
                val credential = result.credential
                
                if (credential is GoogleIdTokenCredential) {
                    Log.d(TAG, "✅ Received Google ID Token")
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "🔥 Firebase Authentication successful")
                            val user = auth.currentUser
                            val name = user?.displayName ?: "User"
                            NotificationHelper.showWelcomeNotification(requireContext(), name, false)
                            checkUserExistsInDatabase(user?.uid ?: "", user?.displayName ?: "User", user?.email ?: "")
                        } else {
                            val error = task.exception?.message ?: "Unknown Firebase error"
                            Log.e(TAG, "❌ Firebase Authentication failed: $error")
                            Toast.makeText(requireContext(), "Auth failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e(TAG, "⚠️ Received unexpected credential type: ${credential.type}")
                    Toast.makeText(requireContext(), "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NoCredentialException) {
                Log.e(TAG, "❌ No Google accounts available: ${e.message}")
                Toast.makeText(requireContext(), "No Google accounts found on this device.", Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialException) {
                Log.e(TAG, "❌ Google Sign-In Error (Type: ${e.type}): ${e.message}")
                Toast.makeText(requireContext(), "Google Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Google Sign-In Critical Error: ${e.message}")
                Toast.makeText(requireContext(), "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserExistsInDatabase(uid: String, name: String, email: String) {
        database.reference.child("users").child(uid).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val userMap = mapOf(
                    "id" to uid,
                    "name" to name,
                    "email" to email,
                    "role" to "member"
                )
                database.reference.child("users").child(uid).setValue(userMap).addOnCompleteListener {
                    navigateToDashboard("member")
                }
            } else {
                val role = snapshot.child("role").getValue(String::class.java) ?: "member"
                navigateToDashboard(role)
            }
        }.addOnFailureListener {
            Log.e(TAG, "❌ Database retrieval failed: ${it.message}")
            navigateToDashboard("member") // Fallback to member role
        }
    }

    private fun startEntranceAnimations() {
        try {
            val context = requireContext()
            val slideUp = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down)
            cardView.startAnimation(slideUp)
            
            val views = arrayOf(ivLogo, tvTitle, tilEmail, tilPassword, btnLogin, btnGoogleLogin, tvGoToSignup)
            views.forEachIndexed { index, v ->
                v.alpha = 0f
                v.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(300L + index * 100L)
                    .start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Animation skipped: ${e.message}")
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "⏳ Attempting standard login for: $email")
        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "✅ Standard login successful")
                    val user = auth.currentUser
                    val name = user?.displayName ?: "User"
                    NotificationHelper.showWelcomeNotification(requireContext(), name, false)
                    checkUserRole()
                } else {
                    Log.e(TAG, "❌ Standard login failed: ${task.exception?.message}")
                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                }
            }
    }

    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return
        Log.d(TAG, "🔍 Checking role for user UID: $userId")

        database.reference.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.child("role").getValue(String::class.java) ?: "member"
                navigateToDashboard(role)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Database role check failed: ${e.message}")
                tvStatus.text = "Error verifying user role"
                btnLogin.isEnabled = true
            }
    }

    private fun navigateToDashboard(role: String) {
        if (!isAdded) return
        try {
            val intent = if (role == "librarian") {
                Intent(requireContext(), LibrarianDashboard::class.java)
            } else {
                Intent(requireContext(), MemberDashboard::class.java)
            }
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e(TAG, "🚀 Error navigating to dashboard: ${e.message}")
            Toast.makeText(requireContext(), "Could not open dashboard", Toast.LENGTH_LONG).show()
            btnLogin.isEnabled = true
        }
    }
}
