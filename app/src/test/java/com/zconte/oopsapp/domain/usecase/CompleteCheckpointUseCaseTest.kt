package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeCheckpointRepository : CheckpointRepository {
    data class RecordedAttempt(val sectionId: String, val kind: String, val scorePct: Int, val passed: Boolean)
    val recorded = mutableListOf<RecordedAttempt>()

    override suspend fun recordAttempt(sectionId: String, kind: String, scorePct: Int, passed: Boolean, takenAt: LocalDate) {
        recorded.add(RecordedAttempt(sectionId, kind, scorePct, passed))
    }
}

private class FakeContentRepositoryForComplete : ContentRepository {
    val markedComplete = mutableListOf<Pair<String, String>>()
    override suspend fun getSections(): List<Section> = emptyList()
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = emptyList()
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {
        markedComplete.add(unitId to via)
    }
}

private class FakeExerciseRepositoryForComplete(
    private val exercisesByUnit: Map<String, List<Exercise>> = emptyMap(),
    private val existingReviewState: Set<String> = emptySet()
) : ExerciseRepository {
    val seeded = mutableListOf<ReviewState>()
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        if (exerciseId in existingReviewState) ReviewState(exerciseId, 2.5, 6, 2, LocalDate.of(2026, 8, 1)) else null
    override suspend fun saveReviewState(state: ReviewState) {
        seeded.add(state)
    }
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = emptyList()
}

class CompleteCheckpointUseCaseTest {

    private val today = LocalDate.of(2026, 7, 20)

    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "mcq", "{}", 1)

    @Test
    fun `passes at exactly the 68 percent threshold`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        val useCase = CompleteCheckpointUseCase(
            checkpointRepository, FakeContentRepositoryForComplete(), FakeExerciseRepositoryForComplete()
        )

        // 68% of 25 = 17 correct, rounds down to exactly 68 -- boundary case.
        val result = useCase("s1", CheckpointKind.REVIEW, correctCount = 17, totalCount = 25, today = today)

        assertEquals(68, result.scorePct)
        assertTrue(result.passed)
        assertEquals(1, checkpointRepository.recorded.size)
        assertTrue(checkpointRepository.recorded.first().passed)
    }

    @Test
    fun `fails below the 68 percent threshold`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        val useCase = CompleteCheckpointUseCase(
            checkpointRepository, FakeContentRepositoryForComplete(), FakeExerciseRepositoryForComplete()
        )

        val result = useCase("s1", CheckpointKind.REVIEW, correctCount = 6, totalCount = 12, today = today)

        assertEquals(50, result.scorePct)
        assertFalse(result.passed)
    }

    @Test
    fun `a passed review checkpoint never marks units complete even if skippedUnitIds is passed by mistake`() = runTest {
        val contentRepository = FakeContentRepositoryForComplete()
        val useCase = CompleteCheckpointUseCase(
            FakeCheckpointRepository(), contentRepository, FakeExerciseRepositoryForComplete()
        )

        useCase("s1", CheckpointKind.REVIEW, correctCount = 10, totalCount = 10, today = today, skippedUnitIds = listOf("u1"))

        assertTrue(contentRepository.markedComplete.isEmpty())
    }

    @Test
    fun `a passed placement checkpoint marks every skipped unit complete via placement`() = runTest {
        val contentRepository = FakeContentRepositoryForComplete()
        val exerciseRepository = FakeExerciseRepositoryForComplete()
        val useCase = CompleteCheckpointUseCase(FakeCheckpointRepository(), contentRepository, exerciseRepository)

        useCase("s2", CheckpointKind.PLACEMENT, correctCount = 10, totalCount = 10, today = today, skippedUnitIds = listOf("u1", "u2"))

        assertEquals(listOf("u1" to UnitCompletionSource.PLACEMENT, "u2" to UnitCompletionSource.PLACEMENT), contentRepository.markedComplete)
    }

    @Test
    fun `a passed placement checkpoint seeds review_state only for exercises without one already`() = runTest {
        val contentRepository = FakeContentRepositoryForComplete()
        val exerciseRepository = FakeExerciseRepositoryForComplete(
            exercisesByUnit = mapOf("u1" to listOf(exercise("u1-ex-1", "u1"), exercise("u1-ex-2", "u1"))),
            existingReviewState = setOf("u1-ex-1")
        )
        val useCase = CompleteCheckpointUseCase(FakeCheckpointRepository(), contentRepository, exerciseRepository)

        useCase("s1", CheckpointKind.PLACEMENT, correctCount = 3, totalCount = 3, today = today, skippedUnitIds = listOf("u1"))

        assertEquals(1, exerciseRepository.seeded.size)
        val seeded = exerciseRepository.seeded.first()
        assertEquals("u1-ex-2", seeded.exerciseId)
        assertEquals(2.5, seeded.easeFactor, 0.0001)
        assertEquals(1, seeded.intervalDays)
        assertEquals(1, seeded.repetitions)
        assertEquals(today.plusDays(1), seeded.dueDate)
    }

    @Test
    fun `a failed placement checkpoint marks nothing complete and seeds nothing`() = runTest {
        val contentRepository = FakeContentRepositoryForComplete()
        val exerciseRepository = FakeExerciseRepositoryForComplete(
            exercisesByUnit = mapOf("u1" to listOf(exercise("u1-ex-1", "u1")))
        )
        val useCase = CompleteCheckpointUseCase(FakeCheckpointRepository(), contentRepository, exerciseRepository)

        val result = useCase("s1", CheckpointKind.PLACEMENT, correctCount = 1, totalCount = 10, today = today, skippedUnitIds = listOf("u1"))

        assertFalse(result.passed)
        assertTrue(contentRepository.markedComplete.isEmpty())
        assertTrue(exerciseRepository.seeded.isEmpty())
    }

    @Test
    fun `computeCheckpointResult is pure and matches the boundary threshold`() {
        assertEquals(CheckpointResult(68, true), computeCheckpointResult(correctCount = 17, totalCount = 25))
        assertEquals(CheckpointResult(0, false), computeCheckpointResult(correctCount = 0, totalCount = 0))
    }
}
