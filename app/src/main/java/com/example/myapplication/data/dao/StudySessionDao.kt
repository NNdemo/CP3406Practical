package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.StudySession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE isCompleted = 0")
    fun getActiveSessions(): Flow<List<StudySession>>

    @Query("SELECT SUM(duration) FROM study_sessions WHERE isCompleted = 1 AND date(startTime) = date(:date)")
    fun getTotalStudyTimeForDate(date: LocalDateTime): Flow<Int?>

    @Insert
    suspend fun insert(session: StudySession): Long

    @Update
    suspend fun update(session: StudySession)

    @Delete
    suspend fun delete(session: StudySession)

    @Query("SELECT * FROM study_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): StudySession?

    @Query("""
        SELECT SUM(duration) FROM study_sessions 
        WHERE isCompleted = 1 
        AND startTime BETWEEN :startDate AND :endDate
    """)
    fun getTotalStudyTimeBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<Int?>
} 