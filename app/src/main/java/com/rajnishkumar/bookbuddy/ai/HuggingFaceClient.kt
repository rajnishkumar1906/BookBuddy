package com.rajnishkumar.bookbuddy.ai

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rajnishkumar.bookbuddy.common.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class HuggingFaceClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor())
            .build()
    }

    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            return chain.proceed(
                chain.request().newBuilder()
                    .header("Authorization", "Bearer ${Constants.HUGGINGFACE_TOKEN}")
                    .header("Content-Type", "application/json")
                    .build()
            )
        }
    }

    /**
     * Enhanced Classification with wider keyword support and descriptive AI prompting.
     */
    suspend fun classifyQuery(query: String): String = withContext(Dispatchers.IO) {
        val q = query.lowercase().trim()
        val words = q.split(Regex("\\s+")).toSet()

        // 1. ADVANCED LOCAL KEYWORD MATCHING
        val greetings = setOf("hi", "hello", "hey", "morning", "howdy", "anyone", "yo", "sup", "greetings", "hii")
        if (words.any { greetings.contains(it) }) return@withContext "GREETING"

        val appreciation = setOf("thanks", "thank", "great", "awesome", "nice", "good", "helpful", "amazing", "perfect", "wow", "brilliant")
        if (words.any { appreciation.contains(it) } || q.contains("thank you")) return@withContext "APPRECIATION"

        val practice = setOf("quiz", "test", "questions", "practice", "mcq", "exam", "challenge", "queisosn", "qestions", "quizz", "evaluate")
        if (words.any { practice.contains(it) } || q.contains("ask me") || q.contains("test me") || q.contains("quiz me")) return@withContext "PRACTICE"

        val rejections = setOf("no", "stop", "cancel", "later", "skip", "nevermind", "exit", "enough", "don't", "dont")
        if (words.any { rejections.contains(it) } || q.contains("not now") || q.contains("no thanks")) return@withContext "REJECTION"

        // 2. DESCRIPTIVE AI FALLBACK (Using Mistral-7B)
        val prompt = """
            <s>[INST] You are an expert classifier for a library AI. 
            Classify the user query into exactly ONE of these categories:
            
            - GREETING: Social openers (e.g., "Hi", "How are you?", "Is anyone there?")
            - APPRECIATION: Positive feedback (e.g., "Thanks!", "This is great", "You helped a lot")
            - PRACTICE: Requests for quizzes/tests (e.g., "Ask me a question", "I want to practice", "Quiz me on this")
            - REJECTION: Declining offers (e.g., "No thanks", "Not now", "Stop it", "I don't want a quiz")
            - QUESTION: Factual queries about book content (e.g., "Who is the hero?", "Explain the plot", "What happened in chapter 2?")
            
            User Query: "$query"
            
            Reply with ONLY the category name in uppercase. No explanation. [/INST]
        """.trimIndent()

        try {
            val json = JsonObject().apply {
                addProperty("inputs", prompt)
                val parameters = JsonObject().apply {
                    addProperty("max_new_tokens", 10)
                    addProperty("return_full_text", false)
                }
                add("parameters", parameters)
            }
            
            val request = Request.Builder()
                .url(Constants.HUGGINGFACE_CHAT_MODEL)
                .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "QUESTION"
                
                val responseBody = response.body?.string() ?: ""
                val element = JsonParser.parseString(responseBody)
                val generatedText = if (element.isJsonArray) {
                    element.asJsonArray[0].asJsonObject.get("generated_text").asString
                } else {
                    element.asJsonObject.get("generated_text").asString
                }
                
                val result = generatedText.substringAfter("[/INST]").trim().uppercase()
                
                when {
                    result.contains("GREETING") -> "GREETING"
                    result.contains("APPRECIATION") -> "APPRECIATION"
                    result.contains("PRACTICE") -> "PRACTICE"
                    result.contains("REJECTION") -> "REJECTION"
                    else -> "QUESTION"
                }
            }
        } catch (e: Exception) {
            Log.e("HuggingFace", "Classification error: ${e.message}")
            "QUESTION" 
        }
    }

    suspend fun getEmbedding(text: String): List<Double> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply { addProperty("inputs", text) }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder().url(Constants.HUGGINGFACE_API_URL).post(body).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: ""
                val element = JsonParser.parseString(bodyStr)
                if (element.isJsonArray) {
                    val array = element.asJsonArray
                    if (array.size() > 0 && array[0].isJsonArray) {
                        array[0].asJsonArray.map { it.asDouble }
                    } else {
                        array.map { it.asDouble }
                    }
                } else emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }
}
