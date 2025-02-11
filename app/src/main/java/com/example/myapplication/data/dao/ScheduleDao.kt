package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.Schedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE date(startTime) = date(:date) ORDER BY startTime")
    fun getSchedulesForDate(date: LocalDateTime): Flow<List<Schedule>>

    @Query("""
        SELECT * FROM schedules 
        WHERE startTime BETWEEN :start AND :end 
        OR endTime BETWEEN :start AND :end
        OR (startTime <= :start AND endTime >= :end)
        ORDER BY startTime
    """)
    fun getSchedulesBetween(start: LocalDateTime, end: LocalDateTime): Flow<List<Schedule>>

    @Query("""
        SELECT * FROM schedules 
        WHERE reminderTime IS NOT NULL 
        AND reminderTime > :now 
        AND isReminded = 0
        ORDER BY reminderTime
    """)
    fun getUpcomingReminders(now: LocalDateTime): Flow<List<Schedule>>

    @Insert
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)

    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: Long): Schedule?

    @Query("UPDATE schedules SET isReminded = 1 WHERE id = :scheduleId")
    suspend fun markReminderAsShown(scheduleId: Long)

    @Query("""
        SELECT * FROM schedules 
        WHERE category = :category 
        AND startTime >= :startDate 
        ORDER BY startTime
    """)
    fun getSchedulesByCategory(category: String, startDate: LocalDateTime): Flow<List<Schedule>>
} 