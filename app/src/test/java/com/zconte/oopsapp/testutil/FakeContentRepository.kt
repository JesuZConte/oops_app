package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.domain.repository.ContentRepository
import java.time.LocalDate

class FakeContentRepository(
    private val sections: List<Section> = emptyList(),
    private val unitsBySection: Map<String, List<LearningUnit>> = emptyMap(),
    initialCompletedUnits: List<CompletedUnit> = emptyList(),
    private val unitSummaries: Map<String, UnitSummary> = emptyMap()
) : ContentRepository {

    val completedUnits = initialCompletedUnits.toMutableList()

    override suspend fun getSections(): List<Section> = sections

    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()

    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits

    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {
        completedUnits.removeAll { it.unitId == unitId }
        completedUnits.add(CompletedUnit(unitId, via))
    }

    override suspend fun getUnitSummary(unitId: String): UnitSummary? = unitSummaries[unitId]
}
