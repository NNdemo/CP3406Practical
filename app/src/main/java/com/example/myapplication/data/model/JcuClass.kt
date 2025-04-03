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
            description = "课程状态: ${status.description}",
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
                    throw IllegalArgumentException("时间范围格式无效: $timeRange")
                }
                
                val startTimeStr = times[0]
                val endTimeStr = times[1]
                
                if (startTimeStr.length < 4 || endTimeStr.length < 4) {
                    throw IllegalArgumentException("时间格式无效，需要至少4位数字: $timeRange")
                }
                
                val startHour = startTimeStr.substring(0, 2).toIntOrNull() 
                    ?: throw NumberFormatException("无法解析小时: ${startTimeStr.substring(0, 2)}")
                    
                val startMinute = startTimeStr.substring(2).toIntOrNull()
                    ?: throw NumberFormatException("无法解析分钟: ${startTimeStr.substring(2)}")
                    
                val endHour = endTimeStr.substring(0, 2).toIntOrNull()
                    ?: throw NumberFormatException("无法解析小时: ${endTimeStr.substring(0, 2)}")
                    
                val endMinute = endTimeStr.substring(2).toIntOrNull()
                    ?: throw NumberFormatException("无法解析分钟: ${endTimeStr.substring(2)}")
                
                // 验证时间范围
                if (startHour < 0 || startHour > 23 || startMinute < 0 || startMinute > 59 ||
                    endHour < 0 || endHour > 23 || endMinute < 0 || endMinute > 59) {
                    throw IllegalArgumentException("时间值超出范围: $timeRange")
                }
                
                // 解析日期
                val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
                val parsedDate = try {
                    java.time.LocalDate.parse(date, formatter)
                } catch (e: Exception) {
                    throw IllegalArgumentException("无法解析日期: $date", e)
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
                println("解析JCU日期时间失败: $dateStr, $timeRange - 错误: ${e.message}")
                
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
    PLANNED("计划中"),
    IN_PROGRESS("进行中"),
    UPCOMING("即将开始"),
    COMPLETED("已完成"),
    ABSENT("缺勤"),
    UNKNOWN("未知")
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