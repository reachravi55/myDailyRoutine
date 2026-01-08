package com.example.mydailyroutine.data

import androidx.room.*
import com.example.mydailyroutine.model.*

@Dao
interface TaskDao {
    // Template operations
    @Query("SELECT * FROM task_templates ORDER BY createdAt DESC")
    suspend fun getAllTemplates(): List<TaskTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: TaskTemplate)

    @Delete
    suspend fun deleteTemplate(template: TaskTemplate)

    @Query("SELECT * FROM template_items WHERE templateId = :templateId ORDER BY position ASC")
    suspend fun getItemsForTemplate(templateId: String): List<TemplateItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplateItem(item: TemplateItem)

    @Delete
    suspend fun deleteTemplateItem(item: TemplateItem)

    // Daily checklist operations
    @Query("SELECT * FROM daily_checklists WHERE dateMillis = :dateMillis ORDER BY createdAt DESC")
    suspend fun getChecklistsForDate(dateMillis: Long): List<DailyChecklist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyChecklist(checklist: DailyChecklist)

    @Delete
    suspend fun deleteDailyChecklist(checklist: DailyChecklist)

    @Query("SELECT * FROM daily_items WHERE dailyChecklistId = :checklistId ORDER BY position ASC")
    suspend fun getItemsForDailyChecklist(checklistId: String): List<DailyItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyItem(item: DailyItem)

    @Update
    suspend fun updateDailyItem(item: DailyItem)

    // Reminders
    @Query("SELECT * FROM reminders WHERE templateId = :templateId OR checklistId = :checklistId")
    suspend fun getReminders(templateId: String?, checklistId: String?): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: String)
}