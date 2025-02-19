package com.example.myapplication.ui.viewmodels

import android.app.Application
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.Manifest
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ai.AiService
import com.example.myapplication.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VoiceCreateViewModel @Inject constructor(
    application: Application,
    private val scheduleRepository: ScheduleRepository,
    private val aiService: AiService
) : AndroidViewModel(application) {
    
    var recognizedText by mutableStateOf("")
        private set
        
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    fun startRecording() {
        if (!hasRecordPermission()) return
        
        try {
            audioFile = File(getApplication<Application>().cacheDir, "audio_record.mp3")
            
            mediaRecorder = MediaRecorder(getApplication()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: 处理错误
        }
    }
    
    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            // TODO: 调用语音识别 API
            // 这里暂时使用模拟数据
            recognizedText = "明天下午两点到四点有数学考试"
            
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: 处理错误
        }
    }
    
    fun createSchedule() {
        viewModelScope.launch {
            try {
                val schedule = aiService.parseScheduleFromText(recognizedText)
                schedule?.let {
                    scheduleRepository.createSchedule(
                        title = it.title,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        description = it.description,
                        category = it.category,
                        isAllDay = it.isAllDay,
                        reminderTime = it.reminderTime,
                        location = it.location,
                        priority = it.priority
                    )
                }
                // TODO: 显示成功消息
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: 显示错误消息
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaRecorder = null
        audioFile?.delete()
    }
} 