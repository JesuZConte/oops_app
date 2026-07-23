package com.zconte.oopsapp.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zconte.oopsapp.data.content.ContentLoader
import com.zconte.oopsapp.data.content.ContentSeeder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @Test
    fun migrate1To2_preservesUserDataAndReseedsV2Content() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
        val dbFile = context.getDatabasePath(TEST_DB)

        // 1. Build a v1-shaped database file directly with raw SQL (the v1 Kotlin entity
        // classes no longer exist in the codebase after Task 1's refactor). review_state and
        // user_stats are copied verbatim from app/schemas/.../2.json since MIGRATION_1_2 never
        // touches them -- they must match the CURRENT entities exactly, not the old ones.
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).apply {
            execSQL("CREATE TABLE topics (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, certObjective TEXT NOT NULL, orderIndex INTEGER NOT NULL)")
            execSQL("CREATE TABLE exercises (id TEXT NOT NULL PRIMARY KEY, topicId TEXT NOT NULL, type TEXT NOT NULL, payload TEXT NOT NULL, difficulty INTEGER NOT NULL)")
            execSQL("CREATE TABLE `review_state` (`exerciseId` TEXT NOT NULL, `easeFactor` REAL NOT NULL, `intervalDays` INTEGER NOT NULL, `repetitions` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, PRIMARY KEY(`exerciseId`))")
            execSQL("CREATE TABLE `user_stats` (`id` INTEGER NOT NULL, `streak` INTEGER NOT NULL, `xp` INTEGER NOT NULL, `lastStudyDate` INTEGER, PRIMARY KEY(`id`))")
            execSQL(
                "INSERT INTO topics (id, name, certObjective, orderIndex) VALUES " +
                    "('java-streams', 'Streams y lambdas', 'streams-lambdas', 0)"
            )
            execSQL(
                "INSERT INTO exercises (id, topicId, type, payload, difficulty) VALUES " +
                    "('streams-05', 'java-streams', 'fill_blank', '{}', 2)"
            )
            execSQL(
                "INSERT INTO review_state (exerciseId, easeFactor, intervalDays, repetitions, dueDate) VALUES " +
                    "('streams-05', 2.6, 6, 2, 19000)"
            )
            execSQL(
                "INSERT INTO user_stats (id, streak, xp, lastStudyDate) VALUES (0, 5, 50, 19000)"
            )
            version = 1
            close()
        }

        // 2. Open via a real Room-managed AppDatabase with the migration registered. Room
        // detects the on-disk user_version is 1, runs MIGRATION_1_2 automatically on first
        // access, and validates the resulting schema against its compiled entities -- the same
        // path a real device takes when the app upgrades.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            runBlocking {
                val json = Json { ignoreUnknownKeys = true }
                val seeder = ContentSeeder(
                    contentLoader = ContentLoader(context, json),
                    sectionDao = db.sectionDao(),
                    unitDao = db.unitDao(),
                    exerciseDao = db.exerciseDao(),
                    contentMetaDao = db.contentMetaDao(),
                    json = json
                )
                seeder.seedIfNeeded()

                // review_state for the pre-existing exercise survived the migration untouched.
                val reviewState = db.reviewStateDao().getByExerciseId("streams-05")
                assertNotNull("review_state for streams-05 must survive the migration", reviewState)
                assertEquals(2.6, reviewState!!.easeFactor, 0.0001)
                assertEquals(6, reviewState.intervalDays)
                assertEquals(2, reviewState.repetitions)

                // user_stats survived untouched.
                val stats = db.userStatsDao().get()
                assertNotNull(stats)
                assertEquals(5, stats!!.streak)
                assertEquals(50, stats.xp)

                // The reseeded v2 content correctly reassigns streams-05 to its new unit, and
                // that unit's exercises are reachable via the section it belongs to.
                val exercisesInUnit = db.exerciseDao().getByUnit("streams-terminal")
                assertTrue(
                    "streams-05 must be reachable from its v2 unit after reseeding",
                    exercisesInUnit.any { it.id == "streams-05" }
                )

                val sections = db.sectionDao().getAll()
                assertTrue("Both packs must be seeded", sections.any { it.id == "java-fundamentals" })
                assertTrue("Both packs must be seeded", sections.any { it.id == "java-streams" })
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate2To3_addsCompletedViaColumnDefaultingToPlayed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration-test-2-3"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)

        // 1. Build a v2-shaped database file directly with raw SQL (pre-migration shape: no
        // completedVia column yet), with one already-completed unit and non-trivial review_state.
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).apply {
            execSQL("CREATE TABLE sections (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, orderIndex INTEGER NOT NULL, examVersion TEXT NOT NULL)")
            execSQL("CREATE TABLE units (id TEXT NOT NULL PRIMARY KEY, sectionId TEXT NOT NULL, name TEXT NOT NULL, certObjective TEXT NOT NULL, orderIndex INTEGER NOT NULL)")
            execSQL("CREATE TABLE exercises (id TEXT NOT NULL PRIMARY KEY, unitId TEXT NOT NULL, type TEXT NOT NULL, payload TEXT NOT NULL, difficulty INTEGER NOT NULL, examVersion TEXT NOT NULL)")
            execSQL("CREATE TABLE unit_progress (unitId TEXT NOT NULL PRIMARY KEY, completed INTEGER NOT NULL, completedAt INTEGER)")
            execSQL("CREATE TABLE checkpoint_attempts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sectionId TEXT NOT NULL, kind TEXT NOT NULL, scorePct INTEGER NOT NULL, passed INTEGER NOT NULL, takenAt INTEGER NOT NULL)")
            execSQL("CREATE TABLE content_meta (configKey TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
            execSQL("CREATE TABLE `review_state` (`exerciseId` TEXT NOT NULL, `easeFactor` REAL NOT NULL, `intervalDays` INTEGER NOT NULL, `repetitions` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, PRIMARY KEY(`exerciseId`))")
            execSQL("CREATE TABLE `user_stats` (`id` INTEGER NOT NULL, `streak` INTEGER NOT NULL, `xp` INTEGER NOT NULL, `lastStudyDate` INTEGER, PRIMARY KEY(`id`))")
            execSQL("INSERT INTO unit_progress (unitId, completed, completedAt) VALUES ('fund-u1', 1, 19000)")
            execSQL("INSERT INTO review_state (exerciseId, easeFactor, intervalDays, repetitions, dueDate) VALUES ('fund-ex-1', 2.6, 6, 2, 19000)")
            execSQL("INSERT INTO user_stats (id, streak, xp, lastStudyDate) VALUES (0, 5, 50, 19000)")
            version = 2
            close()
        }

        // 2. Open via a real Room-managed AppDatabase with both migrations registered.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            runBlocking {
                val progress = db.unitProgressDao().getByUnit("fund-u1")
                assertNotNull("unit_progress row must survive the migration", progress)
                assertEquals("pre-existing completions must backfill to 'played'", "played", progress!!.completedVia)

                val reviewState = db.reviewStateDao().getByExerciseId("fund-ex-1")
                assertNotNull("review_state must survive the migration untouched", reviewState)
                assertEquals(2.6, reviewState!!.easeFactor, 0.0001)

                val stats = db.userStatsDao().get()
                assertNotNull(stats)
                assertEquals(5, stats!!.streak)
            }
        } finally {
            db.close()
        }
    }
}
