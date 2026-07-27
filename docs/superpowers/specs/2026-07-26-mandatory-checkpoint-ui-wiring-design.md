# Checkpoint obligatorio — Plan 2: wiring de UI (timer, resultados, bloqueo de reintento) — Diseño

**Estado:** Aprobado, pendiente de plan de implementación.

## Contexto

`docs/superpowers/specs/2026-07-26-mandatory-cumulative-checkpoint-design.md`
("Plan 1") ya definió y construyó la capa de dominio/persistencia del
checkpoint obligatorio: migración v3→v4, el gate en
`GetLearningPathUseCase` (`SectionPath.checkpointSatisfied`), el muestreo
acumulativo/creciente (`GetCheckpointSessionUseCase`), la barrera de
reintento (`IsCheckpointRetryUnlockedUseCase`), y dos funciones puras aún
sin usar: `computeCheckpointTimeBudgetSeconds` y
`computeCheckpointSectionBreakdown`. Por decisión explícita ("Dos planes"),
Plan 1 no tocó ningún archivo de UI salvo un fix mecánico de firma en
`CheckpointViewModel.kt`.

Plan 1 ya se mergeó a `main` local y se probó en dispositivo (reinstalación
limpia). La QA manual confirmó que el gate funciona de punta a punta
(aprobar/reprobar/reintentar/desbloquear), pero también encontró dos huecos
de UX que esta spec resuelve junto con el trabajo ya previsto:

- **Bug real de crash (ya arreglado en Plan 1, no parte de esta spec):**
  `LocalDate.EPOCH` no existe en el `java.time.LocalDate` real de Android;
  se reemplazó por `LocalDate.ofEpochDay(0)`.
- **Hueco de UX (motivo de esta spec):** tocar "ESTUDIAR HOY" cuando no hay
  nada que estudiar porque la siguiente sección está bloqueada por un
  checkpoint pendiente rebota instantáneamente a Home sin explicación —
  porque `GetTodaySessionUseCase` devuelve una cola vacía y
  `SessionViewModel` marca la sesión como "completa" de inmediato.
- **Copy desactualizado:** `ProgressScreen`'s `CheckpointRow` todavía dice
  "Repaso opcional de esta sección", pese a que Plan 1 lo volvió
  obligatorio.

Esta spec cubre exactamente lo que Plan 1 dejó pendiente: timer visible,
pantalla de resultados con desglose, bloqueo de reintento reflejado en la
UI, y el arreglo del routing de "ESTUDIAR HOY".

## Decisión (resumen)

