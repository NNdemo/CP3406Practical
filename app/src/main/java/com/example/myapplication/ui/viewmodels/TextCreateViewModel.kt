package com.example.myapplication.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppSettings
import com.example.myapplication.data.ai.AiService
import com.example.myapplication.data.model.RepeatType
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

// 表示单个事件的详情
data class EventDetail(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val category: String = "",
    val location: String = "",
    val priority: String = "",
    val description: String = "",
    val status: EventStatus = EventStatus.PENDING,
    // 添加人类可读的时间格式
    val readableStartTime: String = "",
    val readableEndTime: String = ""
) {
    // 将EventDetail转换为Schedule
    fun toSchedule(): Schedule {
        println("EventDetail.toSchedule: 开始转换 - 标题=\"$title\", 开始时间=$startTime, 结束时间=$endTime")
        
        // 支持多种日期时间格式
        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        )
        
        println("EventDetail.toSchedule: 尝试解析开始时间=\"$startTime\"")
        val startDateTime = parseDateTime(startTime, formatters)
        if (startDateTime == null) {
            println("EventDetail.toSchedule: ⚠️ 无法解析开始时间，使用当前时间作为默认值")
        } else {
            println("EventDetail.toSchedule: 开始时间解析成功 - $startDateTime")
        }
        
        val finalStartDateTime = startDateTime ?: LocalDateTime.now()
        
        println("EventDetail.toSchedule: 尝试解析结束时间=\"$endTime\"")
        val endDateTime = parseDateTime(endTime, formatters)
        if (endDateTime == null) {
            println("EventDetail.toSchedule: ⚠️ 无法解析结束时间，使用开始时间+1小时作为默认值")
        } else {
            println("EventDetail.toSchedule: 结束时间解析成功 - $endDateTime")
        }
        
        val finalEndDateTime = endDateTime ?: finalStartDateTime.plusHours(1)
        
        println("EventDetail.toSchedule: 尝试解析类别=\"$category\"")
        val eventCategory = try {
            val result = ScheduleCategory.valueOf(category.uppercase())
            println("EventDetail.toSchedule: 类别解析成功 - $result")
            result
        } catch (e: Exception) {
            println("EventDetail.toSchedule: ⚠️ 无法解析类别，使用OTHER作为默认值 - 错误: ${e.message}")
            ScheduleCategory.OTHER
        }
        
        println("EventDetail.toSchedule: 尝试解析优先级=\"$priority\"")
        val eventPriority = try {
            val result = SchedulePriority.valueOf(priority.uppercase())
            println("EventDetail.toSchedule: 优先级解析成功 - $result")
            result
        } catch (e: Exception) {
            println("EventDetail.toSchedule: ⚠️ 无法解析优先级，使用MEDIUM作为默认值 - 错误: ${e.message}")
            SchedulePriority.MEDIUM
        }
        
        val schedule = Schedule(
            id = 0, // 自动生成
            title = title,
            startTime = finalStartDateTime,
            endTime = finalEndDateTime,
            category = eventCategory,
            description = description,
            location = location,
            priority = eventPriority
        )
        
        println("EventDetail.toSchedule: 转换完成 - Schedule对象创建成功")
        return schedule
    }
    
    // 辅助方法：尝试使用多种格式解析日期时间
    private fun parseDateTime(dateTimeStr: String, formatters: List<DateTimeFormatter>): LocalDateTime? {
        for (formatter in formatters) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter)
            } catch (e: Exception) {
                // 继续尝试下一个格式
            }
        }
        return null  // 如果所有格式都失败，返回null
    }
}

// 事件状态
enum class EventStatus {
    PENDING, // 待确认
    ACCEPTED, // 已接受
    REJECTED, // 已拒绝
    EDITING   // 正在编辑/提问
}

data class TextCreateUiState(
    val inputText: String = "",  // 用户输入的文本
    val isLoading: Boolean = false, 
    val errorMessage: String = "",
    val parsedSchedules: List<Schedule> = emptyList(), // 解析出的多个日程
    val debugMode: Boolean = false,                    // 是否处于调试模式
    val debugStep: Int = 0,                            // 当前调试步骤
    val waitingForConfirmation: Boolean = false,       // 是否等待用户确认
    val debugMessages: List<String> = emptyList()      // 调试消息列表
)

