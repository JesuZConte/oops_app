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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.zconte.oopsapp.domain.model.CheckpointStatus
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
    onOpenSummary: (String) -> Unit,
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
            itemsIndexed(uiState.sections) { index, sectionPath ->
                SectionPathBlock(
                    sectionPath = sectionPath,
                    previousSectionId = uiState.sections.getOrNull(index - 1)?.section?.id,
                    onPlayUnit = onPlayUnit,
                    onOpenCheckpoint = onOpenCheckpoint,
                    onOpenPlacementCheckpoint = onOpenPlacementCheckpoint,
                    onOpenSummary = onOpenSummary
                )
            }
        }
    }
}

@Composable
private fun SectionPathBlock(
    sectionPath: SectionPath,
    previousSectionId: String?,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    onOpenPlacementCheckpoint: (String) -> Unit,
    onOpenSummary: (String) -> Unit
) {
    val extended = OopsTheme.extendedColors
    // A unit can be locked for two different reasons: its own section is gated by a
    // pending mandatory checkpoint on the PREVIOUS section (nothing in this section is
    // playable yet), or the section is unlocked but an earlier unit in it isn't done
    // (this unit specifically can be skip-attempted via a placement checkpoint). Tapping
    // a locked unit must route to whichever gate actually blocks it -- previously every
    // locked tap opened the placement flow, which is meaningless (and doesn't even mark
    // the tapped unit complete) when the real blocker is the previous section's checkpoint.
    val gatedBySectionLock = !sectionPath.unlocked

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = sectionPath.section.name.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
            color = if (sectionPath.unlocked) MaterialTheme.colorScheme.primary else extended.lockedText
        )

        sectionPath.units.forEach { unitProgress ->
            UnitRow(
                unitProgress = unitProgress,
                gatedBySectionLock = gatedBySectionLock,
                onClick = {
                    when {
                        unitProgress.unlocked || unitProgress.completed -> onPlayUnit(unitProgress.unit.id)
                        gatedBySectionLock && previousSectionId != null -> onOpenCheckpoint(previousSectionId)
                        else -> onOpenPlacementCheckpoint(unitProgress.unit.id)
                    }
                },
                onOpenSummary = onOpenSummary
            )
        }

        if (sectionPath.completed && sectionPath.checkpointStatus != CheckpointStatus.SATISFIED) {
            CheckpointRow(
                status = sectionPath.checkpointStatus,
                onClick = { onOpenCheckpoint(sectionPath.section.id) }
            )
        }
    }
}

@Composable
private fun UnitRow(
    unitProgress: UnitProgress,
    gatedBySectionLock: Boolean,
    onClick: () -> Unit,
    onOpenSummary: (String) -> Unit
) {
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
        Column(modifier = Modifier.weight(1f)) {
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
                    gatedBySectionLock -> "🔒 Aprueba el checkpoint anterior primero"
                    else -> "🔒 Toca para intentar saltarla"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = extended.lockedText
            )
        }
        if (playable) {
            Text(
                text = "Ver resumen",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenSummary(unitProgress.unit.id) }
            )
        }
    }
}

@Composable
private fun CheckpointRow(status: CheckpointStatus, onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
    val isWarning = status == CheckpointStatus.RETRY_LOCKED
    val dotColor = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val subtitle = when (status) {
        CheckpointStatus.PENDING -> "Checkpoint obligatorio"
        CheckpointStatus.RETRY_LOCKED -> "Repasa lo fallado para reintentar"
        CheckpointStatus.RETRY_AVAILABLE -> "Reinténtalo ahora"
        CheckpointStatus.SATISFIED -> "" // unreachable: the call site hides this row when SATISFIED
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
                .background(dotColor)
        )
        Column {
            Text(
                text = "CHECKPOINT",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
                color = dotColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isWarning) MaterialTheme.colorScheme.error else extended.lockedText
            )
        }
    }
}
