package com.zconte.oopsapp.domain.model

import java.time.LocalDate

data class FailedCheckpointAttempt(
    val takenAt: LocalDate,
    val failedExerciseIds: List<String>
)
