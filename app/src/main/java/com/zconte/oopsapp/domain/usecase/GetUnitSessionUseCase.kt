package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import javax.inject.Inject

class GetUnitSessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    /**
     * Builds the ladder for a unit played directly from Ruta: intro card,
     * then guided, then solo/practice, per concept, gated by dependsOn and
     * excluding what's already answered -- the same Phase-A candidate rules
     * [GetTodaySessionUseCase] applies, just scoped to one unit and with no
     * daily cap, since tapping a unit means "play it," not "today's dose."
     * A unit with nothing left to offer returns an empty list.
     */
    suspend operator fun invoke(unitId: String): List<Exercise> {
        val unitExercises = exerciseRepository.getExercisesByUnit(unitId)
        val answeredIds = exerciseRepository
            .getAnsweredExerciseIds(unitExercises.map { it.id })
            .toSet()
        return selectPathCandidates(unitExercises, answeredIds)
    }
}
