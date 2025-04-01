package com.example.myapplication.data.ai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface DeepSeekApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

interface ChatGPTApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

interface GrokApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1000,
    val stream: Boolean = false
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: Message,
    val finish_reason: String
)

data class ScheduleParseResult(
    val title: String,
    val startTime: String,
    val endTime: String,
    val description: String?,
    val category: String,
    val isAllDay: Boolean?,
    val reminderTime: String?,
    val location: String?,
    val priority: String
) 