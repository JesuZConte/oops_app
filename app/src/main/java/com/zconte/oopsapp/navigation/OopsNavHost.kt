package com.zconte.oopsapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zconte.oopsapp.ui.checkpoint.CheckpointScreen
import com.zconte.oopsapp.ui.checkpoint.PlacementCheckpointScreen
import com.zconte.oopsapp.ui.home.HomeScreen
import com.zconte.oopsapp.ui.progress.ProgressScreen
import com.zconte.oopsapp.ui.session.SessionScreen
import com.zconte.oopsapp.ui.settings.SettingsScreen

@Composable
fun OopsNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = OopsDestinations.HOME, modifier = modifier) {
        composable(OopsDestinations.HOME) {
            HomeScreen(
                onStudyClick = { navController.navigate(OopsDestinations.SESSION) },
                onProgressClick = {
                    navController.navigate(OopsDestinations.PROGRESS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(OopsDestinations.SESSION) {
            SessionScreen(
                onSessionComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = OopsDestinations.UNIT_SESSION,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType })
        ) {
            SessionScreen(
                onSessionComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = OopsDestinations.CHECKPOINT,
            arguments = listOf(navArgument("sectionId") { type = NavType.StringType })
        ) {
            CheckpointScreen(
                onFinished = { navController.popBackStack() }
            )
        }
        composable(
            route = OopsDestinations.PLACEMENT_CHECKPOINT,
            arguments = listOf(navArgument("targetUnitId") { type = NavType.StringType })
        ) {
            PlacementCheckpointScreen(
                onCancelled = { navController.popBackStack() },
                onFailed = { navController.popBackStack() },
                onUnlocked = { unitId ->
                    navController.navigate("unit_session/$unitId") {
                        popUpTo(OopsDestinations.PROGRESS)
                    }
                }
            )
        }
        composable(OopsDestinations.PROGRESS) {
            ProgressScreen(
                onPlayUnit = { unitId -> navController.navigate("unit_session/$unitId") },
                onOpenCheckpoint = { sectionId -> navController.navigate("checkpoint/$sectionId") },
                onOpenPlacementCheckpoint = { targetUnitId -> navController.navigate("placement_checkpoint/$targetUnitId") }
            )
        }
        composable(OopsDestinations.SETTINGS) {
            SettingsScreen()
        }
    }
}
