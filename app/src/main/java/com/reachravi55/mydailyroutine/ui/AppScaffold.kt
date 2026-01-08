package com.reachravi55.mydailyroutine.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reachravi55.mydailyroutine.ui.nav.AppNavGraph
import com.reachravi55.mydailyroutine.ui.nav.Routes

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val items = listOf(
        BottomItem(Routes.Today, "Today") { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
        BottomItem(Routes.Calendar, "Calendar") { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
        BottomItem(Routes.Lists, "Lists") { Icon(Icons.Filled.List, contentDescription = null) },
        BottomItem(Routes.Settings, "Settings") { Icon(Icons.Filled.Settings, contentDescription = null) }
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val selected = currentRoute?.startsWith(item.route) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            // IMPORTANT: do NOT restore nested stacks.
                            // Always bring each tab back to its root destination.
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        },
                        icon = item.icon,
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        AppNavGraph(
            navController = navController,
            modifier = Modifier
                .then(Modifier)
                .padding(padding)
        )
    }
}
