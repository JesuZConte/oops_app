package com.zconte.oopsapp.domain.usecase

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

private class FakeExerciseRepositoryForUnitProgress(
    private val exercisesByUnit: Map<String, List<Exercise>> = emptyMap(),
    private val answeredIds: Set<String> = emptySet()
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        exerciseIds.filter { it in answeredIds }
}

private class FakeContentRepositoryForUnitProgress : ContentRepository {
    val markedComplete = mutableListOf<String>()
    override suspend fun getSections(): List<Section> = emptyList()
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = emptyList()
    override suspend fun getCompletedUnitIds(): List<String> = markedComplete
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate) {
        markedComplete.add(unitId)
    }
}

class MarkUnitProgressUseCaseTest {

    private val today = LocalDate.of(2026, 7, 20)

    private fun exercise(id: String) = Exercise(id, "unit-1", "fill_blank", "{}", 1)

    @Test
    fun `marks the unit complete when every exercise has been answered`() = runTest {
        val exerciseRepository = FakeExerciseRepositoryForUnitProgress(
            exercisesByUnit = mapOf("unit-1" to listOf(exercise("ex-1"), exercise("ex-2"))),
            answeredIds = setOf("ex-1", "ex-2")
        )
        val contentRepository = FakeContentRepositoryForUnitProgress()
        val useCase = MarkUnitProgressUseCase(exerciseRepository, contentRepository)

        useCase("unit-1", today)

        assertEquals(listOf("unit-1"), contentRepository.markedComplete)
    }

    @Test
    fun `does not mark the unit complete when some exercise is unanswered`() = runTest {
        val exerciseRepository = FakeExerciseRepositoryForUnitProgress(
            exercisesByUnit = mapOf("unit-1" to listOf(exercise("ex-1"), exercise("ex-2"))),
            answeredIds = setOf("ex-1")
        )
        val contentRepository = FakeContentRepositoryForUnitProgress()
        val useCase = MarkUnitProgressUseCase(exerciseRepository, contentRepository)

        useCase("unit-1", today)

        assertTrue(contentRepository.markedComplete.isEmpty())
    }
}
