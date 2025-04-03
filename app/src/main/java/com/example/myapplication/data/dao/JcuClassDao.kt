package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.JcuClass
import com.example.myapplication.data.model.JcuClassStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface JcuClassDao {
    @Query("SELECT * FROM jcu_classes ORDER BY startTime ASC")
    fun getAllClasses(): Flow<List<JcuClass>>
    
    @Query("SELECT * FROM jcu_classes WHERE startTime >= :startDateTime ORDER BY startTime ASC")
    fun getUpcomingClasses(startDateTime: LocalDateTime): Flow<List<JcuClass>>
    
    @Query("SELECT * FROM jcu_classes WHERE startTime < :endDateTime ORDER BY startTime DESC")
    fun getPastClasses(endDateTime: LocalDateTime): Flow<List<JcuClass>>
    
    @Query("SELECT * FROM jcu_classes WHERE courseCode = :courseCode ORDER BY startTime ASC")
    fun getClassesByCourseCode(courseCode: String): Flow<List<JcuClass>>
    
    @Query("SELECT * FROM jcu_classes WHERE status = :status ORDER BY startTime ASC")
    fun getClassesByStatus(status: JcuClassStatus): Flow<List<JcuClass>>
    
    @Query("SELECT * FROM jcu_classes WHERE startTime BETWEEN :startDateTime AND :endDateTime ORDER BY startTime ASC")
    fun getClassesInDateRange(startDateTime: LocalDateTime, endDateTime: LocalDateTime): Flow<List<JcuClass>>
    
    @Query("SELECT DISTINCT courseCode FROM jcu_classes")
    fun getAllCourseCodes(): Flow<List<String>>
    
    @Query("SELECT * FROM jcu_classes WHERE id = :id")
    suspend fun getClassById(id: Long): JcuClass?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(classes: List<JcuClass>): List<Long>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jcuClass: JcuClass): Long
    
    @Update
    suspend fun update(jcuClass: JcuClass)
    
    @Delete
    suspend fun delete(jcuClass: JcuClass)
    
    @Query("DELETE FROM jcu_classes")
    suspend fun deleteAll()
    
    @Query("DELETE FROM jcu_classes WHERE courseCode = :courseCode")
    suspend fun deleteClassesByCourseCode(courseCode: String)
    
    // 根据课程代码、开始时间和结束时间查找课程，用于检查是否已存在
    @Query("SELECT * FROM jcu_classes WHERE courseCode = :courseCode AND startTime = :startTime AND endTime = :endTime LIMIT 1")
    suspend fun findExistingClass(courseCode: String, startTime: LocalDateTime, endTime: LocalDateTime): JcuClass?
    
    @Transaction
    suspend fun upsertClass(jcuClass: JcuClass): Long {
        val existingClass = findExistingClass(jcuClass.courseCode, jcuClass.startTime, jcuClass.endTime)
        return if (existingClass != null) {
            // 如果存在，更新状态
            val updatedClass = existingClass.copy(
                status = jcuClass.status,
                location = jcuClass.location,
                lastUpdated = LocalDateTime.now()
            )
            update(updatedClass)
            existingClass.id
        } else {
            // 不存在则插入
            insert(jcuClass)
        }
    }
    
    @Transaction
    suspend fun syncClasses(newClasses: List<JcuClass>) {
        // 更新或插入新课程
        for (jcuClass in newClasses) {
            upsertClass(jcuClass)
        }
    }
} 