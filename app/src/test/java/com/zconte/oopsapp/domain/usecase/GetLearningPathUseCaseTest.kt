package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForPath(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnitIds: List<String>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnitIds(): List<String> = completedUnitIds
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate) {}
}

class GetLearningPathUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)

    @Test
    fun `first section and its first unit are always unlocked`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnitIds = emptyList()
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertTrue(path.first().unlocked)
        assertTrue(path.first().units[0].unlocked)
        assertFalse(path.first().units[1].unlocked)
    }

    @Test
    fun `a unit unlocks once the previous unit in the same section is completed`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnitIds = listOf("s1-u1")
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertTrue(path.first().units[1].unlocked)
    }

    @Test
    fun `a section unlocks once every unit of the previous section is completed`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnitIds = listOf("s1-u1")
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertTrue(path.first().completed)
        assertTrue(path[1].unlocked)
        assertEquals("s2", path[1].section.id)
    }

    @Test
    fun `a section stays locked while the previous section has incomplete units`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnitIds = listOf("s1-u1")
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertFalse(path[1].unlocked)
    }
}
