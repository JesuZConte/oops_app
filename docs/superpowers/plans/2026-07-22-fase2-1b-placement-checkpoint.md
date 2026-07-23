# Fase 2.1b — Checkpoint de ubicación Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the player tap a locked unit or section in Ruta, take a placement checkpoint over everything it would skip, and — if they pass at 68% — unlock the target and seed SM-2 for the skipped exercises with a short interval, distinguishing "completed by playing" from "completed by checkpoint" in the UI.

**Architecture:** Reuses the existing Sections/Units/Checkpoints infrastructure from Fase 2.1 almost entirely — `CheckpointAttemptEntity`, `UnitProgressEntity`, `markUnitCompleted`, `saveReviewState`, `GetLearningPathUseCase`'s sequential gating, and `CompleteCheckpointUseCase`'s 68% grading all already exist and only need small extensions. Two new use cases (`GetSkippedUnitsUseCase`, `GetPlacementCheckpointSessionUseCase`) plus one new Compose screen/ViewModel pair (`PlacementCheckpointScreen`/`PlacementCheckpointViewModel`) round out the vertical slice.

**Tech Stack:** Kotlin, Jetpack Compose, Room (KSP), Hilt, kotlinx.coroutines, kotlinx.serialization. Unit tests: JUnit4 + `kotlinx-coroutines-test`, hand-written fake repositories (no mocking library in this codebase). Migration tests: `androidx.room:room-testing` instrumented tests.

## Global Constraints

- Pass threshold for both checkpoint kinds: **68%** (`PASS_THRESHOLD_PCT` in `CompleteCheckpointUseCase`, unchanged).
- Placement checkpoint size: **3 questions per skipped unit, capped at 24**, further capped by actual pool size (never pad past what exists).
- Hybrid SM-2 seeding for skipped-but-unsampled exercises: `easeFactor = 2.5, intervalDays = 1, repetitions = 1, dueDate = today.plusDays(1)` — only when the exercise has **no existing `review_state`** (never overwrite real SM-2 progress).
- `UnitCompletionSource` constants: `"played"` (default, existing behavior) and `"placement"` (new).
- Locked-unit subtitle copy in Ruta becomes tappable; completed-via-placement subtitle text is exactly `"Completada por checkpoint"` — no color/icon change (explicit user decision).
- Retries on a failed placement checkpoint are unlimited and non-punitive — no streak/XP penalty, no cooldown.
- Follow existing repository/use-case patterns exactly: constructor-injected `@Inject`, interfaces in `domain/repository`, impls in `data/repository`, hand-written `private class Fake...` per test file (see `CompleteCheckpointUseCaseTest.kt`, `GetLearningPathUseCaseTest.kt` for the exact style).
- Design source of truth: `docs/superpowers/specs/2026-07-22-fase2-1b-placement-checkpoint-design.md`.

---

### Task 1: Room migration v2→v3 — `completedVia` column

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/entity/UnitProgressEntity.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/Migrations.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt`
- Test: `app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt`

**Interfaces:**
- Produces: `UnitProgressEntity.completedVia: String` (default `"played"`), `MIGRATION_2_3: Migration`, `AppDatabase` at `version = 3`.

- [ ] **Step 1: Write the failing migration test**

Add this test method to the existing `MigrationTest` class in
`app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt` (keep the existing
`migrate1To2_preservesUserDataAndReseedsV2Content` test untouched, add this as a second `@Test`
method inside the same class):

```kotlin
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
```

Add these imports to the top of the file if not already present (the existing test already has
`assertEquals`/`assertNotNull`, so only check `Room`/`SQLiteDatabase` are already imported — they
are, from the existing test).

- [ ] **Step 2: Run the test to verify it fails (compile error is expected, not a runtime failure)**

Run: `./gradlew connectedDebugAndroidTest --tests "com.zconte.oopsapp.data.local.MigrationTest"`

Expected: **build fails to compile** — `MIGRATION_2_3` is unresolved and `UnitProgressEntity` has
no `completedVia` property yet. This confirms the test is exercising code that doesn't exist yet.

- [ ] **Step 3: Add the `completedVia` column to the entity**

In `app/src/main/java/com/zconte/oopsapp/data/local/entity/UnitProgressEntity.kt`, replace the
full file content with:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_progress")
data class UnitProgressEntity(
    @PrimaryKey val unitId: String,
    val completed: Boolean,
    val completedAt: Long?,
    val completedVia: String = "played"
)
```

- [ ] **Step 4: Add `MIGRATION_2_3`**

In `app/src/main/java/com/zconte/oopsapp/data/local/Migrations.kt`, append this below the existing
`MIGRATION_1_2` declaration (keep `MIGRATION_1_2` unchanged):

```kotlin

/**
 * Adds completedVia to unit_progress so Ruta can tell "played" units apart from ones unlocked via
 * a placement checkpoint skip. Everything already completed today was played, so the column
 * defaults to 'played' -- no backfill logic needed beyond the column default itself.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE unit_progress ADD COLUMN completedVia TEXT NOT NULL DEFAULT 'played'")
    }
}
```

- [ ] **Step 5: Bump the database version and register the migration**

In `app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt`, change:

```kotlin
    version = 2,
```

to:

```kotlin
    version = 3,
```

In `app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt`, change the import line:

```kotlin
import com.zconte.oopsapp.data.local.MIGRATION_1_2
```

to:

```kotlin
import com.zconte.oopsapp.data.local.MIGRATION_1_2
import com.zconte.oopsapp.data.local.MIGRATION_2_3
```

and change:

```kotlin
            .addMigrations(MIGRATION_1_2)
```

to:

```kotlin
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest --tests "com.zconte.oopsapp.data.local.MigrationTest"`

