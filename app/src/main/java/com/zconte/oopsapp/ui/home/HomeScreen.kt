package com.zconte.oopsapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.ui.components.FunctionalCup
import com.zconte.oopsapp.ui.components.LanguageEmblem
import com.zconte.oopsapp.ui.components.ThemedCard
import com.zconte.oopsapp.ui.theme.PressStart2P

@Composable
fun HomeScreen(
    onStudyClick: () -> Unit,
    onProgressClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val levelFraction = (uiState.xp % 100) / 100f

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Oops!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            LanguageEmblem()
        }

        ThemedCard(accentColor = MaterialTheme.colorScheme.tertiary) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FunctionalCup(xpLevelFraction = levelFraction, streakDays = uiState.streak)
                Column {
                    Text(
                        text = "STREAK",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.streak.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PressStart2P, fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "días seguidos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        ThemedCard(accentColor = MaterialTheme.colorScheme.primary) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = uiState.xp.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    progress = { levelFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStudyClick,
            enabled = uiState.isReady,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("ESTUDIAR HOY", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedButton(
            onClick = onProgressClick,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Ver ruta", style = MaterialTheme.typography.bodyMedium)
        }
    }
}