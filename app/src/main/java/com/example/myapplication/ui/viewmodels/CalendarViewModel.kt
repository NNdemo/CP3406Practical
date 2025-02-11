package com.example.myapplication.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.data.repository.ScheduleRepository
import com.example.myapplication.worker.ReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingSchedule: Schedule? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    application: Application,
    private val scheduleRepository: ScheduleRepository
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadSchedules()
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val startOfDay = LocalDateTime.of(_uiState.value.selectedDate, LocalTime.MIN)
            val endOfDay = LocalDateTime.of(_uiState.value.selectedDate, LocalTime.MAX)
            
            scheduleRepository.getSchedulesBetween(startOfDay, endOfDay)
                .collect { schedules ->
                    _uiState.value = _uiState.value.copy(
                        schedules = schedules,
                        isLoading = false
                    )
                }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingSchedule = null
        )
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            editingSchedule = null
        )
    }

    fun editSchedule(schedule: Schedule) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingSchedule = schedule
        )
    }

    fun createOrUpdateSchedule(
        title: String,
        description: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        isAllDay: Boolean,
        category: ScheduleCategory,
        reminderTime: LocalDateTime?,
        location: String,
        priority: SchedulePriority
    ) {
        viewModelScope.launch {
            val existingSchedule = _uiState.value.editingSchedule
            if (existingSchedule != null) {
                // 更新现有日程
                val updatedSchedule = existingSchedule.copy(
                    title = title,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    isAllDay = isAllDay,
                    category = category,
                    reminderTime = reminderTime,
                    location = location,
                    priority = priority
                )
                scheduleRepository.updateSchedule(updatedSchedule)
                
                // 更新提醒
                if (reminderTime != null) {
                    ReminderWorker.scheduleReminder(getApplication(), updatedSchedule.id, reminderTime)
                } else {
                    ReminderWorker.cancelReminder(getApplication(), updatedSchedule.id)
                }
            } else {
                // 创建新日程
                val scheduleId = scheduleRepository.createSchedule(
                    title = title,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    isAllDay = isAllDay,
                    category = category,
                    reminderTime = reminderTime,
                    location = location,
                    priority = priority
                )
                
                // 设置提醒
                if (reminderTime != null) {
                    ReminderWorker.scheduleReminder(getApplication(), scheduleId, reminderTime)
                }
            }
            hideDialog()
            loadSchedules()
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            // 取消提醒
            ReminderWorker.cancelReminder(getApplication(), schedule.id)
            // 删除日程
            scheduleRepository.deleteSchedule(schedule)
            loadSchedules()
        }
    }

    fun toggleScheduleCompletion(schedule: Schedule) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(isReminded = !schedule.isReminded)
            scheduleRepository.updateSchedule(updatedSchedule)
            loadSchedules()
        }
    }

    fun updateScheduleTime(schedule: Schedule, newTime: LocalTime) {
        viewModelScope.launch {
            val duration = java.time.Duration.between(
                schedule.startTime.toLocalTime(),
                schedule.endTime.toLocalTime()
            )
            val newStartDateTime = LocalDateTime.of(schedule.startTime.toLocalDate(), newTime)
            val newEndDateTime = newStartDateTime.plusNanos(duration.toNanos())
            
            // 如果有提醒时间，也相应调整
            val newReminderTime = schedule.reminderTime?.let { oldReminderTime ->
                val reminderOffset = java.time.Duration.between(schedule.startTime, oldReminderTime)
                newStartDateTime.plus(reminderOffset)
            }
            
            val updatedSchedule = schedule.copy(
                startTime = newStartDateTime,
                endTime = newEndDateTime,
                reminderTime = newReminderTime
            )
            
            scheduleRepository.updateSchedule(updatedSchedule)
            
            // 更新提醒
            if (newReminderTime != null) {
                ReminderWorker.scheduleReminder(getApplication(), updatedSchedule.id, newReminderTime)
            }
            
            loadSchedules()
        }
    }
} 