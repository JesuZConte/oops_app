package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

class CompleteCheckpointUseCaseTest {

    private val today = LocalDate.of(2026, 7, 20)

    @Test
    fun `passes at exactly the 68 percent threshold`() = runTest {
        val repository = FakeCheckpointRepository()
        val useCase = CompleteCheckpointUseCase(repository)

        // 68% of 25 = 17 correct, rounds down to exactly 68 -- boundary case.
        val result = useCase("s1", CheckpointKind.REVIEW, correctCount = 17, totalCount = 25, today = today)

        assertEquals(68, result.scorePct)
        assertTrue(result.passed)
        assertEquals(1, repository.recorded.size)
        assertTrue(repository.recorded.first().passed)
    }

    @Test
    fun `fails below the 68 percent threshold`() = runTest {
        val repository = FakeCheckpointRepository()
        val useCase = CompleteCheckpointUseCase(repository)

        val result = useCase("s1", CheckpointKind.REVIEW, correctCount = 6, totalCount = 12, today = today)

        assertEquals(50, result.scorePct)
        assertFalse(result.passed)
    }
}
