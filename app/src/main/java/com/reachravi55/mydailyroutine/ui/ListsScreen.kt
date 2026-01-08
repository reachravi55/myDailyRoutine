package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.data.newId
import com.reachravi55.mydailyroutine.proto.RoutineList
import com.reachravi55.mydailyroutine.proto.RoutineStore
import kotlinx.coroutines.launch

@Composable
fun ListsScreen(store: RoutineStore, contentPadding: PaddingValues) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Lists", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { showAdd = true }) { Text("Add") }
        }

        if (store.listsCount == 0) {
            EmptyStateCard(
                title = "No lists yet",
                subtitle = "Create lists like Work, Personal, Fitness — then add tasks to them."
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(store.listsList.sortedBy { it.sortOrder }, key = { it.id }) { l ->
                    ListCard(
                        title = l.name,
                        subtitle = "Tap to set as active"
                    ) {
                        scope.launch { repo.setActiveList(l.id) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New list") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val list = RoutineList.newBuilder()
                        .setId(newId())
                        .setName(name.trim().ifBlank { "Untitled" })
                        .setIcon("list")
                        .setColor(0)
                        .setSortOrder(store.listsCount)
                        .build()
                    scope.launch {
                        repo.upsertList(list)
                        repo.setActiveList(list.id)
                    }
                    name = ""
                    showAdd = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
