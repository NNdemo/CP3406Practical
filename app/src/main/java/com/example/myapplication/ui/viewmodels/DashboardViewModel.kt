package com.example.myapplication.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.data.repository.ScheduleRepository
import com.example.myapplication.ui.screens.ViewType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class DashboardUiState(
    val schedules: List<Schedule> = emptyList(),
    val totalSchedules: Int = 0,
    val completedSchedules: Int = 0,
    val isLoading: Boolean = false,
    val showAddScheduleDialog: Boolean = false,
    val activeStudySession: StudySessionState? = null,
    val editingSchedule: Schedule? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val viewType: ViewType = ViewType.DAY
)

data class StudySessionState(
    val id: Long = 0L,
    val subject: String = "",
    val startTime: LocalDateTime = LocalDateTime.now(),
    val isPaused: Boolean = false,
    val pauseStartTime: LocalDateTime? = null,
    val totalPauseDuration: Duration = Duration.ZERO,
    val currentDuration: Duration = Duration.ZERO
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null

    init {
        loadDashboardData()
    }

    fun updateViewType(viewType: ViewType) {
        _uiState.value = _uiState.value.copy(viewType = viewType)
        loadDashboardData()
    }

    private fun loadDashboardData() {
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
                        totalSchedules = schedules.size,
                        completedSchedules = schedules.count { it.isReminded },
                        isLoading = false
                    )
                }
        }
    }

    fun refreshData() {
        loadDashboardData()
    }

    fun showAddScheduleDialog() {
        _uiState.value = _uiState.value.copy(
            showAddScheduleDialog = true,
            editingSchedule = null
        )
    }

    fun hideAddScheduleDialog() {
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
    
    fun updateSchedule(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.updateSchedule(schedule)
            hideAddScheduleDialog()
            loadDashboardData()
        }
    }
    
    fun createSchedule(
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
            hideAddScheduleDialog()
            loadDashboardData()
        }
    }
    
    fun toggleScheduleCompletion(schedule: Schedule) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(isReminded = !schedule.isReminded)
            scheduleRepository.updateSchedule(updatedSchedule)
            loadDashboardData()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
} 