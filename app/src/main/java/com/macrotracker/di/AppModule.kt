package com.macrotracker.di

import android.content.Context
import com.macrotracker.data.local.AppDatabase
import com.macrotracker.data.local.dao.DailyLogDao
import com.macrotracker.data.local.dao.FoodRecipeDao
import com.macrotracker.data.local.dao.HabitAndBackupDao
import com.macrotracker.data.local.dao.PantryShoppingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.create(context)
    }

    @Provides
    fun provideFoodRecipeDao(db: AppDatabase): FoodRecipeDao = db.foodRecipeDao()

    @Provides
    fun provideDailyLogDao(db: AppDatabase): DailyLogDao = db.dailyLogDao()

    @Provides
    fun providePantryShoppingDao(db: AppDatabase): PantryShoppingDao = db.pantryShoppingDao()

    @Provides
    fun provideHabitAndBackupDao(db: AppDatabase): HabitAndBackupDao = db.habitAndBackupDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
