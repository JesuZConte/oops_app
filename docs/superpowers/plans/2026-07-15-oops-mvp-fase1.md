# Oops! MVP Fase 1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a working daily-study loop for the "Oops!" Android app — spaced-repetition (SM-2) exercises on Java Streams, with streak/XP tracking, fully offline, per `docs/specs/PROJECT-OOPS.md`.

**Architecture:** MVVM within a single `app` module, split into `domain` (pure Kotlin, zero `android.*` imports), `data` (Room + JSON content loading), `ui` (Compose screens, one ViewModel each), and `di` (Hilt modules). Repository **interfaces** live in `domain/repository` so `domain/usecase` stays testable with fakes; **implementations** live in `data/repository` and are bound via Hilt. This split isn't spelled out in the original spec's task list (Tasks 3/4 don't mention a repository task explicitly) — it's the natural way to satisfy the spec's own constraint that "el motor SRS y los use cases son Kotlin puro sin dependencias de Android" (section 3), so it's folded into Task 3 below.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose, Navigation Compose 2.9.0, Room 2.7.1 (KSP), Hilt 2.56.2 (KSP), kotlinx.serialization 1.7.3, JUnit 4 + kotlinx-coroutines-test.

## Global Constraints

- Platform: Android only. minSdk 26, targetSdk/compileSdk 36 (already set in `app/build.gradle.kts`).
- Language: Kotlin, idiomatic, pure functions in `domain` with no hidden side effects.
- UI: Jetpack Compose + Navigation Compose. One ViewModel per screen; UI state exposed as an immutable `StateFlow`.
- Persistence: Room (SQLite), local-first, no network calls anywhere in this plan.
- DI: Hilt.
- Serialization: kotlinx.serialization for content packs under `assets/content/`.
- `domain/srs` and `domain/usecase` must not import anything from `android.*` — verified by the fact their tests run as plain JUnit (no Robolectric, no instrumentation).
- Package base: `com.zconte.oopsapp` (confirmed with user — matches the already-generated project, spec updated to match).
- Every task with domain logic ships with JUnit tests before being considered done (spec section 10).

---

## File Structure

```
app/src/main/java/com/zconte/oopsapp/
├── OopsApplication.kt                         # Task 1
├── MainActivity.kt                             # Task 1 (modified)
├── navigation/
│   ├── OopsDestinations.kt                     # Task 1
│   └── OopsNavHost.kt                          # Task 1
├── domain/
│   ├── model/
│   │   ├── Topic.kt                            # Task 2
│   │   ├── Exercise.kt                         # Task 2
│   │   ├── ExerciseContent.kt                  # Task 2
│   │   ├── ReviewState.kt                      # Task 2
│   │   └── UserStats.kt                        # Task 2
│   ├── srs/
│   │   └── SchedulerSm2.kt                     # Task 2
│   ├── repository/
│   │   ├── ExerciseRepository.kt               # Task 3
│   │   └── ProgressRepository.kt               # Task 3
│   └── usecase/
│       ├── GetTodaySessionUseCase.kt           # Task 4
│       ├── SubmitAnswerUseCase.kt              # Task 4
│       └── UpdateStreakUseCase.kt              # Task 4
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── TopicEntity.kt                  # Task 3
│   │   │   ├── ExerciseEntity.kt               # Task 3
│   │   │   ├── ReviewStateEntity.kt            # Task 3
│   │   │   └── UserStatsEntity.kt              # Task 3
│   │   ├── dao/
│   │   │   ├── TopicDao.kt                     # Task 3
│   │   │   ├── ExerciseDao.kt                  # Task 3
│   │   │   ├── ReviewStateDao.kt               # Task 3
│   │   │   └── UserStatsDao.kt                 # Task 3
│   │   └── AppDatabase.kt                      # Task 3
│   ├── content/
│   │   ├── ContentPack.kt                      # Task 3
│   │   ├── ContentMapper.kt                    # Task 3
│   │   ├── ContentLoader.kt                    # Task 3
│   │   └── ContentSeeder.kt                    # Task 3
│   └── repository/
│       ├── ExerciseRepositoryImpl.kt           # Task 3
│       └── ProgressRepositoryImpl.kt           # Task 3
├── ui/
│   ├── session/
│   │   ├── SessionScreen.kt                    # Task 1 (placeholder) / Task 5 (real)
│   │   └── SessionViewModel.kt                 # Task 5
│   ├── home/
│   │   ├── HomeScreen.kt                       # Task 1 (placeholder) / Task 6 (real)
│   │   └── HomeViewModel.kt                    # Task 6
│   ├── progress/
│   │   ├── ProgressScreen.kt                   # Task 1 (placeholder) / Task 7 (real)
│   │   └── ProgressViewModel.kt                # Task 7
│   └── theme/                                   # already exists, untouched
└── di/
    ├── SerializationModule.kt                  # Task 3
    ├── DatabaseModule.kt                       # Task 3
    └── RepositoryModule.kt                     # Task 3

app/src/main/assets/content/streams.json         # Task 3

app/src/test/java/com/zconte/oopsapp/
├── domain/srs/SchedulerSm2Test.kt               # Task 2
├── data/content/ContentPackParsingTest.kt       # Task 3
├── data/content/ContentMapperTest.kt            # Task 3
└── domain/usecase/
    ├── GetTodaySessionUseCaseTest.kt            # Task 4
    ├── SubmitAnswerUseCaseTest.kt                # Task 4
    └── UpdateStreakUseCaseTest.kt                # Task 4
```

---

### Task 1: Bootstrap del proyecto

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/zconte/oopsapp/MainActivity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/OopsApplication.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Produces: three navigation routes (`"home"`, `"session"`, `"progress"`) used by every later UI task; `OopsNavHost(navController: NavHostController)` composable that later tasks slot real screens into by editing the same `composable(...)` blocks.

No domain logic in this task, so no TDD cycle — the deliverable is verified by building and running the app.

- [ ] **Step 1: Add new library versions and coordinates to the version catalog**

Edit `gradle/libs.versions.toml`, adding to `[versions]`:

```toml
room = "2.7.1"
ksp = "2.2.10-2.0.2"
hilt = "2.56.2"
navigationCompose = "2.9.0"
kotlinxSerialization = "1.7.3"
kotlinxCoroutinesTest = "1.9.0"
hiltNavigationCompose = "1.2.0"
```

Add to `[libraries]`:

```toml
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutinesTest" }
```

Add to `[plugins]`:

```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Note: `ksp = "2.2.10-2.0.2"` must match the exact Kotlin version (`2.2.10`). If Gradle fails to resolve it, check https://github.com/google/ksp/releases for the KSP release published against `2.2.10` and use that instead — this is the one version pin in this plan most likely to need a bump.

- [ ] **Step 2: Register the new plugins at the root**

Edit `build.gradle.kts`:

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 3: Apply the plugins and add dependencies in the app module**

Edit `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}
```

In the same file, add to the `dependencies` block (keep everything already there):

```kotlin
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 4: Create the Hilt Application class**

