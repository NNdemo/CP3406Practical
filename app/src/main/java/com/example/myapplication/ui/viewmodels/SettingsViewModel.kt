package com.example.myapplication.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AiModel {
//    DEEPSEEK,
    CHATGPT
}

data class SettingsUiState(
//    val deepseekApiKey: String = "",
    val chatgptApiKey: String = "",
    val defaultAiModel: AiModel = AiModel.CHATGPT
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val appSettings: AppSettings
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            appSettings.settingsFlow.collect { settings ->
                _uiState.value = SettingsUiState(
//                    deepseekApiKey = settings.deepseekApiKey,
                    chatgptApiKey = settings.chatgptApiKey,
                    defaultAiModel = settings.defaultAiModel
                )
            }
        }
    }
    
//    fun updateDeepseekApiKey(apiKey: String) {
//        _uiState.value = _uiState.value.copy(deepseekApiKey = apiKey)
//    }
    
    fun updateChatgptApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(chatgptApiKey = apiKey)
    }
    
    fun updateDefaultAiModel(model: AiModel) {
        _uiState.value = _uiState.value.copy(defaultAiModel = model)
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            with(_uiState.value) {
//                appSettings.updateDeepseekApiKey(deepseekApiKey)
                appSettings.updateChatgptApiKey(chatgptApiKey)
                appSettings.updateDefaultAiModel(defaultAiModel)
            }
        }
    }
} 