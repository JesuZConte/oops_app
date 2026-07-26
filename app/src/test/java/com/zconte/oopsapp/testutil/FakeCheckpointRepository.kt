package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.model.FailedCheckpointAttempt
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate

class FakeCheckpointRepository : CheckpointRepository {

    data class RecordedAttempt(
        val sectionId: String,
        val kind: String,
        val scorePct: Int,
        val passed: Boolean,
        val takenAt: LocalDate,
        val failedExerciseIds: List<String>
    )

    val recordedAttempts = mutableListOf<RecordedAttempt>()

    override suspend fun recordAttempt(
        sectionId: String,
        kind: String,
        scorePct: Int,
        passed: Boolean,
        takenAt: LocalDate,
        failedExerciseIds: List<String>
    ) {
        recordedAttempts.add(RecordedAttempt(sectionId, kind, scorePct, passed, takenAt, failedExerciseIds))
    }

    override suspend fun hasApprovedAttempt(sectionId: String, kind: String): Boolean =
        recordedAttempts.any { it.sectionId == sectionId && it.kind == kind && it.passed }

    override suspend fun getLatestFailedAttempt(sectionId: String, kind: String): FailedCheckpointAttempt? {
        // Mirrors CheckpointAttemptDao.getLatestAttempt's "ORDER BY id DESC LIMIT 1": the
        // most-recently-inserted matching attempt wins ties on takenAt, not the earliest-recorded.
        val latest = recordedAttempts
            .lastOrNull { it.sectionId == sectionId && it.kind == kind }
            ?: return null
        if (latest.passed) return null
        return FailedCheckpointAttempt(latest.takenAt, latest.failedExerciseIds)
    }
}
