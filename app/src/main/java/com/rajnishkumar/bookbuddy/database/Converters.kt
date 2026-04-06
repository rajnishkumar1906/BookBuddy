package com.rajnishkumar.bookbuddy.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rajnishkumar.bookbuddy.models.Quiz

@Suppress("unused")
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromQuiz(quiz: Quiz?): String? {
        return gson.toJson(quiz)
    }

    @TypeConverter
    fun toQuiz(quizString: String?): Quiz? {
        return if (quizString == null) null else gson.fromJson(quizString, Quiz::class.java)
    }

    @TypeConverter
    fun fromDoubleList(list: List<Double>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toDoubleList(value: String?): List<Double>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Double>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toTimestamp(value: String?): Long? {
        return value?.toLongOrNull()
    }
}
