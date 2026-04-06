package com.rajnishkumar.bookbuddy.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.ui.dashboard.LibrarianDashboard
import com.rajnishkumar.bookbuddy.ui.dashboard.MemberDashboard
import com.rajnishkumar.bookbuddy.R
import android.view.animation.AnimationUtils
import com.rajnishkumar.bookbuddy.utils.NotificationHelper

class SignupFragment : Fragment() {

    private val TAG = "SignupFragmentLogger"
    private lateinit var tvStatus: TextView

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerRole: Spinner
    private var tilAdminCode: View? = null
    private lateinit var etAdminCode: EditText
    private lateinit var btnSignup: View
    private lateinit var tvGoToLogin: TextView
    private lateinit var cardView: View
    private lateinit var ivLogo: View
    private lateinit var tvTitle: View
    private var tilName: View? = null
    private var tilEmail: View? = null
    private var tilPassword: View? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var selectedRole = "member"
    private val secretCode = "LIB2024"

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)

        try {
            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance()

            tvStatus = view.findViewById(R.id.tvStatus)
            etName = view.findViewById(R.id.etName)
            etEmail = view.findViewById(R.id.etEmail)
            etPassword = view.findViewById(R.id.etPassword)
            spinnerRole = view.findViewById(R.id.spinnerRole)
            tilAdminCode = view.findViewById(R.id.tilAdminCode)
            etAdminCode = view.findViewById(R.id.etAdminCode)
            btnSignup = view.findViewById(R.id.btnSignup)
            tvGoToLogin = view.findViewById(R.id.tvGoToLogin)
            cardView = view.findViewById(R.id.cardView)
            ivLogo = view.findViewById(R.id.ivLogo)
            tvTitle = view.findViewById(R.id.tvTitle)
            tilName = view.findViewById(R.id.tilName)
            tilEmail = view.findViewById(R.id.tilEmail)
            tilPassword = view.findViewById(R.id.tilPassword)

            setupRoleSpinner()
            btnSignup.setOnClickListener { signupUser() }
            tvGoToLogin.setOnClickListener {
                (activity as AuthActivity).loadFragment(LoginFragment())
            }

            startEntranceAnimations()
        } catch (e: Exception) {
            Log.e(TAG, "🚨 CRASH in onCreateView: ${e.message}")
        }

        return view
    }

    private fun navigateToDashboard(role: String) {
        if (!isAdded) return
        val intent = if (role == "librarian") {
            Intent(requireContext(), LibrarianDashboard::class.java)
        } else {
            Intent(requireContext(), MemberDashboard::class.java)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun setupRoleSpinner() {
        val roles = arrayOf("Member", "Librarian")
        
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, roles) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val textView = v as TextView
                val typedValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                textView.setTextColor(typedValue.data)
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                val textView = v as TextView
                
                val textColorValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, textColorValue, true)
                textView.setTextColor(textColorValue.data)

                val bgColorValue = TypedValue()
                context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, bgColorValue, true)
                v.setBackgroundColor(bgColorValue.data)
                
                return v
            }
        }
        
        spinnerRole.adapter = adapter

        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRole = if (position == 1) "librarian" else "member"
                tilAdminCode?.visibility = if (position == 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun startEntranceAnimations() {
        try {
            val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_fall_down)
            cardView.startAnimation(slideUp)

            val views = listOfNotNull(ivLogo, tvTitle, tilName, tilEmail, tilPassword, spinnerRole, btnSignup, tvGoToLogin)
            views.forEachIndexed { index, v ->
                v.alpha = 0f
                v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(250L + index * 80L)
                    .start()
            }
        } catch (e: Exception) { }
    }

    private fun signupUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val adminCode = etAdminCode.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRole == "librarian" && adminCode != secretCode) {
            Toast.makeText(requireContext(), "Invalid secret code", Toast.LENGTH_SHORT).show()
            return
        }

        btnSignup.isEnabled = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    NotificationHelper.showWelcomeNotification(requireContext(), name, true)
                    saveUserToDatabase(userId, name, email)
                } else {
                    Toast.makeText(requireContext(), "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    btnSignup.isEnabled = true
                }
            }
    }

    private fun saveUserToDatabase(userId: String, name: String, email: String) {
        val user = mapOf("id" to userId, "name" to name, "email" to email, "role" to selectedRole)
        database.reference.child("users").child(userId).setValue(user)
            .addOnSuccessListener {
                if (isAdded) {
                    navigateToDashboard(selectedRole)
                }
            }
            .addOnFailureListener { btnSignup.isEnabled = true }
    }
}
