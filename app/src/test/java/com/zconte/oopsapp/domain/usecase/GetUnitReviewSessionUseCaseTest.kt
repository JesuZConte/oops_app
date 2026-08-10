package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForUnitReview(
    private val byUnit: Map<String, List<Exercise>>
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = byUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = exerciseIds
}

private fun exercise(id: String, unitId: String = "unit-1", type: String = "mcq", pathOrder: Int? = null) =
    Exercise(id = id, unitId = unitId, type = type, payload = "{}", difficulty = 1, pathOrder = pathOrder)

class GetUnitReviewSessionUseCaseTest {

    @Test
    fun `returns every exercise in the unit including intros and already-answered ones, in pathOrder`() = runTest {
        val intro = exercise("intro-1", type = "worked_example", pathOrder = 1)
        val guided = exercise("guided-1", pathOrder = 2)
        val solo = exercise("solo-1", pathOrder = 3)
        val repository = FakeExerciseRepositoryForUnitReview(mapOf("unit-1" to listOf(solo, intro, guided)))
        val useCase = GetUnitReviewSessionUseCase(repository)

        val result = useCase("unit-1")

        assertEquals(listOf("intro-1", "guided-1", "solo-1"), result.map { it.id })
    }

    @Test
    fun `exercises without a pathOrder sort after those that have one`() = runTest {
        val legacy = exercise("legacy-1", pathOrder = null)
        val laddered = exercise("laddered-1", pathOrder = 0)
        val repository = FakeExerciseRepositoryForUnitReview(mapOf("unit-1" to listOf(legacy, laddered)))
        val useCase = GetUnitReviewSessionUseCase(repository)

        val result = useCase("unit-1")

        assertEquals(listOf("laddered-1", "legacy-1"), result.map { it.id })
    }
}
