# Resumenes de unidad ("Tips") Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let players read a short, optional tip (text + example code) for any unit they can already play, before starting it, so they are not forced to guess at material they never studied.

**Architecture:** The tip content lives directly in the existing `content/*.json` packs as a new optional `summary` field per unit — no Room migration. A new `ContentRepository.getUnitSummary(unitId)` method (backed by `ContentRepositoryImpl`, which gains a `ContentLoader` dependency alongside its existing Room DAOs) scans the content packs on demand and returns the match. `GetUnitSummaryUseCase` is a one-line domain wrapper over that repository method — every other use case in this codebase depends only on `domain/repository/*` interfaces, never on a `data/*` class directly, and this keeps that rule intact. A new `UnitSummaryScreen` reads it and offers a "COMENZAR UNIDAD" button into the existing unit session. A new "Ver resumen" affordance appears next to any unlocked or completed unit in `ProgressScreen`.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, kotlinx.serialization, JUnit4, kotlinx-coroutines-test. Hand-written fakes only — no mocking library.

## Global Constraints

- `summary` is optional at the JSON/type level (nullable), but for this plan every one of the 19 existing units across the 5 shipped content files (`java-fundamentals.json`, `generics-collections.json`, `streams.json`, `exception-handling.json`, `concurrency.json`) must get a real `summary`.
- No Room migration: `LearningUnit`/`UnitEntity` gain no new field. The summary is read from the JSON asset on demand, never persisted.
- `ContentLoader` becomes an interface (`AssetContentLoader` is the real impl), consumed only by `ContentRepositoryImpl` — never directly by a use case or ViewModel.
- `ContentRepository` (domain interface) gains `suspend fun getUnitSummary(unitId: String): UnitSummary?`. `GetUnitSummaryUseCase` only calls that method; it must not depend on `ContentLoader`, `ContentPackRegistry`, or any other `data/*` type.
- `ContentPackRegistry` (new, `data/content/ContentPackRegistry.kt`) is the single source of truth for the asset-path list; `ContentSeeder` and `ContentRepositoryImpl` both read `ContentPackRegistry.assetPaths` — the path list must never be duplicated.
- Content text follows the existing convention: no accented characters, matching every other string in `content/*.json` today.
- Adding `summary` to existing JSON files does **not** require bumping `ContentSeeder.CURRENT_CONTENT_VERSION` — `ContentPack.toEntities()` (in `ContentMapper.kt`) never reads `summary`, so re-seeding Room is neither necessary nor sufficient for this feature; the summary is read straight from the asset at request time, every time.
- "Ver resumen" only appears for a unit where `unitProgress.unlocked || unitProgress.completed` is true (same condition `UnitRow` already uses to decide whether tapping the row plays the unit vs. opens the placement checkpoint).
- New route: `unit_summary/{unitId}`, following the existing `unit_session/{unitId}` / `checkpoint/{sectionId}` naming pattern in `OopsDestinations.kt`.
- No Compose UI tests for the new screen (Level 2 of the testing ADR has not started) — verify by compilation + manual on-device QA.

---

