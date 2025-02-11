package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDay: Boolean = false,
    val category: ScheduleCategory = ScheduleCategory.OTHER,
    val reminderTime: LocalDateTime? = null,
    val isReminded: Boolean = false,
    val color: Int? = null,
    val location: String = "",
    val priority: SchedulePriority = SchedulePriority.MEDIUM,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatInterval: Int = 0,
    val repeatUntil: LocalDateTime? = null
)

enum class ScheduleCategory {
    STUDY, EXAM, HOMEWORK, MEETING, CLASS, OTHER
}

enum class SchedulePriority {
    LOW, MEDIUM, HIGH
}

enum class RepeatType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
} 