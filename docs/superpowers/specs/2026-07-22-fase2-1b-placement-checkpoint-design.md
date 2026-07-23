# Fase 2.1b — Checkpoint de ubicación: Implementation Design

> Baja a implementación la parte de la Decisión 2 del ADR
> `docs/adrs/2026-07-20-content-structure-sections-checkpoints.md` ("examen de ubicación") y de la
> sección "Gating"/"Saltar unidades" del spec `docs/specs/2026-07-20-fase2-1-foundation-spec.md`,
> que Fase 2.1 dejó deliberadamente fuera de su rebanada vertical. El checkpoint de **repaso
> voluntario** (fin de sección) ya está construido y no cambia — este documento cubre únicamente
> el mecanismo de salto.

## Objetivo

Permitir que el jugador toque una unidad o sección bloqueada, tome un cuestionario de ubicación
sobre todo lo que se saltaría, y si aprueba al 68% (mismo umbral del examen 1Z0-830, ya usado por
el checkpoint de repaso), desbloquee el destino sin tener que jugar cada unidad intermedia en
orden. Las unidades saltadas se marcan completas y se siembran en SM-2 con intervalo corto para
que resurjan pronto y se verifique el dominio real, en vez de darlas por aprendidas para siempre.

## Contexto de código relevante (verificado, no del spec original)

Fase 2.1 ya construyó casi todo el andamiaje que este mecanismo necesita:

- `CheckpointAttemptEntity` ya tiene el campo `kind`; `CheckpointKind.PLACEMENT` ya existe como
  constante pero **no se usa en ningún lado hoy** — solo `CheckpointKind.REVIEW` está cableado
  (`ui/checkpoint/CheckpointViewModel.kt:99`).
- El gating (`domain/usecase/GetLearningPathUseCase.kt`) ya es estrictamente secuencial en dos
  niveles: unidad dentro de sección, y sección completa. Una unidad bloqueada — sea la 2ª unidad
  de la sección actual o la 1ª de una sección futura — ya es una fila individual no-clickeable en
  `ui/progress/ProgressScreen.kt` (`UnitRow`, línea 136: `.clickable(enabled = unitProgress.unlocked, ...)`).
  **Esto significa que "saltar una unidad suelta" y "saltar una sección entera" son el mismo caso**
  en el código: no hay un concepto de "tap de sección" separado, solo tap de unidad bloqueada.
- `ContentRepository.markUnitCompleted(unitId, completedAt)` y
  `ExerciseRepository.saveReviewState(state)` ya existen y ya son los mecanismos reales que usa el
  flujo normal — no hace falta inventar side-effects nuevos, solo invocarlos desde el lugar nuevo.
- `CompleteCheckpointUseCase` ya calcula `scorePct`/`passed` al 68% y graba el intento — el
  checkpoint de ubicación reutiliza esa misma función, extendida.

## Modelo de datos

### Migración v2 → v3 (nueva)

`UnitProgressEntity` gana una columna para distinguir cómo se completó una unidad:

```kotlin
@Entity(tableName = "unit_progress")
data class UnitProgressEntity(
    @PrimaryKey val unitId: String,
    val completed: Boolean,
    val completedAt: Long?,
    val completedVia: String = UnitCompletionSource.PLAYED   // nuevo
)
```

- `ALTER TABLE unit_progress ADD COLUMN completedVia TEXT NOT NULL DEFAULT 'played'` — todo lo ya
  completado hoy (jugado en orden) se backfillea como `"played"`, correcto por construcción.
- No toca `review_state` ni `user_stats`. Mismo patrón de riesgo bajo que la migración v1→v2 de
  Fase 2.1, pero test instrumentado propio (ver Testing) en vez de reutilizar el existente.
- `AppDatabase.kt`: `version = 3`, agregar el nuevo objeto `Migration(2, 3)`.

### Constantes nuevas

