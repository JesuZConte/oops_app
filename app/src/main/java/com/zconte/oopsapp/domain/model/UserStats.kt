package com.zconte.oopsapp.domain.model

import java.time.LocalDate

data class UserStats(
    val streak: Int,
    val xp: Int,
    val lastStudyDate: LocalDate?
)