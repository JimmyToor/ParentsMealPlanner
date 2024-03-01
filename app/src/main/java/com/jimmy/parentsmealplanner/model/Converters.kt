package com.jimmy.parentsmealplanner.model

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Int? {
        return value?.toEpochDays()
    }
    @TypeConverter
    fun intToLocalDate(value: Int?): LocalDate? {
        return value?.let { LocalDate.fromEpochDays(it) }
    }
}