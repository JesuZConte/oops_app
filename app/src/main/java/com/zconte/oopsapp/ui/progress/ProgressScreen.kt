package com.zconte.oopsapp.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.ui.theme.OopsTheme
import com.zconte.oopsapp.ui.theme.PressStart2P
import com.zconte.oopsapp.ui.theme.RouteChipBackgroundLight
import com.zconte.oopsapp.ui.theme.RouteHeaderBackground

private data class RouteLine(
    val label: String,
    val statusLine: String,
    val color: Color?,
    val locked: Boolean,
    val lockedHint: String? = null,
    val currentStepLabel: String? = null
)

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extended = OopsTheme.extendedColors
    val streamsReadiness = uiState.readinessByObjective["streams-lambdas"] ?: 0f
    val globalPercent = if (uiState.readinessByObjective.isNotEmpty()) {
        (uiState.readinessByObjective.values.average() * 100).toInt()
    } else 0

    val lines = listOf(
        RouteLine(
            label = "STREAMS · L1",
            statusLine = "${(streamsReadiness * 100).toInt()}% dominado",
            color = MaterialTheme.colorScheme.primary,
            locked = false,
            currentStepLabel = "collect() — ahora ▶"
        ),
        RouteLine(
            label = "COLLECTIONS · L2",
            statusLine = "Colecciones y Map/Set",
            color = null,
            locked = true,
            lockedHint = "Se abre al 60% de Streams"
        ),
        RouteLine(
            label = "SQL/JDBC · L3",
            statusLine = "JDBC y NIO.2",
            color = null,
            locked = true,
            lockedHint = "Proximamente"
        )
    )

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RouteHeaderBackground)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ruta 1Z0-830",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "$globalPercent%",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
                color = extended.success
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            items(lines) { line -> RouteLineRow(line) }
        }
    }
}

@Composable
private fun RouteLineRow(line: RouteLine) {
    val extended = OopsTheme.extendedColors
    val stationColor = line.color ?: extended.lockedBorder
    val labelColor = line.color ?: extended.lockedText

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (line.locked) extended.lockedBackground else stationColor)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = line.label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
                color = labelColor
            )
            Text(
                text = line.statusLine,
                style = MaterialTheme.typography.titleMedium,
                color = if (line.locked) extended.lockedText else MaterialTheme.colorScheme.onBackground
            )
            if (line.locked && line.lockedHint != null) {
                Text(
                    text = "🔒 ${line.lockedHint}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.lockedText
                )
            }
            if (!line.locked && line.currentStepLabel != null && line.color != null) {
                val chipShape = RoundedCornerShape(10.dp)
                Text(
                    text = line.currentStepLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (extended.isDark) line.color else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .background(
                            color = if (extended.isDark) line.color.copy(alpha = 0.14f) else RouteChipBackgroundLight,
                            shape = chipShape
                        )
                        .border(
                            width = if (extended.isDark) 1.dp else 2.dp,
                            color = line.color,
                            shape = chipShape
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}