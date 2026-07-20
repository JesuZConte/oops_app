# Spec — Fase 2.1: Fundación + rebanada vertical (Secciones/Unidades/Checkpoints)

> Diseño técnico detallado. Baja a implementación las decisiones del ADR
> `docs/adrs/2026-07-20-content-structure-sections-checkpoints.md` y del roadmap
> `docs/specs/2026-07-20-fase2-content-roadmap.md`. **No es el plan de tareas** —
> es el *qué* y el *por qué* técnico. El plan (writing-plans) sale de este spec.

## Objetivo

Introducir la jerarquía **Sección → Unidad → Checkpoint** sobre el modelo plano
`Topic → Exercise` actual, probada end-to-end con dos secciones reales
(**Fundamentos** nuevo + **Streams** reorganizado), sin perder el progreso SM-2
existente del usuario.

## Decisiones ya resueltas (dialogo de diseño, 2026-07-20)

1. **Mapeo conceptual:** una **Sección** ≈ lo que hoy es un dominio/`Topic`
   (Fundamentos, Streams…). Una **Unidad** = subdivisión nueva dentro de la
   sección (los "mini-juegos"). El `Exercise` cuelga de una Unidad. El estado
   SM-2 sigue siendo por ejercicio individual.
2. **Saltar unidades (checkpoint de ubicación) = híbrido:** al aprobar al 68%,
   los ejercicios de las unidades saltadas se marcan completos para el gate y la
   barra de progreso, y se **siembran en SM-2 con intervalo corto** (reaparecen
   pronto para verificar, no se entierran con intervalo largo). Los ejercicios
   que sí se respondieron en el checkpoint de ubicación usan su resultado real
   como señal de siembra en vez del intervalo genérico corto.
3. **Checkpoint alimenta SM-2:** responder un ejercicio dentro de un checkpoint
   actualiza su agenda SM-2 igual que en la sesión diaria (acierto alarga, fallo
   reinicia). El pass/fail agregado al **68%** es un gate por encima de eso, no
   lo reemplaza.
4. **Unidad completa por primera pasada:** una unidad se marca completa cuando el
   jugador respondió todos sus ejercicios al menos una vez (estilo lección
   Duolingo). El dominio real crece aparte vía SM-2. Completar todas las unidades
   de una sección desbloquea la siguiente sección.
5. **Umbral 68%** para ambos usos del checkpoint (repaso voluntario y ubicación)
   — el umbral real del examen 1Z0-830.
6. **Gating:** en juego normal las unidades van en orden (completar la N
   desbloquea la N+1). El checkpoint de ubicación es el único atajo para saltar
   adelante. El checkpoint de repaso voluntario (fin de sección) **no bloquea**
   nada.

## Modelo de datos

### Entidades

Estado actual (a migrar):
- `TopicEntity(id, name, certObjective, orderIndex)`
- `ExerciseEntity(id, topicId, type, payload, difficulty)`
- `ReviewStateEntity(exerciseId, easeFactor, intervalDays, repetitions, dueDate)` ← **dato precioso, no perder**
- `UserStatsEntity(id, streak, xp, lastStudyDate)` ← **dato precioso, no perder**

Modelo nuevo:

```kotlin
@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val orderIndex: Int,
    val examVersion: String   // "java21" (examen) | "modern" (extra 22-25) | "core"
)

// Reemplaza conceptualmente a TopicEntity: una Unidad pertenece a una Sección.
@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val name: String,
    val certObjective: String,   // se conserva: agrupador de objetivo del examen
    val orderIndex: Int          // orden dentro de la sección
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val unitId: String,          // antes topicId
    val type: String,
    val payload: String,
    val difficulty: Int,
    val examVersion: String      // "java21" | "modern" | "core" — para la etiqueta examen/moderno
)

// Progreso de primera-pasada por unidad (para gating y barra).
@Entity(tableName = "unit_progress")
data class UnitProgressEntity(
    @PrimaryKey val unitId: String,
    val completed: Boolean,       // true = primera pasada terminada
    val completedAt: Long?        // epoch day, null si no completa
)

// Intento de checkpoint (para historial y para no re-otorgar saltos ya hechos).
@Entity(tableName = "checkpoint_attempts")
data class CheckpointAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: String,        // sección evaluada
    val kind: String,             // "review" | "placement"
    val scorePct: Int,            // 0..100
    val passed: Boolean,          // scorePct >= 68
    val takenAt: Long             // epoch day
)
```