Expected: **PASS**, both `migrate1To2_preservesUserDataAndReseedsV2Content` and
`migrate2To3_addsCompletedViaColumnDefaultingToPlayed`. Requires a connected device/emulator —
same as this project's existing `MigrationTest` (see `adb devices` before running).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/local/entity/UnitProgressEntity.kt \
        app/src/main/java/com/zconte/oopsapp/data/local/Migrations.kt \
        app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt \
        app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt \
        app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt \
        app/schemas
git commit -m "Add unit_progress.completedVia column (migration v2->v3)"
```

(`app/schemas` picks up the auto-generated `3.json` schema snapshot produced by the KSP Room
compiler during the build in Step 6 — commit it alongside, same as `2.json` was committed for
the v1→v2 migration.)

---

### Task 2: Surface `completedVia` through the domain layer

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/UnitCompletionSource.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/CompletedUnit.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/repository/ContentRepository.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/dao/UnitProgressDao.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/repository/ContentRepositoryImpl.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt`

**Interfaces:**
- Consumes: `UnitProgressEntity.completedVia` (Task 1).
- Produces: `UnitCompletionSource.PLAYED`/`.PLACEMENT` (String constants), `CompletedUnit(unitId: String, completedVia: String)`, `ContentRepository.getCompletedUnits(): List<CompletedUnit>`, `ContentRepository.markUnitCompleted(unitId: String, completedAt: LocalDate, via: String)`, `UnitProgress.completedVia: String`.

This task changes an interface signature that three existing test files fake — all three must be
updated in the same commit or the module won't compile. There is no way to write a single
"failing test first" for a signature-only change, so this task's TDD loop is: update the fakes and
production code together, then prove correctness with one new behavioral test.

- [ ] **Step 1: Add the new domain types**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/UnitCompletionSource.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

object UnitCompletionSource {
    const val PLAYED = "played"
    const val PLACEMENT = "placement"
}
```

Create `app/src/main/java/com/zconte/oopsapp/domain/model/CompletedUnit.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class CompletedUnit(
    val unitId: String,
    val completedVia: String
)
```

- [ ] **Step 2: Update the `ContentRepository` interface**

Replace the full content of `app/src/main/java/com/zconte/oopsapp/domain/repository/ContentRepository.kt`:

```kotlin
package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import java.time.LocalDate

interface ContentRepository {
    suspend fun getSections(): List<Section>
    suspend fun getUnitsBySection(sectionId: String): List<LearningUnit>
    suspend fun getCompletedUnits(): List<CompletedUnit>
    suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String)
}
```

- [ ] **Step 3: Update `UnitProgressDao`**

In `app/src/main/java/com/zconte/oopsapp/data/local/dao/UnitProgressDao.kt`, replace:

```kotlin
    @Query("SELECT unitId FROM unit_progress WHERE completed = 1")
    suspend fun getCompletedUnitIds(): List<String>
```

with:

```kotlin
    @Query("SELECT * FROM unit_progress WHERE completed = 1")
    suspend fun getCompleted(): List<UnitProgressEntity>
```

- [ ] **Step 4: Update `ContentRepositoryImpl`**

Replace the full content of `app/src/main/java/com/zconte/oopsapp/data/repository/ContentRepositoryImpl.kt`:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.dao.UnitProgressDao
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity
import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import java.time.LocalDate
import javax.inject.Inject

class ContentRepositoryImpl @Inject constructor(
    private val sectionDao: SectionDao,
    private val unitDao: UnitDao,
    private val unitProgressDao: UnitProgressDao
) : ContentRepository {

    override suspend fun getSections(): List<Section> =
        sectionDao.getAll().map { it.toDomain() }

    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> =
        unitDao.getBySection(sectionId).map { it.toDomain() }

    override suspend fun getCompletedUnits(): List<CompletedUnit> =
        unitProgressDao.getCompleted().map { CompletedUnit(it.unitId, it.completedVia) }

    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {
        unitProgressDao.upsert(
            UnitProgressEntity(
                unitId = unitId,
                completed = true,
                completedAt = completedAt.toEpochDay(),
                completedVia = via
            )
        )
    }
}

private fun SectionEntity.toDomain() = Section(id, name, orderIndex, examVersion)

private fun UnitEntity.toDomain() = LearningUnit(id, sectionId, name, certObjective, orderIndex)
```

- [ ] **Step 5: Add `completedVia` to the `UnitProgress` domain model**

In `app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt`, replace the full content:

```kotlin
package com.zconte.oopsapp.domain.model

data class UnitProgress(
    val unit: LearningUnit,
    val completed: Boolean,
    val unlocked: Boolean,
    val completedVia: String = UnitCompletionSource.PLAYED
)

data class SectionPath(
    val section: Section,
    val unlocked: Boolean,
    val units: List<UnitProgress>,
    val completed: Boolean
)
```

- [ ] **Step 6: Update `GetLearningPathUseCase`**

Replace the full content of `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.model.UnitProgress
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

class GetLearningPathUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(): List<SectionPath> {
        val sections = contentRepository.getSections().sortedBy { it.orderIndex }
        val completedUnits = contentRepository.getCompletedUnits().associateBy { it.unitId }

        var previousSectionComplete = true
        return sections.map { section ->
            val units = contentRepository.getUnitsBySection(section.id).sortedBy { it.orderIndex }
            val sectionUnlocked = previousSectionComplete

            var previousUnitComplete = true
            val unitProgress = units.map { unit ->
                val record = completedUnits[unit.id]
                val completed = record != null
                val unlocked = sectionUnlocked && previousUnitComplete
                previousUnitComplete = completed
                UnitProgress(unit, completed, unlocked, record?.completedVia ?: UnitCompletionSource.PLAYED)
            }

            val sectionComplete = units.isNotEmpty() && units.all { it.id in completedUnits }
            previousSectionComplete = sectionComplete

            SectionPath(section, sectionUnlocked, unitProgress, sectionComplete)
        }
    }
}
```

- [ ] **Step 7: Update `MarkUnitProgressUseCase` to pass `via` explicitly**

