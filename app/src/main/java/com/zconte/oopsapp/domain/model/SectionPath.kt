package com.zconte.oopsapp.domain.model

data class UnitProgress(
    val unit: LearningUnit,
    val completed: Boolean,
    val unlocked: Boolean
)

data class SectionPath(
    val section: Section,
    val unlocked: Boolean,
    val units: List<UnitProgress>,
    val completed: Boolean
)
