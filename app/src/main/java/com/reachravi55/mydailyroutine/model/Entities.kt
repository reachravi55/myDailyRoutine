package com.reachravi55.mydailyroutine.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY }

@Entity(tableName = "task_templates")
data class TaskTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String
)