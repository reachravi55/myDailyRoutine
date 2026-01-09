package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.DateUtils
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.proto.RoutineStore
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    store: RoutineStore,
    listId: String,
    onBack: () -> Unit,
    onNewTask: (dateKey: String) -> Unit,
    onEditTask: (taskId: String) -> Unit
) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val scope = rememberCoroutineScope()

    val list = store.listsList.firstOrNull { it.id == listId }
    val title = list?.name ?: "List"

    val tasks = store.tasksList.filter { !it.archived && it.listId == listId }.sortedBy { it.sortOrder }

    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(title) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { renameOpen = true }) { Icon(Icons.Default.Edit, contentDescription = "Rename") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNewTask(DateUtils.todayKey()) }) {
                Icon(Icons.Default.Add, contentDescription = "New task")
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (tasks.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text("No tasks yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create your first task in this list.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(tasks, key = { it.id }) { t ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { onEditTask(t.id) },
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(t.title, style = MaterialTheme.typography.titleMedium)
                                if (t.description.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(t.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Starts: ${t.startDate.ifBlank { DateUtils.todayKey() }} • Repeat: ${t.repeatRule.frequency.name.lowercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text("Rename list") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = renameText.trim()
                    if (name.isNotEmpty()) {
                        scope.launch { repo.renameList(listId, name) }
                        renameOpen = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameOpen = false }) { Text("Cancel") } }
        )
    }
}