Create `app/src/main/java/com/zconte/oopsapp/OopsApplication.kt`:

```kotlin
package com.zconte.oopsapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OopsApplication : Application()
```

- [ ] **Step 5: Register the Application class and enable Hilt on MainActivity**

Edit `app/src/main/AndroidManifest.xml`, add `android:name=".OopsApplication"` to the `<application>` tag:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".OopsApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Oopsapp">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.Oopsapp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 6: Define navigation routes**

Create `app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt`:

```kotlin
package com.zconte.oopsapp.navigation

object OopsDestinations {
    const val HOME = "home"
    const val SESSION = "session"
    const val PROGRESS = "progress"
}
```

- [ ] **Step 7: Create placeholder screens**

Create `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onStudyClick: () -> Unit,
    onProgressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Oops! — Home")
        Button(onClick = onStudyClick) { Text("Estudiar hoy") }
        Button(onClick = onProgressClick) { Text("Ver progreso") }
    }
}
```

Create `app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SessionScreen(
    onSessionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Oops! — Session")
    }
}
```

Create `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Oops! — Progress")
    }
}
```

- [ ] **Step 8: Wire the NavHost**

Create `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`:

```kotlin
package com.zconte.oopsapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zconte.oopsapp.ui.home.HomeScreen
import com.zconte.oopsapp.ui.progress.ProgressScreen
import com.zconte.oopsapp.ui.session.SessionScreen

@Composable
fun OopsNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = OopsDestinations.HOME) {
        composable(OopsDestinations.HOME) {
            HomeScreen(
                onStudyClick = { navController.navigate(OopsDestinations.SESSION) },
                onProgressClick = { navController.navigate(OopsDestinations.PROGRESS) }
            )
        }
        composable(OopsDestinations.SESSION) {
            SessionScreen(
                onSessionComplete = { navController.popBackStack() }
            )
        }
        composable(OopsDestinations.PROGRESS) {
            ProgressScreen()
        }
    }
}
```

- [ ] **Step 9: Point MainActivity at the NavHost and enable Hilt**

Edit `app/src/main/java/com/zconte/oopsapp/MainActivity.kt`, replacing its contents:

```kotlin
package com.zconte.oopsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.zconte.oopsapp.navigation.OopsNavHost
import com.zconte.oopsapp.ui.theme.OopsappTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OopsappTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OopsNavHost()
                }
            }
        }
    }
}
```

Note: this removes the template's `Greeting`/`GreetingPreview` composables — they're no longer referenced. Also delete their leftover reference in `app/src/androidTest/java/com/zconte/oopsapp/ExampleInstrumentedTest.kt` and `app/src/test/java/com/zconte/oopsapp/ExampleUnitTest.kt` only if they fail to compile after this change (check by running the build in Step 10 first; the default template's example tests don't reference `Greeting`, so they should be unaffected).

- [ ] **Step 10: Build and run**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. If KSP/Hilt version resolution fails, adjust the `ksp` version per the note in Step 1.

Install on a device/emulator and confirm: Home screen shows two buttons; "Estudiar hoy" navigates to the Session placeholder; back navigation returns to Home; "Ver progreso" navigates to the Progress placeholder.

- [ ] **Step 11: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/zconte/oopsapp
git commit -m "feat: bootstrap Compose/Room/Hilt/Navigation scaffold with empty screens"
```

---

### Task 2: Motor SRS (domain/srs)

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/ReviewState.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/Topic.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/Exercise.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseContent.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/UserStats.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/srs/SchedulerSm2.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/srs/SchedulerSm2Test.kt`

