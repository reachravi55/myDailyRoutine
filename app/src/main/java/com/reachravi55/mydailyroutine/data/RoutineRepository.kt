package com.reachravi55.mydailyroutine.data

import android.content.Context
import com.reachravi55.mydailyroutine.proto.OccurrenceOverride
import com.reachravi55.mydailyroutine.proto.RepeatRule
import com.reachravi55.mydailyroutine.proto.RoutineStore
import com.reachravi55.mydailyroutine.proto.Subtask
import com.reachravi55.mydailyroutine.proto.Task
import com.reachravi55.mydailyroutine.proto.TaskList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/**
 * Simple repository wrapper around [RoutineDataStore].
 *
 * NOTE: This project keeps persistence in DataStore (proto) so the repository is mostly
 * a convenience layer for screens / viewmodels.
 */
class RoutineRepository private constructor(
    private val appContext: Context,
    private val store: RoutineDataStore,
) {

    /** Expose the whole store for screens that need to derive multiple views. */
    val routineStoreFlow: Flow<RoutineStore> = store.data

    /** Convenience flow of lists. */
    val listsFlow: Flow<List<TaskList>> = store.data.map { it.listsList }

    /** Ensure we always have at least one list so the UI is never empty. */
    suspend fun ensureInitialized() {
        val current = store.data.first()
        if (current.listsCount == 0) {
            val defaultList = TaskList.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName("My List")
                .build()
            store.update { b ->
                b.addLists(defaultList)
            }
        }
    }

    suspend fun createList(name: String): String {
        val id = UUID.randomUUID().toString()
        val list = TaskList.newBuilder().setId(id).setName(name).build()
        store.update { b -> b.addLists(list) }
        return id
    }

    suspend fun renameList(listId: String, newName: String) {
        store.update { b ->
            val idx = b.listsList.indexOfFirst { it.id == listId }
            if (idx >= 0) {
                val updated = b.listsList[idx].toBuilder().setName(newName).build()
                b.setLists(idx, updated)
            }
        }
    }

    suspend fun deleteList(listId: String) {
        store.update { b ->
            // Remove list
            val idx = b.listsList.indexOfFirst { it.id == listId }
            if (idx >= 0) b.removeLists(idx)
            // Also remove tasks that belonged to that list
            val remaining = b.tasksList.filterNot { it.listId == listId }
            b.clearTasks()
            b.addAllTasks(remaining)
        }
    }

    fun tasksForDate(date: LocalDate): Flow<List<Task>> {
        val dateStr = date.toString()
        return store.data.map { s ->
            val tasks = s.tasksList
            val overrides = s.overridesList

            // Apply per-date overrides and filter cancelled occurrences.
            val cancelledIds = overrides
                .filter { it.date == dateStr && it.isCancelled }
                .map { it.taskId }
                .toSet()

            val overrideById = overrides
                .filter { it.date == dateStr && !it.isCancelled }
                .associateBy { it.taskId }

            tasks
                .filter { t -> t.hasDate() && t.date == dateStr }
                .filterNot { t -> cancelledIds.contains(t.id) }
                .map { t ->
                    val ov = overrideById[t.id]
                    if (ov != null) applyOverride(t, ov) else t
                }
        }
    }

    fun tasksForToday(today: LocalDate = LocalDate.now()): Flow<List<Task>> = tasksForDate(today)

    suspend fun upsertTask(task: Task) {
        store.update { b ->
            val idx = b.tasksList.indexOfFirst { it.id == task.id }
            if (idx >= 0) b.setTasks(idx, task) else b.addTasks(task)
        }
    }

    suspend fun deleteTask(taskId: String) {
        store.update { b ->
            val idx = b.tasksList.indexOfFirst { it.id == taskId }
            if (idx >= 0) b.removeTasks(idx)

            // Remove overrides for this task.
            val remainingOverrides = b.overridesList.filterNot { it.taskId == taskId }
            b.clearOverrides()
            b.addAllOverrides(remainingOverrides)
        }
    }

    suspend fun setTaskCompletedForDate(taskId: String, date: LocalDate, completed: Boolean) {
        val dateStr = date.toString()
        store.update { b ->
            val overrides = b.overridesList.toMutableList()
            val existingIdx = overrides.indexOfFirst { it.taskId == taskId && it.date == dateStr }
            val newOverride = (if (existingIdx >= 0) overrides[existingIdx] else OccurrenceOverride.getDefaultInstance())
                .toBuilder()
                .setTaskId(taskId)
                .setDate(dateStr)
                .setIsCompleted(completed)
                .build()

            if (existingIdx >= 0) overrides[existingIdx] = newOverride else overrides.add(newOverride)
            b.clearOverrides()
            b.addAllOverrides(overrides)
        }
    }

    suspend fun setSubtaskCompletedForDate(taskId: String, date: LocalDate, subtaskId: String, completed: Boolean) {
        val dateStr = date.toString()
        store.update { b ->
            val overrides = b.overridesList.toMutableList()
            val existingIdx = overrides.indexOfFirst { it.taskId == taskId && it.date == dateStr }
            val base = if (existingIdx >= 0) overrides[existingIdx] else OccurrenceOverride.getDefaultInstance()
            val baseBuilder = base.toBuilder().setTaskId(taskId).setDate(dateStr)

            val stIdx = baseBuilder.subtaskStatesList.indexOfFirst { it.subtaskId == subtaskId }
            val st = (if (stIdx >= 0) baseBuilder.subtaskStatesList[stIdx] else com.reachravi55.mydailyroutine.proto.SubtaskState.getDefaultInstance())
                .toBuilder()
                .setSubtaskId(subtaskId)
                .setIsCompleted(completed)
                .build()
            if (stIdx >= 0) baseBuilder.setSubtaskStates(stIdx, st) else baseBuilder.addSubtaskStates(st)

            val newOverride = baseBuilder.build()
            if (existingIdx >= 0) overrides[existingIdx] = newOverride else overrides.add(newOverride)
            b.clearOverrides()
            b.addAllOverrides(overrides)
        }
    }

    companion object {
        @Volatile private var INSTANCE: RoutineRepository? = null

        fun get(context: Context): RoutineRepository {
            val appCtx = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RoutineRepository(appCtx, RoutineDataStore(appCtx)).also { INSTANCE = it }
            }
        }
    }
}

private fun applyOverride(task: Task, override: OccurrenceOverride): Task {
    val b = task.toBuilder()
    if (override.hasIsCompleted()) {
        b.setIsCompleted(override.isCompleted)
    }
    // Subtask states are applied in UI; base task structure unchanged.
    return b.build()
}
