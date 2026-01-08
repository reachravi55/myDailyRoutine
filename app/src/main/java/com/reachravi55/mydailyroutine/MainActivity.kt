package com.reachravi55.mydailyroutine

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------- Data models ----------

data class Task(
    val id: Long,
    val title: String,
    val isDone: Boolean
)

enum class RepeatInterval(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class Reminder(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val repeat: RepeatInterval
)

// ---------- Activity ----------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DailyRoutineApp()
                }
            }
        }
    }
}

// ---------- Composable root ----------

@Composable
fun DailyRoutineApp() {
    val context = LocalContext.current

    // Load saved data (and reset checkmarks if day changed)
    var tasks by remember { mutableStateOf(loadTasks(context)) }
    var reminders by remember { mutableStateOf(loadReminders(context)) }

    // Whenever reminders change, reschedule all alarms + persist
    LaunchedEffect(reminders) {
        saveReminders(context, reminders)
        scheduleAllReminders(context, reminders)
    }

    DailyRoutineScreen(
        tasks = tasks,
        reminders = reminders,
        onTasksChange = { newTasks ->
            tasks = newTasks
            saveTasks(context, newTasks)
        },
        onRemindersChange = { newReminders ->
            reminders = newReminders
        }
    )
}

// ---------- UI ----------

