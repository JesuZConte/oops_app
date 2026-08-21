package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

class GetTodaySessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val getCurrentUnitUseCase: GetCurrentUnitUseCase
) {
    suspend operator fun invoke(
        today: LocalDate,
        newExercisesLimit: Int = 5,
        dueExercisesLimit: Int = 10,
        staleThresholdDays: Long = 45,
        random: Random = Random.Default
    ): List<Exercise> {
        // Phase B -- review: the due backlog is already weakness-ordered by ExerciseDao.getDue's
        // ORDER BY. Cap it, reserve 1 slot for a randomly-picked item stale enough that weakness
        // ranking alone would never resurface it, then shuffle only the presentation order.
        val staleCutoff = today.minusDays(staleThresholdDays)
        val staleCandidates = exerciseRepository.getStaleExercises(staleCutoff)
        val agedPick = staleCandidates.shuffled(random).firstOrNull()

        val weaknessSlots = dueExercisesLimit - if (agedPick != null) 1 else 0
        val dueByWeakness = exerciseRepository.getDueExercises(today, limit = Int.MAX_VALUE)
            .filter { it.id != agedPick?.id }
            .take(weaknessSlots)

        val review = (listOfNotNull(agedPick) + dueByWeakness).shuffled(random)

        // Phase A -- Path: advance the current unit in authored order, never shuffled.
        val currentUnit = getCurrentUnitUseCase()
        val new = currentUnit?.let { unit ->
            val unitExercises = exerciseRepository.getExercisesByUnit(unit.id)
            val answeredIds = exerciseRepository
                .getAnsweredExerciseIds(unitExercises.map { it.id })
                .toSet()
            selectPathCandidates(unitExercises, answeredIds).take(newExercisesLimit)
        } ?: emptyList()

        return review + new
    }
}
