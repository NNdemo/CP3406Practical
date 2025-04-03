package com.example.myapplication.data.repository

import com.example.myapplication.data.AppSettings
import com.example.myapplication.data.dao.JcuClassDao
import com.example.myapplication.data.model.JcuClass
import com.example.myapplication.data.model.JcuClassStatistics
import com.example.myapplication.data.model.JcuClassStatus
import com.example.myapplication.data.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JcuClassRepository @Inject constructor(
    private val jcuClassDao: JcuClassDao,
    private val appSettings: AppSettings
) {
    // 获取所有课程
    fun getAllClasses(): Flow<List<JcuClass>> = jcuClassDao.getAllClasses()
    
    // 获取即将到来的课程
    fun getUpcomingClasses(startDateTime: LocalDateTime = LocalDateTime.now()): Flow<List<JcuClass>> =
        jcuClassDao.getUpcomingClasses(startDateTime)
    
    // 获取过去的课程
    fun getPastClasses(endDateTime: LocalDateTime = LocalDateTime.now()): Flow<List<JcuClass>> =
        jcuClassDao.getPastClasses(endDateTime)
    
    // 获取指定课程代码的所有课程
    fun getClassesByCourseCode(courseCode: String): Flow<List<JcuClass>> =
        jcuClassDao.getClassesByCourseCode(courseCode)
    
    // 获取指定状态的所有课程
    fun getClassesByStatus(status: JcuClassStatus): Flow<List<JcuClass>> =
        jcuClassDao.getClassesByStatus(status)
    
    // 获取指定日期范围内的课程
    fun getClassesInDateRange(startDateTime: LocalDateTime, endDateTime: LocalDateTime): Flow<List<JcuClass>> =
        jcuClassDao.getClassesInDateRange(startDateTime, endDateTime)
    
    // 获取所有课程代码
    fun getAllCourseCodes(): Flow<List<String>> = jcuClassDao.getAllCourseCodes()
    
    // 根据ID获取课程
    suspend fun getClassById(id: Long): JcuClass? = jcuClassDao.getClassById(id)
    
    // 插入或更新课程
    suspend fun upsertClass(jcuClass: JcuClass): Long = jcuClassDao.upsertClass(jcuClass)
    
    // 批量插入或更新课程
    suspend fun syncClasses(classes: List<JcuClass>) = jcuClassDao.syncClasses(classes)
    
    // 删除课程
    suspend fun deleteClass(jcuClass: JcuClass) = jcuClassDao.delete(jcuClass)
    
    // 清空所有课程
    suspend fun deleteAllClasses() = jcuClassDao.deleteAll()
    
    // 删除指定课程代码的所有课程
    suspend fun deleteClassesByCourseCode(courseCode: String) = jcuClassDao.deleteClassesByCourseCode(courseCode)
    
    // 出勤率数据
    private var webClassAttendanceRate: Float = 0f
    private var webCampusAttendanceRate: Float = 0f
    
    // 更新网页获取的出勤率
    fun updateWebAttendanceRates(classAttendanceRate: Float, campusAttendanceRate: Float) {
        webClassAttendanceRate = classAttendanceRate
        webCampusAttendanceRate = campusAttendanceRate
        
        // 保存到SharedPreferences
        appSettings.updateWebAttendanceRatesSynchronously(classAttendanceRate, campusAttendanceRate)
    }
    
    // 从设置中加载网页出勤率数据
    suspend fun loadWebAttendanceRates() {
        val settings = appSettings.settingsFlow.first()
        webClassAttendanceRate = settings.webClassAttendanceRate
        webCampusAttendanceRate = settings.webCampusAttendanceRate
    }
    
    // 获取课程统计信息
    fun getClassStatistics(): Flow<JcuClassStatistics> = getAllClasses().map { classes ->
        JcuClassStatistics.calculate(
            classes = classes,
            webClassAttendanceRate = webClassAttendanceRate,
            webCampusAttendanceRate = webCampusAttendanceRate
        )
    }
    
    // 获取特定课程的统计信息
    fun getCourseStatistics(courseCode: String): Flow<JcuClassStatistics> =
        getClassesByCourseCode(courseCode).map { classes ->
            JcuClassStatistics.calculate(
                classes = classes,
                webClassAttendanceRate = webClassAttendanceRate,
                webCampusAttendanceRate = webCampusAttendanceRate
            )
        }
    
    // 将JCU课程转换为日程项，可以直接添加到主日历中
    fun getClassesAsSchedules(): Flow<List<Schedule>> = getAllClasses().map { classes ->
        classes.map { it.toSchedule() }
    }
} 