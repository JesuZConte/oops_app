package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

/**
 * Resolves the section each answered exercise belongs to (via its unit) and hands off to the
 * pure [computeCheckpointSectionBreakdown]. Kept separate from that pure function so the
 * repository lookups it needs stay out of `CheckpointViewModel` and out of the pure-function
 * unit tests Plan 1 already wrote for the tally logic itself.
 */
class GetCheckpointResultBreakdownUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(results: List<Pair<Exercise, Boolean>>): List<CheckpointSectionBreakdown> {
        val sections = contentRepository.getSections()
        val sectionsById = sections.associateBy { it.id }
        val unitsById = sections.flatMap { contentRepository.getUnitsBySection(it.id) }.associateBy { it.id }
        return computeCheckpointSectionBreakdown(results, unitsById, sectionsById)
    }
}
