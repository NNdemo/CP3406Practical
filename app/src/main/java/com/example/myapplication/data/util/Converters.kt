package com.example.myapplication.data.util

import androidx.room.TypeConverter
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.data.model.RepeatType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun fromScheduleCategory(value: ScheduleCategory): String {
        return value.name
    }

    @TypeConverter
    fun toScheduleCategory(value: String): ScheduleCategory {
        return enumValueOf(value)
    }

    @TypeConverter
    fun fromSchedulePriority(value: SchedulePriority): String {
        return value.name
    }

    @TypeConverter
    fun toSchedulePriority(value: String): SchedulePriority {
        return enumValueOf(value)
    }

    @TypeConverter
    fun fromRepeatType(value: RepeatType): String {
        return value.name
    }

    @TypeConverter
    fun toRepeatType(value: String): RepeatType {
        return enumValueOf(value)
    }
} 