In `app/src/main/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCase.kt`, add the
import `com.zconte.oopsapp.domain.model.UnitCompletionSource` and change:

```kotlin
            contentRepository.markUnitCompleted(unitId, today)
```

to:

```kotlin
            contentRepository.markUnitCompleted(unitId, today, UnitCompletionSource.PLAYED)
```

- [ ] **Step 8: Fix the three existing test fakes so the module compiles**

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`, replace
the full content:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.ContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForPath(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetLearningPathUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun played(unitId: String) = CompletedUnit(unitId, UnitCompletionSource.PLAYED)

    @Test
    fun `first section and its first unit are always unlocked`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = emptyList()
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertTrue(path.first().unlocked)
        assertTrue(path.first().units[0].unlocked)
        assertFalse(path.first().units[1].unlocked)
    }

    @Test
    fun `a unit unlocks once the previous unit in the same section is completed`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertTrue(path.first().units[1].unlocked)
    }

    @Test
    fun `a section unlocks once every unit of the previous section is completed`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertTrue(path.first().completed)
        assertTrue(path[1].unlocked)
        assertEquals("s2", path[1].section.id)
    }

    @Test
    fun `a section stays locked while the previous section has incomplete units`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertFalse(path[1].unlocked)
    }

    @Test
    fun `a unit completed via a placement checkpoint surfaces that source`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(CompletedUnit("s1-u1", UnitCompletionSource.PLACEMENT))
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertEquals(UnitCompletionSource.PLACEMENT, path.first().units[0].completedVia)
    }
}
```

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt`, replace
the `FakeContentRepositoryForUnitProgress` class (keep everything else in the file unchanged):

```kotlin
private class FakeContentRepositoryForUnitProgress : ContentRepository {
    val markedComplete = mutableListOf<String>()
    override suspend fun getSections(): List<Section> = emptyList()
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> =
        markedComplete.map { CompletedUnit(it, UnitCompletionSource.PLAYED) }
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {
        markedComplete.add(unitId)
    }
}
```

and add `com.zconte.oopsapp.domain.model.CompletedUnit` and
`com.zconte.oopsapp.domain.model.UnitCompletionSource` to that file's imports.

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt`,
replace the `FakeContentRepositoryForCheckpoint` class (keep everything else in the file unchanged):

```kotlin
private class FakeContentRepositoryForCheckpoint(
    private val sections: List<Section>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = emptyList()
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}
```

and add `com.zconte.oopsapp.domain.model.CompletedUnit` to that file's imports.

- [ ] **Step 9: Run all affected unit tests**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetLearningPathUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCaseTest"`

Expected: **PASS**, all tests in all three classes (5 + 2 + 3 = 10 tests total).

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/model/UnitCompletionSource.kt \
        app/src/main/java/com/zconte/oopsapp/domain/model/CompletedUnit.kt \
        app/src/main/java/com/zconte/oopsapp/domain/repository/ContentRepository.kt \
        app/src/main/java/com/zconte/oopsapp/data/local/dao/UnitProgressDao.kt \
        app/src/main/java/com/zconte/oopsapp/data/repository/ContentRepositoryImpl.kt \
        app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt
git commit -m "Surface completedVia through ContentRepository and GetLearningPathUseCase"
```

---

### Task 3: `GetSkippedUnitsUseCase`

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/SkippedUnitsResult.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCaseTest.kt`

**Interfaces:**
- Consumes: `GetLearningPathUseCase.invoke(): List<SectionPath>` (existing, Task 2's shape).
- Produces: `SkippedUnitsResult(targetUnit: LearningUnit?, skippedUnits: List<LearningUnit>)`, `GetSkippedUnitsUseCase.invoke(targetUnitId: String): SkippedUnitsResult` — consumed by Task 6's `PlacementCheckpointViewModel`.

This use case deliberately depends on `GetLearningPathUseCase` (a use case, not a repository) to
reuse its already-tested sequential-gating computation instead of re-deriving locked/unlocked
logic a second time — the only such dependency in this codebase, justified by DRY.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.ContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForSkipped(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetSkippedUnitsUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun played(unitId: String) = CompletedUnit(unitId, UnitCompletionSource.PLAYED)

    @Test
    fun `skipping a locked unit within the current section returns only that gap`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2), unit("s1-u3", "s1", 3))
            ),
            completedUnits = emptyList()
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("s1-u3")

        assertEquals(listOf("s1-u1", "s1-u2"), result.skippedUnits.map { it.id })
        assertEquals("s1-u3", result.targetUnit?.id)
    }

    @Test
    fun `skipping an entire locked section includes all of it plus the remainder of the current one`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)),
                "s2" to listOf(unit("s2-u1", "s2", 1), unit("s2-u2", "s2", 2))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("s2-u2")

        assertEquals(listOf("s1-u2", "s2-u1"), result.skippedUnits.map { it.id })
    }

    @Test
    fun `skipping across several fully locked sections includes every unit before the target`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1), section("s2", 2), section("s3", 3)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1)),
                "s3" to listOf(unit("s3-u1", "s3", 1))
            ),
            completedUnits = emptyList()
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("s3-u1")

        assertEquals(listOf("s1-u1", "s2-u1"), result.skippedUnits.map { it.id })
    }

    @Test
    fun `a target that is already unlocked has nothing to skip`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("s1-u2")

        assertTrue(result.skippedUnits.isEmpty())
    }

    @Test
    fun `an unknown target id resolves to no target and nothing to skip`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("does-not-exist")

        assertNull(result.targetUnit)
        assertTrue(result.skippedUnits.isEmpty())
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetSkippedUnitsUseCaseTest"`

Expected: **FAIL to compile** — `GetSkippedUnitsUseCase` and `SkippedUnitsResult` don't exist yet.

