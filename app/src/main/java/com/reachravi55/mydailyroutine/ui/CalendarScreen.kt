package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.DateUtils
import com.reachravi55.mydailyroutine.data.RepeatEngine
import com.reachravi55.mydailyroutine.proto.RoutineStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    store: RoutineStore,
    contentPadding: PaddingValues,
    onOpenDay: (LocalDate) -> Unit,
    onNewTaskForDate: (dateKey: String) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val dateKey = DateUtils.formatKey(selected)

    val countForDay = remember(store, selected) {
        store.tasksList.count { !it.archived && RepeatEngine.occursOn(it, selected) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calendar") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNewTaskForDate(dateKey) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New") }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(inner)
                .fillMaxSize()
        ) {
            SectionHeader("${selected.month.name.lowercase().replaceFirstChar { it.titlecase() }} ${selected.year}")

            WeekStrip(
                selected = selected,
                onSelected = { selected = it }
            )

            Spacer(Modifier.height(8.dp))

            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Selected: $dateKey", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("$countForDay task(s) on this date", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onOpenDay(selected) }) { Text("View day") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(selected: LocalDate, onSelected: (LocalDate) -> Unit) {
    val startOfWeek = selected.with(DayOfWeek.MONDAY)
    val days = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(days, key = { it.toString() }) { d ->
            val isSel = d == selected
            val label = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            Card(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                onClick = { onSelected(d) }
            ) {
                Column(
                    Modifier.padding(12.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
