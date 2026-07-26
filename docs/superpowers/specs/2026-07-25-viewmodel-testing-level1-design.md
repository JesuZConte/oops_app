# Tests de ViewModel (Nivel 1) — Diseño

**Estado:** Aprobado, pendiente de plan de implementación.

## Contexto

La ADR `docs/adrs/2026-07-24-viewmodel-and-smoke-testing-strategy.md` ya
decidió una estrategia de testing en dos niveles, motivada por que el QA
manual en dispositivo se vuelve cada vez más largo a medida que crece el
roadmap de contenido. Nivel 1 (tests de ViewModel) queda ahí identificado
como el primer paso, barato, sin infraestructura nueva — pero el diseño de
implementación (qué casos exactos, cómo se arman los fakes) quedó
explícitamente diferido.

El fix de Ruta/SM-2 (`docs/superpowers/specs/2026-07-25-ruta-sm2-progress-sync-design.md`,
mergeado el 2026-07-25) tocó `SessionViewModel` y `CheckpointViewModel` sin
ninguna red automática — solo QA manual. Eso hace más urgente cerrar este
hueco: los tres ViewModels de sesión/checkpoint (`SessionViewModel`,
`CheckpointViewModel`, `PlacementCheckpointViewModel`) siguen en cero
cobertura hoy.

## Decisión

Cubrir los 3 ViewModels en este mismo ciclo (no solo los dos tocados por el
fix reciente) — `PlacementCheckpointViewModel` es el de lógica más
delicada (buffer defer-on-fail de la Fase 2.1b) y es justamente el que
menos conviene dejar sin red.

### Infraestructura de test (nueva, mínima)

Los tres ViewModels usan `viewModelScope`, que por defecto corre en
`Dispatchers.Main.immediate`. En tests JVM puros ese dispatcher no existe
salvo que se instale explícitamente vía `kotlinx-coroutines-test` (ya es
dependencia `testImplementation` del proyecto — no se agrega nada nuevo al
build). Se agrega una regla JUnit4 reutilizable, patrón estándar de
testing de ViewModels con coroutines:

