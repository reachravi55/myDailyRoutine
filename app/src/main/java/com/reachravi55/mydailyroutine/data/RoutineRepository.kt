package com.reachravi55.mydailyroutine.data

import android.content.Context
import com.reachravi55.mydailyroutine.alarms.AlarmScheduler
import com.reachravi55.mydailyroutine.proto.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class RoutineRepository private constructor(private val appContext: Context) {

    private val store = appContext.routineStore

    fun flow(): Flow<RoutineStore> = store.data

    suspend fun ensureInitialized() {
        store.updateData { current ->
            if (current.hasSettings()) current
            else current.toBuilder().setSettings(Settings.newBuilder().setNotificationsEnabled(true)).build()
        }
    }

    suspend fun createList(name: String): String {
        val id = Ids.id()
        store.updateData { current ->
            val maxOrder = current.listsList.maxOfOrNull { it.sortOrder } ?: 0
            val list = RoutineList.newBuilder()
                .setId(id)
                .setName(name.trim())
                .setSortOrder(maxOrder + 1)
                .build()
            val b = current.toBuilder()
            b.addLists(list)
            if (current.activeListId.isBlank()) b.activeListId = id
            b.build()
        }
        return id
    }

    suspend fun renameList(listId: String, newName: String) {
        store.updateData { current ->
            val idx = current.listsList.indexOfFirst { it.id == listId }
            if (idx < 0) return@updateData current
            val updated = current.listsList[idx].toBuilder().setName(newName.trim()).build()
            current.toBuilder().setLists(idx, updated).build()
        }
    }

    suspend fun deleteList(listId: String) {
        store.updateData { current ->
            val b = current.toBuilder()
            // remove tasks in that list + overrides for those tasks
            val taskIds = current.tasksList.filter { it.listId == listId }.map { it.id }.toSet()

            b.clearTasks()
            current.tasksList.filterNot { it.listId == listId }.forEach { b.addTasks(it) }

            b.clearOverrides()
            current.overridesList.filterNot { taskIds.contains(it.taskId) }.forEach { b.addOverrides(it) }

            b.clearLists()
            current.listsList.filterNot { it.id == listId }.forEach { b.addLists(it) }

            if (b.activeListId == listId) {
                b.activeListId = b.listsList.firstOrNull()?.id ?: ""
            }
            b.build()
        }
    }

    suspend fun setActiveList(listId: String) {
        store.updateData { it.toBuilder().setActiveListId(listId).build() }
    }

    suspend fun upsertTask(task: Task) {
        store.updateData { current ->
            val b = current.toBuilder()
            val idx = current.tasksList.indexOfFirst { it.id == task.id }
            if (idx >= 0) b.setTasks(idx, task) else b.addTasks(task)
            b.build()
        }
        // reschedule alarms for this task (next 30 days)
        AlarmScheduler.rescheduleTask(appContext, task, daysAhead = 60)
    }

    suspend fun deleteTask(taskId: String) {
        val task = store.data // flow; can't read here. We'll just remove and rely on boot to resched.
        store.updateData { current ->
            val b = current.toBuilder()
            b.clearTasks()
            current.tasksList.filterNot { it.id == taskId }.forEach { b.addTasks(it) }
            b.clearOverrides()
            current.overridesList.filterNot { it.taskId == taskId }.forEach { b.addOverrides(it) }
            b.build()
        }
    }

    suspend fun setOccurrence(taskId: String, dateKey: String, completed: Boolean, note: String? = null) {
        store.updateData { current ->
            val b = current.toBuilder()
            val idx = current.overridesList.indexOfFirst { it.taskId == taskId && it.date == dateKey }
            val base = if (idx >= 0) current.overridesList[idx] else OccurrenceOverride.getDefaultInstance()
            val ob = base.toBuilder()
                .setTaskId(taskId)
                .setDate(dateKey)
                .setCompleted(completed)

            if (note != null) ob.note = note

            if (idx >= 0) b.setOverrides(idx, ob.build()) else b.addOverrides(ob.build())
            b.build()
        }
    }

    suspend fun setSubtaskOccurrence(taskId: String, dateKey: String, subtaskId: String, completed: Boolean, note: String? = null) {
        store.updateData { current ->
            val b = current.toBuilder()
            val idx = current.overridesList.indexOfFirst { it.taskId == taskId && it.date == dateKey }
            val base = if (idx >= 0) current.overridesList[idx] else OccurrenceOverride.newBuilder().setTaskId(taskId).setDate(dateKey).build()

            val ob = base.toBuilder()
            val sIdx = base.subtaskOverridesList.indexOfFirst { it.subtaskId == subtaskId }
            val sBase = if (sIdx >= 0) base.subtaskOverridesList[sIdx] else SubtaskOverride.getDefaultInstance()
            val sb = sBase.toBuilder().setSubtaskId(subtaskId).setCompleted(completed)
            if (note != null) sb.note = note

            if (sIdx >= 0) ob.setSubtaskOverrides(sIdx, sb.build()) else ob.addSubtaskOverrides(sb.build())

            if (idx >= 0) b.setOverrides(idx, ob.build()) else b.addOverrides(ob.build())
            b.build()
        }
    }

    suspend fun clearDay(dateKey: String) {
        store.updateData { current ->
            val b = current.toBuilder()
            b.clearOverrides()
            current.overridesList.filterNot { it.date == dateKey }.forEach { b.addOverrides(it) }
            b.build()
        }
    }

    suspend fun toggleNotifications(enabled: Boolean) {
        store.updateData { current ->
            val s = current.settings.toBuilder().setNotificationsEnabled(enabled).build()
            current.toBuilder().setSettings(s).build()
        }
    }

    companion object {
        @Volatile private var INSTANCE: RoutineRepository? = null
        fun get(context: Context): RoutineRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RoutineRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