@Composable
fun DailyRoutineScreen(
    tasks: List<Task>,
    reminders: List<Reminder>,
    onTasksChange: (List<Task>) -> Unit,
    onRemindersChange: (List<Reminder>) -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }

    // For adding a reminder
    var newHour by remember { mutableStateOf("") }
    var newMinute by remember { mutableStateOf("") }
    var repeatExpanded by remember { mutableStateOf(false) }
    var selectedRepeat by remember { mutableStateOf(RepeatInterval.DAILY) }
    var reminderError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ----- Tasks section -----
        Text(
            text = "My Daily Tasks",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(8.dp))

        // Add new task row (like Google Tasks / Keep)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                label = { Text("Add a task") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newTaskTitle.isNotBlank()) {
                        val nextId = (tasks.maxOfOrNull { it.id } ?: 0L) + 1L
                        val updated = tasks + Task(nextId, newTaskTitle.trim(), false)
                        onTasksChange(updated)
                        newTaskTitle = ""
                    }
                }
            ) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Task list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { checked ->
                        val updated = tasks.map {
                            if (it.id == task.id) it.copy(isDone = checked) else it
                        }
                        onTasksChange(updated)
                    },
                    onDelete = {
                        val updated = tasks.filterNot { it.id == task.id }
                        onTasksChange(updated)
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                // Reset progress for today (keep tasks)
                val reset = tasks.map { it.copy(isDone = false) }
                onTasksChange(reset)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear progress for today")
        }

        Spacer(Modifier.height(18.dp))

        // ----- Reminders section -----
        Text(
            text = "Reminders",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(8.dp))

        // Existing reminders list
        if (reminders.isEmpty()) {
            Text("No reminders set yet.")
        } else {
            reminders.forEach { reminder ->
                ReminderRow(
                    reminder = reminder,
                    onDelete = {
                        val updated = reminders.filterNot { it.id == reminder.id }
                        onRemindersChange(updated)
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Add reminder controls
        Text(text = "Add reminder", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newHour,
                onValueChange = { newHour = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("Hour (0-23)") },
                modifier = Modifier.width(120.dp)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = newMinute,
                onValueChange = { newMinute = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("Min (0-59)") },
                modifier = Modifier.width(120.dp)
            )
            Spacer(Modifier.width(8.dp))

            Box {
                OutlinedButton(onClick = { repeatExpanded = true }) {
                    Text(selectedRepeat.label)
                }
                DropdownMenu(
                    expanded = repeatExpanded,
                    onDismissRequest = { repeatExpanded = false }
                ) {
                    RepeatInterval.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                selectedRepeat = option
                                repeatExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        reminderError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(4.dp))
        }

        Button(
            onClick = {
                val h = newHour.toIntOrNull()
                val m = newMinute.toIntOrNull()
                if (h == null || m == null || h !in 0..23 || m !in 0..59) {
                    reminderError = "Please enter a valid time."
                } else {
                    reminderError = null
                    val nextId = (reminders.maxOfOrNull { it.id } ?: 0) + 1
                    val newReminder = Reminder(
                        id = nextId,
                        hour = h,
                        minute = m,
                        repeat = selectedRepeat
                    )
                    val updated = reminders + newReminder
                    onRemindersChange(updated)
                    newHour = ""
                    newMinute = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add reminder")
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = onToggle
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}

@Composable
fun ReminderRow(
    reminder: Reminder,
    onDelete: () -> Unit
) {
    val timeText = String.format("%02d:%02d", reminder.hour, reminder.minute)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$timeText · ${reminder.repeat.label}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}

// ---------- Persistence (SharedPreferences) ----------

private const val PREFS_NAME = "daily_routine_prefs"
private const val KEY_TASKS = "tasks"
private const val KEY_LAST_RESET_DATE = "last_reset_date"
private const val KEY_REMINDERS = "reminders"

private fun todayString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

private fun loadTasks(context: Context): List<Task> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val storedDate = prefs.getString(KEY_LAST_RESET_DATE, null)
    val today = todayString()
    val raw = prefs.getString(KEY_TASKS, null)

    var tasks = parseTasks(raw)

    // If date changed, clear checkmarks but keep tasks
    if (storedDate != today) {
        tasks = tasks.map { it.copy(isDone = false) }
        prefs.edit()
            .putString(KEY_LAST_RESET_DATE, today)
            .putString(KEY_TASKS, serializeTasks(tasks))
            .apply()
    }

    // If first run and no tasks, start empty list (or add sample)
    return tasks
}

private fun saveTasks(context: Context, tasks: List<Task>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(KEY_TASKS, serializeTasks(tasks))
        .apply()
}

private fun parseTasks(raw: String?): List<Task> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("::")
            if (parts.size != 3) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1]
            val done = parts[2] == "1"
            Task(id, title, done)
        }
}

private fun serializeTasks(tasks: List<Task>): String {
    return tasks.joinToString("\n") { t ->
        val doneFlag = if (t.isDone) "1" else "0"
        "${t.id}::${t.title}::${doneFlag}"
    }
}

private fun loadReminders(context: Context): List<Reminder> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
    return raw.lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("::")
            if (parts.size != 4) return@mapNotNull null
            val id = parts[0].toIntOrNull() ?: return@mapNotNull null
            val hour = parts[1].toIntOrNull() ?: return@mapNotNull null
            val minute = parts[2].toIntOrNull() ?: return@mapNotNull null
            val repeat = parts[3]
            val interval = RepeatInterval.values().find { it.name == repeat } ?: RepeatInterval.DAILY
            Reminder(id, hour, minute, interval)
        }
}

private fun saveReminders(context: Context, reminders: List<Reminder>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = reminders.joinToString("\n") { r ->
        "${r.id}::${r.hour}::${r.minute}::${r.repeat.name}"
    }
    prefs.edit().putString(KEY_REMINDERS, raw).apply()
}

// ---------- Alarm scheduling ----------

private fun scheduleAllReminders(context: Context, reminders: List<Reminder>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Cancel a range of possible old alarms
    for (id in 1..500) {
        val cancelIntent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            id,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
        }
    }

    // Schedule current reminders
    reminders.forEach { reminder ->
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", "Check your daily tasks")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intervalMillis = when (reminder.repeat) {
            RepeatInterval.DAILY -> AlarmManager.INTERVAL_DAY
            RepeatInterval.WEEKLY -> AlarmManager.INTERVAL_DAY * 7
            RepeatInterval.MONTHLY -> AlarmManager.INTERVAL_DAY * 30
            RepeatInterval.YEARLY -> AlarmManager.INTERVAL_DAY * 365
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            intervalMillis,
            pendingIntent
        )
    }
}
