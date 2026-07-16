package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.srs.SchedulerSm2
import java.time.LocalDate
import javax.inject.Inject

class SubmitAnswerUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(exerciseId: String, quality: Int, today: LocalDate): ReviewState {
        val current = exerciseRepository.getReviewState(exerciseId)
            ?: ReviewState(
                exerciseId = exerciseId, easeFactor = 2.5, intervalDays = 0,
                repetitions = 0, dueDate = today
            )
        val updated = SchedulerSm2.review(current, quality, today)
        exerciseRepository.saveReviewState(updated)
        return updated
    }
}