- [ ] **Step 3: Create `SkippedUnitsResult`**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/SkippedUnitsResult.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class SkippedUnitsResult(
    val targetUnit: LearningUnit?,
    val skippedUnits: List<LearningUnit>
)
```

- [ ] **Step 4: Implement `GetSkippedUnitsUseCase`**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.SkippedUnitsResult
import javax.inject.Inject

class GetSkippedUnitsUseCase @Inject constructor(
    private val getLearningPathUseCase: GetLearningPathUseCase
) {
    suspend operator fun invoke(targetUnitId: String): SkippedUnitsResult {
        val allUnits = getLearningPathUseCase().flatMap { it.units }
        val targetIndex = allUnits.indexOfFirst { it.unit.id == targetUnitId }
        if (targetIndex < 0) return SkippedUnitsResult(targetUnit = null, skippedUnits = emptyList())

        val target = allUnits[targetIndex].unit
        val skipped = allUnits.subList(0, targetIndex)
            .filterNot { it.completed }
            .map { it.unit }
        return SkippedUnitsResult(target, skipped)
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetSkippedUnitsUseCaseTest"`

Expected: **PASS**, all 5 tests.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/model/SkippedUnitsResult.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCaseTest.kt
git commit -m "Add GetSkippedUnitsUseCase"
```

---

### Task 4: `GetPlacementCheckpointSessionUseCase`

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt`

**Interfaces:**
- Consumes: `ExerciseRepository.getExercisesByUnit(unitId: String): List<Exercise>` (existing).
- Produces: `GetPlacementCheckpointSessionUseCase.invoke(skippedUnitIds: List<String>): List<Exercise>` — consumed by Task 6's `PlacementCheckpointViewModel`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForPlacementSession(
    private val exercisesByUnit: Map<String, List<Exercise>>
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = emptyList()
}

class GetPlacementCheckpointSessionUseCaseTest {

    private fun exercisesFor(unitId: String, count: Int) =
        (1..count).map { Exercise("$unitId-ex-$it", unitId, "mcq", "{}", 1) }

    @Test
    fun `one skipped unit yields 3 questions when its pool is large enough`() = runTest {
        val repository = FakeExerciseRepositoryForPlacementSession(mapOf("u1" to exercisesFor("u1", 10)))
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(listOf("u1"))

        assertEquals(3, result.size)
        assertTrue(result.all { it.unitId == "u1" })
    }

    @Test
    fun `size scales at 3 questions per skipped unit`() = runTest {
        val repository = FakeExerciseRepositoryForPlacementSession(
            mapOf(
                "u1" to exercisesFor("u1", 10),
                "u2" to exercisesFor("u2", 10),
                "u3" to exercisesFor("u3", 10)
            )
        )
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(listOf("u1", "u2", "u3"))

        assertEquals(9, result.size)
    }

    @Test
    fun `size caps at 24 even when many units are skipped`() = runTest {
        val unitIds = (1..10).map { "u$it" }
        val exercisesByUnit = unitIds.associateWith { exercisesFor(it, 10) }
        val repository = FakeExerciseRepositoryForPlacementSession(exercisesByUnit)
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(unitIds)

        assertEquals(24, result.size)
    }

    @Test
    fun `a small combined pool is capped, not padded past what exists`() = runTest {
        val repository = FakeExerciseRepositoryForPlacementSession(mapOf("u1" to exercisesFor("u1", 2)))
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(listOf("u1"))

        assertEquals(2, result.size)
    }

