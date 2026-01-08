@file:OptIn(ExperimentalMaterial3Api::class)

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

// ---------- DataStore ----------
private val Context.dataStore by preferencesDataStore(name = "routine_prefs")

private object PrefKeys {
    val completedItems: Preferences.Key<Set<String>> = stringSetPreferencesKey("completed_items")
    val seenOnboarding: Preferences.Key<Boolean> = booleanPreferencesKey("seen_onboarding")

    val morningHour: Preferences.Key<Int> = intPreferencesKey("morning_hour")
    val morningMinute: Preferences.Key<Int> = intPreferencesKey("morning_minute")
    val afternoonHour: Preferences.Key<Int> = intPreferencesKey("afternoon_hour")
    val afternoonMinute: Preferences.Key<Int> = intPreferencesKey("afternoon_minute")
    val eveningHour: Preferences.Key<Int> = intPreferencesKey("evening_hour")
    val eveningMinute: Preferences.Key<Int> = intPreferencesKey("evening_minute")
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
        RoutinePrefs(
            completedItems = prefs[PrefKeys.completedItems] ?: emptySet(),
            reminderSettings = ReminderSettings(
                morningHour = prefs[PrefKeys.morningHour] ?: 7,
                morningMinute = prefs[PrefKeys.morningMinute] ?: 0,
                afternoonHour = prefs[PrefKeys.afternoonHour] ?: 12,
                afternoonMinute = prefs[PrefKeys.afternoonMinute] ?: 0,
                eveningHour = prefs[PrefKeys.eveningHour] ?: 19,
                eveningMinute = prefs[PrefKeys.eveningMinute] ?: 0
            ),
            seenOnboarding = prefs[PrefKeys.seenOnboarding] ?: false
        )
    }

suspend fun setCompletedItems(context: Context, items: Set<String>) {
    context.dataStore.edit { it[PrefKeys.completedItems] = items }
}

suspend fun clearCompletedItems(context: Context) {
    context.dataStore.edit { it[PrefKeys.completedItems] = emptySet() }
}

suspend fun setSeenOnboarding(context: Context, seen: Boolean) {
    context.dataStore.edit { it[PrefKeys.seenOnboarding] = seen }
}

suspend fun setReminderSettings(context: Context, s: ReminderSettings) {
    context.dataStore.edit {
        it[PrefKeys.morningHour] = s.morningHour
        it[PrefKeys.morningMinute] = s.morningMinute
        it[PrefKeys.afternoonHour] = s.afternoonHour
        it[PrefKeys.afternoonMinute] = s.afternoonMinute
        it[PrefKeys.eveningHour] = s.eveningHour
        it[PrefKeys.eveningMinute] = s.eveningMinute
    }
}

// ---------- Alarms ----------
private const val MORNING_REQ = 101
private const val AFTERNOON_REQ = 102
private const val EVENING_REQ = 103

fun scheduleAllReminders(context: Context, settings: ReminderSettings) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleOne(hour: Int, minute: Int, req: Int, title: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title)
        }

        val pending = PendingIntent.getBroadcast(
            context, req, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
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

// ---------- Routine ----------
data class RoutineSection(val title: String, val items: List<String>)

fun buildRoutineSections() = listOf(
    RoutineSection(
        "Sleep & Wake",
        listOf("Wake-up time logged", "Bedtime logged", "Sleep machine / app used", "Hours slept noted")
    ),
    RoutineSection(
        "Water Intake (3 Liters)",
        listOf("0.5 L", "1.0 L", "1.5 L", "2.0 L", "2.5 L", "3.0 L")
    ),
    RoutineSection(
        "Medicines",
        listOf("Morning tablet taken", "Night tablet taken")
    ),
    RoutineSection(
        "Exercise",
        listOf("Morning walk / gym", "Evening walk / gym", "Rest day noted")
    ),
    RoutineSection(
        "Meals",
        listOf("Lunch eaten (12:00 pm)", "Dinner eaten (7:00 pm)")
    ),
    RoutineSection(
        "Work & Home",
        listOf("Office work completed", "Cooking done", "House cleaning done")
    )
)

// ---------- Screens ----------
enum class AppScreen { Onboarding, Home, Settings }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        setContent { MyDailyRoutineApp() }
    }
}

@Composable
fun MyDailyRoutineApp() {
    val context = LocalContext.current
    val prefs by routinePrefsFlow(context).collectAsState(initial = RoutinePrefs())
    val sections = remember { buildRoutineSections() }
    var screen by remember { mutableStateOf(if (prefs.seenOnboarding) AppScreen.Home else AppScreen.Onboarding) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(prefs.seenOnboarding) {
        if (prefs.seenOnboarding) screen = AppScreen.Home
    }

    LaunchedEffect(prefs.reminderSettings) {
        scheduleAllReminders(context, prefs.reminderSettings)
    }

    MaterialTheme {
        when (screen) {
            AppScreen.Onboarding -> OnboardingScreen {
                scope.launch { setSeenOnboarding(context, true) }
            }

            AppScreen.Home -> HomeScreen(
                sections = sections,
                prefs = prefs,
                onToggleItem = { item, checked ->
                    scope.launch {
                        val updated = prefs.completedItems.toMutableSet()
                        if (checked) updated.add(item) else updated.remove(item)
                        setCompletedItems(context, updated)
                    }
                },
                onClear = { scope.launch { clearCompletedItems(context) } },
                onSettings = { screen = AppScreen.Settings },
                onShare = {
                    val text = buildShareText(sections, prefs.completedItems)
                    context.startActivity(Intent.createChooser(Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }, "Share routine"))
                }
            )

            AppScreen.Settings -> SettingsScreen(
                settings = prefs.reminderSettings,
                onSave = {
                    scope.launch { setReminderSettings(context, it) }
                    screen = AppScreen.Home
                },
                onBack = { screen = AppScreen.Home }
            )
        }
    }
}