`ReviewStateEntity` y `UserStatsEntity` **no cambian**.

### Migración (resuelve riesgo #2)

**Estrategia: el contenido es re-sembrable desde los assets; `review_state` y
`user_stats` son el dato del usuario que se preserva.**

- Las tablas de contenido (`sections`, `units`, `exercises`) se tratan como
  derivadas de los JSON packs en `assets/content/`. La migración las
  reconstruye desde los assets v2.
- `review_state` referencia ejercicios por `exerciseId`. **Mientras los ids de
  los 20 ejercicios de Streams existentes se mantengan estables en el pack v2**,
  su estado SM-2 sobrevive intacto — la reorganización solo reasigna cada
  ejercicio a una Unidad (cambia `unitId`), no su `id`.
- La migración Room (v1 → v2):
  1. Crea las tablas nuevas (`sections`, `units`, `unit_progress`,
     `checkpoint_attempts`) y las columnas nuevas (`exercises.examVersion`,
     rename lógico `topicId`→`unitId`).
  2. Borra el contenido viejo (`topics`, y el contenido de `exercises`) y
     re-siembra desde los packs v2 — que incluyen `sections`/`units` y reasignan
     los ejercicios existentes (ids estables) más los nuevos de Fundamentos.
  3. Limpia `review_state` huérfano (ids que ya no existen en v2), si los hay.
  4. `user_stats` intacto.
- Se agrega `exportSchema = true` + esquema versionado (hoy hay un warning de
  `room.schemaLocation` sin configurar) para poder testear la migración.

**Test obligatorio de migración:** partir de una BD v1 sembrada con los 20
ejercicios de Streams + `review_state` no trivial, migrar, y verificar que (a)
el `review_state` de cada ejercicio sobrevive, (b) `user_stats` (racha/XP)
sobrevive, (c) los ejercicios quedan asignados a unidades correctas.

## Formato de contenido v2 (JSON packs)

```jsonc
{
  "sectionId": "java-fundamentals",
  "name": "Fundamentos de Java",
  "orderIndex": 1,
  "examVersion": "core",
  "units": [
    {
      "unitId": "fund-what-is-java",
      "name": "¿Qué es Java?",
      "certObjective": "language-basics",
      "orderIndex": 1,
      "exercises": [ /* ExerciseContent como hoy, + campo examVersion */ ]
    }
  ]
}
```

- Un pack por Sección (antes era un pack por dominio/topic).
- `ContentPack`, `ContentMapper`, `ContentSeeder` se actualizan al formato v2.
- Etiqueta `examVersion` a nivel de ejercicio (heredable de la sección):
  `"java21"` = examinable en 1Z0-830, `"modern"` = extra Java 22-25, `"core"` =
  fundamentos no atados a una versión específica.

## Dominio

### Completar unidad (primera pasada)

- Al responder un ejercicio (`SubmitAnswerUseCase`, ya existente), además de
  actualizar SM-2, se registra que ese ejercicio fue "visto".
- Cuando todos los ejercicios de una unidad fueron vistos ≥1 vez, se marca
  `UnitProgressEntity.completed = true` y se desbloquea la siguiente unidad.

### Sesión de checkpoint (use case nuevo)

