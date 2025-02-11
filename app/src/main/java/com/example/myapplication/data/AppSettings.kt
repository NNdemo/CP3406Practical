package com.example.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.ui.viewmodels.AiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val deepseekApiKey = stringPreferencesKey("deepseek_api_key")
    private val chatgptApiKey = stringPreferencesKey("chatgpt_api_key")
    private val defaultAiModel = stringPreferencesKey("default_ai_model")

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            deepseekApiKey = preferences[deepseekApiKey] ?: "",
            chatgptApiKey = preferences[chatgptApiKey] ?: "",
            defaultAiModel = preferences[defaultAiModel]?.let { AiModel.valueOf(it) } ?: AiModel.DEEPSEEK
        )
    }

    suspend fun updateDeepseekApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[deepseekApiKey] = apiKey
        }
    }

    suspend fun updateChatgptApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[chatgptApiKey] = apiKey
        }
    }

    suspend fun updateDefaultAiModel(model: AiModel) {
        context.dataStore.edit { preferences ->
            preferences[defaultAiModel] = model.name
        }
    }

    data class Settings(
        val deepseekApiKey: String,
        val chatgptApiKey: String,
        val defaultAiModel: AiModel
    )
} 