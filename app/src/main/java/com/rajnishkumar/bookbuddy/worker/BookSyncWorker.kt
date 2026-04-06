package com.rajnishkumar.bookbuddy.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.ai.HuggingFaceClient
import com.rajnishkumar.bookbuddy.database.AppDatabase
import com.rajnishkumar.bookbuddy.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BookSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = FirebaseDatabase.getInstance().reference
    private val roomDb = AppDatabase.getDatabase(context)
    private val huggingFace = HuggingFaceClient()
    private val tag = "BookSyncWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "🔄 Starting Background Sync...")

            // 1. Check for global update timestamp
            val lastUpdateSnapshot = database.child("sync_tracker").child("last_update").get().await()
            val remoteLastUpdate = lastUpdateSnapshot.getValue(Long::class.java) ?: 0L
            
            val localLastSync = roomDb.bookDao().getLastSyncTime() ?: 0L

            if (remoteLastUpdate <= localLastSync && localLastSync != 0L) {
                // Check if there are any books missing embeddings locally even if no new global update
                val missingEmbeddings = roomDb.bookDao().getBooksMissingEmbeddings()
                if (missingEmbeddings.isEmpty()) {
                    Log.d(tag, "✅ Local library is already up to date.")
                    return@withContext Result.success()
                }
            }

            // 2. Fetch all books from Firebase
            val snapshot = database.child("books").get().await()
            if (!snapshot.exists()) return@withContext Result.success()

            val remoteBooks = snapshot.children.mapNotNull { child ->
                try {
                    val book = child.getValue(Book::class.java)
                    book?.id = child.key ?: ""
                    book
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing book ${child.key}", e)
                    null
                }
            }
            
            // 3. Compare with local database
            val localBooks = roomDb.bookDao().getAllBooks()
            val localIdMap = localBooks.associateBy { it.id }

            val booksToUpdate = remoteBooks.filter { remote ->
                val local = localIdMap[remote.id]
                local == null || local.updatedAt < remote.updatedAt || local.embedding.isEmpty()
            }

            if (booksToUpdate.isEmpty()) {
                Log.d(tag, "✅ No new updates found.")
                return@withContext Result.success()
            }

            Log.i(tag, "📦 Syncing ${booksToUpdate.size} books...")

            // 4. Process updates and generate embeddings if needed
            booksToUpdate.forEach { book ->
                val existingLocal = localIdMap[book.id]
                
                // Only regenerate embedding if data changed or it's missing
                val needsNewEmbedding = existingLocal == null || 
                                       existingLocal.title != book.title || 
                                       existingLocal.description != book.description ||
                                       existingLocal.embedding.isEmpty()

                val finalEmbedding = if (needsNewEmbedding) {
                    val textToEmbed = "${book.title} ${book.author} ${book.genre.joinToString(" ")} ${book.description}".take(800)
                    try {
                        huggingFace.getEmbedding(textToEmbed)
                    } catch (e: Exception) {
                        existingLocal?.embedding ?: emptyList()
                    }
                } else {
                    existingLocal?.embedding ?: emptyList()
                }

                val syncedBook = book.copy(embedding = finalEmbedding)
                roomDb.bookDao().insertAll(listOf(syncedBook))
            }

            Log.i(tag, "✨ Sync Complete. Local Database updated.")
            Result.success()

        } catch (e: Exception) {
            Log.e(tag, "❌ Sync failed", e)
            Result.retry()
        }
    }
}
