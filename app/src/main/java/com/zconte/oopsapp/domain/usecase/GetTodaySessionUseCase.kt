package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

class GetTodaySessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val getCurrentUnitUseCase: GetCurrentUnitUseCase
) {
    suspend operator fun invoke(today: LocalDate, newExercisesLimit: Int = 5): List<Exercise> {
        // Phase B — review: everything due today (unaided SRS).
        val due = exerciseRepository.getDueExercises(today, limit = Int.MAX_VALUE)

        // Phase A — Path: advance the current unit in authored order.
        val currentUnit = getCurrentUnitUseCase()
        val new = currentUnit?.let { unit ->
            val unitExercises = exerciseRepository.getExercisesByUnit(unit.id)
            val answeredIds = exerciseRepository
                .getAnsweredExerciseIds(unitExercises.map { it.id })
                .toSet()
            selectPathCandidates(unitExercises, answeredIds).take(newExercisesLimit)
        } ?: emptyList()

        return due + new
    }
}
