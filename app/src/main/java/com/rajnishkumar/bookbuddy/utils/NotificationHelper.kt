package com.rajnishkumar.bookbuddy.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.ui.splash.SplashActivity

object NotificationHelper {
    const val UPLOAD_CHANNEL_ID = "book_upload_channel"
    const val SYNC_CHANNEL_ID = "book_sync_channel"
    const val AUTH_CHANNEL_ID = "auth_channel"

    private const val WELCOME_NOTIFICATION_ID = 1001

    fun showWelcomeNotification(context: Context, userName: String, isNewUser: Boolean) {
        if (!hasNotificationPermission(context)) return

        val title = if (isNewUser) "Welcome to BookBuddy, $userName! 📚" else "Welcome back, $userName! 👋"
        val message = if (isNewUser) {
            "Explore your new library ecosystem with AI-powered discovery."
        } else {
            "Ready to continue your reading journey?"
        }

        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, AUTH_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ai_librarian_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(WELCOME_NOTIFICATION_ID, builder.build())
    }

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val uploadChannel = NotificationChannel(
                UPLOAD_CHANNEL_ID,
                "Book Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of bulk book uploads"
            }

            val syncChannel = NotificationChannel(
                SYNC_CHANNEL_ID,
                "Library Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of local library synchronization"
            }

            val authChannel = NotificationChannel(
                AUTH_CHANNEL_ID,
                "Account Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows notifications for signup and login"
            }

            manager.createNotificationChannel(uploadChannel)
            manager.createNotificationChannel(syncChannel)
            manager.createNotificationChannel(authChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
