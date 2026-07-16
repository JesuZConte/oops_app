package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 0,
    val streak: Int,
    val xp: Int,
    val lastStudyDate: Long?
)