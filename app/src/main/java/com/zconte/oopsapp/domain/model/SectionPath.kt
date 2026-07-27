package com.zconte.oopsapp.domain.model

enum class CheckpointStatus { PENDING, RETRY_LOCKED, RETRY_AVAILABLE, SATISFIED }

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
    val completed: Boolean,
    val checkpointSatisfied: Boolean,
    val checkpointStatus: CheckpointStatus
)
