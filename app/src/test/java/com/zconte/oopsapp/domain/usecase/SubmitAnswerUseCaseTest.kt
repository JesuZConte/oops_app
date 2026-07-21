package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForAnswer : ExerciseRepository {
    val states = mutableMapOf<String, ReviewState>()

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = states[exerciseId]
    override suspend fun saveReviewState(state: ReviewState) {
        states[state.exerciseId] = state
    }
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        states.keys.filter { it in exerciseIds }
}

class SubmitAnswerUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `creates a default review state on first answer and applies SM-2`() = runTest {
        val repository = FakeExerciseRepositoryForAnswer()
        val useCase = SubmitAnswerUseCase(repository)

        val result = useCase("ex-1", quality = 4, today = today)

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(today.plusDays(1), result.dueDate)
        assertEquals(result, repository.states["ex-1"])
    }

    @Test
    fun `reuses the existing review state on later answers`() = runTest {
        val repository = FakeExerciseRepositoryForAnswer().apply {
            states["ex-1"] = ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1,
                repetitions = 1, dueDate = today
            )
        }
        val useCase = SubmitAnswerUseCase(repository)

        val result = useCase("ex-1", quality = 4, today = today)

        assertEquals(2, result.repetitions)
        assertEquals(6, result.intervalDays)
    }
}