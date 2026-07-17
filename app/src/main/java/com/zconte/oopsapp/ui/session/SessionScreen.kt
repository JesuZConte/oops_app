package com.zconte.oopsapp.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.ui.components.CodeBlock
import com.zconte.oopsapp.ui.theme.OopsTheme

private const val MCQ_TYPE = "mcq"

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

    var answer by remember(exercise.id) { mutableStateOf("") }
    val mcqOptions = remember(exercise.id) {
        if (exercise.type == MCQ_TYPE) (exercise.distractors + exercise.answer).shuffled() else emptyList()
    }
    val currentIndex = (uiState.totalExercises - uiState.queue.size + 1).coerceAtLeast(1)
    val progressFraction = if (uiState.totalExercises > 0) currentIndex / uiState.totalExercises.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.weight(1f).height(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$currentIndex/${uiState.totalExercises}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = exercise.prompt,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        exercise.code?.let { code ->
            CodeBlock(code = code, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.weight(1f))

        if (!uiState.isAnswered) {
            if (exercise.type == MCQ_TYPE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mcqOptions.forEach { option ->
                        McqOptionButton(
                            text = option,
                            state = McqOptionState.NORMAL,
                            onClick = { viewModel.submitAnswer(option) }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.submitAnswer(answer) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("COMPROBAR", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            if (exercise.type == MCQ_TYPE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mcqOptions.forEach { option ->
                        val state = when {
                            option != uiState.selectedAnswer -> McqOptionState.NORMAL
                            uiState.isCorrect -> McqOptionState.CORRECT
                            else -> McqOptionState.INCORRECT
                        }
                        McqOptionButton(text = option, state = state, onClick = {})
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            FeedbackBanner(
                isCorrect = uiState.isCorrect,
                answer = exercise.answer,
                explanation = exercise.explanation
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.nextExercise() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SIGUIENTE", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private enum class McqOptionState { NORMAL, CORRECT, INCORRECT }

@Composable
private fun McqOptionButton(text: String, state: McqOptionState, onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
    val borderColor = when (state) {
        McqOptionState.NORMAL -> MaterialTheme.colorScheme.outline
        McqOptionState.CORRECT -> extended.success
        McqOptionState.INCORRECT -> MaterialTheme.colorScheme.error
    }
    val backgroundColor = when (state) {
        McqOptionState.NORMAL -> MaterialTheme.colorScheme.surface
        McqOptionState.CORRECT -> extended.success.copy(alpha = 0.15f)
        McqOptionState.INCORRECT -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = state == McqOptionState.NORMAL, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            if (state == McqOptionState.CORRECT) {
                Text("✓", color = extended.success, style = MaterialTheme.typography.titleMedium)
            }
            if (state == McqOptionState.INCORRECT) {
                Text("✗", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FeedbackBanner(isCorrect: Boolean, answer: String, explanation: String) {
    val extended = OopsTheme.extendedColors
    val color = if (isCorrect) extended.success else MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(2.dp, color, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (isCorrect) "¡Correcto! +10 XP" else "Incorrecto. Respuesta: $answer",
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}