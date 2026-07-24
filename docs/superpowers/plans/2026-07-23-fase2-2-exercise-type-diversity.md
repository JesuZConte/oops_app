# Fase 2.2 — Diversidad de tipos de ejercicio — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two new exercise types — `parsons` (ordenar líneas de código, tocando en orden) and `predict_output` (predecir salida de un snippet) — to the existing exercise engine, plus a handful of real exercises in the two curated content packs to validate them end-to-end.

**Architecture:** `ExerciseContent` gains one optional field (`lines`) — no Room migration, since `Exercise.payload` is an opaque JSON blob. Grading logic, today duplicated identically across three ViewModels, is extracted into one pure, type-aware function. `ExerciseAnswerCard.kt` grows a third UI branch (tap-to-order builder for parsons) and a multiline variant of the existing free-text input (predict_output), plus a type-aware `FeedbackBanner`.

**Tech Stack:** Kotlin, Jetpack Compose, kotlinx.serialization, JUnit4 (no ViewModel/Compose UI tests in this project — see Global Constraints).

**Design doc:** `docs/superpowers/specs/2026-07-23-fase2-2-exercise-type-diversity-design.md`

## Global Constraints

- Grading is strictly binary (correct/incorrect) — no partial credit. Matches
  the existing SM-2 quality signal (correct → 5, incorrect → 2), unchanged.
- Parsons grading: exact match, case-sensitive (`userAnswer.trim() ==
  exercise.answer.trim()`) — it's code, `list` and `List` are not
  interchangeable.
- predict_output grading: case-sensitive, but tolerant of trailing
  per-line whitespace and of blank lines at the very start/end.
  `mcq`/`fill_blank` grading is unchanged (`ignoreCase = true`, trimmed).
- Parsons UI is tap-to-order (tap an available line to append it, tap a
  placed line to return it) — **no drag-and-drop**, decided explicitly to
  avoid gesture/reorder-animation complexity in Compose.
- No Room schema change and no migration — `ExerciseContent` is serialized
  into `Exercise.payload` (opaque `String` column); adding a field to it is
  transparent to the database.
- Content prompts/explanations follow the existing style already in
  `java-fundamentals.json`/`streams.json`: no accent marks, no inverted `¿`
  (e.g. `"Que imprime este codigo?"`, not `"¿Qué imprime este código?"`).
- **Parsons content: at most ~6 lines per exercise.** The answer-input area
  (Parsons chip list, free-text field) sits below the scrollable
  prompt/code region, not inside it — same layout family as the Fase 2.1
  bug where the keyboard pushed COMPROBAR off-screen. Unbounded content
  there risks the same failure. All exercises authored in Task 5 stay at
  5 lines or fewer. The `predict_output` input field is capped with
  `maxLines = 6` for the same reason (Task 4) — typed output beyond that
  scrolls within the field instead of growing the layout. (Identified in
  senior-dev plan review, 2026-07-23; a full fix would move the
  answer-input area inside the scrollable region — deferred as unnecessary
  while content stays within these bounds.)
- `ContentSeeder.CURRENT_CONTENT_VERSION` **must** be bumped from `"2"` to
  `"3"` for the new content to actually reseed on devices that already have
  the app installed — the seeder is a no-op if the stored version already
  matches (`app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`).
  Reseeding wipes and reloads only `sections`/`units`/`exercises` — it does
  not touch `review_state`/`unit_progress`/`checkpoint_attempts`, so existing
  progress is preserved as long as no existing exercise/unit `id` is renamed
  or removed (this plan only adds new ids).

---

