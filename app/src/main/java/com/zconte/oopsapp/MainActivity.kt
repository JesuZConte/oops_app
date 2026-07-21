package com.zconte.oopsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zconte.oopsapp.domain.model.ThemeMode
import com.zconte.oopsapp.domain.repository.SettingsRepository
import com.zconte.oopsapp.navigation.OopsBottomBar
import com.zconte.oopsapp.navigation.OopsDestinations
import com.zconte.oopsapp.navigation.OopsNavHost
import com.zconte.oopsapp.ui.theme.OopsappTheme
import com.zconte.oopsapp.ui.theme.resolveDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private val FULL_SCREEN_ROUTES = setOf(
    OopsDestinations.SESSION,
    OopsDestinations.UNIT_SESSION,
    OopsDestinations.CHECKPOINT
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = resolveDarkTheme(themeMode, isSystemInDarkTheme())

            OopsappTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute !in FULL_SCREEN_ROUTES) {
                            OopsBottomBar(navController, currentRoute)
                        }
                    }
                ) { innerPadding ->
                    OopsNavHost(
                        navController = navController,
                        modifier = Modifier.padding(
                            bottom = if (currentRoute in FULL_SCREEN_ROUTES) {
                                0.dp
                            } else {
                                innerPadding.calculateBottomPadding()
                            }
                        )
                    )
                }
            }
        }
    }
}