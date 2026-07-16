package com.zconte.oopsapp.domain.srs

import com.zconte.oopsapp.domain.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SchedulerSm2Test {

    private val today: LocalDate = LocalDate.of(2026, 7, 15)

    @Test
    fun `first review with passing quality sets interval to 1 day`() {
        val state = ReviewState(
            exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 0,
            repetitions = 0, dueDate = today
        )

        val result = SchedulerSm2.review(state, quality = 4, today = today)

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(today.plusDays(1), result.dueDate)
        assertEquals(2.5, result.easeFactor, 0.0001)
    }

    @Test
    fun `second review with passing quality sets interval to 6 days`() {
        val afterFirst = ReviewState(
            exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1,
            repetitions = 1, dueDate = today
        )

        val result = SchedulerSm2.review(afterFirst, quality = 4, today = today)

        assertEquals(2, result.repetitions)
        assertEquals(6, result.intervalDays)
        assertEquals(today.plusDays(6), result.dueDate)
    }

    @Test
    fun `failing quality resets repetitions and reschedules for tomorrow`() {
        val afterSeveralReviews = ReviewState(
            exerciseId = "ex-1", easeFactor = 2.6, intervalDays = 16,
            repetitions = 3, dueDate = today
        )

        val result = SchedulerSm2.review(afterSeveralReviews, quality = 2, today = today)

        assertEquals(0, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(today.plusDays(1), result.dueDate)
        assertEquals(2.6, result.easeFactor, 0.0001)
    }

    @Test
    fun `third review grows interval using ease factor`() {
        val afterSecond = ReviewState(
            exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 6,
            repetitions = 2, dueDate = today
        )

        val result = SchedulerSm2.review(afterSecond, quality = 5, today = today)

        assertEquals(3, result.repetitions)
        assertEquals(2.6, result.easeFactor, 0.0001)
        assertEquals(16, result.intervalDays)
        assertEquals(today.plusDays(16), result.dueDate)
    }

    @Test
    fun `ease factor never drops below 1_3`() {
        val lowEase = ReviewState(
            exerciseId = "ex-1", easeFactor = 1.3, intervalDays = 6,
            repetitions = 2, dueDate = today
        )

        val result = SchedulerSm2.review(lowEase, quality = 3, today = today)

        assertEquals(1.3, result.easeFactor, 0.0001)
    }
}