### Task 1: Domain model — `ExerciseType` constants + `ExerciseContent.lines`

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseType.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseContent.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt`

**Interfaces:**
- Produces: `ExerciseType` object with `MCQ`, `FILL_BLANK`, `PARSONS`,
  `PREDICT_OUTPUT` constants (all `String`) — consumed by Tasks 2 and 3.
- Produces: `ExerciseContent.lines: List<String>` (default `emptyList()`) —
  consumed by Task 3 (Parsons UI) and Task 5 (content).

- [ ] **Step 1: Write the failing tests**

Add two test methods to the existing
`app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt`
(inside the `ContentPackParsingTest` class, after the existing two `@Test`
methods):

```kotlin
    @Test
    fun `parsons exercise parses lines field`() {
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
                      "id": "streams-parsons-01",
                      "type": "parsons",
                      "difficulty": 2,
                      "prompt": "Ordena las lineas:",
                      "lines": ["numeros.stream()", ".filter(n -> n % 2 == 0)", ".count()"],
                      "answer": "numeros.stream()\n.filter(n -> n % 2 == 0)\n.count()",
                      "code": "numeros.stream()\n.filter(n -> n % 2 == 0)\n.count()",
                      "explanation": "Cuenta los pares del stream."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals(
            listOf("numeros.stream()", ".filter(n -> n % 2 == 0)", ".count()"),
            pack.units.first().exercises.first().lines
        )
    }

    @Test
    fun `exercise without lines field defaults to empty list`() {
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

        assertEquals(emptyList<String>(), pack.units.first().exercises.first().lines)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.data.content.ContentPackParsingTest"`
Expected: FAIL — compile error, `lines` is unresolved on `ExerciseContent`.

- [ ] **Step 3: Add `ExerciseType` and the `lines` field**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseType.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

object ExerciseType {
    const val MCQ = "mcq"
    const val FILL_BLANK = "fill_blank"
    const val PARSONS = "parsons"
    const val PREDICT_OUTPUT = "predict_output"
}
```

Modify `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseContent.kt`
to the full new content:

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
    val lines: List<String> = emptyList(),
    val explanation: String
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.data.content.ContentPackParsingTest"`
Expected: PASS (4 tests: the 2 pre-existing + the 2 new ones).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseType.kt \
        app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseContent.kt \
        app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt
git commit -m "feat: add ExerciseType constants and ExerciseContent.lines field"
```

---

### Task 2: Shared grading — `gradeExerciseAnswer` + wire into the 3 ViewModels

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GradeExerciseAnswerUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GradeExerciseAnswerUseCaseTest.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt:72`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt:79`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModel.kt:100`

**Interfaces:**
- Consumes: `ExerciseType` (Task 1), `ExerciseContent` (existing, now with
  `lines`).
- Produces: top-level function `fun gradeExerciseAnswer(exercise:
  ExerciseContent, userAnswer: String): Boolean` — consumed by the 3
  ViewModels in this task, and available to any future caller.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GradeExerciseAnswerUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.model.ExerciseType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeExerciseAnswerUseCaseTest {

    private fun mcqExercise(answer: String) = ExerciseContent(
        id = "ex-mcq", type = ExerciseType.MCQ, difficulty = 1, prompt = "p",
        answer = answer, explanation = "e"
    )

    private fun fillBlankExercise(answer: String) = ExerciseContent(
        id = "ex-fill", type = ExerciseType.FILL_BLANK, difficulty = 1, prompt = "p",
        answer = answer, explanation = "e"
    )

    private fun parsonsExercise(answer: String) = ExerciseContent(
        id = "ex-parsons", type = ExerciseType.PARSONS, difficulty = 1, prompt = "p",
        answer = answer, explanation = "e"
    )

    private fun predictOutputExercise(answer: String) = ExerciseContent(
        id = "ex-predict", type = ExerciseType.PREDICT_OUTPUT, difficulty = 1, prompt = "p",
        answer = answer, explanation = "e"
    )

    @Test
    fun `mcq is case-insensitive and trims, unchanged from prior behavior`() {
        assertTrue(gradeExerciseAnswer(mcqExercise("Java Virtual Machine"), "  java virtual machine  "))
        assertFalse(gradeExerciseAnswer(mcqExercise("Java Virtual Machine"), "Java Verified Method"))
    }

    @Test
    fun `fill_blank is case-insensitive and trims, unchanged from prior behavior`() {
        assertTrue(gradeExerciseAnswer(fillBlankExercise("javac"), " Javac "))
        assertFalse(gradeExerciseAnswer(fillBlankExercise("javac"), "java"))
    }

    @Test
    fun `parsons requires exact order and is case-sensitive`() {
        val answer = "list.stream()\n.filter(x -> x > 0)\n.count()"
        assertTrue(gradeExerciseAnswer(parsonsExercise(answer), answer))
        assertFalse(gradeExerciseAnswer(parsonsExercise(answer), "list.stream()\n.count()\n.filter(x -> x > 0)"))
        assertFalse(gradeExerciseAnswer(parsonsExercise(answer), answer.replace("list", "List")))
    }

    @Test
    fun `predict_output is case-sensitive but tolerates trailing whitespace and blank edges`() {
        val exercise = predictOutputExercise("a\nb")
        assertTrue(gradeExerciseAnswer(exercise, "a\nb"))
        assertTrue(gradeExerciseAnswer(exercise, "a \nb  \n"))
        assertTrue(gradeExerciseAnswer(exercise, "\na\nb\n"))
        assertFalse(gradeExerciseAnswer(exercise, "A\nb"))
        assertFalse(gradeExerciseAnswer(exercise, "a\nb\nc"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GradeExerciseAnswerUseCaseTest"`
Expected: FAIL — compile error, `gradeExerciseAnswer` is unresolved.

- [ ] **Step 3: Implement `gradeExerciseAnswer`**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GradeExerciseAnswerUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.model.ExerciseType

fun gradeExerciseAnswer(exercise: ExerciseContent, userAnswer: String): Boolean =
    when (exercise.type) {
        ExerciseType.PARSONS -> gradeParsons(exercise, userAnswer)
        ExerciseType.PREDICT_OUTPUT -> gradePredictOutput(exercise, userAnswer)
        else -> userAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)
    }

private fun gradeParsons(exercise: ExerciseContent, userAnswer: String): Boolean =
    userAnswer.trim() == exercise.answer.trim()

private fun gradePredictOutput(exercise: ExerciseContent, userAnswer: String): Boolean =
    normalizeOutput(userAnswer) == normalizeOutput(exercise.answer)

private fun normalizeOutput(text: String): String =
    text.trim().lines().joinToString("\n") { it.trimEnd() }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GradeExerciseAnswerUseCaseTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Wire into the 3 ViewModels**

In `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt`,
add the import `import com.zconte.oopsapp.domain.usecase.gradeExerciseAnswer`
(alongside the existing `domain.usecase` imports) and replace line 72:

```kotlin
        val correct = userAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)
```

with:

```kotlin
        val correct = gradeExerciseAnswer(exercise, userAnswer)
```

Apply the identical import + line replacement to
`app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt:79`
and
`app/src/main/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModel.kt:100`.

- [ ] **Step 6: Verify the project still compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GradeExerciseAnswerUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GradeExerciseAnswerUseCaseTest.kt \
        app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModel.kt
git commit -m "feat: extract shared type-aware exercise grading, wire into all 3 ViewModels"
```

---

### Task 3: Parsons UI — tap-to-order builder in `ExerciseAnswerCard`

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`

**Interfaces:**
- Consumes: `ExerciseType` (Task 1), `ExerciseContent.lines` (Task 1),
  `CodeBlock` (existing, unchanged signature).
- Produces: `FeedbackBanner(isCorrect: Boolean, exerciseType: String, answer:
  String, explanation: String)` — signature change consumed by Task 4 (adds
  the `PREDICT_OUTPUT` branch to the same `when`).

This task has no automated test — this project has no ViewModel/Compose UI
tests (see Global Constraints in the design spec); UI correctness is
verified by manual on-device QA once content exists to play (Task 5). Steps
below are implementation steps with a compile check as the pass/fail gate.

- [ ] **Step 1: Replace the full file content**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`
with:

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.model.ExerciseType
import com.zconte.oopsapp.ui.theme.JetBrainsMono
import com.zconte.oopsapp.ui.theme.OopsTheme

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
        if (exercise.type == ExerciseType.MCQ) (exercise.distractors + exercise.answer).shuffled() else emptyList()
    }
    var parsonsAvailable by remember(exercise.id) {
        mutableStateOf(if (exercise.type == ExerciseType.PARSONS) exercise.lines.shuffled() else emptyList())
    }
    var parsonsBuilt by remember(exercise.id) { mutableStateOf(emptyList<String>()) }
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

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = exercise.prompt,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            exercise.code?.let { code ->
                val hideBeforeAnswered = exercise.type == ExerciseType.PARSONS && !state.isAnswered
                if (!hideBeforeAnswered) {
                    val filledAnswer = if (state.isAnswered && exercise.type == ExerciseType.FILL_BLANK) {
                        exercise.answer
                    } else {
                        null
                    }
                    CodeBlock(code = code, filledAnswer = filledAnswer, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (!state.isAnswered) {
            when (exercise.type) {
                ExerciseType.MCQ -> {
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
                    ComprobarButton(enabled = selectedOption != null) {
                        selectedOption?.let { onSubmit(it) }
                    }
                }
                ExerciseType.PARSONS -> {
                    ParsonsBuilder(
                        available = parsonsAvailable,
                        built = parsonsBuilt,
                        onTapAvailable = { line ->
                            parsonsBuilt = parsonsBuilt + line
                            parsonsAvailable = parsonsAvailable - line
                        },
                        onTapBuilt = { line ->
                            parsonsAvailable = parsonsAvailable + line
                            parsonsBuilt = parsonsBuilt - line
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    ComprobarButton(enabled = parsonsBuilt.size == exercise.lines.size) {
                        onSubmit(parsonsBuilt.joinToString("\n"))
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    ComprobarButton { onSubmit(answer) }
                }
            }
        } else {
            if (exercise.type == ExerciseType.MCQ) {
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
                exerciseType = exercise.type,
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

@Composable
private fun ComprobarButton(enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text("COMPROBAR", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ParsonsBuilder(
    available: List<String>,
    built: List<String>,
    onTapAvailable: (String) -> Unit,
    onTapBuilt: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Tu secuencia:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (built.isEmpty()) {
                Text(
                    text = "Toca las lineas de abajo en el orden correcto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                built.forEach { line ->
                    ParsonsLineChip(text = line, selected = true, onClick = { onTapBuilt(line) })
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            available.forEach { line ->
                ParsonsLineChip(text = line, selected = false, onClick = { onTapAvailable(line) })
            }
        }
    }
}

@Composable
private fun ParsonsLineChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(1.5.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 14.dp)
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            color = extended.codeText
        )
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
private fun FeedbackBanner(isCorrect: Boolean, exerciseType: String, answer: String, explanation: String) {
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
        exerciseType == ExerciseType.PARSONS -> "Incorrecto"
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

This step: (a) removes the hardcoded `private const val MCQ_TYPE = "mcq"` in
favor of `ExerciseType.MCQ`; (b) hides the generic code-block reveal for
`parsons` before answering, and tightens `filledAnswer` to only apply to
`FILL_BLANK`; (c) adds the `ParsonsBuilder`/`ParsonsLineChip` composables and
wires them into a new `when` branch; (d) changes `FeedbackBanner` to take
`exerciseType` and suppress the inline "Respuesta: ..." for `parsons` (its
answer is already revealed via the `CodeBlock` above); (e) extracts a shared
`ComprobarButton` composable — the identical `Button(...) { Text("COMPROBAR",
...) }` block would otherwise appear three times (MCQ, Parsons, free-text),
a DRY regression this task would introduce (flagged in senior-dev plan
review, 2026-07-23). The `mcq`/`fill_blank` paths are otherwise
byte-for-byte unchanged in behavior.

- [ ] **Step 2: Verify the project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt
git commit -m "feat: add Parsons tap-to-order exercise UI"
```

---

### Task 4: predict_output UI — multiline input + expected-output reveal

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`

**Interfaces:**
- Consumes: `ExerciseType.PREDICT_OUTPUT` (Task 1), the `FeedbackBanner`
  signature from Task 3.

- [ ] **Step 1: Multiline input for `predict_output`**

In `ExerciseAnswerCard.kt`, inside the not-answered `when (exercise.type)`
block from Task 3, replace the `else ->` branch's `OutlinedTextField` call:

```kotlin
                else -> {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.labelMedium
                    )
```

with:

```kotlin
                else -> {
                    val isPredictOutput = exercise.type == ExerciseType.PREDICT_OUTPUT
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = !isPredictOutput,
                        minLines = if (isPredictOutput) 3 else 1,
                        maxLines = if (isPredictOutput) 6 else 1,
                        textStyle = if (isPredictOutput) {
                            MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono)
                        } else {
                            MaterialTheme.typography.labelMedium
                        }
                    )
```

(the rest of that `else` branch — the `Spacer` and COMPROBAR `Button` — is
unchanged).

- [ ] **Step 2: Expected-output reveal in `FeedbackBanner`**

In the same file, replace the `title` computation inside `FeedbackBanner`:

```kotlin
    val title = when {
        isCorrect && !extended.isDark -> "¡Correcto! +10 XP 🎉"
        isCorrect -> "¡Correcto! +10 XP"
        exerciseType == ExerciseType.PARSONS -> "Incorrecto"
        else -> "Incorrecto. Respuesta: $answer"
    }
```

with:

```kotlin
    val title = when {
        isCorrect && !extended.isDark -> "¡Correcto! +10 XP 🎉"
        isCorrect -> "¡Correcto! +10 XP"
        exerciseType == ExerciseType.PARSONS || exerciseType == ExerciseType.PREDICT_OUTPUT -> "Incorrecto"
        else -> "Incorrecto. Respuesta: $answer"
    }
```

Then, in the same composable's `Column { ... }` body, insert the expected-output
block between the title `Text` and the explanation `Text`:

```kotlin
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        if (!isCorrect && exerciseType == ExerciseType.PREDICT_OUTPUT) {
            Text(
                text = "Salida esperada:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = answer,
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
```

- [ ] **Step 3: Verify the project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt
git commit -m "feat: add predict_output multiline input and expected-output reveal"
```

---

### Task 5: Content — real Parsons/predict_output exercises + content version bump

**Files:**
- Modify: `app/src/main/assets/content/streams.json`
- Modify: `app/src/main/assets/content/java-fundamentals.json`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`

**Interfaces:**
- Consumes: the `parsons`/`predict_output` engine support from Tasks 1-4.
- Produces: nothing consumed by later tasks — this is the last task.

- [ ] **Step 1: Add 2 `parsons` + 2 `predict_output` exercises to `streams.json`**

In `app/src/main/assets/content/streams.json`, inside the `streams-terminal`
unit's `exercises` array, add a new element after `streams-18` (before the
array's closing `]`) — remember to add a trailing comma after `streams-18`'s
closing `}`:

```json
        {
          "id": "streams-parsons-01",
          "type": "parsons",
          "difficulty": 2,
          "prompt": "Ordena las lineas para filtrar los pares y sumarlos:",
          "lines": ["numeros.stream()", ".filter(n -> n % 2 == 0)", ".mapToInt(Integer::intValue)", ".sum()"],
          "answer": "numeros.stream()\n.filter(n -> n % 2 == 0)\n.mapToInt(Integer::intValue)\n.sum()",
          "code": "numeros.stream()\n.filter(n -> n % 2 == 0)\n.mapToInt(Integer::intValue)\n.sum()",
          "explanation": "stream() abre el pipeline, filter() selecciona los pares, mapToInt() desempaqueta a int y sum() reduce a un total."
        }
```

Inside the `streams-intermediate` unit's `exercises` array, add after
`streams-15` (trailing comma after `streams-15`'s closing `}`):

```json
        {
          "id": "streams-predict-01",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "Stream.of(\"a\", \"b\", \"c\")\n    .limit(2)\n    .forEach(System.out::println);",
          "answer": "a\nb",
          "explanation": "limit(2) trunca el stream a los primeros 2 elementos antes de forEach()."
        }
```

Inside the `streams-collectors` unit's `exercises` array, add after
`streams-19` (trailing comma after `streams-19`'s closing `}`):

```json
        {
          "id": "streams-parsons-02",
          "type": "parsons",
          "difficulty": 3,
          "prompt": "Ordena las lineas para pasar las palabras a mayusculas y juntarlas en una lista:",
          "lines": ["palabras.stream()", ".map(String::toUpperCase)", ".collect(Collectors.toList())"],
          "answer": "palabras.stream()\n.map(String::toUpperCase)\n.collect(Collectors.toList())",
          "code": "palabras.stream()\n.map(String::toUpperCase)\n.collect(Collectors.toList())",
          "explanation": "map() transforma cada elemento y collect(Collectors.toList()) los acumula en una lista nueva."
        }
```

Inside the `streams-creation` unit's `exercises` array, add after
`streams-16` (trailing comma after `streams-16`'s closing `}`):

```json
        {
          "id": "streams-predict-02",
          "type": "predict_output",
          "difficulty": 1,
          "prompt": "Que imprime este codigo?",
          "code": "IntStream.range(1, 4)\n    .forEach(System.out::println);",
          "answer": "1\n2\n3",
          "explanation": "range(1, 4) genera 1, 2 y 3 (el limite superior es exclusivo); forEach imprime cada uno en su propia linea."
        }
