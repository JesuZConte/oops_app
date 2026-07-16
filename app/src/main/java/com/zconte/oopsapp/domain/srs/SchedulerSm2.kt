package com.zconte.oopsapp.domain.srs

import com.zconte.oopsapp.domain.model.ReviewState
import java.time.LocalDate

object SchedulerSm2 {
    fun review(state: ReviewState, quality: Int, today: LocalDate): ReviewState {
        // Ease factor is recalculated on every review, pass or fail (canonical SM-2).
        val newEase = (
            state.easeFactor +
                (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
            ).coerceAtLeast(1.3)

        if (quality < 3) {
            return state.copy(
                easeFactor = newEase,
                repetitions = 0,
                intervalDays = 1,
                dueDate = today.plusDays(1)
            )
        }
        val reps = state.repetitions + 1
        val interval = when (reps) {
            1 -> 1
            2 -> 6
            // Uses the PRE-update ease factor for this review, not newEase.
            else -> Math.round(state.intervalDays * state.easeFactor).toInt()
        }
        return state.copy(
            easeFactor = newEase,
            repetitions = reps,
            intervalDays = interval,
            dueDate = today.plusDays(interval.toLong())
        )
    }
}