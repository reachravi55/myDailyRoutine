package com.reachravi55.mydailyroutine.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.reachravi55.mydailyroutine.data.RoutineRepository

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)

    val store by repo.flow().collectAsState(initial = com.reachravi55.mydailyroutine.proto.RoutineStore.getDefaultInstance())
    AppScaffold(store = store)
}
