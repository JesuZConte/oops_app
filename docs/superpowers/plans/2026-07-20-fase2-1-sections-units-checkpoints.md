# Fase 2.1 — Secciones, Unidades y Checkpoint de repaso Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el modelo plano `Topic → Exercise` por una jerarquía **Sección → Unidad → Ejercicio**, probada end-to-end con dos secciones reales (Fundamentos de Java nuevo, Streams reorganizado), con progresión por unidad (primera pasada) y un checkpoint de repaso voluntario que alimenta SM-2 — sin perder el progreso SM-2 ni las estadísticas de usuario ya existentes.

**Architecture:** El contenido (secciones/unidades/ejercicios) se trata como derivado y re-sembrable desde `assets/content/`; el progreso del usuario (`review_state`, `user_stats`) es el dato precioso que la migración nunca toca. La migración Room v1→v2 solo hace DDL sobre las tablas de contenido (drop+recreate); un `ContentSeeder` versionado (no un guard de `count() > 0`) repuebla sections/units/exercises en cada arranque cuando detecta una versión de contenido nueva.

**Tech Stack:** Kotlin, Room (migración manual + `MigrationTestHelper`), Hilt, kotlinx.serialization, Jetpack Compose.

## Global Constraints

- **Umbral de aprobación del checkpoint: 68%** (`scorePct >= 68`), igual al examen real 1Z0-830.
- **Alcance de esta ronda (2.1): solo el checkpoint de *repaso voluntario*** (fin de sección, no bloqueante). El checkpoint de *ubicación* (saltar contenido con siembra híbrida de SM-2) queda diferido a una Fase 2.1b — no implementar en este plan.
- **Composición del checkpoint de repaso:** tamaño objetivo 12 preguntas. Hasta 3 de secciones anteriores ya completadas (si existen), el resto (9-12) de la sección recién completada, acotado por cuántos ejercicios existan realmente en cada pool. Si no hay secciones anteriores (caso Fundamentos, la primera), el checkpoint es 100% de la sección actual.
- **Las respuestas dentro de un checkpoint alimentan SM-2** exactamente igual que en la sesión diaria (vía `SubmitAnswerUseCase`) — el checkpoint no es una evaluación aislada, es una sesión temática más. El pass/fail al 68% es un cálculo agregado por encima, no un mecanismo alternativo.
- **Una Unidad se completa por "primera pasada":** cuando el jugador respondió todos sus ejercicios al menos una vez (existe una fila en `review_state` para cada `exerciseId` de la unidad). No requiere ningún nivel de dominio SM-2 — el dominio real sigue creciendo aparte.
- **El contenido es descartable y re-sembrable; el progreso del usuario no.** La migración Room (`MIGRATION_1_2`) solo crea/borra tablas de **contenido** (`sections`, `units`, `exercises` — drop del `exercises` v1 y `topics`, create de las tablas v2) y nunca ejecuta DDL ni DML sobre `review_state` o `user_stats`. No existe ninguna `@ForeignKey` desde `review_state` hacia `exercises` hoy — **no agregar una** en este plan, o el drop/recreate de `exercises` borraría en cascada el progreso SM-2 del usuario.
- **`ContentSeeder` usa un guard de versión de contenido (`content_meta`), no `exerciseDao.count() > 0`.** Los ids de ejercicio existentes de Streams (`streams-01`..`streams-20`) se preservan exactamente — la re-siembra solo les reasigna `unitId`, nunca cambia su `id`, para que su `review_state` (referenciado por `exerciseId`) siga resolviendo correctamente tras la migración.
- **No se agregan tipos de ejercicio nuevos al modelo de datos.** La diversidad teórico/práctico/lectura-de-código del ADR se logra con `type = "mcq"` variando el `prompt`/`code` (ya soportado por `SessionScreen` hoy sin cambios) — confirmado leyendo `SessionScreen.kt`: solo distingue `mcq` de "todo lo demás".
- **`examVersion`** por sección (heredado por todos sus ejercicios, sin override por ejercicio en esta ronda): `"core"` para Fundamentos, `"java21"` para Streams (es contenido examinable en el 1Z0-830, aunque la API exista desde Java 8).
- No se toca `SessionViewModel`'s completion path existente (orden `pendingAnswerJob?.join()` antes de `updateStreakUseCase`) más allá de generalizar qué use case puebla la cola inicial — la lógica de puntaje/pass-fail del checkpoint vive en un ViewModel separado, no mezclada con ese flujo ya probado.
- No inventar mockups visuales para el camino Sección→Unidad de Ruta — no existe diseño para esto. Reusar el lenguaje visual arcade ya construido (`ThemedCard`, colores de `OopsExtendedColors`, patrón de nodo bloqueado ya usado en `ProgressScreen.kt`).

---

### Task 1: Esquema Room v2 (entidades, DAOs, migración pura-DDL)

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/SectionEntity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/UnitEntity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/UnitProgressEntity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/CheckpointAttemptEntity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/ContentMetaEntity.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/entity/ExerciseEntity.kt`
- Delete: `app/src/main/java/com/zconte/oopsapp/data/local/entity/TopicEntity.kt`
- Delete: `app/src/main/java/com/zconte/oopsapp/data/local/dao/TopicDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/dao/SectionDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/dao/UnitDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/dao/UnitProgressDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/dao/CheckpointAttemptDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/dao/ContentMetaDao.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/dao/ReviewStateDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/Migrations.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt`
- Modify: `app/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Produces: `SectionEntity(id, name, orderIndex, examVersion)`, `UnitEntity(id, sectionId, name, certObjective, orderIndex)`, `ExerciseEntity(id, unitId, type, payload, difficulty, examVersion)` (renamed field `topicId`→`unitId`, added `examVersion`), `UnitProgressEntity(unitId, completed, completedAt)`, `CheckpointAttemptEntity(id, sectionId, kind, scorePct, passed, takenAt)`, `ContentMetaEntity(configKey, value)`. `MIGRATION_1_2: Migration`. DAOs: `SectionDao`, `UnitDao`, `UnitProgressDao`, `CheckpointAttemptDao`, `ContentMetaDao`, updated `ExerciseDao`/`ReviewStateDao`. Consumed by Task 2 (content seeding), Task 4 (migration test), Task 5 (domain/repositories).

- [ ] **Step 1: Add the new entities**

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/SectionEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val orderIndex: Int,
    val examVersion: String
)
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/UnitEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int
)
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/UnitProgressEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_progress")
data class UnitProgressEntity(
    @PrimaryKey val unitId: String,
    val completed: Boolean,
    val completedAt: Long?
)
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/CheckpointAttemptEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkpoint_attempts")
data class CheckpointAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: String,
    val kind: String,
    val scorePct: Int,
    val passed: Boolean,
    val takenAt: Long
)
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/ContentMetaEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_meta")
data class ContentMetaEntity(
    @PrimaryKey val configKey: String,
    val value: String
)
```

- [ ] **Step 2: Update ExerciseEntity, delete TopicEntity**

Replace `app/src/main/java/com/zconte/oopsapp/data/local/entity/ExerciseEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val type: String,
    val payload: String,
    val difficulty: Int,
    val examVersion: String
)
```

Delete `app/src/main/java/com/zconte/oopsapp/data/local/entity/TopicEntity.kt` and `app/src/main/java/com/zconte/oopsapp/data/local/dao/TopicDao.kt`.

- [ ] **Step 3: New DAOs**

Create `app/src/main/java/com/zconte/oopsapp/data/local/dao/SectionDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.SectionEntity

@Dao
interface SectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<SectionEntity>)

    @Query("SELECT * FROM sections ORDER BY orderIndex")
    suspend fun getAll(): List<SectionEntity>

    @Query("DELETE FROM sections")
    suspend fun clearAll()
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/dao/UnitDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.UnitEntity

@Dao
interface UnitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<UnitEntity>)

    @Query("SELECT * FROM units WHERE sectionId = :sectionId ORDER BY orderIndex")
    suspend fun getBySection(sectionId: String): List<UnitEntity>

    @Query("SELECT * FROM units ORDER BY orderIndex")
    suspend fun getAll(): List<UnitEntity>

    @Query("DELETE FROM units")
    suspend fun clearAll()
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/dao/UnitProgressDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity

@Dao
interface UnitProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UnitProgressEntity)

    @Query("SELECT * FROM unit_progress WHERE unitId = :unitId")
    suspend fun getByUnit(unitId: String): UnitProgressEntity?

    @Query("SELECT unitId FROM unit_progress WHERE completed = 1")
    suspend fun getCompletedUnitIds(): List<String>
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/dao/CheckpointAttemptDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity

@Dao
interface CheckpointAttemptDao {
    @Insert
    suspend fun insert(attempt: CheckpointAttemptEntity)

    @Query("SELECT * FROM checkpoint_attempts WHERE sectionId = :sectionId ORDER BY takenAt DESC")
    suspend fun getBySection(sectionId: String): List<CheckpointAttemptEntity>
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/dao/ContentMetaDao.kt`:

```kotlin
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
```

- [ ] **Step 4: Update ExerciseDao and ReviewStateDao for the unitId/units rename**

Replace `app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.ExerciseEntity

data class ObjectiveTotalCount(val objective: String, val totalCount: Int)

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("DELETE FROM exercises")
    suspend fun clearAll()

    @Query(
        """
        SELECT exercises.* FROM exercises
        INNER JOIN review_state ON exercises.id = review_state.exerciseId
        WHERE review_state.dueDate <= :today
        """
    )
    suspend fun getDue(today: Long): List<ExerciseEntity>

    @Query(
        """
        SELECT * FROM exercises
        WHERE id NOT IN (SELECT exerciseId FROM review_state)
        LIMIT :limit
        """
    )
    suspend fun getNew(limit: Int): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE unitId = :unitId")
    suspend fun getByUnit(unitId: String): List<ExerciseEntity>

    @Query(
        """
        SELECT exercises.* FROM exercises
        INNER JOIN units ON exercises.unitId = units.id
        WHERE units.sectionId = :sectionId
        """
    )
    suspend fun getBySection(sectionId: String): List<ExerciseEntity>

    @Query(
        """
        SELECT units.certObjective AS objective, COUNT(*) AS totalCount
        FROM exercises
        INNER JOIN units ON exercises.unitId = units.id
        GROUP BY units.certObjective
        """
    )
    suspend fun getTotalCountByObjective(): List<ObjectiveTotalCount>
}
```

Replace `app/src/main/java/com/zconte/oopsapp/data/local/dao/ReviewStateDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity

data class ObjectiveMasteryCount(val objective: String, val masteredCount: Int)

