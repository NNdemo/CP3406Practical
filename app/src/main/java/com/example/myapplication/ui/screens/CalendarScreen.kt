package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.CalendarView
import com.example.myapplication.ui.components.ScheduleDialog
import com.example.myapplication.ui.viewmodels.CalendarViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showAddDialog) {
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
                title = { Text("日程安排") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = viewModel::showAddDialog) {
                        Icon(Icons.Default.Add, contentDescription = "添加日程")
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
            DateSelector(
                selectedDate = uiState.selectedDate,
                onDateSelected = viewModel::selectDate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // 日历视图
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CalendarView(
                    selectedDate = uiState.selectedDate,
                    schedules = uiState.schedules,
                    onDateSelected = viewModel::selectDate,
                    onScheduleEdit = viewModel::editSchedule,
                    onScheduleToggle = viewModel::toggleScheduleCompletion,
                    onScheduleDrag = viewModel::updateScheduleTime,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun DateSelector(
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前一天")
        }

        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
            style = MaterialTheme.typography.titleLarge
        )

        IconButton(onClick = { onDateSelected(selectedDate.plusDays(1)) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "后一天")
        }
    }
} 