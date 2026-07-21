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
            .addMigrations(MIGRATION_1_2)
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
}
