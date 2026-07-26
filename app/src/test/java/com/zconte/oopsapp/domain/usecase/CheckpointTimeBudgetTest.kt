package com.zconte.oopsapp.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckpointTimeBudgetTest {

    @Test
    fun `10 questions get an 18 minute budget`() {
        assertEquals(18 * 60, computeCheckpointTimeBudgetSeconds(questionCount = 10))
    }

    @Test
    fun `20 questions get a 36 minute budget`() {
        assertEquals(36 * 60, computeCheckpointTimeBudgetSeconds(questionCount = 20))
    }

    @Test
    fun `8 questions round to the nearest minute`() {
        // 8 * 1.8 = 14.4 minutes -> rounds to 14.
        assertEquals(14 * 60, computeCheckpointTimeBudgetSeconds(questionCount = 8))
    }
}
