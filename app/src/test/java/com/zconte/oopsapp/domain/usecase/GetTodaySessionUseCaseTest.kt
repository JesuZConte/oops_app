package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.testutil.FakeCheckpointRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForTodaySession(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

private class FakeExerciseRepositoryForSession(
    private val due: List<Exercise> = emptyList(),
    private val exercisesByUnit: Map<String, List<Exercise>> = emptyMap(),
    private val answeredIds: Set<String> = emptySet()
) : ExerciseRepository {
    val savedStates = mutableListOf<ReviewState>()

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = due.take(limit)
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        savedStates.find { it.exerciseId == exerciseId }
    override suspend fun saveReviewState(state: ReviewState) {
        savedStates.removeAll { it.exerciseId == state.exerciseId }
        savedStates.add(state)
    }
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        exerciseIds.filter { it in answeredIds }
}

class GetTodaySessionUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)

    private fun exercise(id: String, unitId: String = "s1-u1") = Exercise(id, unitId, "fill_blank", "{}", 1)
    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)

    @Test
    fun `session lists due exercises before new ones`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            due = listOf(exercise("due-1"), exercise("due-2")),
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("new-1")))
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository()))
        )

        val result = useCase(today)

        assertEquals(listOf("due-1", "due-2", "new-1"), result.map { it.id })
    }

    @Test
    fun `new exercises are limited to the requested count`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(exercise("new-1"), exercise("new-2"), exercise("new-3"))
            )
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository()))
        )

        val result = useCase(today, newExercisesLimit = 2)

        assertEquals(listOf("new-1", "new-2"), result.map { it.id })
    }

    @Test
    fun `new exercises never come from a unit other than the current one`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))
            ),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(exercise("current-1", "s1-u1")),
                "s1-u2" to listOf(exercise("other-unit-1", "s1-u2"))
            )
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository()))
        )

        val result = useCase(today)

        assertEquals(listOf("current-1"), result.map { it.id })
    }

    @Test
    fun `already-answered exercises in the current unit are not offered again as new`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("answered-1"), exercise("unanswered-1"))),
            answeredIds = setOf("answered-1")
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository()))
        )

        val result = useCase(today)

        assertEquals(listOf("unanswered-1"), result.map { it.id })
    }

    @Test
    fun `no new exercises once every section is fully complete`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(CompletedUnit("s1-u1", com.zconte.oopsapp.domain.model.UnitCompletionSource.PLAYED))
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            due = listOf(exercise("due-1"))
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository()))
        )

        val result = useCase(today)

        assertEquals(listOf("due-1"), result.map { it.id })
    }
}
