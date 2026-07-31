package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.testutil.FakeContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetUnitSummaryUseCaseTest {

    @Test
    fun `returns the repository's summary for the given unit`() = runTest {
        val summary = UnitSummary(unitName = "Streams", text = "Texto", code = "codigo")
        val repository = FakeContentRepository(unitSummaries = mapOf("streams-creation" to summary))
        val useCase = GetUnitSummaryUseCase(repository)

        val result = useCase("streams-creation")

        assertEquals(summary, result)
    }

    @Test
    fun `returns null when the repository has no summary for that unit`() = runTest {
        val repository = FakeContentRepository()
        val useCase = GetUnitSummaryUseCase(repository)

        val result = useCase("unknown-unit")

        assertNull(result)
    }
}
