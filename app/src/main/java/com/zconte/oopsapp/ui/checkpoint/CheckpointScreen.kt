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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.ui.components.ExerciseAnswerCard
import com.zconte.oopsapp.ui.components.ExerciseAnswerState
import com.zconte.oopsapp.ui.theme.OopsTheme

@Composable
fun CheckpointScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckpointViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isComplete) {
        CheckpointResultView(result = uiState.result, onContinue = onFinished, modifier = modifier)
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
            selectedAnswer = uiState.selectedAnswer
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

@Composable
private fun CheckpointResultView(result: CheckpointResult?, onContinue: () -> Unit, modifier: Modifier = Modifier) {
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
