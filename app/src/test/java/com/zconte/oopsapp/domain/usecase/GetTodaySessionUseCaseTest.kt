package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForSession(
    private val due: List<Exercise> = emptyList(),
    private val new: List<Exercise> = emptyList()
) : ExerciseRepository {
    val savedStates = mutableListOf<ReviewState>()

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = due.take(limit)
    override suspend fun getNewExercises(limit: Int): List<Exercise> = new.take(limit)
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        savedStates.find { it.exerciseId == exerciseId }
    override suspend fun saveReviewState(state: ReviewState) {
        savedStates.removeAll { it.exerciseId == state.exerciseId }
        savedStates.add(state)
    }
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        savedStates.map { it.exerciseId }.filter { it in exerciseIds }
}

class GetTodaySessionUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)

    private fun exercise(id: String) = Exercise(id, "java-streams", "fill_blank", "{}", 1)

    @Test
    fun `session lists due exercises before new ones`() = runTest {
        val repository = FakeExerciseRepositoryForSession(
            due = listOf(exercise("due-1"), exercise("due-2")),
            new = listOf(exercise("new-1"))
        )
        val useCase = GetTodaySessionUseCase(repository)

        val result = useCase(today)

        assertEquals(listOf("due-1", "due-2", "new-1"), result.map { it.id })
    }

    @Test
    fun `session limits new exercises to the requested count`() = runTest {
        val repository = FakeExerciseRepositoryForSession(
            due = emptyList(),
            new = listOf(exercise("new-1"), exercise("new-2"), exercise("new-3"))
        )
        val useCase = GetTodaySessionUseCase(repository)

        val result = useCase(today, newExercisesLimit = 2)

        assertEquals(listOf("new-1", "new-2"), result.map { it.id })
    }
}