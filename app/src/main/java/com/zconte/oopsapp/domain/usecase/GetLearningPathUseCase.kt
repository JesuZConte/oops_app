package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.model.UnitProgress
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

class GetLearningPathUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(): List<SectionPath> {
        val sections = contentRepository.getSections().sortedBy { it.orderIndex }
        val completedUnits = contentRepository.getCompletedUnits().associateBy { it.unitId }

        var previousSectionComplete = true
        return sections.map { section ->
            val units = contentRepository.getUnitsBySection(section.id).sortedBy { it.orderIndex }
            val sectionUnlocked = previousSectionComplete

            var previousUnitComplete = true
            val unitProgress = units.map { unit ->
                val record = completedUnits[unit.id]
                val completed = record != null
                val unlocked = sectionUnlocked && previousUnitComplete
                previousUnitComplete = completed
                UnitProgress(unit, completed, unlocked, record?.completedVia ?: UnitCompletionSource.PLAYED)
            }

            val sectionComplete = units.isNotEmpty() && units.all { it.id in completedUnits }
            previousSectionComplete = sectionComplete

            SectionPath(section, sectionUnlocked, unitProgress, sectionComplete)
        }
    }
}
