package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.data.model.JcuClass
import com.example.myapplication.data.model.JcuClassStatus
import com.example.myapplication.ui.viewmodels.JcuViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JcuScreen(
    viewModel: JcuViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val classes by viewModel.allClasses.collectAsStateWithLifecycle(initialValue = emptyList())
    val statistics by viewModel.statistics.collectAsStateWithLifecycle(initialValue = null)
    
    var showSettings by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 顶部应用栏
            TopAppBar(
                title = { Text("JCU Course Management") },
                actions = {
                    IconButton(onClick = { viewModel.fetchClasses() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    
                    IconButton(onClick = { showSettings = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
            
            // 主要内容区域
            if (!uiState.isLoggedIn) {
                // 未登录状态显示登录表单
                LoginForm(
                    username = uiState.username,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    onLogin = { username, password, saveCredentials ->
                        viewModel.login(username, password, saveCredentials)
                    },
                    onClearError = { viewModel.clearError() }
                )
            } else {
                // 已登录状态显示课程内容
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    // 统计信息卡片
                    item {
                        StatisticsCard(statistics = statistics)
                    }
                    
                    // 上一次刷新时间
                    item {
                        if (uiState.lastRefreshTime.isNotEmpty()) {
                            Text(
                                text = "Last update: ${uiState.lastRefreshTime}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    
                    // 课程列表
                    if (classes.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Class List",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Button(
                                    onClick = { viewModel.importClassesToSchedules() },
                                    enabled = !uiState.isImporting
                                ) {
                                    Text(text = "Import to Schedule")
                                }
                            }
                        }
                        
                        items(classes) { jcuClass ->
                            ClassItem(jcuClass = jcuClass)
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Loading class data...")
                                    } else {
                                        Text("No class data available")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { viewModel.fetchClasses() }) {
                                            Text("Refresh")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 导入结果提示
                    if (uiState.lastImportResult.isNotEmpty()) {
                        item {
                            Text(
                                text = uiState.lastImportResult,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                
                // 底部操作区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { viewModel.logout() }
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
        
        // 设置对话框
        if (showSettings) {
            SettingsDialog(
                refreshInterval = uiState.refreshInterval,
                onDismiss = { showSettings = false },
                onSave = { refreshInterval ->
                    viewModel.updateSettings(refreshInterval)
                    showSettings = false
                }
            )
        }
        
        // 加载指示器
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center)
            )
        }
    }
    
    // 首次进入检查是否有保存的凭证
    LaunchedEffect(Unit) {
        viewModel.loginWithSavedCredentials()
    }
}

@Composable
fun LoginForm(
    username: String,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (String, String, Boolean) -> Unit,
    onClearError: () -> Unit
) {
    var usernameInput by remember { mutableStateOf(username) }
    var password by remember { mutableStateOf("") }
    var saveCredentials by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "JCU Student Login",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { 
                    usernameInput = it
                    onClearError()
                },
                label = { Text("Username") },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    onClearError()
                },
                label = { Text("Password") },
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = saveCredentials,
                    onCheckedChange = { saveCredentials = it },
                    enabled = !isLoading
                )
                
                Text(
                    text = "Save login information",
                    modifier = Modifier.clickable { saveCredentials = !saveCredentials }
                )
            }
            
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onLogin(usernameInput, password, saveCredentials) },
                enabled = !isLoading && usernameInput.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }
    }
}

@Composable
fun StatisticsCard(statistics: com.example.myapplication.data.model.JcuClassStatistics?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Class Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (statistics != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = "Total",
                        value = statistics.totalClasses,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    StatItem(
                        title = "Completed",
                        value = statistics.completedClasses,
                        color = Color.Green
                    )
                    
                    StatItem(
                        title = "Absent",
                        value = statistics.absentClasses,
                        color = Color.Red
                    )
                    
                    StatItem(
                        title = "Planned",
                        value = statistics.plannedClasses,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 从网页获取的出勤率信息
                if (statistics.webClassAttendanceRate > 0 || statistics.webCampusAttendanceRate > 0) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "JCU System Data:",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Class Attendance Rate",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val webClassRate = statistics.webClassAttendanceRate * 100
                                
                                LinearProgressIndicator(
                                    progress = { statistics.webClassAttendanceRate },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = when {
                                        webClassRate >= 90 -> Color.Green
                                        webClassRate >= 80 -> Color(0xFFFFA500) // Orange
                                        else -> Color.Red
                                    }
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "%.1f%%".format(webClassRate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Campus Attendance Rate",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val webCampusRate = statistics.webCampusAttendanceRate * 100
                                
                                LinearProgressIndicator(
                                    progress = { statistics.webCampusAttendanceRate },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = when {
                                        webCampusRate >= 90 -> Color.Green
                                        webCampusRate >= 80 -> Color(0xFFFFA500) // Orange
                                        else -> Color.Red
                                    }
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "%.1f%%".format(webCampusRate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No statistics data available",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ClassItem(jcuClass: JcuClass) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    
    val statusColor = when (jcuClass.status) {
        JcuClassStatus.COMPLETED -> Color.Green
        JcuClassStatus.ABSENT -> Color.Red
        JcuClassStatus.PLANNED -> Color.Gray
        JcuClassStatus.IN_PROGRESS -> Color.Blue
        JcuClassStatus.UPCOMING -> Color(0xFFFFA500) // 橙色
        JcuClassStatus.UNKNOWN -> Color.Gray
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态指示器
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = statusColor,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 课程信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${jcuClass.courseCode} - ${jcuClass.courseName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Time: ${jcuClass.startTime.format(formatter)} - ${jcuClass.endTime.format(formatter)}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "Location: ${jcuClass.location}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 状态文字
            Text(
                text = jcuClass.status.description,
                color = statusColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    refreshInterval: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var intervalInput by remember { mutableStateOf(refreshInterval.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "JCU Settings")
        },
        text = {
            Column {
                Text(
                    text = "Set automatic refresh frequency (hours)",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = intervalInput,
                    onValueChange = { 
                        // 只允许输入数字
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            intervalInput = it
                        }
                    },
                    label = { Text("Refresh interval (hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Set to 0 to disable automatic refresh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val interval = intervalInput.toIntOrNull() ?: refreshInterval
                    onSave(interval)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
} 