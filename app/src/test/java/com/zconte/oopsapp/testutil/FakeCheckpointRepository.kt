package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate

class FakeCheckpointRepository : CheckpointRepository {

    data class RecordedAttempt(
        val sectionId: String,
        val kind: String,
        val scorePct: Int,
        val passed: Boolean,
        val takenAt: LocalDate
    )

    val recordedAttempts = mutableListOf<RecordedAttempt>()

    override suspend fun recordAttempt(sectionId: String, kind: String, scorePct: Int, passed: Boolean, takenAt: LocalDate) {
        recordedAttempts.add(RecordedAttempt(sectionId, kind, scorePct, passed, takenAt))
    }
}
