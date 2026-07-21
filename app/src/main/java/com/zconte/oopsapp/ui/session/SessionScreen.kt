package com.zconte.oopsapp.ui.session

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.ui.components.ExerciseAnswerCard
import com.zconte.oopsapp.ui.components.ExerciseAnswerState

@Composable
fun SessionScreen(
    onSessionComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSessionComplete) {
        if (uiState.isSessionComplete) onSessionComplete()
    }

    val exercise = uiState.currentExercise
    if (exercise == null) {
        Text(
            "Cargando sesion...",
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    val currentIndex = (uiState.totalExercises - uiState.queue.size + 1).coerceAtLeast(1)

    ExerciseAnswerCard(
        state = ExerciseAnswerState(
            exercise = exercise,
            currentIndex = currentIndex,
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
