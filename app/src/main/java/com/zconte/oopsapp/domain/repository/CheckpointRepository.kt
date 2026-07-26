package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.FailedCheckpointAttempt
import java.time.LocalDate

interface CheckpointRepository {
    suspend fun recordAttempt(
        sectionId: String,
        kind: String,
        scorePct: Int,
        passed: Boolean,
        takenAt: LocalDate,
        failedExerciseIds: List<String> = emptyList()
    )

    suspend fun hasApprovedAttempt(sectionId: String, kind: String): Boolean

    suspend fun getLatestFailedAttempt(sectionId: String, kind: String): FailedCheckpointAttempt?
}
