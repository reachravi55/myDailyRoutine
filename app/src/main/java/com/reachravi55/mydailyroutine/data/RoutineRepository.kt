package com.reachravi55.mydailyroutine.data

import android.content.Context
import com.reachravi55.mydailyroutine.proto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoutineRepository private constructor(private val appContext: Context) {

    private val store = appContext.store()

    fun flow(): Flow<RoutineStore> = store.data

    suspend fun ensureInitialized() {
        // No defaults (per your request) — but keep settings message non-null.
        store.updateData { current ->
            if (current.hasSettings()) current
            else current.toBuilder().setSettings(UserSettings.getDefaultInstance()).build()
        }
    }

    fun activeListIdFlow(): Flow<String?> = flow().map { s ->
        s.settings.activeListId.takeIf { it.isNotBlank() }
    }

    fun listsFlow(): Flow<List<RoutineList>> = flow().map { it.listsList.sortedBy { l -> l.sortOrder } }
    fun tasksFlow(): Flow<List<Task>> = flow().map { it.tasksList.filter { t -> !t.archived } }

    suspend fun upsertList(list: RoutineList) {
        store.updateData { current ->
            val b = current.toBuilder()
            val idx = current.listsList.indexOfFirst { it.id == list.id }
            if (idx >= 0) b.setLists(idx, list) else b.addLists(list)
            b.build()
        }
    }

    suspend fun deleteList(listId: String) {
        store.updateData { current ->
            val b = current.toBuilder()
            // remove list
            val newLists = current.listsList.filterNot { it.id == listId }
            b.clearLists()
            newLists.forEach { b.addLists(it) }
            // archive tasks in that list
            val newTasks = current.tasksList.map { t ->
                if (t.listId == listId) t.toBuilder().setArchived(true).build() else t
            }
            b.clearTasks()
            newTasks.forEach { b.addTasks(it) }
            b.build()
        }
    }

    suspend fun setActiveList(listId: String?) {
        store.updateData { current ->
            val b = current.toBuilder()
            val s = (if (current.hasSettings()) current.settings else UserSettings.getDefaultInstance()).toBuilder()
            s.activeListId = listId ?: ""
            b.settings = s.build()
            b.build()
        }
    }

    suspend fun upsertTask(task: Task) {
        store.updateData { current ->
            val b = current.toBuilder()
            val idx = current.tasksList.indexOfFirst { it.id == task.id }
            val withTimes = task.toBuilder()
                .setUpdatedAtEpochMs(Instant.now().toEpochMilli())
                .apply {
                    if (task.createdAtEpochMs == 0L) setCreatedAtEpochMs(Instant.now().toEpochMilli())
                }
                .build()
            if (idx >= 0) b.setTasks(idx, withTimes) else b.addTasks(withTimes)
            b.build()
        }
    }

    suspend fun archiveTask(taskId: String) {
        store.updateData { current ->
            val b = current.toBuilder()
            val idx = current.tasksList.indexOfFirst { it.id == taskId }
            if (idx >= 0) {
                b.setTasks(idx, current.tasksList[idx].toBuilder().setArchived(true).build())
            }
            b.build()
        }
    }

    suspend fun setOccurrence(taskId: String, dateKey: String, completed: Boolean, note: String? = null) {
        store.updateData { current ->
            val b = current.toBuilder()
            val existingIdx = current.overridesList.indexOfFirst { it.taskId == taskId && it.date == dateKey }
            val base = if (existingIdx >= 0) current.overridesList[existingIdx] else OccurrenceOverride.getDefaultInstance()

            val ob = base.toBuilder()
                .setTaskId(taskId)
                .setDate(dateKey)
                .setCompleted(completed)

            if (note != null) ob.note = note

            if (existingIdx >= 0) b.setOverrides(existingIdx, ob.build()) else b.addOverrides(ob.build())
            b.build()
        }
    }

    suspend fun setSubtaskState(taskId: String, dateKey: String, subtaskId: String, completed: Boolean, note: String? = null) {
        store.updateData { current ->
            val b = current.toBuilder()
            val existingIdx = current.overridesList.indexOfFirst { it.taskId == taskId && it.date == dateKey }
            val base = if (existingIdx >= 0) current.overridesList[existingIdx] else OccurrenceOverride.getDefaultInstance()
            val ob = base.toBuilder().setTaskId(taskId).setDate(dateKey)

            // upsert subtask state
            val states = ob.subtaskStatesList.toMutableList()
            val sIdx = states.indexOfFirst { it.subtaskId == subtaskId }
            val sb = (if (sIdx >= 0) states[sIdx] else SubtaskState.getDefaultInstance()).toBuilder()
                .setSubtaskId(subtaskId)
                .setCompleted(completed)
            if (note != null) sb.note = note

            if (sIdx >= 0) states[sIdx] = sb.build() else states.add(sb.build())
            ob.clearSubtaskStates()
            states.forEach { ob.addSubtaskStates(it) }

            if (existingIdx >= 0) b.setOverrides(existingIdx, ob.build()) else b.addOverrides(ob.build())
            b.build()
        }
    }

    suspend fun clearDay(dateKey: String) {
        store.updateData { current ->
            val b = current.toBuilder()
            val remaining = current.overridesList.filterNot { it.date == dateKey }
            b.clearOverrides()
            remaining.forEach { b.addOverrides(it) }
            b.build()
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
