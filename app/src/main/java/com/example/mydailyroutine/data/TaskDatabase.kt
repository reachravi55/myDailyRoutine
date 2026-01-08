package com.example.mydailyroutine.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mydailyroutine.model.*

@Database(entities = [Task::class, ChecklistItem::class, Reminder::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}