package com.rajnishkumar.bookbuddy.ui.sensor

import android.util.Log

object UsageTracker {
    private const val TAG = "UsageTrackerLogger"
    private var startTime = 0L
    private var isRunning = false

    fun startTimer(onLimitReached: () -> Unit) {
        if (isRunning) {
            Log.d(TAG, "⏰ Timer already running")
            return
        }
        
        Log.d(TAG, "⏰ Starting usage timer")
        startTime = System.currentTimeMillis()
        isRunning = true
        
        // Simulating a 30-minute check in a real app this would be a WorkManager or Service
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "⏰ Timer check: ${elapsed / 1000 / 60} minutes elapsed")
            if (elapsed >= 30 * 60 * 1000) {
                Log.w(TAG, "🚨 Usage limit reached!")
                onLimitReached()
            }
        }, 30 * 60 * 1000)
    }

    fun stopTimer() {
        Log.d(TAG, "⏰ Stopping usage timer")
        isRunning = false
    }
}
