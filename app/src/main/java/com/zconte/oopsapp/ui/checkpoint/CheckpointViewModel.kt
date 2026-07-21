package com.zconte.oopsapp.ui.checkpoint

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCase
import com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
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

data class CheckpointUiState(
    val queue: List<Exercise> = emptyList(),
    val currentExercise: ExerciseContent? = null,
    val currentIndex: Int = 0,
    val totalExercises: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val isComplete: Boolean = false,
    val result: CheckpointResult? = null,
    val isCompleting: Boolean = false
)

@HiltViewModel
class CheckpointViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCheckpointSessionUseCase: GetCheckpointSessionUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val completeCheckpointUseCase: CompleteCheckpointUseCase,
    private val json: Json
) : ViewModel() {

    private val sectionId: String = checkNotNull(savedStateHandle["sectionId"])

    private val _uiState = MutableStateFlow(CheckpointUiState())
    val uiState: StateFlow<CheckpointUiState> = _uiState.asStateFlow()

    private var correctCount = 0
    private var pendingAnswerJob: Job? = null

    init {
        viewModelScope.launch {
            val queue = getCheckpointSessionUseCase(sectionId)
            if (queue.isEmpty()) {
                _uiState.update { it.copy(isComplete = true, result = CheckpointResult(0, false)) }
            } else {
                _uiState.update {
                    it.copy(
                        queue = queue,
                        totalExercises = queue.size,
                        currentIndex = 1,
                        currentExercise = decode(queue.first())
                    )
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
        if (correct) correctCount++

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
                pendingAnswerJob?.join()
                val result = completeCheckpointUseCase(
                    sectionId = sectionId,
                    kind = CheckpointKind.REVIEW,
                    correctCount = correctCount,
                    totalCount = _uiState.value.totalExercises,
                    today = LocalDate.now()
                )
                _uiState.update { it.copy(isComplete = true, result = result) }
            }
        } else {
            _uiState.update {
                it.copy(
                    queue = remaining,
                    currentIndex = it.currentIndex + 1,
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
