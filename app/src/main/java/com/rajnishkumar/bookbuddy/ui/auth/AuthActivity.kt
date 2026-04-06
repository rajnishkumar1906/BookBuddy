package com.rajnishkumar.bookbuddy.ui.auth

import android.util.Log
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.ui.sensor.BaseActivity

class AuthActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AuthActivity", "onCreate called")
        setContentView(R.layout.activity_auth)

        if (savedInstanceState == null) {
            Log.d("AuthActivity", "Loading default LoginFragment")
            loadFragment(LoginFragment())
        }
    }

    fun loadFragment(fragment: Fragment) {
        Log.d("AuthActivity", "Loading fragment: ${fragment.javaClass.simpleName}")
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}