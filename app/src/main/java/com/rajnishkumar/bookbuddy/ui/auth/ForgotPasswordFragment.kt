package com.rajnishkumar.bookbuddy.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.rajnishkumar.bookbuddy.R

class ForgotPasswordFragment : Fragment() {

    private val TAG = "ForgotPasswordFragment"
    private lateinit var etEmail: EditText
    private lateinit var btnResetPassword: View
    private lateinit var tvBackToLogin: TextView
    private lateinit var tvStatus: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var cardView: View
    private lateinit var tilEmail: View

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_forgot_password, container, false)
        Log.d(TAG, "🟢 ForgotPasswordFragment view created")

        auth = FirebaseAuth.getInstance()

        etEmail = view.findViewById(R.id.etEmail)
        btnResetPassword = view.findViewById(R.id.btnResetPassword)
        tvBackToLogin = view.findViewById(R.id.tvBackToLogin)
        tvStatus = view.findViewById(R.id.tvStatus)
        ivLogo = view.findViewById(R.id.ivLogo)
        tvTitle = view.findViewById(R.id.tvTitle)
        cardView = view.findViewById(R.id.cardView)
        tilEmail = view.findViewById(R.id.tilEmail)

        btnResetPassword.setOnClickListener { resetPassword() }
        tvBackToLogin.setOnClickListener {
            (activity as AuthActivity).loadFragment(LoginFragment())
        }

        startEntranceAnimations()
        return view
    }

    private fun resetPassword() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }

        tvStatus.text = "Sending reset link..."
        tvStatus.setTextColor(resources.getColor(R.color.primary))

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    tvStatus.text = "Reset link sent! Check your email."
                    tvStatus.setTextColor(resources.getColor(R.color.primary))
                    Toast.makeText(requireContext(), "Password reset email sent.", Toast.LENGTH_LONG).show()
                } else {
                    val error = task.exception?.message ?: "Failed to send reset email"
                    tvStatus.text = error
                    tvStatus.setTextColor(resources.getColor(R.color.error_red))
                    Log.e(TAG, "❌ Reset password failed: $error")
                }
            }
    }

    private fun startEntranceAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_text)
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_fall_down)

        ivLogo.startAnimation(fadeIn)
        tvTitle.startAnimation(fadeIn)
        cardView.startAnimation(slideUp)
        tilEmail.startAnimation(slideUp)
        btnResetPassword.startAnimation(slideUp)
        tvBackToLogin.startAnimation(slideUp)
    }
}