```kotlin
// app/src/test/java/com/zconte/oopsapp/testutil/MainDispatcherRule.kt
package com.zconte.oopsapp.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### Fakes: casos de uso reales + fakes de repositorio (no mocks de use case)

Los ViewModels dependen de casos de uso concretos (no interfaces) —
`SubmitAnswerUseCase`, `MarkUnitProgressUseCase`, `UpdateStreakUseCase`,
`CompleteCheckpointUseCase`, `GetTodaySessionUseCase`,
`GetUnitSessionUseCase`, `GetCheckpointSessionUseCase`,
`GetSkippedUnitsUseCase`, `GetPlacementCheckpointSessionUseCase` — que a
su vez dependen únicamente de 4 repositorios: `ExerciseRepository`,
`ContentRepository`, `ProgressRepository`, `CheckpointRepository`.

En vez de introducir dobles de test para cada caso de uso (requeriría
convertirlos de clases concretas a interfaces solo para poder testear,
una re-arquitectura no justificada por esto), **se construyen los casos
de uso reales, respaldados por fakes escritos a mano de esos 4
repositorios** — mismo patrón "sin librería de mocking" ya usado en todo
`domain/usecase`, aplicado un nivel más arriba. Esto verifica
comportamiento real de principio a fin (¿se agenda bien SM-2?, ¿se marca
bien el progreso de la unidad?), no una simulación de la lógica.

Los 4 fakes se comparten entre los 3 archivos de test (primera vez que el
mismo fake es consumido por 3+ archivos — antes cada test de use case
definía sus fakes como clases privadas por archivo, incluso solapados,
porque el solape era de 1-2 métodos; acá son interfaces completas de
varios métodos). Se agregan en un paquete nuevo, solo-test:

- `app/src/test/java/com/zconte/oopsapp/testutil/FakeExerciseRepository.kt`
- `app/src/test/java/com/zconte/oopsapp/testutil/FakeContentRepository.kt`
- `app/src/test/java/com/zconte/oopsapp/testutil/FakeProgressRepository.kt`
- `app/src/test/java/com/zconte/oopsapp/testutil/FakeCheckpointRepository.kt`

Cada fake es mutable/inspeccionable (listas/mapas que el test puede leer
después de ejercitar el ViewModel) — mismo estilo que los fakes ya
existentes en `domain/usecase`, solo que reutilizables.

## Cobertura por ViewModel

### `SessionViewModelTest`

- `init` sin `unitId` (SavedStateHandle) → llama `getTodaySessionUseCase`.
- `init` con `unitId` → llama `getUnitSessionUseCase(unitId)` en su lugar.
- `init` con cola vacía → `isSessionComplete = true` de entrada, sin
  ejercicio actual.
- `submitAnswer` con respuesta correcta → `isCorrect = true`,
  `submitAnswerUseCase` llamado con `quality = 5`, `markUnitProgressUseCase`
  llamado con el `unitId` del ejercicio.
- `submitAnswer` con respuesta incorrecta → `isCorrect = false`,
  `quality = 2`.
- `submitAnswer` llamado dos veces sobre el mismo ejercicio (ya
  respondido) → no-op, no se llaman los use cases una segunda vez.
- `nextExercise` cuando quedan ejercicios → avanza la cola, resetea
  `isAnswered`/`isCorrect`/`selectedAnswer`, decodifica el siguiente.
- `nextExercise` en el último ejercicio → espera el job de la última
  respuesta, llama `updateStreakUseCase`, marca `isSessionComplete = true`.

### `CheckpointViewModelTest`

- `init` → llama `getCheckpointSessionUseCase(sectionId)`; cola vacía →
  `isComplete = true` de entrada con `CheckpointResult(0, false)`.
- `submitAnswer` correcta → incrementa el conteo interno de correctas;
  incorrecta → no lo incrementa. Ambas llaman `submitAnswerUseCase` y
  `markUnitProgressUseCase`.
- `nextExercise` no-último → avanza la cola, incrementa `currentIndex`.
- `nextExercise` último → espera el job pendiente, `updateStreakUseCase`,
  y `completeCheckpointUseCase(sectionId, CheckpointKind.REVIEW,
  correctCount, total, today)` — el resultado devuelto queda en
  `uiState.result`.

### `PlacementCheckpointViewModelTest`

- `init` → llama `getSkippedUnitsUseCase(targetUnitId)` y
  `getPlacementCheckpointSessionUseCase(skippedIds)`; puebla
  `skippedUnits`/`targetUnit`/`queue`.
- `startCheckpoint()` con cola vacía → completa directo con
  `CheckpointResult(0, false)`.
- `startCheckpoint()` con cola → `hasStarted = true`, decodifica el primer
  ejercicio.
- `submitAnswer` → **nunca** llama `submitAnswerUseCase` en el momento
  (queda bufferado internamente). Se verifica con un fake de
  `ExerciseRepository` que falla el test (`error(...)`) si
  `saveReviewState`/la ruta de `submitAnswerUseCase` se invoca durante
  `submitAnswer` — no alcanza con un assert después, el test debe fallar
  ruidosamente si el orden se rompe.
- `nextExercise` en el último ejercicio, **aprueba** (≥68%) → recién ahí
  se llama `submitAnswerUseCase` una vez por cada respuesta bufferada, con
  la `quality` correspondiente a si esa respuesta puntual fue correcta o
  no.
- `nextExercise` en el último ejercicio, **reprueba** (<68%) →
  `submitAnswerUseCase` no se llama ninguna vez — es el caso más
  importante del archivo, la garantía de la que depende todo el diseño de
  Fase 2.1b (no filtrar ejercicios todavía bloqueados a la rotación diaria
  si el examen de ubicación se reprueba).
- No se testea `markUnitProgressUseCase` acá — este ViewModel no la
  inyecta; el desbloqueo de unidades saltadas ya lo cubre
  `CompleteCheckpointUseCaseTest` (existente, sin cambios).

## Explícitamente fuera de alcance

- Cualquier test de Compose/UI — es Nivel 2 de la ADR, ciclo separado.
- Navegación (qué pantalla se abre después) — vive fuera del ViewModel.
- Re-testear la lógica interna de `SchedulerSm2`/`computeCheckpointResult`
  — ya tienen su propia cobertura en `domain/usecase`; acá solo se
  verifica que el ViewModel los invoque con los argumentos correctos.
- Cambios a los ViewModels mismos — este ciclo es puramente de tests,
  cero cambio de comportamiento productivo.

## Testing

Los propios tests de ViewModel **son** el testing de este ciclo — no hay
una capa adicional. Cada archivo sigue TDD estándar del proyecto
(`kotlinx-coroutines-test`'s `runTest`, JUnit4, sin librería de mocking).
Verificación final: `./gradlew :app:testDebugUnitTest` (regresión
completa) — sin QA manual en dispositivo, porque no hay cambio de
comportamiento observable por el usuario.

## Decisiones registradas

| Decisión | Elegido | Fecha |
|---|---|---|
| Alcance del ciclo | Los 3 ViewModels (Session, Checkpoint, Placement), no solo los 2 tocados por el fix reciente | 2026-07-25 |
| Fakes de use case vs. de repositorio | Fakes de los 4 repositorios + casos de uso reales (no se convierten los use cases a interfaces) | 2026-07-25 |
| Fakes compartidos vs. privados por archivo | Compartidos en `testutil/` — primera vez con 3+ consumidores reales del mismo fake | 2026-07-25 |
| Caso más crítico a cubrir | `PlacementCheckpointViewModel` reprobado → cero llamadas a `submitAnswerUseCase` | 2026-07-25 |
