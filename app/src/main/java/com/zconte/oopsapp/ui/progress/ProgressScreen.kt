package com.zconte.oopsapp.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.model.UnitProgress
import com.zconte.oopsapp.ui.theme.OopsTheme
import com.zconte.oopsapp.ui.theme.PressStart2P
import com.zconte.oopsapp.ui.theme.RouteHeaderBackground

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    onOpenPlacementCheckpoint: (String) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
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

    val allUnits = uiState.sections.flatMap { it.units }
    val globalPercent = if (allUnits.isEmpty()) 0 else (allUnits.count { it.completed } * 100) / allUnits.size

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
                color = OopsTheme.extendedColors.success
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            items(uiState.sections) { sectionPath ->
                SectionPathBlock(
                    sectionPath = sectionPath,
                    onPlayUnit = onPlayUnit,
                    onOpenCheckpoint = onOpenCheckpoint,
                    onOpenPlacementCheckpoint = onOpenPlacementCheckpoint
                )
            }
        }
    }
}

@Composable
private fun SectionPathBlock(
    sectionPath: SectionPath,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    onOpenPlacementCheckpoint: (String) -> Unit
) {
    val extended = OopsTheme.extendedColors

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = sectionPath.section.name.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
            color = if (sectionPath.unlocked) MaterialTheme.colorScheme.primary else extended.lockedText
        )

        sectionPath.units.forEach { unitProgress ->
            UnitRow(
                unitProgress = unitProgress,
                onClick = {
                    if (unitProgress.unlocked || unitProgress.completed) {
                        onPlayUnit(unitProgress.unit.id)
                    } else {
                        onOpenPlacementCheckpoint(unitProgress.unit.id)
                    }
                }
            )
        }

        if (sectionPath.completed) {
            CheckpointRow(onClick = { onOpenCheckpoint(sectionPath.section.id) })
        }
    }
}

@Composable
private fun UnitRow(unitProgress: UnitProgress, onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
    val playable = unitProgress.unlocked || unitProgress.completed
    val dotColor = when {
        unitProgress.completed -> extended.success
        unitProgress.unlocked -> MaterialTheme.colorScheme.primary
        else -> extended.lockedBorder
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (playable) dotColor else extended.lockedBackground)
        )
        Column {
            Text(
                text = unitProgress.unit.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (playable) MaterialTheme.colorScheme.onBackground else extended.lockedText
            )
            Text(
                text = when {
                    unitProgress.completed && unitProgress.completedVia == UnitCompletionSource.PLACEMENT -> "Completada por checkpoint"
                    unitProgress.completed -> "Completada"
                    unitProgress.unlocked -> "Toca para jugar"
                    else -> "🔒 Toca para intentar saltarla"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = extended.lockedText
            )
        }
    }
}

@Composable
private fun CheckpointRow(onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
        )
        Column {
            Text(
                text = "CHECKPOINT",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = "Repaso opcional de esta seccion",
                style = MaterialTheme.typography.bodyMedium,
                color = extended.lockedText
            )
        }
    }
}
