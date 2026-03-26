package com.example.bookbuddy.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val apiKey = Constants.GEMINI_API_KEY
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun generateSummary(description: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            return@withContext "Please add your Gemini API key in Constants.kt"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${Constants.GEMINI_SUMMARY_MODEL}:generateContent?key=$apiKey"

        val prompt = """
            Summarize this book description in 2-3 engaging sentences for a library app:
            
            $description
        """.trimIndent()

        // Build request using Gson
        val requestJson = JsonObject().apply {
            val contents = JsonObject().apply {
                val parts = JsonObject().apply {
                    addProperty("text", prompt)
                }
                add("parts", gson.toJsonTree(listOf(parts)))
            }
            add("contents", gson.toJsonTree(listOf(contents)))

            val generationConfig = JsonObject().apply {
                addProperty("temperature", 0.7)
                addProperty("maxOutputTokens", 150)
            }
            add("generationConfig", generationConfig)
        }

        val requestBody = gson.toJson(requestJson).toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            val json = gson.fromJson(responseBody, JsonObject::class.java)

            if (json.has("error")) {
                val error = json.getAsJsonObject("error")
                return@withContext "Error: ${error.get("message").asString}"
            }

            val candidates = json.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val content = candidates[0].asJsonObject
                    .getAsJsonObject("content")
                val parts = content?.getAsJsonArray("parts")
                if (parts != null && parts.size() > 0) {
                    return@withContext parts[0].asJsonObject.get("text").asString.trim()
                }
            }

            "Summary not available"
        } catch (e: Exception) {
            e.printStackTrace()
            "Summary generation failed"
        }
    }
}