**Interfaces:**
- Produces: `data class ReviewState(exerciseId: String, easeFactor: Double, intervalDays: Int, repetitions: Int, dueDate: LocalDate)`; `object SchedulerSm2 { fun review(state: ReviewState, quality: Int, today: LocalDate): ReviewState }`; `data class Exercise(id: String, topicId: String, type: String, payload: String, difficulty: Int)`; `data class ExerciseContent(id: String, type: String, difficulty: Int, prompt: String, code: String?, answer: String, distractors: List<String>, explanation: String)` (annotated `@Serializable`, used by both Task 3's content loader and Task 5's session screen); `data class Topic(id: String, name: String, certObjective: String, orderIndex: Int)`; `data class UserStats(streak: Int, xp: Int, lastStudyDate: LocalDate?)`.
- Consumes: nothing (pure Kotlin, no dependencies on other tasks).

- [ ] **Step 1: Create the domain models**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/ReviewState.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

import java.time.LocalDate

data class ReviewState(
    val exerciseId: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: LocalDate
)
```

Create `app/src/main/java/com/zconte/oopsapp/domain/model/Topic.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class Topic(
    val id: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int
)
```

Create `app/src/main/java/com/zconte/oopsapp/domain/model/Exercise.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class Exercise(
    val id: String,
    val topicId: String,
    val type: String,
    val payload: String,
    val difficulty: Int
)
```

Create `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseContent.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseContent(
    val id: String,
    val type: String,
    val difficulty: Int,
    val prompt: String,
    val code: String? = null,
    val answer: String,
    val distractors: List<String> = emptyList(),
    val explanation: String
)
```

Create `app/src/main/java/com/zconte/oopsapp/domain/model/UserStats.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

import java.time.LocalDate

data class UserStats(
    val streak: Int,
    val xp: Int,
    val lastStudyDate: LocalDate?
)
```

- [ ] **Step 2: Write the failing tests for SchedulerSm2**

Create `app/src/test/java/com/zconte/oopsapp/domain/srs/SchedulerSm2Test.kt`:

```kotlin
package com.zconte.oopsapp.domain.srs

import com.zconte.oopsapp.domain.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SchedulerSm2Test {

    private val today: LocalDate = LocalDate.of(2026, 7, 15)

    @Test
    fun `first review with passing quality sets interval to 1 day`() {
        val state = ReviewState(
            exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 0,
            repetitions = 0, dueDate = today
        )

        val result = SchedulerSm2.review(state, quality = 4, today = today)

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(today.plusDays(1), result.dueDate)
        assertEquals(2.5, result.easeFactor, 0.0001)
    }

    @Test
    fun `second review with passing quality sets interval to 6 days`() {
        val afterFirst = ReviewState(
            exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1,
            repetitions = 1, dueDate = today
        )

        val result = SchedulerSm2.review(afterFirst, quality = 4, today = today)

        assertEquals(2, result.repetitions)
        assertEquals(6, result.intervalDays)
        assertEquals(today.plusDays(6), result.dueDate)
    }

    @Test
    fun `failing quality resets repetitions and reschedules for tomorrow`() {
        val afterSeveralReviews = ReviewState(
            exerciseId = "ex-1", easeFactor = 2.6, intervalDays = 16,
            repetitions = 3, dueDate = today
        )

        val result = SchedulerSm2.review(afterSeveralReviews, quality = 2, today = today)

        assertEquals(0, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(today.plusDays(1), result.dueDate)
        assertEquals(2.6, result.easeFactor, 0.0001)
    }

    @Test
    fun `third review grows interval using ease factor`() {
        val afterSecond = ReviewState(
            exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 6,
            repetitions = 2, dueDate = today
        )

        val result = SchedulerSm2.review(afterSecond, quality = 5, today = today)

        assertEquals(3, result.repetitions)
        assertEquals(2.6, result.easeFactor, 0.0001)
        assertEquals(16, result.intervalDays)
        assertEquals(today.plusDays(16), result.dueDate)
    }

    @Test
    fun `ease factor never drops below 1_3`() {
        val lowEase = ReviewState(
            exerciseId = "ex-1", easeFactor = 1.3, intervalDays = 6,
            repetitions = 2, dueDate = today
        )

        val result = SchedulerSm2.review(lowEase, quality = 3, today = today)

        assertEquals(1.3, result.easeFactor, 0.0001)
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.srs.SchedulerSm2Test"`
Expected: FAIL with "Unresolved reference: SchedulerSm2" (the object doesn't exist yet).

- [ ] **Step 4: Implement SchedulerSm2**

Create `app/src/main/java/com/zconte/oopsapp/domain/srs/SchedulerSm2.kt`:

```kotlin
package com.zconte.oopsapp.domain.srs

import com.zconte.oopsapp.domain.model.ReviewState
import java.time.LocalDate

object SchedulerSm2 {
    fun review(state: ReviewState, quality: Int, today: LocalDate): ReviewState {
        if (quality < 3) {
            return state.copy(
                repetitions = 0,
                intervalDays = 1,
                dueDate = today.plusDays(1)
            )
        }
        val newEase = (
            state.easeFactor +
                (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
            ).coerceAtLeast(1.3)
        val reps = state.repetitions + 1
        val interval = when (reps) {
            1 -> 1
            2 -> 6
            else -> Math.round(state.intervalDays * newEase).toInt()
        }
        return state.copy(
            easeFactor = newEase,
            repetitions = reps,
            intervalDays = interval,
            dueDate = today.plusDays(interval.toLong())
        )
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.srs.SchedulerSm2Test"`
Expected: PASS, 5 tests green.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain app/src/test/java/com/zconte/oopsapp/domain
git commit -m "feat: implement SM-2 scheduler with domain models and unit tests"
```

---

### Task 3: Capa de datos (data/local + data/content + data/repository)

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/TopicEntity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/ExerciseEntity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/ReviewStateEntity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/UserStatsEntity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/dao/TopicDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/dao/ReviewStateDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/dao/UserStatsDao.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/content/ContentPack.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/content/ContentMapper.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/content/ContentLoader.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/repository/ProgressRepository.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/repository/ProgressRepositoryImpl.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/di/SerializationModule.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`
- Create: `app/src/main/assets/content/streams.json`
- Test: `app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/data/content/ContentMapperTest.kt`

**Interfaces:**
- Consumes: `Exercise`, `ExerciseContent`, `ReviewState`, `Topic`, `UserStats` from Task 2 (`domain/model`).
- Produces:
  `interface ExerciseRepository { suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise>; suspend fun getNewExercises(limit: Int): List<Exercise>; suspend fun getReviewState(exerciseId: String): ReviewState?; suspend fun saveReviewState(state: ReviewState) }`
  `interface ProgressRepository { suspend fun getUserStats(): UserStats; suspend fun saveUserStats(stats: UserStats); suspend fun getReadinessByObjective(): Map<String, Float> }`
  Both interfaces are bound to their `*Impl` via Hilt (`RepositoryModule`), and consumed by Task 4's use cases.

The parsing/mapping logic here is pure Kotlin (no `Context`, no Room) and gets tests. The Room entities/DAOs and the thin `Context.assets` wrapper are infrastructure glue verified by the build + a manual run, consistent with spec section 10 ("tests... cuando aplique lógica de dominio").

- [ ] **Step 1: Create the Room entities**

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/TopicEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int
)
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/ExerciseEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val type: String,
    val payload: String,
    val difficulty: Int
)
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/ReviewStateEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_state")
data class ReviewStateEntity(
    @PrimaryKey val exerciseId: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: Long
)
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/UserStatsEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 0,
    val streak: Int,
    val xp: Int,
    val lastStudyDate: Long
)
```

- [ ] **Step 2: Create the DAOs**

Create `app/src/main/java/com/zconte/oopsapp/data/local/dao/TopicDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.TopicEntity

@Dao
interface TopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<TopicEntity>)

    @Query("SELECT * FROM topics ORDER BY orderIndex")
    suspend fun getAll(): List<TopicEntity>
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt`:

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

    @Query(
        """
        SELECT topics.certObjective AS objective, COUNT(*) AS totalCount
        FROM exercises
        INNER JOIN topics ON exercises.topicId = topics.id
        GROUP BY topics.certObjective
        """
    )
    suspend fun getTotalCountByObjective(): List<ObjectiveTotalCount>
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/dao/ReviewStateDao.kt`:

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

    @Query(
        """
        SELECT topics.certObjective AS objective, COUNT(*) AS masteredCount
        FROM review_state
        INNER JOIN exercises ON review_state.exerciseId = exercises.id
        INNER JOIN topics ON exercises.topicId = topics.id
        WHERE review_state.repetitions >= 2
        GROUP BY topics.certObjective
        """
    )
    suspend fun getMasteredCountByObjective(): List<ObjectiveMasteryCount>
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/local/dao/UserStatsDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.UserStatsEntity

