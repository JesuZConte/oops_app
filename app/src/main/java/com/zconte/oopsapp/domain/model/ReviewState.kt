package com.zconte.oopsapp.domain.model

import java.time.LocalDate

data class ReviewState(
    val exerciseId: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: LocalDate
)