package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseRole

/**
 * Ladder candidate selection shared by [GetTodaySessionUseCase] and
 * [GetUnitSessionUseCase]. A concept is "born" once its solo/practice
 * exercise(s) have been answered -- answering only a guided step does NOT
 * count, so a player who answers a guided step and quits still gets the
 * concept's solo step (and, harmlessly, its intro card) offered again next
 * session, rather than losing it. Born concepts are dropped entirely (intro
 * included). Composition concepts (non-empty dependsOn) are gated until all
 * their dependency concepts are born. Already-answered exercises are never
 * re-offered regardless of role. Legacy exercises (conceptId == null) keep
 * the original behavior: offered when unanswered. Results are ordered by
 * pathOrder (legacy/null last, original order kept); no limit is applied
 * here, so callers that need a daily cap take() it themselves.
 */
fun selectPathCandidates(unitExercises: List<Exercise>, answeredIds: Set<String>): List<Exercise> {
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

    return candidates.sortedBy { it.pathOrder ?: Int.MAX_VALUE }
}
