package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Free-form replay of a unit's full content, on demand, regardless of
 * progress: every exercise (intros included), already-answered or not, in
 * pathOrder. Unlike [GetUnitSessionUseCase], nothing here is gated or
 * filtered -- this is "let me look at this again," not "advance the
 * ladder." [SessionViewModel] treats sessions built from this use case as
 * non-graded: answers are still checked for immediate feedback, but never
 * written to review_state or counted toward streak/unit progress.
 */
class GetUnitReviewSessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(unitId: String): List<Exercise> =
        exerciseRepository.getExercisesByUnit(unitId).sortedBy { it.pathOrder ?: Int.MAX_VALUE }
}