```

- [ ] **Step 2: Add 2 `parsons` + 2 `predict_output` exercises to `java-fundamentals.json`**

Inside the `fund-what-is-java` unit's `exercises` array, add after
`fund-whatis-06` (trailing comma after `fund-whatis-06`'s closing `}`):

```json
        {
          "id": "fund-predict-01",
          "type": "predict_output",
          "difficulty": 1,
          "prompt": "Que imprime este codigo?",
          "code": "int a = 5;\nint b = 3;\nSystem.out.println(a + b);",
          "answer": "8",
          "explanation": "a + b suma los valores de las variables: 5 + 3 = 8."
        }
```

Inside the `fund-class-structure` unit's `exercises` array, add after
`fund-class-06` (trailing comma after `fund-class-06`'s closing `}`):

```json
        {
          "id": "fund-parsons-01",
          "type": "parsons",
          "difficulty": 1,
          "prompt": "Ordena las lineas para formar el getter del field nombre:",
          "lines": ["public String getNombre() {", "    return nombre;", "}"],
          "answer": "public String getNombre() {\n    return nombre;\n}",
          "code": "public String getNombre() {\n    return nombre;\n}",
          "explanation": "Un getter declara el tipo de retorno y su nombre, contiene un return con el valor del field, y cierra con la llave correspondiente."
        }
