package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import javax.inject.Inject

class IsCheckpointRetryUnlockedUseCase @Inject constructor(
    private val checkpointRepository: CheckpointRepository,
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(sectionId: String, kind: String = CheckpointKind.REVIEW): Boolean {
        val lastFailed = checkpointRepository.getLatestFailedAttempt(sectionId, kind) ?: return true
        if (lastFailed.failedExerciseIds.isEmpty()) return true

        return lastFailed.failedExerciseIds.all { exerciseId ->
            val state = exerciseRepository.getReviewState(exerciseId)
            // Strict '>': re-exposure on the SAME day as the failed attempt does not count -- that
            // is exactly what the failed attempt's own answers just wrote via SubmitAnswerUseCase.
            state != null && state.lastReviewedAt > lastFailed.takenAt
        }
    }
}