@Dao
interface ReviewStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReviewStateEntity)

    @Query("SELECT * FROM review_state WHERE exerciseId = :exerciseId")
    suspend fun getByExerciseId(exerciseId: String): ReviewStateEntity?

    @Query("SELECT exerciseId FROM review_state WHERE exerciseId IN (:exerciseIds)")
    suspend fun getExistingIds(exerciseIds: List<String>): List<String>

    @Query(
        """
        SELECT units.certObjective AS objective, COUNT(*) AS masteredCount
        FROM review_state
        INNER JOIN exercises ON review_state.exerciseId = exercises.id
        INNER JOIN units ON exercises.unitId = units.id
        WHERE review_state.repetitions >= 2
        GROUP BY units.certObjective
        """
    )
    suspend fun getMasteredCountByObjective(): List<ObjectiveMasteryCount>
}
```

`ReviewStateEntity` and `UserStatsEntity` are unchanged — do not touch those two files.

- [ ] **Step 5: Write the pure-DDL migration**

Create `app/src/main/java/com/zconte/oopsapp/data/local/Migrations.kt`:

```kotlin
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
```

- [ ] **Step 6: Update AppDatabase**

Replace `app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt`:

```kotlin
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
```

- [ ] **Step 7: Wire the migration and new DAOs in DI**

Replace `app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt`:

```kotlin
package com.zconte.oopsapp.di

import android.content.Context
import androidx.room.Room
import com.zconte.oopsapp.data.local.AppDatabase
import com.zconte.oopsapp.data.local.MIGRATION_1_2
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
            .addMigrations(MIGRATION_1_2)
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
```

- [ ] **Step 8: Add room-testing dependency and configure schema export**

In `gradle/libs.versions.toml`, find the line `androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }` and add immediately after it:

```toml
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
```

In `app/build.gradle.kts`, add the KSP schema export arg. Find:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}
```

Add immediately after this block (top level, alongside `android { ... }`):

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

In the `dependencies { ... }` block, find `androidTestImplementation(libs.androidx.espresso.core)` and add immediately after it:

```kotlin
    androidTestImplementation(libs.androidx.room.testing)
```

- [ ] **Step 9: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. The build will fail to reference `TopicDao`/`TopicEntity` from any remaining call site — that's expected and resolved in Tasks 2 and 5. If this task's own files don't compile in isolation (e.g., `ContentMapper.kt`, `ContentSeeder.kt`, `ExerciseRepositoryImpl.kt`, `ProgressRepositoryImpl.kt` still reference the old shape), that's fine — Task 2 and Task 5 fix those. Confirm no errors originate from the files this task itself created or modified.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/local/ app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt app/build.gradle.kts gradle/libs.versions.toml
git commit -m "Room v2 schema: Section/Unit hierarchy, pure-DDL migration preserving review_state/user_stats"
```

---

### Task 2: Formato de contenido v2 + ContentSeeder versionado

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentPack.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentMapper.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/data/content/ContentMapperTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt`

**Interfaces:**
- Consumes: `SectionEntity`, `UnitEntity`, `ExerciseEntity`, `ContentMetaEntity`, `SectionDao`, `UnitDao`, `ExerciseDao`, `ContentMetaDao` (Task 1).
- Produces: `ContentPack(sectionId, name, orderIndex, examVersion, units: List<UnitPack>)`, `UnitPack(unitId, name, certObjective, orderIndex, exercises: List<ExerciseContent>)`, `SectionPackEntities(section, units, exercises)`, `ContentPack.toEntities(json): SectionPackEntities`. `ContentSeeder.seedIfNeeded()` (renamed from `seedIfEmpty()`) — consumed by Task 3 (packs it loads), Task 4 (migration test calls it directly), and this task's own change to `HomeViewModel`.

**Context:** `ExerciseContent` (the domain model decoded from `payload`, used by `SessionScreen`/`SessionViewModel`) does **not** change — `examVersion` lives only on the entity/pack level, inherited uniformly from the section for every exercise in it. `ContentLoader.kt` needs no changes — it just decodes whatever `ContentPack.serializer()` describes now.

- [ ] **Step 1: Update ContentPack to the v2 nested shape**

Replace `app/src/main/java/com/zconte/oopsapp/data/content/ContentPack.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.Serializable

@Serializable
data class ContentPack(
    val sectionId: String,
    val name: String,
    val orderIndex: Int,
    val examVersion: String,
    val units: List<UnitPack>
)

@Serializable
data class UnitPack(
    val unitId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int,
    val exercises: List<ExerciseContent>
)
```

- [ ] **Step 2: Update ContentMapper**

Replace `app/src/main/java/com/zconte/oopsapp/data/content/ContentMapper.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.json.Json

data class SectionPackEntities(
    val section: SectionEntity,
    val units: List<UnitEntity>,
    val exercises: List<ExerciseEntity>
)

fun ContentPack.toEntities(json: Json): SectionPackEntities {
    val section = SectionEntity(
        id = sectionId,
        name = name,
        orderIndex = orderIndex,
        examVersion = examVersion
    )
    val unitEntities = units.map { unitPack ->
        UnitEntity(
            id = unitPack.unitId,
            sectionId = sectionId,
            name = unitPack.name,
            certObjective = unitPack.certObjective,
            orderIndex = unitPack.orderIndex
        )
    }
    val exerciseEntities = units.flatMap { unitPack ->
        unitPack.exercises.map { content ->
            ExerciseEntity(
                id = content.id,
                unitId = unitPack.unitId,
                type = content.type,
                payload = json.encodeToString(ExerciseContent.serializer(), content),
                difficulty = content.difficulty,
                examVersion = examVersion
            )
        }
    }
    return SectionPackEntities(section, unitEntities, exerciseEntities)
}
```

- [ ] **Step 3: Update ContentSeeder to a content-version guard**

Replace `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.data.local.dao.ContentMetaDao
import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.entity.ContentMetaEntity
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val CONTENT_VERSION_KEY = "content_version"
private const val CURRENT_CONTENT_VERSION = "2"

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
        "content/streams.json"
    )

    suspend fun seedIfNeeded() {
        val seededVersion = contentMetaDao.get(CONTENT_VERSION_KEY)?.value
        if (seededVersion == CURRENT_CONTENT_VERSION) return

        sectionDao.clearAll()
        unitDao.clearAll()
        exerciseDao.clearAll()

        packAssetPaths.forEach { assetPath ->
            val pack = contentLoader.loadPack(assetPath)
            val entities = pack.toEntities(json)
            sectionDao.insertAll(listOf(entities.section))
            unitDao.insertAll(entities.units)
            exerciseDao.insertAll(entities.exercises)
        }

        contentMetaDao.upsert(ContentMetaEntity(configKey = CONTENT_VERSION_KEY, value = CURRENT_CONTENT_VERSION))
    }
}
```

- [ ] **Step 4: Update the one call site**

In `app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`, find:

```kotlin
    init {
        viewModelScope.launch {
            contentSeeder.seedIfEmpty()
            refreshStats()
            _uiState.update { it.copy(isReady = true) }
        }
    }
```

Replace with:

```kotlin
    init {
        viewModelScope.launch {
            contentSeeder.seedIfNeeded()
            refreshStats()
            _uiState.update { it.copy(isReady = true) }
        }
    }
```

(This file's `readiness["streams-lambdas"]` lookup is fixed in Task 10 — do not touch it here.)

- [ ] **Step 5: Rewrite ContentMapperTest for the v2 shape**

Replace `app/src/test/java/com/zconte/oopsapp/data/content/ContentMapperTest.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps a content pack into a section entity, unit entities, and exercise entities`() {
        val pack = ContentPack(
            sectionId = "java-streams",
            name = "Streams y lambdas",
            orderIndex = 2,
            examVersion = "java21",
            units = listOf(
                UnitPack(
                    unitId = "streams-terminal",
                    name = "Operaciones terminales",
                    certObjective = "streams-lambdas",
                    orderIndex = 1,
                    exercises = listOf(
                        ExerciseContent(
                            id = "streams-01",
                            type = "fill_blank",
                            difficulty = 2,
                            prompt = "prompt",
                            code = "code",
                            answer = "collect",
                            distractors = listOf("map"),
                            explanation = "explanation"
                        )
                    )
                )
            )
        )

        val entities = pack.toEntities(json)

        assertEquals("java-streams", entities.section.id)
        assertEquals("java21", entities.section.examVersion)
        assertEquals(1, entities.units.size)
        assertEquals("streams-terminal", entities.units.first().id)
        assertEquals("java-streams", entities.units.first().sectionId)
        assertEquals("streams-lambdas", entities.units.first().certObjective)
        assertEquals(1, entities.exercises.size)
        assertEquals("streams-01", entities.exercises.first().id)
        assertEquals("streams-terminal", entities.exercises.first().unitId)
        assertEquals("java21", entities.exercises.first().examVersion)
        assertEquals("fill_blank", entities.exercises.first().type)
        assertEquals(2, entities.exercises.first().difficulty)

        val decoded = json.decodeFromString(ExerciseContent.serializer(), entities.exercises.first().payload)
        assertEquals("collect", decoded.answer)
    }
}
```

- [ ] **Step 6: Rewrite ContentPackParsingTest for the v2 shape**

Replace `app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentPackParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a content pack with one unit and one exercise`() {
        val raw = """
            {
              "sectionId": "java-streams",
              "name": "Streams y lambdas",
              "orderIndex": 2,
              "examVersion": "java21",
              "units": [
                {
                  "unitId": "streams-terminal",
                  "name": "Operaciones terminales",
                  "certObjective": "streams-lambdas",
                  "orderIndex": 1,
                  "exercises": [
                    {
                      "id": "streams-collect-01",
                      "type": "fill_blank",
                      "difficulty": 2,
                      "prompt": "Convierte un Stream<String> en List<String>:",
                      "code": "stream._____(Collectors.toList())",
                      "answer": "collect",
                      "distractors": ["map", "reduce", "forEach"],
                      "explanation": "collect() es una operacion terminal que acumula elementos."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals("java-streams", pack.sectionId)
        assertEquals("java21", pack.examVersion)
        assertEquals(1, pack.units.size)
        assertEquals("streams-lambdas", pack.units.first().certObjective)
        assertEquals(1, pack.units.first().exercises.size)
        assertEquals("collect", pack.units.first().exercises.first().answer)
        assertEquals(listOf("map", "reduce", "forEach"), pack.units.first().exercises.first().distractors)
    }

    @Test
    fun `exercise without code field parses with null code`() {
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
                  "exercises": [
                    {
                      "id": "streams-mcq-01",
                      "type": "mcq",
                      "difficulty": 1,
                      "prompt": "Que metodo crea un Stream desde una List?",
                      "answer": "stream",
                      "distractors": ["toStream", "asStream", "of"],
                      "explanation": "List.stream() crea el Stream."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals(null, pack.units.first().exercises.first().code)
    }
}
```

