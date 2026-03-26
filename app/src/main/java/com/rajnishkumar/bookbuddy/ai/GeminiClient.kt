package com.rajnishkumar.bookbuddy.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rajnishkumar.bookbuddy.Constants
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

class GeminiClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/${Constants.GEMINI_MODEL}:generateContent?key=${Constants.GEMINI_API_KEY}"

    suspend fun summarizeBook(description: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prompt = "Summarize the following book description in a concise and engaging way (max 100 words): $description"
        generateContent(prompt)
    }

    suspend fun expandDescription(title: String, author: String, currentDesc: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prompt = """
            The following book description is too short. Please expand it into a detailed, 300-word informative description including plot themes, setting, and style.
            Book: $title
            Author: $author
            Current Info: $currentDesc
        """.trimIndent()
        generateContent(prompt)
    }

    suspend fun chatWithBook(context: String, history: String, question: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prompt = """
            You are a helpful library assistant. 
            Base your answer only on the provided context and history.
            
            Context: $context
            
            History: 
            $history
            
            User Question: $question
        """.trimIndent()
        generateContent(prompt)
    }

    private fun generateContent(prompt: String): String {
        val json = JsonObject()
        val contents = JsonArray()
        val content = JsonObject()
        val parts = JsonArray()
        val part = JsonObject()
        
        part.addProperty("text", prompt)
        parts.add(part)
        content.add("parts", parts)
        contents.add(content)
        json.add("contents", contents)

        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder().url(GEMINI_URL).post(body).build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val root = JsonParser.parseString(responseBody).asJsonObject
                    root.getAsJsonArray("candidates")[0]
                        .asJsonObject.getAsJsonObject("content")
                        .getAsJsonArray("parts")[0]
                        .asJsonObject.get("text").asString
                } else {
                    "Error: ${response.code}"
                }
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}