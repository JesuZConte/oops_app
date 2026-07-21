package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.dao.UnitProgressDao
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import java.time.LocalDate
import javax.inject.Inject

class ContentRepositoryImpl @Inject constructor(
    private val sectionDao: SectionDao,
    private val unitDao: UnitDao,
    private val unitProgressDao: UnitProgressDao
) : ContentRepository {

    override suspend fun getSections(): List<Section> =
        sectionDao.getAll().map { it.toDomain() }

    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> =
        unitDao.getBySection(sectionId).map { it.toDomain() }

    override suspend fun getCompletedUnitIds(): List<String> =
        unitProgressDao.getCompletedUnitIds()

    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate) {
        unitProgressDao.upsert(
            UnitProgressEntity(unitId = unitId, completed = true, completedAt = completedAt.toEpochDay())
        )
    }
}

private fun SectionEntity.toDomain() = Section(id, name, orderIndex, examVersion)

private fun UnitEntity.toDomain() = LearningUnit(id, sectionId, name, certObjective, orderIndex)
