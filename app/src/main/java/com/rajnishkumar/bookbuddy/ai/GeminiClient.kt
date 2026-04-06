package com.rajnishkumar.bookbuddy.ai

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rajnishkumar.bookbuddy.common.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    // Simple rate limiting: max 15 requests per minute
    private var lastRequestTime = 0L
    private var requestCount = 0
    private val MAX_REQUESTS_PER_MINUTE = 15

    private suspend fun checkRateLimit(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRequestTime > 60000) {
            lastRequestTime = currentTime
            requestCount = 0
        }
        if (requestCount >= MAX_REQUESTS_PER_MINUTE) return false
        requestCount++
        return true
    }

    /**
     * Used by VocalRobo and MemberDashboard to generate plain-text AI responses.
     */
    suspend fun generateDirectResponse(prompt: String): String {
        return generateAnswer(prompt)
    }

    /**
     * Central answer generation method used by RAG and other features.
     */
    suspend fun generateAnswer(prompt: String): String = withContext(Dispatchers.IO) {
        if (!checkRateLimit()) delay(3000)
        generateRawContent(prompt)
    }

    suspend fun summarizeBook(title: String, author: String, description: String): String {
        val prompt = "Summarize the book '$title' by $author based on this description: $description. Provide a concise narrative summary. No markdown."
        return generateAnswer(prompt)
    }

    suspend fun generateQuiz(title: String, context: String): String {
        val prompt = """
            Generate exactly 5 multiple choice questions for the book "$title" based on this context: $context.
            Return ONLY a valid JSON object. No other text.
            Format:
            {
              "questions": [
                {
                  "question": "Question text here?",
                  "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                  "correct_answer": "Exact text of the correct option"
                }
              ]
            }
        """.trimIndent()
        
        val response = generateAnswer(prompt)
        return extractJson(response)
    }

    suspend fun generateSingleQuestion(title: String, context: String): String {
        val prompt = """
            Generate exactly 1 multiple choice question for the book "$title" based on this context: $context.
            Return ONLY a valid JSON object. No other text.
            Format:
            {
              "questions": [
                {
                  "question": "Question text here?",
                  "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                  "correct_answer": "Exact text of the correct option"
                }
              ]
            }
        """.trimIndent()
        
        val response = generateAnswer(prompt)
        return extractJson(response)
    }

    suspend fun expandDescription(title: String, author: String, currentDesc: String, genre: List<String>): String {
        val prompt = "Expand book description for '$title' by $author into a detailed narrative (~800 words). Genres: ${genre.joinToString()}. Original: $currentDesc."
        return generateAnswer(prompt)
    }

    private suspend fun generateRawContent(prompt: String, retryCount: Int = 0): String {
        if (retryCount > 2) return ""
        val json = JsonObject().apply {
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("parts", JsonArray().apply { add(JsonObject().apply { addProperty("text", prompt) }) })
                })
            })
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${Constants.GEMINI_MODEL}:generateContent?key=${Constants.GEMINI_API_KEY}"

        return try {
            client.newCall(Request.Builder().url(url).post(body).build()).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.code == 429) {
                    delay(3000)
                    return generateRawContent(prompt, retryCount + 1)
                }
                if (response.isSuccessful) {
                    val root = JsonParser.parseString(bodyStr).asJsonObject
                    val candidates = root.getAsJsonArray("candidates")
                    if (candidates != null && candidates.size() > 0) {
                        val firstCandidate = candidates[0].asJsonObject
                        val content = firstCandidate.getAsJsonObject("content")
                        if (content != null) {
                            val parts = content.getAsJsonArray("parts")
                            if (parts != null && parts.size() > 0) {
                                val text = parts[0].asJsonObject.get("text").asString
                                return@use text.trim()
                            }
                        }
                    }
                    Log.e("GeminiClient", "No content found in successful response: $bodyStr")
                    ""
                } else {
                    Log.e("GeminiClient", "API call failed with code ${response.code}: ${response.message}")
                    ""
                }
            }
        } catch (e: Exception) { "" }
    }

    private fun extractJson(text: String): String {
        try {
            var cleaned = text.trim()

            // 1. Handle common Gemini/LLM markdown formatting
            if (cleaned.contains("```")) {
                cleaned = when {
                    cleaned.contains("```json") -> cleaned.substringAfter("```json").substringBeforeLast("```")
                    cleaned.contains("```JSON") -> cleaned.substringAfter("```JSON").substringBeforeLast("```")
                    else -> cleaned.substringAfter("```").substringBeforeLast("```")
                }
            }

            // 2. Locate the outermost JSON structure (object or array)
            val startObject = cleaned.indexOf("{")
            val startArray = cleaned.indexOf("[")
            
            val start = when {
                startObject != -1 && startArray != -1 -> Math.min(startObject, startArray)
                startObject != -1 -> startObject
                startArray != -1 -> startArray
                else -> -1
            }

            val endObject = cleaned.lastIndexOf("}")
            val endArray = cleaned.lastIndexOf("]")
            
            val end = when {
                endObject != -1 && endArray != -1 -> Math.max(endObject, endArray)
                endObject != -1 -> endObject
                endArray != -1 -> endArray
                else -> -1
            }

            if (start != -1 && end != -1 && end > start) {
                val jsonCandidate = cleaned.substring(start, end + 1).trim()
                
                // 3. Final validation: Try parsing it with JsonParser to ensure it's valid JSON
                return try {
                    JsonParser.parseString(jsonCandidate)
                    jsonCandidate
                } catch (e: Exception) {
                    Log.e("GeminiClient", "Extracted text is not valid JSON: $jsonCandidate")
                    ""
                }
            }
            
            // 4. Fallback: Check if the whole cleaned text is valid JSON
            return try {
                JsonParser.parseString(cleaned)
                cleaned
            } catch (e: Exception) {
                Log.e("GeminiClient", "No JSON found in response: $text")
                ""
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Error extracting JSON: ${e.message}")
            return ""
        }
    }
}