@Dao
interface UserStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UserStatsEntity)

    @Query("SELECT * FROM user_stats WHERE id = 0")
    suspend fun get(): UserStatsEntity?
}
```

- [ ] **Step 3: Create AppDatabase**

Create `app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt`:

```kotlin
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
```

- [ ] **Step 4: Write the failing parsing/mapping tests**

Create `app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentPackParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a content pack with one exercise`() {
        val raw = """
            {
              "topicId": "java-streams",
              "name": "Streams y lambdas",
              "certObjective": "streams-lambdas",
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
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals("java-streams", pack.topicId)
        assertEquals("streams-lambdas", pack.certObjective)
        assertEquals(1, pack.exercises.size)
        assertEquals("collect", pack.exercises.first().answer)
        assertEquals(listOf("map", "reduce", "forEach"), pack.exercises.first().distractors)
    }

    @Test
    fun `exercise without code field parses with null code`() {
        val raw = """
            {
              "topicId": "java-streams",
              "name": "Streams y lambdas",
              "certObjective": "streams-lambdas",
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
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals(null, pack.exercises.first().code)
    }
}
```

Create `app/src/test/java/com/zconte/oopsapp/data/content/ContentMapperTest.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps a content pack into a topic entity and exercise entities`() {
        val pack = ContentPack(
            topicId = "java-streams",
            name = "Streams y lambdas",
            certObjective = "streams-lambdas",
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

        val (topic, exercises) = pack.toEntities(json)

        assertEquals("java-streams", topic.id)
        assertEquals("streams-lambdas", topic.certObjective)
        assertEquals(1, exercises.size)
        assertEquals("streams-01", exercises.first().id)
        assertEquals("java-streams", exercises.first().topicId)
        assertEquals("fill_blank", exercises.first().type)
        assertEquals(2, exercises.first().difficulty)

        val decoded = json.decodeFromString(ExerciseContent.serializer(), exercises.first().payload)
        assertEquals("collect", decoded.answer)
    }
}
```

- [ ] **Step 5: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.content.*"`
Expected: FAIL to compile — `ContentPack` and `toEntities` don't exist yet.

- [ ] **Step 6: Implement ContentPack and the mapper**

