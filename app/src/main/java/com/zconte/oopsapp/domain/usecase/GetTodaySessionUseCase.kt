package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseRole
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
            selectPathExercises(unitExercises, answeredIds, newExercisesLimit)
        } ?: emptyList()

        return due + new
    }

    /**
     * Phase-A selection. A concept is "born" once its solo/practice exercise(s)
     * have been answered -- answering only a guided step does NOT count, so a
     * player who answers a guided step and quits still gets the concept's solo
     * step (and, harmlessly, its intro card) offered again next session, rather
     * than losing it. Born concepts are dropped entirely (intro included).
     * Composition concepts (non-empty dependsOn) are gated until all their
     * dependency concepts are born. Already-answered exercises are never
     * re-offered as "new" regardless of role. Legacy exercises (conceptId ==
     * null) keep today's behavior: offered when unanswered. Results are
     * ordered by pathOrder (legacy/null last, original order kept).
     */
    private fun selectPathExercises(
        unitExercises: List<Exercise>,
        answeredIds: Set<String>,
        limit: Int
    ): List<Exercise> {
        val bornConceptIds = unitExercises
            .filter {
                it.conceptId != null &&
                    (it.role == ExerciseRole.SOLO || it.role == ExerciseRole.PRACTICE) &&
                    it.id in answeredIds
            }
            .mapNotNull { it.conceptId }
            .toSet()

        val candidates = unitExercises.filter { ex ->
            if (ex.id in answeredIds) return@filter false
            val concept = ex.conceptId
            concept == null || (concept !in bornConceptIds && ex.dependsOn.all { it in bornConceptIds })
        }

        return candidates
            .sortedBy { it.pathOrder ?: Int.MAX_VALUE }
            .take(limit)
    }
}
