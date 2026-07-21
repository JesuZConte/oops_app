package com.zconte.oopsapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Content (sections/units/exercises) is fully re-derived from assets by ContentSeeder after
 * this migration runs, so this migration only drops and recreates the content tables in their
 * v2 shape. review_state and user_stats are the user's own data (SM-2 progress, streak/XP) and
 * are never touched here -- no DDL, no DML. There is no foreign key from review_state to
 * exercises, so dropping/recreating exercises does not cascade-delete review_state.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS topics")
        db.execSQL("DROP TABLE IF EXISTS exercises")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sections (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                examVersion TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS units (
                id TEXT NOT NULL PRIMARY KEY,
                sectionId TEXT NOT NULL,
                name TEXT NOT NULL,
                certObjective TEXT NOT NULL,
                orderIndex INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercises (
                id TEXT NOT NULL PRIMARY KEY,
                unitId TEXT NOT NULL,
                type TEXT NOT NULL,
                payload TEXT NOT NULL,
                difficulty INTEGER NOT NULL,
                examVersion TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS unit_progress (
                unitId TEXT NOT NULL PRIMARY KEY,
                completed INTEGER NOT NULL,
                completedAt INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS checkpoint_attempts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sectionId TEXT NOT NULL,
                kind TEXT NOT NULL,
                scorePct INTEGER NOT NULL,
                passed INTEGER NOT NULL,
                takenAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS content_meta (
                configKey TEXT NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}
