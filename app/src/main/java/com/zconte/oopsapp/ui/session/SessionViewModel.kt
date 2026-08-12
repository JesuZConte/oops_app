package com.zconte.oopsapp.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCase
import com.zconte.oopsapp.domain.usecase.GetUnitReviewSessionUseCase
import com.zconte.oopsapp.domain.usecase.GetUnitSessionUseCase
import com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
import com.zconte.oopsapp.domain.usecase.UpdateStreakUseCase
import com.zconte.oopsapp.domain.usecase.gradeExerciseAnswer
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
    savedStateHandle: SavedStateHandle,
    private val getTodaySessionUseCase: GetTodaySessionUseCase,
    private val getUnitSessionUseCase: GetUnitSessionUseCase,
    private val getUnitReviewSessionUseCase: GetUnitReviewSessionUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val markUnitProgressUseCase: MarkUnitProgressUseCase,
    private val json: Json
) : ViewModel() {

    private val unitId: String? = savedStateHandle["unitId"]

    // Free-form replay of an already-played unit: answers get immediate feedback
    // but are never persisted to review_state, streak, or unit progress.
    private val isReview: Boolean = savedStateHandle["isReview"] ?: false

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var pendingAnswerJob: Job? = null

    // Every exercise id ever placed in the queue this session, across all batches -- lets a
    // unit session tell "nothing left" apart from "the DB hasn't caught up yet" when it
    // re-queries after draining a batch (see nextExercise()).
    private val servedExerciseIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            val queue = when {
                unitId != null && isReview -> getUnitReviewSessionUseCase(unitId)
                unitId != null -> getUnitSessionUseCase(unitId)
                else -> getTodaySessionUseCase(LocalDate.now())
            }
            if (queue.isEmpty()) {
                // Nothing due and nothing new: nothing to show, so the session is trivially complete.
                _uiState.update { it.copy(isSessionComplete = true) }
            } else {
                servedExerciseIds += queue.map { it.id }
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
        val queuedExercise = current.queue.first()
        val correct = gradeExerciseAnswer(exercise, userAnswer)

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        if (isReview) return

        pendingAnswerJob = viewModelScope.launch {
            submitAnswerUseCase(queuedExercise.id, quality = if (correct) 5 else 2, today = LocalDate.now())
            markUnitProgressUseCase(queuedExercise.unitId, LocalDate.now())
        }
    }

    fun nextExercise() {
        if (_uiState.value.isCompleting) return
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    queue = remaining,
                    currentExercise = decode(remaining.first()),
                    isAnswered = false,
                    isCorrect = false,
                    selectedAnswer = null
                )
            }
            return
        }

        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            // Wait for the last exercise's answer write before continuing or completing, so
            // the just-answered concept counts as "born" if we re-query, and navigating away
            // (which clears this ViewModel's scope) can't cancel the write mid-flight.
            pendingAnswerJob?.join()

            // A direct unit tap can drain a batch (one concept's intro/guided/solo) while the
            // unit still has more concepts behind a dependsOn gate. Re-querying now picks up
            // whatever just got unblocked by the answer above, so one tap plays the whole unit
            // instead of ending the session and making the user tap back in per concept. The
            // servedExerciseIds filter guards against looping on the same batch: everything
            // getUnitSessionUseCase can still return here is either newly unblocked or an
            // unanswered leftover, never a repeat of what this session already served.
            val continuation = if (unitId != null && !isReview) {
                getUnitSessionUseCase(unitId).filterNot { it.id in servedExerciseIds }
            } else {
                emptyList()
            }

            if (continuation.isNotEmpty()) {
                servedExerciseIds += continuation.map { it.id }
                _uiState.update {
                    it.copy(
                        queue = continuation,
                        currentExercise = decode(continuation.first()),
                        totalExercises = it.totalExercises + continuation.size,
                        isAnswered = false,
                        isCorrect = false,
                        selectedAnswer = null,
                        isCompleting = false
                    )
                }
            } else {
                if (!isReview) updateStreakUseCase(LocalDate.now())
                _uiState.update { it.copy(isSessionComplete = true) }
            }
        }
    }

    private fun decode(exercise: Exercise): ExerciseContent =
        json.decodeFromString(ExerciseContent.serializer(), exercise.payload)
}
