package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate

class FakeExerciseRepository(
    private val dueExercises: List<Exercise> = emptyList(),
    private val exercisesByUnit: Map<String, List<Exercise>> = emptyMap(),
    private val exercisesBySection: Map<String, List<Exercise>> = emptyMap()
) : ExerciseRepository {

    val savedReviewStates = mutableListOf<ReviewState>()

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = dueExercises.take(limit)

    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()

    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = exercisesBySection[sectionId] ?: emptyList()

    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        savedReviewStates.find { it.exerciseId == exerciseId }

    override suspend fun saveReviewState(state: ReviewState) {
        savedReviewStates.removeAll { it.exerciseId == state.exerciseId }
        savedReviewStates.add(state)
    }

    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        exerciseIds.filter { id -> savedReviewStates.any { it.exerciseId == id } }
}
