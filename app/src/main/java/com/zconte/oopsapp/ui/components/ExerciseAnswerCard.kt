package com.zconte.oopsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.ui.theme.OopsTheme

private const val MCQ_TYPE = "mcq"

data class ExerciseAnswerState(
    val exercise: ExerciseContent,
    val currentIndex: Int,
    val totalExercises: Int,
    val isAnswered: Boolean,
    val isCorrect: Boolean,
    val selectedAnswer: String?
)

@Composable
fun ExerciseAnswerCard(
    state: ExerciseAnswerState,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exercise = state.exercise
    var answer by remember(exercise.id) { mutableStateOf("") }
    var selectedOption by remember(exercise.id) { mutableStateOf<String?>(null) }
    val mcqOptions = remember(exercise.id) {
        if (exercise.type == MCQ_TYPE) (exercise.distractors + exercise.answer).shuffled() else emptyList()
    }
    val progressFraction = if (state.totalExercises > 0) state.currentIndex / state.totalExercises.toFloat() else 0f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.weight(1f).height(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${state.currentIndex}/${state.totalExercises}",
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

        if (!state.isAnswered) {
            if (exercise.type == MCQ_TYPE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mcqOptions.forEach { option ->
                        McqOptionButton(
                            text = option,
                            state = if (option == selectedOption) McqOptionState.SELECTED else McqOptionState.NORMAL,
                            onClick = { selectedOption = option }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { selectedOption?.let { onSubmit(it) } },
                    enabled = selectedOption != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("COMPROBAR", style = MaterialTheme.typography.titleMedium)
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
                    onClick = { onSubmit(answer) },
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
                        val optionState = when {
                            option != state.selectedAnswer -> McqOptionState.NORMAL
                            state.isCorrect -> McqOptionState.CORRECT
                            else -> McqOptionState.INCORRECT
                        }
                        McqOptionButton(text = option, state = optionState, onClick = {}, locked = true)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            FeedbackBanner(
                isCorrect = state.isCorrect,
                answer = exercise.answer,
                explanation = exercise.explanation
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SIGUIENTE", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private enum class McqOptionState { NORMAL, SELECTED, CORRECT, INCORRECT }

@Composable
private fun McqOptionButton(
    text: String,
    state: McqOptionState,
    onClick: () -> Unit,
    locked: Boolean = false
) {
    val extended = OopsTheme.extendedColors
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when (state) {
        McqOptionState.NORMAL -> MaterialTheme.colorScheme.outline
        McqOptionState.SELECTED -> MaterialTheme.colorScheme.primary
        McqOptionState.CORRECT -> extended.success
        McqOptionState.INCORRECT -> MaterialTheme.colorScheme.error
    }
    // CORRECT in light mode is paired with an opaque offset "hard shadow" rect drawn behind the
    // card (see drawBehind below), mirroring ThemedCard.kt. That trick only works if the card's
    // own fill is fully opaque -- a translucent (alpha) fill would let the dark shadow rect bleed
    // through the whole card instead of just peeking out at the offset edge. So we use an opaque
    // pastel blend (lerp) for that one case instead of alpha compositing.
    val backgroundColor = when (state) {
        McqOptionState.NORMAL -> MaterialTheme.colorScheme.surface
        McqOptionState.SELECTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        McqOptionState.CORRECT -> if (extended.isDark) {
            extended.success.copy(alpha = 0.15f)
        } else {
            lerp(MaterialTheme.colorScheme.surface, extended.success, 0.18f)
        }
        McqOptionState.INCORRECT -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    }

    // Themed decoration for the CORRECT state: dark = green glow, light = hard shadow offset,
    // matching the pattern used in ThemedCard.kt.
    val isCorrectState = state == McqOptionState.CORRECT
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (isCorrectState && !extended.isDark) {
                    val offsetPx = 3.dp.toPx()
                    drawRoundRect(
                        color = extended.hardShadowColor,
                        topLeft = Offset(offsetPx, offsetPx),
                        size = size,
                        cornerRadius = CornerRadius(14.dp.toPx())
                    )
                }
            }
            .then(
                if (isCorrectState && extended.isDark) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = shape,
                        ambientColor = extended.success,
                        spotColor = extended.success
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(backgroundColor)
            .border(2.dp, borderColor, shape)
            .clickable(enabled = state == McqOptionState.NORMAL && !locked, onClick = onClick)
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
    val shape = RoundedCornerShape(14.dp)
    // Light mode pairs an opaque offset "hard shadow" rect behind the card (see drawBehind
    // below) with the card fill, same trick as ThemedCard.kt -- needs an opaque fill (lerp
    // blend) rather than alpha compositing, or the shadow rect bleeds through. Dark mode has no
    // full-size rect behind it (just a thin left-border accent), so alpha compositing is fine.
    val backgroundColor = if (extended.isDark) {
        color.copy(alpha = 0.12f)
    } else {
        lerp(MaterialTheme.colorScheme.surface, color, 0.15f)
    }
    val title = when {
        isCorrect && !extended.isDark -> "¡Correcto! +10 XP 🎉"
        isCorrect -> "¡Correcto! +10 XP"
        else -> "Incorrecto. Respuesta: $answer"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (extended.isDark) {
                    // Dark: colored left border accent (3dp).
                    drawRect(
                        color = color,
                        topLeft = Offset.Zero,
                        size = Size(3.dp.toPx(), size.height)
                    )
                } else {
                    // Light: hard shadow offset behind the card, same pattern as ThemedCard.
                    val offsetPx = 4.dp.toPx()
                    drawRoundRect(
                        color = extended.hardShadowColor,
                        topLeft = Offset(offsetPx, offsetPx),
                        size = size,
                        cornerRadius = CornerRadius(14.dp.toPx())
                    )
                }
            }
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (!extended.isDark) Modifier.border(2.dp, color, shape) else Modifier
            )
            .padding(start = if (extended.isDark) 17.dp else 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
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
