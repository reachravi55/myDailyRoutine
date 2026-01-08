package com.reachravi55.mydailyroutine.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.reachravi55.mydailyroutine.model.TaskTemplate

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_templates ORDER BY rowid DESC")
    suspend fun getAllTemplates(): List<TaskTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: TaskTemplate)
}