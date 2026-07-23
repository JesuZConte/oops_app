package com.zconte.oopsapp.di

import android.content.Context
import androidx.room.Room
import com.zconte.oopsapp.data.local.AppDatabase
import com.zconte.oopsapp.data.local.MIGRATION_1_2
import com.zconte.oopsapp.data.local.MIGRATION_2_3
import com.zconte.oopsapp.data.local.dao.CheckpointAttemptDao
import com.zconte.oopsapp.data.local.dao.ContentMetaDao
import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.dao.UnitProgressDao
import com.zconte.oopsapp.data.local.dao.UserStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "oops.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideSectionDao(db: AppDatabase): SectionDao = db.sectionDao()

    @Provides
    fun provideUnitDao(db: AppDatabase): UnitDao = db.unitDao()

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideReviewStateDao(db: AppDatabase): ReviewStateDao = db.reviewStateDao()

    @Provides
    fun provideUserStatsDao(db: AppDatabase): UserStatsDao = db.userStatsDao()

    @Provides
    fun provideUnitProgressDao(db: AppDatabase): UnitProgressDao = db.unitProgressDao()

    @Provides
    fun provideCheckpointAttemptDao(db: AppDatabase): CheckpointAttemptDao = db.checkpointAttemptDao()

    @Provides
    fun provideContentMetaDao(db: AppDatabase): ContentMetaDao = db.contentMetaDao()
}
