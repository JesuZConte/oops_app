package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.CheckpointAttemptDao
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptFailureEntity
import com.zconte.oopsapp.domain.model.FailedCheckpointAttempt
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate
import javax.inject.Inject

class CheckpointRepositoryImpl @Inject constructor(
    private val checkpointAttemptDao: CheckpointAttemptDao
) : CheckpointRepository {

    override suspend fun recordAttempt(
        sectionId: String,
        kind: String,
        scorePct: Int,
        passed: Boolean,
        takenAt: LocalDate,
        failedExerciseIds: List<String>
    ) {
        val attemptId = checkpointAttemptDao.insert(
            CheckpointAttemptEntity(
                sectionId = sectionId,
                kind = kind,
                scorePct = scorePct,
                passed = passed,
                takenAt = takenAt.toEpochDay()
            )
        )
        if (failedExerciseIds.isNotEmpty()) {
            checkpointAttemptDao.insertFailures(
                failedExerciseIds.map { CheckpointAttemptFailureEntity(attemptId = attemptId, exerciseId = it) }
            )
        }
    }

    override suspend fun hasApprovedAttempt(sectionId: String, kind: String): Boolean =
        checkpointAttemptDao.hasApprovedAttempt(sectionId, kind)

    override suspend fun getLatestFailedAttempt(sectionId: String, kind: String): FailedCheckpointAttempt? {
        val latest = checkpointAttemptDao.getLatestAttempt(sectionId, kind) ?: return null
        if (latest.passed) return null
        val failedIds = checkpointAttemptDao.getFailedExerciseIds(latest.id)
        return FailedCheckpointAttempt(takenAt = LocalDate.ofEpochDay(latest.takenAt), failedExerciseIds = failedIds)
    }
}
