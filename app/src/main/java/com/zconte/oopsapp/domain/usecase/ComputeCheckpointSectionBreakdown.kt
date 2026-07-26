package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section

data class CheckpointSectionBreakdown(
    val section: Section,
    val correct: Int,
    val total: Int
)

/**
 * Per-section accuracy breakdown for a checkpoint result screen -- so a mandatory gate isn't a
 * black box. Reuses data the caller already has (which unit each answered exercise belongs to,
 * which section each unit belongs to) rather than requiring new persistence.
 */
fun computeCheckpointSectionBreakdown(
    results: List<Pair<Exercise, Boolean>>,
    unitsById: Map<String, LearningUnit>,
    sectionsById: Map<String, Section>
): List<CheckpointSectionBreakdown> =
    results
        .mapNotNull { (exercise, correct) ->
            val unit = unitsById[exercise.unitId] ?: return@mapNotNull null
            val section = sectionsById[unit.sectionId] ?: return@mapNotNull null
            section to correct
        }
        .groupBy({ it.first }, { it.second })
        .map { (section, corrects) ->
            CheckpointSectionBreakdown(section = section, correct = corrects.count { it }, total = corrects.size)
        }
        .sortedBy { it.section.orderIndex }
