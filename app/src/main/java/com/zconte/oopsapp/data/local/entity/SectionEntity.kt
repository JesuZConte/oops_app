package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val orderIndex: Int,
    val examVersion: String
)
