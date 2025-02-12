package com.example.myapplication.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.ui.components.ScheduleDialog
import com.example.myapplication.ui.viewmodels.TimelineViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ViewType {
    DAY, WEEK, MONTH
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showAddScheduleDialog) {
        ScheduleDialog(
            schedule = uiState.editingSchedule,
            selectedDate = uiState.selectedDate,
            onDismiss = viewModel::hideDialog,
            onConfirm = viewModel::createOrUpdateSchedule
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = viewModel::showAddDialog) {
                        Icon(Icons.Default.Add, contentDescription = "Add Schedule")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 日期选择器
            CalendarDateSelector(
                selectedDate = uiState.selectedDate,
                onDateSelected = viewModel::updateDate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // 时间线视图
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                TimelineView(
                    schedules = uiState.schedules,
                    onScheduleEdit = viewModel::editSchedule,
                    onScheduleToggle = viewModel::toggleScheduleCompletion,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun CalendarDateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateSelected(selectedDate.minusDays(1)) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
        }

        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
            style = MaterialTheme.typography.titleLarge
        )

        IconButton(onClick = { onDateSelected(selectedDate.plusDays(1)) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
        }
    }
}

@Composable
fun TimelineView(
    schedules: List<Schedule>,
    onScheduleEdit: (Schedule) -> Unit,
    onScheduleToggle: (Schedule) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 跨天日程
        val crossDaySchedules = schedules.filter { schedule ->
            !schedule.startTime.toLocalDate().isEqual(schedule.endTime.toLocalDate())
        }
        if (crossDaySchedules.isNotEmpty()) {
            item {
                Text(
                    text = "Multi-day Events",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(crossDaySchedules) { schedule ->
                TimelineScheduleCard(
                    schedule = schedule,
                    onEdit = { onScheduleEdit(schedule) },
                    onToggle = { onScheduleToggle(schedule) }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        // 当天日程
        val sameDaySchedules = schedules.filter { schedule ->
            schedule.startTime.toLocalDate().isEqual(schedule.endTime.toLocalDate())
        }.sortedBy { it.startTime }

        items(sameDaySchedules) { schedule ->
            TimelineScheduleCard(
                schedule = schedule,
                onEdit = { onScheduleEdit(schedule) },
                onToggle = { onScheduleToggle(schedule) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TimelineScheduleCard(
    schedule: Schedule,
    onEdit: () -> Unit,
    onToggle: () -> Unit
) {
    val backgroundColor = when(schedule.category) {
        ScheduleCategory.STUDY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        ScheduleCategory.EXAM -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
        ScheduleCategory.HOMEWORK -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
        ScheduleCategory.MEETING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
        ScheduleCategory.CLASS -> Color(0xFF2196F3).copy(alpha = 0.85f)
        ScheduleCategory.OTHER -> Color(0xFF607D8B).copy(alpha = 0.85f)
    }.let { baseColor ->
        when(schedule.priority) {
            SchedulePriority.HIGH -> baseColor.copy(alpha = 0.95f)
            SchedulePriority.MEDIUM -> baseColor
            SchedulePriority.LOW -> baseColor.copy(alpha = 0.75f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onDoubleClick = onToggle
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = Color.White
                )
                if (schedule.isReminded) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            if (schedule.location.isNotBlank()) {
                Text(
                    text = schedule.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = buildTimeRangeText(schedule),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun buildTimeRangeText(schedule: Schedule): String {
    if (schedule.isAllDay) return "All Day"
    
    val startDate = schedule.startTime.toLocalDate()
    val endDate = schedule.endTime.toLocalDate()
    val startTime = schedule.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    val endTime = schedule.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    
    return if (startDate == endDate) {
        "${startDate.format(DateTimeFormatter.ofPattern("MM-dd"))} $startTime - $endTime"
    } else {
        "${startDate.format(DateTimeFormatter.ofPattern("MM-dd"))} $startTime - " +
        "${endDate.format(DateTimeFormatter.ofPattern("MM-dd"))} $endTime"
    }
} 