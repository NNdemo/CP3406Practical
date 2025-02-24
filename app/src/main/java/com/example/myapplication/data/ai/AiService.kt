package com.example.myapplication.data.ai

import com.example.myapplication.data.AppSettings
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.ui.viewmodels.AiModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val appSettings: AppSettings,
    private val deepSeekApiService: DeepSeekApiService,
    private val chatGPTApiService: ChatGPTApiService,
    private val gson: Gson
) {
    companion object {
//        private const val DEEPSEEK_MODEL = "deepseek-chat"
        private const val CHATGPT_MODEL = "gpt-3.5-turbo"
    }

    suspend fun parseScheduleFromText(text: String): Schedule? = withContext(Dispatchers.IO) {
        val settings = appSettings.settingsFlow.first()
        
        return@withContext when (settings.defaultAiModel) {
//            AiModel.DEEPSEEK -> parseWithDeepseek(text, settings.deepseekApiKey)
            AiModel.CHATGPT -> parseWithChatGPT(text, settings.chatgptApiKey)
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
                messages = listOf(Message("user", prompt))
            )
            
            val response = chatGPTApiService.chat("Bearer $apiKey", request)
            if (!response.isSuccessful) return null
            
            val result = response.body()?.choices?.firstOrNull()?.message?.content
            return result?.let { parseResponse(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun buildPrompt(text: String): String {
        return """
            Please parse the following text into schedule information, return in JSON format:
            $text
            
            Requirements:
            1. Extract time information (start time, end time)
            2. Identify event type (STUDY/EXAM/HOMEWORK/MEETING/CLASS/OTHER)
            3. Identify location information (if any)
            4. Determine priority (HIGH/MEDIUM/LOW)
            
            JSON format example:
            {
                "title": "Math Exam",
                "startTime": "2024-03-20 14:00",
                "endTime": "2024-03-20 16:00",
                "category": "EXAM",
                "location": "Building 301",
                "priority": "HIGH"
            }
        """.trimIndent()
    }
    
    private fun parseResponse(jsonString: String): Schedule? {
        return try {
            val result = gson.fromJson(jsonString, ScheduleParseResult::class.java)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            
            Schedule(
                title = result.title,
                startTime = LocalDateTime.parse(result.startTime, formatter),
                endTime = LocalDateTime.parse(result.endTime, formatter),
                category = ScheduleCategory.valueOf(result.category),
                location = result.location ?: "",
                priority = SchedulePriority.valueOf(result.priority)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 