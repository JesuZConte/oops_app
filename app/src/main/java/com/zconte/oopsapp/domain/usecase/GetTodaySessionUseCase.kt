package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

class GetTodaySessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(today: LocalDate, newExercisesLimit: Int = 5): List<Exercise> {
        val due = exerciseRepository.getDueExercises(today, limit = Int.MAX_VALUE)
        val new = exerciseRepository.getNewExercises(limit = newExercisesLimit)
        return due + new
    }
}