Sigue el patrón ya establecido por `domain/model/CheckpointKind.kt` (constantes string, no enum,
consistente con cómo se persiste `CheckpointAttemptEntity.kind`):

```kotlin
object UnitCompletionSource {
    const val PLAYED = "played"
    const val PLACEMENT = "placement"
}
```

### Modelo de dominio

`UnitProgress` (`domain/model/SectionPath.kt`) gana el campo `completedVia: String` para que
`ProgressScreen` pueda leerlo sin ir a buscar la entidad Room directamente.

No se agregan tablas nuevas. `CheckpointAttemptEntity`/`CheckpointAttemptDao` no cambian.

## Dominio

### `GetSkippedUnitsUseCase(targetUnitId: String): List<LearningUnit>` (nuevo)

Aplana el resultado de `GetLearningPathUseCase` en una secuencia global ordenada
(sección→unidad, ambos ya vienen con `orderIndex`), ubica `targetUnitId`, y devuelve todas las
unidades **no completadas** que aparecen antes de él en esa secuencia. Al ser gating secuencial,
esto es siempre un prefijo contiguo — no requiere lógica especial para saltos que abarcan varias
secciones, sale de aplanar y filtrar.

Si `targetUnitId` ya está desbloqueado (lista vacía), quien llame no debería haber ofrecido el
checkpoint — se trata como precondición de la UI, no como caso a manejar defensivamente acá.

### `GetPlacementCheckpointSessionUseCase(skippedUnitIds: List<String>): List<Exercise>` (nuevo)

Arma el cuestionario desde `exerciseRepository.getExercisesByUnit(id)` de cada unidad saltada
(método ya existente). Tamaño objetivo: **3 preguntas por unidad saltada, tope 24**,
`coerceAtMost` al tamaño real del pool combinado (mismo patrón defensivo que ya usa
`GetCheckpointSessionUseCase` para el repaso voluntario). Muestreo aleatorio simple sobre el pool
combinado, no por-unidad-garantizada — una unidad con pocos ejercicios puede quedar sub-representada,
aceptable por la misma razón que el examen real aproxima con muestreo (ADR, sección Trade-offs).

Separado de `GetCheckpointSessionUseCase` a propósito: son dos algoritmos de muestreo distintos
con inputs distintos (un `sectionId` vs. una lista de unidades), fusionarlos en una sola clase con
ramas por `kind` los acoplaría sin necesidad real.

### `CompleteCheckpointUseCase` (extendido)

```kotlin
suspend operator fun invoke(
    sectionId: String,
    kind: String,
    correctCount: Int,
    totalCount: Int,
    today: LocalDate,
    skippedUnitIds: List<String> = emptyList()   // nuevo, solo relevante si kind == PLACEMENT
): CheckpointResult
```

Para un intento de ubicación, `sectionId` es la sección del `targetUnitId` (identifica "a dónde
intentaba llegar", solo para el registro en `checkpoint_attempts` — no afecta el cálculo de
`scorePct`/`passed`, que ya es agnóstico de sección). El cálculo de `scorePct`/`passed`/
`recordAttempt` no cambia. Se agrega, solo cuando `kind == PLACEMENT && passed`:

1. `skippedUnitIds.forEach { contentRepository.markUnitCompleted(it, today, UnitCompletionSource.PLACEMENT) }`
   — `markUnitCompleted` gana un tercer parámetro `via: String` (antes solo `PLAYED` desde
   `MarkUnitProgressUseCase`, que sigue llamándolo con `PLAYED` sin cambios de comportamiento).
2. Para cada ejercicio de esas unidades que **no** fue parte de la muestra del checkpoint (y por
   lo tanto no pasó por `SubmitAnswerUseCase` durante este intento): si no tiene `review_state`
   previo (`saveReviewState` no debe pisar progreso SM-2 real de una unidad que el jugador ya
   había tocado antes por otro camino), sembrar
   `ReviewState(exerciseId, easeFactor = 2.5, intervalDays = 1, repetitions = 1, dueDate = today.plusDays(1))`.
   Los ejercicios que sí fueron parte de la muestra ya actualizaron su SM-2 real vía
   `SubmitAnswerUseCase` dentro del flujo normal de checkpoint — no se re-siembran.

