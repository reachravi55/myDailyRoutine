import androidx.compose.runtime.saveable.rememberSaveable
package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reachravi55.mydailyroutine.viewmodel.EditorViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    navController: NavController,
    vm: EditorViewModel,
    dateArg: String?
) {
    val date = remember(dateArg) {
        (dateArg?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString())
    }

    val lists by vm.lists.collectAsState(initial = emptyList())

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedListId by rememberSaveable { mutableStateOf("") }
    var repeatMode by rememberSaveable { mutableStateOf("NONE") } // NONE/DAILY/WEEKLY/MONTHLY/YEARLY

    // Reminders: simple 3-slot for now (polish later)
    var reminder1 by rememberSaveable { mutableStateOf("") } // "HH:MM"
    var reminder2 by rememberSaveable { mutableStateOf("") }
    var reminder3 by rememberSaveable { mutableStateOf("") }

    val canSave = title.trim().isNotEmpty()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Task") }
            )
        },
        bottomBar = {
            // Always-visible Save / Cancel so user cannot get trapped.
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            if (!canSave) return@Button

                            vm.createTask(
                                title = title.trim(),
                                description = description.trim(),
                                listId = selectedListId,
                                scheduledDate = date,
                                repeatMode = repeatMode,
                                reminders = listOf(reminder1, reminder2, reminder3)
                                    .map { it.trim() }
                                    .filter { it.matches(Regex("""^\d{1,2}:\d{2}$""")) }
                            )
                            navController.popBackStack()
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                }
            }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("For: $date", style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                minLines = 2,
                maxLines = 4
            )

            // List selection (simple dropdown)
            Text("List", style = MaterialTheme.typography.titleSmall)
            ListDropdown(
                lists = lists.map { it.id to it.name },
                selectedId = selectedListId,
                onSelect = { selectedListId = it }
            )

            Text("Repeat", style = MaterialTheme.typography.titleSmall)
            RepeatDropdown(
                selected = repeatMode,
                onSelect = { repeatMode = it }
            )

            Text("Reminders (HH:MM)", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = reminder1,
                onValueChange = { reminder1 = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reminder 1") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = reminder2,
                onValueChange = { reminder2 = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reminder 2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = reminder3,
                onValueChange = { reminder3 = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reminder 3") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Tip: Enter reminders like 08:00 or 19:30. (We’ll add a proper time picker next.)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ListDropdown(
    lists: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = lists.firstOrNull { it.first == selectedId }?.second ?: "None"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select list") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelect("")
                    expanded = false
                }
            )
            lists.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RepeatDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Repeat") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