// ---------- Onboarding ----------
@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Welcome") }) }) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("My Daily Routine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Track your day with reminders & progress.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text(
                    "• Simple checklist\n• Progress tracking\n• Custom reminders\n• Share your day",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
        }
    }
}

// ---------- Home ----------
@Composable
fun HomeScreen(
    sections: List<RoutineSection>,
    prefs: RoutinePrefs,
    onToggleItem: (String, Boolean) -> Unit,
    onClear: () -> Unit,
    onSettings: () -> Unit,
    onShare: () -> Unit
) {
    val completed = prefs.completedItems
    val total = sections.flatMap { it.items }.size
    val done = completed.size
    val progress = if (total == 0) 0f else done.toFloat() / total
    val date = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Daily Routine") },
                actions = {
                    TextButton(onClick = onShare) { Text("Share") }
                    TextButton(onClick = onSettings) { Text("Settings") }
                }
            )
        }
    ) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp)
        ) {
            Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("$done of $total tasks completed")
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sections) { section ->
                    SectionCard(section, completed, onToggleItem)
                }

                item { ReminderSummaryCard(prefs.reminderSettings) }
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("Clear progress for today") }
        }
    }
}

@Composable
fun SectionCard(section: RoutineSection, completed: Set<String>, toggle: (String, Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            section.items.forEach {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Checkbox(checked = completed.contains(it), onCheckedChange = { c -> toggle(it, c) })
                    Spacer(Modifier.width(8.dp))
                    Text(it)
                }
            }
        }
    }
}

@Composable
fun ReminderSummaryCard(s: ReminderSettings) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("• Morning: %02d:%02d".format(s.morningHour, s.morningMinute))
            Text("• Afternoon: %02d:%02d".format(s.afternoonHour, s.afternoonMinute))
            Text("• Evening: %02d:%02d".format(s.eveningHour, s.eveningMinute))
        }
    }
}

// ---------- Settings ----------
@Composable
fun SettingsScreen(settings: ReminderSettings, onSave: (ReminderSettings) -> Unit, onBack: () -> Unit) {
    var mh by remember { mutableStateOf(settings.morningHour.toString()) }
    var mm by remember { mutableStateOf(settings.morningMinute.toString()) }
    var ah by remember { mutableStateOf(settings.afternoonHour.toString()) }
    var am by remember { mutableStateOf(settings.afternoonMinute.toString()) }
    var eh by remember { mutableStateOf(settings.eveningHour.toString()) }
    var em by remember { mutableStateOf(settings.eveningMinute.toString()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reminder Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReminderTimeRow("Morning", mh, mm, { mh = it }, { mm = it })
                ReminderTimeRow("Afternoon", ah, am, { ah = it }, { am = it })
                ReminderTimeRow("Evening", eh, em, { eh = it }, { em = it })
            }

            Button(
                onClick = {
                    onSave(
                        ReminderSettings(
                            mh.toIntOrNull()?.coerceIn(0, 23) ?: settings.morningHour,
                            mm.toIntOrNull()?.coerceIn(0, 59) ?: settings.morningMinute,
                            ah.toIntOrNull()?.coerceIn(0, 23) ?: settings.afternoonHour,
                            am.toIntOrNull()?.coerceIn(0, 59) ?: settings.afternoonMinute,
                            eh.toIntOrNull()?.coerceIn(0, 23) ?: settings.eveningHour,
                            em.toIntOrNull()?.coerceIn(0, 59) ?: settings.eveningMinute
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save reminder times") }
        }
    }
}

@Composable
fun ReminderTimeRow(
    label: String,
    hour: String,
    min: String,
    onH: (String) -> Unit,
    onM: (String) -> Unit
) {
    Column {
        Text(label, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(hour, onH, label = { Text("Hour (0–23)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(min, onM, label = { Text("Min (0–59)") }, modifier = Modifier.weight(1f))
        }
    }
}

// ---------- Share ----------
fun buildShareText(sections: List<RoutineSection>, completed: Set<String>): String {
    val date = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    return buildString {
        append("My daily routine for $date:\n\n")
        sections.forEach {
            append(it.title).append(":\n")
            it.items.forEach { item ->
                append("  [").append(if (completed.contains(item)) "✓" else "✗").append("] ").append(item).append('\n')
            }
            append('\n')
        }
    }.trim()
}
