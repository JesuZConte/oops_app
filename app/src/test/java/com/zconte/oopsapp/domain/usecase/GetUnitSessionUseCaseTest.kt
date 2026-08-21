package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseRole
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForUnitSession(
    private val byUnit: Map<String, List<Exercise>>,
    private val answered: Set<String> = emptySet()
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = byUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        exerciseIds.filter { it in answered }
}

private fun exercise(
    id: String,
    unitId: String = "unit-1",
    type: String = "mcq",
    pathOrder: Int? = null,
    conceptId: String? = null,
    role: String? = null,
    dependsOn: List<String> = emptyList()
) = Exercise(
    id = id,
    unitId = unitId,
    type = type,
    payload = "{}",
    difficulty = 1,
    conceptId = conceptId,
    role = role,
    pathOrder = pathOrder,
    dependsOn = dependsOn
)

class GetUnitSessionUseCaseTest {

    @Test
    fun `a fresh unit surfaces the intro card ahead of its guided and solo steps`() = runTest {
        val intro = exercise("intro-1", type = "worked_example", pathOrder = 1, conceptId = "c1", role = ExerciseRole.INTRO)
        val guided = exercise("guided-1", pathOrder = 2, conceptId = "c1", role = ExerciseRole.GUIDED)
        val solo = exercise("solo-1", pathOrder = 3, conceptId = "c1", role = ExerciseRole.SOLO)
        val repository = FakeExerciseRepositoryForUnitSession(mapOf("unit-1" to listOf(guided, solo, intro)))
        val useCase = GetUnitSessionUseCase(repository)

        val result = useCase("unit-1")

        assertEquals(listOf("intro-1", "guided-1", "solo-1"), result.map { it.id })
    }

    @Test
    fun `answering only the guided step does not born the concept, so the intro replays`() = runTest {
        val intro = exercise("intro-1", type = "worked_example", pathOrder = 1, conceptId = "c1", role = ExerciseRole.INTRO)
        val guided = exercise("guided-1", pathOrder = 2, conceptId = "c1", role = ExerciseRole.GUIDED)
        val solo = exercise("solo-1", pathOrder = 3, conceptId = "c1", role = ExerciseRole.SOLO)
        val repository = FakeExerciseRepositoryForUnitSession(
            byUnit = mapOf("unit-1" to listOf(intro, guided, solo)),
            answered = setOf("guided-1")
        )
        val useCase = GetUnitSessionUseCase(repository)

        val result = useCase("unit-1")

        assertEquals(listOf("intro-1", "solo-1"), result.map { it.id })
    }

    @Test
    fun `a concept gated by an unborn dependency is withheld`() = runTest {
        val introA = exercise("intro-a", type = "worked_example", pathOrder = 1, conceptId = "a", role = ExerciseRole.INTRO)
        val soloA = exercise("solo-a", pathOrder = 2, conceptId = "a", role = ExerciseRole.SOLO)
        val introB = exercise("intro-b", type = "worked_example", pathOrder = 3, conceptId = "b", role = ExerciseRole.INTRO, dependsOn = listOf("a"))
        val soloB = exercise("solo-b", pathOrder = 4, conceptId = "b", role = ExerciseRole.SOLO, dependsOn = listOf("a"))
        val repository = FakeExerciseRepositoryForUnitSession(mapOf("unit-1" to listOf(introA, soloA, introB, soloB)))
        val useCase = GetUnitSessionUseCase(repository)

        val result = useCase("unit-1")

        assertEquals(listOf("intro-a", "solo-a"), result.map { it.id })
    }

    @Test
    fun `a fully completed unit returns an empty queue`() = runTest {
        val intro = exercise("intro-1", type = "worked_example", pathOrder = 1, conceptId = "c1", role = ExerciseRole.INTRO)
        val solo = exercise("solo-1", pathOrder = 2, conceptId = "c1", role = ExerciseRole.SOLO)
        val repository = FakeExerciseRepositoryForUnitSession(
            byUnit = mapOf("unit-1" to listOf(intro, solo)),
            answered = setOf("solo-1")
        )
        val useCase = GetUnitSessionUseCase(repository)

        val result = useCase("unit-1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `legacy exercises without a conceptId are offered when unanswered`() = runTest {
        val legacy1 = exercise("legacy-1", pathOrder = 1)
        val legacy2 = exercise("legacy-2", pathOrder = 2)
        val repository = FakeExerciseRepositoryForUnitSession(mapOf("unit-1" to listOf(legacy1, legacy2)))
        val useCase = GetUnitSessionUseCase(repository)

        val result = useCase("unit-1")

        assertEquals(listOf("legacy-1", "legacy-2"), result.map { it.id })
    }
}
