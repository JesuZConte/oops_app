package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForUnitSession(
    private val byUnit: Map<String, List<Exercise>>
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = byUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = emptyList()
}

class GetUnitSessionUseCaseTest {

    @Test
    fun `returns every exercise belonging to the requested unit`() = runTest {
        val exercise1 = Exercise("ex-1", "unit-1", "fill_blank", "{}", 1)
        val exercise2 = Exercise("ex-2", "unit-1", "mcq", "{}", 1)
        val repository = FakeExerciseRepositoryForUnitSession(mapOf("unit-1" to listOf(exercise1, exercise2)))
        val useCase = GetUnitSessionUseCase(repository)

        val result = useCase("unit-1")

        assertEquals(listOf("ex-1", "ex-2"), result.map { it.id })
    }
}
