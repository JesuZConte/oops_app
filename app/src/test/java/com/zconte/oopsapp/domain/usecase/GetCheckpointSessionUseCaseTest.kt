package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForCheckpoint(
    private val bySection: Map<String, List<Exercise>>
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = bySection[sectionId] ?: emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = emptyList()
}

private class FakeContentRepositoryForCheckpoint(
    private val sections: List<Section>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = emptyList()
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetCheckpointSessionUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "mcq", "{}", 1)

    @Test
    fun `the first section's checkpoint is entirely from that section (no earlier content exists)`() = runTest {
        val currentPool = (1..15).map { exercise("s1-ex-$it", "s1-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(mapOf("s1" to currentPool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s1")

        assertEquals(12, result.size)
        assertTrue(result.all { it.id.startsWith("s1-ex-") })
    }

    @Test
    fun `a later section's checkpoint mixes in up to 3 questions from earlier sections`() = runTest {
        val s1Pool = (1..15).map { exercise("s1-ex-$it", "s1-unit") }
        val s2Pool = (1..20).map { exercise("s2-ex-$it", "s2-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(mapOf("s1" to s1Pool, "s2" to s2Pool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1), section("s2", 2)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s2")

        assertEquals(12, result.size)
        val fromEarlier = result.count { it.id.startsWith("s1-ex-") }
        val fromCurrent = result.count { it.id.startsWith("s2-ex-") }
        assertEquals(3, fromEarlier)
        assertEquals(9, fromCurrent)
    }

    @Test
    fun `a small section pool is capped, not padded past what exists`() = runTest {
        val s1Pool = listOf(exercise("s1-ex-1", "s1-unit"), exercise("s1-ex-2", "s1-unit"))
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(mapOf("s1" to s1Pool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s1")

        assertEquals(2, result.size)
    }
}
