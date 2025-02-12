package com.example.myapplication.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
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
import com.example.myapplication.ui.viewmodels.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    if (uiState.showAddScheduleDialog) {
        ScheduleDialog(
            schedule = uiState.editingSchedule,
            selectedDate = LocalDate.now(),
            onDismiss = viewModel::hideAddScheduleDialog,
            onConfirm = { title, description, startTime, endTime, isAllDay, category, reminderTime, location, priority ->
                if (uiState.editingSchedule != null) {
                    viewModel.updateSchedule(
                        uiState.editingSchedule!!.copy(
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
                    )
                } else {
                    viewModel.createSchedule(
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
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Assistant") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = viewModel::refreshData) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's date display
            Text(
                text = "Today's Date: ${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))}",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Schedule overview card
            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Schedule",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${uiState.completedSchedules}/${uiState.totalSchedules}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { 
                            if (uiState.totalSchedules > 0) {
                                uiState.completedSchedules.toFloat() / uiState.totalSchedules.toFloat()
                            } else 0f 
                        }
                    )
                }
            }
            
            // Schedule grid view
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.schedules) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onLongClick = { viewModel.editSchedule(schedule) },
                        onDoubleClick = { viewModel.toggleScheduleCompletion(schedule) }
                    )
                }
            }
            
            // Quick add schedule button
            Button(
                onClick = viewModel::showAddScheduleDialog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Schedule"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Schedule")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleCard(
    schedule: Schedule,
    onLongClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
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

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onLongClick,
                onDoubleClick = onDoubleClick,
                onLongClick = { /* 暂时不处理长按拖拽 */ }
            ),
        colors = CardDefaults.elevatedCardColors(
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
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (schedule.isReminded) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            // Date and time
            Text(
                text = buildTimeRangeText(schedule),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            
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
        }
    }
}

private fun buildTimeRangeText(schedule: Schedule): String {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    
    if (schedule.isAllDay) {
        return if (schedule.startTime.toLocalDate() == schedule.endTime.toLocalDate()) {
            "${schedule.startTime.format(dateFormatter)} All Day"
        } else {
            "${schedule.startTime.format(dateFormatter)}-${schedule.endTime.format(dateFormatter)} All Day"
        }
    }
    
    val startDate = schedule.startTime.toLocalDate()
    val endDate = schedule.endTime.toLocalDate()
    
    return if (startDate == endDate) {
        "${startDate.format(dateFormatter)}\n${schedule.startTime.format(timeFormatter)}-${schedule.endTime.format(timeFormatter)}"
    } else {
        "${startDate.format(dateFormatter)} ${schedule.startTime.format(timeFormatter)}\n↓\n${endDate.format(dateFormatter)} ${schedule.endTime.format(timeFormatter)}"
    }
}

private fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("MM月dd日"))
} 