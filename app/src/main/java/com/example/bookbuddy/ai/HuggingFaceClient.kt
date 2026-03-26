package com.example.bookbuddy.ai

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class HuggingFaceClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val modelUrl = "https://api-inference.huggingface.co/pipeline/feature-extraction/${Constants.HUGGINGFACE_MODEL}"
    private val jsonMediaType = "application/json".toMediaType()
    private val TAG = "HuggingFaceClient"

    suspend fun getEmbedding(text: String): List<Double> = withContext(Dispatchers.IO) {
        delay(Constants.HUGGINGFACE_DELAY_MS)

        try {
            val requestBody = """
                {
                    "inputs": ${JSONArray(listOf(text))}
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(modelUrl)
                .header("Content-Type", "application/json")
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(responseBody)

                if (jsonArray.length() > 0) {
                    val embeddingArray = jsonArray.getJSONArray(0)

                    // Validate embedding dimensions
                    if (embeddingArray.length() != Constants.EMBEDDING_DIMENSIONS) {
                        Log.e(TAG, "❌ Embedding size mismatch! Expected: ${Constants.EMBEDDING_DIMENSIONS}, Got: ${embeddingArray.length()}")
                    } else {
                        Log.d(TAG, "✅ Got embedding with correct dimensions: ${embeddingArray.length()}")
                    }

                    return@withContext List(embeddingArray.length()) { i ->
                        embeddingArray.getDouble(i)
                    }
                }
            } else if (response.code == 503) {
                Log.d(TAG, "Model loading, retrying...")
                delay(3000)
                return@withContext getEmbedding(text)
            } else {
                Log.e(TAG, "API error: ${response.code} - ${response.message}")
            }

            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting embedding", e)
            emptyList()
        }
    }

    suspend fun getBatchEmbeddings(texts: List<String>): List<List<Double>> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyList()

        delay(Constants.HUGGINGFACE_DELAY_MS)

        try {
            val requestBody = """
                {
                    "inputs": ${JSONArray(texts)}
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(modelUrl)
                .header("Content-Type", "application/json")
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(responseBody)

                val results = mutableListOf<List<Double>>()

                for (i in 0 until jsonArray.length()) {
                    val embeddingArray = jsonArray.getJSONArray(i)

                    // Validate each embedding
                    if (embeddingArray.length() != Constants.EMBEDDING_DIMENSIONS) {
                        Log.e(TAG, "❌ Batch embedding $i size mismatch! Expected: ${Constants.EMBEDDING_DIMENSIONS}, Got: ${embeddingArray.length()}")
                    }

                    val embedding = List(embeddingArray.length()) { j ->
                        embeddingArray.getDouble(j)
                    }
                    results.add(embedding)
                }

                Log.d(TAG, "✅ Got ${results.size} embeddings with dimensions ${Constants.EMBEDDING_DIMENSIONS}")
                return@withContext results
            }

            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting batch embeddings", e)
            emptyList()
        }
    }
}