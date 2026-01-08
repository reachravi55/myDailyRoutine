package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.proto.RoutineStore
import java.time.LocalDate

private sealed class BottomDest(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object Today : BottomDest("today", "Today", { Icon(Icons.Default.Checklist, contentDescription = null) })
    data object Calendar : BottomDest("calendar", "Calendar", { Icon(Icons.Default.CalendarMonth, contentDescription = null) })
    data object Lists : BottomDest("lists", "Lists", { Icon(Icons.Default.ViewList, contentDescription = null) })
    data object Settings : BottomDest("settings", "Settings", { Icon(Icons.Default.Settings, contentDescription = null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(store: RoutineStore) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val nav = rememberNavController()

    val bottom = listOf(BottomDest.Today, BottomDest.Calendar, BottomDest.Lists, BottomDest.Settings)
    var currentRoute by remember { mutableStateOf(BottomDest.Today.route) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("My Daily Routine") },
                actions = {
                    if (currentRoute == BottomDest.Today.route || currentRoute == BottomDest.Calendar.route) {
                        TextButton(onClick = {
                            nav.navigate("taskEditor")
                        }) { Text("New") }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                bottom.forEach { d ->
                    NavigationBarItem(
                        selected = currentRoute == d.route,
                        onClick = {
                            currentRoute = d.route
                            nav.navigate(d.route) { launchSingleTop = true; popUpTo(nav.graph.startDestinationId) { saveState = true }; restoreState = true }
                        },
                        icon = d.icon,
                        label = { Text(d.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = BottomDest.Today.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomDest.Today.route) {
                currentRoute = BottomDest.Today.route
                TodayScreen(
                    store = store,
                    date = LocalDate.now(),
                    contentPadding = padding
                )
            }
            composable(BottomDest.Calendar.route) {
                currentRoute = BottomDest.Calendar.route
                CalendarScreen(
                    store = store,
                    contentPadding = padding,
                    onOpenDate = { date ->
                        nav.navigate("day/${date}")
                    }
                )
            }
            composable("day/{date}", arguments = listOf(navArgument("date") { type = NavType.StringType })) { backStack ->
                currentRoute = BottomDest.Calendar.route
                val dateKey = backStack.arguments?.getString("date") ?: LocalDate.now().toString()
                DayScreen(store = store, date = LocalDate.parse(dateKey), contentPadding = padding)
            }
            composable(BottomDest.Lists.route) {
                currentRoute = BottomDest.Lists.route
                ListsScreen(store = store, contentPadding = padding)
            }
            composable(BottomDest.Settings.route) {
                currentRoute = BottomDest.Settings.route
                SettingsScreen(store = store, contentPadding = padding)
            }
            composable("taskEditor?taskId={taskId}", arguments = listOf(
                navArgument("taskId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )) { backStack ->
                val taskId = backStack.arguments?.getString("taskId")
                TaskEditorScreen(store = store, taskId = taskId, onDone = { nav.popBackStack() })
            }
            composable("taskEditor") {
                TaskEditorScreen(store = store, taskId = null, onDone = { nav.popBackStack() })
            }
        }
    }
}
