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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------- DataStore setup ----------

private val Context.dataStore by preferencesDataStore(name = "routine_prefs")

private object PrefKeys {
    val completedItems: Preferences.Key<Set<String>> =
        stringSetPreferencesKey("completed_items")

    val seenOnboarding: Preferences.Key<Boolean> =
        booleanPreferencesKey("seen_onboarding")

    val morningHour: Preferences.Key<Int> =
        intPreferencesKey("morning_hour")
    val morningMinute: Preferences.Key<Int> =
        intPreferencesKey("morning_minute")
    val afternoonHour: Preferences.Key<Int> =
        intPreferencesKey("afternoon_hour")
    val afternoonMinute: Preferences.Key<Int> =
        intPreferencesKey("afternoon_minute")
    val eveningHour: Preferences.Key<Int> =
        intPreferencesKey("evening_hour")
    val eveningMinute: Preferences.Key<Int> =
        intPreferencesKey("evening_minute")
}

data class ReminderSettings(
    val morningHour: Int = 7,
    val morningMinute: Int = 0,
    val afternoonHour: Int = 12,
    val afternoonMinute: Int = 0,
    val eveningHour: Int = 19,
    val eveningMinute: Int = 0
)

data class RoutinePrefs(
    val completedItems: Set<String> = emptySet(),
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val seenOnboarding: Boolean = false
)

fun routinePrefsFlow(context: Context): Flow<RoutinePrefs> =
    context.dataStore.data.map { prefs ->
        val completed = prefs[PrefKeys.completedItems] ?: emptySet()
        val seen = prefs[PrefKeys.seenOnboarding] ?: false

        val reminders = ReminderSettings(
            morningHour = prefs[PrefKeys.morningHour] ?: 7,
            morningMinute = prefs[PrefKeys.morningMinute] ?: 0,
            afternoonHour = prefs[PrefKeys.afternoonHour] ?: 12,
            afternoonMinute = prefs[PrefKeys.afternoonMinute] ?: 0,
            eveningHour = prefs[PrefKeys.eveningHour] ?: 19,
            eveningMinute = prefs[PrefKeys.eveningMinute] ?: 0
        )

        RoutinePrefs(
            completedItems = completed,
            reminderSettings = reminders,
            seenOnboarding = seen
        )
    }

suspend fun setCompletedItems(context: Context, items: Set<String>) {
    context.dataStore.edit { prefs ->
        prefs[PrefKeys.completedItems] = items
    }
}

suspend fun clearCompletedItems(context: Context) {
    context.dataStore.edit { prefs ->
        prefs[PrefKeys.completedItems] = emptySet()
    }
}

suspend fun setSeenOnboarding(context: Context, seen: Boolean) {
    context.dataStore.edit { prefs ->
        prefs[PrefKeys.seenOnboarding] = seen
    }
}

suspend fun setReminderSettings(context: Context, settings: ReminderSettings) {
    context.dataStore.edit { prefs ->
        prefs[PrefKeys.morningHour] = settings.morningHour
        prefs[PrefKeys.morningMinute] = settings.morningMinute
        prefs[PrefKeys.afternoonHour] = settings.afternoonHour
        prefs[PrefKeys.afternoonMinute] = settings.afternoonMinute
        prefs[PrefKeys.eveningHour] = settings.eveningHour
        prefs[PrefKeys.eveningMinute] = settings.eveningMinute
    }
}

// ---------- Alarm scheduling ----------

private const val MORNING_REQ = 101
private const val AFTERNOON_REQ = 102
private const val EVENING_REQ = 103

fun scheduleAllReminders(context: Context, settings: ReminderSettings) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleOne(hour: Int, minute: Int, requestCode: Int, title: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }

    scheduleOne(settings.morningHour, settings.morningMinute, MORNING_REQ, "Morning routine")
    scheduleOne(settings.afternoonHour, settings.afternoonMinute, AFTERNOON_REQ, "Afternoon routine")
    scheduleOne(settings.eveningHour, settings.eveningMinute, EVENING_REQ, "Evening routine")
}

