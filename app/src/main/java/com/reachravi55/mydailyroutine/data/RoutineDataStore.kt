package com.reachravi55.mydailyroutine.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.reachravi55.mydailyroutine.proto.RoutineStore
import java.io.InputStream
import java.io.OutputStream

object RoutineStoreSerializer : Serializer<RoutineStore> {
    override val defaultValue: RoutineStore = RoutineStore.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): RoutineStore =
        try { RoutineStore.parseFrom(input) } catch (_: Exception) { defaultValue }

    override suspend fun writeTo(t: RoutineStore, output: OutputStream) = t.writeTo(output)
}

private val Context.routineStore: DataStore<RoutineStore> by dataStore(
    fileName = "routine_store.pb",
    serializer = RoutineStoreSerializer
)

internal fun Context.store(): DataStore<RoutineStore> = routineStore
