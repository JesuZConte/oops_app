package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int
)
