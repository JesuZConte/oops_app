package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.content.ContentPack
import com.zconte.oopsapp.data.content.ContentPackRegistry
import com.zconte.oopsapp.data.content.UnitPack
import com.zconte.oopsapp.data.content.UnitSummaryPack
import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.dao.UnitProgressDao
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity
import com.zconte.oopsapp.testutil.FakeContentLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class NoOpSectionDao : SectionDao {
    override suspend fun insertAll(sections: List<SectionEntity>) {}
    override suspend fun getAll(): List<SectionEntity> = emptyList()
    override suspend fun clearAll() {}
}

private class NoOpUnitDao : UnitDao {
    override suspend fun insertAll(units: List<UnitEntity>) {}
    override suspend fun getBySection(sectionId: String): List<UnitEntity> = emptyList()
    override suspend fun getAll(): List<UnitEntity> = emptyList()
    override suspend fun clearAll() {}
}

private class NoOpUnitProgressDao : UnitProgressDao {
    override suspend fun upsert(progress: UnitProgressEntity) {}
    override suspend fun getByUnit(unitId: String): UnitProgressEntity? = null
    override suspend fun getCompleted(): List<UnitProgressEntity> = emptyList()
}

class ContentRepositoryImplTest {

    private val paths = ContentPackRegistry.assetPaths

    private fun emptyPack(name: String) = ContentPack(
        sectionId = name, name = name, orderIndex = 0, examVersion = "core", units = emptyList()
    )

    private fun packWithUnit(name: String, unitId: String, unitName: String, summary: UnitSummaryPack?) = ContentPack(
        sectionId = name,
        name = name,
        orderIndex = 0,
        examVersion = "core",
        units = listOf(
            UnitPack(
                unitId = unitId,
                name = unitName,
                certObjective = "objective",
                orderIndex = 0,
                summary = summary,
                exercises = emptyList()
            )
        )
    )

    private fun createRepository(packsByPath: Map<String, ContentPack>) = ContentRepositoryImpl(
        sectionDao = NoOpSectionDao(),
        unitDao = NoOpUnitDao(),
        unitProgressDao = NoOpUnitProgressDao(),
        contentLoader = FakeContentLoader(packsByPath)
    )

    @Test
    fun `finds the summary in the first pack scanned`() = runTest {
        val packsByPath = paths.indices.associate { index ->
            paths[index] to if (index == 0) {
                packWithUnit("a", "target-unit", "Unidad objetivo", UnitSummaryPack("Texto A", "codigo A"))
            } else {
                emptyPack("p$index")
            }
        }
        val repository = createRepository(packsByPath)

        val summary = repository.getUnitSummary("target-unit")

        assertEquals("Unidad objetivo", summary?.unitName)
        assertEquals("Texto A", summary?.text)
        assertEquals("codigo A", summary?.code)
    }

    @Test
    fun `keeps scanning until it finds the unit in a later pack`() = runTest {
        val lastIndex = paths.lastIndex
        val packsByPath = paths.indices.associate { index ->
            paths[index] to if (index == lastIndex) {
                packWithUnit("z", "target-unit", "Unidad objetivo", UnitSummaryPack("Texto Z", null))
            } else {
                emptyPack("p$index")
            }
        }
        val repository = createRepository(packsByPath)

        val summary = repository.getUnitSummary("target-unit")

        assertEquals("Texto Z", summary?.text)
    }

    @Test
    fun `a unit with no summary field returns null`() = runTest {
        val packsByPath = paths.indices.associate { index ->
            paths[index] to if (index == 0) {
                packWithUnit("a", "target-unit", "Unidad objetivo", null)
            } else {
                emptyPack("p$index")
            }
        }
        val repository = createRepository(packsByPath)

        val summary = repository.getUnitSummary("target-unit")

        assertNull(summary)
    }

    @Test
    fun `an unknown unit id returns null`() = runTest {
        val packsByPath = paths.indices.associate { index -> paths[index] to emptyPack("p$index") }
        val repository = createRepository(packsByPath)

        val summary = repository.getUnitSummary("unknown-unit")

        assertNull(summary)
    }
}