1. El checkpoint gana una **pantalla intro** ("N preguntas · M minutos ·
   Comenzar") antes de arrancar — el timer inicia al tocar "Comenzar", no
   al cargar la pantalla.
2. El **timer** se muestra junto al contador de preguntas existente
   ("3/12 · 14:32"), como presupuesto total (ya definido en Plan 1), y se
   persiste como un timestamp límite (no un contador en memoria) para
   sobrevivir rotación/recomposición.
3. Al **agotarse el tiempo**, se auto-envía lo respondido; lo no
   respondido cuenta como incorrecto en el puntaje, pero **no** entra al
   conjunto de "fallados" que bloquea el reintento (evita un dead-lock
   permanente — ver sección 4).
4. La pantalla de resultado muestra el **desglose por sección** (ya
   calculado por Plan 1) **solo cuando repruebas** — si apruebas, se
   mantiene la pantalla simple actual.
5. La fila del checkpoint en Ver Ruta pasa a tener **3 estados visuales**
   distintos (pendiente / bloqueado / disponible para reintentar) y
   desaparece cuando ya está aprobado.
6. **"ESTUDIAR HOY" nunca vuelve a rebotar en silencio ni choca contra un
   checkpoint bloqueado**: enruta según una prioridad de 3 niveles (sesión
   normal → checkpoint rendible → mensaje explicativo).
7. Cero cambios de esquema o de dominio nuevo más allá de wrappers
   delgados sobre lo que Plan 1 ya construyó.

## 1. Estado del checkpoint por sección (fundamento del resto)

`SectionPath` gana un campo `checkpointStatus: CheckpointStatus`, un enum
con 4 valores, calculado en `GetLearningPathUseCase`:

| Valor | Condición | Se traduce en |
|---|---|---|
| `SATISFIED` | `checkpointSatisfied` (ya definido en Plan 1) | Fila oculta en Ver Ruta; sección siguiente desbloqueada |
| `PENDING` | Sección completa, ningún intento fallido registrado (`getLatestFailedAttempt == null`) | "Checkpoint obligatorio" |
| `RETRY_LOCKED` | Hay un intento fallido y `IsCheckpointRetryUnlockedUseCase` da `false` | "Repasa lo fallado para reintentar" (color de advertencia) |
| `RETRY_AVAILABLE` | Hay un intento fallido y `IsCheckpointRetryUnlockedUseCase` da `true` | "Reinténtalo ahora" |

`GetLearningPathUseCase` gana una dependencia nueva
(`IsCheckpointRetryUnlockedUseCase`, ya existe de Plan 1) para calcular
esto. Es la única fuente de verdad; todo lo demás (Ver Ruta, Home, el
routing de "ESTUDIAR HOY") lo consume, sin recalcular nada por su cuenta.

Para una sección cuyas unidades **todavía no están completas**
(`sectionComplete == false`), `checkpointStatus` es `PENDING` por
definición (no hay intento fallido posible sin haber terminado la
sección) — es un valor sin efecto observable ahí, porque `ProgressScreen`
sigue mostrando/ocultando `CheckpointRow` según `sectionPath.completed`
antes de mirar `checkpointStatus` (sección 2). No hace falta un 5º valor
"no aplica".

**`checkpointStatus` es aditivo, no reemplaza nada de Plan 1.** El campo
existente `SectionPath.checkpointSatisfied: Boolean` (y la variable interna
`previousSectionFullyDone` que ya lo consume para calcular `unlocked` de
la siguiente sección) se dejan intactos — son la lógica de gate probada en
dispositivo, no hay que tocarla. `checkpointStatus == SATISFIED` debe ser
equivalente a `checkpointSatisfied == true` (la misma condición, expuesta
también como enum para que la UI distinga los 3 sub-estados de "todavía
no").

## 2. Ver Ruta (`ProgressScreen` / `CheckpointRow`)

`CheckpointRow` lee `sectionPath.checkpointStatus`:

- Se **oculta** cuando es `SATISFIED` (comportamiento actual, sin cambio).
- Se muestra con copy y color según `PENDING` / `RETRY_LOCKED` /
  `RETRY_AVAILABLE` (reemplaza el "Repaso opcional de esta sección" fijo
  actual).
- El `onClick` sigue siendo el mismo en los 3 casos visibles: navega al
  checkpoint. Es `CheckpointViewModel` quien decide qué mostrar dentro
  (preguntas, o el mensaje de bloqueo — sección 5), no `ProgressScreen`.

## 3. Home (`HomeViewModel` / `HomeScreen`)

- `HomeViewModel.refreshStats()` cambia el criterio de "sección actual" de
  `sections.firstOrNull { !it.completed }` a
  `sections.firstOrNull { !it.checkpointSatisfied }`. Es un cambio de una
  condición: hoy salta de largo una sección con unidades completas pero
  checkpoint pendiente (porque `completed=true`); con `checkpointSatisfied`
  se queda correctamente en ella.
- `HomeUiState` gana un flag (`isCheckpointPending: Boolean`, derivado de
  `currentSection.completed && !currentSection.checkpointSatisfied`). La
  tarjeta TU RUTA, cuando este flag es verdadero, muestra **"Checkpoint
  pendiente"** en vez del `%` y la barra de progreso.

## 4. Routing de "ESTUDIAR HOY" (nuevo `GetNextStudyStepUseCase`)

Nuevo use case puro-de-orquestación (sin UI) que decide a dónde debe ir
"ESTUDIAR HOY". Devuelve exactamente uno de 3 resultados (sin casos
ambiguos ni superpuestos):

1. **`DailySession`** — hay algo que estudiar hoy
   (`GetTodaySessionUseCase` no devuelve vacío). Comportamiento actual, sin
   cambio: navega a la sesión normal.
2. **`Checkpoint(sectionId)`** — no hay nada que estudiar, pero existe una
   sección con `checkpointStatus` distinto de `SATISFIED` (es decir,
   `PENDING`, `RETRY_AVAILABLE`, o `RETRY_LOCKED` — **los 3 casos navegan
   al mismo destino**, `checkpoint/{sectionId}`; es `CheckpointViewModel`
   quien decide internamente si muestra preguntas, la pantalla intro, o el
   mensaje de bloqueo de la sección 5). Esto arregla el bug de esta spec:
   nunca se abre una sesión vacía que rebota, y nunca se navega "a nada" —
   si el checkpoint está bloqueado, el usuario ve el mensaje explicativo,
   no una sesión en blanco.
3. **`NothingPending`** — no hay nada que estudiar y ninguna sección tiene
   un checkpoint pendiente (todo satisfecho, o no queda contenido). Único
   caso donde de verdad no hay nada que hacer.

Esto reemplaza el `onStudyClick: () -> Unit` incondicional actual de
`HomeScreen` por un flujo que primero consulta `GetNextStudyStepUseCase` y
navega según el resultado — sin ruta nueva (los destinos, `SESSION` y
`checkpoint/{sectionId}`, ya existen).

## 5. Checkpoint — máquina de estados (`CheckpointViewModel` / `CheckpointScreen`)

`CheckpointUiState` pasa a modelar explícitamente 4 fases:

- **`RETRY_LOCKED`**: si `IsCheckpointRetryUnlockedUseCase` da `false` al
  entrar, no se cargan preguntas. Se muestra un mensaje **específico**
  (usa el desglose por sección del intento fallido, ya calculable con lo
  que Plan 1 construyó) del tipo: "Fallaste preguntas de *Streams y
  lambdas*. Repásalas en tu práctica diaria — cuando vuelvan a
  aparecerte y las respondas, podrás reintentar el checkpoint." Con un
  botón para volver. Es el mismo estado que resuelve el caso 3 de la
  sección 4 (nunca es una pared muda).
- **`INTRO`** (si el reintento está disponible o es el primer intento):
  pantalla corta con "N preguntas · M minutos" y un botón "Comenzar". El
  timer **arranca al tocar Comenzar**, no antes — evita que una entrada
  accidental o una rotación de pantalla consuman tiempo del examen.
- **`IN_PROGRESS`**: el flujo actual de `ExerciseAnswerCard`, con el timer
  agregado junto al contador ("3/12 · 14:32" — mismo header, sin UI
  nueva).
- **`COMPLETE`**: la pantalla de resultado actual, con el desglose
  agregado condicionalmente (sección 7).

## 6. Timer

- **Presupuesto:** `computeCheckpointTimeBudgetSeconds(queue.size)` (ya
  construido en Plan 1), calculado una vez al entrar a `IN_PROGRESS`.
- **Persistencia:** se guarda un timestamp límite
  (`System.currentTimeMillis() + budgetMillis`) en `SavedStateHandle`, no
  un contador que se decrementa en memoria — así una rotación de pantalla
  o la recreación del `ViewModel` no reinicia ni pierde el tiempo restante;
  el tiempo mostrado siempre se deriva de `deadline - now()`.
- **Al agotarse:** se auto-completa el checkpoint con lo respondido hasta
  ese momento. Las preguntas no alcanzadas cuentan como **incorrectas para
  el puntaje** (`correctCount`/`totalCount`, afecta si apruebas o no —
  fiel al examen real), pero:
- **No entran al conjunto de `failedExerciseIds`** que alimenta
  `IsCheckpointRetryUnlockedUseCase`. Motivo (bug real detectado en
  revisión de este diseño, antes de implementar): una pregunta nunca
  respondida no pasó por `SubmitAnswerUseCase`, así que su
  `ReviewState.lastReviewedAt` no se actualiza y su `dueDate` puede seguir
  en el futuro — nunca reaparecería en `getDueExercises` (INNER JOIN con
  `review_state`, filtra por `dueDate <= hoy`) para que el jugador la
  "reestudie", dejando el reintento bloqueado **para siempre**. Solo lo
  que el jugador respondió explícitamente mal cuenta para el gate de
  reintento — es justo (no se puede exigir repasar algo que nunca se vio)
  y evita el dead-lock sin piezas nuevas.

## 7. Pantalla de resultados con desglose (solo al reprobar)

`CheckpointResultView` (la vista actual de "¡Superado!" / "Casi lo
logras") agrega, **solo cuando `!result.passed`**, la tabla de aciertos
por sección ya diseñada en Plan 1:

```
Fundamentos de Java      9/10   90%
Streams y lambdas        2/4    50%
Genéricos y Colecciones  2/5    40%
```

- Nuevo wrapper delgado `GetCheckpointResultBreakdownUseCase` sobre
  `computeCheckpointSectionBreakdown` (ya existe y está testeada): junta
  `contentRepository.getSections()` +
  `contentRepository.getUnitsBySection(...)` para construir los mapas
  `unitsById`/`sectionsById` que la función pura necesita, a partir de la
  lista `List<Pair<Exercise, Boolean>>` que `CheckpointViewModel` acumula
  durante la sesión (cada exercise respondido, con su corrección).
- Si `passed == true`, se mantiene la pantalla simple actual — sin cambio.
- Sin drill-down por ejercicio individual en esta spec (mencionado como
  posible extensión en Plan 1, no se construye acá — YAGNI).

## Explícitamente fuera de alcance (no se construye en este plan)

- **Modo examen puro (diferir feedback por pregunta hasta el final):** más
  fiel al examen real 1Z0-830 (que no da feedback inmediato), pero el
  feedback por pregunta ya está construido y Luis lo probó conforme
  durante la QA de Plan 1. Se deja como idea para revisar más adelante
  *solo si* el timer se siente injusto en la práctica (una pregunta que
  toma mucho tiempo de lectura/feedback resta presupuesto igual). No se
  construye ahora.
- **Drill-down por ejercicio individual** en el desglose de resultados.
- **Timer para el examen de ubicación** (`PlacementCheckpointViewModel`):
  Plan 1 ya decidió que solo el checkpoint de sección voluntario/obligatorio
  lo tiene; el examen de ubicación no se toca en este plan.
- Todo lo ya registrado como fuera de alcance en la spec de Plan 1
  (simulacro de examen completo, preparación de entrevistas).

## Migración

Ninguna. Este plan no toca esquema ni tablas — es wiring de ViewModel/UI
puro sobre persistencia que Plan 1 ya migró. No requiere reinstalación
limpia por sí solo (aunque probablemente se pruebe junto con QA general).

## Testing

- **Tests de dominio/use case (JVM, fakes escritos a mano, sin librería de
  mocking — convención del proyecto):** `GetLearningPathUseCase`'s nuevo
  cálculo de `checkpointStatus` (los 4 valores, con fakes de
  `IsCheckpointRetryUnlockedUseCase`/`CheckpointRepository`);
  `GetNextStudyStepUseCase`'s 3 prioridades; `GetCheckpointResultBreakdownUseCase`'s
  construcción de mapas.
- **Tests de ViewModel** (reusando la infraestructura de Nivel 1 de la ADR
  de testing, `app/src/test/java/com/zconte/oopsapp/testutil/`):
  `CheckpointViewModel`'s máquina de estados (`RETRY_LOCKED` sin cargar
  preguntas, `INTRO` sin arrancar el timer hasta "Comenzar", auto-envío al
  agotarse el tiempo con la exclusión de `failedExerciseIds` para lo no
  respondido, desglose solo cuando reprueba); `HomeViewModel`'s nuevo
  criterio de sección actual y el flag de checkpoint pendiente.
- **QA manual en dispositivo:** confirmar que "ESTUDIAR HOY" nunca rebota
  en silencio (los 3 casos de la sección 4); confirmar que fallar una
  pregunta por timeout no bloquea el reintento tras re-estudiar solo lo
  explícitamente fallado; confirmar que rotar la pantalla durante el
  checkpoint no reinicia ni congela el timer; confirmar los 3 estados
  visuales de `CheckpointRow` en Ver Ruta; confirmar que el desglose solo
  aparece al reprobar.

## Decisiones registradas

| Decisión | Elegido | Fecha |
|---|---|---|
| Pantalla intro antes del checkpoint | Sí; el timer arranca al tocar "Comenzar" | 2026-07-26 |
| Ubicación del timer | Junto al contador de preguntas existente | 2026-07-26 |
| Timeout | Auto-envía lo respondido; lo no respondido cuenta como incorrecto en el puntaje | 2026-07-26 |
| Timeout × gate de reintento | Lo no respondido por timeout NO entra a `failedExerciseIds` (evita dead-lock permanente) | 2026-07-26 |
| Desglose de resultados | Solo al reprobar, expandiendo la misma pantalla (sin ruta nueva) | 2026-07-26 |
| Estados visuales de `CheckpointRow` | 3 estados (pendiente/bloqueado/disponible) + oculta cuando aprobado | 2026-07-26 |
| Routing de "ESTUDIAR HOY" | 3 niveles de prioridad; nunca rebota en silencio ni choca contra un checkpoint bloqueado | 2026-07-26 |
| Ubicación de la lógica de estado de reintento | `GetLearningPathUseCase` (única fuente de verdad, mismo patrón que Plan 1) | 2026-07-26 |
| Modo examen puro (sin feedback inmediato) | Fuera de alcance; revisar solo si el timer se siente injusto en la práctica | 2026-07-26 |
| Persistencia del timer | Timestamp límite en `SavedStateHandle`, no un contador en memoria | 2026-07-26 |
