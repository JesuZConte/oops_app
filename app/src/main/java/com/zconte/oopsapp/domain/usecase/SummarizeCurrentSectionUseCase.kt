package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.SectionPath

data class HomeSectionSummary(
    val currentSection: SectionPath?,
    val isCheckpointPending: Boolean
)

/**
 * Picks which section Home's "TU RUTA" card should describe, and whether that section is
 * waiting on its checkpoint. `!checkpointSatisfied` (not `!completed`) is what makes this
 * correct: a section whose units are all done but whose checkpoint isn't approved must stay
 * "current" rather than have the card jump ahead to the next, still-locked section.
 */
fun summarizeCurrentSection(sections: List<SectionPath>): HomeSectionSummary {
    val currentSection = sections.firstOrNull { !it.checkpointSatisfied } ?: sections.lastOrNull()
    val isCheckpointPending = currentSection?.let { it.completed && !it.checkpointSatisfied } ?: false
    return HomeSectionSummary(currentSection, isCheckpointPending)
}
