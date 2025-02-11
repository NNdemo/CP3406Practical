package com.example.myapplication.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.repository.ScheduleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleRepository: ScheduleRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "schedule_reminders"
        private const val CHANNEL_NAME = "日程提醒"
        private const val NOTIFICATION_ID = 1
        
        fun scheduleReminder(
            context: Context,
            scheduleId: Long,
            reminderTime: LocalDateTime
        ) {
            val workManager = WorkManager.getInstance(context)
            
            // 创建工作请求的输入数据
            val inputData = workDataOf(
                "schedule_id" to scheduleId,
                "reminder_time" to reminderTime.toString()
            )
            
            // 计算延迟时间
            val now = LocalDateTime.now()
            val delay = java.time.Duration.between(now, reminderTime)
            
            // 创建工作请求
            val reminderWork = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(inputData)
                .setInitialDelay(delay.seconds, TimeUnit.SECONDS)
                .addTag("reminder_$scheduleId")
                .build()
            
            // 安排工作
            workManager.enqueueUniqueWork(
                "reminder_$scheduleId",
                ExistingWorkPolicy.REPLACE,
                reminderWork
            )
        }
        
        fun cancelReminder(context: Context, scheduleId: Long) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("reminder_$scheduleId")
        }
    }

    override suspend fun doWork(): Result {
        try {
            val scheduleId = inputData.getLong("schedule_id", -1L)
            if (scheduleId == -1L) return Result.failure()

            val schedule = scheduleRepository.getScheduleById(scheduleId) ?: return Result.failure()
            
            // 检查是否已经提醒过
            if (schedule.isReminded) return Result.success()
            
            // 创建通知渠道
            createNotificationChannel()
            
            // 创建打开应用的Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            
            // 构建通知
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("日程提醒")
                .setContentText("${schedule.title} - ${schedule.location}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            // 显示通知
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            // 标记为已提醒
            scheduleRepository.markReminderAsShown(scheduleId)
            
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = "显示日程提醒通知"
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
} 