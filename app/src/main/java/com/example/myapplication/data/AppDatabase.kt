package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapplication.data.dao.JcuClassDao
import com.example.myapplication.data.dao.ScheduleDao
import com.example.myapplication.data.dao.StudySessionDao
import com.example.myapplication.data.dao.TaskDao
import com.example.myapplication.data.model.JcuClass
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.data.model.StudySession
import com.example.myapplication.data.model.Task
import com.example.myapplication.data.util.Converters

@Database(
    entities = [
        Schedule::class,
        Task::class,
        StudySession::class,
        JcuClass::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun taskDao(): TaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun jcuClassDao(): JcuClassDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 