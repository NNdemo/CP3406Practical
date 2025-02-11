package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.ScheduleDao
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.data.model.RepeatType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao
) {
    fun getSchedulesForDate(date: LocalDateTime): Flow<List<Schedule>> =
        scheduleDao.getSchedulesForDate(date)

    fun getSchedulesBetween(start: LocalDateTime, end: LocalDateTime): Flow<List<Schedule>> =
        scheduleDao.getSchedulesBetween(start, end)

    fun getUpcomingReminders(now: LocalDateTime = LocalDateTime.now()): Flow<List<Schedule>> =
        scheduleDao.getUpcomingReminders(now)

    suspend fun createSchedule(
        title: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        description: String = "",
        category: ScheduleCategory = ScheduleCategory.OTHER,
        isAllDay: Boolean = false,
        reminderTime: LocalDateTime? = null,
        location: String = "",
        priority: SchedulePriority = SchedulePriority.MEDIUM,
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 0,
        repeatUntil: LocalDateTime? = null
    ): Long {
        val schedule = Schedule(
            title = title,
            startTime = startTime,
            endTime = endTime,
            description = description,
            category = category,
            isAllDay = isAllDay,
            reminderTime = reminderTime,
            location = location,
            priority = priority,
            repeatType = repeatType,
            repeatInterval = repeatInterval,
            repeatUntil = repeatUntil
        )
        return scheduleDao.insert(schedule)
    }

    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.update(schedule)

    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.delete(schedule)

    suspend fun getScheduleById(scheduleId: Long): Schedule? = scheduleDao.getScheduleById(scheduleId)

    suspend fun markReminderAsShown(scheduleId: Long) = scheduleDao.markReminderAsShown(scheduleId)

    fun getSchedulesByCategory(category: ScheduleCategory, startDate: LocalDateTime = LocalDateTime.now()): Flow<List<Schedule>> =
        scheduleDao.getSchedulesByCategory(category.name, startDate)
} 