    @Test
    fun `no skipped units yields an empty session`() = runTest {
        val repository = FakeExerciseRepositoryForPlacementSession(emptyMap())
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(emptyList())

        assertTrue(result.isEmpty())
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetPlacementCheckpointSessionUseCaseTest"`

Expected: **FAIL to compile** — `GetPlacementCheckpointSessionUseCase` doesn't exist yet.

- [ ] **Step 3: Implement the use case**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import javax.inject.Inject

private const val QUESTIONS_PER_SKIPPED_UNIT = 3
private const val MAX_SIZE = 24

class GetPlacementCheckpointSessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(skippedUnitIds: List<String>): List<Exercise> {
        val pool = skippedUnitIds.flatMap { exerciseRepository.getExercisesByUnit(it) }
        val targetSize = (skippedUnitIds.size * QUESTIONS_PER_SKIPPED_UNIT).coerceAtMost(MAX_SIZE)
        return pool.shuffled().take(targetSize.coerceAtMost(pool.size))
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetPlacementCheckpointSessionUseCaseTest"`

Expected: **PASS**, all 5 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt
git commit -m "Add GetPlacementCheckpointSessionUseCase"
```

---

### Task 5: Extend `CompleteCheckpointUseCase` with the unlock + hybrid-seed side effect

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt`

**Interfaces:**
- Consumes: `ContentRepository.markUnitCompleted(unitId, completedAt, via)` (Task 2),
  `ExerciseRepository.getExercisesByUnit`/`getReviewState`/`saveReviewState` (existing).
- Produces: `CompleteCheckpointUseCase.invoke(sectionId, kind, correctCount, totalCount, today, skippedUnitIds: List<String> = emptyList()): CheckpointResult` — consumed by Task 6's `PlacementCheckpointViewModel`. Existing callers (`CheckpointViewModel`) are unaffected since `skippedUnitIds` defaults to empty and the new side effect only runs for `kind == CheckpointKind.PLACEMENT`.

- [ ] **Step 1: Write the failing tests**

Replace the full content of `app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeCheckpointRepository : CheckpointRepository {
    data class RecordedAttempt(val sectionId: String, val kind: String, val scorePct: Int, val passed: Boolean)
    val recorded = mutableListOf<RecordedAttempt>()

    override suspend fun recordAttempt(sectionId: String, kind: String, scorePct: Int, passed: Boolean, takenAt: LocalDate) {
        recorded.add(RecordedAttempt(sectionId, kind, scorePct, passed))
    }
}

private class FakeContentRepositoryForComplete : ContentRepository {
    val markedComplete = mutableListOf<Pair<String, String>>()
    override suspend fun getSections(): List<Section> = emptyList()
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = emptyList()
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {
        markedComplete.add(unitId to via)
    }
}

private class FakeExerciseRepositoryForComplete(
    private val exercisesByUnit: Map<String, List<Exercise>> = emptyMap(),
    private val existingReviewState: Set<String> = emptySet()
) : ExerciseRepository {
    val seeded = mutableListOf<ReviewState>()
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        if (exerciseId in existingReviewState) ReviewState(exerciseId, 2.5, 6, 2, LocalDate.of(2026, 8, 1)) else null
    override suspend fun saveReviewState(state: ReviewState) {
        seeded.add(state)
    }
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = emptyList()
}

class CompleteCheckpointUseCaseTest {

    private val today = LocalDate.of(2026, 7, 20)

    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "mcq", "{}", 1)

    @Test
    fun `passes at exactly the 68 percent threshold`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        val useCase = CompleteCheckpointUseCase(
            checkpointRepository, FakeContentRepositoryForComplete(), FakeExerciseRepositoryForComplete()
        )

        // 68% of 25 = 17 correct, rounds down to exactly 68 -- boundary case.
        val result = useCase("s1", CheckpointKind.REVIEW, correctCount = 17, totalCount = 25, today = today)

        assertEquals(68, result.scorePct)
        assertTrue(result.passed)
        assertEquals(1, checkpointRepository.recorded.size)
        assertTrue(checkpointRepository.recorded.first().passed)
    }

    @Test
    fun `fails below the 68 percent threshold`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        val useCase = CompleteCheckpointUseCase(
            checkpointRepository, FakeContentRepositoryForComplete(), FakeExerciseRepositoryForComplete()
        )

        val result = useCase("s1", CheckpointKind.REVIEW, correctCount = 6, totalCount = 12, today = today)

        assertEquals(50, result.scorePct)
        assertFalse(result.passed)
    }

    @Test
    fun `a passed review checkpoint never marks units complete even if skippedUnitIds is passed by mistake`() = runTest {
        val contentRepository = FakeContentRepositoryForComplete()
        val useCase = CompleteCheckpointUseCase(
            FakeCheckpointRepository(), contentRepository, FakeExerciseRepositoryForComplete()
        )

        useCase("s1", CheckpointKind.REVIEW, correctCount = 10, totalCount = 10, today = today, skippedUnitIds = listOf("u1"))

        assertTrue(contentRepository.markedComplete.isEmpty())
    }

    @Test
    fun `a passed placement checkpoint marks every skipped unit complete via placement`() = runTest {
        val contentRepository = FakeContentRepositoryForComplete()
        val exerciseRepository = FakeExerciseRepositoryForComplete()
        val useCase = CompleteCheckpointUseCase(FakeCheckpointRepository(), contentRepository, exerciseRepository)

        useCase("s2", CheckpointKind.PLACEMENT, correctCount = 10, totalCount = 10, today = today, skippedUnitIds = listOf("u1", "u2"))

        assertEquals(listOf("u1" to UnitCompletionSource.PLACEMENT, "u2" to UnitCompletionSource.PLACEMENT), contentRepository.markedComplete)
    }

    @Test
    fun `a passed placement checkpoint seeds review_state only for exercises without one already`() = runTest {
        val contentRepository = FakeContentRepositoryForComplete()
        val exerciseRepository = FakeExerciseRepositoryForComplete(
            exercisesByUnit = mapOf("u1" to listOf(exercise("u1-ex-1", "u1"), exercise("u1-ex-2", "u1"))),
            existingReviewState = setOf("u1-ex-1")
        )
        val useCase = CompleteCheckpointUseCase(FakeCheckpointRepository(), contentRepository, exerciseRepository)

        useCase("s1", CheckpointKind.PLACEMENT, correctCount = 3, totalCount = 3, today = today, skippedUnitIds = listOf("u1"))

        assertEquals(1, exerciseRepository.seeded.size)
        val seeded = exerciseRepository.seeded.first()
        assertEquals("u1-ex-2", seeded.exerciseId)
        assertEquals(2.5, seeded.easeFactor, 0.0001)
        assertEquals(1, seeded.intervalDays)
        assertEquals(1, seeded.repetitions)
        assertEquals(today.plusDays(1), seeded.dueDate)
    }

    @Test
    fun `a failed placement checkpoint marks nothing complete and seeds nothing`() = runTest {
        val contentRepository = FakeContentRepositoryForComplete()
        val exerciseRepository = FakeExerciseRepositoryForComplete(
            exercisesByUnit = mapOf("u1" to listOf(exercise("u1-ex-1", "u1")))
        )
        val useCase = CompleteCheckpointUseCase(FakeCheckpointRepository(), contentRepository, exerciseRepository)

        val result = useCase("s1", CheckpointKind.PLACEMENT, correctCount = 1, totalCount = 10, today = today, skippedUnitIds = listOf("u1"))

        assertFalse(result.passed)
        assertTrue(contentRepository.markedComplete.isEmpty())
        assertTrue(exerciseRepository.seeded.isEmpty())
    }
}
```

- [ ] **Step 2: Run the tests to verify the new ones fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCaseTest"`

Expected: **FAIL to compile** — the 3-arg `CompleteCheckpointUseCase` constructor and
`skippedUnitIds` parameter don't exist yet.

- [ ] **Step 3: Extend `CompleteCheckpointUseCase`**

Replace the full content of `app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

private const val PASS_THRESHOLD_PCT = 68
private const val SEED_EASE_FACTOR = 2.5
private const val SEED_INTERVAL_DAYS = 1
private const val SEED_REPETITIONS = 1

class CompleteCheckpointUseCase @Inject constructor(
    private val checkpointRepository: CheckpointRepository,
    private val contentRepository: ContentRepository,
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(
        sectionId: String,
        kind: String,
        correctCount: Int,
        totalCount: Int,
        today: LocalDate,
        skippedUnitIds: List<String> = emptyList()
    ): CheckpointResult {
        val scorePct = if (totalCount == 0) 0 else (correctCount * 100) / totalCount
        val passed = scorePct >= PASS_THRESHOLD_PCT
        checkpointRepository.recordAttempt(sectionId, kind, scorePct, passed, today)

        if (kind == CheckpointKind.PLACEMENT && passed) {
            unlockSkippedUnits(skippedUnitIds, today)
        }

        return CheckpointResult(scorePct, passed)
    }

    private suspend fun unlockSkippedUnits(skippedUnitIds: List<String>, today: LocalDate) {
        skippedUnitIds.forEach { unitId ->
            contentRepository.markUnitCompleted(unitId, today, UnitCompletionSource.PLACEMENT)
            exerciseRepository.getExercisesByUnit(unitId).forEach { exercise ->
                if (exerciseRepository.getReviewState(exercise.id) == null) {
                    exerciseRepository.saveReviewState(
                        ReviewState(
                            exerciseId = exercise.id,
                            easeFactor = SEED_EASE_FACTOR,
                            intervalDays = SEED_INTERVAL_DAYS,
                            repetitions = SEED_REPETITIONS,
                            dueDate = today.plusDays(1)
                        )
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCaseTest"`

Expected: **PASS**, all 6 tests.

- [ ] **Step 5: Run the full unit test suite to confirm nothing else broke**

Run: `./gradlew testDebugUnitTest`

Expected: **PASS**, every test in `app/src/test`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt
git commit -m "Extend CompleteCheckpointUseCase to unlock and hybrid-seed skipped units on placement pass"
```

---

### Task 6: Placement checkpoint UI + navigation wiring

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModel.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointScreen.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `GetSkippedUnitsUseCase` (Task 3), `GetPlacementCheckpointSessionUseCase` (Task 4),
  `CompleteCheckpointUseCase` extended signature (Task 5), `SubmitAnswerUseCase`/
  `UpdateStreakUseCase` (existing), `ExerciseAnswerCard`/`ExerciseAnswerState` (existing, from
  `ui/components`), `UnitProgress.completedVia` (Task 2).
- Produces: `OopsDestinations.PLACEMENT_CHECKPOINT` route, `ProgressScreen(... onOpenPlacementCheckpoint: (String) -> Unit)`.

This project has no ViewModel/Compose unit tests (confirmed: `app/src/test` only covers
`domain/usecase`) — UI correctness here is verified by an on-device manual walkthrough, matching
how this project has always verified UI work.

- [ ] **Step 1: Add the new route constant**

In `app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt`, add a line inside the
object (after `CHECKPOINT`):

```kotlin
    const val PLACEMENT_CHECKPOINT = "placement_checkpoint/{targetUnitId}"
```

- [ ] **Step 2: Create `PlacementCheckpointViewModel`**

Create `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.checkpoint

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCase
import com.zconte.oopsapp.domain.usecase.GetPlacementCheckpointSessionUseCase
import com.zconte.oopsapp.domain.usecase.GetSkippedUnitsUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
import com.zconte.oopsapp.domain.usecase.UpdateStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject

data class PlacementCheckpointUiState(
    val isLoadingSkipped: Boolean = true,
    val hasStarted: Boolean = false,
    val targetUnit: LearningUnit? = null,
    val skippedUnits: List<LearningUnit> = emptyList(),
    val queue: List<Exercise> = emptyList(),
    val currentExercise: ExerciseContent? = null,
    val currentIndex: Int = 0,
    val totalExercises: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val isComplete: Boolean = false,
    val result: CheckpointResult? = null,
    val isCompleting: Boolean = false
)

@HiltViewModel
class PlacementCheckpointViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSkippedUnitsUseCase: GetSkippedUnitsUseCase,
    private val getPlacementCheckpointSessionUseCase: GetPlacementCheckpointSessionUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val completeCheckpointUseCase: CompleteCheckpointUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val json: Json
) : ViewModel() {

    private val targetUnitId: String = checkNotNull(savedStateHandle["targetUnitId"])

    private val _uiState = MutableStateFlow(PlacementCheckpointUiState())
    val uiState: StateFlow<PlacementCheckpointUiState> = _uiState.asStateFlow()

    private var correctCount = 0
    private var pendingAnswerJob: Job? = null

    init {
        viewModelScope.launch {
            val result = getSkippedUnitsUseCase(targetUnitId)
            _uiState.update {
                it.copy(isLoadingSkipped = false, targetUnit = result.targetUnit, skippedUnits = result.skippedUnits)
            }
        }
    }

    fun startCheckpoint() {
        viewModelScope.launch {
            val skippedIds = _uiState.value.skippedUnits.map { it.id }
            val queue = getPlacementCheckpointSessionUseCase(skippedIds)
            if (queue.isEmpty()) {
                _uiState.update { it.copy(hasStarted = true, isComplete = true, result = CheckpointResult(0, false)) }
            } else {
                _uiState.update {
                    it.copy(
                        hasStarted = true,
                        queue = queue,
                        totalExercises = queue.size,
                        currentIndex = 1,
                        currentExercise = decode(queue.first())
                    )
                }
            }
        }
    }

    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        if (current.isAnswered) return
        val exercise = current.currentExercise ?: return
        val exerciseId = current.queue.first().id
        val correct = userAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)
        if (correct) correctCount++

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        pendingAnswerJob = viewModelScope.launch {
            submitAnswerUseCase(exerciseId, quality = if (correct) 5 else 2, today = LocalDate.now())
        }
    }

