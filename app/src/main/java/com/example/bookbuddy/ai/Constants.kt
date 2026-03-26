package com.example.bookbuddy.ai

object Constants {
    // Gemini API
    const val GEMINI_API_KEY = "AIzaSyCwzXw5mIl83uuVY0CJhWSzBiwHDk_c-7s"
    const val GEMINI_SUMMARY_MODEL = "gemini-2.5-flash"

    // Hugging Face settings
    const val HUGGINGFACE_MODEL = "sentence-transformers/all-MiniLM-L6-v2"  // 👈 Model name
    const val EMBEDDING_DIMENSIONS = 384                                     // 👈 Dimensions
    const val HUGGINGFACE_DELAY_MS = 1000L

    // Search settings
    const val MAX_SEARCH_RESULTS = 10
    const val MIN_SIMILARITY_THRESHOLD = 0.3
}