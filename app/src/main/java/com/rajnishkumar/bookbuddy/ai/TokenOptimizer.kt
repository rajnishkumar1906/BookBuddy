package com.rajnishkumar.bookbuddy.ai

import android.util.Log

object TokenOptimizer {
    private const val TAG = "TokenOptimizer"

    // Common English stopwords to remove to save tokens
    private val stopwords = setOf(
        "a", "an", "the", "and", "or", "but", "if", "then", "else", "when", "at", "from", "by", 
        "for", "with", "about", "against", "between", "into", "through", "during", "before", 
        "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", 
        "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", 
        "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", 
        "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", 
        "will", "just", "don", "should", "now", "i", "me", "my", "myself", "we", "our", "ours", 
        "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", 
        "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", 
        "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", 
        "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", 
        "has", "had", "having", "do", "does", "did", "doing"
    )

    /**
     * Compresses text by removing stopwords and special characters to save LLM tokens.
     */
    fun compress(text: String): String {
        if (text.isEmpty()) return ""
        
        val originalWords = text.split(Regex("\\s+"))
        val filteredWords = originalWords.filter { word ->
            val cleanWord = word.lowercase().replace(Regex("[^a-z]"), "")
            cleanWord.isNotEmpty() && !stopwords.contains(cleanWord)
        }

        val compressed = filteredWords.joinToString(" ")
        val saving = 100 - (compressed.length.toFloat() / text.length.toFloat() * 100).toInt()
        
        Log.d(TAG, "Compressed text saved approximately $saving% tokens.")
        return compressed
    }

    /**
     * Extracts only the most important sentences (Lexical Summarization)
     */
    fun extractKeySentences(text: String, maxSentences: Int = 3): String {
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        if (sentences.size <= maxSentences) return text
        
        // Simple heuristic: Take first and last, plus middle or highest length
        val result = mutableListOf<String>()
        result.add(sentences.first())
        if (sentences.size > 2) {
            val middle = sentences.subList(1, sentences.size - 1).maxByOrNull { it.length }
            middle?.let { result.add(it) }
        }
        result.add(sentences.last())
        
        return result.take(maxSentences).joinToString(" ")
    }
}