// ---------- Routine model ----------

data class RoutineSection(
    val title: String,
    val items: List<String>
)

fun buildRoutineSections(): List<RoutineSection> =
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

// ---------- App screens ----------

enum class AppScreen {
    Onboarding,
    Home,
    Settings
}

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
            MyDailyRoutineApp()
        }
    }
}

@Composable
fun MyDailyRoutineApp() {
    val context = LocalContext.current
    val prefsFlow = remember { routinePrefsFlow(context) }
    val prefs by prefsFlow.collectAsState(initial = RoutinePrefs())
    val sections = remember { buildRoutineSections() }
    val allItems = remember { sections.flatMap { it.items } }

    var currentScreen by remember {
        mutableStateOf(
            if (prefs.seenOnboarding) AppScreen.Home else AppScreen.Onboarding
        )
    }

    val coroutineScope = rememberCoroutineScope()

    // Switch away from onboarding once flag is set
    LaunchedEffect(prefs.seenOnboarding) {
        if (prefs.seenOnboarding) {
            currentScreen = AppScreen.Home
        }
    }

    // Whenever reminder settings change, update alarms
    LaunchedEffect(prefs.reminderSettings) {
        scheduleAllReminders(context, prefs.reminderSettings)
    }

    MaterialTheme {
        when (currentScreen) {
            AppScreen.Onboarding -> OnboardingScreen(
                onContinue = {
                    coroutineScope.launch {
                        setSeenOnboarding(context, true)
                    }
                }
            )

            AppScreen.Home -> HomeScreen(
                sections = sections,
                prefs = prefs,
                onToggleItem = { label, isChecked ->
                    coroutineScope.launch {
                        val updated = prefs.completedItems.toMutableSet()
                        if (isChecked) updated.add(label) else updated.remove(label)
                        setCompletedItems(context, updated)
                    }
                },
                onClear = {
                    coroutineScope.launch {
                        clearCompletedItems(context)
                    }
                },
                onOpenSettings = { currentScreen = AppScreen.Settings },
                onShareProgress = {
                    val text = buildShareText(sections, prefs.completedItems)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Share routine")
                    )
                }
            )

            AppScreen.Settings -> SettingsScreen(
                reminderSettings = prefs.reminderSettings,
                onSave = { newSettings ->
                    coroutineScope.launch {
                        setReminderSettings(context, newSettings)
                    }
                    currentScreen = AppScreen.Home
                },
                onBack = { currentScreen = AppScreen.Home }
            )
        }
    }
}

// ---------- Onboarding ----------

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "My Daily Routine",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This app helps you track sleep, water, medicines, exercise, meals, and home tasks – with gentle reminders throughout the day.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "• Check items as you go\n• See your progress for today\n• Get 3 reminders (morning, noon, evening)\n• Adjust reminder times in Settings",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get started")
            }
        }
    }
}

// ---------- Home screen ----------

