package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkpoint_attempts")
data class CheckpointAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: String,
    val kind: String,
    val scorePct: Int,
    val passed: Boolean,
    val takenAt: Long
)
