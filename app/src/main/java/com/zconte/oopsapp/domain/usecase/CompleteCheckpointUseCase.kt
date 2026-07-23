package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

private const val PASS_THRESHOLD_PCT = 68
private const val SEED_EASE_FACTOR = 2.5
private const val SEED_INTERVAL_DAYS = 1
private const val SEED_REPETITIONS = 1

/**
 * Pure grading so callers (e.g. a checkpoint ViewModel) can predict pass/fail from the same
 * correct/total counts before committing any side effects, without duplicating the formula.
 */
fun computeCheckpointResult(correctCount: Int, totalCount: Int): CheckpointResult {
    val scorePct = if (totalCount == 0) 0 else (correctCount * 100) / totalCount
    return CheckpointResult(scorePct, scorePct >= PASS_THRESHOLD_PCT)
}

class CompleteCheckpointUseCase @Inject constructor(
    private val checkpointRepository: CheckpointRepository,
    private val contentRepository: ContentRepository,
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(
        sectionId: String,
        kind: String,
        correctCount: Int,
        totalCount: Int,
        today: LocalDate,
        skippedUnitIds: List<String> = emptyList()
    ): CheckpointResult {
        val result = computeCheckpointResult(correctCount, totalCount)
        checkpointRepository.recordAttempt(sectionId, kind, result.scorePct, result.passed, today)

        if (kind == CheckpointKind.PLACEMENT && result.passed) {
            unlockSkippedUnits(skippedUnitIds, today)
        }

        return result
    }

    private suspend fun unlockSkippedUnits(skippedUnitIds: List<String>, today: LocalDate) {
        skippedUnitIds.forEach { unitId ->
            contentRepository.markUnitCompleted(unitId, today, UnitCompletionSource.PLACEMENT)
            exerciseRepository.getExercisesByUnit(unitId).forEach { exercise ->
                if (exerciseRepository.getReviewState(exercise.id) == null) {
                    exerciseRepository.saveReviewState(
                        ReviewState(
                            exerciseId = exercise.id,
                            easeFactor = SEED_EASE_FACTOR,
                            intervalDays = SEED_INTERVAL_DAYS,
                            repetitions = SEED_REPETITIONS,
                            dueDate = today.plusDays(1)
                        )
                    )
                }
            }
        }
    }
}
