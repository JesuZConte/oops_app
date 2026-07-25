# Ruta / SM-2 Progress Sync — Diseño

**Estado:** Aprobado, pendiente de plan de implementación.

## Contexto

QA manual de Luis (2026-07-25, tras dejar pasar un día simulado) encontró
que Ruta quedó mostrando 0% de progreso ("Qué es Java?" como posición
actual) a pesar de tener XP 140 y haber respondido, según la base de datos
del dispositivo, **el 100% de los 69 ejercicios de las 3 secciones**
(Fundamentos, Genéricos y Colecciones, Streams) — confirmado extrayendo
`oops.db` del dispositivo (`unit_progress` vacía, `review_state` con 69
filas, todas las unidades con 100% de sus ejercicios respondidos).

Causa raíz, confirmada leyendo el código:

- `GetTodaySessionUseCase` (el motor SM-2 de "Estudiar hoy", de la Fase 1)
  arma la sesión diaria con `getDueExercises()` + `getNewExercises()`
  (`ExerciseDao.kt`). Ninguna de las dos queries filtra por sección/unidad
  bloqueada — tratan el pool completo de ejercicios como disponible desde
  el día uno, sin relación con lo que Ruta muestra como desbloqueado. Por
  eso aparecieron ejercicios de Streams sin haberla desbloqueado.
- `unit_progress` (la tabla que hace avanzar Ruta) solo se escribe vía
  `MarkUnitProgressUseCase`, y hoy **solo se llama al terminar una sesión
  de unidad específica** (`SessionViewModel.nextExercise()`,
  `unitId?.let { markUnitProgressUseCase(it, ...) }`, donde `unitId` viene
  de la navegación y es `null` para "Estudiar hoy"). Jugar la sesión diaria
  nunca marca ninguna unidad como completa, sin importar cuánto contenido
  se responda.

Este hueco existe desde que se introdujo el modelo Sección→Unidad (Fase
2.1) — `GetTodaySessionUseCase` nunca se actualizó para conocer ese modelo.
No se hizo visible antes porque el QA previo se hizo jugando unidades
específicas desde Ruta (que sí dispara `MarkUnitProgressUseCase`
correctamente), no principalmente vía "Estudiar hoy".

## Decisión

Unificar ambos sistemas alrededor de una sola fuente de verdad
(`review_state` = qué se jugó de verdad), con dos cambios:

### 1. "Estudiar hoy" ofrece contenido nuevo solo de la unidad actual de Ruta

Nuevo use case `GetCurrentUnitUseCase` (envuelve `GetLearningPathUseCase`,
mismo patrón que `GetSkippedUnitsUseCase`): recorre secciones/unidades en
orden y devuelve la primera `LearningUnit` con `unlocked && !completed`, o
`null` si no queda nada por jugar.

`GetTodaySessionUseCase` pasa de:
```kotlin
val due = exerciseRepository.getDueExercises(today, limit = Int.MAX_VALUE)
val new = exerciseRepository.getNewExercises(limit = newExercisesLimit)
return due + new
```
a: vencidos sin cambio de alcance (siguen siendo válidos de cualquier
sección — incluye los sembrados por un examen de ubicación aprobado) +
nuevos **solo de la unidad actual** (vía `getExercisesByUnit` +
`getAnsweredExerciseIds`, ambos ya existentes en `ExerciseRepository`, sin
necesidad de una query SQL nueva).

Efecto: "Estudiar hoy" se convierte literalmente en "la unidad actual de
Ruta, empaquetada como sesión diaria" más los repasos vencidos — ya no
expone contenido de secciones/unidades no alcanzadas todavía.

### 2. Marcar unidad completa se mueve al momento de responder, no al final de sesión

`MarkUnitProgressUseCase` ya implementa exactamente la lógica necesaria
(¿todos los ejercicios de esta unidad tienen `review_state`?) — no cambia.
Lo que cambia es **cuándo se llama**:

- `SessionViewModel.submitAnswer()`: después de `submitAnswerUseCase(...)`,
  se llama `markUnitProgressUseCase(exercise.unitId, today)` usando el
  `unitId` del ejercicio recién respondido (disponible en
  `current.queue.first().unitId` — es un `Exercise`, no el `ExerciseContent`
  decodificado). Se elimina la llamada actual de `nextExercise()`
  (`unitId?.let { ... }`, basada en el `unitId` de navegación) — queda
  subsumida por la nueva, que cubre tanto sesiones de unidad específica
  como "Estudiar hoy" con el mismo mecanismo.
