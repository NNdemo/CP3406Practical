package com.example.myapplication.di

import com.example.myapplication.data.ai.ChatGPTApiService
import com.example.myapplication.data.ai.DeepSeekApiService
import com.example.myapplication.data.ai.GrokApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm")
            .create()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    @Named("chatgpt")
    fun provideChatGPTRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    @Named("grok")
    fun provideGrokRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.x.ai/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideDeepSeekApiService(@Named("deepseek") retrofit: Retrofit): DeepSeekApiService {
        return retrofit.create(DeepSeekApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideChatGPTApiService(@Named("chatgpt") retrofit: Retrofit): ChatGPTApiService {
        return retrofit.create(ChatGPTApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideGrokApiService(@Named("grok") retrofit: Retrofit): GrokApiService {
        return retrofit.create(GrokApiService::class.java)
    }
} 