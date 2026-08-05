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
            // An approved checkpoint attempt is a permanent record: once earned, it stays
            // satisfied even if later content-authoring adds new, not-yet-played units to
            // this section (which would otherwise flip sectionComplete back to false and
            // cascade-lock every downstream section's unfinished units). Full placement
            // completion is NOT a permanent record in the same way — it reflects today's
            // unit state, so it still requires sectionComplete.
            val checkpointSatisfied = checkpointRepository.hasApprovedAttempt(section.id, CheckpointKind.REVIEW) ||
                (sectionComplete && unitProgress.all { it.completedVia == UnitCompletionSource.PLACEMENT })
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
