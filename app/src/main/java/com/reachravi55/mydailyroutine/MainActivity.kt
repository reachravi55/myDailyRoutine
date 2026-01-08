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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

        // Schedule fixed reminders for your dad
        scheduleReminders()

        setContent {
            MyDailyRoutineApp()
        }
    }

    private fun scheduleReminders() {
        // Morning – 7:00 AM
        schedule(7, 0, 101, "Morning routine")

        // Afternoon – 12:00 PM
        schedule(12, 0, 102, "Afternoon routine")

        // Evening – 7:00 PM
        schedule(19, 0, 103, "Evening routine")
    }

    private fun schedule(h: Int, m: Int, code: Int, title: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("title", title)
        }

        val pending = PendingIntent.getBroadcast(
            this,
            code,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If that time today has passed, schedule from tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }
}

// ---------------- UI MODELS ----------------

data class RoutineSection(
    val title: String,
    val items: List<String>
)

// ---------------- COMPOSABLE UI ----------------

@Composable
fun MyDailyRoutineApp() {
    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("My Daily Routine") }
                )
            }
        ) { paddingValues ->
            RoutineScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
fun RoutineScreen(modifier: Modifier = Modifier) {
    // Define the full routine – this is what makes the app feel “complete”
    val sections = remember {
        listOf(
            RoutineSection(
                title = "Sleep & Wake",
                items = listOf(
                    "Wake-up time logged",
                    "Bedtime logged",
                    "Sleep machine / app used",
                    "Hours slept noted"
                )
            ),
            RoutineSection(
                title = "Water Intake (3 Liters)",
                items = listOf(
                    "0.5 L",
                    "1.0 L",
                    "1.5 L",
                    "2.0 L",
                    "2.5 L",
                    "3.0 L"
                )
            ),
            RoutineSection(
                title = "Medicines",
                items = listOf(
                    "Morning tablet taken",
                    "Night tablet taken"
                )
            ),
            RoutineSection(
                title = "Exercise",
                items = listOf(
                    "Morning walk / gym",
                    "Evening walk / gym",
                    "Rest day noted"
                )
            ),
            RoutineSection(
                title = "Meals",
                items = listOf(
                    "Lunch eaten (12:00 pm)",
                    "Dinner eaten (7:00 pm)"
                )
            ),
            RoutineSection(
                title = "Work & Home",
                items = listOf(
                    "Office work completed",
                    "Cooking done",
                    "House cleaning done"
                )
            )
        )
    }

    val allItems = remember(sections) { sections.flatMap { it.items } }

    // Track checkbox state for each item by label
    val checkStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allItems.forEach { put(it, false) }
        }
    }

    val completedCount = checkStates.values.count { it }
    val totalCount = allItems.size
    val progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

    val dateFormatter = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    }
    val todayText = remember { dateFormatter.format(Date()) }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        // Header: today + progress
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = todayText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$completedCount of $totalCount tasks completed",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable list of sections
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sections) { section ->
                RoutineSectionCard(
                    section = section,
                    checkStates = checkStates
                )
            }

            item {
                ReminderSummaryCard()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clear button that actually does something
        Button(
            onClick = {
                checkStates.keys.forEach { key ->
                    checkStates[key] = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Clear progress for today")
        }
    }
}

@Composable
fun RoutineSectionCard(
    section: RoutineSection,
    checkStates: MutableMap<String, Boolean>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            section.items.forEach { label ->
                val checked = checkStates[label] ?: false

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            checkStates[label] = isChecked
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("• Morning reminder at 7:00 am")
            Text("• Afternoon reminder at 12:00 pm")
            Text("• Evening reminder at 7:00 pm")

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "You will get a notification at these times every day.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
