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
import com.reachravi55.mydailyroutine.viewmodel.TodayViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    navController: NavController,
    vm: TodayViewModel
) {
    val today = remember { LocalDate.now().toString() }

    val lists by vm.lists.collectAsState(initial = emptyList())
    var selectedListId by rememberSaveable { mutableStateOf("") }

    val tasks by vm.todayTasks(selectedListId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Today") },
                actions = {
                    TextButton(onClick = {
                        navController.navigate("${Routes.TaskEditor}?date=$today")
                    }) { Text("New") }
                }
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(16.dp)
        ) {
            if (lists.isNotEmpty()) {
                FilterChipRow(
                    lists = lists.map { it.id to it.name },
                    selectedId = selectedListId,
                    onSelect = { selectedListId = it }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (tasks.isEmpty()) {
                Text("No tasks for today yet.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tasks) { t ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("${Routes.TaskDetail}/${t.id}?date=$today")
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

@Composable
private fun FilterChipRow(
    lists: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = { onSelect("") },
            label = { Text("All") }
        )
        lists.forEach { (id, name) ->
            FilterChip(
                selected = selectedId == id,
                onClick = { onSelect(id) },
                label = { Text(name) }
            )
        }
    }
}
