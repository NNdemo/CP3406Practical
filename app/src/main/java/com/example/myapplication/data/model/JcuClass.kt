package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * JCU课程类模型
 */
@Entity(tableName = "jcu_classes")
data class JcuClass(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseCode: String,           // 课程代码，如CP3406
    val courseName: String,           // 课程名称
    val startTime: LocalDateTime,     // 上课开始时间
    val endTime: LocalDateTime,       // 上课结束时间
    val location: String,             // 上课地点
    val status: JcuClassStatus,       // 出勤状态
    val lastUpdated: LocalDateTime = LocalDateTime.now() // 最后更新时间
) {
    /**
     * 转换为Schedule对象
     */
    fun toSchedule(): Schedule {
        return Schedule(
            title = "$courseCode - $courseName",
            description = "Class Status: ${status.description}",
            startTime = startTime,
            endTime = endTime,
            location = location,
            category = ScheduleCategory.STUDY,
            priority = when(status) {
                JcuClassStatus.ABSENT -> SchedulePriority.HIGH
                JcuClassStatus.PLANNED -> SchedulePriority.MEDIUM
                else -> SchedulePriority.LOW
            }
        )
    }
    
    companion object {
        /**
         * 解析JCU时间格式
         */
        fun parseJcuDateTime(dateStr: String, timeRange: String): Pair<LocalDateTime, LocalDateTime> {
            try {
                // 日期格式如: "26-Mar" 或 "26-Mar-2023"
                val year = LocalDateTime.now().year
                val date = if (dateStr.contains("-")) {
                    val parts = dateStr.split("-")
                    if (parts.size > 2) {
                        // 有年份
                        dateStr
                    } else {
                        // 没有年份，添加当前年份
                        "$dateStr-$year"
                    }
                } else {
                    "$dateStr-$year"
                }
                
                // 时间格式如: "1300-1430" (13:00-14:30)
                val times = timeRange.split("-")
                if (times.size < 2) {
                    throw IllegalArgumentException("Invalid time range format: $timeRange")
                }
                
                val startTimeStr = times[0]
                val endTimeStr = times[1]
                
                if (startTimeStr.length < 4 || endTimeStr.length < 4) {
                    throw IllegalArgumentException("Invalid time format, at least 4 digits required: $timeRange")
                }
                
                val startHour = startTimeStr.substring(0, 2).toIntOrNull() 
                    ?: throw NumberFormatException("Unable to parse hours: ${startTimeStr.substring(0, 2)}")
                    
                val startMinute = startTimeStr.substring(2).toIntOrNull()
                    ?: throw NumberFormatException("Unable to parse minutes: ${startTimeStr.substring(2)}")
                    
                val endHour = endTimeStr.substring(0, 2).toIntOrNull()
                    ?: throw NumberFormatException("Unable to parse hours: ${endTimeStr.substring(0, 2)}")
                    
                val endMinute = endTimeStr.substring(2).toIntOrNull()
                    ?: throw NumberFormatException("Unable to parse minutes: ${endTimeStr.substring(2)}")
                
                // 验证时间范围
                if (startHour < 0 || startHour > 23 || startMinute < 0 || startMinute > 59 ||
                    endHour < 0 || endHour > 23 || endMinute < 0 || endMinute > 59) {
                    throw IllegalArgumentException("Time values out of range: $timeRange")
                }
                
                // 解析日期
                val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
                val parsedDate = try {
                    java.time.LocalDate.parse(date, formatter)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Unable to parse date: $date", e)
                }
                
                // 创建日期时间对象
                val startDateTime = LocalDateTime.of(
                    parsedDate.year, parsedDate.month, parsedDate.dayOfMonth,
                    startHour, startMinute
                )
                
                val endDateTime = LocalDateTime.of(
                    parsedDate.year, parsedDate.month, parsedDate.dayOfMonth,
                    endHour, endMinute
                )
                
                return Pair(startDateTime, endDateTime)
            } catch (e: Exception) {
                println("Failed to parse JCU date time: $dateStr, $timeRange - Error: ${e.message}")
                
                // 出错时返回当前时间作为默认值
                val now = LocalDateTime.now()
                return Pair(now, now.plusHours(1))
            }
        }
    }
}

/**
 * 课程状态枚举
 */
enum class JcuClassStatus(val description: String) {
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    ABSENT("Absent"),
    UNKNOWN("Unknown")
}

/**
 * JCU课程统计信息
 */
data class JcuClassStatistics(
    val totalClasses: Int = 0,
    val completedClasses: Int = 0,
    val absentClasses: Int = 0,
    val plannedClasses: Int = 0,
    val attendanceRate: Float = 0f,
    val webClassAttendanceRate: Float = 0f,  // 从网页获取的课程出勤率
    val webCampusAttendanceRate: Float = 0f  // 从网页获取的校园出勤率
) {
    companion object {
        fun calculate(
            classes: List<JcuClass>,
            webClassAttendanceRate: Float = 0f,
            webCampusAttendanceRate: Float = 0f
        ): JcuClassStatistics {
            if (classes.isEmpty()) {
                return JcuClassStatistics(
                    webClassAttendanceRate = webClassAttendanceRate,
                    webCampusAttendanceRate = webCampusAttendanceRate
                )
            }
            
            val total = classes.size
            val completed = classes.count { it.status == JcuClassStatus.COMPLETED }
            val absent = classes.count { it.status == JcuClassStatus.ABSENT }
            val planned = classes.count { it.status == JcuClassStatus.PLANNED }
            
            // 计算出勤率：已完成 / (已完成 + 缺勤)
            val attendedClasses = completed + absent
            val attendanceRate = if (attendedClasses > 0) {
                completed.toFloat() / attendedClasses
            } else {
                0f
            }
            
            return JcuClassStatistics(
                totalClasses = total,
                completedClasses = completed,
                absentClasses = absent,
                plannedClasses = planned,
                attendanceRate = attendanceRate,
                webClassAttendanceRate = webClassAttendanceRate,
                webCampusAttendanceRate = webCampusAttendanceRate
            )
        }
    }
} 