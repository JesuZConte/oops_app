package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointStatus
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.model.UnitProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummarizeCurrentSectionUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)

    private fun sectionPath(
        id: String,
        order: Int,
        completed: Boolean,
        checkpointSatisfied: Boolean,
        status: CheckpointStatus
    ) = SectionPath(
        section = section(id, order),
        unlocked = true,
        units = listOf(UnitProgress(unit("$id-u1", id, 1), completed, true, UnitCompletionSource.PLAYED)),
        completed = completed,
        checkpointSatisfied = checkpointSatisfied,
        checkpointStatus = status
    )

    @Test
    fun `an in-progress section is current, and its checkpoint is not pending`() {
        val sections = listOf(
            sectionPath("s1", 1, completed = false, checkpointSatisfied = false, status = CheckpointStatus.PENDING)
        )

        val summary = summarizeCurrentSection(sections)

        assertEquals("s1", summary.currentSection?.section?.id)
        assertFalse(summary.isCheckpointPending)
    }

    @Test
    fun `a section done with units but not checkpoint-satisfied stays current, with its checkpoint pending`() {
        val sections = listOf(
            sectionPath("s1", 1, completed = true, checkpointSatisfied = false, status = CheckpointStatus.PENDING),
            sectionPath("s2", 2, completed = false, checkpointSatisfied = false, status = CheckpointStatus.PENDING)
        )

        val summary = summarizeCurrentSection(sections)

        assertEquals("s1", summary.currentSection?.section?.id)
        assertTrue(summary.isCheckpointPending)
    }

    @Test
    fun `once satisfied, the next section becomes current`() {
        val sections = listOf(
            sectionPath("s1", 1, completed = true, checkpointSatisfied = true, status = CheckpointStatus.SATISFIED),
            sectionPath("s2", 2, completed = false, checkpointSatisfied = false, status = CheckpointStatus.PENDING)
        )

        val summary = summarizeCurrentSection(sections)

        assertEquals("s2", summary.currentSection?.section?.id)
        assertFalse(summary.isCheckpointPending)
    }

    @Test
    fun `when every section is satisfied, the last one is current with nothing pending`() {
        val sections = listOf(
            sectionPath("s1", 1, completed = true, checkpointSatisfied = true, status = CheckpointStatus.SATISFIED)
        )

        val summary = summarizeCurrentSection(sections)

        assertEquals("s1", summary.currentSection?.section?.id)
        assertFalse(summary.isCheckpointPending)
    }

    @Test
    fun `a section with new unplayed units stays current even if its checkpoint was already approved`() {
        val sections = listOf(
            sectionPath("s1", 1, completed = false, checkpointSatisfied = true, status = CheckpointStatus.SATISFIED),
            sectionPath("s2", 2, completed = true, checkpointSatisfied = true, status = CheckpointStatus.SATISFIED)
        )

        val summary = summarizeCurrentSection(sections)

        assertEquals("s1", summary.currentSection?.section?.id)
        assertFalse(summary.isCheckpointPending)
    }

    @Test
    fun `an empty roadmap has no current section`() {
        val summary = summarizeCurrentSection(emptyList())

        assertEquals(null, summary.currentSection)
        assertFalse(summary.isCheckpointPending)
    }
}
