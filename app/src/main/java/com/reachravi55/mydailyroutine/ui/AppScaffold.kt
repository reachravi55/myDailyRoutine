package com.reachravi55.mydailyroutine.ui

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
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.reachravi55.mydailyroutine.data.DateUtils
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

    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomDest.Today.route

    Scaffold(
        modifier = Modifier,
        bottomBar = {
            NavigationBar {
                bottom.forEach { dest ->
                    val selected = currentRoute.startsWith(dest.route)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(dest.route) {
                                // IMPORTANT: Do NOT restore nested backstack state when switching tabs.
                                // Users expect each bottom tab to return to its "root" screen.
                                // Avoid findStartDestination()/saveState to keep compatibility with
                                // older navigation-compose versions used by the template.
                                popUpTo(nav.graph.startDestinationId) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        icon = dest.icon,
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = BottomDest.Today.route,
            modifier = Modifier
        ) {
            composable(BottomDest.Today.route) {
                TodayScreen(store = store, contentPadding = padding, onOpenDay = { date ->
                    nav.navigate("day/${DateUtils.formatKey(date)}")
                }, onNewTask = { listId, dateKey ->
                    nav.navigate("taskEditor?taskId=&listId=$listId&date=$dateKey")
                })
            }
            composable(BottomDest.Calendar.route) {
                CalendarScreen(
                    store = store,
                    contentPadding = padding,
                    onOpenDay = { date ->
                        nav.navigate("day/${DateUtils.formatKey(date)}")
                    },
                    onNewTaskForDate = { dateKey ->
                        val listId = store.activeListId
                        nav.navigate("taskEditor?taskId=&listId=$listId&date=$dateKey")
                    }
                )
            }
            composable(BottomDest.Lists.route) {
                ListsScreen(
                    store = store,
                    contentPadding = padding,
                    onOpenList = { listId ->
                        nav.navigate("list/$listId")
                    }
                )
            }
            composable(BottomDest.Settings.route) {
                SettingsScreen(store = store, contentPadding = padding)
            }

            composable(
                route = "list/{listId}",
                arguments = listOf(navArgument("listId") { type = NavType.StringType })
            ) { backStack ->
                val listId = backStack.arguments?.getString("listId") ?: return@composable
                ListDetailScreen(
                    store = store,
                    listId = listId,
                    onBack = { nav.popBackStack() },
                    onNewTask = { dateKey ->
                        nav.navigate("taskEditor?taskId=&listId=$listId&date=$dateKey")
                    },
                    onEditTask = { taskId ->
                        nav.navigate("taskEditor?taskId=$taskId&listId=$listId&date=")
                    }
                )
            }

            composable(
                route = "day/{dateKey}",
                arguments = listOf(navArgument("dateKey") { type = NavType.StringType })
            ) { backStack ->
                val dateKey = backStack.arguments?.getString("dateKey") ?: DateUtils.todayKey()
                DayScreen(
                    store = store,
                    date = DateUtils.parseKey(dateKey),
                    contentPadding = padding,
                    onBack = { nav.popBackStack() },
                    onNewTask = { listId ->
                        nav.navigate("taskEditor?taskId=&listId=$listId&date=$dateKey")
                    }
                )
            }

            composable(
                route = "taskEditor?taskId={taskId}&listId={listId}&date={date}",
                arguments = listOf(
                    navArgument("taskId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("listId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStack ->
                val taskId = backStack.arguments?.getString("taskId").orEmpty().ifBlank { null }
                val listId = backStack.arguments?.getString("listId").orEmpty()
                val dateKey = backStack.arguments?.getString("date").orEmpty().ifBlank { null }

                TaskEditorScreen(
                    store = store,
                    initialTaskId = taskId,
                    initialListId = listId,
                    initialDateKey = dateKey,
                    onDone = { nav.popBackStack() }
                )
            }
        }
    }
}
