package com.example.mydailyroutine.model

import androidx.room.*
import java.time.Instant
import java.util.UUID

enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY }

/**
 * A reusable checklist template (e.g., "Morning routine"). Users can create templates and reuse them.
 */
@Entity(tableName = "task_templates")
data class TaskTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = Instant.now().toEpochMilli()
)

/** Items that belong to a template (the checklist entries) */
@Entity(
    tableName = "template_items",
    foreignKeys = [ForeignKey(entity = TaskTemplate::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("templateId")]
)
data class TemplateItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val label: String,
    val position: Int = 0
)

/**
 * A per-day checklist instance - represents a user's checklist for a particular day.
 * Use dateMillis to indicate the local date (normalized to midnight) the checklist is for.
 */
@Entity(tableName = "daily_checklists")
data class DailyChecklist(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String?,          // optional: null means ad-hoc checklist
    val title: String,
    val dateMillis: Long,            // normalized to local date start in millis (e.g., midnight)
    val createdAt: Long = Instant.now().toEpochMilli()
)

/** The state of an item on a daily checklist (completed or not) */
@Entity(
    tableName = "daily_items",
    foreignKeys = [ForeignKey(entity = DailyChecklist::class, parentColumns = ["id"], childColumns = ["dailyChecklistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("dailyChecklistId")]
)
data class DailyItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dailyChecklistId: String,
    val label: String,
    val completed: Boolean = false,
    val position: Int = 0
)

/**
 * Reminders: attach to a template (e.g., remind for "Morning routine") or to a specific checklist (optional).
 * timesJson: JSON array of times-of-day in minutes-since-midnight or "HH:mm" strings (store consistently).
 * repeatDaysJson: used for weekly recurrence (JSON array of ints 1..7), nullable.
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String?,          // reminder attached to template; can be null if attached to a specific checklist
    val checklistId: String?,         // optional override to attach to a specific checklist instance
    val timesJson: String,            // JSON array like "[480, 1020]" -> 08:00 and 17:00 (minutes-since-midnight)
    val recurrence: Recurrence = Recurrence.DAILY,
    val repeatDaysJson: String? = null,
    val enabled: Boolean = true,
    val createdAt: Long = Instant.now().toEpochMilli()
)