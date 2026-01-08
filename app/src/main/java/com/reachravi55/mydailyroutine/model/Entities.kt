package com.reachravi55.mydailyroutine.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY }

/** Template for a checklist (reusable) */
@Entity(tableName = "task_templates")
data class TaskTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String
)

/** Example of other entities (kept minimal for scaffold) */
@Entity(tableName = "template_items")
data class TemplateItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val label: String,
    val position: Int = 0
)

@Entity(tableName = "daily_checklists")
data class DailyChecklist(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String?,
    val title: String,
    val dateMillis: Long
)

@Entity(tableName = "daily_items")
data class DailyItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dailyChecklistId: String,
    val label: String,
    val completed: Boolean = false,
    val position: Int = 0
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String?,
    val checklistId: String?,
    val timeMinutes: Int, // minutes since midnight for a single time (simple)
    val recurrence: Recurrence = Recurrence.DAILY,
    val enabled: Boolean = true
)