Create `app/src/main/java/com/zconte/oopsapp/data/content/ContentPack.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.Serializable

@Serializable
data class ContentPack(
    val topicId: String,
    val name: String,
    val certObjective: String,
    val exercises: List<ExerciseContent>
)
```

Create `app/src/main/java/com/zconte/oopsapp/data/content/ContentMapper.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.TopicEntity
import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.json.Json

fun ContentPack.toEntities(json: Json): Pair<TopicEntity, List<ExerciseEntity>> {
    val topic = TopicEntity(
        id = topicId,
        name = name,
        certObjective = certObjective,
        orderIndex = 0
    )
    val exerciseEntities = exercises.map { content ->
        ExerciseEntity(
            id = content.id,
            topicId = topicId,
            type = content.type,
            payload = json.encodeToString(ExerciseContent.serializer(), content),
            difficulty = content.difficulty
        )
    }
    return topic to exerciseEntities
}
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.content.*"`
Expected: PASS, 3 tests green.

- [ ] **Step 8: Create the 20-exercise Streams content pack**

Create `app/src/main/assets/content/streams.json`:

```json
{
  "topicId": "java-streams",
  "name": "Streams y lambdas",
  "certObjective": "streams-lambdas",
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
      "id": "streams-15",
      "type": "mcq",
      "difficulty": 3,
      "prompt": "Que metodo aplana un Stream de listas en un unico Stream de elementos?",
      "answer": "flatMap",
      "distractors": ["map", "collect", "reduce"],
      "explanation": "flatMap() sustituye cada elemento por un stream y los aplana en uno solo."
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
    },
    {
      "id": "streams-20",
      "type": "mcq",
      "difficulty": 1,
      "prompt": "Que metodo crea un Stream a partir de una List?",
      "answer": "stream",
      "distractors": ["toStream", "asStream", "of"],
      "explanation": "List.stream() crea un Stream secuencial respaldado por la coleccion."
    }
  ]
}
```

- [ ] **Step 9: Implement the asset loader and seeder**

Create `app/src/main/java/com/zconte/oopsapp/data/content/ContentLoader.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ContentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    fun loadPack(assetPath: String): ContentPack {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return json.decodeFromString(ContentPack.serializer(), text)
    }
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`:

```kotlin
package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.TopicDao
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ContentSeeder @Inject constructor(
    private val contentLoader: ContentLoader,
    private val topicDao: TopicDao,
    private val exerciseDao: ExerciseDao,
    private val json: Json
) {
    suspend fun seedIfEmpty() {
        if (exerciseDao.count() > 0) return
        val pack = contentLoader.loadPack("content/streams.json")
        val (topic, exercises) = pack.toEntities(json)
        topicDao.insertAll(listOf(topic))
        exerciseDao.insertAll(exercises)
    }
}
```

- [ ] **Step 10: Define the repository interfaces in domain**

Create `app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt`:

```kotlin
package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import java.time.LocalDate

interface ExerciseRepository {
    suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise>
    suspend fun getNewExercises(limit: Int): List<Exercise>
    suspend fun getReviewState(exerciseId: String): ReviewState?
    suspend fun saveReviewState(state: ReviewState)
}
```

Create `app/src/main/java/com/zconte/oopsapp/domain/repository/ProgressRepository.kt`:

```kotlin
package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.UserStats

interface ProgressRepository {
    suspend fun getUserStats(): UserStats
    suspend fun saveUserStats(stats: UserStats)
    suspend fun getReadinessByObjective(): Map<String, Float>
}
```

- [ ] **Step 11: Implement the repositories against Room**

Create `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`:

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

    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        reviewStateDao.getByExerciseId(exerciseId)?.toDomain()

    override suspend fun saveReviewState(state: ReviewState) {
        reviewStateDao.upsert(state.toEntity())
    }
}

private fun ExerciseEntity.toDomain() = Exercise(
    id = id, topicId = topicId, type = type, payload = payload, difficulty = difficulty
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

Create `app/src/main/java/com/zconte/oopsapp/data/repository/ProgressRepositoryImpl.kt`:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.dao.UserStatsDao
import com.zconte.oopsapp.data.local.entity.UserStatsEntity
import com.zconte.oopsapp.domain.model.UserStats
import com.zconte.oopsapp.domain.repository.ProgressRepository
import java.time.LocalDate
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val userStatsDao: UserStatsDao,
    private val reviewStateDao: ReviewStateDao,
    private val exerciseDao: ExerciseDao
) : ProgressRepository {

    override suspend fun getUserStats(): UserStats {
        val entity = userStatsDao.get()
        return UserStats(
            streak = entity?.streak ?: 0,
            xp = entity?.xp ?: 0,
            lastStudyDate = entity?.lastStudyDate?.let { LocalDate.ofEpochDay(it) }
        )
    }

    override suspend fun saveUserStats(stats: UserStats) {
        userStatsDao.upsert(
            UserStatsEntity(
                streak = stats.streak,
                xp = stats.xp,
                lastStudyDate = stats.lastStudyDate?.toEpochDay() ?: 0L
            )
        )
    }

    override suspend fun getReadinessByObjective(): Map<String, Float> {
        val mastered = reviewStateDao.getMasteredCountByObjective()
            .associate { it.objective to it.masteredCount }
        val total = exerciseDao.getTotalCountByObjective()
            .associate { it.objective to it.totalCount }
        return total.mapValues { (objective, totalCount) ->
            if (totalCount == 0) 0f else (mastered[objective] ?: 0).toFloat() / totalCount
        }
    }
}
```

- [ ] **Step 12: Wire Hilt modules**

Create `app/src/main/java/com/zconte/oopsapp/di/SerializationModule.kt`:

```kotlin
package com.zconte.oopsapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SerializationModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }
}
```

Create `app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt`:

```kotlin
package com.zconte.oopsapp.di