- [ ] **Step 7: Build and run these tests**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.zconte.oopsapp.data.content.*"`
Expected: BUILD SUCCESSFUL, all tests pass. (`ContentSeeder`/`HomeViewModel` reference DAOs from Task 1, already in place.)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt app/src/test/java/com/zconte/oopsapp/data/content/
git commit -m "Content format v2: nested Section/Unit packs, version-guarded ContentSeeder"
```

---

### Task 3: Autoría de contenido — Fundamentos nuevo + Streams reorganizado

**Files:**
- Create: `app/src/main/assets/content/java-fundamentals.json`
- Modify: `app/src/main/assets/content/streams.json`

**Interfaces:**
- Consumes: the v2 `ContentPack`/`UnitPack` JSON shape (Task 2). `ContentSeeder`'s `packAssetPaths` (Task 2, Step 3) already references both file paths by name — this task only has to make the files match that shape and exist at those exact paths.
- Produces: seeded content consumed by Task 4's migration test (which asserts `streams-05` ends up in unit `streams-terminal`) and by the app at runtime.

**Context:** Streams' 20 existing exercise ids (`streams-01` .. `streams-20`) and their content are **preserved verbatim** — only the wrapping structure changes (flat `exercises` list → grouped into 4 `units`). This is what lets `review_state` rows keyed by those ids survive the migration untouched. Do not change any `id`, `answer`, `explanation`, `prompt`, `code`, `difficulty`, or `distractors` value for the existing 20 — only regroup them.

- [ ] **Step 1: Create the Fundamentos pack**

Create `app/src/main/assets/content/java-fundamentals.json`:

```json
{
  "sectionId": "java-fundamentals",
  "name": "Fundamentos de Java",
  "orderIndex": 1,
  "examVersion": "core",
  "units": [
    {
      "unitId": "fund-what-is-java",
      "name": "Que es Java?",
      "certObjective": "language-basics",
      "orderIndex": 1,
      "exercises": [
        {
          "id": "fund-whatis-01",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que significa JVM?",
          "answer": "Java Virtual Machine",
          "distractors": ["Java Verified Method", "Java Variable Manager", "Java Visual Machine"],
          "explanation": "La JVM (Java Virtual Machine) es el programa que ejecuta el bytecode Java, independiente del sistema operativo."
        },
        {
          "id": "fund-whatis-02",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que transforma el codigo fuente .java en bytecode .class?",
          "answer": "El compilador (javac)",
          "distractors": ["La JVM", "El JRE", "El sistema operativo"],
          "explanation": "javac es el compilador que traduce codigo fuente Java a bytecode."
        },
        {
          "id": "fund-whatis-03",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Compila Main.java desde la terminal:",
          "code": "_____ Main.java",
          "answer": "javac",
          "distractors": ["java", "jar", "jshell"],
          "explanation": "javac Main.java genera Main.class con el bytecode."
        },
        {
          "id": "fund-whatis-04",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Ejecuta la clase compilada Main:",
          "code": "_____ Main",
          "answer": "java",
          "distractors": ["javac", "jar", "jshell"],
          "explanation": "El comando java arranca la JVM y ejecuta la clase indicada."
        },
        {
          "id": "fund-whatis-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que incluye el JDK que el JRE no incluye?",
          "answer": "Herramientas de desarrollo (como el compilador)",
          "distractors": ["La JVM", "Las librerias estandar", "El recolector de basura"],
          "explanation": "El JDK (Java Development Kit) es el JRE mas herramientas de desarrollo (javac, jshell, etc.). El JRE (Java Runtime Environment) solo permite ejecutar, no compilar."
        },
        {
          "id": "fund-whatis-06",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Java es un lenguaje...",
          "answer": "Compilado a bytecode e interpretado/JIT por la JVM",
          "distractors": ["Puramente interpretado, sin compilacion", "Compilado directo a codigo maquina nativo", "Que no necesita ningun runtime"],
          "explanation": "Java compila a bytecode intermedio; la JVM lo interpreta y ademas compila en caliente partes del codigo a codigo nativo en tiempo de ejecucion (JIT)."
        }
      ]
    },
    {
      "unitId": "fund-class-structure",
      "name": "Estructura de una clase",
      "certObjective": "language-basics",
      "orderIndex": 2,
      "exercises": [
        {
          "id": "fund-class-01",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que es un constructor?",
          "answer": "Un metodo especial que inicializa un objeto nuevo",
          "distractors": ["Un metodo que devuelve un valor obligatoriamente", "Un campo estatico de la clase", "Un tipo de bucle"],
          "explanation": "El constructor se ejecuta al crear una instancia con new y suele inicializar los fields."
        },
        {
          "id": "fund-class-02",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que es este bloque de codigo?",
          "code": "public Persona(String nombre) {\n    this.nombre = nombre;\n}",
          "answer": "Un constructor",
          "distractors": ["Un getter", "Un setter", "Un metodo estatico"],
          "explanation": "Tiene el mismo nombre que la clase y no declara tipo de retorno: es un constructor."
        },
        {
          "id": "fund-class-03",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que es este metodo?",
          "code": "public String getNombre() {\n    return nombre;\n}",
          "answer": "Un getter",
          "distractors": ["Un setter", "Un constructor", "Un field"],
          "explanation": "Devuelve el valor de un field sin modificarlo: es un getter."
        },
        {
          "id": "fund-class-04",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que es este metodo?",
          "code": "public void setNombre(String nombre) {\n    this.nombre = nombre;\n}",
          "answer": "Un setter",
          "distractors": ["Un getter", "Un constructor", "Un field"],
          "explanation": "Recibe un valor y lo asigna a un field: es un setter."
        },
        {
          "id": "fund-class-05",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Puede un archivo .java contener mas de una clase de nivel superior?",
          "answer": "Si, pero solo una puede ser public y debe coincidir con el nombre del archivo",
          "distractors": ["No, nunca mas de una clase por archivo", "Si, todas pueden ser public", "No, el nombre del archivo no importa"],
          "explanation": "Un archivo puede tener varias clases top-level, pero como maximo una public, y su nombre debe coincidir con el del archivo."
        },
        {
          "id": "fund-class-06",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Declara un field privado de tipo String llamado nombre:",
          "code": "public class Persona {\n    private String _____;\n}",
          "answer": "nombre",
          "distractors": [],
          "explanation": "Los fields se declaran con un tipo y un identificador dentro del cuerpo de la clase."
        }
      ]
    },
    {
      "unitId": "fund-types-and-main",
      "name": "Tipos, variables y el metodo main",
      "certObjective": "language-basics",
      "orderIndex": 3,
      "exercises": [
        {
          "id": "fund-main-01",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual es la firma correcta del metodo main?",
          "answer": "public static void main(String[] args)",
          "distractors": ["public void main(String[] args)", "static void main(String args)", "public static int main(String[] args)"],
          "explanation": "main debe ser public, static, sin retorno (void), y recibir un arreglo de String."
        },
        {
          "id": "fund-main-02",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Cual de estos es un tipo primitivo en Java?",
          "answer": "int",
          "distractors": ["String", "Integer", "ArrayList"],
          "explanation": "int es primitivo; String, Integer y ArrayList son tipos de referencia (clases)."
        },
        {
          "id": "fund-main-03",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Declara una variable entera llamada edad con valor 30:",
          "code": "int _____ = 30;",
          "answer": "edad",
          "distractors": [],
          "explanation": "La sintaxis es tipo nombre = valor;"
        },
        {
          "id": "fund-main-04",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que diferencia hay entre int e Integer?",
          "answer": "int es primitivo, Integer es un objeto que lo envuelve (wrapper)",
          "distractors": ["No hay diferencia", "Integer es primitivo e int es un objeto", "int solo existe en versiones antiguas de Java"],
          "explanation": "Integer es la clase wrapper de int, permite valores null y se usa en colecciones genericas."
        },
        {
          "id": "fund-main-05",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que el metodo main es static?",
          "answer": "Porque la JVM lo invoca sin crear antes una instancia de la clase",
          "distractors": ["Porque todos los metodos en Java son static", "Porque main no puede acceder a fields", "Porque static hace que el metodo sea mas rapido"],
          "explanation": "static permite invocar main directamente sobre la clase, sin necesidad de instanciarla primero."
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Reorganize the Streams pack into units, preserving all 20 exercise ids and content verbatim**

Replace `app/src/main/assets/content/streams.json`:

```json
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
      "orderIndex": 1,
      "exercises": [
        {
          "id": "streams-20",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que metodo crea un Stream a partir de una List?",
          "answer": "stream",
          "distractors": ["toStream", "asStream", "of"],
          "explanation": "List.stream() crea un Stream secuencial respaldado por la coleccion."
        },
        {
          "id": "streams-16",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Genera un rango de enteros de 1 (inclusive) a 10 (exclusive):",
          "code": "IntStream._____(1, 10)",
          "answer": "range",
          "distractors": ["of", "generate", "iterate"],
          "explanation": "IntStream.range(start, end) genera enteros con el limite superior exclusivo."
        }
      ]
    },
    {
      "unitId": "streams-intermediate",
      "name": "Operaciones intermedias",
      "certObjective": "streams-lambdas",
      "orderIndex": 2,
      "exercises": [
        {
          "id": "streams-02",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Filtra los elementos que cumplen una condicion:",
          "code": "stream._____(s -> s.length() > 3)",
          "answer": "filter",
          "distractors": ["map", "sorted", "limit"],
          "explanation": "filter() conserva solo los elementos que cumplen el predicado."
        },
        {
          "id": "streams-03",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Transforma cada elemento del stream:",
          "code": "stream._____(String::toUpperCase)",
          "answer": "map",
          "distractors": ["filter", "reduce", "peek"],
          "explanation": "map() aplica una funcion a cada elemento y produce un nuevo stream."
        },
        {
          "id": "streams-06",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Elimina elementos duplicados:",
          "code": "stream._____()",
          "answer": "distinct",
          "distractors": ["unique", "filter", "dedupe"],
          "explanation": "distinct() usa equals() para descartar duplicados."
        },
        {
          "id": "streams-07",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Ordena los elementos usando su orden natural:",
          "code": "stream._____()",
          "answer": "sorted",
          "distractors": ["order", "arrange", "collect"],
          "explanation": "sorted() sin argumentos usa Comparable/orden natural."
        },
        {
          "id": "streams-08",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Limita el stream a los primeros 5 elementos:",
          "code": "stream._____(5)",
          "answer": "limit",
          "distractors": ["take", "first", "cap"],
          "explanation": "limit(n) trunca el stream a n elementos."
        },
        {
          "id": "streams-09",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Descarta los primeros 3 elementos:",
          "code": "stream._____(3)",
          "answer": "skip",
          "distractors": ["drop", "offset", "limit"],
          "explanation": "skip(n) descarta los primeros n elementos del stream."
        },
        {
          "id": "streams-15",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que metodo aplana un Stream de listas en un unico Stream de elementos?",
          "answer": "flatMap",
          "distractors": ["map", "collect", "reduce"],
          "explanation": "flatMap() sustituye cada elemento por un stream y los aplana en uno solo."
        }
      ]
    },
    {
      "unitId": "streams-terminal",
      "name": "Operaciones terminales",
      "certObjective": "streams-lambdas",
      "orderIndex": 3,
      "exercises": [
        {
          "id": "streams-01",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Convierte un Stream<String> en List<String>:",
          "code": "stream._____(Collectors.toList())",
          "answer": "collect",
          "distractors": ["map", "reduce", "forEach"],
          "explanation": "collect() es una operacion terminal que acumula elementos en una coleccion."
        },
        {
          "id": "streams-04",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Cual de estas es una operacion terminal (cierra el stream)?",
          "answer": "forEach",
          "distractors": ["map", "filter", "sorted"],
          "explanation": "forEach() consume el stream; map, filter y sorted son operaciones intermedias (lazy)."
        },
        {
          "id": "streams-05",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Cuenta los elementos del stream:",
          "code": "long total = stream._____();",
          "answer": "count",
          "distractors": ["size", "sum", "length"],
          "explanation": "count() es una operacion terminal que devuelve un long."
        },
        {
          "id": "streams-10",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que operacion combina todos los elementos en un unico resultado usando un acumulador?",
          "answer": "reduce",
          "distractors": ["collect", "map", "filter"],
          "explanation": "reduce() combina elementos de a pares hasta obtener un unico valor."
        },
        {
          "id": "streams-11",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Verifica si algun elemento cumple una condicion:",
          "code": "stream._____(s -> s.isEmpty())",
          "answer": "anyMatch",
          "distractors": ["allMatch", "noneMatch", "filter"],
          "explanation": "anyMatch() devuelve true si al menos un elemento cumple el predicado."
        },
        {
          "id": "streams-12",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Verifica que todos los elementos cumplan una condicion:",
          "code": "stream._____(s -> s.length() > 0)",
          "answer": "allMatch",
          "distractors": ["anyMatch", "noneMatch", "map"],
          "explanation": "allMatch() devuelve true solo si todos los elementos cumplen el predicado."
        },
        {
          "id": "streams-13",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Verifica que ningun elemento cumpla una condicion:",
          "code": "stream._____(s -> s == null)",
          "answer": "noneMatch",
          "distractors": ["allMatch", "anyMatch", "filter"],
          "explanation": "noneMatch() devuelve true si ningun elemento cumple el predicado."
        },
        {
          "id": "streams-17",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Obtiene el elemento minimo segun un comparador:",
          "code": "stream._____(Comparator.naturalOrder())",
          "answer": "min",
          "distractors": ["max", "sorted", "reduce"],
          "explanation": "min() es una operacion terminal que devuelve un Optional<T>."
        },
        {
          "id": "streams-18",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Obtiene el elemento maximo segun un comparador:",
          "code": "stream._____(Comparator.naturalOrder())",
          "answer": "max",
          "distractors": ["min", "sorted", "limit"],
          "explanation": "max() es una operacion terminal que devuelve un Optional<T>."
        }
      ]
    },
    {
      "unitId": "streams-collectors",
      "name": "Collectors avanzados",
      "certObjective": "streams-lambdas",
      "orderIndex": 4,
      "exercises": [
        {
          "id": "streams-14",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Une los elementos del stream en un String separado por coma:",
          "code": "stream.collect(Collectors._____(\", \"))",
          "answer": "joining",
          "distractors": ["toList", "toSet", "groupingBy"],
          "explanation": "Collectors.joining() concatena Strings con el separador dado."
        },
        {
          "id": "streams-19",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Agrupa los elementos por una propiedad derivada:",
          "code": "stream.collect(Collectors._____(String::length))",
          "answer": "groupingBy",
          "distractors": ["joining", "toMap", "partitioningBy"],
          "explanation": "Collectors.groupingBy() agrupa elementos en un Map segun la funcion clasificadora."
        }
      ]
    }
  ]
}
```

- [ ] **Step 3: Validate both packs parse correctly**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.data.content.*"`
Expected: BUILD SUCCESSFUL (this doesn't load the assets directly, but confirms the shape used by the tests above still matches — full asset-loading validation happens in Task 4's instrumented test, which reads these exact files from a real `Context.assets`).

Manually verify (no automated step): both files together contain exactly 20 `streams-*` ids (unchanged from before) plus 17 new `fund-*` ids, and no id repeats within the pair.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/content/
git commit -m "Author Fundamentos de Java content pack; reorganize Streams into 4 units (ids preserved)"
```

---

### Task 4: Test de migración + seeder (el gate crítico)

**Files:**
- Create: `app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt`

**Interfaces:**
- Consumes: `AppDatabase`, `MIGRATION_1_2` (Task 1); `ContentSeeder`, `ContentLoader` (Task 2); the real asset files from Task 3.
- Produces: nothing consumed by later tasks — this is the correctness gate for Tasks 1-3 before building anything on top.

**Context:** This is an **instrumented test** (`androidTest`, needs a connected device or emulator — `./gradlew connectedAndroidTest`), not a local JVM test, because Room's `MigrationTestHelper` needs a real SQLite implementation. This is a deliberate exception to this project's usual "testeable sin emulador" convention (see `PROJECT-OOPS.md` section 3) — Room migrations cannot be verified any other way.

**The critical thing this test must do, and why:** it is not enough to run the migration in isolation and check the schema is valid — `ContentSeeder.seedIfNeeded()` must also run against the migrated database, through the same path the real app takes on startup, and the test must assert on the *result of that*. The risk this guards against: `ContentSeeder`'s version guard could silently skip seeding after a migration (if the guard logic were wrong), leaving `sections`/`units`/`exercises` empty while `review_state` still references exercise ids that no longer exist anywhere — the app wouldn't crash, it would just silently show empty content. Asserting only "the migration's DDL ran without throwing" would not catch that class of bug.

- [ ] **Step 1: Write the migration + seeder integration test**

Create `app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt`:

```kotlin
package com.zconte.oopsapp.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zconte.oopsapp.data.content.ContentLoader
import com.zconte.oopsapp.data.content.ContentSeeder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesUserDataAndReseedsV2Content() {
        // 1. Seed a v1-shaped database with raw SQL (the v1 Kotlin entity classes no longer
        // exist in the codebase after Task 1's refactor -- the v1 schema shape is reproduced
        // here directly, matching what TopicEntity/the old ExerciseEntity used to declare).
        helper.createDatabase(TEST_DB, 1).apply {
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
            close()
        }

        // 2. Run the real migration and validate the resulting schema against what Room expects.
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // 3. Open the migrated file as a real AppDatabase (same name/path) and run the actual
        // seeder against it -- the same path the app takes on startup.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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
```

- [ ] **Step 2: Run the instrumented test**

Run: `./gradlew connectedAndroidTest --tests "com.zconte.oopsapp.data.local.MigrationTest"`
Expected: BUILD SUCCESSFUL, test passes. Requires a connected device or running emulator (`adb devices` must show one). If it fails on the `review_state`/`user_stats` assertions, the migration is touching tables it shouldn't — stop and fix Task 1's `MIGRATION_1_2` before proceeding to any later task. If it fails on the `streams-terminal`/`sections` assertions, the seeder or the content pack (Task 2/3) has a bug — do not paper over it by relaxing the assertions.

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt
git commit -m "Add migration+seeder integration test (the v1->v2 correctness gate)"
```

---

### Task 5: Modelos de dominio y repositorios (Section/LearningUnit)

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/Section.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/LearningUnit.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/model/Exercise.kt`
- Delete: `app/src/main/java/com/zconte/oopsapp/domain/model/Topic.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/repository/ContentRepository.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/repository/ContentRepositoryImpl.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt`

**Interfaces:**
- Consumes: `SectionEntity`, `UnitEntity`, `UnitProgressEntity`, `SectionDao`, `UnitDao`, `UnitProgressDao`, updated `ExerciseDao`/`ReviewStateDao` (Task 1).
- Produces: `Section(id, name, orderIndex, examVersion)`, `LearningUnit(id, sectionId, name, certObjective, orderIndex)`, `Exercise(id, unitId, type, payload, difficulty, examVersion = "core")`, `ContentRepository` (`getSections`, `getUnitsBySection`, `getCompletedUnitIds`, `markUnitCompleted`), extended `ExerciseRepository` (`getExercisesByUnit`, `getExercisesBySection`, `getAnsweredExerciseIds`). Consumed by Task 6 (gating), Task 7 (checkpoint assembly), Task 9 (Ruta), Task 10 (Home).

**Context:** The domain model is named `LearningUnit`, not `Unit` — `Unit` would shadow `kotlin.Unit` everywhere it's imported, which is exactly the kind of footgun worth avoiding by naming choice. `examVersion` on `Exercise` gets a default value (`"core"`) specifically so it's the last positional constructor parameter and every existing 5-arg positional call to `Exercise(...)` in test fakes keeps compiling unchanged — only the *name* of the 2nd parameter changes (`topicId`→`unitId`), which positional calls don't care about. `Topic.kt` (`domain/model/Topic.kt`) is confirmed dead code — grepping the codebase for it outside its own declaration finds zero references — delete it, don't deprecate it.

- [ ] **Step 1: New domain models**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/Section.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class Section(
    val id: String,
    val name: String,
    val orderIndex: Int,
    val examVersion: String
)
```

Create `app/src/main/java/com/zconte/oopsapp/domain/model/LearningUnit.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class LearningUnit(
    val id: String,
    val sectionId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int
)
```

- [ ] **Step 2: Update Exercise, delete Topic**

Replace `app/src/main/java/com/zconte/oopsapp/domain/model/Exercise.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class Exercise(
    val id: String,
    val unitId: String,
    val type: String,
    val payload: String,
    val difficulty: Int,
    val examVersion: String = "core"
)
```

Delete `app/src/main/java/com/zconte/oopsapp/domain/model/Topic.kt`.

- [ ] **Step 3: New ContentRepository interface + impl**

Create `app/src/main/java/com/zconte/oopsapp/domain/repository/ContentRepository.kt`:

```kotlin
package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import java.time.LocalDate

interface ContentRepository {
    suspend fun getSections(): List<Section>
    suspend fun getUnitsBySection(sectionId: String): List<LearningUnit>
    suspend fun getCompletedUnitIds(): List<String>
    suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate)
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/repository/ContentRepositoryImpl.kt`:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.dao.UnitProgressDao
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity
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

    override suspend fun getCompletedUnitIds(): List<String> =
        unitProgressDao.getCompletedUnitIds()

    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate) {
        unitProgressDao.upsert(
            UnitProgressEntity(unitId = unitId, completed = true, completedAt = completedAt.toEpochDay())
        )
    }
}

private fun SectionEntity.toDomain() = Section(id, name, orderIndex, examVersion)

private fun UnitEntity.toDomain() = LearningUnit(id, sectionId, name, certObjective, orderIndex)
```

- [ ] **Step 4: Extend ExerciseRepository**

Replace `app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt`:

```kotlin
package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import java.time.LocalDate

interface ExerciseRepository {
    suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise>
    suspend fun getNewExercises(limit: Int): List<Exercise>
    suspend fun getExercisesByUnit(unitId: String): List<Exercise>
    suspend fun getExercisesBySection(sectionId: String): List<Exercise>
    suspend fun getReviewState(exerciseId: String): ReviewState?
    suspend fun saveReviewState(state: ReviewState)
    suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String>
}
```

Replace `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val reviewStateDao: ReviewStateDao
) : ExerciseRepository {

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> =
        exerciseDao.getDue(today.toEpochDay()).take(limit).map { it.toDomain() }

    override suspend fun getNewExercises(limit: Int): List<Exercise> =
        exerciseDao.getNew(limit).map { it.toDomain() }

    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> =
        exerciseDao.getByUnit(unitId).map { it.toDomain() }

    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> =
        exerciseDao.getBySection(sectionId).map { it.toDomain() }

    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        reviewStateDao.getByExerciseId(exerciseId)?.toDomain()

    override suspend fun saveReviewState(state: ReviewState) {
        reviewStateDao.upsert(state.toEntity())
    }

    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        if (exerciseIds.isEmpty()) emptyList() else reviewStateDao.getExistingIds(exerciseIds)
}

private fun ExerciseEntity.toDomain() = Exercise(
    id = id, unitId = unitId, type = type, payload = payload, difficulty = difficulty, examVersion = examVersion
)

private fun ReviewStateEntity.toDomain() = ReviewState(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = LocalDate.ofEpochDay(dueDate)
)

private fun ReviewState.toEntity() = ReviewStateEntity(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = dueDate.toEpochDay()
)
```

- [ ] **Step 5: Wire ContentRepository in DI**

In `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`, find:

```kotlin
import com.zconte.oopsapp.data.repository.ExerciseRepositoryImpl
import com.zconte.oopsapp.data.repository.ProgressRepositoryImpl
import com.zconte.oopsapp.data.repository.SettingsRepositoryImpl
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.repository.SettingsRepository
```

Replace with:

```kotlin
import com.zconte.oopsapp.data.repository.ContentRepositoryImpl
import com.zconte.oopsapp.data.repository.ExerciseRepositoryImpl
import com.zconte.oopsapp.data.repository.ProgressRepositoryImpl
import com.zconte.oopsapp.data.repository.SettingsRepositoryImpl
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.repository.SettingsRepository
```

Find:

```kotlin
    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
```

Replace with:

```kotlin
    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository
}
```

- [ ] **Step 6: Fix existing test fakes to implement the extended interface**

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`, find:

```kotlin
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = due.take(limit)
    override suspend fun getNewExercises(limit: Int): List<Exercise> = new.take(limit)
    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        savedStates.find { it.exerciseId == exerciseId }
    override suspend fun saveReviewState(state: ReviewState) {
        savedStates.removeAll { it.exerciseId == state.exerciseId }
        savedStates.add(state)
    }
```

Replace with:

```kotlin
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = due.take(limit)
    override suspend fun getNewExercises(limit: Int): List<Exercise> = new.take(limit)
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        savedStates.find { it.exerciseId == exerciseId }
    override suspend fun saveReviewState(state: ReviewState) {
        savedStates.removeAll { it.exerciseId == state.exerciseId }
        savedStates.add(state)
    }
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        savedStates.map { it.exerciseId }.filter { it in exerciseIds }
```

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt`, find:

```kotlin
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = states[exerciseId]
    override suspend fun saveReviewState(state: ReviewState) {
        states[state.exerciseId] = state
    }
```

Replace with:

```kotlin
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = states[exerciseId]
    override suspend fun saveReviewState(state: ReviewState) {
        states[state.exerciseId] = state
    }
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        states.keys.filter { it in exerciseIds }
```

- [ ] **Step 7: Build and test**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still pass (`GetTodaySessionUseCaseTest`, `SubmitAnswerUseCaseTest`, `UpdateStreakUseCaseTest`, `ContentMapperTest`, `ContentPackParsingTest`, `ThemeResolverTest`, `SettingsRepositoryImplTest`, `SchedulerSm2Test`).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/ app/src/main/java/com/zconte/oopsapp/data/repository/ app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt app/src/test/java/com/zconte/oopsapp/domain/usecase/
git commit -m "Domain models and repositories for Section/LearningUnit; delete unused Topic model"
```

---

### Task 6: Progreso por unidad (primera pasada) y gating secuencial

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCase.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`

**Interfaces:**
- Consumes: `ContentRepository`, `ExerciseRepository` (Task 5).
- Produces: `UnitProgress(unit, completed, unlocked)`, `SectionPath(section, unlocked, units, completed)`, `MarkUnitProgressUseCase(unitId, today)`, `GetLearningPathUseCase(): List<SectionPath>`. Consumed by Task 9 (Ruta UI) and Task 8 (session completion calls `MarkUnitProgressUseCase`).

**Context:** "Primera pasada" (Global Constraints) means: a unit is complete when every one of its exercises has a `review_state` row (has been answered at least once, regardless of correctness) — checked via `ExerciseRepository.getAnsweredExerciseIds`, added in Task 5. Gating is purely sequential: the first section is always unlocked; a section unlocks when the previous section's units are all complete; within an unlocked section, the first unit is always unlocked and unit N+1 unlocks only when unit N is complete.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForUnitProgress(
    private val exercisesByUnit: Map<String, List<Exercise>> = emptyMap(),
    private val answeredIds: Set<String> = emptySet()
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        exerciseIds.filter { it in answeredIds }
}

private class FakeContentRepositoryForUnitProgress : ContentRepository {
    val markedComplete = mutableListOf<String>()
    override suspend fun getSections(): List<Section> = emptyList()
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = emptyList()
    override suspend fun getCompletedUnitIds(): List<String> = markedComplete
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate) {
        markedComplete.add(unitId)
    }
}

class MarkUnitProgressUseCaseTest {

    private val today = LocalDate.of(2026, 7, 20)

    private fun exercise(id: String) = Exercise(id, "unit-1", "fill_blank", "{}", 1)

    @Test
    fun `marks the unit complete when every exercise has been answered`() = runTest {
        val exerciseRepository = FakeExerciseRepositoryForUnitProgress(
            exercisesByUnit = mapOf("unit-1" to listOf(exercise("ex-1"), exercise("ex-2"))),
            answeredIds = setOf("ex-1", "ex-2")
        )
        val contentRepository = FakeContentRepositoryForUnitProgress()
        val useCase = MarkUnitProgressUseCase(exerciseRepository, contentRepository)

        useCase("unit-1", today)

        assertEquals(listOf("unit-1"), contentRepository.markedComplete)
    }

    @Test
    fun `does not mark the unit complete when some exercise is unanswered`() = runTest {
        val exerciseRepository = FakeExerciseRepositoryForUnitProgress(
            exercisesByUnit = mapOf("unit-1" to listOf(exercise("ex-1"), exercise("ex-2"))),
            answeredIds = setOf("ex-1")
        )
        val contentRepository = FakeContentRepositoryForUnitProgress()
        val useCase = MarkUnitProgressUseCase(exerciseRepository, contentRepository)

        useCase("unit-1", today)

        assertTrue(contentRepository.markedComplete.isEmpty())
    }
}
```

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
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
    private val completedUnitIds: List<String>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnitIds(): List<String> = completedUnitIds
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate) {}
}

class GetLearningPathUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)

    @Test
    fun `first section and its first unit are always unlocked`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnitIds = emptyList()
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
            completedUnitIds = listOf("s1-u1")
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
            completedUnitIds = listOf("s1-u1")
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
            completedUnitIds = listOf("s1-u1")
        )
        val useCase = GetLearningPathUseCase(repository)

        val path = useCase()

        assertFalse(path[1].unlocked)
    }
}
```

- [ ] **Step 2: Run to verify they fail (types don't exist yet)**

Run: `./gradlew :app:compileTestKotlin`
Expected: FAIL — `MarkUnitProgressUseCase`, `GetLearningPathUseCase`, `SectionPath`, `UnitProgress` are unresolved references.

- [ ] **Step 3: Implement SectionPath/UnitProgress and both use cases**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class UnitProgress(
    val unit: LearningUnit,
    val completed: Boolean,
    val unlocked: Boolean
)

data class SectionPath(
    val section: Section,
    val unlocked: Boolean,
    val units: List<UnitProgress>,
    val completed: Boolean
)
```

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

class MarkUnitProgressUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(unitId: String, today: LocalDate) {
        val exercises = exerciseRepository.getExercisesByUnit(unitId)
        if (exercises.isEmpty()) return
        val answeredIds = exerciseRepository.getAnsweredExerciseIds(exercises.map { it.id })
        if (answeredIds.toSet().containsAll(exercises.map { it.id })) {
            contentRepository.markUnitCompleted(unitId, today)
        }
    }
}
```

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitProgress
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

class GetLearningPathUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(): List<SectionPath> {
        val sections = contentRepository.getSections().sortedBy { it.orderIndex }
        val completedUnitIds = contentRepository.getCompletedUnitIds().toSet()

        var previousSectionComplete = true
        return sections.map { section ->
            val units = contentRepository.getUnitsBySection(section.id).sortedBy { it.orderIndex }
            val sectionUnlocked = previousSectionComplete

            var previousUnitComplete = true
            val unitProgress = units.map { unit ->
                val completed = unit.id in completedUnitIds
                val unlocked = sectionUnlocked && previousUnitComplete
                previousUnitComplete = completed
                UnitProgress(unit, completed, unlocked)
            }

            val sectionComplete = units.isNotEmpty() && units.all { it.id in completedUnitIds }
            previousSectionComplete = sectionComplete

            SectionPath(section, sectionUnlocked, unitProgress, sectionComplete)
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetLearningPathUseCaseTest"`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/ app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt
git commit -m "Unit first-pass completion and sequential section/unit gating"
```

---

### Task 7: Ensamblado y resultado del checkpoint de repaso

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/CheckpointKind.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/CheckpointResult.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/repository/CheckpointRepository.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/repository/CheckpointRepositoryImpl.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCase.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt`

