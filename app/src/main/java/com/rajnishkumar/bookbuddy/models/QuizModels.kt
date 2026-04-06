package com.rajnishkumar.bookbuddy.models

import com.google.gson.annotations.SerializedName

data class Quiz(
    @SerializedName("questions")
    val questions: List<QuizQuestion>
)

data class QuizQuestion(
    @SerializedName("question")
    val question: String,

    @SerializedName("options")
    val options: List<String>,

    @SerializedName("correct_answer")
    val correctAnswer: String,

    // Fields for state persistence
    var selectedAnswer: String? = null,
    var isSubmitted: Boolean = false
)