    fun nextExercise() {
        if (_uiState.value.isCompleting) return
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isEmpty()) {
            _uiState.update { it.copy(isCompleting = true) }
            viewModelScope.launch {
                pendingAnswerJob?.join()
                updateStreakUseCase(LocalDate.now())
                val state = _uiState.value
                val result = completeCheckpointUseCase(
                    sectionId = state.targetUnit?.sectionId ?: "",
                    kind = CheckpointKind.PLACEMENT,
                    correctCount = correctCount,
                    totalCount = state.totalExercises,
                    today = LocalDate.now(),
                    skippedUnitIds = state.skippedUnits.map { it.id }
                )
                _uiState.update { it.copy(isComplete = true, result = result) }
            }
        } else {
            _uiState.update {
                it.copy(
                    queue = remaining,
                    currentIndex = it.currentIndex + 1,
                    currentExercise = decode(remaining.first()),
                    isAnswered = false,
                    isCorrect = false,
                    selectedAnswer = null
                )
            }
        }
    }

    private fun decode(exercise: Exercise): ExerciseContent =
        json.decodeFromString(ExerciseContent.serializer(), exercise.payload)
}
```

- [ ] **Step 3: Create `PlacementCheckpointScreen`**

Create `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.checkpoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.ui.components.ExerciseAnswerCard
import com.zconte.oopsapp.ui.components.ExerciseAnswerState
import com.zconte.oopsapp.ui.theme.OopsTheme

