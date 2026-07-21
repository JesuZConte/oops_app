package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

class MarkUnitProgressUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(unitId: String, today: LocalDate) {
        val exercises = exerciseRepository.getExercisesByUnit(unitId)
        // Content authoring must ensure every unit has at least one exercise: a unit with zero
        // exercises can never satisfy the "all answered" check below, so it (and any sequenced
        // gating that depends on its completion) would be permanently stuck.
        if (exercises.isEmpty()) return
        val answeredIds = exerciseRepository.getAnsweredExerciseIds(exercises.map { it.id })
        if (answeredIds.toSet().containsAll(exercises.map { it.id })) {
            contentRepository.markUnitCompleted(unitId, today)
        }
    }
}
