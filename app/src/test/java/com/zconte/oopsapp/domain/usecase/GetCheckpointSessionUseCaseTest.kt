package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForCheckpoint(
    private val bySection: Map<String, List<Exercise>> = emptyMap(),
    private val due: List<Exercise> = emptyList()
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = due.take(limit)
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
    override suspend fun getUnitSummary(unitId: String): UnitSummary? = null
}

class GetCheckpointSessionUseCaseTest {

    private val today = LocalDate.of(2026, 7, 26)

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "mcq", "{}", 1)

    @Test
    fun `checkpointSize starts at the floor for the first section`() {
        assertEquals(8, checkpointSize(sectionsTraversed = 1))
    }

    @Test
    fun `checkpointSize grows by 2 per section traversed`() {
        assertEquals(10, checkpointSize(sectionsTraversed = 2))
        assertEquals(16, checkpointSize(sectionsTraversed = 5))
    }

    @Test
    fun `checkpointSize is capped at the ceiling`() {
        assertEquals(20, checkpointSize(sectionsTraversed = 7))
        assertEquals(20, checkpointSize(sectionsTraversed = 50))
    }

    @Test
    fun `the first section draws entirely from its own pool, sized at the floor`() = runTest {
        val currentPool = (1..15).map { exercise("s1-ex-$it", "s1-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(bySection = mapOf("s1" to currentPool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s1", today)

        assertEquals(8, result.size)
        assertTrue(result.all { it.id.startsWith("s1-ex-") })
    }

    @Test
    fun `a later section mixes in roughly half from earlier sections`() = runTest {
        val s1Pool = (1..15).map { exercise("s1-ex-$it", "s1-unit") }
        val s2Pool = (1..20).map { exercise("s2-ex-$it", "s2-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(bySection = mapOf("s1" to s1Pool, "s2" to s2Pool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1), section("s2", 2)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s2", today)

        // sectionsTraversed = 2 -> size 10; half-and-half split -> 5 prior, 5 current.
        assertEquals(10, result.size)
        assertEquals(5, result.count { it.id.startsWith("s1-ex-") })
        assertEquals(5, result.count { it.id.startsWith("s2-ex-") })
    }

    @Test
    fun `due prior exercises are preferred over non-due ones when sampling earlier sections`() = runTest {
        val duePool = (1..5).map { exercise("s1-due-$it", "s1-unit") }
        val restPool = (1..20).map { exercise("s1-rest-$it", "s1-unit") }
        val s2Pool = (1..20).map { exercise("s2-ex-$it", "s2-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(
            bySection = mapOf("s1" to duePool + restPool, "s2" to s2Pool),
            due = duePool
        )
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1), section("s2", 2)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s2", today)

        // sectionsTraversed = 2 -> size 10 -> 5 prior. There are exactly 5 due items, so all 5
        // must be used before any non-due prior item.
        val priorInResult = result.filter { it.id.startsWith("s1-") }
        assertEquals(5, priorInResult.size)
        assertTrue(priorInResult.all { it.id.startsWith("s1-due-") })
    }

    @Test
    fun `a small section pool is capped, not padded past what exists`() = runTest {
        val s1Pool = listOf(exercise("s1-ex-1", "s1-unit"), exercise("s1-ex-2", "s1-unit"))
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(bySection = mapOf("s1" to s1Pool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s1", today)

        assertEquals(2, result.size)
    }

    @Test
    fun `an unknown section id yields no session`() = runTest {
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint()
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("does-not-exist", today)

        assertTrue(result.isEmpty())
    }
}