@Composable
fun PlacementCheckpointScreen(
    onCancelled: () -> Unit,
    onFailed: () -> Unit,
    onUnlocked: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlacementCheckpointViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isComplete) {
        PlacementResultView(
            result = uiState.result,
            targetUnitId = uiState.targetUnit?.id,
            onUnlocked = onUnlocked,
            onFailed = onFailed,
            modifier = modifier
        )
        return
    }

    if (!uiState.hasStarted) {
        PlacementEntryView(
            isLoading = uiState.isLoadingSkipped,
            skippedCount = uiState.skippedUnits.size,
            targetName = uiState.targetUnit?.name,
            onStart = viewModel::startCheckpoint,
            onCancel = onCancelled,
            modifier = modifier
        )
        return
    }

    val exercise = uiState.currentExercise
    if (exercise == null) {
        Text(
            "Cargando checkpoint...",
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    ExerciseAnswerCard(
        state = ExerciseAnswerState(
            exercise = exercise,
            currentIndex = uiState.currentIndex,
            totalExercises = uiState.totalExercises,
            isAnswered = uiState.isAnswered,
            isCorrect = uiState.isCorrect,
            selectedAnswer = uiState.selectedAnswer
        ),
        onSubmit = viewModel::submitAnswer,
        onNext = viewModel::nextExercise,
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp)
    )
}

