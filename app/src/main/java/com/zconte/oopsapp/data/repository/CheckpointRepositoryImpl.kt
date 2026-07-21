package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.CheckpointAttemptDao
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate
import javax.inject.Inject

class CheckpointRepositoryImpl @Inject constructor(
    private val checkpointAttemptDao: CheckpointAttemptDao
) : CheckpointRepository {
    override suspend fun recordAttempt(sectionId: String, kind: String, scorePct: Int, passed: Boolean, takenAt: LocalDate) {
        checkpointAttemptDao.insert(
            CheckpointAttemptEntity(
                sectionId = sectionId,
                kind = kind,
                scorePct = scorePct,
                passed = passed,
                takenAt = takenAt.toEpochDay()
            )
        )
    }
}