- `CheckpointViewModel.submitAnswer()`: mismo patrón — se agrega la llamada
  a `markUnitProgressUseCase` (nueva dependencia inyectada), usando el
  `unitId` del ejercicio del checkpoint voluntario recién respondido.

**`PlacementCheckpointViewModel` queda explícitamente fuera de este
cambio.** Ya tiene su propio mecanismo (Fase 2.1b): las respuestas se
buffean y solo se confirman a SM-2 si el examen se aprueba, y
`CompleteCheckpointUseCase.unlockSkippedUnits()` ya marca las unidades
saltadas como completas (`completedVia = PLACEMENT`) cuando corresponde —
sin depender de si se respondió cada ejercicio individual de esas unidades.
Agregar la marca por-respuesta ahí escribiría estado antes de saber si el
examen se aprueba, rompiendo la garantía ya establecida de "no escribir
nada si repruebas".

## Efecto secundario esperado (sin cambio de código adicional)

El checkpoint voluntario de fin de sección probablemente tampoco se le
ofreció nunca a Luis, porque "sección completa" depende de la misma tabla
`unit_progress` que nunca se poblaba. Se corrige solo con el fix de arriba
— no requiere tocar `GetCheckpointSessionUseCase` ni la UI de Ruta.

## Progreso actual del dispositivo de prueba

Con `unit_progress` vacía hoy, este fix no recupera retroactivamente lo ya
jugado (si una unidad ya tiene el 100% de sus ejercicios respondidos, no
queda ningún ejercicio "nuevo" que dispare la marca al responder). Se
decide (2026-07-25, sin usuarios reales todavía) **no construir un
mecanismo de backfill/migración** — Luis reinstala limpio después de este
fix, como en cada fase anterior. Si en el futuro la app tiene una base de
usuarios reales instalada, este mismo escenario ameritaría revisar esta
decisión (ej. un backfill disparado por versión de contenido, similar al
patrón de `ContentSeeder`) — explícitamente fuera de alcance ahora.

## Testing

- **`GetCurrentUnitUseCaseTest`** (nuevo): unidad actual con secciones
  parcialmente completas, con todo completo (devuelve `null`), con
  secciones bloqueadas de por medio.
- **`GetTodaySessionUseCaseTest`** (actualizar): "nuevo" ya no debe incluir
  ejercicios de una unidad distinta a la actual, incluso si esa unidad
  está desbloqueada; "vencido" no cambia de comportamiento.
- Sin tests de ViewModel (no existen en este proyecto hoy — ver
  `docs/adrs/2026-07-24-viewmodel-and-smoke-testing-strategy.md`,
  decisión ya tomada de agregarlos como iniciativa separada). Verificación
  de `SessionViewModel`/`CheckpointViewModel` vía compilación + QA manual
  en dispositivo, mismo patrón que el resto del proyecto.
- QA manual en dispositivo (reinstalación limpia): jugar la unidad actual
  completa vía "Estudiar hoy" y confirmar que Ruta avanza; confirmar que
  "Estudiar hoy" no ofrece contenido de una sección todavía bloqueada;
  confirmar que al completar la última unidad de una sección aparece el
  checkpoint voluntario; confirmar que el examen de ubicación (Fase 2.1b)
  sigue funcionando igual (no debe verse afectado por estos cambios).

## Decisiones registradas

| Decisión | Elegido | Fecha |
|---|---|---|
| Alcance del fix | Unificar ambos sistemas (no solo comunicar la separación, no restringir "Estudiar hoy" a solo repaso) | 2026-07-25 |
| Qué ofrece "Estudiar hoy" como nuevo | Solo la unidad actual de Ruta, no cualquier unidad desbloqueada | 2026-07-25 |
| PlacementCheckpointViewModel | Sin cambios — preserva su mecanismo de defer-on-fail ya existente | 2026-07-25 |
| Progreso actual del dispositivo de prueba | Reinstalar limpio, sin backfill | 2026-07-25 |
