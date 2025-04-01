package com.example.myapplication.data.ai

import com.example.myapplication.data.AppSettings
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.ui.viewmodels.AiModel
import com.example.myapplication.ui.viewmodels.EventDetail
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val appSettings: AppSettings,
    private val deepSeekApiService: DeepSeekApiService,
    private val chatGPTApiService: ChatGPTApiService,
    private val grokApiService: GrokApiService,
    private val gson: Gson
) {
    companion object {
//        private const val DEEPSEEK_MODEL = "deepseek-chat"
        private const val CHATGPT_MODEL = "gpt-3.5-turbo"
        private const val GROK_MODEL = "grok-1"
    }

    suspend fun parseScheduleFromText(text: String): Schedule? = withContext(Dispatchers.IO) {
        val settings = appSettings.settingsFlow.first()
        
        return@withContext when (settings.defaultAiModel) {
//            AiModel.DEEPSEEK -> parseWithDeepseek(text, settings.deepseekApiKey)
            AiModel.CHATGPT -> parseWithChatGPT(text, settings.chatgptApiKey)
            AiModel.GROK -> parseWithGrok(text, settings.grokApiKey)
        }
    }
    
//    private suspend fun parseWithDeepseek(text: String, apiKey: String): Schedule? {
//        if (apiKey.isEmpty()) return null
//
//        try {
//            val prompt = buildPrompt(text)
//            val request = ChatRequest(
//                model = DEEPSEEK_MODEL,
//                messages = listOf(Message("user", prompt))
//            )
//
//            val response = deepSeekApiService.chat("Bearer $apiKey", request)
//            if (!response.isSuccessful) return null
//
//            val result = response.body()?.choices?.firstOrNull()?.message?.content
//            return result?.let { parseResponse(it) }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return null
//        }
//    }
    
    private suspend fun parseWithChatGPT(text: String, apiKey: String): Schedule? {
        if (apiKey.isEmpty()) return null
        
        try {
            val prompt = buildPrompt(text)
            val request = ChatRequest(
                model = CHATGPT_MODEL,
                messages = listOf(Message("user", prompt)),
                temperature = 0.2, // 降低温度以获得更精确的结果
                max_tokens = 1500
            )
            
            val response = chatGPTApiService.chat("Bearer $apiKey", request)
            if (!response.isSuccessful) {
                println("API调用失败: ${response.code()} - ${response.errorBody()?.string()}")
                return null
            }
            
            val result = response.body()?.choices?.firstOrNull()?.message?.content
            if (result.isNullOrBlank()) {
                println("API返回结果为空")
                return null
            }
            
            println("API返回原始结果: $result")
            return parseResponse(result)
        } catch (e: Exception) {
            e.printStackTrace()
            println("解析失败: ${e.message}")
            return null
        }
    }
    
    private suspend fun parseWithGrok(text: String, apiKey: String): Schedule? {
        if (apiKey.isEmpty()) {
            println("Grok API 密钥为空")
            return null
        }
        
        try {
            println("开始调用Grok API解析日程...")
            
            val prompt = buildPrompt(text)
            println("Grok 日程解析请求提示: ${prompt.substring(0, Math.min(100, prompt.length))}...")
            
            // 配置请求
            val messages = listOf(Message("user", prompt))
            println("Grok 日程解析请求消息类型: ${messages.firstOrNull()?.role}")
            
            val request = ChatRequest(
                model = GROK_MODEL,
                messages = messages,
                temperature = 0.2,
                max_tokens = 1500,
                stream = false
            )
            println("Grok 日程解析请求参数: model=${request.model}, temperature=${request.temperature}, max_tokens=${request.max_tokens}")
            
            // 发送请求
            println("发送日程解析请求到Grok API")
            val response = try {
                grokApiService.chat("Bearer $apiKey", request)
            } catch (e: Exception) {
                println("Grok API日程解析请求异常: ${e.javaClass.name} - ${e.message}")
                e.printStackTrace()
                return null
            }
            
            // 检查响应
            println("Grok API日程解析响应状态码: ${response.code()}")
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "无错误信息"
                println("Grok API日程解析调用失败: ${response.code()} - $errorBody")
                return null
            }
            
            // 解析响应
            val responseBody = response.body()
            println("Grok API日程解析响应体: $responseBody")
            if (responseBody == null) {
                println("Grok API日程解析响应体为空")
                return null
            }
            
            val choices = responseBody.choices
            println("Grok API日程解析响应choices: $choices")
            if (choices.isEmpty()) {
                println("Grok API日程解析响应choices为空")
                return null
            }
            
            val result = choices.firstOrNull()?.message?.content
            if (result.isNullOrBlank()) {
                println("Grok API日程解析返回结果为空")
                return null
            }
            
            println("Grok API日程解析原始结果: $result")
            return parseResponse(result)
        } catch (e: Exception) {
            e.printStackTrace()
            println("Grok日程解析失败: ${e.javaClass.name} - ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    // 获取多个事件的方法
    suspend fun getMultipleEventsFromText(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val settings = appSettings.settingsFlow.first()
            
            return@withContext when (settings.defaultAiModel) {
                AiModel.CHATGPT -> getMultipleEventsFromChatGPT(prompt, settings.chatgptApiKey)
                AiModel.GROK -> {
                    try {
                        getMultipleEventsFromGrok(prompt, settings.grokApiKey)
                    } catch (e: Exception) {
                        println("Grok API调用失败，尝试回退到ChatGPT: ${e.message}")
                        // 如果Grok API调用失败，尝试使用ChatGPT
                        if (settings.chatgptApiKey.isNotBlank()) {
                            getMultipleEventsFromChatGPT(prompt, settings.chatgptApiKey)
                        } else {
                            null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("获取多个事件全局异常: ${e.javaClass.name} - ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }
    
    // 使用ChatGPT获取多个事件
    private suspend fun getMultipleEventsFromChatGPT(prompt: String, apiKey: String): String? {
        if (apiKey.isEmpty()) return null
        
        try {
            val request = ChatRequest(
                model = CHATGPT_MODEL,
                messages = listOf(Message("user", prompt)),
                temperature = 0.3,
                max_tokens = 2500
            )
            
            val response = chatGPTApiService.chat("Bearer $apiKey", request)
            if (!response.isSuccessful) {
                println("ChatGPT多事件API调用失败: ${response.code()} - ${response.errorBody()?.string()}")
                return null
            }
            
            val result = response.body()?.choices?.firstOrNull()?.message?.content
            if (result.isNullOrBlank()) {
                println("ChatGPT多事件API返回结果为空")
                return null
            }
            
            println("ChatGPT多事件API返回原始结果: $result")
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            println("ChatGPT多事件解析失败: ${e.message}")
            return null
        }
    }
    
    // 使用Grok获取多个事件
    private suspend fun getMultipleEventsFromGrok(prompt: String, apiKey: String): String? {
        if (apiKey.isEmpty()) {
            println("Grok API 密钥为空")
            return null
        }
        
        try {
            println("开始调用Grok API...")
            
            // 配置请求
            val messages = listOf(Message("user", prompt))
            println("Grok 请求消息: $messages")
            
            val request = ChatRequest(
                model = GROK_MODEL,
                messages = messages,
                temperature = 0.3,
                max_tokens = 2500,
                stream = false
            )
            println("Grok 请求参数: model=${request.model}, temperature=${request.temperature}, max_tokens=${request.max_tokens}, stream=${request.stream}")
            
            // 发送请求
            println("发送请求到Grok API，URL: https://api.x.ai/v1/chat/completions")
            val response = try {
                grokApiService.chat("Bearer $apiKey", request)
            } catch (e: Exception) {
                println("Grok API请求异常: ${e.javaClass.name} - ${e.message}")
                e.printStackTrace()
                return null
            }
            
            // 检查响应
            println("Grok API响应状态码: ${response.code()}")
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "无错误信息"
                println("Grok多事件API调用失败: ${response.code()} - $errorBody")
                return null
            }
            
            // 解析响应
            val responseBody = response.body()
            println("Grok API响应体: $responseBody")
            if (responseBody == null) {
                println("Grok API响应体为空")
                return null
            }
            
            val choices = responseBody.choices
            println("Grok API响应choices: $choices")
            if (choices.isEmpty()) {
                println("Grok API响应choices为空")
                return null
            }
            
            val result = choices.firstOrNull()?.message?.content
            if (result.isNullOrBlank()) {
                println("Grok多事件API返回结果为空")
                return null
            }
            
            println("Grok多事件API返回原始结果: $result")
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            println("Grok多事件解析失败: ${e.javaClass.name} - ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    // 解析问题答案的方法
    suspend fun getResponseToQuestion(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val settings = appSettings.settingsFlow.first()
            
            return@withContext when (settings.defaultAiModel) {
                AiModel.CHATGPT -> getResponseFromChatGPT(prompt, settings.chatgptApiKey)
                AiModel.GROK -> {
                    try {
                        getResponseFromGrok(prompt, settings.grokApiKey)
                    } catch (e: Exception) {
                        println("Grok API问题答案调用失败，尝试回退到ChatGPT: ${e.message}")
                        // 如果Grok API调用失败，尝试使用ChatGPT
                        if (settings.chatgptApiKey.isNotBlank()) {
                            getResponseFromChatGPT(prompt, settings.chatgptApiKey)
                        } else {
                            "API调用失败：${e.message}。请检查网络连接和API设置。"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("获取问题答案全局异常: ${e.javaClass.name} - ${e.message}")
            e.printStackTrace()
            return@withContext "处理请求时发生错误：${e.message}。请检查网络连接和API设置。"
        }
    }
    
    // 使用ChatGPT获取问题答案
    private suspend fun getResponseFromChatGPT(prompt: String, apiKey: String): String {
        if (apiKey.isEmpty()) return "ChatGPT API密钥未设置，无法进行提问"
        
        try {
            val request = ChatRequest(
                model = CHATGPT_MODEL,
                messages = listOf(Message("user", prompt)),
                temperature = 0.5,
                max_tokens = 1500
            )
            
            val response = chatGPTApiService.chat("Bearer $apiKey", request)
            if (!response.isSuccessful) {
                return "ChatGPT API调用失败: ${response.code()}"
            }
            
            val result = response.body()?.choices?.firstOrNull()?.message?.content
            return result ?: "未能获取ChatGPT回答"
        } catch (e: Exception) {
            e.printStackTrace()
            return "处理问题失败: ${e.message}"
        }
    }
    
    // 使用Grok获取问题答案
    private suspend fun getResponseFromGrok(prompt: String, apiKey: String): String {
        if (apiKey.isEmpty()) return "Grok API密钥未设置，无法进行提问"
        
        try {
            println("开始调用Grok API获取问题答案...")
            
            // 配置请求
            val messages = listOf(Message("user", prompt))
            println("Grok 问题请求消息: $messages")
            
            val request = ChatRequest(
                model = GROK_MODEL,
                messages = messages,
                temperature = 0.5,
                max_tokens = 1500,
                stream = false
            )
            println("Grok 问题请求参数: model=${request.model}, temperature=${request.temperature}, max_tokens=${request.max_tokens}")
            
            // 发送请求
            println("发送问题请求到Grok API")
            val response = try {
                grokApiService.chat("Bearer $apiKey", request)
            } catch (e: Exception) {
                println("Grok API问题请求异常: ${e.javaClass.name} - ${e.message}")
                e.printStackTrace()
                return "Grok API请求异常: ${e.message}"
            }
            
            // 检查响应
            println("Grok API问题响应状态码: ${response.code()}")
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "无错误信息"
                println("Grok API问题调用失败: ${response.code()} - $errorBody")
                return "Grok API调用失败: ${response.code()} - $errorBody"
            }
            
            // 解析响应
            val responseBody = response.body()
            println("Grok API问题响应体: $responseBody")
            if (responseBody == null) {
                println("Grok API问题响应体为空")
                return "Grok API响应为空"
            }
            
            val choices = responseBody.choices
            println("Grok API问题响应choices: $choices")
            if (choices.isEmpty()) {
                println("Grok API问题响应choices为空")
                return "Grok API未返回答案"
            }
            
            val result = choices.firstOrNull()?.message?.content
            println("Grok API问题原始结果: $result")
            return result ?: "未能获取Grok回答"
        } catch (e: Exception) {
            e.printStackTrace()
            println("Grok问题处理失败: ${e.javaClass.name} - ${e.message}")
            return "处理问题失败: ${e.message}"
        }
    }
    
    // 解析多个事件的JSON
    fun parseMultipleEventsJson(jsonString: String): List<EventDetail> {
        try {
            println("开始解析JSON: ${jsonString}")
            
            // 尝试提取JSON部分（防止返回结果包含额外文本）
            val jsonPattern = "\\[\\s*\\{[\\s\\S]*\\}\\s*\\]".toRegex()
            val jsonMatch = jsonPattern.find(jsonString)
            val jsonContent = jsonMatch?.value ?: jsonString
            
            println("提取的JSON内容: ${jsonContent}")
            
            // 解析JSON数组
            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
            val eventsList: List<Map<String, Any>> = try {
                gson.fromJson(jsonContent, listType)
            } catch (e: JsonSyntaxException) {
                println("JSON语法错误1: ${e.message}")
                
                // 尝试更宽松的JSON提取
                val backupJsonPattern = "\\[.*\\]".toRegex(RegexOption.DOT_MATCHES_ALL)
                val backupJsonMatch = backupJsonPattern.find(jsonString)
                if (backupJsonMatch != null) {
                    println("使用备用正则提取JSON")
                    try {
                        gson.fromJson(backupJsonMatch.value, listType)
                    } catch (e2: JsonSyntaxException) {
                        println("JSON语法错误2: ${e2.message}")
                        
                        // 再次失败，尝试创建虚拟事件
                        println("创建虚拟事件数据")
                        createMockEventsList(jsonString)
                    }
                } else {
                    println("备用正则提取失败，创建虚拟事件")
                    createMockEventsList(jsonString)
                }
            }
            
            println("成功解析JSON数组，包含 ${eventsList.size} 个事件")
            
            // 转换为EventDetail对象
            return eventsList.mapNotNull { eventMap ->
                try {
                    val title = eventMap["title"]?.toString() ?: eventMap["标题"]?.toString() ?: ""
                    val startTime = eventMap["startTime"]?.toString() 
                        ?: eventMap["开始时间"]?.toString() 
                        ?: eventMap["start_time"]?.toString() 
                        ?: ""
                    val endTime = eventMap["endTime"]?.toString() 
                        ?: eventMap["结束时间"]?.toString() 
                        ?: eventMap["end_time"]?.toString() 
                        ?: ""
                    val category = eventMap["category"]?.toString() 
                        ?: eventMap["类别"]?.toString() 
                        ?: "OTHER"
                    val location = eventMap["location"]?.toString() 
                        ?: eventMap["地点"]?.toString() 
                        ?: ""
                    val priority = eventMap["priority"]?.toString() 
                        ?: eventMap["优先级"]?.toString() 
                        ?: "MEDIUM"
                    val description = eventMap["description"]?.toString() 
                        ?: eventMap["描述"]?.toString() 
                        ?: ""
                    
                    // 清理和标准化类别和优先级
                    val cleanCategory = cleanupCategory(category)
                    val cleanPriority = cleanupPriority(priority)
                    
                    // 标准化日期时间格式
                    val (cleanStartTime, cleanEndTime) = standardizeDateTimes(startTime, endTime)
                    
                    if (title.isBlank()) {
                        println("事件标题为空，跳过")
                        null
                    } else {
                        println("成功解析事件: $title")
                        EventDetail(
                            title = title,
                            startTime = cleanStartTime.ifBlank { getCurrentTimeFormatted() },
                            endTime = cleanEndTime.ifBlank { getOneHourLaterFormatted() },
                            category = cleanCategory,
                            location = location,
                            priority = cleanPriority,
                            description = description
                        )
                    }
                } catch (e: Exception) {
                    println("处理事件失败: ${e.javaClass.name} - ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("解析JSON总异常: ${e.javaClass.name} - ${e.message}")
            return emptyList()
        }
    }
    
    // 创建模拟事件列表，用于在无法解析JSON时提供基本功能
    private fun createMockEventsList(text: String): List<Map<String, Any>> {
        println("创建模拟事件数据")
        
        val mockEvent = mapOf(
            "title" to "自动创建的事件",
            "startTime" to getCurrentTimeFormatted(),
            "endTime" to getOneHourLaterFormatted(),
            "category" to "OTHER",
            "location" to "",
            "priority" to "MEDIUM",
            "description" to text
        )
        
        return listOf(mockEvent)
    }
    
    // 获取当前时间，格式化为 yyyy-MM-dd HH:mm
    private fun getCurrentTimeFormatted(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return now.format(formatter)
    }
    
    // 获取一小时后的时间，格式化为 yyyy-MM-dd HH:mm
    private fun getOneHourLaterFormatted(): String {
        val oneHourLater = LocalDateTime.now().plusHours(1)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return oneHourLater.format(formatter)
    }
    
    // 从回答中尝试提取更新后的事件信息
    fun tryExtractUpdatedEvent(response: String, currentEvent: EventDetail): EventDetail {
        try {
            // 先尝试从回答中提取JSON
            val jsonPattern = "\\{[\\s\\S]*\\}".toRegex()
            val jsonMatch = jsonPattern.find(response)
            
            if (jsonMatch != null) {
                val jsonContent = jsonMatch.value
                val eventMap: Map<String, Any> = try {
                    gson.fromJson(jsonContent, object : TypeToken<Map<String, Any>>() {}.type)
                } catch (e: JsonSyntaxException) {
                    return currentEvent
                }
                
                val title = eventMap["title"]?.toString() ?: eventMap["标题"]?.toString() ?: currentEvent.title
                val startTime = eventMap["startTime"]?.toString() 
                    ?: eventMap["开始时间"]?.toString() 
                    ?: eventMap["start_time"]?.toString() 
                    ?: currentEvent.startTime
                val endTime = eventMap["endTime"]?.toString() 
                    ?: eventMap["结束时间"]?.toString() 
                    ?: eventMap["end_time"]?.toString() 
                    ?: currentEvent.endTime
                val category = eventMap["category"]?.toString() 
                    ?: eventMap["类别"]?.toString() 
                    ?: currentEvent.category
                val location = eventMap["location"]?.toString() 
                    ?: eventMap["地点"]?.toString() 
                    ?: currentEvent.location
                val priority = eventMap["priority"]?.toString() 
                    ?: eventMap["优先级"]?.toString() 
                    ?: currentEvent.priority
                val description = eventMap["description"]?.toString() 
                    ?: eventMap["描述"]?.toString() 
                    ?: currentEvent.description
                
                // 清理和标准化类别和优先级
                val cleanCategory = cleanupCategory(category)
                val cleanPriority = cleanupPriority(priority)
                
                // 标准化日期时间格式
                val (cleanStartTime, cleanEndTime) = standardizeDateTimes(startTime, endTime)
                
                return EventDetail(
                    id = currentEvent.id,
                    title = title,
                    startTime = cleanStartTime.ifBlank { currentEvent.startTime },
                    endTime = cleanEndTime.ifBlank { currentEvent.endTime },
                    category = cleanCategory,
                    location = location,
                    priority = cleanPriority,
                    description = description,
                    status = currentEvent.status
                )
            }
            
            // 如果没有找到JSON，返回原事件
            return currentEvent
        } catch (e: Exception) {
            e.printStackTrace()
            return currentEvent
        }
    }
    
    // 清理和标准化类别
    private fun cleanupCategory(category: String): String {
        val upperCategory = category.uppercase(Locale.getDefault())
        return when {
            upperCategory.contains("STUDY") -> "STUDY"
            upperCategory.contains("EXAM") -> "EXAM"
            upperCategory.contains("HOMEWORK") -> "HOMEWORK"
            upperCategory.contains("MEETING") -> "MEETING"
            upperCategory.contains("CLASS") -> "CLASS"
            else -> "OTHER"
        }
    }
    
    // 清理和标准化优先级
    private fun cleanupPriority(priority: String): String {
        val upperPriority = priority.uppercase(Locale.getDefault())
        return when {
            upperPriority.contains("HIGH") -> "HIGH"
            upperPriority.contains("LOW") -> "LOW"
            else -> "MEDIUM"
        }
    }
    
    // 标准化日期时间格式
    private fun standardizeDateTimes(startTime: String, endTime: String): Pair<String, String> {
        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd H:mm"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"),
            DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
        )
        
        var parsedStartTime: LocalDateTime? = null
        var parsedEndTime: LocalDateTime? = null
        var startFormatter: DateTimeFormatter? = null
        
        // 尝试解析开始时间
        for (formatter in formatters) {
            try {
                parsedStartTime = LocalDateTime.parse(startTime, formatter)
                startFormatter = formatter
                break
            } catch (e: DateTimeParseException) {
                continue
            }
        }
        
        // 尝试解析结束时间
        for (formatter in formatters) {
            try {
                parsedEndTime = LocalDateTime.parse(endTime, formatter)
                break
            } catch (e: DateTimeParseException) {
                continue
            }
        }
        
        // 标准化输出格式
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val cleanStartTime = parsedStartTime?.format(outputFormatter) ?: startTime
        val cleanEndTime = parsedEndTime?.format(outputFormatter) ?: endTime
        
        return Pair(cleanStartTime, cleanEndTime)
    }
    
    private fun buildPrompt(text: String): String {
        return """
            请解析以下文本中的日程安排信息，并以JSON格式返回。如果信息不完整，请根据上下文合理推断：
            "$text"
            
            提取要求：
            1. 标题：提取事件的主要内容作为标题
            2. 开始时间：格式为"yyyy-MM-dd HH:mm"，如果仅提供时间没有日期，请根据上下文推断（如"明天"、"下周一"等）
            3. 结束时间：格式同上，如未明确指出，请合理估计（通常会议/考试为1-2小时）
            4. 描述：任何关于事件的额外信息
            5. 类别：必须是以下之一：STUDY（学习）、EXAM（考试）、HOMEWORK（作业）、MEETING（会议）、CLASS（课程）、OTHER（其他）
            6. 是否全天：布尔值，默认为false
            7. 提醒时间：默认为开始时间前30分钟，格式同上
            8. 地点：事件发生的地点
            9. 优先级：必须是以下之一：HIGH（高）、MEDIUM（中）、LOW（低）
            
            JSON格式示例：
            {
                "title": "数学考试",
                "startTime": "2025-03-20 14:00",
                "endTime": "2025-03-20 16:00",
                "description": "期中考试，涵盖第1-5章内容",
                "category": "EXAM",
                "isAllDay": false,
                "reminderTime": "2025-03-20 13:30",
                "location": "C214教室",
                "priority": "HIGH"
            }
            
            请确保JSON格式正确，所有字段的名称和格式必须与示例一致。所有字段都必须提供，如果文本中没有明确提及的信息，请合理推断。category和priority必须使用上述指定的枚举值。
        """.trimIndent()
    }
    
    private fun parseResponse(jsonString: String): Schedule? {
        return try {
            // 尝试提取JSON部分（防止返回结果包含额外文本）
            val jsonPattern = "\\{[\\s\\S]*\\}".toRegex()
            val jsonMatch = jsonPattern.find(jsonString)
            val jsonContent = jsonMatch?.value ?: jsonString
            
            val result = gson.fromJson(jsonContent, ScheduleParseResult::class.java)
            
            // 检查必要字段是否为空
            if (result.title.isBlank() || result.startTime.isBlank() || result.endTime.isBlank()) {
                println("关键字段为空: title=${result.title}, startTime=${result.startTime}, endTime=${result.endTime}")
                return null
            }
            
            // 尝试使用多种日期格式解析
            val formatters = listOf(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd H:mm")
            )
            
            var startDateTime: LocalDateTime? = null
            var endDateTime: LocalDateTime? = null
            var reminderDateTime: LocalDateTime? = null
            
            // 尝试不同的格式解析日期
            for (formatter in formatters) {
                try {
                    if (startDateTime == null) startDateTime = LocalDateTime.parse(result.startTime, formatter)
                    if (endDateTime == null) endDateTime = LocalDateTime.parse(result.endTime, formatter)
                    if (reminderDateTime == null && !result.reminderTime.isNullOrBlank()) {
                        reminderDateTime = LocalDateTime.parse(result.reminderTime, formatter)
                    }
                    
                    // 如果所有日期都已解析成功，跳出循环
                    if (startDateTime != null && endDateTime != null && 
                        (reminderDateTime != null || result.reminderTime.isNullOrBlank())) {
                        break
                    }
                } catch (e: DateTimeParseException) {
                    // 尝试下一个格式
                    continue
                }
            }
            
            // 如果仍然无法解析日期，返回null
            if (startDateTime == null || endDateTime == null) {
                println("无法解析日期: startTime=${result.startTime}, endTime=${result.endTime}")
                return null
            }
            
            // 确保类别和优先级是有效的枚举值
            val categoryStr = result.category.uppercase(Locale.getDefault())
            val priorityStr = result.priority.uppercase(Locale.getDefault())
            
            val category = try {
                ScheduleCategory.valueOf(categoryStr)
            } catch (e: IllegalArgumentException) {
                println("无效的类别: $categoryStr，使用默认值OTHER")
                ScheduleCategory.OTHER
            }
            
            val priority = try {
                SchedulePriority.valueOf(priorityStr)
            } catch (e: IllegalArgumentException) {
                println("无效的优先级: $priorityStr，使用默认值MEDIUM")
                SchedulePriority.MEDIUM
            }
            
            // 创建Schedule对象
            Schedule(
                title = result.title,
                description = result.description ?: "",
                startTime = startDateTime,
                endTime = endDateTime,
                isAllDay = result.isAllDay ?: false,
                category = category,
                reminderTime = reminderDateTime,
                location = result.location ?: "",
                priority = priority
            )
        } catch (e: Exception) {
            e.printStackTrace()
            println("JSON解析异常: ${e.message}")
            null
        }
    }
    
    // 通用AI文本解析方法，带详细日志
    suspend fun parseTextWithAI(prompt: String, model: AiModel): String? = withContext(Dispatchers.IO) {
        try {
            val settings = appSettings.settingsFlow.first()
            
            println("parseTextWithAI: 开始调用AI服务, 模型: $model")
            
            return@withContext when (model) {
                AiModel.CHATGPT -> {
                    if (settings.chatgptApiKey.isBlank()) {
                        println("parseTextWithAI: ChatGPT API密钥未配置")
                        return@withContext null
                    }
                    
                    println("parseTextWithAI: 使用ChatGPT解析文本")
                    parseTextWithChatGPT(prompt, settings.chatgptApiKey)
                }
                AiModel.GROK -> {
                    if (settings.grokApiKey.isBlank()) {
                        println("parseTextWithAI: Grok API密钥未配置")
                        return@withContext null
                    }
                    
                    println("parseTextWithAI: 使用Grok解析文本")
                    parseTextWithGrok(prompt, settings.grokApiKey)
                }
            }
        } catch (e: Exception) {
            println("parseTextWithAI: 全局异常: ${e.javaClass.name} - ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }
    
    // 使用ChatGPT解析文本
    private suspend fun parseTextWithChatGPT(prompt: String, apiKey: String): String? {
        if (apiKey.isEmpty()) {
            println("parseTextWithChatGPT: API密钥为空")
            return null
        }
        
        try {
            println("parseTextWithChatGPT: 配置请求参数")
            val request = ChatRequest(
                model = CHATGPT_MODEL,
                messages = listOf(Message("user", prompt)),
                temperature = 0.2,
                max_tokens = 2500
            )
            
            println("parseTextWithChatGPT: 发送请求")
            val response = chatGPTApiService.chat("Bearer $apiKey", request)
            
            println("parseTextWithChatGPT: 收到响应, 状态码: ${response.code()}")
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "无错误信息"
                println("parseTextWithChatGPT: 请求失败: ${response.code()} - $errorBody")
                return null
            }
            
            val result = response.body()?.choices?.firstOrNull()?.message?.content
            println("parseTextWithChatGPT: 原始响应: ${result}")
            
            if (result.isNullOrBlank()) {
                println("parseTextWithChatGPT: 响应内容为空")
                return null
            }
            
            return result
        } catch (e: Exception) {
            println("parseTextWithChatGPT: 异常: ${e.javaClass.name} - ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    // 直接调用Grok API解析文本（专用于调试）
    suspend fun parseTextWithGrok(prompt: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            println("parseTextWithGrok: API密钥为空")
            return@withContext null
        }else{
            println("parseTextWithGrok: API密钥不为空")
        }
        
        try {
            println("parseTextWithGrok: 配置请求参数")
            
            // 配置请求 - 根据x.ai官方文档
            val messages = listOf(
                Message("system", "You are a helpful assistant that extracts event information from text and returns it in JSON format."),
                Message("user", prompt)
            )
            
            val request = ChatRequest(
                model = "grok-2-1212",  // 使用正确的模型名称
                messages = messages,
                temperature = 0.2,
                max_tokens = 2500,
                stream = false
            )
            
            println("parseTextWithGrok: 请求参数 - model=${request.model}, messages=${messages.size}")
            println("parseTextWithGrok: 系统消息: ${messages[0].content}")
            println("parseTextWithGrok: 用户消息: ${messages[1].content}")
            
            // 发送请求到x.ai API (确保URL正确)
            println("parseTextWithGrok: 发送请求到 https://api.x.ai/v1/chat/completions")
            val response = try {
                grokApiService.chat("Bearer $apiKey", request)
            } catch (e: Exception) {
                println("parseTextWithGrok: 请求发送异常: ${e.javaClass.name} - ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
            
            // 检查响应
            println("parseTextWithGrok: 响应状态码: ${response.code()}")
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "无错误信息"
                println("parseTextWithGrok: 请求失败: ${response.code()} - $errorBody")
                return@withContext null
            }
            
            // 解析响应
            val responseBody = response.body()
            println("parseTextWithGrok: 响应体: $responseBody")
            if (responseBody == null) {
                println("parseTextWithGrok: 响应体为空")
                return@withContext null
            }
            
            // 详细记录API响应
            println("parseTextWithGrok: id=${responseBody.id ?: "无ID"}")
            
            val choices = responseBody.choices
            if (choices.isEmpty()) {
                println("parseTextWithGrok: 响应choices为空")
                return@withContext null
            }
            
            val result = choices.firstOrNull()?.message?.content
            println("parseTextWithGrok: 原始响应内容: ${result}")
            println("parseTextWithGrok: finish_reason=${choices.firstOrNull()?.finish_reason}")
            
            if (result.isNullOrBlank()) {
                println("parseTextWithGrok: 响应内容为空")
                return@withContext null
            }
            
            return@withContext result
        } catch (e: Exception) {
            println("parseTextWithGrok: 异常: ${e.javaClass.name} - ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }
} 