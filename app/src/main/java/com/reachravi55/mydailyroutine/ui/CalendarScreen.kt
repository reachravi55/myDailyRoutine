package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reachravi55.mydailyroutine.ui.nav.Routes
import com.reachravi55.mydailyroutine.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    vm: CalendarViewModel
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    // Selected date should NOT trap user — this is just UI state for this screen.
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selected = remember(selectedDate) { LocalDate.parse(selectedDate) }

    val tasksForDay by vm.tasksForDate(selected).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Calendar") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("${Routes.TaskEditor}?date=${selected}")
                }
            ) { Text("+") }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(16.dp)
        ) {
            // Simple date picker row (v2 polish later)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    selectedDate = selected.minusDays(1).toString()
                }) { Text("◀") }

                Text(
                    text = selected.format(formatter),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(onClick = {
                    selectedDate = selected.plusDays(1).toString()
                }) { Text("▶") }
            }

            Spacer(Modifier.height(12.dp))

            Text("Tasks", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (tasksForDay.isEmpty()) {
                Text("No tasks for this date.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasksForDay) { t ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("${Routes.TaskDetail}/${t.id}?date=${selected}")
                                }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(t.title, style = MaterialTheme.typography.titleMedium)
                                if (t.listName.isNotBlank()) {
                                    Text(t.listName, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