```

Inside the `fund-types-and-main` unit's `exercises` array, add after
`fund-main-05` (trailing comma after `fund-main-05`'s closing `}`):

```json
        {
          "id": "fund-parsons-02",
          "type": "parsons",
          "difficulty": 1,
          "prompt": "Ordena las lineas para formar una clase Main que imprime Hola:",
          "lines": ["public class Main {", "    public static void main(String[] args) {", "        System.out.println(\"Hola\");", "    }", "}"],
          "answer": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hola\");\n    }\n}",
          "code": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hola\");\n    }\n}",
          "explanation": "La clase contiene el metodo main, que contiene la instruccion a ejecutar; las llaves de apertura y cierre definen el anidamiento."
        },
        {
          "id": "fund-predict-02",
          "type": "predict_output",
          "difficulty": 1,
          "prompt": "Que imprime este codigo?",
          "code": "int edad = 30;\nedad = edad + 1;\nSystem.out.println(edad);",
          "answer": "31",
          "explanation": "Se reasigna edad sumandole 1 antes de imprimir, resultando en 31."
        }
```

- [ ] **Step 3: Validate both files are syntactically valid JSON**

Run: `python3 -m json.tool app/src/main/assets/content/streams.json > /dev/null && python3 -m json.tool app/src/main/assets/content/java-fundamentals.json > /dev/null && echo VALID`
Expected: `VALID` printed, no errors.

- [ ] **Step 4: Bump the content version so the seeder reloads it**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
change line 12:

```kotlin
private const val CURRENT_CONTENT_VERSION = "2"
```

to:

```kotlin
private const val CURRENT_CONTENT_VERSION = "3"
```

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (including the `ContentPackParsingTest`
and `GradeExerciseAnswerUseCaseTest` additions from Tasks 1-2 — this run is
the regression check that nothing in Tasks 1-4 broke on real content).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/assets/content/streams.json \
        app/src/main/assets/content/java-fundamentals.json \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: add parsons/predict_output exercises to Fundamentos and Streams"
```

