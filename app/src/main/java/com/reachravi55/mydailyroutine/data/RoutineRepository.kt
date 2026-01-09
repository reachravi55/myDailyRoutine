package com.reachravi55.mydailyroutine.data

import android.content.Context
import androidx.datastore.core.DataStore
import com.reachravi55.mydailyroutine.proto.OccurrenceOverride
import com.reachravi55.mydailyroutine.proto.RoutineList
import com.reachravi55.mydailyroutine.proto.RoutineStore
import com.reachravi55.mydailyroutine.proto.Settings
import com.reachravi55.mydailyroutine.proto.Subtask
import com.reachravi55.mydailyroutine.proto.SubtaskState
import com.reachravi55.mydailyroutine.proto.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RoutineRepository private constructor(
    private val store: DataStore<RoutineStore>,
) {
    fun flow(): Flow<RoutineStore> = store.data

    /**
     * Ensures a usable starting state: at least one list and a selected active list.
     */
    suspend fun ensureInitialized() {
        store.updateData { s ->
            if (s.listsCount > 0 && s.activeListId.isNotBlank()) return@updateData s

            val current = s.toBuilder()

            val listId = if (s.listsCount > 0) {
                s.listsList.first().id
            } else {
                val defaultListId = Ids.id()
                val defaultList = RoutineList.newBuilder()
                    .setId(defaultListId)
                    .setName("Default")
                    // A pleasant default accent; UI can ignore or override
                    .setColorHex("#4F46E5")
                    .build()
                current.addLists(defaultList)
                defaultListId
            }

            if (current.activeListId.isBlank()) {
                current.activeListId = listId
            }

            current.build()
        }
    }

    suspend fun createList(name: String, colorHex: String = "#4F46E5", makeActive: Boolean = true): String {
        val id = Ids.id()
        store.updateData { s ->
            val b = s.toBuilder()
            b.addLists(
                RoutineList.newBuilder()
                    .setId(id)
                    .setName(name.trim())
                    .setColorHex(colorHex)
                    .build()
            )
            if (makeActive) b.activeListId = id
            b.build()
        }
        return id
    }

    suspend fun renameList(listId: String, name: String) {
        store.updateData { s ->
            val b = s.toBuilder()
            val idx = s.listsList.indexOfFirst { it.id == listId }
            if (idx >= 0) {
                val updated = s.listsList[idx].toBuilder().setName(name.trim()).build()
                b.setLists(idx, updated)
            }
            b.build()
        }
    }

    suspend fun setActiveList(listId: String) {
        store.updateData { s ->
            if (s.activeListId == listId) return@updateData s
            s.toBuilder().setActiveListId(listId).build()
        }
    }

    suspend fun deleteList(listId: String) {
        store.updateData { s ->
            val b = s.toBuilder()

            // Remove list
            val remainingLists = s.listsList.filterNot { it.id == listId }
            b.clearLists()
            remainingLists.forEach { b.addLists(it) }

            // Remove tasks in that list
            val remainingTasks = s.tasksList.filterNot { it.listId == listId }
            b.clearTasks()
            remainingTasks.forEach { b.addTasks(it) }

            // Remove overrides for removed tasks
            val remainingTaskIds = remainingTasks.map { it.id }.toSet()
            val remainingOverrides = s.occurrenceOverridesList.filter { it.taskId in remainingTaskIds }
            b.clearOccurrenceOverrides()
            remainingOverrides.forEach { b.addOccurrenceOverrides(it) }

            // Fix active list
            val nextActive = when {
                s.activeListId != listId -> s.activeListId
                remainingLists.isNotEmpty() -> remainingLists.first().id
                else -> ""
            }
            b.activeListId = nextActive

            b.build()
        }

        // If all lists removed, recreate default
        ensureInitialized()
    }

    suspend fun createOrUpdateTask(task: Task) {
        store.updateData { s ->
            val b = s.toBuilder()
            val idx = s.tasksList.indexOfFirst { it.id == task.id }
            if (idx >= 0) b.setTasks(idx, task) else b.addTasks(task)
            b.build()
        }
    }

    suspend fun deleteTask(taskId: String) {
        store.updateData { s ->
            val b = s.toBuilder()
            val remainingTasks = s.tasksList.filterNot { it.id == taskId }
            b.clearTasks()
            remainingTasks.forEach { b.addTasks(it) }

            val remainingOverrides = s.occurrenceOverridesList.filterNot { it.taskId == taskId }
            b.clearOccurrenceOverrides()
            remainingOverrides.forEach { b.addOccurrenceOverrides(it) }

            b.build()
        }
    }

    suspend fun setTaskCompleted(taskId: String, dateEpochDay: Long, isCompleted: Boolean) {
        store.updateData { s ->
            val b = s.toBuilder()
            val idx = s.occurrenceOverridesList.indexOfFirst { it.taskId == taskId && it.dateEpochDay == dateEpochDay }
            if (idx >= 0) {
                val updated = s.occurrenceOverridesList[idx].toBuilder()
                    .setIsCompleted(isCompleted)
                    .build()
                b.setOccurrenceOverrides(idx, updated)
            } else {
                b.addOccurrenceOverrides(
                    OccurrenceOverride.newBuilder()
                        .setTaskId(taskId)
                        .setDateEpochDay(dateEpochDay)
                        .setIsCompleted(isCompleted)
                        .build()
                )
            }
            b.build()
        }
    }

    suspend fun setSubtaskCompleted(taskId: String, dateEpochDay: Long, subtaskId: String, isCompleted: Boolean) {
        store.updateData { s ->
            val b = s.toBuilder()
            val idx = s.occurrenceOverridesList.indexOfFirst { it.taskId == taskId && it.dateEpochDay == dateEpochDay }

            val overrideBuilder = if (idx >= 0) {
                s.occurrenceOverridesList[idx].toBuilder()
            } else {
                OccurrenceOverride.newBuilder().setTaskId(taskId).setDateEpochDay(dateEpochDay)
            }

            // Update or add subtask state
            val states = overrideBuilder.subtaskStatesList.toMutableList()
            val stateIdx = states.indexOfFirst { it.subtaskId == subtaskId }
            val newState = SubtaskState.newBuilder().setSubtaskId(subtaskId).setIsCompleted(isCompleted).build()
            if (stateIdx >= 0) states[stateIdx] = newState else states.add(newState)

            overrideBuilder.clearSubtaskStates()
            states.forEach { overrideBuilder.addSubtaskStates(it) }

            val updated = overrideBuilder.build()
            if (idx >= 0) b.setOccurrenceOverrides(idx, updated) else b.addOccurrenceOverrides(updated)

            b.build()
        }
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        store.updateData { s ->
            val b = s.toBuilder()
            b.settings = transform(s.settings)
            b.build()
        }
    }

    suspend fun readOnce(): RoutineStore = store.data.first()

    companion object {
        @Volatile private var INSTANCE: RoutineRepository? = null

        fun get(context: Context): RoutineRepository {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val repo = RoutineRepository(appContext.routineStore)
                INSTANCE = repo
                repo
            }
        }
    }
}
