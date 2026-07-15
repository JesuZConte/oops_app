package com.zconte.oopsapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zconte.oopsapp.ui.home.HomeScreen
import com.zconte.oopsapp.ui.progress.ProgressScreen
import com.zconte.oopsapp.ui.session.SessionScreen

@Composable
fun OopsNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = OopsDestinations.HOME) {
        composable(OopsDestinations.HOME) {
            HomeScreen(
                onStudyClick = { navController.navigate(OopsDestinations.SESSION) },
                onProgressClick = { navController.navigate(OopsDestinations.PROGRESS) }
            )
        }
        composable(OopsDestinations.SESSION) {
            SessionScreen(
                onSessionComplete = { navController.popBackStack() }
            )
        }
        composable(OopsDestinations.PROGRESS) {
            ProgressScreen()
        }
    }
}