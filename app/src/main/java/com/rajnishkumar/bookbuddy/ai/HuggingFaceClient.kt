package com.rajnishkumar.bookbuddy.ai

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.rajnishkumar.bookbuddy.Constants
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import java.util.concurrent.TimeUnit

class HuggingFaceClient {

    companion object {
        private const val TAG = "HuggingFaceClient"
        private const val SUMMARIZATION_URL = "https://router.huggingface.co/hf-inference/models/facebook/bart-large-cnn"
        private const val CHAT_MODEL_URL = "https://router.huggingface.co/hf-inference/models/mistralai/Mistral-7B-Instruct-v0.2"
    }

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
            val request = chain.request()
            return chain.proceed(request.newBuilder()
                .header("Authorization", "Bearer ${Constants.HUGGINGFACE_TOKEN}")
                .header("Content-Type", "application/json")
                .build())
        }
    }

    suspend fun getEmbedding(text: String): List<Double> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), 
            "{\"inputs\": \"${escapeJson(text)}\", \"options\": {\"wait_for_model\": true}}")
        val request = Request.Builder().url(Constants.HUGGINGFACE_API_URL).post(requestBody).build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) return@withContext emptyList()
                flattenResponse(body)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun summarizeText(text: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(),
            "{\"inputs\": \"${escapeJson(text)}\", \"parameters\": {\"max_length\": 150, \"min_length\": 40}}")
        val request = Request.Builder().url(SUMMARIZATION_URL).post(requestBody).build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) JsonParser.parseString(body).asJsonArray[0].asJsonObject.get("summary_text").asString
                else "Summary unavailable."
            }
        } catch (e: Exception) { "Summary error." }
    }

    /**
     * Real RAG Chat: Sends Context + History + New Question
     */
    suspend fun chatWithBook(context: String, history: String, question: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prompt = """
            Context: $context
            History: $history
            User: $question
            Assistant:
        """.trimIndent()

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(),
            "{\"inputs\": \"${escapeJson(prompt)}\", \"parameters\": {\"max_new_tokens\": 250, \"return_full_text\": false}}")

        val request = Request.Builder().url(CHAT_MODEL_URL).post(requestBody).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(body)
                    if (json.isJsonArray) json.asJsonArray[0].asJsonObject.get("generated_text").asString.trim()
                    else "I couldn't process that."
                } else "AI is currently busy."
            }
        } catch (e: Exception) { "AI Error: ${e.message}" }
    }

    private fun flattenResponse(response: String): List<Double> {
        val jsonElement = JsonParser.parseString(response)
        val result = mutableListOf<Double>()
        extractNumbers(jsonElement, result)
        return if (result.size >= 384) result.take(384) else result
    }

    private fun extractNumbers(element: JsonElement, list: MutableList<Double>) {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) list.add(element.asDouble)
        else if (element.isJsonArray) element.asJsonArray.forEach { extractNumbers(it, list) }
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
    }
}