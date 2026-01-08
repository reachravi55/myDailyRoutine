package com.example.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mydailyroutine.model.Task

@Composable
fun TaskListScreen(tasks: List<Task>, onOpen: (Task) -> Unit, onCreate: () -> Unit) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(tasks) { task ->
                ListItem(
                    headlineText = { Text(task.title) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    modifier = Modifier.clickable { onOpen(task) }
                )
            }
        }
    }
}