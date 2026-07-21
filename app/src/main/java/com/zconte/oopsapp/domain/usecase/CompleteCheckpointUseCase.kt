package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate
import javax.inject.Inject

private const val PASS_THRESHOLD_PCT = 68

class CompleteCheckpointUseCase @Inject constructor(
    private val checkpointRepository: CheckpointRepository
) {
    suspend operator fun invoke(
        sectionId: String,
        kind: String,
        correctCount: Int,
        totalCount: Int,
        today: LocalDate
    ): CheckpointResult {
        val scorePct = if (totalCount == 0) 0 else (correctCount * 100) / totalCount
        val passed = scorePct >= PASS_THRESHOLD_PCT
        checkpointRepository.recordAttempt(sectionId, kind, scorePct, passed, today)
        return CheckpointResult(scorePct, passed)
    }
}
