package com.example.myapplication.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.data.repository.ScheduleRepository
import com.example.myapplication.ui.screens.ViewType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class TimelineUiState(
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = false,
    val showAddScheduleDialog: Boolean = false,
    val editingSchedule: Schedule? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val viewType: ViewType = ViewType.DAY
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadSchedules()
    }

    fun updateViewType(viewType: ViewType) {
        _uiState.value = _uiState.value.copy(viewType = viewType)
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val (startDateTime, endDateTime) = when (_uiState.value.viewType) {
                ViewType.DAY -> {
                    val date = _uiState.value.selectedDate
                    Pair(
                        date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay().minusNanos(1)
                    )
                }
                ViewType.WEEK -> {
                    val date = _uiState.value.selectedDate
                    val startOfWeek = date.minusDays(date.dayOfWeek.value.toLong() - 1)
                    Pair(
                        startOfWeek.atStartOfDay(),
                        startOfWeek.plusWeeks(1).atStartOfDay().minusNanos(1)
                    )
                }
                ViewType.MONTH -> {
                    val date = _uiState.value.selectedDate
                    val startOfMonth = date.withDayOfMonth(1)
                    Pair(
                        startOfMonth.atStartOfDay(),
                        startOfMonth.plusMonths(1).atStartOfDay().minusNanos(1)
                    )
                }
            }
            
            scheduleRepository.getSchedulesBetween(startDateTime, endDateTime)
                .collect { schedules ->
                    _uiState.value = _uiState.value.copy(
                        schedules = schedules.sortedBy { it.startTime },
                        isLoading = false
                    )
                }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddScheduleDialog = true,
            editingSchedule = null
        )
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            showAddScheduleDialog = false,
            editingSchedule = null
        )
    }

    fun editSchedule(schedule: Schedule) {
        _uiState.value = _uiState.value.copy(
            showAddScheduleDialog = true,
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
            } else {
                scheduleRepository.createSchedule(
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    description = description,
                    category = category,
                    isAllDay = isAllDay,
                    reminderTime = reminderTime,
                    location = location,
                    priority = priority
                )
            }
            hideDialog()
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

    fun updateDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadSchedules()
    }
} 