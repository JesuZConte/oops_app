package com.zconte.oopsapp.domain.model

data class UnitProgress(
    val unit: LearningUnit,
    val completed: Boolean,
    val unlocked: Boolean,
    val completedVia: String = UnitCompletionSource.PLAYED
)

data class SectionPath(
    val section: Section,
    val unlocked: Boolean,
    val units: List<UnitProgress>,
    val completed: Boolean
)
