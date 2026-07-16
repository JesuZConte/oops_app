package com.zconte.oopsapp.domain.srs

import com.zconte.oopsapp.domain.model.ReviewState
import java.time.LocalDate

object SchedulerSm2 {
    fun review(state: ReviewState, quality: Int, today: LocalDate): ReviewState {
        if (quality < 3) {
            return state.copy(
                repetitions = 0,
                intervalDays = 1,
                dueDate = today.plusDays(1)
            )
        }
        val newEase = (
            state.easeFactor +
                (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
            ).coerceAtLeast(1.3)
        val reps = state.repetitions + 1
        val interval = when (reps) {
            1 -> 1
            2 -> 6
            else -> Math.round(state.intervalDays * newEase).toInt()
        }
        return state.copy(
            easeFactor = newEase,
            repetitions = reps,
            intervalDays = interval,
            dueDate = today.plusDays(interval.toLong())
        )
    }
}