@HiltViewModel
class TextCreateViewModel @Inject constructor(
    application: Application,
    private val scheduleRepository: ScheduleRepository,
    private val aiService: AiService,
    internal val appSettings: AppSettings
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(TextCreateUiState())
    val uiState: StateFlow<TextCreateUiState> = _uiState.asStateFlow()
    
    // 辅助方法：尝试使用多种格式解析日期时间
    private fun parseDateTime(dateTimeStr: String, formatters: List<DateTimeFormatter>): LocalDateTime? {
        for (formatter in formatters) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter)
            } catch (e: Exception) {
                // 继续尝试下一个格式
            }
        }
        return null  // 如果所有格式都失败，返回null
    }
    
    // 更新输入文本
    fun updateInputText(input: String) {
        _uiState.value = _uiState.value.copy(inputText = input)
    }
    
    // 切换调试模式
    fun toggleDebugMode() {
        _uiState.value = _uiState.value.copy(
            debugMode = !_uiState.value.debugMode
        )
        println("调试模式: ${if (_uiState.value.debugMode) "开启" else "关闭"}")
    }
    
    // 开始调试流程
    fun startDebug() {
        _uiState.value = _uiState.value.copy(
            debugStep = 1,
            debugMessages = listOf("开始调试流程..."),
            waitingForConfirmation = true
        )
    }
    
    // 确认当前调试步骤并继续
    fun confirmDebugStep() {
        val nextStep = _uiState.value.debugStep + 1
        _uiState.value = _uiState.value.copy(
            debugStep = nextStep,
            waitingForConfirmation = false,
            debugMessages = _uiState.value.debugMessages + listOf("继续执行步骤 $nextStep...")
        )
        
        // 执行下一步调试操作
        when (nextStep) {
            2 -> checkApiConfig()
            3 -> parseTextContent()
            else -> _uiState.value = _uiState.value.copy(
                waitingForConfirmation = true,
                debugMessages = _uiState.value.debugMessages + listOf("调试完成")
            )
        }
    }
    
    // 检查API配置
    private fun checkApiConfig() {
        viewModelScope.launch {
            val messages = _uiState.value.debugMessages + listOf("检查API配置...")
            _uiState.value = _uiState.value.copy(debugMessages = messages)
            
            // 这里可以添加实际的API配置检查逻辑
            
            _uiState.value = _uiState.value.copy(
                waitingForConfirmation = true,
                debugMessages = _uiState.value.debugMessages + listOf("API配置检查完成")
            )
        }
    }
    
    // 解析文本内容
    fun parseEventsFromText() {
        if (_uiState.value.inputText.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "请输入文本内容"
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = ""
        )
        
        viewModelScope.launch {
            try {
                println("开始解析文本: \"${_uiState.value.inputText}\"")
                val settings = appSettings.settingsFlow.first()
                val apiKeyConfigured = when (settings.defaultAiModel) {
                    AiModel.CHATGPT -> settings.chatgptApiKey.isNotBlank()
                    AiModel.GROK -> settings.grokApiKey.isNotBlank()
                }
                
                // 获取当前时间和时区信息
                val now = LocalDateTime.now()
                val timeZone = java.util.TimeZone.getDefault()
                val timeZoneId = timeZone.id
                val timeZoneOffset = timeZone.rawOffset / (1000 * 60 * 60)  // 转换为小时
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val currentTimeStr = now.format(formatter)
                
                // 检测用户输入语言
                val userText = _uiState.value.inputText
                val isChineseText = userText.any { it.code in 0x4E00..0x9FFF }
                val responseLanguage = if (isChineseText) "zh" else "en"
                
                // 构建提示
                val prompt = """
                    你是一个日程安排助手。请从以下文本中提取事件信息，并以JSON格式返回。
                    
                    当前时间：$currentTimeStr
                    当前时区：$timeZoneId (UTC${if (timeZoneOffset >= 0) "+" else ""}$timeZoneOffset)
                    
                    JSON格式要求：
                    1. 返回一个对象数组
                    2. 每个对象包含以下字段：
                       - title: 事件标题
                       - description: 事件描述
                       - startTime: 开始时间，格式为yyyy-MM-dd'T'HH:mm:ss
                       - endTime: 结束时间，格式为yyyy-MM-dd'T'HH:mm:ss
                       - location: 地点（如有）
                       - category: 事件类别，可选 STUDY, EXAM, HOMEWORK, MEETING, CLASS, OTHER
                       - priority: 优先级，可选 HIGH, MEDIUM, LOW
                    
                    请使用${if (isChineseText) "中文" else "英文"}进行理解和处理。
                    请仅返回JSON数组，不要包含其他说明文字。如果无法从文本中提取事件，请返回一个空数组[]。
                    
                    文本内容：${_uiState.value.inputText}
                """.trimIndent()
                
                println("准备发送API请求")
                
                // 根据配置选择解析方法
                var schedules = listOf<Schedule>()
                
                if (apiKeyConfigured) {
                    // 调用AI服务
                    val model = settings.defaultAiModel
                    println("使用${model}模型进行解析, 语言: ${if (isChineseText) "中文" else "英文"}, 时区: $timeZoneId")
                    
                    val result = aiService.parseTextWithAI(prompt, model)
                    println("API响应结果: $result")
                    
                    // 解析JSON
                    if (!result.isNullOrBlank()) {
                        schedules = parseJsonToSchedules(result)
                    } else {
                        println("API返回空结果")
                    }
                } else {
                    // 使用本地简单解析作为后备
                    println("API密钥未配置，使用本地解析")
                    schedules = localParseText(_uiState.value.inputText)
                }
                
                println("解析完成，找到${schedules.size}个日程")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    parsedSchedules = schedules,
                    errorMessage = if (schedules.isEmpty()) "未能从文本中解析出日程信息" else ""
                )
            } catch (e: Exception) {
                println("解析过程发生异常: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "解析失败: ${e.message}"
                )
            }
        }
    }
    
    // 解析文本内容（调试模式）
    private fun parseTextContent() {
        viewModelScope.launch {
            try {
                val messages = _uiState.value.debugMessages + listOf("开始解析文本内容...")
                _uiState.value = _uiState.value.copy(
                    debugMessages = messages,
                    isLoading = true
                )
                
                // 检查API配置
                val settings = appSettings.settingsFlow.first()
                val apiKeyConfigured = when (settings.defaultAiModel) {
                    AiModel.CHATGPT -> settings.chatgptApiKey.isNotBlank()
                    AiModel.GROK -> settings.grokApiKey.isNotBlank()
                }
                val model = settings.defaultAiModel
                
                val updatedMessages = _uiState.value.debugMessages + 
                    listOf("API配置状态: ${model}密钥已${if (apiKeyConfigured) "配置" else "未配置"}")
                _uiState.value = _uiState.value.copy(debugMessages = updatedMessages)
                
                // 获取当前时间和时区信息
                val now = LocalDateTime.now()
                val timeZone = java.util.TimeZone.getDefault()
                val timeZoneId = timeZone.id
                val timeZoneOffset = timeZone.rawOffset / (1000 * 60 * 60)  // 转换为小时
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val currentTimeStr = now.format(formatter)
                
                // 检测用户输入语言
                val userText = _uiState.value.inputText
                val isChineseText = userText.any { it.code in 0x4E00..0x9FFF }
                val responseLanguage = if (isChineseText) "zh" else "en"
                
                _uiState.value = _uiState.value.copy(
                    debugMessages = _uiState.value.debugMessages + 
                        listOf("当前时间: $currentTimeStr, 时区: $timeZoneId (UTC${if (timeZoneOffset >= 0) "+" else ""}$timeZoneOffset)")
                )
                
                _uiState.value = _uiState.value.copy(
                    debugMessages = _uiState.value.debugMessages + 
                        listOf("检测到用户语言: ${if (isChineseText) "中文" else "英文"}")
                )
                
                // 构建提示
                val prompt = """
                    你是一个日程安排助手。请从以下文本中提取事件信息，并以JSON格式返回。
                    
                    当前时间：$currentTimeStr
                    当前时区：$timeZoneId (UTC${if (timeZoneOffset >= 0) "+" else ""}$timeZoneOffset)
                    
                    JSON格式要求：
                    1. 返回一个对象数组
                    2. 每个对象包含以下字段：
                       - title: 事件标题
                       - description: 事件描述
                       - startTime: 开始时间，格式为yyyy-MM-dd'T'HH:mm:ss
                       - endTime: 结束时间，格式为yyyy-MM-dd'T'HH:mm:ss
                       - location: 地点（如有）
                       - category: 事件类别，可选 STUDY, EXAM, HOMEWORK, MEETING, CLASS, OTHER
                       - priority: 优先级，可选 HIGH, MEDIUM, LOW
                    
                    请使用${if (isChineseText) "中文" else "英文"}进行理解和处理。
                    请仅返回JSON数组，不要包含其他说明文字。如果无法从文本中提取事件，请返回一个空数组[]。
                    
                    文本内容：${_uiState.value.inputText}
                """.trimIndent()
                
                _uiState.value = _uiState.value.copy(
                    debugMessages = _uiState.value.debugMessages + 
                        listOf("准备发送API请求，提示内容长度: ${prompt.length}字符")
                )
                
                // 根据配置选择解析方法
                var schedules = listOf<Schedule>()
                var apiResponse = ""
                
                if (apiKeyConfigured) {
                    // 调用AI服务
                    _uiState.value = _uiState.value.copy(
                        debugMessages = _uiState.value.debugMessages + 
                            listOf("使用${model}模型发送请求, 语言: ${if (isChineseText) "中文" else "英文"}, 时区: $timeZoneId...")
                    )
                    
                    apiResponse = aiService.parseTextWithAI(prompt, model) ?: ""
                    
                    _uiState.value = _uiState.value.copy(
                        debugMessages = _uiState.value.debugMessages + 
                            listOf("收到API响应，长度: ${apiResponse.length}字符")
                    )
                    
                    // 解析JSON
                    if (apiResponse.isNotBlank()) {
                        _uiState.value = _uiState.value.copy(
                            debugMessages = _uiState.value.debugMessages + 
                                listOf("开始解析JSON响应")
                        )
                        
                        schedules = parseJsonToSchedules(apiResponse)
                        
                        _uiState.value = _uiState.value.copy(
                            debugMessages = _uiState.value.debugMessages + 
                                listOf("JSON解析完成，找到${schedules.size}个日程")
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            debugMessages = _uiState.value.debugMessages + 
                                listOf("API返回空结果")
                        )
                    }
                } else {
                    // 使用本地简单解析作为后备
                    _uiState.value = _uiState.value.copy(
                        debugMessages = _uiState.value.debugMessages + 
                            listOf("API密钥未配置，使用本地解析")
                    )
                    
                    schedules = localParseText(_uiState.value.inputText)
                    
                    _uiState.value = _uiState.value.copy(
                        debugMessages = _uiState.value.debugMessages + 
                            listOf("本地解析完成，找到${schedules.size}个日程")
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    parsedSchedules = schedules,
                    waitingForConfirmation = true,
                    debugMessages = _uiState.value.debugMessages + 
                        listOf("解析过程已完成，找到 ${schedules.size} 个日程")
                )
            } catch (e: Exception) {
                println("调试解析过程发生异常: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    waitingForConfirmation = true,
                    debugMessages = _uiState.value.debugMessages + 
                        listOf("解析过程发生异常: ${e.message}")
                )
            }
        }
    }
    
    // 解析JSON为Schedule列表
    private fun parseJsonToSchedules(jsonString: String): List<Schedule> {
        try {
            println("开始解析JSON: $jsonString")
            
            // 提取JSON部分
            var cleanedJson = jsonString
            
            // 如果响应包含```json和```标记，则提取中间部分
            if (cleanedJson.contains("```")) {
                val jsonPattern = "```(?:json)?\\s*([\\s\\S]*?)\\s*```".toRegex()
                val matchResult = jsonPattern.find(cleanedJson)
                if (matchResult != null) {
                    cleanedJson = matchResult.groupValues[1].trim()
                    println("从响应中提取JSON部分: $cleanedJson")
                }
            }
            
            // 如果还不是以[开头的有效JSON，尝试定位JSON数组部分
            if (!cleanedJson.trim().startsWith("[") && cleanedJson.contains("[")) {
                val startIndex = cleanedJson.indexOf("[")
                val endIndex = cleanedJson.lastIndexOf("]") + 1
                if (startIndex >= 0 && endIndex > startIndex) {
                    cleanedJson = cleanedJson.substring(startIndex, endIndex)
                    println("进一步提取JSON数组: $cleanedJson")
                }
            }
            
            // 尝试直接解析为Schedule对象
            return try {
                val type = object : TypeToken<List<Schedule>>() {}.type
                Gson().fromJson<List<Schedule>>(cleanedJson, type)
            } catch (e: Exception) {
                println("无法直接解析为Schedule对象: ${e.message}")
                
                // 尝试解析为Map后手动转换
                val mapType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val eventMaps = Gson().fromJson<List<Map<String, Any>>>(cleanedJson, mapType)
                
                eventMaps.mapNotNull { map ->
                    try {
                        // 提取必要字段
                        val title = map["title"] as? String ?: return@mapNotNull null
                        val description = map["description"] as? String ?: ""
                        val location = map["location"] as? String ?: ""
                        
                        // 解析时间
                        val startTimeStr = map["startTime"] as? String ?: return@mapNotNull null
                        val endTimeStr = map["endTime"] as? String ?: return@mapNotNull null
                        
                        // 支持多种格式
                        val formatters = listOf(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        )
                        
                        val startDateTime = parseDateTime(startTimeStr, formatters) 
                            ?: LocalDateTime.now()
                        val endDateTime = parseDateTime(endTimeStr, formatters) 
                            ?: startDateTime.plusHours(1)
                        
                        // 解析分类和优先级
                        val categoryStr = (map["category"] as? String)?.uppercase() ?: "OTHER"
                        val category = try {
                            ScheduleCategory.valueOf(categoryStr)
                        } catch (e: Exception) {
                            ScheduleCategory.OTHER
                        }
                        
                        val priorityStr = (map["priority"] as? String)?.uppercase() ?: "MEDIUM"
                        val priority = try {
                            SchedulePriority.valueOf(priorityStr)
                        } catch (e: Exception) {
                            SchedulePriority.MEDIUM
                        }
                        
                        // 创建Schedule对象
                        Schedule(
                            id = 0,
                            title = title,
                            description = description,
                            startTime = startDateTime,
                            endTime = endDateTime,
                            location = location,
                            category = category,
                            priority = priority
                        )
                    } catch (e: Exception) {
                        println("解析单个事件时出错: ${e.message}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            println("解析JSON异常: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    // 本地简单文本解析方法
    private fun localParseText(text: String): List<Schedule> {
        println("使用本地解析解析文本: \"$text\"")
        
        val result = mutableListOf<Schedule>()
        
        try {
            // 简单的时间和地点匹配
            val timePattern = "(今天|明天|后天|下周[一二三四五六日]|\\d+月\\d+日|\\d+[/\\-]\\d+)?(上午|下午|晚上)?\\s*(\\d+)\\s*(:|：)?(\\d+)?".toRegex()
            val locationPattern = "在\\s*([\\w\\d一-龥]+[室厅堂楼栋馆区])".toRegex()
            
            // 尝试提取时间
            val now = LocalDateTime.now()
            var startDateTime = now.plusHours(1)
            
            val timeMatch = timePattern.find(text)
            if (timeMatch != null) {
                val dateHint = timeMatch.groupValues[1] // 可能是 "今天", "明天", "后天"等
                val amPm = timeMatch.groupValues[2] // 可能是 "上午", "下午", "晚上"
                val hour = timeMatch.groupValues[3].toIntOrNull() ?: 12
                val minute = timeMatch.groupValues[5].toIntOrNull() ?: 0
                
                // 处理日期
                startDateTime = when {
                    dateHint == null || dateHint.isEmpty() -> now
                    dateHint.contains("今天") -> now
                    dateHint.contains("明天") -> now.plusDays(1)
                    dateHint.contains("后天") -> now.plusDays(2)
                    dateHint.startsWith("下周") -> {
                        val dayOfWeek = when {
                            dateHint.contains("一") -> 1
                            dateHint.contains("二") -> 2
                            dateHint.contains("三") -> 3
                            dateHint.contains("四") -> 4
                            dateHint.contains("五") -> 5
                            dateHint.contains("六") -> 6
                            dateHint.contains("日") || dateHint.contains("天") -> 7
                            else -> 1
                        }
                        now.plusWeeks(1).with(java.time.DayOfWeek.of(dayOfWeek))
                    }
                    dateHint.contains("月") && dateHint.contains("日") -> {
                        // 处理"5月20日"格式
                        val parts = dateHint.split("月", "日")
                        val month = parts[0].toIntOrNull() ?: now.monthValue
                        val day = parts[1].toIntOrNull() ?: now.dayOfMonth
                        LocalDateTime.of(now.year, month, day, 0, 0)
                    }
                    dateHint.contains("/") || dateHint.contains("-") -> {
                        // 处理"5/20"或"5-20"格式
                        val separator = if (dateHint.contains("/")) "/" else "-"
                        val parts = dateHint.split(separator)
                        val month = parts[0].toIntOrNull() ?: now.monthValue
                        val day = parts[1].toIntOrNull() ?: now.dayOfMonth
                        LocalDateTime.of(now.year, month, day, 0, 0)
                    }
                    else -> now
                }
                
                // 处理时间
                var adjustedHour = hour
                if (amPm == "下午" || amPm == "晚上") {
                    if (hour < 12) {
                        adjustedHour += 12
                    }
                }
                
                // 设置时分
                startDateTime = startDateTime
                    .withHour(adjustedHour)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0)
            }
            
            // 提取地点
            var location = ""
            val locationMatch = locationPattern.find(text)
            if (locationMatch != null) {
                location = locationMatch.groupValues[1]
            }
            
            // 猜测事件标题和描述
            var titleAndDesc = text
            
            // 移除已提取的时间和地点
            if (timeMatch != null) {
                titleAndDesc = titleAndDesc.replace(timeMatch.value, "")
            }
            if (locationMatch != null) {
                titleAndDesc = titleAndDesc.replace(locationMatch.value, "")
            }
            
            // 清理标点符号
            titleAndDesc = titleAndDesc.trim().replace("，", ",").replace("。", ".")
            
            // 猜测标题和描述
            val parts = titleAndDesc.split("[,.]".toRegex(), 2)
            val title = parts[0].trim().ifEmpty { "未命名事件" }
            val description = if (parts.size > 1) parts[1].trim() else ""
            
            // 猜测类别
            val category = guessCategory(title + description)
            
            // 创建Schedule对象
            val schedule = Schedule(
                id = 0,
                title = title,
                description = description,
                startTime = startDateTime,
                endTime = startDateTime.plusHours(1),
                location = location,
                category = category,
                priority = SchedulePriority.MEDIUM
            )
            
            result.add(schedule)
            println("本地解析创建日程: 标题=\"${schedule.title}\", 时间=${schedule.startTime}, 地点=${schedule.location}")
        } catch (e: Exception) {
            println("本地解析过程出错: ${e.message}")
            e.printStackTrace()
        }
        
        return result
    }
    
    // 根据文本猜测事件类别
    private fun guessCategory(text: String): ScheduleCategory {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("学习") || lowerText.contains("复习") || lowerText.contains("study") -> ScheduleCategory.STUDY
            lowerText.contains("考试") || lowerText.contains("测试") || lowerText.contains("exam") -> ScheduleCategory.EXAM
            lowerText.contains("作业") || lowerText.contains("作业") || lowerText.contains("homework") -> ScheduleCategory.HOMEWORK
            lowerText.contains("会议") || lowerText.contains("讨论") || lowerText.contains("meeting") -> ScheduleCategory.MEETING
            lowerText.contains("课") || lowerText.contains("class") || lowerText.contains("lecture") -> ScheduleCategory.CLASS
            else -> ScheduleCategory.OTHER
        }
    }
    
    // 移除已解析的日程
    fun removeParsedSchedule(schedule: Schedule) {
        val currentSchedules = _uiState.value.parsedSchedules.toMutableList()
        currentSchedules.remove(schedule)
        _uiState.value = _uiState.value.copy(parsedSchedules = currentSchedules)
        println("已移除日程: ${schedule.title}, 剩余 ${currentSchedules.size} 个日程")
    }
    
    // 清除所有已解析的日程
    fun clearParsedSchedules() {
        _uiState.value = _uiState.value.copy(parsedSchedules = emptyList())
        println("已清除所有日程")
    }
}