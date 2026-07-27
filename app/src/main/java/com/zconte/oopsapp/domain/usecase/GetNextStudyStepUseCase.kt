package com.zconte.oopsapp.domain.usecase

import java.time.LocalDate
import javax.inject.Inject

sealed class NextStudyStep {
    object DailySession : NextStudyStep()
    data class Checkpoint(val sectionId: String) : NextStudyStep()
    object NothingPending : NextStudyStep()
}

/**
 * Decides where the "ESTUDIAR HOY" button should take the player: their normal daily session,
 * a pending section checkpoint (rendible or retry-locked -- CheckpointViewModel tells those
 * apart), or nowhere, if there is truly nothing to do. Exists so that button never opens an
 * empty session that silently bounces back.
 */
class GetNextStudyStepUseCase @Inject constructor(
    private val getTodaySessionUseCase: GetTodaySessionUseCase,
    private val getLearningPathUseCase: GetLearningPathUseCase
) {
    suspend operator fun invoke(today: LocalDate): NextStudyStep {
        if (getTodaySessionUseCase(today).isNotEmpty()) return NextStudyStep.DailySession

        val pendingSection = getLearningPathUseCase().firstOrNull { it.completed && !it.checkpointSatisfied }
        return pendingSection?.let { NextStudyStep.Checkpoint(it.section.id) } ?: NextStudyStep.NothingPending
    }
}