import android.content.Context
import androidx.room.Room
import com.zconte.oopsapp.data.local.AppDatabase
import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.dao.TopicDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "oops.db").build()

    @Provides
    fun provideTopicDao(db: AppDatabase): TopicDao = db.topicDao()

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideReviewStateDao(db: AppDatabase): ReviewStateDao = db.reviewStateDao()

    @Provides
    fun provideUserStatsDao(db: AppDatabase): UserStatsDao = db.userStatsDao()
}
```

Create `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`:

```kotlin
package com.zconte.oopsapp.di

import com.zconte.oopsapp.data.repository.ExerciseRepositoryImpl
import com.zconte.oopsapp.data.repository.ProgressRepositoryImpl
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository
}
```

- [ ] **Step 13: Build**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests green (5 from Task 2 + 3 from this task).

- [ ] **Step 14: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data app/src/main/java/com/zconte/oopsapp/domain/repository app/src/main/java/com/zconte/oopsapp/di app/src/main/assets app/src/test/java/com/zconte/oopsapp/data
git commit -m "feat: add Room persistence, content pack loading/seeding, and repositories"
```

---

### Task 4: Use cases (domain/usecase)

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCase.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/UpdateStreakUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/UpdateStreakUseCaseTest.kt`

**Interfaces:**
- Consumes: `ExerciseRepository`, `ProgressRepository` (Task 3, `domain/repository`); `SchedulerSm2` (Task 2); `Exercise`, `ReviewState`, `UserStats` (Task 2, `domain/model`).
- Produces:
  `class GetTodaySessionUseCase(private val exerciseRepository: ExerciseRepository) { suspend operator fun invoke(today: LocalDate, newExercisesLimit: Int = 5): List<Exercise> }`
  `class SubmitAnswerUseCase(private val exerciseRepository: ExerciseRepository) { suspend operator fun invoke(exerciseId: String, quality: Int, today: LocalDate): ReviewState }`
  `class UpdateStreakUseCase(private val progressRepository: ProgressRepository) { suspend operator fun invoke(today: LocalDate): UserStats }`
  These three are constructor-injected directly into ViewModels in Tasks 5/6 (no `@Inject constructor` needed here since they're plain classes with a single dependency each — Hilt can still constructor-inject them as long as the constructor is annotated; add `@Inject` in this task so Tasks 5/6 don't need extra Hilt modules).

Tests use hand-written fakes (no mocking library) implementing the domain repository interfaces — this keeps `domain` free of any test-double framework dependency too.

- [ ] **Step 1: Write the failing tests for GetTodaySessionUseCase**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepository(
    private val due: List<Exercise> = emptyList(),
    private val new: List<Exercise> = emptyList()
) : ExerciseRepository {
    val savedStates = mutableListOf<ReviewState>()

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = due.take(limit)
    override suspend fun getNewExercises(limit: Int): List<Exercise> = new.take(limit)
    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        savedStates.find { it.exerciseId == exerciseId }
    override suspend fun saveReviewState(state: ReviewState) {
        savedStates.removeAll { it.exerciseId == state.exerciseId }
        savedStates.add(state)
    }
}

class GetTodaySessionUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)

    private fun exercise(id: String) = Exercise(id, "java-streams", "fill_blank", "{}", 1)

    @Test
    fun `session lists due exercises before new ones`() = runTest {
        val repository = FakeExerciseRepository(
            due = listOf(exercise("due-1"), exercise("due-2")),
            new = listOf(exercise("new-1"))
        )
        val useCase = GetTodaySessionUseCase(repository)

        val result = useCase(today)

        assertEquals(listOf("due-1", "due-2", "new-1"), result.map { it.id })
    }

    @Test
    fun `session limits new exercises to the requested count`() = runTest {
        val repository = FakeExerciseRepository(
            due = emptyList(),
            new = listOf(exercise("new-1"), exercise("new-2"), exercise("new-3"))
        )
        val useCase = GetTodaySessionUseCase(repository)

        val result = useCase(today, newExercisesLimit = 2)

        assertEquals(listOf("new-1", "new-2"), result.map { it.id })
    }
}
```

- [ ] **Step 2: Write the failing tests for SubmitAnswerUseCase**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepository : ExerciseRepository {
    val states = mutableMapOf<String, ReviewState>()

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = states[exerciseId]
    override suspend fun saveReviewState(state: ReviewState) {
        states[state.exerciseId] = state
    }
}

class SubmitAnswerUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `creates a default review state on first answer and applies SM-2`() = runTest {
        val repository = FakeExerciseRepository()
        val useCase = SubmitAnswerUseCase(repository)

