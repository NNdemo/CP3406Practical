package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.viewmodels.EventDetail
import com.example.myapplication.ui.viewmodels.EventStatus
import com.example.myapplication.ui.viewmodels.TextCreateViewModel
import com.example.myapplication.ui.viewmodels.SettingsViewModel
import com.example.myapplication.ui.viewmodels.AiModel
import com.example.myapplication.data.model.Schedule
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.myapplication.ui.viewmodels.ScheduleViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.example.myapplication.ui.components.ScheduleItem
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextCreateScreen(
    onBackClick: () -> Unit,
    scheduleViewModel: ScheduleViewModel  // 直接接收ScheduleViewModel
) {
    val viewModel: TextCreateViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current

    // Dialog state
    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("") }

    // 接受事件处理函数
    val onAccept: (Schedule) -> Unit = { schedule ->
        try {
            println("TextCreateScreen: Accepting schedule - Title=\"${schedule.title}\"")
            
            // 使用Dispatchers.IO替代viewModelScope
            CoroutineScope(Dispatchers.IO).launch {
                println("TextCreateScreen: Adding schedule in coroutine")
                scheduleViewModel.addSchedule(schedule)
                println("TextCreateScreen: Schedule add request sent")
                
                // 显示成功提示
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Adding schedule: ${schedule.title}", Toast.LENGTH_SHORT).show()
                }
            }
            
            // 移除已接受的日程
            viewModel.removeParsedSchedule(schedule)
            
            // 如果已接受所有日程，则返回主屏幕
            if (uiState.parsedSchedules.isEmpty()) {
                println("TextCreateScreen: All schedules accepted, preparing to return")
                // 给用户一点时间看到Toast
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    onBackClick()
                }
            }
        } catch (e: Exception) {
            println("TextCreateScreen: Error accepting schedule - ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "Add failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 拒绝事件处理函数
    val onReject: (Schedule) -> Unit = { schedule ->
        println("TextCreateScreen: Rejecting schedule - Title=\"${schedule.title}\"")
        viewModel.removeParsedSchedule(schedule)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 调试模式切换开关
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Debug",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Switch(
                            checked = uiState.debugMode,
                            onCheckedChange = { viewModel.toggleDebugMode() }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 输入区域
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateInputText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                label = { Text("Enter text description") },
                placeholder = { Text("Example: Meeting in the conference room from 3pm to 5pm tomorrow to discuss project progress") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 解析按钮
            Button(
                onClick = {
                    // 在调试模式下，执行步骤调试流程
                    if (uiState.debugMode) {
                        viewModel.startDebug()
                    } else {
                        // 正常解析
                        viewModel.parseEventsFromText()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.debugMode) "Start Debug" else "Parse")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 正在解析进度条
            if (uiState.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Parsing...")
            }

            // 调试等待确认按钮
            if (uiState.debugMode && uiState.waitingForConfirmation) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Debug Steps",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(uiState.debugMessages) { message ->
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                HorizontalDivider()
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.confirmDebugStep() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm and Continue")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 显示解析错误
            if (uiState.errorMessage.isNotEmpty()) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 解析结果列表
            if (uiState.parsedSchedules.isNotEmpty()) {
                Text(
                    text = "Parsing Results",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.parsedSchedules) { schedule ->
                        ScheduleItem(
                            schedule = schedule,
                            onAccept = onAccept,
                            onReject = onReject
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 批量接受按钮
                if (uiState.parsedSchedules.isNotEmpty()) {
                    Button(
                        onClick = {
                            try {
                                println("Batch accepting ${uiState.parsedSchedules.size} schedules")
                                
                                // 使用Dispatchers.IO替代viewModelScope
                                CoroutineScope(Dispatchers.IO).launch {
                                    println("TextCreateScreen: Batch adding schedules in coroutine")
                                    
                                    // 批量添加所有日程
                                    uiState.parsedSchedules.forEach { schedule ->
                                        println("Adding schedule: Title=\"${schedule.title}\", Start time=${schedule.startTime}, End time=${schedule.endTime}, Location=${schedule.location}")
                                        scheduleViewModel.addSchedule(schedule)
                                        println("Schedule add request sent")
                                    }
                                    
                                    // 通知用户添加成功
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Adding ${uiState.parsedSchedules.size} schedules", Toast.LENGTH_SHORT).show()
                                        
                                        // 清除已解析日程并返回主屏幕
                                        viewModel.clearParsedSchedules()
                                        println("Cleared parsed schedules list")
                                        
                                        // 延迟返回，给用户时间看到Toast
                                        delay(1000)
                                        onBackClick()
                                    }
                                }
                            } catch (e: Exception) {
                                println("Error adding batch schedules: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(context, "Add failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Accept All")
                    }
                }
            }
        }
    }

    // 显示对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Notice") },
            text = { Text(dialogText) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ExpandableSection(
    title: String,
    initialExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            content()
        }
    }
}

@Composable
fun QuestionDialog(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onUpdateQuestion: (String) -> Unit,
    currentQuestion: String
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Questions about this event",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = currentQuestion,
                    onValueChange = onUpdateQuestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = { Text("Example: Can I change the time to 4pm?") },
                    label = { Text("Your question") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onSubmit,
                        enabled = currentQuestion.isNotBlank()
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun ApiKeyReminderCard() {
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val isApiKeyConfigured = (uiState.defaultAiModel == AiModel.CHATGPT && uiState.chatgptApiKey.isNotBlank()) ||
            (uiState.defaultAiModel == AiModel.GROK && uiState.grokApiKey.isNotBlank())
    
    if (!isApiKeyConfigured) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "API Key Not Configured",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Text(
                    text = "You need to set up an API key for AI processing. Go to Settings to configure it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Button(
                    onClick = {
                        Toast.makeText(context, "Please go to Settings to configure API key", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("I Understand")
                }
            }
        }
    }
} 