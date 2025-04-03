package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.dao.ScheduleDao
import com.example.myapplication.data.dao.TaskDao
import com.example.myapplication.data.dao.StudySessionDao
import com.example.myapplication.data.dao.JcuClassDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return AppDatabase.getDatabase(appContext)
    }
    
    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao {
        return database.scheduleDao()
    }
    
    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }
    
    @Provides
    fun provideStudySessionDao(database: AppDatabase): StudySessionDao {
        return database.studySessionDao()
    }
    
    @Provides
    fun provideJcuClassDao(database: AppDatabase): JcuClassDao {
        return database.jcuClassDao()
    }
} 