---

## After all tasks: manual on-device QA

No automated UI tests exist in this project (see Global Constraints). Once
all 5 tasks are merged, install a clean build and manually verify on-device
(same pattern as Fases 2.1/2.1b):

1. Play a Parsons exercise to completion: tap lines out of order, confirm a
   tapped "built" line returns to the available pool, confirm COMPROBAR
   stays disabled until all lines are placed, submit correct and confirm the
   full correct code reveals with a plain "Incorrecto"-free success banner,
   then repeat with a wrong order and confirm the code still reveals
   correctly with a simple "Incorrecto" title (no garbled multiline answer
   text).
2. Play a predict_output exercise: confirm the input field is multiline and
   monospaced, submit a wrong-case answer and confirm it's marked incorrect
   with the "Salida esperada:" block shown, submit the exact correct
   multiline output and confirm it's marked correct.
3. Confirm keyboard/scroll behavior holds for the predict_output multiline
   field (this project fixed a keyboard-covers-input bug in Fase 2.1 QA —
   verify the fix still holds with a taller multiline field).

## Known technical debt (identified in senior-dev plan review, 2026-07-23, not addressed in this plan)

- **SRP:** `ExerciseAnswerCard` dispatches all 4 exercise types' answer-input
  UI inline in one `when` block, inside an already-multi-purpose composable.
  Extracting `McqAnswerInput`/`ParsonsAnswerInput`/`TextAnswerInput` as
  separate composables would improve readability and let each type's UI be
  reasoned about in isolation. Deferred — the current dispatch-by-type
  pattern is idiomatic enough for 4 types, not broken.
- **Content DRY:** for `parsons` exercises, `lines`, `answer`, and `code`
  must be kept in sync by hand in the JSON (the correct order lives in
  `lines`; `answer`/`code` are supposed to be the same value joined by
  `"\n"`). If a content author edits `lines` without updating `answer`/
  `code`, the exercise silently breaks (wrong grading or wrong reveal). A
  fix would make the engine derive grading and the reveal solely from
  `lines` for `parsons`, so `answer`/`code` become inert if they drift.
  Deferred for this plan (5 authored exercises can be kept in sync by
  hand); worth revisiting before Fase 2.3 authors Parsons content at scale.
- **Architecture:** `ExerciseContent` models all 4 exercise types as one
  flat data class with fields that are only meaningful for a subset of
  types (documented by convention, not enforced by the compiler). A sealed
  hierarchy per type would give compile-time exhaustiveness, but the blast
  radius (polymorphic serialization, `ContentMapper`, every decode site)
  is large relative to the benefit at 4 types, and it would be inconsistent
  with the pattern the codebase already used for `mcq`/`fill_blank` before
  this plan. Correctly out of scope — revisit only if the type count keeps
  growing.
