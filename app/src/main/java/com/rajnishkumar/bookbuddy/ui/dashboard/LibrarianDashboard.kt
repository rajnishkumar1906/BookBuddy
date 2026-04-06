package com.rajnishkumar.bookbuddy.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.ui.sensor.BaseActivity

class LibrarianDashboard : BaseActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_librarian_dashboard)

        bottomNav = findViewById(R.id.librarianBottomNav)

        if (savedInstanceState == null) {
            loadFragment(LibrarianHomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> LibrarianHomeFragment()
                R.id.nav_manage -> ManageBooksFragment()
                R.id.nav_profile -> LibrarianProfileFragment()
                else -> LibrarianHomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        if (!isFinishing && !isDestroyed) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.librarianNavHost, fragment)
                .commit()
        }
    }
}
