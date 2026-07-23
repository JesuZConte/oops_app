package com.zconte.oopsapp.domain.model

data class SkippedUnitsResult(
    val targetUnit: LearningUnit?,
    val skippedUnits: List<LearningUnit>
)
