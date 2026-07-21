package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.ContentMetaEntity

@Dao
interface ContentMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: ContentMetaEntity)

    @Query("SELECT * FROM content_meta WHERE configKey = :configKey")
    suspend fun get(configKey: String): ContentMetaEntity?
}
