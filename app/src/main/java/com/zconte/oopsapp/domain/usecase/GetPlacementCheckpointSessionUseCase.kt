package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import javax.inject.Inject

private const val QUESTIONS_PER_SKIPPED_UNIT = 3
private const val MAX_SIZE = 24

class GetPlacementCheckpointSessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(skippedUnitIds: List<String>): List<Exercise> {
        val pool = skippedUnitIds.flatMap { exerciseRepository.getExercisesByUnit(it) }
        val targetSize = (skippedUnitIds.size * QUESTIONS_PER_SKIPPED_UNIT).coerceAtMost(MAX_SIZE)
        return pool.shuffled().take(targetSize.coerceAtMost(pool.size))
    }
}
