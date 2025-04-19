package com.example.budget.utils

import androidx.room.TypeConverter
import java.util.*

class Converters {

    @TypeConverter
    fun toString(date: Date?): String? {
        return date?.toString()
    }
} 