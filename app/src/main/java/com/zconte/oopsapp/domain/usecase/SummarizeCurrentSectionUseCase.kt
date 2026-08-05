package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.SectionPath

data class HomeSectionSummary(
    val currentSection: SectionPath?,
    val isCheckpointPending: Boolean
)

/**
 * Picks which section Home's "TU RUTA" card should describe, and whether that section is
 * waiting on its checkpoint. A section is "current" if it is NOT both fully complete AND
 * checkpoint-satisfied: `!(completed && checkpointSatisfied)`. Neither flag alone is enough —
 * a section whose units are all done but whose checkpoint isn't approved must stay current
 * (checkpoint pending), and a section whose checkpoint was approved earlier but which later
 * had new, unplayed units added to it (a permanently-satisfied checkpoint no longer implies
 * `completed`) must also stay current, so the card doesn't jump ahead to the next section
 * while this one still has real unplayed content.
 */
fun summarizeCurrentSection(sections: List<SectionPath>): HomeSectionSummary {
    val currentSection = sections.firstOrNull { !(it.completed && it.checkpointSatisfied) } ?: sections.lastOrNull()
    val isCheckpointPending = currentSection?.let { it.completed && !it.checkpointSatisfied } ?: false
    return HomeSectionSummary(currentSection, isCheckpointPending)
}
