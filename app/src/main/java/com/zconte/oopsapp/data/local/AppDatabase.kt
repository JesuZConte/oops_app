package com.zconte.oopsapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.dao.TopicDao
import com.zconte.oopsapp.data.local.dao.UserStatsDao
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import com.zconte.oopsapp.data.local.entity.TopicEntity
import com.zconte.oopsapp.data.local.entity.UserStatsEntity

@Database(
    entities = [TopicEntity::class, ExerciseEntity::class, ReviewStateEntity::class, UserStatsEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun reviewStateDao(): ReviewStateDao
    abstract fun userStatsDao(): UserStatsDao
}