        val result = useCase("ex-1", quality = 4, today = today)

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(today.plusDays(1), result.dueDate)
        assertEquals(result, repository.states["ex-1"])
    }

    @Test
    fun `reuses the existing review state on later answers`() = runTest {
        val repository = FakeExerciseRepository().apply {
            states["ex-1"] = ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1,
                repetitions = 1, dueDate = today
            )
        }
        val useCase = SubmitAnswerUseCase(repository)

        val result = useCase("ex-1", quality = 4, today = today)

        assertEquals(2, result.repetitions)
        assertEquals(6, result.intervalDays)
    }
}
```

- [ ] **Step 3: Write the failing tests for UpdateStreakUseCase**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/UpdateStreakUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.UserStats
import com.zconte.oopsapp.domain.repository.ProgressRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeProgressRepository(initial: UserStats) : ProgressRepository {
    var stats = initial

    override suspend fun getUserStats(): UserStats = stats
    override suspend fun saveUserStats(stats: UserStats) {
        this.stats = stats
    }
    override suspend fun getReadinessByObjective(): Map<String, Float> = emptyMap()
}

class UpdateStreakUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `first ever study session sets streak to 1`() = runTest {
        val repository = FakeProgressRepository(UserStats(streak = 0, xp = 0, lastStudyDate = null))
        val useCase = UpdateStreakUseCase(repository)

        val result = useCase(today)

        assertEquals(1, result.streak)
    }

    @Test
    fun `studying the day after the last session increments the streak`() = runTest {
        val repository = FakeProgressRepository(
            UserStats(streak = 3, xp = 30, lastStudyDate = today.minusDays(1))
        )
        val useCase = UpdateStreakUseCase(repository)

        val result = useCase(today)

        assertEquals(4, result.streak)
    }

    @Test
    fun `studying after a gap resets the streak to 1`() = runTest {
        val repository = FakeProgressRepository(
            UserStats(streak = 5, xp = 50, lastStudyDate = today.minusDays(3))
        )
        val useCase = UpdateStreakUseCase(repository)

        val result = useCase(today)

        assertEquals(1, result.streak)
    }
}
```

- [ ] **Step 4: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.*"`
Expected: FAIL to compile — the three use case classes don't exist yet.

- [ ] **Step 5: Implement the use cases**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

class GetTodaySessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(today: LocalDate, newExercisesLimit: Int = 5): List<Exercise> {
        val due = exerciseRepository.getDueExercises(today, limit = Int.MAX_VALUE)
        val new = exerciseRepository.getNewExercises(limit = newExercisesLimit)
        return due + new
    }
}
```

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.srs.SchedulerSm2
import java.time.LocalDate
import javax.inject.Inject

class SubmitAnswerUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(exerciseId: String, quality: Int, today: LocalDate): ReviewState {
        val current = exerciseRepository.getReviewState(exerciseId)
            ?: ReviewState(
                exerciseId = exerciseId, easeFactor = 2.5, intervalDays = 0,
                repetitions = 0, dueDate = today
            )
        val updated = SchedulerSm2.review(current, quality, today)
        exerciseRepository.saveReviewState(updated)
        return updated
    }
}
```

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/UpdateStreakUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.UserStats
import com.zconte.oopsapp.domain.repository.ProgressRepository
import java.time.LocalDate
import javax.inject.Inject

private const val XP_PER_SESSION = 10

class UpdateStreakUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    suspend operator fun invoke(today: LocalDate): UserStats {
        val stats = progressRepository.getUserStats()
        val newStreak = when (stats.lastStudyDate) {
            today -> stats.streak
            today.minusDays(1) -> stats.streak + 1
            else -> 1
        }
        val updated = stats.copy(
            streak = newStreak,
            xp = stats.xp + XP_PER_SESSION,
            lastStudyDate = today
        )
        progressRepository.saveUserStats(updated)
        return updated
    }
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.*"`
Expected: PASS, 7 tests green.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase app/src/test/java/com/zconte/oopsapp/domain/usecase
git commit -m "feat: implement session/answer/streak use cases with unit tests"
```

---

### Task 5: Pantalla de sesión (ui/session)

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt`

**Interfaces:**
- Consumes: `GetTodaySessionUseCase`, `SubmitAnswerUseCase`, `UpdateStreakUseCase` (Task 4); `Exercise`, `ExerciseContent` (Task 2); `Json` (provided by `SerializationModule`, Task 3).
- Produces: `SessionScreen(onSessionComplete: () -> Unit, modifier: Modifier)` — same signature the Task 1 `OopsNavHost` already calls, so no navigation changes needed.

No new domain logic here (the ViewModel only orchestrates already-tested use cases), so this task is verified manually per Step 4 below rather than with JUnit — consistent with spec section 10.

- [ ] **Step 1: Implement the SessionViewModel**

Create `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
import com.zconte.oopsapp.domain.usecase.UpdateStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isSessionComplete: Boolean = false
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val getTodaySessionUseCase: GetTodaySessionUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val queue = getTodaySessionUseCase(LocalDate.now())
            _uiState.update {
                it.copy(queue = queue, currentExercise = queue.firstOrNull()?.let(::decode))
            }
        }
    }

    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        val exercise = current.currentExercise ?: return
        val exerciseId = current.queue.first().id
        val correct = userAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        viewModelScope.launch {
            submitAnswerUseCase(exerciseId, quality = if (correct) 5 else 2, today = LocalDate.now())
        }
    }

    fun nextExercise() {
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isEmpty()) {
            viewModelScope.launch { updateStreakUseCase(LocalDate.now()) }
            _uiState.update { it.copy(isSessionComplete = true) }
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

- [ ] **Step 2: Implement the SessionScreen UI**

Replace the contents of `app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
        Text("Cargando sesion...", modifier = modifier.padding(16.dp))
        return
    }

    var answer by remember(exercise.id) { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp)) {
        Text(exercise.prompt)
        exercise.code?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
        Spacer(Modifier.height(16.dp))

        if (!uiState.isAnswered) {
            OutlinedTextField(value = answer, onValueChange = { answer = it })
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.submitAnswer(answer) }) { Text("Responder") }
        } else {
            Text(if (uiState.isCorrect) "Correcto!" else "Incorrecto. Respuesta: ${exercise.answer}")
            Spacer(Modifier.height(4.dp))
            Text(exercise.explanation)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.nextExercise() }) { Text("Siguiente") }
        }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual verification**

