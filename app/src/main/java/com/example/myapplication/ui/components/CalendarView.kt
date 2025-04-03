package com.example.myapplication.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarView(
    selectedDate: LocalDate,
    schedules: List<Schedule>,
    onDateSelected: (LocalDate) -> Unit,
    onScheduleEdit: (Schedule) -> Unit,
    onScheduleToggle: (Schedule) -> Unit,
    onScheduleDrag: (Schedule, LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 周视图
        WeekHeader(selectedDate = selectedDate, onDateSelected = onDateSelected)
        
        // 24小时时间轴列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 处理跨天日程
            val crossDaySchedules = schedules.filter { schedule ->
                schedule.startTime.toLocalDate() != schedule.endTime.toLocalDate() &&
                (schedule.startTime.toLocalDate().isBefore(selectedDate) || 
                schedule.endTime.toLocalDate().isAfter(selectedDate))
            }
            
            if (crossDaySchedules.isNotEmpty()) {
                item {
                    Text(
                        text = "Multi-day Events",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    crossDaySchedules.forEach { schedule ->
                        ScheduleListItem(
                            schedule = schedule,
                            onEdit = { onScheduleEdit(schedule) },
                            onToggle = { onScheduleToggle(schedule) },
                            onDrag = { },  // 跨天日程不支持拖拽
                            showFullDate = true
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
            
            // 生成24小时的时间槽
            for (hour in 0..23) {
                item {
                    TimeSlot(
                        hour = hour,
                        schedules = schedules.filter { schedule ->
                            val startHour = schedule.startTime.hour
                            val endHour = if (schedule.endTime.toLocalDate() == schedule.startTime.toLocalDate()) {
                                schedule.endTime.hour
                            } else {
                                23 // 如果跨天，则在当天显示到23点
                            }
                            
                            // 只显示当天的日程
                            schedule.startTime.toLocalDate() == selectedDate &&
                            schedule.endTime.toLocalDate() == selectedDate &&
                            ((startHour <= hour && hour <= endHour) || schedule.isAllDay)
                        }.sortedWith(
                            compareByDescending<Schedule> { it.priority }
                            .thenBy { it.startTime }
                        ),
                        onScheduleEdit = onScheduleEdit,
                        onScheduleToggle = onScheduleToggle,
                        onScheduleDrag = onScheduleDrag,
                        selectedDate = selectedDate
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeSlot(
    hour: Int,
    schedules: List<Schedule>,
    onScheduleEdit: (Schedule) -> Unit,
    onScheduleToggle: (Schedule) -> Unit,
    onScheduleDrag: (Schedule, LocalTime) -> Unit,
    selectedDate: LocalDate
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 时间标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间显示
            Text(
                text = String.format("%02d:00", hour),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(48.dp)
            )
            
            // 分隔线
            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
        
        // 该时间段的日程
        schedules.forEach { schedule ->
            ScheduleListItem(
                schedule = schedule,
                onEdit = { onScheduleEdit(schedule) },
                onToggle = { onScheduleToggle(schedule) },
                onDrag = { newTime ->
                    onScheduleDrag(schedule, newTime)
                },
                showFullDate = false
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleListItem(
    schedule: Schedule,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDrag: (LocalTime) -> Unit,
    showFullDate: Boolean = false,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val backgroundColor = when(schedule.category) {
        ScheduleCategory.STUDY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        ScheduleCategory.WORK -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
        ScheduleCategory.ENTERTAINMENT -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
        ScheduleCategory.MEETING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
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
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onEdit,
                onDoubleClick = onToggle,
                onLongClick = {
                    isDragging = true
                }
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change: PointerInputChange, dragAmount: Offset ->
                        change.consume()
                        offsetY += dragAmount.y
                        // 每30分钟对应一个时间段
                        val timeAdjustment = (offsetY / 60f).roundToInt() * 30
                        val newTime = schedule.startTime.toLocalTime().plusMinutes(timeAdjustment.toLong())
                        onDrag(newTime)
                    }
                )
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
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
                    modifier = Modifier.weight(1f)
                )
                if (schedule.isReminded) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (schedule.location.isNotBlank()) {
                Text(
                    text = schedule.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = buildTimeRangeText(schedule, showFullDate, schedule.startTime.toLocalDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildTimeRangeText(schedule: Schedule, showFullDate: Boolean, selectedDate: LocalDate = schedule.startTime.toLocalDate()): String {
    if (schedule.isAllDay) return "All Day"
    
    val startDate = schedule.startTime.toLocalDate()
    val endDate = schedule.endTime.toLocalDate()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    
    return when {
        // 同一天的日程
        startDate == endDate -> {
            "${schedule.startTime.format(timeFormatter)} - ${schedule.endTime.format(timeFormatter)}"
        }
        // 跨天的日程，显示完整日期
        showFullDate -> {
            "${startDate.format(dateFormatter)} ${schedule.startTime.format(timeFormatter)} → ${endDate.format(dateFormatter)} ${schedule.endTime.format(timeFormatter)}"
        }
        // 跨天的日程，当前显示是开始日期
        startDate == selectedDate -> {
            "${schedule.startTime.format(timeFormatter)} → Next day ${schedule.endTime.format(timeFormatter)}"
        }
        // 跨天的日程，当前显示是结束日期
        endDate == selectedDate -> {
            "Previous day ${schedule.startTime.format(timeFormatter)} → ${schedule.endTime.format(timeFormatter)}"
        }
        // 多天的日程
        else -> {
            "${startDate.format(dateFormatter)} → ${endDate.format(dateFormatter)}"
        }
    }
}

@Composable
private fun WeekHeader(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp)
    ) {
        // 显示周数
        Text(
            text = "Week",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        // 显示日期行
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 获取本周的日期
            val monday = selectedDate.with(java.time.DayOfWeek.MONDAY)
            val weekDays = (0..6).map { monday.plusDays(it.toLong()) }
            
            weekDays.forEach { date ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDateSelected(date) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 星期几
                    Text(
                        text = when(date.dayOfWeek) {
                            java.time.DayOfWeek.MONDAY -> "MON"
                            java.time.DayOfWeek.TUESDAY -> "TUE"
                            java.time.DayOfWeek.WEDNESDAY -> "WED"
                            java.time.DayOfWeek.THURSDAY -> "THU"
                            java.time.DayOfWeek.FRIDAY -> "FRI"
                            java.time.DayOfWeek.SATURDAY -> "SAT"
                            java.time.DayOfWeek.SUNDAY -> "SUN"
                            null -> "UNK"
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // 日期数字
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (date == selectedDate) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (date == selectedDate)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
} 