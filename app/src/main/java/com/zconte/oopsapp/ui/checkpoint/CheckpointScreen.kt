package com.zconte.oopsapp.ui.checkpoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.usecase.CheckpointSectionBreakdown
import com.zconte.oopsapp.ui.components.ExerciseAnswerCard
import com.zconte.oopsapp.ui.components.ExerciseAnswerState
import com.zconte.oopsapp.ui.theme.OopsTheme
import kotlinx.coroutines.delay

@Composable
fun CheckpointScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckpointViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.showIntro, uiState.isComplete, uiState.isRetryLocked) {
        if (uiState.showIntro || uiState.isComplete || uiState.isRetryLocked) return@LaunchedEffect
        while (true) {
            delay(1000)
            viewModel.tick()
        }
    }

    if (uiState.isRetryLocked) {
        RetryLockedView(onBack = onFinished, modifier = modifier)
        return
    }

    if (uiState.isComplete) {
        CheckpointResultView(
            result = uiState.result,
            sectionBreakdown = uiState.sectionBreakdown,
            onContinue = onFinished,
            modifier = modifier
        )
        return
    }

    if (uiState.showIntro) {
        CheckpointIntroView(
            questionCount = uiState.totalExercises,
            timeBudgetSeconds = uiState.timeBudgetSeconds,
            onStart = viewModel::startCheckpoint,
            modifier = modifier
        )
        return
    }

    val exercise = uiState.currentExercise
    if (exercise == null) {
        Text(
            "Cargando checkpoint...",
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    ExerciseAnswerCard(
        state = ExerciseAnswerState(
            exercise = exercise,
            currentIndex = uiState.currentIndex,
            totalExercises = uiState.totalExercises,
            isAnswered = uiState.isAnswered,
            isCorrect = uiState.isCorrect,
            selectedAnswer = uiState.selectedAnswer,
            timeRemainingLabel = formatRemaining(uiState.timeRemainingSeconds)
        ),
        onSubmit = viewModel::submitAnswer,
        onNext = viewModel::nextExercise,
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp)
    )
}

private fun formatRemaining(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun RetryLockedView(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "Todavía no puedes reintentar",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Fallaste ejercicios que aún no has vuelto a responder. " +
                "En cuanto vuelvan a aparecerte en tu práctica diaria y los respondas, " +
                "podrás reintentar el checkpoint.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("VOLVER", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CheckpointIntroView(
    questionCount: Int,
    timeBudgetSeconds: Int,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = timeBudgetSeconds / 60
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "Checkpoint",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "$questionCount preguntas · $minutes minutos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("COMENZAR", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CheckpointResultView(
    result: CheckpointResult?,
    sectionBreakdown: List<CheckpointSectionBreakdown>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passed = result?.passed == true
    val extended = OopsTheme.extendedColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = if (passed) "¡Checkpoint superado!" else "Casi lo logras",
            style = MaterialTheme.typography.headlineSmall,
            color = if (passed) extended.success else MaterialTheme.colorScheme.error
        )
        Text(
            text = "${result?.scorePct ?: 0}% (necesitas 68% para aprobar)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!passed && sectionBreakdown.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sectionBreakdown.forEach { item ->
                    Text(
                        text = "${item.section.name}: ${item.correct}/${item.total} (${(item.correct * 100) / item.total}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("CONTINUAR", style = MaterialTheme.typography.titleMedium)
        }
    }
}
