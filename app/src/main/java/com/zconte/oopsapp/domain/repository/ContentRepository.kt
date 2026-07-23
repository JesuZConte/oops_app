package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import java.time.LocalDate

interface ContentRepository {
    suspend fun getSections(): List<Section>
    suspend fun getUnitsBySection(sectionId: String): List<LearningUnit>
    suspend fun getCompletedUnits(): List<CompletedUnit>
    suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String)
}
