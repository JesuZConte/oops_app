package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import java.time.LocalDate

interface ExerciseRepository {
    suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise>
    suspend fun getNewExercises(limit: Int): List<Exercise>
    suspend fun getExercisesByUnit(unitId: String): List<Exercise>
    suspend fun getExercisesBySection(sectionId: String): List<Exercise>
    suspend fun getReviewState(exerciseId: String): ReviewState?
    suspend fun saveReviewState(state: ReviewState)
    suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String>
}
