package com.rajnishkumar.bookbuddy.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rajnishkumar.bookbuddy.ui.sensor.BaseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.database.AppDatabase
import com.rajnishkumar.bookbuddy.models.UserProfile
import com.rajnishkumar.bookbuddy.ui.auth.AuthActivity
import com.rajnishkumar.bookbuddy.ui.dashboard.LibrarianDashboard
import com.rajnishkumar.bookbuddy.ui.dashboard.MemberDashboard
import com.rajnishkumar.bookbuddy.worker.BookSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private val SPLASH_TAG = "SplashActivityLogger"
    private var isRedirected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.ivSplashLogo)
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1500
            fillAfter = true
        }
        logo.startAnimation(fadeIn)

        lifecycleScope.launch(Dispatchers.IO) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                fetchAndCacheProfile(user.uid)
                triggerBackgroundSync()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isRedirected) checkUserStatus(true) 
        }, 8000)

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus(false)
        }, 2500)
    }

    private suspend fun fetchAndCacheProfile(uid: String) {
        try {
            val database = FirebaseDatabase.getInstance().reference
            val snapshot = database.child("users").child(uid).get().await()
            if (snapshot.exists()) {
                val genres = snapshot.child("favoriteGenres").getValue(String::class.java) ?: ""
                
                val roomDb = AppDatabase.getDatabase(applicationContext)
                // IMPORTANT: Fetch existing local profile to preserve the picture path
                val existing = roomDb.userProfileDao().getProfile(uid)
                val currentPic = existing?.profilePicPath 
                
                roomDb.userProfileDao().saveProfile(UserProfile(uid, currentPic, genres))
                Log.d(SPLASH_TAG, "✅ Profile synced. Pic retained: ${currentPic != null}")
            }
        } catch (e: Exception) {
            Log.e(SPLASH_TAG, "❌ Profile fetch failed", e)
        }
    }

    private fun triggerBackgroundSync() {
        val syncRequest = OneTimeWorkRequestBuilder<BookSyncWorker>().build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "book_sync",
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun checkUserStatus(isForced: Boolean) {
        if (isRedirected) return
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            navigateTo(AuthActivity::class.java)
            return
        }

        if (isForced) {
            navigateTo(MemberDashboard::class.java)
            return
        }

        val database = FirebaseDatabase.getInstance().reference
        database.child("users").child(currentUser.uid).child("role").get()
            .addOnCompleteListener { task ->
                if (isRedirected) return@addOnCompleteListener
                if (task.isSuccessful) {
                    val role = task.result.getValue(String::class.java) ?: "member"
                    if (role == "librarian") navigateTo(LibrarianDashboard::class.java)
                    else navigateTo(MemberDashboard::class.java)
                } else {
                    navigateTo(MemberDashboard::class.java)
                }
            }
    }

    private fun navigateTo(destination: Class<*>) {
        if (isRedirected) return
        isRedirected = true
        startActivity(Intent(this, destination))
        finish()
    }
}
