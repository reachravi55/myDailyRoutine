package com.reachravi55.mydailyroutine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reachravi55.mydailyroutine.model.*

@Database(entities = [TaskTemplate::class, TemplateItem::class, DailyChecklist::class, DailyItem::class, Reminder::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mydailyroutine.db").build().also { INSTANCE = it }
            }
    }
}