Install and run the app. From Home, tap "Estudiar hoy". Confirm: the first Streams exercise appears with its prompt/code; typing an answer and tapping "Responder" shows correct/incorrect feedback plus the explanation; "Siguiente" advances through the queue; after the last exercise, the screen navigates back to Home (via `onSessionComplete` → `popBackStack()`).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/session
git commit -m "feat: implement session screen with answer evaluation and SM-2 feedback loop"
```

---

### Task 6: Home con racha y XP (ui/home)

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`

**Interfaces:**
- Consumes: `ProgressRepository` (Task 3), `ContentSeeder` (Task 3).
- Produces: `HomeScreen(onStudyClick: () -> Unit, onProgressClick: () -> Unit, modifier: Modifier)` — same signature `OopsNavHost` already calls.

- [ ] **Step 1: Implement the HomeViewModel**

Create `app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.data.content.ContentSeeder
import com.zconte.oopsapp.domain.repository.ProgressRepository
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
    val isReady: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val contentSeeder: ContentSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contentSeeder.seedIfEmpty()
            refreshStats()
            _uiState.update { it.copy(isReady = true) }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshStats() }
    }

    private suspend fun refreshStats() {
        val stats = progressRepository.getUserStats()
        _uiState.update { it.copy(streak = stats.streak, xp = stats.xp) }
    }
}
```

- [ ] **Step 2: Implement the HomeScreen UI**

Replace the contents of `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    onStudyClick: () -> Unit,
    onProgressClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
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

    Column(modifier = modifier.padding(16.dp)) {
        Text("Racha: ${uiState.streak} dias")
        Text("XP: ${uiState.xp}")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStudyClick, enabled = uiState.isReady) { Text("Estudiar hoy") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onProgressClick) { Text("Ver progreso") }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual verification**

Fresh install (uninstall the app first to clear the local database, or clear app data). Launch: confirm "Estudiar hoy" starts disabled for a brief moment while the DB seeds, then enables. Complete a session (Task 5) and return to Home: confirm streak shows 1 and XP shows 10. Force-stop and relaunch the app the same day, complete another session: confirm streak stays at 1 (same-day) while XP increases by 10 again.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/home
git commit -m "feat: implement home screen with streak/XP and first-run content seeding"
```

---

### Task 7: Progreso (ui/progress)

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressViewModel.kt`

**Interfaces:**
- Consumes: `ProgressRepository.getReadinessByObjective()` (Task 3).
- Produces: `ProgressScreen(modifier: Modifier)` — same signature `OopsNavHost` already calls.

- [ ] **Step 1: Implement the ProgressViewModel**

Create `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val readinessByObjective: Map<String, Float> = emptyMap()
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val readiness = progressRepository.getReadinessByObjective()
            _uiState.update { it.copy(readinessByObjective = readiness) }
        }
    }
}
```

- [ ] **Step 2: Implement the ProgressScreen UI**

Replace the contents of `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(uiState.readinessByObjective.entries.toList()) { (objective, readiness) ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("$objective — ${(readiness * 100).toInt()}%")
                LinearProgressIndicator(progress = { readiness })
            }
        }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual verification**

From Home, tap "Ver progreso" before answering any exercises: confirm it shows "streams-lambdas — 0%". Complete a full session answering at least 2 exercises correctly twice each (so `repetitions >= 2`), return to Progress: confirm the percentage increased proportionally to `masteredCount / totalCount` (20 total Streams exercises in the seed pack).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/progress
git commit -m "feat: implement progress screen grouped by certification objective"
```

Task 7 completes Fase 1 per the spec: a daily-usable app with Streams as the first 1Z0-830 domain.

---

## Self-Review Notes

- **Spec coverage:** every section of `PROJECT-OOPS.md` maps to a task — §3/§4 → Task 1, §5 → Task 3, §6 → Task 2, §7 → Task 3 (with the 20 seed exercises from §9's Task 3 requirement), §8 → Task 7, §9 Tasks 1–7 → Plan Tasks 1–7, §10 conventions → enforced throughout (pure `domain`, one ViewModel per screen, `StateFlow` state, tests on domain logic).
- **Repository/DI gap in the original spec:** the spec's task list never explicitly assigns `data/repository` or Hilt wiring to a task, even though §4's architecture diagram includes them. This plan folds them into Task 3 (see the Architecture note at the top) since Task 4's use cases can't compile without the repository interfaces existing first.
- **Type consistency:** `Exercise`, `ExerciseContent`, `ReviewState`, `UserStats` are defined once in Task 2 and reused verbatim (same field names/types) through Tasks 3–7. Repository method signatures introduced in Task 3 match exactly what Task 4's use cases and their fakes call. `*UseCase` naming and `operator invoke` convention is consistent across Task 4 and its ViewModel consumers in Tasks 5–7.