@Composable
private fun PlacementEntryView(
    isLoading: Boolean,
    skippedCount: Int,
    targetName: String?,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        if (isLoading) {
            Text("Cargando...", style = MaterialTheme.typography.bodyMedium)
        } else {
            val questionCount = (skippedCount * 3).coerceAtMost(24)
            Text(
                text = "Vas a saltar $skippedCount " + if (skippedCount == 1) "unidad" else "unidades",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Responde $questionCount preguntas. Si apruebas al 68%, se desbloquea ${targetName.orEmpty()}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            enabled = !isLoading && skippedCount > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("EMPEZAR", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun PlacementResultView(
    result: CheckpointResult?,
    targetUnitId: String?,
    onUnlocked: (String) -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passed = result?.passed == true
    val extended = OopsTheme.extendedColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = if (passed) "¡Salto desbloqueado!" else "Todavia no",
            style = MaterialTheme.typography.headlineSmall,
            color = if (passed) extended.success else MaterialTheme.colorScheme.error
        )
        Text(
            text = "${result?.scorePct ?: 0}% (necesitas 68% para aprobar)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { if (passed && targetUnitId != null) onUnlocked(targetUnitId) else onFailed() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (passed) "JUGAR" else "CONTINUAR", style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

- [ ] **Step 4: Wire the route into `OopsNavHost`**

In `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`, add the import:

```kotlin
import com.zconte.oopsapp.ui.checkpoint.PlacementCheckpointScreen
```

Add this `composable` block right after the existing `OopsDestinations.CHECKPOINT` block:

```kotlin
        composable(
            route = OopsDestinations.PLACEMENT_CHECKPOINT,
            arguments = listOf(navArgument("targetUnitId") { type = NavType.StringType })
        ) {
            PlacementCheckpointScreen(
                onCancelled = { navController.popBackStack() },
                onFailed = { navController.popBackStack() },
                onUnlocked = { unitId ->
                    navController.navigate("unit_session/$unitId") {
                        popUpTo(OopsDestinations.PROGRESS)
                    }
                }
            )
        }
```

Change the `PROGRESS` composable block from:

```kotlin
        composable(OopsDestinations.PROGRESS) {
            ProgressScreen(
                onPlayUnit = { unitId -> navController.navigate("unit_session/$unitId") },
                onOpenCheckpoint = { sectionId -> navController.navigate("checkpoint/$sectionId") }
            )
        }
```

to:

```kotlin
        composable(OopsDestinations.PROGRESS) {
            ProgressScreen(
                onPlayUnit = { unitId -> navController.navigate("unit_session/$unitId") },
                onOpenCheckpoint = { sectionId -> navController.navigate("checkpoint/$sectionId") },
                onOpenPlacementCheckpoint = { targetUnitId -> navController.navigate("placement_checkpoint/$targetUnitId") }
            )
        }
```

- [ ] **Step 5: Make locked units tappable in `ProgressScreen`**

In `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`, add the import:

```kotlin
import com.zconte.oopsapp.domain.model.UnitCompletionSource
```

Change the `ProgressScreen` function signature from:

```kotlin
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
```

to:

```kotlin
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    onOpenPlacementCheckpoint: (String) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
```

In the same file, change the call to `SectionPathBlock` inside the `LazyColumn`/`items` block from:

```kotlin
            items(uiState.sections) { sectionPath ->
                SectionPathBlock(
                    sectionPath = sectionPath,
                    onPlayUnit = onPlayUnit,
                    onOpenCheckpoint = onOpenCheckpoint
                )
            }
```

to:

```kotlin
            items(uiState.sections) { sectionPath ->
                SectionPathBlock(
                    sectionPath = sectionPath,
                    onPlayUnit = onPlayUnit,
                    onOpenCheckpoint = onOpenCheckpoint,
                    onOpenPlacementCheckpoint = onOpenPlacementCheckpoint
                )
            }
```

Change the `SectionPathBlock` function from:

```kotlin
@Composable
private fun SectionPathBlock(
    sectionPath: SectionPath,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit
) {
    val extended = OopsTheme.extendedColors

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = sectionPath.section.name.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
            color = if (sectionPath.unlocked) MaterialTheme.colorScheme.primary else extended.lockedText
        )

        sectionPath.units.forEach { unitProgress ->
            UnitRow(unitProgress = unitProgress, onClick = { onPlayUnit(unitProgress.unit.id) })
        }

        if (sectionPath.completed) {
            CheckpointRow(onClick = { onOpenCheckpoint(sectionPath.section.id) })
        }
    }
}
```

to:

```kotlin
@Composable
private fun SectionPathBlock(
    sectionPath: SectionPath,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    onOpenPlacementCheckpoint: (String) -> Unit
) {
    val extended = OopsTheme.extendedColors

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = sectionPath.section.name.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
            color = if (sectionPath.unlocked) MaterialTheme.colorScheme.primary else extended.lockedText
        )

        sectionPath.units.forEach { unitProgress ->
            UnitRow(
                unitProgress = unitProgress,
                onClick = {
                    if (unitProgress.unlocked || unitProgress.completed) {
                        onPlayUnit(unitProgress.unit.id)
                    } else {
                        onOpenPlacementCheckpoint(unitProgress.unit.id)
                    }
                }
            )
        }

        if (sectionPath.completed) {
            CheckpointRow(onClick = { onOpenCheckpoint(sectionPath.section.id) })
        }
    }
}
```

Change the `UnitRow` function from:

```kotlin
@Composable
private fun UnitRow(unitProgress: UnitProgress, onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
    val playable = unitProgress.unlocked || unitProgress.completed
    val dotColor = when {
        unitProgress.completed -> extended.success
        unitProgress.unlocked -> MaterialTheme.colorScheme.primary
        else -> extended.lockedBorder
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unitProgress.unlocked, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (playable) dotColor else extended.lockedBackground)
        )
        Column {
            Text(
                text = unitProgress.unit.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (playable) MaterialTheme.colorScheme.onBackground else extended.lockedText
            )
            Text(
                text = when {
                    unitProgress.completed -> "Completada"
                    unitProgress.unlocked -> "Toca para jugar"
                    else -> "🔒 Termina la unidad anterior"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = extended.lockedText
            )
        }
    }
}
```

to:

```kotlin
@Composable
private fun UnitRow(unitProgress: UnitProgress, onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
    val playable = unitProgress.unlocked || unitProgress.completed
    val dotColor = when {
        unitProgress.completed -> extended.success
        unitProgress.unlocked -> MaterialTheme.colorScheme.primary
        else -> extended.lockedBorder
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (playable) dotColor else extended.lockedBackground)
        )
        Column {
            Text(
                text = unitProgress.unit.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (playable) MaterialTheme.colorScheme.onBackground else extended.lockedText
            )
            Text(
                text = when {
                    unitProgress.completed && unitProgress.completedVia == UnitCompletionSource.PLACEMENT -> "Completada por checkpoint"
                    unitProgress.completed -> "Completada"
                    unitProgress.unlocked -> "Toca para jugar"
                    else -> "🔒 Toca para intentar saltarla"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = extended.lockedText
            )
        }
    }
}
```

- [ ] **Step 6: Compile**

Run: `./gradlew compileDebugKotlin`

Expected: **BUILD SUCCESSFUL**.

- [ ] **Step 7: Manual on-device verification**

With a device connected (`adb devices`):

```bash
./gradlew installDebug
adb shell am force-stop com.zconte.oopsapp
adb shell am start -n com.zconte.oopsapp/.MainActivity
```

Walk through, using `adb shell uiautomator dump` + reading the XML for exact tap bounds (this
project's established approach — do not eyeball scaled screenshot coordinates):

1. Open Ruta. Confirm a unit two or more steps ahead (in the current section or a locked future
   section) is now visibly tappable and its subtitle reads "🔒 Toca para intentar saltarla".
2. Tap it. Confirm the entry screen appears showing the correct skipped-unit count and the
   target's name, with "EMPEZAR" and "Cancelar".
3. Tap "Cancelar". Confirm it returns to Ruta with nothing changed.
4. Tap the same locked unit again, tap "EMPEZAR". Confirm the questionnaire starts and its
   question count matches `3 * skippedCount` (capped at 24).
5. Deliberately fail it (answer everything wrong). Confirm the result screen shows a score below
   68%, and tapping the button returns to Ruta with the target still locked.
6. Tap the unit again, retake it, this time answering enough correctly to clear 68%. Confirm the
   result screen shows "¡Salto desbloqueado!" and tapping "JUGAR" navigates directly into the
   target unit's session.
7. Back out to Ruta. Confirm every previously-skipped unit now shows the green "completed" dot
   with subtitle "Completada por checkpoint", and the target unit (and, if it was the first unit
   of a locked section, that whole section) is unlocked.
8. If the skip spanned into a following section, confirm that section header is no longer dimmed.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModel.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointScreen.kt \
        app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt \
        app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt \
        app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt
git commit -m "Add placement checkpoint UI, wire it into Ruta and navigation"
```

---

## Post-plan checklist

- [ ] Run `./gradlew testDebugUnitTest` once more — full green.
- [ ] Run `./gradlew connectedDebugAndroidTest --tests "com.zconte.oopsapp.data.local.MigrationTest"` once more — full green.
- [ ] Follow `superpowers:finishing-a-development-branch` to decide merge/PR/cleanup.
