package com.rajnishkumar.bookbuddy.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.ai.BulkUploadHelper
import java.io.File

class BookUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "book_upload_channel"
        const val NOTIFICATION_ID = 1001
        const val KEY_FILE_PATH = "file_path"
    }

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val fileUri = Uri.fromFile(File(filePath))

        // Set initial foreground info with correct type for Android 14+
        setForeground(createForegroundInfo(0, 0, "Initializing..."))

        val helper = BulkUploadHelper(applicationContext)
        
        return try {
            val result = helper.uploadBooksFromCSV(fileUri) { progress ->
                // Update notification progress
                val notification = createNotification(progress.completed, progress.total, progress.currentBook)
                notificationManager.notify(NOTIFICATION_ID, notification)
                
                // Update worker progress for UI observation
                setProgressAsync(workDataOf(
                    "completed" to progress.completed,
                    "total" to progress.total,
                    "current" to progress.currentBook,
                    "percentage" to progress.percentage,
                    "isFinished" to false
                ))
            }

            // Cleanup temp file
            File(filePath).delete()

            // Final progress update
            setProgressAsync(workDataOf("isFinished" to true))

            showFinalNotification(result.success, result.failed, result.skipped)
            Result.success()
        } catch (e: Exception) {
            Log.e("BookUploadWorker", "Upload failed", e)
            File(filePath).delete()
            Result.failure()
        }
    }

    private fun createForegroundInfo(completed: Int, total: Int, current: String): ForegroundInfo {
        createNotificationChannel()
        val notification = createNotification(completed, total, current)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(completed: Int, total: Int, current: String): android.app.Notification {
        val progressText = if (total > 0) "Uploading $completed/$total: $current" else "Preparing upload..."
        
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("BookBuddy: Uploading Collection 📚")
            .setTicker("Uploading Collection")
            .setContentText(progressText)
            .setSmallIcon(R.drawable.ic_ai_librarian_logo)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(total, completed, total == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showFinalNotification(success: Int, failed: Int, skipped: Int) {
        val message = "Finished! Added: $success, Failed: $failed, Skipped: $skipped"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Upload Complete ✅")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_ai_librarian_logo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Book Uploads"
            val descriptionText = "Notifications for bulk book uploads"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
