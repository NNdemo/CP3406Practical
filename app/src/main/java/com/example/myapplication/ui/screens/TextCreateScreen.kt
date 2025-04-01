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
            println("TextCreateScreen: 接受日程 - 标题=\"${schedule.title}\"")
            
            // 使用Dispatchers.IO替代viewModelScope
            CoroutineScope(Dispatchers.IO).launch {
                println("TextCreateScreen: 在协程中添加日程")
                scheduleViewModel.addSchedule(schedule)
                println("TextCreateScreen: 日程添加请求已发送")
                
                // 显示成功提示
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "正在添加日程: ${schedule.title}", Toast.LENGTH_SHORT).show()
                }
            }
            
            // 移除已接受的日程
            viewModel.removeParsedSchedule(schedule)
            
            // 如果已接受所有日程，则返回主屏幕
            if (uiState.parsedSchedules.isEmpty()) {
                println("TextCreateScreen: 所有日程已接受，准备返回")
                // 给用户一点时间看到Toast
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    onBackClick()
                }
            }
        } catch (e: Exception) {
            println("TextCreateScreen: 接受日程时出错 - ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "添加失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 拒绝事件处理函数
    val onReject: (Schedule) -> Unit = { schedule ->
        println("TextCreateScreen: 拒绝日程 - 标题=\"${schedule.title}\"")
        viewModel.removeParsedSchedule(schedule)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建日程") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 调试模式切换开关
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "调试",
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
                label = { Text("输入文本描述") },
                placeholder = { Text("例如：明天下午3点到5点在会议室开会讨论项目进度") }
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
                Text(if (uiState.debugMode) "开始调试" else "解析")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 正在解析进度条
            if (uiState.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("正在解析...")
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
                            text = "调试步骤",
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
                            Text("确认并继续")
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
                    text = "解析结果",
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
                                println("批量接受 ${uiState.parsedSchedules.size} 个日程")
                                
                                // 使用Dispatchers.IO替代viewModelScope
                                CoroutineScope(Dispatchers.IO).launch {
                                    println("TextCreateScreen: 在协程中批量添加日程")
                                    
                                    // 批量添加所有日程
                                    uiState.parsedSchedules.forEach { schedule ->
                                        println("添加日程: 标题=\"${schedule.title}\", 开始时间=${schedule.startTime}, 结束时间=${schedule.endTime}, 地点=${schedule.location}")
                                        scheduleViewModel.addSchedule(schedule)
                                        println("日程添加请求已发送")
                                    }
                                    
                                    // 通知用户添加成功
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "正在添加 ${uiState.parsedSchedules.size} 个日程", Toast.LENGTH_SHORT).show()
                                        
                                        // 清除已解析日程并返回主屏幕
                                        viewModel.clearParsedSchedules()
                                        println("已清除已解析日程列表")
                                        
                                        // 延迟返回，给用户时间看到Toast
                                        delay(1000)
                                        onBackClick()
                                    }
                                }
                            } catch (e: Exception) {
                                println("批量添加日程时出错: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(context, "添加失败: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("接受全部")
                    }
                }
            }
        }
    }

    // 显示对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("提示") },
            text = { Text(dialogText) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("确定")
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
                contentDescription = if (expanded) "收起" else "展开",
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
                    text = "关于此事件的问题",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = currentQuestion,
                    onValueChange = onUpdateQuestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = { Text("例如：能否修改时间为下午4点？") },
                    label = { Text("您的问题") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = onSubmit,
                        enabled = currentQuestion.isNotBlank()
                    ) {
                        Text("提交")
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
                        contentDescription = "警告",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "API密钥未配置",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Text(
                    text = "您需要先在设置中配置${if (uiState.defaultAiModel == AiModel.CHATGPT) "ChatGPT" else "Grok"} API密钥才能获得准确的日程解析结果。否则将使用简单的本地解析，可能不够准确。",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                TextButton(
                    onClick = { 
                        // 导航到设置页面的操作
                        // 这里暂时使用Toast提示，实际应用中应该进行导航
                        android.widget.Toast.makeText(context, "请点击底部导航栏的'设置'图标来配置API密钥", android.widget.Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("前往设置")
                }
            }
        }
    }
} 