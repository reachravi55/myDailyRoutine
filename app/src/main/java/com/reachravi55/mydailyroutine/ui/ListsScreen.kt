package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.proto.RoutineStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    store: RoutineStore,
    contentPadding: PaddingValues,
    onOpenList: (listId: String) -> Unit
) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val scope = rememberCoroutineScope()

    var showNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    val lists = store.listsList.filterNot { it.archived }.sortedBy { it.sortOrder }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lists") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNew = true }) {
                Icon(Icons.Default.Add, contentDescription = "New list")
            }
        }
    ) { inner ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(inner)
                .fillMaxSize()
        ) {
            if (lists.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text("Create your first list", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Lists help you organize tasks (Work, Personal, Fitness).", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(lists, key = { it.id }) { l ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { onOpenList(l.id) },
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(l.name, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(2.dp))
                                    Text("Tap to view tasks", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("New list") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        scope.launch { repo.createList(name) }
                        newName = ""
                        showNew = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Cancel") } }
        )
    }
}
