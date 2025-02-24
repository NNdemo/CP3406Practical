package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(
    schedule: Schedule? = null,
    selectedDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onConfirm: (
        String,
        String,
        LocalDateTime,
        LocalDateTime,
        Boolean,
        ScheduleCategory,
        LocalDateTime?,
        String,
        SchedulePriority
    ) -> Unit
) {
    var title by remember { mutableStateOf(schedule?.title ?: "") }
    var description by remember { mutableStateOf(schedule?.description ?: "") }
    var startDate by remember { mutableStateOf(schedule?.startTime?.toLocalDate() ?: selectedDate) }
    var startTime by remember { mutableStateOf(schedule?.startTime?.toLocalTime() ?: LocalTime.now()) }
    var endDate by remember { mutableStateOf(schedule?.endTime?.toLocalDate() ?: selectedDate) }
    var endTime by remember { mutableStateOf(schedule?.endTime?.toLocalTime() ?: LocalTime.now().plusHours(1)) }
    var isAllDay by remember { mutableStateOf(schedule?.isAllDay ?: false) }
    var category by remember { mutableStateOf(schedule?.category ?: ScheduleCategory.OTHER) }
    val reminderTime by remember { mutableStateOf(schedule?.reminderTime) }
    var location by remember { mutableStateOf(schedule?.location ?: "") }
    var priority by remember { mutableStateOf(schedule?.priority ?: SchedulePriority.MEDIUM) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (schedule == null) "Add Schedule" else "Edit Schedule") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("All Day Event")
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }

                // Time selection
                if (!isAllDay) {
                    Text("Start Time")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            modifier = Modifier.clickable { showStartDatePicker = true }
                        )
                        Text(
                            text = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            modifier = Modifier.clickable { showStartTimePicker = true }
                        )
                    }
                    
                    Text("End Time")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            modifier = Modifier.clickable { showEndDatePicker = true }
                        )
                        Text(
                            text = endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            modifier = Modifier.clickable { showEndTimePicker = true }
                        )
                    }
                }

                // Category selection
                var showCategoryMenu by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = showCategoryMenu,
                    onExpandedChange = { showCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = category.name,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        ScheduleCategory.values().forEach { categoryOption ->
                            DropdownMenuItem(
                                text = { Text(categoryOption.name) },
                                onClick = {
                                    category = categoryOption
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                // Priority selection
                var showPriorityMenu by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = showPriorityMenu,
                    onExpandedChange = { showPriorityMenu = it }
                ) {
                    OutlinedTextField(
                        value = priority.name,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Priority") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showPriorityMenu,
                        onDismissRequest = { showPriorityMenu = false }
                    ) {
                        SchedulePriority.values().forEach { priorityOption ->
                            DropdownMenuItem(
                                text = { Text(priorityOption.name) },
                                onClick = {
                                    priority = priorityOption
                                    showPriorityMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val startDateTime = if (isAllDay) {
                            LocalDateTime.of(startDate, LocalTime.MIN)
                        } else {
                            LocalDateTime.of(startDate, startTime)
                        }
                        val endDateTime = if (isAllDay) {
                            LocalDateTime.of(endDate, LocalTime.MAX)
                        } else {
                            LocalDateTime.of(endDate, endTime)
                        }
                        onConfirm(
                            title,
                            description,
                            startDateTime,
                            endDateTime,
                            isAllDay,
                            category,
                            reminderTime,
                            location,
                            priority
                        )
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Date picker
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = { Text("Select Start Date") }
            )
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            endDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = { Text("Select End Date") }
            )
        }
    }

    // Time picker
    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = startTime.hour,
            initialMinute = startTime.minute,
            onDismissRequest = { showStartTimePicker = false },
            onTimeSelected = { hour, minute ->
                startTime = LocalTime.of(hour, minute)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialHour = endTime.hour,
            initialMinute = endTime.minute,
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = { hour, minute ->
                endTime = LocalTime.of(hour, minute)
                showEndTimePicker = false
            }
        )
    }
}

@Composable
fun TimePickerDialog(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    onDismissRequest: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            Column {
                Row {
                    OutlinedTextField(
                        value = selectedHour.toString(),
                        onValueChange = { 
                            val hour = it.toIntOrNull()
                            if (hour != null && hour in 0..23) {
                                selectedHour = hour
                            }
                        },
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = selectedMinute.toString(),
                        onValueChange = { 
                            val minute = it.toIntOrNull()
                            if (minute != null && minute in 0..59) {
                                selectedMinute = minute
                            }
                        },
                        label = { Text("Minute") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(selectedHour, selectedMinute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
} 