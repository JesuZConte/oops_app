package com.zconte.oopsapp.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
import com.zconte.oopsapp.domain.usecase.UpdateStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val isCompleting: Boolean = false,
    val isSessionComplete: Boolean = false,
    val totalExercises: Int = 0
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

    private var pendingAnswerJob: Job? = null

    init {
        viewModelScope.launch {
            val queue = getTodaySessionUseCase(LocalDate.now())
            if (queue.isEmpty()) {
                // Nothing due and nothing new: nothing to show, so the session is trivially complete.
                _uiState.update { it.copy(isSessionComplete = true) }
            } else {
                _uiState.update {
                    it.copy(queue = queue, totalExercises = queue.size, currentExercise = decode(queue.first()))
                }
            }
        }
    }

    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        if (current.isAnswered) return
        val exercise = current.currentExercise ?: return
        val exerciseId = current.queue.first().id
        val correct = userAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        pendingAnswerJob = viewModelScope.launch {
            submitAnswerUseCase(exerciseId, quality = if (correct) 5 else 2, today = LocalDate.now())
        }
    }

    fun nextExercise() {
        if (_uiState.value.isCompleting) return
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isEmpty()) {
            _uiState.update { it.copy(isCompleting = true) }
            viewModelScope.launch {
                // Wait for the last exercise's answer write before completing, so navigating
                // away (and clearing this ViewModel's scope) can't cancel it mid-flight.
                pendingAnswerJob?.join()
                updateStreakUseCase(LocalDate.now())
                _uiState.update { it.copy(isSessionComplete = true) }
            }
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