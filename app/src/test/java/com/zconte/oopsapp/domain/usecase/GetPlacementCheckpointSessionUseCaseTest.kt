package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForPlacementSession(
    private val exercisesByUnit: Map<String, List<Exercise>>
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = emptyList()
}

class GetPlacementCheckpointSessionUseCaseTest {

    private fun exercisesFor(unitId: String, count: Int) =
        (1..count).map { Exercise("$unitId-ex-$it", unitId, "mcq", "{}", 1) }

    @Test
    fun `one skipped unit yields 3 questions when its pool is large enough`() = runTest {
        val repository = FakeExerciseRepositoryForPlacementSession(mapOf("u1" to exercisesFor("u1", 10)))
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(listOf("u1"))

        assertEquals(3, result.size)
        assertTrue(result.all { it.unitId == "u1" })
    }

    @Test
    fun `size scales at 3 questions per skipped unit`() = runTest {
        val repository = FakeExerciseRepositoryForPlacementSession(
            mapOf(
                "u1" to exercisesFor("u1", 10),
                "u2" to exercisesFor("u2", 10),
                "u3" to exercisesFor("u3", 10)
            )
        )
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(listOf("u1", "u2", "u3"))

        assertEquals(9, result.size)
    }

    @Test
    fun `size caps at 24 even when many units are skipped`() = runTest {
        val unitIds = (1..10).map { "u$it" }
        val exercisesByUnit = unitIds.associateWith { exercisesFor(it, 10) }
        val repository = FakeExerciseRepositoryForPlacementSession(exercisesByUnit)
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(unitIds)

        assertEquals(24, result.size)
    }

    @Test
    fun `a small combined pool is capped, not padded past what exists`() = runTest {
        val repository = FakeExerciseRepositoryForPlacementSession(mapOf("u1" to exercisesFor("u1", 2)))
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(listOf("u1"))

        assertEquals(2, result.size)
    }

    @Test
    fun `no skipped units yields an empty session`() = runTest {
        val repository = FakeExerciseRepositoryForPlacementSession(emptyMap())
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(emptyList())

        assertTrue(result.isEmpty())
    }
}
