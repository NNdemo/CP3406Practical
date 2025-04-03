package com.example.myapplication.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppSettings
import com.example.myapplication.data.model.JcuClass
import com.example.myapplication.data.model.JcuClassStatistics
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.network.JcuApiService
import com.example.myapplication.data.repository.JcuClassRepository
import com.example.myapplication.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class JcuViewModel @Inject constructor(
    application: Application,
    private val appSettings: AppSettings,
    private val jcuApiService: JcuApiService,
    private val jcuClassRepository: JcuClassRepository,
    private val scheduleRepository: ScheduleRepository
) : AndroidViewModel(application) {
    
    // 日志标签
    private val TAG = "JcuViewModel"
    
    // UI状态
    private val _uiState = MutableStateFlow(JcuUiState())
    val uiState: StateFlow<JcuUiState> = _uiState.asStateFlow()
    
    // 课程列表Flow
    val allClasses = jcuClassRepository.getAllClasses()
    
    // 统计信息Flow
    val statistics = jcuClassRepository.getClassStatistics()
    
    // 课程代码Flow
    val courseCodes = jcuClassRepository.getAllCourseCodes()
    
    // 即将到来的课程Flow
    val upcomingClasses = jcuClassRepository.getUpcomingClasses()
    
    // 自动刷新Job
    private var autoRefreshJob: Job? = null
    
    init {
        // 从设置中获取刷新间隔
        viewModelScope.launch {
            appSettings.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    refreshInterval = settings.jcuRefreshInterval
                )
            }
        }
        
        // 加载出勤率数据
        viewModelScope.launch {
            jcuClassRepository.loadWebAttendanceRates()
        }
        
        viewModelScope.launch {
            // 监听设置变化
            appSettings.settingsFlow.collect { settings ->
                // 如果刷新间隔更改，更新自动刷新任务
                if (settings.jcuRefreshInterval != _uiState.value.refreshInterval) {
                    _uiState.value = _uiState.value.copy(
                        refreshInterval = settings.jcuRefreshInterval
                    )
                    setupAutoRefresh()
                }
            }
        }
    }
    
    /**
     * 登录JCU
     */
    fun login(username: String, password: String, saveCredentials: Boolean) {
        viewModelScope.launch {
            // 更新状态为加载中
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            // 验证输入
            if (username.isBlank() || password.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Username and password cannot be empty"
                )
                return@launch
            }
            
            try {
                Log.d(TAG, "Starting JCU login process, username: $username")
                
                // 调用登录API
                val result = jcuApiService.login(username, password)
                
                when (result) {
                    is JcuApiService.LoginResult.Success -> {
                        Log.d(TAG, "JCU login successful")
                        
                        // 登录成功，保存凭证（如果需要）
                        if (saveCredentials) {
                            appSettings.updateJcuUsername(username)
                            appSettings.updateJcuPassword(password)
                            appSettings.updateJcuSaveCredentials(true)
                        } else {
                            // 只保存用户名但不保存密码
                            appSettings.updateJcuUsername(username)
                            appSettings.updateJcuSaveCredentials(false)
                        }
                        
                        // 更新状态为登录成功
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            errorMessage = null,
                            username = username
                        )
                        
                        // 直接获取课程数据
                        fetchClasses()
                        
                        // 设置自动刷新
                        setupAutoRefresh()
                    }
                    is JcuApiService.LoginResult.Error -> {
                        // 登录失败
                        Log.e(TAG, "JCU login failed: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                // 异常处理
                Log.e(TAG, "Exception occurred during JCU login", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error during login: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 通过已保存的凭证登录
     */
    fun loginWithSavedCredentials() {
        viewModelScope.launch {
            val settings = appSettings.settingsFlow.first()
            
            if (settings.jcuSaveCredentials && settings.jcuUsername.isNotBlank() && settings.jcuPassword.isNotBlank()) {
                login(settings.jcuUsername, settings.jcuPassword, true)
            } else if (settings.jcuUsername.isNotBlank()) {
                // 只有用户名，需要用户输入密码
                _uiState.value = _uiState.value.copy(
                    username = settings.jcuUsername,
                    needPassword = true
                )
            }
        }
    }
    
    /**
     * 获取课程数据
     */
    fun fetchClasses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            try {
                // 调用API获取课程数据
                val result = jcuApiService.fetchClasses()
                
                when (result) {
                    is JcuApiService.FetchResult.Success -> {
                        // 获取成功，保存到数据库
                        val classes = result.classes
                        jcuClassRepository.syncClasses(classes)
                        
                        // 更新出勤率数据
                        jcuClassRepository.updateWebAttendanceRates(
                            classAttendanceRate = result.classAttendanceRate,
                            campusAttendanceRate = result.campusAttendanceRate
                        )
                        
                        // 更新最后刷新时间
                        val now = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            lastRefreshTime = now.format(formatter)
                        )
                    }
                    is JcuApiService.FetchResult.Error -> {
                        // 获取失败
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                // 异常处理
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error fetching class data: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 导入课程到日程表
     */
    fun importClassesToSchedules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImporting = true
            )
            
            // 获取所有课程
            val classes = jcuClassRepository.getAllClasses().first()
            var importedCount = 0
            var duplicateCount = 0
            
            // 依次转换为Schedule对象并保存
            withContext(Dispatchers.IO) {
                for (jcuClass in classes) {
                    try {
                        val schedule = jcuClass.toSchedule()
                        val id = scheduleRepository.createSchedule(
                            title = schedule.title,
                            startTime = schedule.startTime,
                            endTime = schedule.endTime,
                            description = schedule.description,
                            category = schedule.category,
                            location = schedule.location,
                            priority = schedule.priority
                        )
                        
                        if (id > 0) {
                            importedCount++
                        }
                    } catch (e: android.database.sqlite.SQLiteConstraintException) {
                        // 课程已存在，唯一索引约束冲突
                        Log.d(TAG, "Class already exists, skipping: ${jcuClass.courseCode} - ${jcuClass.startTime}")
                        duplicateCount++
                    } catch (e: Exception) {
                        // 其他异常
                        Log.e(TAG, "Error importing class: ${e.message}", e)
                    }
                }
            }
            
            _uiState.value = _uiState.value.copy(
                isImporting = false,
                lastImportResult = "Successfully imported ${importedCount} classes to schedule (${duplicateCount} already existed)"
            )
        }
    }
    
    /**
     * 设置JCU设置
     */
    fun updateSettings(refreshInterval: Int) {
        viewModelScope.launch {
            appSettings.updateJcuRefreshInterval(refreshInterval)
            setupAutoRefresh()
        }
    }
    
    /**
     * 设置自动刷新任务
     */
    private fun setupAutoRefresh() {
        // 取消已有的自动刷新任务
        autoRefreshJob?.cancel()
        
        // 根据设置中的刷新间隔创建新的自动刷新任务
        val refreshIntervalMillis = _uiState.value.refreshInterval * 60 * 60 * 1000L // 小时转毫秒
        
        if (refreshIntervalMillis > 0 && _uiState.value.isLoggedIn) {
            autoRefreshJob = viewModelScope.launch {
                while (true) {
                    delay(refreshIntervalMillis)
                    if (_uiState.value.isLoggedIn) {
                        fetchClasses()
                    }
                }
            }
        }
    }
    
    /**
     * 登出
     */
    fun logout() {
        viewModelScope.launch {
            // 取消自动刷新任务
            autoRefreshJob?.cancel()
            
            // 清除Cookie
            jcuApiService.clearCookies()
            
            // 更新状态
            _uiState.value = JcuUiState(
                refreshInterval = _uiState.value.refreshInterval,
                username = _uiState.value.username,
                needPassword = true
            )
        }
    }
    
    /**
     * 清除所有课程数据
     */
    fun clearAllClasses() {
        viewModelScope.launch {
            jcuClassRepository.deleteAllClasses()
        }
    }
    
    /**
     * 获取特定课程的统计信息
     */
    fun getCourseStatistics(courseCode: String): Flow<JcuClassStatistics> {
        return jcuClassRepository.getCourseStatistics(courseCode)
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
    
    /**
     * JCU UI状态
     */
    data class JcuUiState(
        val isLoading: Boolean = false,
        val isLoggedIn: Boolean = false,
        val isImporting: Boolean = false,
        val errorMessage: String? = null,
        val username: String = "",
        val needPassword: Boolean = false,
        val lastRefreshTime: String = "",
        val lastImportResult: String = "",
        val refreshInterval: Int = 6 // 默认6小时刷新一次
    )
} 