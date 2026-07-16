package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_state")
data class ReviewStateEntity(
    @PrimaryKey val exerciseId: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: Long
)