package com.example.bookbuddy.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.bookbuddy.models.Book
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BookAIHelper(private val context: Context) {

    // Optional: Image labeler for book covers (if you want this feature)
    private var imageLabeler: FirebaseVisionImageLabeler? = null

    init {
        // Initialize image labeler (optional)
        try {
            imageLabeler = FirebaseVision.getInstance().onDeviceImageLabeler
        } catch (e: Exception) {
            Log.e("BookAI", "Error initializing image labeler", e)
        }
    }

    /**
     * FEATURE 1: Natural Language Search
     * Convert user query like "I want scary books for teenagers" into search terms
     */
    suspend fun processNaturalLanguageQuery(query: String): SearchIntent {
        return withContext(Dispatchers.IO) {
            val intent = SearchIntent()

            try {
                // Extract genre from common patterns
                try {
                    extractGenreFromQuery(query, intent)
                } catch (e: Exception) {
                    Log.e("BookAI", "Error extracting genre", e)
                }

                // Detect reading level
                try {
                    intent.readingLevel = detectReadingLevel(query)
                } catch (e: Exception) {
                    Log.e("BookAI", "Error detecting reading level", e)
                    intent.readingLevel = "intermediate"
                }

                // Simple keyword extraction
                try {
                    extractKeywordsFromQuery(query, intent)
                } catch (e: Exception) {
                    Log.e("BookAI", "Error extracting keywords", e)
                }

                Log.d("BookAI", "Processed query: $intent")

            } catch (e: Exception) {
                Log.e("BookAI", "Error processing query", e)
                // Fallback to keyword extraction
                try {
                    extractKeywordsFromQuery(query, intent)
                } catch (ex: Exception) {
                    // Ultimate fallback - empty intent
                }
            }

            return@withContext intent
        }
    }

    /**
     * FEATURE 2: Generate Book Summary
     * Create a concise summary from book description
     */
    fun generateBookSummary(book: Book): String {
        return try {
            when {
                book.summary.isNotEmpty() -> book.summary
                book.description.isNotEmpty() -> {
                    generateSummaryFromDescription(book.description)
                }
                else -> generateFallbackSummary(book)
            }
        } catch (e: Exception) {
            Log.e("BookAI", "Error generating summary", e)
            "Summary not available"
        }
    }

    private fun generateSummaryFromDescription(description: String): String {
        return try {
            val sentences = description.split("[.!?]".toRegex())
            when {
                sentences.size >= 3 -> {
                    "${safeGet(sentences, 0)}. ${safeGet(sentences, 1)}. ${safeGet(sentences, 2)}."
                }
                sentences.size == 2 -> {
                    "${safeGet(sentences, 0)}. ${safeGet(sentences, 1)}."
                }
                else -> description.take(150) + "..."
            }
        } catch (e: Exception) {
            description.take(100) + "..."
        }
    }

    private fun generateFallbackSummary(book: Book): String {
        return try {
            "A ${safeLowercase(book.genre)} book by ${book.author}."
        } catch (e: Exception) {
            "A book by ${book.author}"
        }
    }

    /**
     * FEATURE 3: Get Book Recommendations
     * Based on user's reading history and preferences
     */
    suspend fun getRecommendations(
        userHistory: List<UserInteraction>,
        allBooks: List<Book>,
        userPreferences: List<String> = emptyList()
    ): List<RecommendedBook> {
        return withContext(Dispatchers.IO) {
            try {
                val scoredBooks = mutableListOf<RecommendedBook>()

                // Get user's preferred genres from history
                val genrePreferences = try {
                    userHistory
                        .groupBy { it.bookGenre }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(3)
                        .map { it.first }
                } catch (e: Exception) {
                    emptyList()
                }

                // Get user's favorite authors
                val authorPreferences = try {
                    userHistory
                        .groupBy { it.bookAuthor }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(2)
                        .map { it.first }
                } catch (e: Exception) {
                    emptyList()
                }

                // Books user has already interacted with
                val readBookIds = try {
                    userHistory.map { it.bookId }.toSet()
                } catch (e: Exception) {
                    emptySet()
                }

                // Score each book
                allBooks.forEach { book ->
                    try {
                        if (book.id !in readBookIds) {
                            var score = 0.0

                            // Genre match (highest weight)
                            if (book.genre in genrePreferences) {
                                score += 10.0
                            }

                            // Author match
                            if (book.author in authorPreferences) {
                                score += 8.0
                            }

                            // Popularity score
                            score += book.timesBorrowed * 0.5

                            // Rating score
                            score += book.rating * 2

                            // Availability (prefer available books)
                            if (book.availableCopies > 0) {
                                score += 5.0
                            }

                            if (score > 0) {
                                scoredBooks.add(RecommendedBook(book, score))
                            }
                        }
                    } catch (e: Exception) {
                        // Skip this book if error
                    }
                }

                // Sort by score and return top 10
                try {
                    scoredBooks
                        .sortedByDescending { it.score }
                        .take(10)
                } catch (e: Exception) {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("BookAI", "Error getting recommendations", e)
                emptyList()
            }
        }
    }

    /**
     * FEATURE 4: Author & Genre Detection
     * Auto-detect genre and author from book details
     */
    fun detectGenreAndAuthor(book: Book): BookMetadata {
        return try {
            val metadata = BookMetadata()

            // Detect genre from description keywords
            try {
                metadata.detectedGenres = detectGenresFromText(book.description + " " + book.title)
            } catch (e: Exception) {
                metadata.detectedGenres = emptyList()
            }

            // Detect if author is well-known
            try {
                metadata.isWellKnownAuthor = isWellKnownAuthor(book.author)
            } catch (e: Exception) {
                metadata.isWellKnownAuthor = false
            }

            // Extract themes from description
            try {
                metadata.themes = extractThemes(book.description)
            } catch (e: Exception) {
                metadata.themes = emptyList()
            }

            metadata
        } catch (e: Exception) {
            Log.e("BookAI", "Error detecting genre/author", e)
            BookMetadata()
        }
    }

    /**
     * OPTIONAL FEATURE: Analyze book cover image
     * Uses Firebase ML Vision to detect labels from book covers
     */
    suspend fun analyzeBookCover(bitmap: Bitmap?): List<String> {
        return withContext(Dispatchers.IO) {
            val labels = mutableListOf<String>()

            if (bitmap == null) {
                return@withContext labels
            }

            try {
                if (imageLabeler == null) {
                    return@withContext labels
                }

                val image = FirebaseVisionImage.fromBitmap(bitmap)
                val results = try {
                    imageLabeler?.processImage(image)?.await()
                } catch (e: Exception) {
                    Log.e("BookAI", "Error processing image", e)
                    null
                }

                results?.forEach { label ->
                    try {
                        label.text?.let {
                            labels.add(it)
                            Log.d("BookAI", "Detected label: ${label.text} (${label.confidence})")
                        }
                    } catch (e: Exception) {
                        // Skip this label
                    }
                }

            } catch (e: Exception) {
                Log.e("BookAI", "Error analyzing image", e)
            }

            return@withContext labels
        }
    }

    /**
     * Helper Functions
     */
    private fun extractGenreFromQuery(query: String, intent: SearchIntent) {
        try {
            val genreKeywords = mapOf(
                "fantasy" to listOf("magic", "dragon", "wizard", "mythical", "elf", "spell", "sorcerer"),
                "sci-fi" to listOf("space", "alien", "future", "robot", "technology", "sci-fi", "science fiction", "cyber"),
                "horror" to listOf("scary", "horror", "terrifying", "ghost", "haunted", "fear", "creepy", "spooky"),
                "romance" to listOf("love", "romance", "relationship", "heart", "passion", "romantic", "dating"),
                "mystery" to listOf("mystery", "detective", "crime", "suspense", "thriller", "murder", "whodunit"),
                "biography" to listOf("life", "story", "autobiography", "memoir", "real story", "true story"),
                "history" to listOf("history", "historical", "ancient", "war", "king", "queen", "empire"),
                "children" to listOf("kids", "children", "young", "picture book", "child", "toddler", "baby"),
                "comedy" to listOf("funny", "humor", "comedy", "hilarious", "laugh", "joke"),
                "adventure" to listOf("adventure", "quest", "journey", "explore", "trek", "wild")
            )

            val queryLower = query.lowercase()
            genreKeywords.forEach { (genre, keywords) ->
                try {
                    if (keywords.any { queryLower.contains(it) }) {
                        intent.genres.add(genre)
                    }
                } catch (e: Exception) {
                    // Skip this genre
                }
            }
        } catch (e: Exception) {
            Log.e("BookAI", "Error in extractGenreFromQuery", e)
        }
    }

    private fun detectReadingLevel(query: String): String {
        return try {
            when {
                query.contains(Regex("beginner|easy|simple|basic|learn|new to|first time", RegexOption.IGNORE_CASE)) -> "beginner"
                query.contains(Regex("advanced|expert|complex|difficult|challenging|hard", RegexOption.IGNORE_CASE)) -> "advanced"
                query.contains(Regex("children|kids|young|teen|youth|young adult|ya", RegexOption.IGNORE_CASE)) -> "children"
                else -> "intermediate"
            }
        } catch (e: Exception) {
            "intermediate"
        }
    }

    private fun extractKeywordsFromQuery(query: String, intent: SearchIntent) {
        try {
            val words = query.lowercase().split(" ")
            val stopWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
                "of", "with", "by", "from", "as", "is", "was", "are", "were", "find",
                "me", "books", "like", "want", "some", "that", "this", "i", "can", "get",
                "please", "help", "looking", "need", "show", "give", "would", "could",
                "any", "all", "these", "those", "have", "has", "had", "been", "being")

            words.forEach { word ->
                try {
                    if (word.length > 2 && word !in stopWords) {
                        intent.topics.add(word)
                    }
                } catch (e: Exception) {
                    // Skip this word
                }
            }
        } catch (e: Exception) {
            Log.e("BookAI", "Error in extractKeywordsFromQuery", e)
        }
    }

    private fun detectGenresFromText(text: String): List<String> {
        return try {
            val detected = mutableListOf<String>()
            val textLower = text.lowercase()

            val genrePatterns = mapOf(
                "fantasy" to listOf("magic", "wizard", "dragon", "sword", "quest", "mythical", "sorcerer", "spell", "kingdom"),
                "science fiction" to listOf("space", "alien", "future", "robot", "sci-fi", "interstellar", "galaxy", "cyber", "dystopian"),
                "mystery" to listOf("detective", "crime", "murder", "suspect", "clue", "investigation", "mystery", "whodunit"),
                "romance" to listOf("love", "romance", "heart", "relationship", "passion", "kiss", "wedding", "marriage"),
                "horror" to listOf("horror", "scary", "terror", "fear", "haunted", "ghost", "monster", "vampire", "zombie"),
                "historical" to listOf("history", "historical", "century", "war", "ancient", "medieval", "king", "queen", "empire"),
                "adventure" to listOf("adventure", "journey", "quest", "travel", "explore", "discover", "voyage"),
                "biography" to listOf("biography", "autobiography", "life story", "memoir", "true story")
            )

            genrePatterns.forEach { (genre, keywords) ->
                try {
                    if (keywords.any { textLower.contains(it) }) {
                        detected.add(genre)
                    }
                } catch (e: Exception) {
                    // Skip this pattern
                }
            }

            detected.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isWellKnownAuthor(author: String): Boolean {
        return try {
            val wellKnownAuthors = setOf(
                "j.k. rowling", "jk rowling", "joanne rowling",
                "stephen king", "stephen king",
                "j.r.r. tolkien", "jrr tolkien", "tolkien",
                "george orwell", "orwell",
                "jane austen", "austen",
                "charles dickens", "dickens",
                "mark twain", "twain",
                "ernest hemingway", "hemingway",
                "agatha christie", "christie",
                "dan brown",
                "paulo coelho", "coelho",
                "haruki murakami", "murakami",
                "william shakespeare", "shakespeare",
                "leo tolstoy", "tolstoy",
                "fyodor dostoevsky", "dostoevsky"
            )
            wellKnownAuthors.any { author.lowercase().contains(it) }
        } catch (e: Exception) {
            false
        }
    }

    private fun extractThemes(text: String): List<String> {
        return try {
            val themes = mutableListOf<String>()
            val textLower = text.lowercase()

            val themePatterns = mapOf(
                "adventure" to listOf("adventure", "journey", "quest", "travel", "explore", "voyage", "trek"),
                "love" to listOf("love", "romance", "passion", "heart", "affection", "emotion"),
                "friendship" to listOf("friend", "friendship", "companion", "buddy", "together", "loyalty"),
                "war" to listOf("war", "battle", "fight", "soldier", "combat", "enemy", "conflict"),
                "death" to listOf("death", "die", "dying", "mortality", "loss", "grief", "mourning"),
                "coming of age" to listOf("grow", "mature", "become", "learn", "discover", "childhood", "teen"),
                "survival" to listOf("survive", "survival", "stranded", "alone", "wilderness", "endure"),
                "family" to listOf("family", "mother", "father", "parent", "child", "sister", "brother", "home"),
                "revenge" to listOf("revenge", "avenge", "vengeance", "retribution"),
                "betrayal" to listOf("betray", "traitor", "deceive", "lie", "trust")
            )

            themePatterns.forEach { (theme, keywords) ->
                try {
                    if (keywords.any { textLower.contains(it) }) {
                        themes.add(theme)
                    }
                } catch (e: Exception) {
                    // Skip this theme
                }
            }

            themes.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Safe helper functions
    private fun safeGet(list: List<String>, index: Int): String {
        return try {
            if (index < list.size) list[index] else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun safeLowercase(text: String): String {
        return try {
            text.lowercase()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Data Classes
     */
    data class SearchIntent(
        var language: String = "en",
        val bookTitles: MutableList<String> = mutableListOf(),
        val authors: MutableList<String> = mutableListOf(),
        val genres: MutableList<String> = mutableListOf(),
        val topics: MutableList<String> = mutableListOf(),
        var readingLevel: String = "intermediate"
    )

    data class RecommendedBook(
        val book: Book,
        val score: Double
    )

    data class BookMetadata(
        var detectedGenres: List<String> = emptyList(),
        var isWellKnownAuthor: Boolean = false,
        var themes: List<String> = emptyList()
    )

    data class UserInteraction(
        val bookId: String,
        val bookTitle: String,
        val bookAuthor: String,
        val bookGenre: String,
        val action: String, // "view", "borrow", "return", "rate"
        val rating: Float = 0f,
        val timestamp: Long
    )
}