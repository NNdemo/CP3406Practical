package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myapplication.ui.viewmodels.EventDetail
import com.example.myapplication.ui.viewmodels.EventStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "schedules",
    indices = [Index(value = ["title", "location", "startTime", "endTime"], unique = true)]
)
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
) {
    // 将Schedule转换为EventDetail
    fun toEventDetail(): EventDetail {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val readableFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
        
        return EventDetail(
            title = title,
            startTime = startTime.format(formatter),
            endTime = endTime.format(formatter),
            category = category.name,
            location = location,
            priority = priority.name,
            description = description,
            status = EventStatus.PENDING,
            readableStartTime = startTime.format(readableFormatter),
            readableEndTime = endTime.format(readableFormatter)
        )
    }
}

enum class ScheduleCategory {
    STUDY, WORK, ENTERTAINMENT, MEETING, OTHER
}

enum class SchedulePriority {
    LOW, MEDIUM, HIGH
}

enum class RepeatType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
} 