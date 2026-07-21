package com.zconte.oopsapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zconte.oopsapp.data.local.dao.CheckpointAttemptDao
import com.zconte.oopsapp.data.local.dao.ContentMetaDao
import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.dao.UnitProgressDao
import com.zconte.oopsapp.data.local.dao.UserStatsDao
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity
import com.zconte.oopsapp.data.local.entity.ContentMetaEntity
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity
import com.zconte.oopsapp.data.local.entity.UserStatsEntity

@Database(
    entities = [
        SectionEntity::class, UnitEntity::class, ExerciseEntity::class,
        ReviewStateEntity::class, UserStatsEntity::class,
        UnitProgressEntity::class, CheckpointAttemptEntity::class, ContentMetaEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sectionDao(): SectionDao
    abstract fun unitDao(): UnitDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun reviewStateDao(): ReviewStateDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun unitProgressDao(): UnitProgressDao
    abstract fun checkpointAttemptDao(): CheckpointAttemptDao
    abstract fun contentMetaDao(): ContentMetaDao
}