**Interfaces:**
- Consumes: `ExerciseRepository`, `ContentRepository` (Task 5); `CheckpointAttemptEntity`, `CheckpointAttemptDao` (Task 1).
- Produces: `CheckpointKind.REVIEW`/`CheckpointKind.PLACEMENT` (constants; only `REVIEW` is wired to UI this round), `CheckpointResult(scorePct, passed)`, `GetCheckpointSessionUseCase(sectionId): List<Exercise>`, `CompleteCheckpointUseCase(sectionId, kind, correctCount, totalCount, today): CheckpointResult`. Consumed by Task 8 (`CheckpointViewModel`).

**Context:** Per-question SM-2 feeding needs no new code here — Task 8's `CheckpointViewModel` calls the existing `SubmitAnswerUseCase` for each answered question, exactly like `SessionViewModel` does today (Global Constraints: checkpoint answers feed SM-2 like any other session). This task only builds question *assembly* (which exercises go in the checkpoint) and *scoring* (pass/fail at 68%, persisted).

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForCheckpoint(
    private val bySection: Map<String, List<Exercise>>
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = bySection[sectionId] ?: emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = emptyList()
}

private class FakeContentRepositoryForCheckpoint(
    private val sections: List<Section>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = emptyList()
    override suspend fun getCompletedUnitIds(): List<String> = emptyList()
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate) {}
}

class GetCheckpointSessionUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "mcq", "{}", 1)

    @Test
    fun `the first section's checkpoint is entirely from that section (no earlier content exists)`() = runTest {
        val currentPool = (1..15).map { exercise("s1-ex-$it", "s1-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(mapOf("s1" to currentPool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s1")

        assertEquals(12, result.size)
        assertTrue(result.all { it.id.startsWith("s1-ex-") })
    }

    @Test
    fun `a later section's checkpoint mixes in up to 3 questions from earlier sections`() = runTest {
        val s1Pool = (1..15).map { exercise("s1-ex-$it", "s1-unit") }
        val s2Pool = (1..20).map { exercise("s2-ex-$it", "s2-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(mapOf("s1" to s1Pool, "s2" to s2Pool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1), section("s2", 2)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s2")

        assertEquals(12, result.size)
        val fromEarlier = result.count { it.id.startsWith("s1-ex-") }
        val fromCurrent = result.count { it.id.startsWith("s2-ex-") }
        assertEquals(3, fromEarlier)
        assertEquals(9, fromCurrent)
    }

    @Test
    fun `a small section pool is capped, not padded past what exists`() = runTest {
        val s1Pool = listOf(exercise("s1-ex-1", "s1-unit"), exercise("s1-ex-2", "s1-unit"))
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(mapOf("s1" to s1Pool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s1")

        assertEquals(2, result.size)
    }
}
```

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

class CompleteCheckpointUseCaseTest {

    private val today = LocalDate.of(2026, 7, 20)

    @Test
    fun `passes at exactly the 68 percent threshold`() = runTest {
        val repository = FakeCheckpointRepository()
        val useCase = CompleteCheckpointUseCase(repository)

        // 68% of 25 = 17 correct, rounds down to exactly 68 -- boundary case.
        val result = useCase("s1", CheckpointKind.REVIEW, correctCount = 17, totalCount = 25, today = today)

        assertEquals(68, result.scorePct)
        assertTrue(result.passed)
        assertEquals(1, repository.recorded.size)
        assertTrue(repository.recorded.first().passed)
    }

    @Test
    fun `fails below the 68 percent threshold`() = runTest {
        val repository = FakeCheckpointRepository()
        val useCase = CompleteCheckpointUseCase(repository)

        val result = useCase("s1", CheckpointKind.REVIEW, correctCount = 6, totalCount = 12, today = today)

        assertEquals(50, result.scorePct)
        assertFalse(result.passed)
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :app:compileTestKotlin`
Expected: FAIL — `CheckpointKind`, `CheckpointResult`, `CheckpointRepository`, `GetCheckpointSessionUseCase`, `CompleteCheckpointUseCase` are unresolved.

- [ ] **Step 3: Implement the models, repository, and use cases**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/CheckpointKind.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

object CheckpointKind {
    const val REVIEW = "review"
    const val PLACEMENT = "placement"
}
```

Create `app/src/main/java/com/zconte/oopsapp/domain/model/CheckpointResult.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class CheckpointResult(
    val scorePct: Int,
    val passed: Boolean
)
```

Create `app/src/main/java/com/zconte/oopsapp/domain/repository/CheckpointRepository.kt`:

```kotlin
package com.zconte.oopsapp.domain.repository

import java.time.LocalDate

interface CheckpointRepository {
    suspend fun recordAttempt(sectionId: String, kind: String, scorePct: Int, passed: Boolean, takenAt: LocalDate)
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/repository/CheckpointRepositoryImpl.kt`:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.CheckpointAttemptDao
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate
import javax.inject.Inject

class CheckpointRepositoryImpl @Inject constructor(
    private val checkpointAttemptDao: CheckpointAttemptDao
) : CheckpointRepository {
    override suspend fun recordAttempt(sectionId: String, kind: String, scorePct: Int, passed: Boolean, takenAt: LocalDate) {
        checkpointAttemptDao.insert(
            CheckpointAttemptEntity(
                sectionId = sectionId,
                kind = kind,
                scorePct = scorePct,
                passed = passed,
                takenAt = takenAt.toEpochDay()
            )
        )
    }
}
```

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import javax.inject.Inject

private const val TARGET_SIZE = 12
private const val PRIOR_SAMPLE_SIZE = 3

class GetCheckpointSessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(sectionId: String): List<Exercise> {
        val sections = contentRepository.getSections().sortedBy { it.orderIndex }
        val currentSection = sections.find { it.id == sectionId } ?: return emptyList()
        val earlierSectionIds = sections.filter { it.orderIndex < currentSection.orderIndex }.map { it.id }

        val currentPool = exerciseRepository.getExercisesBySection(sectionId)
        val priorPool = earlierSectionIds.flatMap { exerciseRepository.getExercisesBySection(it) }

        val priorSample = priorPool.shuffled().take(minOf(PRIOR_SAMPLE_SIZE, priorPool.size))
        val currentSampleSize = (TARGET_SIZE - priorSample.size).coerceAtMost(currentPool.size)
        val currentSample = currentPool.shuffled().take(currentSampleSize)

        return (currentSample + priorSample).shuffled()
    }
}
```

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate
import javax.inject.Inject

private const val PASS_THRESHOLD_PCT = 68

class CompleteCheckpointUseCase @Inject constructor(
    private val checkpointRepository: CheckpointRepository
) {
    suspend operator fun invoke(
        sectionId: String,
        kind: String,
        correctCount: Int,
        totalCount: Int,
        today: LocalDate
    ): CheckpointResult {
        val scorePct = if (totalCount == 0) 0 else (correctCount * 100) / totalCount
        val passed = scorePct >= PASS_THRESHOLD_PCT
        checkpointRepository.recordAttempt(sectionId, kind, scorePct, passed, today)
        return CheckpointResult(scorePct, passed)
    }
}
```

- [ ] **Step 4: Wire CheckpointRepository in DI**

In `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`, find:

```kotlin
import com.zconte.oopsapp.data.repository.ContentRepositoryImpl
```

Replace with:

```kotlin
import com.zconte.oopsapp.data.repository.CheckpointRepositoryImpl
import com.zconte.oopsapp.data.repository.ContentRepositoryImpl
```

Find:

```kotlin
import com.zconte.oopsapp.domain.repository.ContentRepository
```

Replace with:

```kotlin
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
```

Find:

```kotlin
    @Binds
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository
}
```

Replace with:

```kotlin
    @Binds
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    abstract fun bindCheckpointRepository(impl: CheckpointRepositoryImpl): CheckpointRepository
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCaseTest"`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/ app/src/main/java/com/zconte/oopsapp/data/repository/CheckpointRepositoryImpl.kt app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt
git commit -m "Checkpoint assembly (current section + prior sample) and 68% pass/fail scoring"
```

---

### Task 8: Extraer UI de respuesta compartida + pantalla de Checkpoint

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt`

**Interfaces:**
- Consumes: `GetCheckpointSessionUseCase`, `CompleteCheckpointUseCase`, `CheckpointKind`, `CheckpointResult` (Task 7); `SubmitAnswerUseCase` (existing).
- Produces: `ExerciseAnswerCard(state: ExerciseAnswerState, onSubmit, onNext, modifier)`, `ExerciseAnswerState(exercise, currentIndex, totalExercises, isAnswered, isCorrect, selectedAnswer)` — a reusable Compose piece. `CheckpointViewModel` (reads `sectionId` from `SavedStateHandle`), `CheckpointScreen(onFinished, modifier)`. Consumed by Task 9 (nav route wiring passes `sectionId`, and `onFinished` navigates back to Ruta).

**Context:** This is a byte-for-byte extraction of `SessionScreen.kt`'s existing question/answer/feedback rendering (the `McqOptionButton`/`FeedbackBanner` visuals and their exact drawBehind/shadow theming are already correct and tested on-device — do not redesign them, only relocate them) into a shared composable both `SessionScreen` and the new `CheckpointScreen` call. `SessionViewModel` itself is **not** touched in this task — its existing completion-ordering (`pendingAnswerJob?.join()` before declaring done) stays exactly as is. `CheckpointViewModel` is a separate, independent state machine with its own (smaller) version of that same join-before-complete pattern — not a generalization of `SessionViewModel`.

- [ ] **Step 1: Extract ExerciseAnswerCard from SessionScreen**

Create `app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`:

```kotlin
package com.zconte.oopsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.ui.theme.OopsTheme

private const val MCQ_TYPE = "mcq"

data class ExerciseAnswerState(
    val exercise: ExerciseContent,
    val currentIndex: Int,
    val totalExercises: Int,
    val isAnswered: Boolean,
    val isCorrect: Boolean,
    val selectedAnswer: String?
)

@Composable
fun ExerciseAnswerCard(
    state: ExerciseAnswerState,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exercise = state.exercise
    var answer by remember(exercise.id) { mutableStateOf("") }
    var selectedOption by remember(exercise.id) { mutableStateOf<String?>(null) }
    val mcqOptions = remember(exercise.id) {
        if (exercise.type == MCQ_TYPE) (exercise.distractors + exercise.answer).shuffled() else emptyList()
    }
    val progressFraction = if (state.totalExercises > 0) state.currentIndex / state.totalExercises.toFloat() else 0f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.weight(1f).height(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${state.currentIndex}/${state.totalExercises}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = exercise.prompt,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        exercise.code?.let { code ->
            CodeBlock(code = code, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.weight(1f))

        if (!state.isAnswered) {
            if (exercise.type == MCQ_TYPE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mcqOptions.forEach { option ->
                        McqOptionButton(
                            text = option,
                            state = if (option == selectedOption) McqOptionState.SELECTED else McqOptionState.NORMAL,
                            onClick = { selectedOption = option }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { selectedOption?.let { onSubmit(it) } },
                    enabled = selectedOption != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("COMPROBAR", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onSubmit(answer) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("COMPROBAR", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            if (exercise.type == MCQ_TYPE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mcqOptions.forEach { option ->
                        val optionState = when {
                            option != state.selectedAnswer -> McqOptionState.NORMAL
                            state.isCorrect -> McqOptionState.CORRECT
                            else -> McqOptionState.INCORRECT
                        }
                        McqOptionButton(text = option, state = optionState, onClick = {}, locked = true)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            FeedbackBanner(
                isCorrect = state.isCorrect,
                answer = exercise.answer,
                explanation = exercise.explanation
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SIGUIENTE", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private enum class McqOptionState { NORMAL, SELECTED, CORRECT, INCORRECT }

@Composable
private fun McqOptionButton(
    text: String,
    state: McqOptionState,
    onClick: () -> Unit,
    locked: Boolean = false
) {
    val extended = OopsTheme.extendedColors
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when (state) {
        McqOptionState.NORMAL -> MaterialTheme.colorScheme.outline
        McqOptionState.SELECTED -> MaterialTheme.colorScheme.primary
        McqOptionState.CORRECT -> extended.success
        McqOptionState.INCORRECT -> MaterialTheme.colorScheme.error
    }
    // CORRECT in light mode is paired with an opaque offset "hard shadow" rect drawn behind the
    // card (see drawBehind below), mirroring ThemedCard.kt. That trick only works if the card's
    // own fill is fully opaque -- a translucent (alpha) fill would let the dark shadow rect bleed
    // through the whole card instead of just peeking out at the offset edge. So we use an opaque
    // pastel blend (lerp) for that one case instead of alpha compositing.
    val backgroundColor = when (state) {
        McqOptionState.NORMAL -> MaterialTheme.colorScheme.surface
        McqOptionState.SELECTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        McqOptionState.CORRECT -> if (extended.isDark) {
            extended.success.copy(alpha = 0.15f)
        } else {
            lerp(MaterialTheme.colorScheme.surface, extended.success, 0.18f)
        }
        McqOptionState.INCORRECT -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    }

    // Themed decoration for the CORRECT state: dark = green glow, light = hard shadow offset,
    // matching the pattern used in ThemedCard.kt.
    val isCorrectState = state == McqOptionState.CORRECT
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (isCorrectState && !extended.isDark) {
                    val offsetPx = 3.dp.toPx()
                    drawRoundRect(
                        color = extended.hardShadowColor,
                        topLeft = Offset(offsetPx, offsetPx),
                        size = size,
                        cornerRadius = CornerRadius(14.dp.toPx())
                    )
                }
            }
            .then(
                if (isCorrectState && extended.isDark) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = shape,
                        ambientColor = extended.success,
                        spotColor = extended.success
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(backgroundColor)
            .border(2.dp, borderColor, shape)
            .clickable(enabled = state == McqOptionState.NORMAL && !locked, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            if (state == McqOptionState.CORRECT) {
                Text("✓", color = extended.success, style = MaterialTheme.typography.titleMedium)
            }
            if (state == McqOptionState.INCORRECT) {
                Text("✗", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FeedbackBanner(isCorrect: Boolean, answer: String, explanation: String) {
    val extended = OopsTheme.extendedColors
    val color = if (isCorrect) extended.success else MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(14.dp)
    // Light mode pairs an opaque offset "hard shadow" rect behind the card (see drawBehind
    // below) with the card fill, same trick as ThemedCard.kt -- needs an opaque fill (lerp
    // blend) rather than alpha compositing, or the shadow rect bleeds through. Dark mode has no
    // full-size rect behind it (just a thin left-border accent), so alpha compositing is fine.
    val backgroundColor = if (extended.isDark) {
        color.copy(alpha = 0.12f)
    } else {
        lerp(MaterialTheme.colorScheme.surface, color, 0.15f)
    }
    val title = when {
        isCorrect && !extended.isDark -> "¡Correcto! +10 XP 🎉"
        isCorrect -> "¡Correcto! +10 XP"
        else -> "Incorrecto. Respuesta: $answer"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (extended.isDark) {
                    // Dark: colored left border accent (3dp).
                    drawRect(
                        color = color,
                        topLeft = Offset.Zero,
                        size = Size(3.dp.toPx(), size.height)
                    )
                } else {
                    // Light: hard shadow offset behind the card, same pattern as ThemedCard.
                    val offsetPx = 4.dp.toPx()
                    drawRoundRect(
                        color = extended.hardShadowColor,
                        topLeft = Offset(offsetPx, offsetPx),
                        size = size,
                        cornerRadius = CornerRadius(14.dp.toPx())
                    )
                }
            }
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (!extended.isDark) Modifier.border(2.dp, color, shape) else Modifier
            )
            .padding(start = if (extended.isDark) 17.dp else 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

- [ ] **Step 2: Reduce SessionScreen to a thin wrapper around ExerciseAnswerCard**

Replace `app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.session

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.ui.components.ExerciseAnswerCard
import com.zconte.oopsapp.ui.components.ExerciseAnswerState

@Composable
fun SessionScreen(
    onSessionComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSessionComplete) {
        if (uiState.isSessionComplete) onSessionComplete()
    }

    val exercise = uiState.currentExercise
    if (exercise == null) {
        Text(
            "Cargando sesion...",
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    val currentIndex = (uiState.totalExercises - uiState.queue.size + 1).coerceAtLeast(1)

    ExerciseAnswerCard(
        state = ExerciseAnswerState(
            exercise = exercise,
            currentIndex = currentIndex,
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
```

`SessionViewModel.kt` is unchanged in this task.

- [ ] **Step 3: Build and run the existing session flow before continuing**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Then run the app and complete a daily session on-device or emulator, confirming the visuals (MCQ options, feedback banner, correct/incorrect styling in both themes) look byte-identical to before the extraction — this refactor must be behavior-preserving.

- [ ] **Step 4: CheckpointViewModel**

Create `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.checkpoint

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCase
import com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
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

data class CheckpointUiState(
    val queue: List<Exercise> = emptyList(),
    val currentExercise: ExerciseContent? = null,
    val currentIndex: Int = 0,
    val totalExercises: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val isComplete: Boolean = false,
    val result: CheckpointResult? = null
)

@HiltViewModel
class CheckpointViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCheckpointSessionUseCase: GetCheckpointSessionUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val completeCheckpointUseCase: CompleteCheckpointUseCase,
    private val json: Json
) : ViewModel() {

    private val sectionId: String = checkNotNull(savedStateHandle["sectionId"])

    private val _uiState = MutableStateFlow(CheckpointUiState())
    val uiState: StateFlow<CheckpointUiState> = _uiState.asStateFlow()

    private var correctCount = 0
    private var pendingAnswerJob: Job? = null

    init {
        viewModelScope.launch {
            val queue = getCheckpointSessionUseCase(sectionId)
            if (queue.isEmpty()) {
                _uiState.update { it.copy(isComplete = true, result = CheckpointResult(0, false)) }
            } else {
                _uiState.update {
                    it.copy(
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
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isEmpty()) {
            viewModelScope.launch {
                pendingAnswerJob?.join()
                val result = completeCheckpointUseCase(
                    sectionId = sectionId,
                    kind = CheckpointKind.REVIEW,
                    correctCount = correctCount,
                    totalCount = _uiState.value.totalExercises,
                    today = LocalDate.now()
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

- [ ] **Step 5: CheckpointScreen**

Create `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt`:

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
fun CheckpointScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckpointViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isComplete) {
        CheckpointResultView(result = uiState.result, onContinue = onFinished, modifier = modifier)
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
private fun CheckpointResultView(result: CheckpointResult?, onContinue: () -> Unit, modifier: Modifier = Modifier) {
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
            text = if (passed) "¡Checkpoint superado!" else "Casi lo logras",
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
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("CONTINUAR", style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

- [ ] **Step 6: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. `CheckpointScreen`/`CheckpointViewModel` aren't reachable from any nav route yet — that's Task 9.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt app/src/main/java/com/zconte/oopsapp/ui/checkpoint/
git commit -m "Extract shared ExerciseAnswerCard; add CheckpointViewModel/CheckpointScreen"
```

---

### Task 9: Sesión por unidad + rutas de navegación nuevas

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCaseTest.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/MainActivity.kt`

**Interfaces:**
- Consumes: `ExerciseRepository.getExercisesByUnit` (Task 5), `MarkUnitProgressUseCase` (Task 6), `CheckpointScreen`/`CheckpointViewModel` (Task 8).
- Produces: `GetUnitSessionUseCase(unitId): List<Exercise>`. `OopsDestinations.UNIT_SESSION = "unit_session/{unitId}"`, `OopsDestinations.CHECKPOINT = "checkpoint/{sectionId}"`. `SessionScreen`/`SessionViewModel` now handle both the daily route and the per-unit route via an optional `unitId` nav arg. Consumed by Task 10 (Ruta's tap targets navigate to these routes).

**Context:** `SessionViewModel` gets exactly one new piece of state — an optional `unitId` read from `SavedStateHandle` — that decides which use case populates the initial queue and whether completion also calls `MarkUnitProgressUseCase`. The existing `pendingAnswerJob?.join()`-before-completing ordering is untouched. Playing a unit's exercises for the first time should reward the player the same as a daily session (streak/XP) — `UpdateStreakUseCase` is called in both modes; `MarkUnitProgressUseCase` is called additionally when in unit mode.

- [ ] **Step 1: GetUnitSessionUseCase with a test**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForUnitSession(
    private val byUnit: Map<String, List<Exercise>>
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = byUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = emptyList()
}

class GetUnitSessionUseCaseTest {

    @Test
    fun `returns every exercise belonging to the requested unit`() = runTest {
        val exercise1 = Exercise("ex-1", "unit-1", "fill_blank", "{}", 1)
        val exercise2 = Exercise("ex-2", "unit-1", "mcq", "{}", 1)
        val repository = FakeExerciseRepositoryForUnitSession(mapOf("unit-1" to listOf(exercise1, exercise2)))
        val useCase = GetUnitSessionUseCase(repository)

        val result = useCase("unit-1")

        assertEquals(listOf("ex-1", "ex-2"), result.map { it.id })
    }
}
```

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import javax.inject.Inject

class GetUnitSessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(unitId: String): List<Exercise> = exerciseRepository.getExercisesByUnit(unitId)
}
```

- [ ] **Step 2: Generalize SessionViewModel for unit-mode**

Replace `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCase
import com.zconte.oopsapp.domain.usecase.GetUnitSessionUseCase
import com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCase
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

data class SessionUiState(
    val queue: List<Exercise> = emptyList(),
    val currentExercise: ExerciseContent? = null,
    val selectedAnswer: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val isCompleting: Boolean = false,
    val isSessionComplete: Boolean = false,
    val totalExercises: Int = 0
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTodaySessionUseCase: GetTodaySessionUseCase,
    private val getUnitSessionUseCase: GetUnitSessionUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val markUnitProgressUseCase: MarkUnitProgressUseCase,
    private val json: Json
) : ViewModel() {

    private val unitId: String? = savedStateHandle["unitId"]

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var pendingAnswerJob: Job? = null

    init {
        viewModelScope.launch {
            val queue = unitId?.let { getUnitSessionUseCase(it) } ?: getTodaySessionUseCase(LocalDate.now())
            if (queue.isEmpty()) {
                // Nothing due and nothing new: nothing to show, so the session is trivially complete.
                _uiState.update { it.copy(isSessionComplete = true) }
            } else {
                _uiState.update {
                    it.copy(queue = queue, totalExercises = queue.size, currentExercise = decode(queue.first()))
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
                // Wait for the last exercise's answer write before completing, so navigating
                // away (and clearing this ViewModel's scope) can't cancel it mid-flight.
                pendingAnswerJob?.join()
                updateStreakUseCase(LocalDate.now())
                unitId?.let { markUnitProgressUseCase(it, LocalDate.now()) }
                _uiState.update { it.copy(isSessionComplete = true) }
            }
        } else {
            _uiState.update {
                it.copy(
                    queue = remaining,
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

- [ ] **Step 3: New nav destinations**

Replace `app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt`:

```kotlin
package com.zconte.oopsapp.navigation

object OopsDestinations {
    const val HOME = "home"
    const val SESSION = "session"
    const val UNIT_SESSION = "unit_session/{unitId}"
    const val CHECKPOINT = "checkpoint/{sectionId}"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
}
```

- [ ] **Step 4: Wire the new routes in the nav host**

Replace `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`:

```kotlin
package com.zconte.oopsapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zconte.oopsapp.ui.checkpoint.CheckpointScreen
import com.zconte.oopsapp.ui.home.HomeScreen
import com.zconte.oopsapp.ui.progress.ProgressScreen
import com.zconte.oopsapp.ui.session.SessionScreen
import com.zconte.oopsapp.ui.settings.SettingsScreen

@Composable
fun OopsNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = OopsDestinations.HOME, modifier = modifier) {
        composable(OopsDestinations.HOME) {
            HomeScreen(
                onStudyClick = { navController.navigate(OopsDestinations.SESSION) },
                onProgressClick = {
                    navController.navigate(OopsDestinations.PROGRESS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(OopsDestinations.SESSION) {
            SessionScreen(
                onSessionComplete = { navController.popBackStack() }
            )
        }
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
            arguments = listOf(navArgument("sectionId") { type = NavType.StringType })
        ) {
            CheckpointScreen(
                onFinished = { navController.popBackStack() }
            )
        }
        composable(OopsDestinations.PROGRESS) {
            ProgressScreen(
                onPlayUnit = { unitId -> navController.navigate("unit_session/$unitId") },
                onOpenCheckpoint = { sectionId -> navController.navigate("checkpoint/$sectionId") }
            )
        }
        composable(OopsDestinations.SETTINGS) {
            SettingsScreen()
        }
    }
}
```

- [ ] **Step 5: Treat all three exercise-answering routes as full-screen (no bottom bar)**

In `app/src/main/java/com/zconte/oopsapp/MainActivity.kt`, find:

```kotlin
import com.zconte.oopsapp.navigation.OopsBottomBar
import com.zconte.oopsapp.navigation.OopsDestinations
import com.zconte.oopsapp.navigation.OopsNavHost
```

Replace with:

```kotlin
import com.zconte.oopsapp.navigation.OopsBottomBar
import com.zconte.oopsapp.navigation.OopsDestinations
import com.zconte.oopsapp.navigation.OopsNavHost

private val FULL_SCREEN_ROUTES = setOf(
    OopsDestinations.SESSION,
    OopsDestinations.UNIT_SESSION,
    OopsDestinations.CHECKPOINT
)
```

Find:

```kotlin
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute != OopsDestinations.SESSION) {
                            OopsBottomBar(navController, currentRoute)
                        }
                    }
                ) { innerPadding ->
                    OopsNavHost(
                        navController = navController,
                        modifier = Modifier.padding(
                            bottom = if (currentRoute == OopsDestinations.SESSION) {
                                0.dp
                            } else {
                                innerPadding.calculateBottomPadding()
                            }
                        )
                    )
                }
```

Replace with:

```kotlin
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute !in FULL_SCREEN_ROUTES) {
                            OopsBottomBar(navController, currentRoute)
                        }
                    }
                ) { innerPadding ->
                    OopsNavHost(
                        navController = navController,
                        modifier = Modifier.padding(
                            bottom = if (currentRoute in FULL_SCREEN_ROUTES) {
                                0.dp
                            } else {
                                innerPadding.calculateBottomPadding()
                            }
                        )
                    )
                }
```

**Note:** `currentRoute` for a parameterized destination resolves to the route *pattern* (e.g. `"unit_session/{unitId}"`), not the resolved path with the real id substituted in — that's exactly what `OopsDestinations.UNIT_SESSION`/`CHECKPOINT` already are, so the `in FULL_SCREEN_ROUTES` check matches correctly without any string manipulation.

- [ ] **Step 6: Build and test**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass including the new `GetUnitSessionUseCaseTest`. `ProgressScreen`'s call site in `OopsNavHost.kt` now requires `onPlayUnit`/`onOpenCheckpoint` parameters that don't exist on `ProgressScreen` yet — this is expected to fail to compile until Task 10 updates `ProgressScreen`'s signature. If you're executing tasks strictly in order, this is fine; do not work around it by reverting this task's `OopsNavHost.kt` change.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCase.kt app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCaseTest.kt app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt app/src/main/java/com/zconte/oopsapp/navigation/ app/src/main/java/com/zconte/oopsapp/MainActivity.kt
git commit -m "Generalize SessionViewModel for per-unit play; add unit_session/checkpoint routes"
```

---

### Task 10: Ruta — camino Sección → Unidad

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `GetLearningPathUseCase`, `SectionPath`, `UnitProgress` (Task 6).
- Produces: `ProgressScreen(modifier, onPlayUnit: (String) -> Unit, onOpenCheckpoint: (String) -> Unit, viewModel)` — the two new required parameters `OopsNavHost.kt` already calls (Task 9, Step 4).

**Context:** No mockup exists for a Section→Unit path (Global Constraints) — this reuses the existing visual language verbatim: the same dark header bar, `PressStart2P` labels, colored-dot row pattern, and `OopsExtendedColors` locked-state colors already established in the current `ProgressScreen.kt`/`OopsExtendedColors`. Two deliberate, visible behavior changes from what exists today, both because the old flat-domain model they were built for no longer exists:

1. The static "collect() — ahora ▶" current-step chip (added in the earlier design-corrections round as an acknowledged placeholder for a concept the data model didn't support yet) is retired — its job is now done for real by the new per-unit rows, which are actually navigable, not static text.
2. The header's global percentage changes meaning: it was SM-2 mastery-based readiness on the single Streams domain; it becomes "unidades completadas / unidades totales" across every section (locked and unlocked) — a curriculum-completion metric, appropriate now that there's an actual curriculum instead of one domain.

- [ ] **Step 1: Replace ProgressViewModel**

Replace `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.usecase.GetLearningPathUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val sections: List<SectionPath> = emptyList()
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val getLearningPathUseCase: GetLearningPathUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val sections = getLearningPathUseCase()
            _uiState.update { it.copy(sections = sections) }
        }
    }
}
```

- [ ] **Step 2: Replace ProgressScreen**

Replace `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitProgress
import com.zconte.oopsapp.ui.theme.OopsTheme
import com.zconte.oopsapp.ui.theme.PressStart2P
import com.zconte.oopsapp.ui.theme.RouteHeaderBackground

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    onPlayUnit: (String) -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allUnits = uiState.sections.flatMap { it.units }
    val globalPercent = if (allUnits.isEmpty()) 0 else (allUnits.count { it.completed } * 100) / allUnits.size

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RouteHeaderBackground)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ruta 1Z0-830",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "$globalPercent%",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
                color = OopsTheme.extendedColors.success
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            items(uiState.sections) { sectionPath ->
                SectionPathBlock(
                    sectionPath = sectionPath,
                    onPlayUnit = onPlayUnit,
                    onOpenCheckpoint = onOpenCheckpoint
                )
            }
        }
    }
}

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

@Composable
private fun CheckpointRow(onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
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
                .background(MaterialTheme.colorScheme.tertiary)
        )
        Column {
            Text(
                text = "CHECKPOINT",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = "Repaso opcional de esta seccion",
                style = MaterialTheme.typography.bodyMedium,
                color = extended.lockedText
            )
        }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — this resolves the `ProgressScreen(onPlayUnit, onOpenCheckpoint, ...)` call site Task 9 left uncompilable.

- [ ] **Step 4: Run the app and verify on-device or emulator**

Launch the app, go to Ruta. Confirm: Fundamentos section shows 3 units (first unlocked, other two locked with the 🔒 hint), tapping the unlocked unit opens a session with that unit's exercises, finishing it marks it complete and unlocks the next unit, finishing all of Fundamentos' units reveals the CHECKPOINT row and unlocks the Streams section's first unit. Tapping CHECKPOINT opens a review checkpoint over Fundamentos' content, answering it through to the result screen, tapping CONTINUAR returns to Ruta. Confirm this all renders correctly in both light and dark theme (reusing `OopsExtendedColors`/`ThemedCard`-adjacent patterns, so it should, but verify — this is new layout, not just new colors on old layout).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/progress/
git commit -m "Ruta: Section -> Unit path with playable unit rows and end-of-section checkpoint entry"
```

---

### Task 11: Home — tarjeta "TU RUTA" apuntando al nuevo modelo

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `GetLearningPathUseCase`, `SectionPath` (Task 6).
- Produces: no new public interfaces — this is the last task, it only fixes the one remaining call site that still assumed the old flat `"streams-lambdas"` objective string.

**Context:** `HomeViewModel.uiState.streamsReadiness` (a `Float` keyed to the hardcoded string `"streams-lambdas"`) is replaced with `currentSectionName`/`currentSectionProgress`, driven by whichever section is the player's current one: the first section that isn't fully complete, or the last section if everything is done. Progress for that card is "units completed / units in that section" — the same metric Task 10's Ruta header uses, just scoped to one section instead of all of them, so the number on Home and the number you see when you tap into Ruta are computed the same way.

- [ ] **Step 1: Replace HomeViewModel**

Replace `app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.data.content.ContentSeeder
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.usecase.GetLearningPathUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val streak: Int = 0,
    val xp: Int = 0,
    val isReady: Boolean = false,
    val currentSectionName: String = "",
    val currentSectionProgress: Float = 0f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val getLearningPathUseCase: GetLearningPathUseCase,
    private val contentSeeder: ContentSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contentSeeder.seedIfNeeded()
            refreshStats()
            _uiState.update { it.copy(isReady = true) }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshStats() }
    }

    private suspend fun refreshStats() {
        val stats = progressRepository.getUserStats()
        val sections = getLearningPathUseCase()
        val currentSection = sections.firstOrNull { !it.completed } ?: sections.lastOrNull()
        val progress = currentSection?.let { section ->
            if (section.units.isEmpty()) 0f else section.units.count { it.completed }.toFloat() / section.units.size
        } ?: 0f

        _uiState.update {
            it.copy(
                streak = stats.streak,
                xp = stats.xp,
                currentSectionName = currentSection?.section?.name ?: "",
                currentSectionProgress = progress
            )
        }
    }
}
```

- [ ] **Step 2: Point the TU RUTA card at the new fields**

In `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`, find:

```kotlin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Streams",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(uiState.streamsReadiness * 100).toInt()}% ▶",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { uiState.streamsReadiness },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
```

Replace with:

```kotlin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.currentSectionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(uiState.currentSectionProgress * 100).toInt()}% ▶",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { uiState.currentSectionProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the app and verify on-device or emulator**

Open Home. Confirm the TU RUTA card shows "Fundamentos de Java" (the current section on a fresh install) with its unit-completion percentage, and tapping it still opens Ruta. Play through Fundamentos' units and confirm the percentage climbs and, once that section is complete, the card switches to showing "Streams y lambdas" as the current section.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/home/
git commit -m "Home: TU RUTA card reflects the current section's unit-completion progress"
```

---

## Final Steps (after all 11 tasks)

1. Dispatch the final whole-branch code review (per subagent-driven-development) covering the full diff across all 11 tasks — pay special attention to the migration/seeder correctness (Tasks 1-4) and the `SessionViewModel`/`CheckpointViewModel` completion-ordering (Tasks 8-9), since both are the highest-risk areas in this plan.
2. Run the full test suite (`./gradlew test`) and the instrumented migration test (`./gradlew connectedAndroidTest`) one more time on the assembled branch, not just per-task.
3. Update `docs/CHANGELOG.md` with a new dated section summarizing: the Section/Unit/Checkpoint data model and migration, the reorganized content (Fundamentos + Streams), unit first-pass progression and gating, the voluntary review checkpoint (68% threshold, feeds SM-2), the retired static Ruta chip, and the new Home/Ruta wiring. Note the deferred placement/skip checkpoint (Fase 2.1b) explicitly as out of scope for this round, not forgotten.
4. Use `superpowers:finishing-a-development-branch` to merge/push per the user's choice.
