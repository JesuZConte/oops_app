package com.zconte.oopsapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

private data class BottomBarDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomBarDestinations = listOf(
    BottomBarDestination(OopsDestinations.HOME, "Home", Icons.Filled.Home),
    BottomBarDestination(OopsDestinations.PROGRESS, "Ruta", Icons.Filled.Route),
    BottomBarDestination(OopsDestinations.SETTINGS, "Ajustes", Icons.Filled.Settings)
)

@Composable
fun OopsBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        bottomBarDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}