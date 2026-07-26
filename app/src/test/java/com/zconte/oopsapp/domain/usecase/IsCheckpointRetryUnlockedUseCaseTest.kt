package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.testutil.FakeCheckpointRepository
import com.zconte.oopsapp.testutil.FakeExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IsCheckpointRetryUnlockedUseCaseTest {

    private val attemptDay = LocalDate.of(2026, 7, 26)

    @Test
    fun `unlocked when there is no attempt on record at all`() = runTest {
        val useCase = IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), FakeExerciseRepository())

        assertTrue(useCase("s1"))
    }

    @Test
    fun `unlocked when the latest attempt was passed`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 80, passed = true, takenAt = attemptDay, failedExerciseIds = emptyList()
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, FakeExerciseRepository())

        assertTrue(useCase("s1"))
    }

    @Test
    fun `unlocked when the latest failed attempt had no failed exercises`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay, failedExerciseIds = emptyList()
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, FakeExerciseRepository())

        assertTrue(useCase("s1"))
    }

    @Test
    fun `locked when a failed exercise has never been reviewed again`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay,
            failedExerciseIds = listOf("ex-1")
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, FakeExerciseRepository())

        assertFalse(useCase("s1"))
    }

    @Test
    fun `locked when a failed exercise was reviewed again on the SAME day as the failed attempt`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay,
            failedExerciseIds = listOf("ex-1")
        )
        // This is exactly what the failed checkpoint attempt itself just wrote via SubmitAnswerUseCase
        // -- same-day re-exposure must NOT count, or the gate would self-satisfy instantly.
        val exerciseRepository = FakeExerciseRepository()
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1, repetitions = 0,
                dueDate = attemptDay.plusDays(1), lastReviewedAt = attemptDay
            )
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository)

        assertFalse(useCase("s1"))
    }

    @Test
    fun `unlocked once every failed exercise was reviewed again on a later day`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay,
            failedExerciseIds = listOf("ex-1", "ex-2")
        )
        val exerciseRepository = FakeExerciseRepository()
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1, repetitions = 1,
                dueDate = attemptDay.plusDays(2), lastReviewedAt = attemptDay.plusDays(1)
            )
        )
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-2", easeFactor = 2.5, intervalDays = 1, repetitions = 1,
                dueDate = attemptDay.plusDays(2), lastReviewedAt = attemptDay.plusDays(1)
            )
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository)

        assertTrue(useCase("s1"))
    }

    @Test
    fun `locked when only some of the failed exercises were reviewed again`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay,
            failedExerciseIds = listOf("ex-1", "ex-2")
        )
        val exerciseRepository = FakeExerciseRepository()
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1, repetitions = 1,
                dueDate = attemptDay.plusDays(2), lastReviewedAt = attemptDay.plusDays(1)
            )
        )
        // ex-2 never reviewed again.
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository)

        assertFalse(useCase("s1"))
    }
}
