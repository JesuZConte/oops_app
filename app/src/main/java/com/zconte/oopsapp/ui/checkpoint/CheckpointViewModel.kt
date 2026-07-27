package com.zconte.oopsapp.ui.checkpoint

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.usecase.CheckpointSectionBreakdown
import com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCase
import com.zconte.oopsapp.domain.usecase.GetCheckpointResultBreakdownUseCase
import com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCase
import com.zconte.oopsapp.domain.usecase.IsCheckpointRetryUnlockedUseCase
import com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
import com.zconte.oopsapp.domain.usecase.UpdateStreakUseCase
import com.zconte.oopsapp.domain.usecase.computeCheckpointTimeBudgetSeconds
import com.zconte.oopsapp.domain.usecase.gradeExerciseAnswer
import com.zconte.oopsapp.domain.util.Clock
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

private const val CHECKPOINT_DEADLINE_KEY = "checkpointDeadlineMillis"

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
    val isCompleting: Boolean = false,
    val isRetryLocked: Boolean = false,
    val showIntro: Boolean = false,
    val timeBudgetSeconds: Int = 0,
    val timeRemainingSeconds: Int = 0,
    val sectionBreakdown: List<CheckpointSectionBreakdown> = emptyList()
)

@HiltViewModel
class CheckpointViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCheckpointSessionUseCase: GetCheckpointSessionUseCase,
    private val isCheckpointRetryUnlockedUseCase: IsCheckpointRetryUnlockedUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val completeCheckpointUseCase: CompleteCheckpointUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val markUnitProgressUseCase: MarkUnitProgressUseCase,
    private val json: Json,
    private val clock: Clock,
    private val getCheckpointResultBreakdownUseCase: GetCheckpointResultBreakdownUseCase
) : ViewModel() {

    private val sectionId: String = checkNotNull(savedStateHandle["sectionId"])

    private val _uiState = MutableStateFlow(CheckpointUiState())
    val uiState: StateFlow<CheckpointUiState> = _uiState.asStateFlow()

    private val answeredResults = mutableListOf<Pair<Exercise, Boolean>>()
    private var pendingAnswerJob: Job? = null

    init {
        viewModelScope.launch {
            if (!isCheckpointRetryUnlockedUseCase(sectionId)) {
                _uiState.update { it.copy(isRetryLocked = true) }
                return@launch
            }
            val queue = getCheckpointSessionUseCase(sectionId, LocalDate.now())
            if (queue.isEmpty()) {
                _uiState.update { it.copy(isComplete = true, result = CheckpointResult(0, false)) }
            } else {
                _uiState.update {
                    it.copy(
                        queue = queue,
                        totalExercises = queue.size,
                        showIntro = true,
                        timeBudgetSeconds = computeCheckpointTimeBudgetSeconds(queue.size)
                    )
                }
            }
        }
    }

    fun startCheckpoint() {
        val state = _uiState.value
        if (!state.showIntro) return
        val deadline = clock.nowMillis() + state.timeBudgetSeconds * 1000L
        savedStateHandle[CHECKPOINT_DEADLINE_KEY] = deadline
        _uiState.update {
            it.copy(
                showIntro = false,
                currentIndex = 1,
                currentExercise = decode(it.queue.first()),
                timeRemainingSeconds = state.timeBudgetSeconds
            )
        }
    }

    fun tick() {
        val state = _uiState.value
        if (state.showIntro || state.isComplete || state.isRetryLocked || state.isCompleting) return
        val deadline = savedStateHandle.get<Long>(CHECKPOINT_DEADLINE_KEY) ?: return
        val remainingMillis = deadline - clock.nowMillis()
        if (remainingMillis <= 0) {
            _uiState.update { it.copy(timeRemainingSeconds = 0, isCompleting = true) }
            viewModelScope.launch { finishCheckpoint() }
        } else {
            _uiState.update { it.copy(timeRemainingSeconds = (remainingMillis / 1000).toInt()) }
        }
    }

    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        if (current.isAnswered) return
        val exercise = current.currentExercise ?: return
        val queuedExercise = current.queue.first()
        val correct = gradeExerciseAnswer(exercise, userAnswer)
        answeredResults.add(queuedExercise to correct)

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        pendingAnswerJob = viewModelScope.launch {
            submitAnswerUseCase(queuedExercise.id, quality = if (correct) 5 else 2, today = LocalDate.now())
            markUnitProgressUseCase(queuedExercise.unitId, LocalDate.now())
        }
    }

    fun nextExercise() {
        if (_uiState.value.isCompleting) return
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isEmpty()) {
            _uiState.update { it.copy(isCompleting = true) }
            viewModelScope.launch { finishCheckpoint() }
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

    private suspend fun finishCheckpoint() {
        pendingAnswerJob?.join()
        updateStreakUseCase(LocalDate.now())
        val correctCount = answeredResults.count { it.second }
        val failedExerciseIds = answeredResults.filter { !it.second }.map { it.first.id }
        val result = completeCheckpointUseCase(
            sectionId = sectionId,
            kind = CheckpointKind.REVIEW,
            correctCount = correctCount,
            totalCount = _uiState.value.totalExercises,
            today = LocalDate.now(),
            failedExerciseIds = failedExerciseIds
        )
        val breakdown = if (!result.passed) getCheckpointResultBreakdownUseCase(answeredResults) else emptyList()
        _uiState.update { it.copy(isComplete = true, result = result, sectionBreakdown = breakdown) }
    }

    private fun decode(exercise: Exercise): ExerciseContent =
        json.decodeFromString(ExerciseContent.serializer(), exercise.payload)
}
