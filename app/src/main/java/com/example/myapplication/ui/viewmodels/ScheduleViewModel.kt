package com.example.myapplication.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    // 添加Flow以跟踪当前日期的日程
    private val _currentDate = MutableStateFlow(LocalDateTime.now())
    val currentDate = _currentDate.asStateFlow()
    
    // 当前日程表列表
    val currentSchedules = _currentDate.flatMapLatest { date ->
        scheduleRepository.getSchedulesForDate(date)
    }
    
    // 刷新日程表列表
    fun refreshSchedules(date: LocalDateTime) {
        println("ScheduleViewModel: 刷新日程表，请求日期: $date")
        _currentDate.value = date
        println("ScheduleViewModel: 已更新查询日期")
    }

    // 添加日程方法
    fun addSchedule(schedule: Schedule): Long {
        println("ScheduleViewModel: 添加日程 - 标题=\"${schedule.title}\", 开始时间=${schedule.startTime}, 结束时间=${schedule.endTime}")
        
        // 使用变量来保存ID
        var resultId: Long = -1
        
        // 在viewModelScope中执行，不再使用runBlocking
        viewModelScope.launch {
            try {
                println("ScheduleViewModel: 开始添加日程到数据库")
                resultId = scheduleRepository.createSchedule(
                    title = schedule.title,
                    startTime = schedule.startTime,
                    endTime = schedule.endTime,
                    description = schedule.description,
                    category = schedule.category,
                    isAllDay = schedule.isAllDay,
                    reminderTime = schedule.reminderTime,
                    location = schedule.location,
                    priority = schedule.priority,
                    repeatType = schedule.repeatType,
                    repeatInterval = schedule.repeatInterval,
                    repeatUntil = schedule.repeatUntil
                )
                println("ScheduleViewModel: 日程添加成功 - ID=$resultId, 标题=\"${schedule.title}\"")
                
                // 添加后自动刷新当天的日程
                refreshSchedules(_currentDate.value)
            } catch (e: Exception) {
                println("ScheduleViewModel: 添加日程失败 - ${e.javaClass.name}: ${e.message}")
                e.printStackTrace()
            }
        }
        
        // 因为是异步操作，这里总是返回-1
        // 客户端代码需要通过观察currentSchedules来获取更新后的数据
        return -1
    }
} 