`GetCheckpointSessionUseCase(sectionId, kind)`:
- Ensambla un cuestionario de tamaño fijo mezclando:
  - **Mayoría** de ejercicios de la sección objetivo.
  - Una **muestra menor** de secciones anteriores ya vistas (el "callback tipo
    entrevista").
- **Recomendación de arquitecto (ajustable):** ~12 preguntas, ~9 de la sección
  objetivo + ~3 de secciones anteriores. Con 12 preguntas el 68% ≈ 8/12 aciertos
  para aprobar. Estos números son tuning, no arquitectura — se afinan en pruebas.
- `kind = "placement"`: la sección objetivo es la que se quiere saltar; aprobar
  dispara la lógica de salto híbrido (decisión #2).
- `kind = "review"`: la sección objetivo es la recién completada; no dispara
  ningún gate, solo alimenta SM-2 y registra el intento.

### Gating

- `SectionGatingService` (o equivalente) expone: qué secciones/unidades están
  desbloqueadas, y si un destino requiere checkpoint de ubicación para saltar.
- Al pasar un checkpoint de ubicación: marcar unidades saltadas
  `completed = true`, sembrar sus ejercicios en `review_state` con intervalo
  corto (los muestreados usan su resultado real), registrar `CheckpointAttempt`.

### Reintentos

- **Recomendación de arquitecto:** reintentos ilimitados y no punitivos (estilo
  Duolingo). Reprobar un checkpoint no penaliza racha/XP; simplemente no otorga
  el salto (ubicación) o no marca "repasado" (voluntario). Ajustable.

### Checkpoint y SM-2

- Cada ejercicio respondido dentro de un checkpoint pasa por el mismo
  `SubmitAnswerUseCase` → SM-2 (decisión #3). El pass/fail agregado se calcula
  aparte sobre las respuestas del checkpoint.

## Contenido de la rebanada (propuesta de arquitecto, a confirmar/curar)

**Sección 1 — Fundamentos de Java** (`examVersion: "core"`), del cap. 1 del
libro. Unidades propuestas:
1. ¿Qué es Java? (JDK/JRE/JVM, compilación vs. ejecución)
2. Estructura de una clase (fields, methods, constructors; clase vs. archivo)
3. Tipos, variables y el método `main`

**Sección — Streams y lambdas** (reorganización del contenido existente;
`examVersion: "java21"`). Los 20 ejercicios actuales se reparten (ids estables)
en unidades propuestas:
1. Creación de streams
2. Operaciones intermedias (map/filter…)
3. Operaciones terminales (collect/reduce/forEach…)
4. Lambdas y method references

La autoría/asignación exacta de ejercicios es tarea del plan, no de este spec.

## Tipos de ejercicio en la rebanada

- `fill_blank` — ya implementado, se conserva.
- `mcq` variantes (ya previsto en `PROJECT-OOPS.md` §7), como formatos de prompt
  distintos, sin nuevos `type` en el modelo si es posible:
  - concepto/entrevista (prompt de texto: "¿qué es una clase abstracta?")
  - clasificación de código (prompt con snippet: "¿getter, setter o
    constructor?")
- Escenarios **prácticos** con andamiaje *constreñido* (hueco a rellenar /
  selección / ordenar). **La escritura libre de código desde cero se difiere**
  (riesgo #1: sin compilador embebido hasta Fase 3).

## UI

- **Ruta:** de líneas de dominio planas a un camino **Sección → Unidades**
  estilo Duolingo, con nodos de unidad (bloqueada / actual / completa) y un nodo
  de checkpoint al final de cada sección. Reusa el lenguaje visual arcade ya
  existente (`ThemedCard`, pills, etc.).
- **Session:** renderiza los tipos de ejercicio de la rebanada; entra tanto para
  sesión diaria como para checkpoint.
- **Flujo de checkpoint:** pantalla de entrada (voluntario vs. ubicación),
  cuestionario, pantalla de resultado con score vs. 68% y consecuencia (salto
  otorgado / repaso registrado / reintentar).
- **Home:** la tarjeta "TU RUTA" refleja el progreso de la sección/unidad actual
  (reusa lo ya construido, ajustando la fuente del dato).

## Testing

- **Migración v1→v2** preservando `review_state` + `user_stats` (test crítico).
- `SchedulerSm2` no cambia (sus tests siguen válidos).
- Nuevo: `GetCheckpointSessionUseCase` (composición y tamaño), lógica de
  gating/salto híbrido (siembra de intervalo corto), completar unidad por
  primera pasada, cálculo de pass/fail al 68%. Todo Kotlin puro, sin Android.
- Contenido v2: parseo de packs con jerarquía Sección/Unidad.

## Fuera de alcance (difere a fases siguientes)

- Escritura libre de código con evaluación real (Fase 3, requiere sandbox).
- Mapeo/curación del resto de secciones — cap. 2-11 del libro + features Java 21
  de examen + extras 22-25 (Fase 2.3).
- Tipos de ejercicio adicionales más allá de los de la rebanada (Fase 2.2:
  Parsons completo, predict_output, etc.).
- Estadísticas de áreas débiles, notificación diaria (Fase 2.4).
