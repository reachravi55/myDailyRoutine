package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.proto.RoutineStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DayScreen(store: RoutineStore, date: LocalDate, contentPadding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        TodayScreen(store = store, date = date, contentPadding = PaddingValues(0.dp))
    }
}
