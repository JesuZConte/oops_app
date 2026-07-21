package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_progress")
data class UnitProgressEntity(
    @PrimaryKey val unitId: String,
    val completed: Boolean,
    val completedAt: Long?
)