@Composable
fun HomeScreen(
    sections: List<RoutineSection>,
    prefs: RoutinePrefs,
    onToggleItem: (String, Boolean) -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
    onShareProgress: () -> Unit
) {
    val completedItems = prefs.completedItems
    val totalCount = sections.flatMap { it.items }.size
    val completedCount = completedItems.size
    val progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

    val dateFormatter = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    }
    val todayText = remember { dateFormatter.format(Date()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Daily Routine") },
                actions = {
                    TextButton(onClick = onShareProgress) {
                        Text("Share")
                    }
                    TextButton(onClick = onOpenSettings) {
                        Text("Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header card
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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sections) { section ->
                    RoutineSectionCard(
                        section = section,
                        completedItems = completedItems,
                        onToggleItem = onToggleItem
                    )
                }

                item {
                    ReminderSummaryCard(prefs.reminderSettings)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear progress for today")
            }
        }
    }
}

@Composable
fun RoutineSectionCard(
    section: RoutineSection,
    completedItems: Set<String>,
    onToggleItem: (String, Boolean) -> Unit
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
                val checked = completedItems.contains(label)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            onToggleItem(label, isChecked)
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
fun ReminderSummaryCard(settings: ReminderSettings) {
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

            Text("• Morning at %02d:%02d".format(settings.morningHour, settings.morningMinute))
            Text("• Afternoon at %02d:%02d".format(settings.afternoonHour, settings.afternoonMinute))
            Text("• Evening at %02d:%02d".format(settings.eveningHour, settings.eveningMinute))

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "You will get a notification at these times every day.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ---------- Settings screen ----------

@Composable
fun SettingsScreen(
    reminderSettings: ReminderSettings,
    onSave: (ReminderSettings) -> Unit,
    onBack: () -> Unit
) {
    var morningHour by remember { mutableStateOf(reminderSettings.morningHour.toString()) }
    var morningMinute by remember { mutableStateOf(reminderSettings.morningMinute.toString()) }
    var afternoonHour by remember { mutableStateOf(reminderSettings.afternoonHour.toString()) }
    var afternoonMinute by remember { mutableStateOf(reminderSettings.afternoonMinute.toString()) }
    var eveningHour by remember { mutableStateOf(reminderSettings.eveningHour.toString()) }
    var eveningMinute by remember { mutableStateOf(reminderSettings.eveningMinute.toString()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reminder Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set reminder times (24-hour clock):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                ReminderTimeRow(
                    label = "Morning",
                    hour = morningHour,
                    minute = morningMinute,
                    onHourChange = { morningHour = it },
                    onMinuteChange = { morningMinute = it }
                )

                ReminderTimeRow(
                    label = "Afternoon",
                    hour = afternoonHour,
                    minute = afternoonMinute,
                    onHourChange = { afternoonHour = it },
                    onMinuteChange = { afternoonMinute = it }
                )

                ReminderTimeRow(
                    label = "Evening",
                    hour = eveningHour,
                    minute = eveningMinute,
                    onHourChange = { eveningHour = it },
                    onMinuteChange = { eveningMinute = it }
                )
            }

            Button(
                onClick = {
                    val mh = morningHour.toIntOrNull() ?: reminderSettings.morningHour
                    val mm = morningMinute.toIntOrNull() ?: reminderSettings.morningMinute
                    val ah = afternoonHour.toIntOrNull() ?: reminderSettings.afternoonHour
                    val am = afternoonMinute.toIntOrNull() ?: reminderSettings.afternoonMinute
                    val eh = eveningHour.toIntOrNull() ?: reminderSettings.eveningHour
                    val em = eveningMinute.toIntOrNull() ?: reminderSettings.eveningMinute

                    val clamped = ReminderSettings(
                        morningHour = mh.coerceIn(0, 23),
                        morningMinute = mm.coerceIn(0, 59),
                        afternoonHour = ah.coerceIn(0, 23),
                        afternoonMinute = am.coerceIn(0, 59),
                        eveningHour = eh.coerceIn(0, 23),
                        eveningMinute = em.coerceIn(0, 59)
                    )

                    onSave(clamped)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save reminder times")
            }
        }
    }
}

@Composable
fun ReminderTimeRow(
    label: String,
    hour: String,
    minute: String,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = hour,
                onValueChange = onHourChange,
                label = { Text("Hour (0–23)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = minute,
                onValueChange = onMinuteChange,
                label = { Text("Min (0–59)") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------- Share helper ----------

fun buildShareText(
    sections: List<RoutineSection>,
    completedItems: Set<String>
): String {
    val dateText = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    val builder = StringBuilder()
    builder.append("My daily routine for $dateText:\n\n")

    sections.forEach { section ->
        builder.append(section.title).append(":\n")
        section.items.forEach { item ->
            val mark = if (completedItems.contains(item)) "✓" else "✗"
            builder.append("  [$mark] ").append(item).append('\n')
        }
        builder.append('\n')
    }
    return builder.toString().trim()
}