### Task 1: Content schema + domain model for unit summaries

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentPack.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/UnitSummary.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt`

**Interfaces:**
- Produces: `UnitSummaryPack(text: String, code: String? = null)` and `UnitPack.summary: UnitSummaryPack?` (data/content layer); `UnitSummary(unitName: String, text: String, code: String? = null)` (domain model, a `data class` so structural `assertEquals` works in later tasks' tests).

- [ ] **Step 1: Write the failing tests**

Add these two tests to the existing `ContentPackParsingTest.kt` (after the last test in the file, still inside the `class ContentPackParsingTest { ... }` body):

```kotlin
    @Test
    fun `unit summary parses text and optional code`() {
        val raw = """
            {
              "sectionId": "java-streams",
              "name": "Streams y lambdas",
              "orderIndex": 2,
              "examVersion": "java21",
              "units": [
                {
                  "unitId": "streams-creation",
                  "name": "Creacion de streams",
                  "certObjective": "streams-lambdas",
                  "orderIndex": 0,
                  "summary": {
                    "text": "Un Stream se crea a partir de una fuente de datos.",
                    "code": "lista.stream()"
                  },
                  "exercises": []
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals("Un Stream se crea a partir de una fuente de datos.", pack.units.first().summary?.text)
        assertEquals("lista.stream()", pack.units.first().summary?.code)
    }

    @Test
    fun `unit without a summary field parses with a null summary`() {
        val raw = """
            {
              "sectionId": "java-streams",
              "name": "Streams y lambdas",
              "orderIndex": 2,
              "examVersion": "java21",
              "units": [
                {
                  "unitId": "streams-creation",
                  "name": "Creacion de streams",
                  "certObjective": "streams-lambdas",
                  "orderIndex": 0,
                  "exercises": []
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals(null, pack.units.first().summary)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.content.ContentPackParsingTest"`
Expected: compile error — `UnitPack` has no member `summary` yet.

- [ ] **Step 3: Add the `summary` field to the content schema**

In `ContentPack.kt`, replace:

```kotlin
@Serializable
data class UnitPack(
    val unitId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int,
    val exercises: List<ExerciseContent>
)
```

with:

```kotlin
@Serializable
data class UnitSummaryPack(
    val text: String,
    val code: String? = null
)

@Serializable
data class UnitPack(
    val unitId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int,
    val summary: UnitSummaryPack? = null,
    val exercises: List<ExerciseContent>
)
```

- [ ] **Step 4: Create the domain model**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/UnitSummary.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class UnitSummary(
    val unitName: String,
    val text: String,
    val code: String? = null
)
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.content.ContentPackParsingTest"`
Expected: PASS (6 tests: the 4 pre-existing + the 2 new ones).

- [ ] **Step 6: Run the full JVM suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS, same total test count as before plus 2.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentPack.kt app/src/main/java/com/zconte/oopsapp/domain/model/UnitSummary.kt app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt
git commit -m "feat: add optional summary field to unit content schema"
```

---

### Task 2: Extract `ContentLoader` to an interface; centralize asset paths in `ContentPackRegistry`

This is a pure refactor — no behavior changes, no new capability yet (Task 3 adds the new capability on top of this). Verified by the existing regression suite staying green, not by a new test.

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentLoader.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: nothing new from Task 1.
- Produces: `interface ContentLoader { fun loadPack(assetPath: String): ContentPack }` and its real impl `AssetContentLoader`; `object ContentPackRegistry { val assetPaths: List<String> }`. Task 3's `ContentRepositoryImpl` and Task 3's test fixtures depend on both.

- [ ] **Step 1: Record the baseline**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (same count as the end of Task 1). Note the count — you'll compare against it in Step 5.

- [ ] **Step 2: Extract `ContentLoader` to an interface**

Replace the full contents of `ContentLoader.kt` with:

```kotlin
package com.zconte.oopsapp.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface ContentLoader {
    fun loadPack(assetPath: String): ContentPack
}

class AssetContentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : ContentLoader {
    override fun loadPack(assetPath: String): ContentPack {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return json.decodeFromString(ContentPack.serializer(), text)
    }
}
```

- [ ] **Step 3: Create `ContentPackRegistry`**

Create `app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt`:

```kotlin
package com.zconte.oopsapp.data.content

object ContentPackRegistry {
    val assetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json",
        "content/exception-handling.json",
        "content/concurrency.json"
    )
}
```

- [ ] **Step 4: Point `ContentSeeder` at the registry and the interface**

In `ContentSeeder.kt`, replace:

```kotlin
class ContentSeeder @Inject constructor(
    private val contentLoader: ContentLoader,
    private val sectionDao: SectionDao,
    private val unitDao: UnitDao,
    private val exerciseDao: ExerciseDao,
    private val contentMetaDao: ContentMetaDao,
    private val json: Json
) {
    private val packAssetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json",
        "content/exception-handling.json",
        "content/concurrency.json"
    )

    suspend fun seedIfNeeded() {
        val seededVersion = contentMetaDao.get(CONTENT_VERSION_KEY)?.value
        if (seededVersion == CURRENT_CONTENT_VERSION) return

        sectionDao.clearAll()
        unitDao.clearAll()
        exerciseDao.clearAll()

        packAssetPaths.forEach { assetPath ->
```

with:

```kotlin
class ContentSeeder @Inject constructor(
    private val contentLoader: ContentLoader,
    private val sectionDao: SectionDao,
    private val unitDao: UnitDao,
    private val exerciseDao: ExerciseDao,
    private val contentMetaDao: ContentMetaDao,
    private val json: Json
) {
    suspend fun seedIfNeeded() {
        val seededVersion = contentMetaDao.get(CONTENT_VERSION_KEY)?.value
        if (seededVersion == CURRENT_CONTENT_VERSION) return

        sectionDao.clearAll()
        unitDao.clearAll()
        exerciseDao.clearAll()

        ContentPackRegistry.assetPaths.forEach { assetPath ->
```

`ContentSeeder`'s parameter type (`contentLoader: ContentLoader`) does not need to change textually — it already reads as the interface name, Hilt will resolve it via the new binding added in Step 5.

- [ ] **Step 5: Add the Hilt binding**

In `RepositoryModule.kt`, add the import:

```kotlin
import com.zconte.oopsapp.data.content.AssetContentLoader
import com.zconte.oopsapp.data.content.ContentLoader
```

and add this binding inside `abstract class RepositoryModule`, alongside the existing `@Binds` methods:

```kotlin
    @Binds
    abstract fun bindContentLoader(impl: AssetContentLoader): ContentLoader
```

- [ ] **Step 6: Run the full suite to confirm nothing broke**

Run: `./gradlew testDebugUnitTest`
Expected: PASS, same test count as Step 1 (this task adds no new tests — it's a pure refactor).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentLoader.kt app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt
git commit -m "refactor: extract ContentLoader to an interface, centralize asset paths in ContentPackRegistry"
```

---

### Task 3: `ContentRepository.getUnitSummary` + `ContentRepositoryImpl` implementation

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/repository/ContentRepository.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/repository/ContentRepositoryImpl.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/testutil/FakeContentRepository.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/testutil/FakeContentLoader.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/data/repository/ContentRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `ContentLoader`, `ContentPackRegistry.assetPaths` (Task 2); `UnitSummary`, `UnitSummaryPack` (Task 1).
- Produces: `ContentRepository.getUnitSummary(unitId: String): UnitSummary?` — Task 4's `GetUnitSummaryUseCase` calls this directly. `FakeContentRepository(unitSummaries: Map<String, UnitSummary> = emptyMap())` — Task 4's test constructs this fake with a custom map.

- [ ] **Step 1: Add `FakeContentLoader` test double**

Create `app/src/test/java/com/zconte/oopsapp/testutil/FakeContentLoader.kt`:

```kotlin
package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.data.content.ContentLoader
import com.zconte.oopsapp.data.content.ContentPack

class FakeContentLoader(private val packsByPath: Map<String, ContentPack>) : ContentLoader {
    override fun loadPack(assetPath: String): ContentPack =
        packsByPath[assetPath] ?: error("No fake pack registered for path: $assetPath")
}
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/zconte/oopsapp/data/repository/ContentRepositoryImplTest.kt`:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.content.ContentPack
import com.zconte.oopsapp.data.content.ContentPackRegistry
import com.zconte.oopsapp.data.content.UnitPack
import com.zconte.oopsapp.data.content.UnitSummaryPack
import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.dao.UnitProgressDao
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity
import com.zconte.oopsapp.testutil.FakeContentLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class NoOpSectionDao : SectionDao {
    override suspend fun insertAll(sections: List<SectionEntity>) {}
    override suspend fun getAll(): List<SectionEntity> = emptyList()
    override suspend fun clearAll() {}
}

private class NoOpUnitDao : UnitDao {
    override suspend fun insertAll(units: List<UnitEntity>) {}
    override suspend fun getBySection(sectionId: String): List<UnitEntity> = emptyList()
    override suspend fun getAll(): List<UnitEntity> = emptyList()
    override suspend fun clearAll() {}
}

private class NoOpUnitProgressDao : UnitProgressDao {
    override suspend fun upsert(progress: UnitProgressEntity) {}
    override suspend fun getByUnit(unitId: String): UnitProgressEntity? = null
    override suspend fun getCompleted(): List<UnitProgressEntity> = emptyList()
}

class ContentRepositoryImplTest {

    private val paths = ContentPackRegistry.assetPaths

    private fun emptyPack(name: String) = ContentPack(
        sectionId = name, name = name, orderIndex = 0, examVersion = "core", units = emptyList()
    )

    private fun packWithUnit(name: String, unitId: String, unitName: String, summary: UnitSummaryPack?) = ContentPack(
        sectionId = name,
        name = name,
        orderIndex = 0,
        examVersion = "core",
        units = listOf(
            UnitPack(
                unitId = unitId,
                name = unitName,
                certObjective = "objective",
                orderIndex = 0,
                summary = summary,
                exercises = emptyList()
            )
        )
    )

    private fun createRepository(packsByPath: Map<String, ContentPack>) = ContentRepositoryImpl(
        sectionDao = NoOpSectionDao(),
        unitDao = NoOpUnitDao(),
        unitProgressDao = NoOpUnitProgressDao(),
        contentLoader = FakeContentLoader(packsByPath)
    )

    @Test
    fun `finds the summary in the first pack scanned`() = runTest {
        val packsByPath = paths.indices.associate { index ->
            paths[index] to if (index == 0) {
                packWithUnit("a", "target-unit", "Unidad objetivo", UnitSummaryPack("Texto A", "codigo A"))
            } else {
                emptyPack("p$index")
            }
        }
        val repository = createRepository(packsByPath)

        val summary = repository.getUnitSummary("target-unit")

        assertEquals("Unidad objetivo", summary?.unitName)
        assertEquals("Texto A", summary?.text)
        assertEquals("codigo A", summary?.code)
    }

    @Test
    fun `keeps scanning until it finds the unit in a later pack`() = runTest {
        val lastIndex = paths.lastIndex
        val packsByPath = paths.indices.associate { index ->
            paths[index] to if (index == lastIndex) {
                packWithUnit("z", "target-unit", "Unidad objetivo", UnitSummaryPack("Texto Z", null))
            } else {
                emptyPack("p$index")
            }
        }
        val repository = createRepository(packsByPath)

        val summary = repository.getUnitSummary("target-unit")

        assertEquals("Texto Z", summary?.text)
    }

    @Test
    fun `a unit with no summary field returns null`() = runTest {
        val packsByPath = paths.indices.associate { index ->
            paths[index] to if (index == 0) {
                packWithUnit("a", "target-unit", "Unidad objetivo", null)
            } else {
                emptyPack("p$index")
            }
        }
        val repository = createRepository(packsByPath)

        val summary = repository.getUnitSummary("target-unit")

        assertNull(summary)
    }

    @Test
    fun `an unknown unit id returns null`() = runTest {
        val packsByPath = paths.indices.associate { index -> paths[index] to emptyPack("p$index") }
        val repository = createRepository(packsByPath)

        val summary = repository.getUnitSummary("unknown-unit")

        assertNull(summary)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.repository.ContentRepositoryImplTest"`
Expected: compile error — `ContentRepositoryImpl` has no `contentLoader` parameter and `ContentRepository`/`ContentRepositoryImpl` have no `getUnitSummary` method yet.

- [ ] **Step 4: Add `getUnitSummary` to the domain interface**

In `ContentRepository.kt`, add the import:

```kotlin
import com.zconte.oopsapp.domain.model.UnitSummary
```

and add this method to the interface, after `markUnitCompleted`:

```kotlin
    suspend fun getUnitSummary(unitId: String): UnitSummary?
```

- [ ] **Step 5: Implement it in `ContentRepositoryImpl`**

Replace the full contents of `ContentRepositoryImpl.kt` with:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.content.ContentLoader
import com.zconte.oopsapp.data.content.ContentPackRegistry
import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.dao.UnitProgressDao
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity
import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.domain.repository.ContentRepository
import java.time.LocalDate
import javax.inject.Inject

class ContentRepositoryImpl @Inject constructor(
    private val sectionDao: SectionDao,
    private val unitDao: UnitDao,
    private val unitProgressDao: UnitProgressDao,
    private val contentLoader: ContentLoader
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

    override suspend fun getUnitSummary(unitId: String): UnitSummary? {
        for (assetPath in ContentPackRegistry.assetPaths) {
            val pack = contentLoader.loadPack(assetPath)
            val unitPack = pack.units.firstOrNull { it.unitId == unitId } ?: continue
            val summaryPack = unitPack.summary ?: return null
            return UnitSummary(unitName = unitPack.name, text = summaryPack.text, code = summaryPack.code)
        }
        return null
    }
}

private fun SectionEntity.toDomain() = Section(id, name, orderIndex, examVersion)

private fun UnitEntity.toDomain() = LearningUnit(id, sectionId, name, certObjective, orderIndex)
```

- [ ] **Step 6: Update `FakeContentRepository` to implement the new method**

In `FakeContentRepository.kt`, add the import:

```kotlin
import com.zconte.oopsapp.domain.model.UnitSummary
```

Replace:

```kotlin
class FakeContentRepository(
    private val sections: List<Section> = emptyList(),
    private val unitsBySection: Map<String, List<LearningUnit>> = emptyMap(),
    initialCompletedUnits: List<CompletedUnit> = emptyList()
) : ContentRepository {

    val completedUnits = initialCompletedUnits.toMutableList()

    override suspend fun getSections(): List<Section> = sections

    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()

    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits

    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {
        completedUnits.removeAll { it.unitId == unitId }
        completedUnits.add(CompletedUnit(unitId, via))
    }
}
```

with:

```kotlin
class FakeContentRepository(
    private val sections: List<Section> = emptyList(),
    private val unitsBySection: Map<String, List<LearningUnit>> = emptyMap(),
    initialCompletedUnits: List<CompletedUnit> = emptyList(),
    private val unitSummaries: Map<String, UnitSummary> = emptyMap()
) : ContentRepository {

    val completedUnits = initialCompletedUnits.toMutableList()

    override suspend fun getSections(): List<Section> = sections

    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()

    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits

    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {
        completedUnits.removeAll { it.unitId == unitId }
        completedUnits.add(CompletedUnit(unitId, via))
    }

    override suspend fun getUnitSummary(unitId: String): UnitSummary? = unitSummaries[unitId]
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.repository.ContentRepositoryImplTest"`
Expected: PASS (4 tests).

- [ ] **Step 8: Run the full suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS. This also confirms every existing call site of `FakeContentRepository` and every other implementer of `ContentRepository` still compiles with the new interface method.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/repository/ContentRepository.kt app/src/main/java/com/zconte/oopsapp/data/repository/ContentRepositoryImpl.kt app/src/test/java/com/zconte/oopsapp/testutil/FakeContentRepository.kt app/src/test/java/com/zconte/oopsapp/testutil/FakeContentLoader.kt app/src/test/java/com/zconte/oopsapp/data/repository/ContentRepositoryImplTest.kt
git commit -m "feat: implement ContentRepository.getUnitSummary by scanning content packs"
```

---

### Task 4: `GetUnitSummaryUseCase`

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetUnitSummaryUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSummaryUseCaseTest.kt`

**Interfaces:**
- Consumes: `ContentRepository.getUnitSummary` (Task 3), `FakeContentRepository(unitSummaries = ...)` (Task 3).
- Produces: `GetUnitSummaryUseCase(unitId: String): UnitSummary?` (`operator fun invoke`) — Task 5's `UnitSummaryViewModel` calls this.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSummaryUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.testutil.FakeContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetUnitSummaryUseCaseTest {

    @Test
    fun `returns the repository's summary for the given unit`() = runTest {
        val summary = UnitSummary(unitName = "Streams", text = "Texto", code = "codigo")
        val repository = FakeContentRepository(unitSummaries = mapOf("streams-creation" to summary))
        val useCase = GetUnitSummaryUseCase(repository)

        val result = useCase("streams-creation")

        assertEquals(summary, result)
    }

    @Test
    fun `returns null when the repository has no summary for that unit`() = runTest {
        val repository = FakeContentRepository()
        val useCase = GetUnitSummaryUseCase(repository)

        val result = useCase("unknown-unit")

        assertNull(result)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetUnitSummaryUseCaseTest"`
Expected: compile error — `GetUnitSummaryUseCase` does not exist yet.

- [ ] **Step 3: Implement the use case**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetUnitSummaryUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

class GetUnitSummaryUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(unitId: String): UnitSummary? =
        contentRepository.getUnitSummary(unitId)
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetUnitSummaryUseCaseTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Run the full suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetUnitSummaryUseCase.kt app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSummaryUseCaseTest.kt
git commit -m "feat: add GetUnitSummaryUseCase"
```

---

### Task 5: `UnitSummaryScreen` + `UnitSummaryViewModel` + navigation route

No new JVM tests in this task — Compose screens are verified by compilation and manual on-device QA in this codebase (Level 2 of the testing ADR has not started). Verify by building the app and (once Task 6 wires an entry point) by navigating to the screen manually.

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/ui/summary/UnitSummaryViewModel.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/summary/UnitSummaryScreen.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`

**Interfaces:**
- Consumes: `GetUnitSummaryUseCase` (Task 4); `CodeBlock(code: String, filledAnswer: String? = null, modifier: Modifier = Modifier)` (existing, `ui/components/CodeBlock.kt`, unchanged).
- Produces: route `unit_summary/{unitId}` — Task 6's "Ver resumen" button navigates here.

- [ ] **Step 1: Add the route constant**

In `OopsDestinations.kt`, add a new line inside the `object OopsDestinations` body, after `UNIT_SESSION`:

```kotlin
    const val UNIT_SUMMARY = "unit_summary/{unitId}"
```

- [ ] **Step 2: Create the ViewModel**

Create `app/src/main/java/com/zconte/oopsapp/ui/summary/UnitSummaryViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.domain.usecase.GetUnitSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnitSummaryUiState(
    val unitId: String = "",
    val summary: UnitSummary? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class UnitSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUnitSummaryUseCase: GetUnitSummaryUseCase
) : ViewModel() {

    private val unitId: String = checkNotNull(savedStateHandle["unitId"])

    private val _uiState = MutableStateFlow(UnitSummaryUiState(unitId = unitId))
    val uiState: StateFlow<UnitSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val summary = getUnitSummaryUseCase(unitId)
            _uiState.update { it.copy(summary = summary, isLoading = false) }
        }
    }
}
```

- [ ] **Step 3: Create the screen**

Create `app/src/main/java/com/zconte/oopsapp/ui/summary/UnitSummaryScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.ui.components.CodeBlock

@Composable
fun UnitSummaryScreen(
    onStartUnit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnitSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.summary

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> Text(
                    text = "Cargando...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                summary == null -> Text(
                    text = "Resumen no disponible todavia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                else -> {
                    Text(
                        text = summary.unitName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = summary.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    summary.code?.let { code ->
                        CodeBlock(code = code, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Button(
            onClick = { onStartUnit(uiState.unitId) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("COMENZAR UNIDAD", style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

- [ ] **Step 4: Wire the route in `OopsNavHost`**

In `OopsNavHost.kt`, add the import:

```kotlin
import com.zconte.oopsapp.ui.summary.UnitSummaryScreen
```

then find:

```kotlin
        composable(
            route = OopsDestinations.UNIT_SESSION,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType })
        ) {
            SessionScreen(
                onSessionComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = OopsDestinations.CHECKPOINT,
```

and replace it with (inserting the new `UNIT_SUMMARY` composable between the two existing ones):

```kotlin
        composable(
            route = OopsDestinations.UNIT_SESSION,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType })
        ) {
            SessionScreen(
                onSessionComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = OopsDestinations.UNIT_SUMMARY,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType })
        ) {
            UnitSummaryScreen(
                onStartUnit = { unitId ->
                    navController.navigate("unit_session/$unitId") {
                        popUpTo(OopsDestinations.PROGRESS)
                    }
                }
            )
        }
        composable(
            route = OopsDestinations.CHECKPOINT,
```

- [ ] **Step 5: Build to verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run the full JVM suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS, unchanged count (this task adds no new JVM tests).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/summary/UnitSummaryViewModel.kt app/src/main/java/com/zconte/oopsapp/ui/summary/UnitSummaryScreen.kt app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt
git commit -m "feat: add UnitSummaryScreen with a COMENZAR UNIDAD entry into the unit session"
```

---

### Task 6: "Ver resumen" affordance in `ProgressScreen`

No new JVM tests — same reasoning as Task 5. Manual on-device QA covers this (see the plan's final QA checklist).

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`

**Interfaces:**
- Consumes: route `unit_summary/{unitId}` (Task 5).
- Produces: nothing new for later tasks — this is the final UI wiring task.

- [ ] **Step 1: Thread `onOpenSummary` through `ProgressScreen` and `SectionPathBlock`**

In `ProgressScreen.kt`, replace the function signature:

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

with:

```kotlin
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    onOpenPlacementCheckpoint: (String) -> Unit,
    onOpenSummary: (String) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
```

Then replace the `SectionPathBlock` call inside the `LazyColumn`:

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

with:

```kotlin
            items(uiState.sections) { sectionPath ->
                SectionPathBlock(
                    sectionPath = sectionPath,
                    onPlayUnit = onPlayUnit,
                    onOpenCheckpoint = onOpenCheckpoint,
                    onOpenPlacementCheckpoint = onOpenPlacementCheckpoint,
                    onOpenSummary = onOpenSummary
                )
            }
```

- [ ] **Step 2: Thread it through `SectionPathBlock` into `UnitRow`**

Replace:

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
```

with:

```kotlin
@Composable
private fun SectionPathBlock(
    sectionPath: SectionPath,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    onOpenPlacementCheckpoint: (String) -> Unit,
    onOpenSummary: (String) -> Unit
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
                },
                onOpenSummary = onOpenSummary
            )
        }
```

- [ ] **Step 3: Add the affordance to `UnitRow`**

Replace:

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

with:

```kotlin
@Composable
private fun UnitRow(unitProgress: UnitProgress, onClick: () -> Unit, onOpenSummary: (String) -> Unit) {
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
        Column(modifier = Modifier.weight(1f)) {
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
        if (playable) {
            Text(
                text = "Ver resumen",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenSummary(unitProgress.unit.id) }
            )
        }
    }
}
```

`Modifier.weight(1f)` on the inner `Column` is required here — without it, adding the trailing "Ver resumen" text can overflow or crowd the row once a unit name is long, since the row no longer has just two children. The nested `clickable` on "Ver resumen" consumes its own tap before it reaches the outer row's `clickable`, so tapping it will not also trigger `onClick` — verify this by hand in Step 6 of the QA checklist below, since there is no Compose test covering it.

- [ ] **Step 4: Wire it in `OopsNavHost`**

In `OopsNavHost.kt`, replace the `OopsDestinations.PROGRESS` composable block:

```kotlin
        composable(OopsDestinations.PROGRESS) {
            ProgressScreen(
                onPlayUnit = { unitId -> navController.navigate("unit_session/$unitId") },
                onOpenCheckpoint = { sectionId -> navController.navigate("checkpoint/$sectionId") },
                onOpenPlacementCheckpoint = { targetUnitId -> navController.navigate("placement_checkpoint/$targetUnitId") }
            )
        }
```

with:

```kotlin
        composable(OopsDestinations.PROGRESS) {
            ProgressScreen(
                onPlayUnit = { unitId -> navController.navigate("unit_session/$unitId") },
                onOpenCheckpoint = { sectionId -> navController.navigate("checkpoint/$sectionId") },
                onOpenPlacementCheckpoint = { targetUnitId -> navController.navigate("placement_checkpoint/$targetUnitId") },
                onOpenSummary = { unitId -> navController.navigate("unit_summary/$unitId") }
            )
        }
```

- [ ] **Step 5: Build to verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run the full JVM suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS, unchanged count.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt
git commit -m "feat: add a Ver resumen affordance to playable units in Ver Ruta"
```

---

### Task 7: Content retrofit — write `summary` for all 19 existing units

**Files:**
- Modify: `app/src/main/assets/content/java-fundamentals.json`
- Modify: `app/src/main/assets/content/generics-collections.json`
- Modify: `app/src/main/assets/content/streams.json`
- Modify: `app/src/main/assets/content/exception-handling.json`
- Modify: `app/src/main/assets/content/concurrency.json`

**Interfaces:**
- Consumes: the `summary` JSON schema from Task 1.
- Produces: nothing for later tasks — this is the last task in the plan.

No TDD cycle here — this is content authoring, validated by JSON syntax + the full regression suite (which already exercises `ContentPack` deserialization against every real asset via `ContentSeeder`-adjacent tests) + manual on-device QA. Per the Global Constraints, do **not** bump `ContentSeeder.CURRENT_CONTENT_VERSION` for this task.

For every unit below, add a `summary` field immediately after its `orderIndex` line and before its `exercises` line — the `orderIndex` value combined with the unit's `unitId` line right above it is a unique anchor within each file. No accented characters in any of the added text, matching the rest of each file.

- [ ] **Step 1: `java-fundamentals.json` — 3 units**

Find, in the unit with `"unitId": "fund-what-is-java"`:

```json
      "unitId": "fund-what-is-java",
      "name": "Que es Java?",
      "certObjective": "language-basics",
      "orderIndex": 1,
      "exercises": [
```

Replace with:

```json
      "unitId": "fund-what-is-java",
      "name": "Que es Java?",
      "certObjective": "language-basics",
      "orderIndex": 1,
      "summary": {
        "text": "Java es un lenguaje compilado a bytecode, no a codigo nativo. El compilador javac convierte tus archivos .java en archivos .class con bytecode. Ese bytecode lo ejecuta la JVM (Java Virtual Machine), lo que permite que el mismo .class corra en cualquier sistema operativo con JVM instalada. El JDK incluye el compilador y las herramientas de desarrollo; el JRE solo incluye lo necesario para ejecutar programas ya compilados.",
        "code": "javac Main.java\njava Main"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "fund-class-structure"`:

```json
      "unitId": "fund-class-structure",
      "name": "Estructura de una clase",
      "certObjective": "language-basics",
      "orderIndex": 2,
      "exercises": [
```

Replace with:

```json
      "unitId": "fund-class-structure",
      "name": "Estructura de una clase",
      "certObjective": "language-basics",
      "orderIndex": 2,
      "summary": {
        "text": "Una clase en Java agrupa fields (el estado) y metodos (el comportamiento). El constructor es un metodo especial, sin tipo de retorno y con el mismo nombre que la clase, que se ejecuta al crear un objeto con new. Un archivo .java puede tener varias clases, pero solo una puede ser public y su nombre debe coincidir con el del archivo.",
        "code": "public class Persona {\n    private String nombre;\n\n    public Persona(String nombre) {\n        this.nombre = nombre;\n    }\n\n    public String getNombre() {\n        return nombre;\n    }\n}"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "fund-types-and-main"`:

```json
      "unitId": "fund-types-and-main",
      "name": "Tipos, variables y el metodo main",
      "certObjective": "language-basics",
      "orderIndex": 3,
      "exercises": [
```

Replace with:

```json
      "unitId": "fund-types-and-main",
      "name": "Tipos, variables y el metodo main",
      "certObjective": "language-basics",
      "orderIndex": 3,
      "summary": {
        "text": "El metodo main es el punto de entrada de cualquier programa Java: debe ser public static void main(String[] args) para que la JVM lo pueda invocar sin crear un objeto primero (por eso es static). Java distingue entre tipos primitivos (int, boolean, double...) que guardan el valor directamente, y sus wrappers (Integer, Boolean, Double...) que son objetos y permiten null.",
        "code": "public static void main(String[] args) {\n    int edad = 30;\n    Integer edadObjeto = edad;\n}"
      },
      "exercises": [
```

If any `orderIndex`/`certObjective` value above does not match the file exactly, re-check the current content of `java-fundamentals.json` before editing — the anchor must match verbatim or the edit will silently target the wrong unit or fail to apply.

- [ ] **Step 2: Run the JSON validator on `java-fundamentals.json`**

Run: `python3 -m json.tool app/src/main/assets/content/java-fundamentals.json > /dev/null`
Expected: no output, exit code 0 (valid JSON). If it fails, fix the syntax error before continuing.

- [ ] **Step 3: `generics-collections.json` — 4 units**

Find, in the unit with `"unitId": "gencol-generics"`:

```json
      "unitId": "gencol-generics",
      "name": "Generics",
      "certObjective": "generics-collections",
      "orderIndex": 1,
      "exercises": [
```

Replace with:

```json
      "unitId": "gencol-generics",
      "name": "Generics",
      "certObjective": "generics-collections",
      "orderIndex": 1,
      "summary": {
        "text": "Los Generics permiten escribir clases y metodos que trabajan con cualquier tipo, sin perder seguridad de tipos en tiempo de compilacion (a diferencia de usar Object, que obliga a castear y puede fallar en runtime). Un wildcard como <? extends Number> dice que el tipo puede ser Number o cualquier subclase, util cuando solo necesitas leer valores.",
        "code": "class Caja<T> {\n    private T contenido;\n\n    void guardar(T valor) {\n        contenido = valor;\n    }\n\n    T obtener() {\n        return contenido;\n    }\n}"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "gencol-lists-sets"`:

```json
      "unitId": "gencol-lists-sets",
      "name": "Listas y Sets",
      "certObjective": "generics-collections",
      "orderIndex": 2,
      "exercises": [
```

Replace with:

```json
      "unitId": "gencol-lists-sets",
      "name": "Listas y Sets",
      "certObjective": "generics-collections",
      "orderIndex": 2,
      "summary": {
        "text": "Una List mantiene el orden de insercion y permite duplicados; un Set no garantiza orden (salvo LinkedHashSet o TreeSet) y nunca permite duplicados. ArrayList es mejor para acceso aleatorio por indice; LinkedList conviene cuando insertas o eliminas mucho en los extremos, porque no necesita desplazar elementos.",
        "code": "List<String> nombres = new ArrayList<>();\nnombres.add(\"Ana\");\nnombres.add(\"Ana\");\n\nSet<String> unicos = new HashSet<>(nombres);"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "gencol-maps-deques"`:

```json
      "unitId": "gencol-maps-deques",
      "name": "Maps y Deques",
      "certObjective": "generics-collections",
      "orderIndex": 3,
      "exercises": [
```

Replace with:

```json
      "unitId": "gencol-maps-deques",
      "name": "Maps y Deques",
      "certObjective": "generics-collections",
      "orderIndex": 3,
      "summary": {
        "text": "Un Map guarda pares clave-valor, sin duplicar claves. HashMap no garantiza orden; TreeMap mantiene las claves ordenadas automaticamente. Un Deque (double-ended queue) permite agregar y quitar elementos tanto al inicio como al final, algo que una List soporta pero de forma menos eficiente y menos explicita.",
        "code": "Map<String, Integer> edades = new TreeMap<>();\nedades.put(\"Ana\", 30);\n\nDeque<String> pila = new ArrayDeque<>();\npila.addFirst(\"primero\");"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "gencol-comparators-immutable"`:

```json
      "unitId": "gencol-comparators-immutable",
      "name": "Comparadores y colecciones inmutables",
      "certObjective": "generics-collections",
      "orderIndex": 4,
      "exercises": [
```

Replace with:

```json
      "unitId": "gencol-comparators-immutable",
      "name": "Comparadores y colecciones inmutables",
      "certObjective": "generics-collections",
      "orderIndex": 4,
      "summary": {
        "text": "Comparable define un orden natural dentro de la propia clase (metodo compareTo); Comparator define un orden externo y reutilizable, util cuando necesitas varias formas distintas de ordenar el mismo tipo. List.of(...) y sus equivalentes para Set y Map crean colecciones inmutables: cualquier intento de modificarlas lanza UnsupportedOperationException.",
        "code": "List<String> nombres = new ArrayList<>(List.of(\"Beto\", \"Ana\"));\nnombres.sort(Comparator.naturalOrder());\n\nList<String> fija = List.of(\"Ana\", \"Beto\");"
      },
      "exercises": [
```

If any `orderIndex` value above does not match the file exactly, re-check the current content of `generics-collections.json` before editing.

- [ ] **Step 4: Run the JSON validator on `generics-collections.json`**

Run: `python3 -m json.tool app/src/main/assets/content/generics-collections.json > /dev/null`
Expected: no output, exit code 0.

- [ ] **Step 5: `streams.json` — 4 units**

Find, in the unit with `"unitId": "streams-creation"`:

```json
      "unitId": "streams-creation",
      "name": "Creacion de streams",
      "certObjective": "streams-lambdas",
      "orderIndex": 1,
      "exercises": [
```

Replace with:

```json
      "unitId": "streams-creation",
      "name": "Creacion de streams",
      "certObjective": "streams-lambdas",
      "orderIndex": 1,
      "summary": {
        "text": "Un Stream se crea a partir de una fuente de datos, como una List (con .stream()) o un rango de numeros (con IntStream.range). Un Stream no almacena datos ni los modifica: describe una secuencia de operaciones que se ejecutan solo cuando llega una operacion terminal.",
        "code": "List<Integer> numeros = List.of(1, 2, 3);\nnumeros.stream();\n\nIntStream.range(1, 10);"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "streams-intermediate"`:

```json
      "unitId": "streams-intermediate",
      "name": "Operaciones intermedias",
      "certObjective": "streams-lambdas",
      "orderIndex": 2,
      "exercises": [
```

Replace with:

```json
      "unitId": "streams-intermediate",
      "name": "Operaciones intermedias",
      "certObjective": "streams-lambdas",
      "orderIndex": 2,
      "summary": {
        "text": "Las operaciones intermedias transforman un Stream en otro Stream y son perezosas (lazy): no hacen nada hasta que llega una operacion terminal. filter selecciona elementos, map los transforma, distinct quita duplicados, sorted ordena, limit y skip recortan la secuencia, y flatMap aplana un Stream de streams (o de listas) en un unico Stream.",
        "code": "lista.stream()\n    .filter(n -> n % 2 == 0)\n    .map(n -> n * 2)\n    .distinct()\n    .sorted()\n    .limit(5);"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "streams-terminal"`:

```json
      "unitId": "streams-terminal",
      "name": "Operaciones terminales",
      "certObjective": "streams-lambdas",
      "orderIndex": 3,
      "exercises": [
```

Replace with:

```json
      "unitId": "streams-terminal",
      "name": "Operaciones terminales",
      "certObjective": "streams-lambdas",
      "orderIndex": 3,
      "summary": {
        "text": "Las operaciones terminales cierran el Stream y producen un resultado: collect junta los elementos en una coleccion, count los cuenta, reduce los combina en un unico valor con un acumulador. anyMatch, allMatch y noneMatch verifican condiciones sobre los elementos, y min/max obtienen el extremo segun un Comparator.",
        "code": "long total = lista.stream()\n    .filter(n -> n > 0)\n    .count();\n\nboolean hayNegativos = lista.stream().anyMatch(n -> n < 0);"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "streams-collectors"`:

```json
      "unitId": "streams-collectors",
      "name": "Collectors avanzados",
      "certObjective": "streams-lambdas",
      "orderIndex": 4,
      "exercises": [
```

Replace with:

```json
      "unitId": "streams-collectors",
      "name": "Collectors avanzados",
      "certObjective": "streams-lambdas",
      "orderIndex": 4,
      "summary": {
        "text": "Collectors ofrece formas listas para usar al acumular un Stream con collect(). Collectors.joining() une los elementos en un String con un separador. Collectors.groupingBy() agrupa los elementos segun una propiedad derivada, devolviendo un Map donde cada clave apunta a la lista de elementos de ese grupo.",
        "code": "String unidos = palabras.stream()\n    .collect(Collectors.joining(\", \"));\n\nMap<Integer, List<String>> porLongitud = palabras.stream()\n    .collect(Collectors.groupingBy(String::length));"
      },
      "exercises": [
```

If any `orderIndex` value above does not match the file exactly, re-check the current content of `streams.json` before editing.

- [ ] **Step 6: Run the JSON validator on `streams.json`**

Run: `python3 -m json.tool app/src/main/assets/content/streams.json > /dev/null`
Expected: no output, exit code 0.

- [ ] **Step 7: `exception-handling.json` — 4 units**

Find, in the unit with `"unitId": "excep-jerarquia"`:

```json
      "unitId": "excep-jerarquia",
      "name": "Jerarquia de excepciones",
      "certObjective": "exception-handling",
      "orderIndex": 1,
      "exercises": [
```

Replace with:

```json
      "unitId": "excep-jerarquia",
      "name": "Jerarquia de excepciones",
      "certObjective": "exception-handling",
      "orderIndex": 1,
      "summary": {
        "text": "Throwable es la superclase de todo lo que se puede lanzar en Java: se divide en Exception y Error. Las excepciones checked (que extienden Exception directamente) obligan a declararlas o capturarlas en tiempo de compilacion; las unchecked (que extienden RuntimeException) no lo exigen, porque suelen representar errores de programacion. Error representa problemas graves del entorno, como OutOfMemoryError, que normalmente no se capturan.",
        "code": "class MiExcepcionChecked extends Exception {\n    MiExcepcionChecked(String mensaje) {\n        super(mensaje);\n    }\n}"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "excep-try-catch-finally"`:

```json
      "unitId": "excep-try-catch-finally",
      "name": "Try-catch-finally y multi-catch",
      "certObjective": "exception-handling",
      "orderIndex": 2,
      "exercises": [
```

Replace with:

```json
      "unitId": "excep-try-catch-finally",
      "name": "Try-catch-finally y multi-catch",
      "certObjective": "exception-handling",
      "orderIndex": 2,
      "summary": {
        "text": "Un bloque try puede tener varios catch, o un multi-catch (catch (IOException | SQLException e)) cuando quieres manejar varios tipos igual, siempre que ninguno sea subtipo del otro. finally se ejecuta siempre, haya o no excepcion, salvo que el programa termine antes con System.exit().",
        "code": "try {\n    leerArchivo();\n} catch (IOException | SQLException e) {\n    System.out.println(\"Error: \" + e.getMessage());\n} finally {\n    System.out.println(\"Limpieza\");\n}"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "excep-try-with-resources"`:

```json
      "unitId": "excep-try-with-resources",
      "name": "Try-with-resources",
      "certObjective": "exception-handling",
      "orderIndex": 3,
      "exercises": [
```

Replace with:

```json
      "unitId": "excep-try-with-resources",
      "name": "Try-with-resources",
      "certObjective": "exception-handling",
      "orderIndex": 3,
      "summary": {
        "text": "try-with-resources cierra automaticamente cualquier recurso que implemente AutoCloseable al salir del bloque try, sin necesitar un finally manual. Si tanto el bloque try como el close() automatico lanzan una excepcion, la del try es la que se propaga; la del close() queda registrada como suppressed.",
        "code": "try (BufferedReader lector = new BufferedReader(new FileReader(\"datos.txt\"))) {\n    System.out.println(lector.readLine());\n}"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "excep-personalizadas"`:

```json
      "unitId": "excep-personalizadas",
      "name": "Excepciones personalizadas y encadenamiento",
      "certObjective": "exception-handling",
      "orderIndex": 4,
      "exercises": [
```

Replace with:

```json
      "unitId": "excep-personalizadas",
      "name": "Excepciones personalizadas y encadenamiento",
      "certObjective": "exception-handling",
      "orderIndex": 4,
      "summary": {
        "text": "Conviene crear una excepcion personalizada cuando el llamador necesita distinguir tu error especifico de otros errores genericos, o cuando quieres agregar datos propios al error. Al envolver una excepcion de bajo nivel en una de mas alto nivel, pasala como causa (throw new MiExcepcion(mensaje, original)) para no perder la traza original.",
        "code": "class SaldoInsuficienteException extends RuntimeException {\n    SaldoInsuficienteException(String mensaje, Throwable causa) {\n        super(mensaje, causa);\n    }\n}"
      },
      "exercises": [
```

If any `orderIndex` value above does not match the file exactly, re-check the current content of `exception-handling.json` before editing.

- [ ] **Step 8: Run the JSON validator on `exception-handling.json`**

Run: `python3 -m json.tool app/src/main/assets/content/exception-handling.json > /dev/null`
Expected: no output, exit code 0.

- [ ] **Step 9: `concurrency.json` — 4 units**

Find, in the unit with `"unitId": "conc-threads-lifecycle"`:

```json
      "unitId": "conc-threads-lifecycle",
      "name": "Threads y ciclo de vida",
      "certObjective": "concurrency",
      "orderIndex": 1,
      "exercises": [
```

Replace with:

```json
      "unitId": "conc-threads-lifecycle",
      "name": "Threads y ciclo de vida",
      "certObjective": "concurrency",
      "orderIndex": 1,
      "summary": {
        "text": "Llamar start() en un Thread crea un hilo nuevo y ejecuta run() en el; llamar run() directamente solo ejecuta ese codigo en el hilo actual, sin concurrencia real. Thread.sleep() pone al hilo en estado TIMED_WAITING sin liberar los locks que tenga. Un hilo que intenta entrar a un bloque synchronized ocupado por otro queda BLOCKED hasta que el otro lo libera.",
        "code": "Thread hilo = new Thread(() -> System.out.println(\"trabajando\"));\nhilo.start();\nhilo.join();"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "conc-executors"`:

```json
      "unitId": "conc-executors",
      "name": "Executors y thread pools",
      "certObjective": "concurrency",
      "orderIndex": 2,
      "exercises": [
```

Replace with:

```json
      "unitId": "conc-executors",
      "name": "Executors y thread pools",
      "certObjective": "concurrency",
      "orderIndex": 2,
      "summary": {
        "text": "Un ExecutorService administra un pool de hilos reutilizables, evitando el costo de crear un hilo nuevo por cada tarea. execute() no devuelve nada; submit() devuelve un Future, util cuando la tarea es un Callable que retorna un valor o puede lanzar una excepcion. shutdown() deja terminar las tareas en curso antes de cerrar; shutdownNow() intenta interrumpirlas de inmediato.",
        "code": "ExecutorService pool = Executors.newFixedThreadPool(4);\nFuture<Integer> resultado = pool.submit(() -> 42);\npool.shutdown();"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "conc-sincronizacion"`:

```json
      "unitId": "conc-sincronizacion",
      "name": "Sincronizacion",
      "certObjective": "concurrency",
      "orderIndex": 3,
      "exercises": [
```

Replace with:

```json
      "unitId": "conc-sincronizacion",
      "name": "Sincronizacion",
      "certObjective": "concurrency",
      "orderIndex": 3,
      "summary": {
        "text": "Una race condition ocurre cuando varios hilos leen y modifican el mismo dato compartido sin coordinacion, y el resultado final depende del orden en que se intercalen. La palabra clave synchronized asegura que solo un hilo a la vez ejecute ese metodo o bloque. ReentrantLock ofrece lo mismo pero de forma explicita, con la ventaja de poder intentar el lock con timeout; siempre se libera en un finally.",
        "code": "private final ReentrantLock lock = new ReentrantLock();\n\nvoid incrementar() {\n    lock.lock();\n    try {\n        contador++;\n    } finally {\n        lock.unlock();\n    }\n}"
      },
      "exercises": [
```

Find, in the unit with `"unitId": "conc-virtual-threads"`:

```json
      "unitId": "conc-virtual-threads",
      "name": "Virtual threads y colecciones concurrentes",
      "certObjective": "concurrency",
      "orderIndex": 4,
      "exercises": [
```

Replace with:

```json
      "unitId": "conc-virtual-threads",
      "name": "Virtual threads y colecciones concurrentes",
      "certObjective": "concurrency",
      "orderIndex": 4,
      "summary": {
        "text": "Un virtual thread (Java 21) es un hilo liviano administrado por la JVM, no por el sistema operativo, lo que permite crear miles sin agotar recursos. Executors.newVirtualThreadPerTaskExecutor() crea un virtual thread nuevo por cada tarea enviada. Cuando varios hilos acceden al mismo Map, ConcurrentHashMap evita corrupcion de datos sin bloquear todo el mapa como haria sincronizar un HashMap manualmente; AtomicInteger permite incrementar un contador de forma segura entre hilos sin necesitar synchronized.",
        "code": "try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {\n    executor.submit(() -> System.out.println(\"tarea liviana\"));\n}"
      },
      "exercises": [
```

If any `orderIndex` value above does not match the file exactly, re-check the current content of `concurrency.json` before editing.

- [ ] **Step 10: Run the JSON validator on `concurrency.json`**

Run: `python3 -m json.tool app/src/main/assets/content/concurrency.json > /dev/null`
Expected: no output, exit code 0.

- [ ] **Step 11: Check for accented characters across all 5 files**

Run: `grep -n '[áéíóúñÁÉÍÓÚÑ]' app/src/main/assets/content/java-fundamentals.json app/src/main/assets/content/generics-collections.json app/src/main/assets/content/streams.json app/src/main/assets/content/exception-handling.json app/src/main/assets/content/concurrency.json`
Expected: no output (no matches). If any line matches, replace the accented character with its unaccented equivalent to match the established content convention.

- [ ] **Step 12: Run the full JVM suite as a final regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS, same test count as after Task 6 (this task only edits JSON assets, adding no new JVM tests, and does not touch `CURRENT_CONTENT_VERSION`).

- [ ] **Step 13: Commit**

```bash
git add app/src/main/assets/content/java-fundamentals.json app/src/main/assets/content/generics-collections.json app/src/main/assets/content/streams.json app/src/main/assets/content/exception-handling.json app/src/main/assets/content/concurrency.json
git commit -m "content: add unit summaries for all 19 existing units"
```

---

## Manual On-Device QA (after all 7 tasks are merged)

- Install the app (update install is fine — no Room migration, no `CURRENT_CONTENT_VERSION` bump, so existing progress is untouched).
- In Ver Ruta, confirm "Ver resumen" appears next to every unlocked or completed unit, and does **not** appear next to a locked unit.
- Tap "Ver resumen" on an unlocked unit and confirm it does **not** also trigger "jugar" / the placement checkpoint (the nested-clickable concern from Task 6, Step 3).
- Open the summary for at least one unit per section (5 sections) and confirm the text and the code block (with syntax highlighting) both render correctly.
- Tap "COMENZAR UNIDAD" and confirm it opens that unit's session correctly.
- From inside the session, go back and confirm it does **not** return to the summary screen (it should pop straight back to Ver Ruta, per the `popUpTo(OopsDestinations.PROGRESS)` in Task 5).
