package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.RepeatEngine
import com.reachravi55.mydailyroutine.proto.RoutineStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    store: RoutineStore,
    contentPadding: PaddingValues,
    onOpenDate: (String) -> Unit
) {
    val today = LocalDate.now()
    val days = remember { (0..30).map { today.plusDays(it.toLong()) } }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Text("Calendar", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            items(days) { d ->
                val count = store.tasksList.count { t -> !t.archived && RepeatEngine.occurrences(t, d, d).isNotEmpty() }
                AssistChip(
                    onClick = { onOpenDate(d.toString()) },
                    label = { Text("${d.dayOfMonth} (${count})") },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Divider(Modifier.padding(vertical = 8.dp))
        Text(
            "Tip: Tap a day to view and complete its checklist.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
