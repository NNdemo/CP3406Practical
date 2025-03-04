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
import com.example.myapplication.data.model.RepeatType
import com.example.myapplication.data.model.ScheduleCategory
import com.example.myapplication.data.model.SchedulePriority
import com.example.myapplication.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
            recognizedText = "There will be a math exam at C214 from 2 p.m. to 4 p.m. tomorrow."
            
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: 处理错误
        }
    }
    
    // Define a data class to represent the schedule
    data class Schedule(
        val title: String,
        val startTime: String,
        val endTime: String,
        val description: String,
        val category: String,
        val isAllDay: Boolean,
        val reminderTime: String,
        val location: String,
        val priority: String
    )
    
    fun createSchedule() {
        viewModelScope.launch {
            try {
                // val schedule = aiService.parseScheduleFromText(recognizedText)
                // modified schedule
                val schedule = Schedule(
                    title = "Math Exam",
                    startTime = "2025-03-05T14:00:00",
                    endTime = "2025-03-05T16:00:00",
                    description = "This is a math exam for the students.",
                    category = "exam",
                    isAllDay = false,
                    reminderTime = "2025-03-05T13:30:00",
                    location = "C214",
                    priority = "high"
                )
    
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val startDateTime = LocalDateTime.parse(schedule.startTime, formatter)
                val endDateTime = LocalDateTime.parse(schedule.endTime, formatter)
                val reminderDateTime = LocalDateTime.parse(schedule.reminderTime, formatter)
    
                // Convert the string to the ScheduleCategory enum
                val category = ScheduleCategory.valueOf(schedule.category.uppercase())
    
                // Convert the string to the SchedulePriority enum
                val priority = SchedulePriority.valueOf(schedule.priority.uppercase())

                schedule.let {
                    scheduleRepository.createSchedule(
                        title = it.title,
                        startTime = startDateTime,
                        endTime = endDateTime,
                        description = it.description,
                        category = category,
                        isAllDay = it.isAllDay,
                        reminderTime = reminderDateTime,
                        location = it.location,
                        priority = priority,
                        repeatType = RepeatType.NONE,
                        repeatInterval = 0,
                        repeatUntil = null
                    )
                }
                // TODO: Display success message
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: Display error message
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