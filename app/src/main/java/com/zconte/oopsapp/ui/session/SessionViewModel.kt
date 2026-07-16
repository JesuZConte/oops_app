package com.zconte.oopsapp.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
import com.zconte.oopsapp.domain.usecase.UpdateStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject

data class SessionUiState(
    val queue: List<Exercise> = emptyList(),
    val currentExercise: ExerciseContent? = null,
    val selectedAnswer: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val isSessionComplete: Boolean = false
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val getTodaySessionUseCase: GetTodaySessionUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val queue = getTodaySessionUseCase(LocalDate.now())
            _uiState.update {
                it.copy(queue = queue, currentExercise = queue.firstOrNull()?.let(::decode))
            }
        }
    }

    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        val exercise = current.currentExercise ?: return
        val exerciseId = current.queue.first().id
        val correct = userAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        viewModelScope.launch {
            submitAnswerUseCase(exerciseId, quality = if (correct) 5 else 2, today = LocalDate.now())
        }
    }

    fun nextExercise() {
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isEmpty()) {
            viewModelScope.launch { updateStreakUseCase(LocalDate.now()) }
            _uiState.update { it.copy(isSessionComplete = true) }
        } else {
            _uiState.update {
                it.copy(
                    queue = remaining,
                    currentExercise = decode(remaining.first()),
                    isAnswered = false,
                    isCorrect = false,
                    selectedAnswer = null
                )
            }
        }
    }

    private fun decode(exercise: Exercise): ExerciseContent =
        json.decodeFromString(ExerciseContent.serializer(), exercise.payload)
}