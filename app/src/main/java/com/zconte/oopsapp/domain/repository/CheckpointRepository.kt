package com.zconte.oopsapp.domain.repository

import java.time.LocalDate

interface CheckpointRepository {
    suspend fun recordAttempt(sectionId: String, kind: String, scorePct: Int, passed: Boolean, takenAt: LocalDate)
}
