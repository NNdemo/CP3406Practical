package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.TaskDao
import com.example.myapplication.data.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    
    fun getActiveTasks(): Flow<List<Task>> = taskDao.getActiveTasks()
    
    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()
    
    fun getActiveTaskCount(): Flow<Int> = taskDao.getActiveTaskCount()
    
    fun getCompletedTaskCount(): Flow<Int> = taskDao.getCompletedTaskCount()
    
    suspend fun createTask(title: String, description: String = "", dueDate: LocalDateTime? = null): Long {
        val task = Task(
            title = title,
            description = description,
            dueDate = dueDate
        )
        return taskDao.insert(task)
    }
    
    suspend fun updateTask(task: Task) = taskDao.update(task)
    
    suspend fun deleteTask(task: Task) = taskDao.delete(task)
    
    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)
    
    suspend fun toggleTaskCompletion(taskId: Long, completed: Boolean) {
        val completedAt = if (completed) LocalDateTime.now() else null
        taskDao.updateTaskCompletion(taskId, completed, completedAt)
    }
} 