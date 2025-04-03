package com.example.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.ui.viewmodels.AiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
//    private val deepseekApiKey = stringPreferencesKey("deepseek_api_key")
    private val chatgptApiKey = stringPreferencesKey("chatgpt_api_key")
    private val grokApiKey = stringPreferencesKey("grok_api_key")
    private val defaultAiModel = stringPreferencesKey("default_ai_model")
    
    // JCU相关设置
    private val jcuUsername = stringPreferencesKey("jcu_username")
    private val jcuPassword = stringPreferencesKey("jcu_password")
    private val jcuSaveCredentials = stringPreferencesKey("jcu_save_credentials")
    private val jcuRefreshInterval = stringPreferencesKey("jcu_refresh_interval")

    // 出勤率相关的键
    private val webClassAttendanceRate = floatPreferencesKey("web_class_attendance_rate")
    private val webCampusAttendanceRate = floatPreferencesKey("web_campus_attendance_rate")

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
//            deepseekApiKey = preferences[deepseekApiKey] ?: "",
            chatgptApiKey = preferences[chatgptApiKey] ?: "",
            grokApiKey = preferences[grokApiKey] ?: "",
            defaultAiModel = preferences[defaultAiModel]?.let { AiModel.valueOf(it) } ?: AiModel.CHATGPT,
            jcuUsername = preferences[jcuUsername] ?: "",
            jcuPassword = preferences[jcuPassword] ?: "",
            jcuSaveCredentials = preferences[jcuSaveCredentials]?.toBoolean() ?: false,
            jcuRefreshInterval = preferences[jcuRefreshInterval]?.toInt() ?: 6,
            webClassAttendanceRate = preferences[webClassAttendanceRate] ?: 0f,
            webCampusAttendanceRate = preferences[webCampusAttendanceRate] ?: 0f
        )
    }

//    suspend fun updateDeepseekApiKey(apiKey: String) {
//        context.dataStore.edit { preferences ->
//            preferences[deepseekApiKey] = apiKey
//        }
//    }

    suspend fun updateChatgptApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[chatgptApiKey] = apiKey
        }
    }

    suspend fun updateGrokApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[grokApiKey] = apiKey
        }
    }

    suspend fun updateDefaultAiModel(model: AiModel) {
        context.dataStore.edit { preferences ->
            preferences[defaultAiModel] = model.name
        }
    }
    
    // JCU相关的设置更新方法
    suspend fun updateJcuUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[jcuUsername] = username
        }
    }
    
    suspend fun updateJcuPassword(password: String) {
        context.dataStore.edit { preferences ->
            preferences[jcuPassword] = password
        }
    }
    
    suspend fun updateJcuSaveCredentials(save: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[jcuSaveCredentials] = save.toString()
            // 如果不保存凭证，则清除密码
            if (!save) {
                preferences[jcuPassword] = ""
            }
        }
    }
    
    suspend fun updateJcuRefreshInterval(hours: Int) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences[jcuRefreshInterval] = hours.toString()
        }
    }
    
    // 清除JCU凭证
    suspend fun clearJcuCredentials() {
        context.dataStore.edit { preferences ->
            preferences[jcuUsername] = ""
            preferences[jcuPassword] = ""
            preferences[jcuSaveCredentials] = "false"
        }
    }

    // 更新网页出勤率
    suspend fun updateWebAttendanceRates(classRate: Float, campusRate: Float) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences[webClassAttendanceRate] = classRate
            preferences[webCampusAttendanceRate] = campusRate
        }
    }
    
    // 同步更新网页出勤率（用于仓库类非协程上下文）
    fun updateWebAttendanceRatesSynchronously(classRate: Float, campusRate: Float) {
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[webClassAttendanceRate] = classRate
                preferences[webCampusAttendanceRate] = campusRate
            }
        }
    }

    data class Settings(
//        val deepseekApiKey: String,
        val chatgptApiKey: String,
        val grokApiKey: String,
        val defaultAiModel: AiModel,
        // JCU相关设置
        val jcuUsername: String = "",
        val jcuPassword: String = "",
        val jcuSaveCredentials: Boolean = false,
        val jcuRefreshInterval: Int = 6,
        val webClassAttendanceRate: Float = 0f,
        val webCampusAttendanceRate: Float = 0f
    )
} 