El desbloqueo de secciones enteras **no requiere código nuevo**: `GetLearningPathUseCase` ya
deriva `sectionComplete`/`sectionUnlocked` de `unit_progress` en cada carga, así que marcar las
unidades saltadas como completas desbloquea en cascada cualquier sección intermedia la próxima
vez que Ruta se recomponga.

## UI

- **`ProgressScreen.UnitRow`**: se quita `enabled = unitProgress.unlocked` del `.clickable` — toda
  fila bloqueada pasa a ser clickeable y navega al nuevo flujo (antes era no-op). El subtítulo de
  una unidad completada cambia de ser siempre `"Completada"` a:
  `if (completedVia == PLACEMENT) "Completada por checkpoint" else "Completada"`. Mismo punto
  verde, sin cambio de color/ícono — decisión explícita del usuario (mantener el cambio mínimo).
- **Nueva pantalla `PlacementCheckpointScreen` + `PlacementCheckpointViewModel`**, misma forma que
  `CheckpointScreen`/`CheckpointViewModel` existentes (reutiliza `ExerciseAnswerCard`), con un
  estado adicional **antes** de las preguntas:
  - **Entrada**: `GetSkippedUnitsUseCase(targetUnitId)` se resuelve al entrar; se muestra cuántas
    unidades se saltarían y cuántas preguntas tendrá el cuestionario (`3 × N` con tope), con un
    botón para empezar y uno para cancelar (vuelve a Ruta sin gastar el intento).
  - **Preguntas**: idéntico al checkpoint de repaso (progreso, `ExerciseAnswerCard`, `SIGUIENTE`).
  - **Resultado**: aprobado → botón "CONTINUAR" navega directo a jugar `targetUnitId` (no solo
    vuelve a Ruta, ya que el objetivo era llegar ahí). Reprobado → "CONTINUAR" vuelve a Ruta;
    reintentos ilimitados y no punitivos (no afecta racha/XP), el jugador puede volver a tocar la
    misma unidad bloqueada cuando quiera.
- **Nav**: nueva ruta `placement_checkpoint/{targetUnitId}` en `OopsNavHost.kt`, alcanzable desde
  `ProgressScreen` cuando se toca una unidad con `unlocked == false`.

## Testing

Todo Kotlin puro sin Android salvo la migración, mismo patrón que el resto del dominio:

- `GetSkippedUnitsUseCase`: salto dentro de la sección actual, salto de una sección completa,
  salto de varias secciones encadenadas, target ya desbloqueado (lista vacía).
- `GetPlacementCheckpointSessionUseCase`: escalado `3 × unidades` con tope 24, `coerceAtMost` con
  pools chicos (unidad con pocos ejercicios).
- `CompleteCheckpointUseCase` extendido: siembra solo en ejercicios sin `review_state` previo, no
  siembra ni marca nada si `passed == false`, no marca nada si `kind == REVIEW` aunque se pase
  `skippedUnitIds` por error (defensivo, un solo `if`).
- **Migración v2→v3** (instrumentada, nueva): parte de una BD v2 con unidades ya completadas,
  migra, verifica `completedVia = 'played'` en todas, y que `review_state`/`user_stats` no se
  tocan — mismo tipo de test que `MigrationTest` ya tiene para v1→v2, pero cubriendo esta
  migración específica.

## Fuera de alcance

- Distinguir con color/ícono (no solo texto) una unidad completada por checkpoint — decisión
  explícita del usuario de mantenerlo solo textual por ahora.
- Cualquier tipo de ejercicio nuevo o cambio de formato de contenido (Fase 2.2).
- Curación de más secciones más allá de Fundamentos/Streams (Fase 2.3).
