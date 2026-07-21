package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_meta")
data class ContentMetaEntity(
    @PrimaryKey val configKey: String,
    val value: String
)
