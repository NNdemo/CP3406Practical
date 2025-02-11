package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.StudySessionDao
import com.example.myapplication.data.model.StudySession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudySessionRepository @Inject constructor(
    private val studySessionDao: StudySessionDao
) {
    fun getAllSessions(): Flow<List<StudySession>> = studySessionDao.getAllSessions()
    
    fun getActiveSessions(): Flow<List<StudySession>> = studySessionDao.getActiveSessions()
    
    fun getTotalStudyTimeForDate(date: LocalDateTime): Flow<Int?> = 
        studySessionDao.getTotalStudyTimeForDate(date)
    
    fun getTotalStudyTimeBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<Int?> =
        studySessionDao.getTotalStudyTimeBetweenDates(startDate, endDate)
    
    suspend fun startStudySession(subject: String, description: String = ""): Long {
        val session = StudySession(
            startTime = LocalDateTime.now(),
            subject = subject,
            description = description
        )
        return studySessionDao.insert(session)
    }
    
    suspend fun endStudySession(sessionId: Long) {
        val session = studySessionDao.getSessionById(sessionId)
        session?.let {
            val endTime = LocalDateTime.now()
            val duration = java.time.Duration.between(it.startTime, endTime).toMinutes().toInt()
            val updatedSession = it.copy(
                endTime = endTime,
                duration = duration,
                isCompleted = true
            )
            studySessionDao.update(updatedSession)
        }
    }
    
    suspend fun deleteSession(session: StudySession) = studySessionDao.delete(session)
    
    suspend fun getSessionById(sessionId: Long): StudySession? = studySessionDao.getSessionById(sessionId)
} 