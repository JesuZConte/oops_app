package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointStatus
import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.model.UnitProgress
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

class GetLearningPathUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
    private val checkpointRepository: CheckpointRepository,
    private val isCheckpointRetryUnlockedUseCase: IsCheckpointRetryUnlockedUseCase
) {
    suspend operator fun invoke(): List<SectionPath> {
        val sections = contentRepository.getSections().sortedBy { it.orderIndex }
        val completedUnits = contentRepository.getCompletedUnits().associateBy { it.unitId }

        var previousSectionFullyDone = true
        return sections.map { section ->
            val units = contentRepository.getUnitsBySection(section.id).sortedBy { it.orderIndex }
            val sectionUnlocked = previousSectionFullyDone

            var previousUnitComplete = true
            val unitProgress = units.map { unit ->
                val record = completedUnits[unit.id]
                val completed = record != null
                val unlocked = sectionUnlocked && previousUnitComplete
                previousUnitComplete = completed
                UnitProgress(unit, completed, unlocked, record?.completedVia ?: UnitCompletionSource.PLAYED)
            }

            val sectionComplete = units.isNotEmpty() && units.all { it.id in completedUnits }
            val checkpointSatisfied = sectionComplete && (
                checkpointRepository.hasApprovedAttempt(section.id, CheckpointKind.REVIEW) ||
                    unitProgress.all { it.completedVia == UnitCompletionSource.PLACEMENT }
                )
            val checkpointStatus = computeCheckpointStatus(section.id, sectionComplete, checkpointSatisfied)
            previousSectionFullyDone = checkpointSatisfied

            SectionPath(section, sectionUnlocked, unitProgress, sectionComplete, checkpointSatisfied, checkpointStatus)
        }
    }

    private suspend fun computeCheckpointStatus(
        sectionId: String,
        sectionComplete: Boolean,
        checkpointSatisfied: Boolean
    ): CheckpointStatus = when {
        checkpointSatisfied -> CheckpointStatus.SATISFIED
        !sectionComplete -> CheckpointStatus.PENDING
        checkpointRepository.getLatestFailedAttempt(sectionId, CheckpointKind.REVIEW) == null -> CheckpointStatus.PENDING
        isCheckpointRetryUnlockedUseCase(sectionId, CheckpointKind.REVIEW) -> CheckpointStatus.RETRY_AVAILABLE
        else -> CheckpointStatus.RETRY_LOCKED
    }
}
