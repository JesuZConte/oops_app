# Fase 2.2 — Diversidad de tipos de ejercicio — Diseño

**Estado:** Aprobado, pendiente de plan de implementación.

## Contexto

El roadmap de Fase 2 (`docs/specs/2026-07-20-fase2-content-roadmap.md`) describe
2.2 como: *"Tipos del ADR (concepto/entrevista, clasificación de código,
escenarios prácticos con andamiaje variable). Empezar constreñido (fill_blank,
opciones, ordenar tipo Parsons) y diferir la escritura libre de código desde
cero"*.

De esos tres tipos "constreñidos", `fill_blank` y `mcq` **ya existen** en el
motor — y `mcq` ya cubre, con distinto formato de prompt (texto vs. snippet de
código a clasificar), tanto las preguntas "concepto/entrevista" como
"clasificación de código" que pide la ADR (`docs/adrs/2026-07-20-content-structure-sections-checkpoints.md`,
sección 6). Lo único genuinamente nuevo que pide el roadmap para 2.2 es
**Parsons** (ordenar líneas de código). Se decidió (2026-07-23) ampliar
también el alcance a **`predict_output`** (predecir la salida de un snippet),
mencionado en el plan original (`PROJECT-OOPS.md` sección 7) aunque el
roadmap de Fase 2 no lo liste explícitamente en 2.2.

Los "escenarios prácticos con andamiaje variable" de la ADR **no requieren
motor nuevo**: el mismo problema práctico puede plantearse con distinto nivel
de andamiaje simplemente eligiendo el `type` — `fill_blank` (casi resuelto,
un hueco), `mcq` (elegir entre soluciones) o `parsons` (ordenar los pasos) son
ya tres niveles de andamiaje distintos sobre el mismo escenario. Es una
convención de autoría de contenido, no una feature de código.

## Alcance

**Incluido:**
- Tipo `parsons` — ordenar líneas de código, motor + UI + grading.
- Tipo `predict_output` — predecir la salida (potencialmente multilínea) de
  un snippet, motor + UI + grading.
- Contenido real mínimo: ~4 ejercicios `parsons` + ~4 `predict_output`
  agregados a los packs existentes (`java-fundamentals.json`, `streams.json`)
  para validar el motor end-to-end con contenido jugable (misma filosofía de
  "rebanada vertical" que 2.1).

**Explícitamente fuera de alcance:**
- Escritura libre de código desde cero (diferido, sin grading automático
  viable hasta tener compilador embebido — Fase 3+).
- Drag-and-drop para Parsons (se usa "tocar en orden" — ver Sección 3).
- Líneas distractoras en Parsons (todas las líneas provistas deben usarse;
  simplifica grading y autoría).
- Crédito parcial en grading (sigue el modelo binario correcto/incorrecto
  existente, alineado con la señal de calidad SM-2 actual de 5/2).
- Curación exhaustiva del resto de unidades/secciones — eso es Fase 2.3.

**Nota para Fase 2.3:** el orden actual de secciones (Streams primero,
Fundamentos segundo) es temporal — decisión explícita de la ADR
(`docs/adrs/2026-07-20-content-structure-sections-checkpoints.md`, sección 4)
tomada solo para tener una rebanada vertical jugable rápido en 2.1. El mapeo
definitivo de todas las secciones del libro de referencia queda pendiente;
cuando se haga en 2.3, Streams probablemente se reubique cerca de
Generics/Collections (en el libro, "Functional Programming" es el capítulo 4,
inmediatamente después de "Generics and Collections"). 2.2 **no** toca el
orden de secciones.

## Diseño

### 1. Modelo de datos

`ExerciseContent` (`domain/model/ExerciseContent.kt`) gana un campo opcional
nuevo. No requiere migración de Room — `Exercise.payload` es un blob JSON
serializado, opaco para el schema de la base de datos.

```kotlin
data class ExerciseContent(
    val id: String,
    val type: String,
    val difficulty: Int,
    val prompt: String,
    val code: String? = null,
    val answer: String,
    val distractors: List<String> = emptyList(),
    val lines: List<String> = emptyList(),   // nuevo — solo "parsons"
    val explanation: String
)
```

Convención por tipo (documentada, no forzada por el compilador — mismo
patrón que `code`/`distractors` hoy):

- **`fill_blank`** (existente) — sin cambios.
- **`mcq`** (existente) — sin cambios.
- **`parsons`** (nuevo) — `lines`: las líneas en el orden correcto (se
  mezclan en cliente, igual que `distractors + answer` para MCQ). `answer`:
  esas líneas unidas con `"\n"` — fuente de verdad para el grading. `code`:
  el snippet completo correcto, usado solo para el reveal post-respuesta (no
  se muestra antes de responder).
- **`predict_output`** (nuevo) — reutiliza `code` (snippet a leer, visible
  desde el inicio) y `answer` (salida esperada, unida con `"\n"` si es
  multilínea). No usa `lines`.

Se agrega un objeto de constantes de tipo (reemplaza el
`private const val MCQ_TYPE = "mcq"` hardcodeado en `ExerciseAnswerCard.kt`),
siguiendo el patrón ya usado por `UnitCompletionSource`:

```kotlin
object ExerciseType {
    const val MCQ = "mcq"
    const val FILL_BLANK = "fill_blank"
    const val PARSONS = "parsons"
    const val PREDICT_OUTPUT = "predict_output"
}
```

### 2. Grading compartido

Hoy `SessionViewModel`, `CheckpointViewModel` y `PlacementCheckpointViewModel`
repiten idéntica la misma línea:

```kotlin
val correct = userAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)
```

Con dos tipos nuevos que necesitan comparación distinta, triplicar la lógica
type-aware en los 3 ViewModels sería una violación real de DRY (no
cosmética). Se extrae una función pura, mismo patrón que
`computeCheckpointResult` (función top-level junto al use case relacionado,
sin clase/DI por no tener dependencias):

```kotlin
// domain/usecase/GradeExerciseAnswerUseCase.kt

fun gradeExerciseAnswer(exercise: ExerciseContent, userAnswer: String): Boolean =
    when (exercise.type) {
        ExerciseType.PARSONS -> gradeParsons(exercise, userAnswer)
        ExerciseType.PREDICT_OUTPUT -> gradePredictOutput(exercise, userAnswer)
        else -> userAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)
    }

private fun gradeParsons(exercise: ExerciseContent, userAnswer: String): Boolean =
    userAnswer.trim() == exercise.answer.trim()  // sensible a mayusculas: es codigo

private fun gradePredictOutput(exercise: ExerciseContent, userAnswer: String): Boolean =
    normalizeOutput(userAnswer) == normalizeOutput(exercise.answer)

private fun normalizeOutput(text: String): String =
    text.trim().lines().joinToString("\n") { it.trimEnd() }
```

Reglas de comparación:
- **Parsons**: exacto y sensible a mayúsculas (es código; `list` y `List` no
  son intercambiables).
- **predict_output**: sensible a mayúsculas (la salida real de Java lo es,
  ej. `true` vs `True`), tolerante a espacios finales por línea y a líneas en
  blanco sobrantes al inicio/final.
- `mcq`/`fill_blank`: sin cambio de comportamiento (`ignoreCase = true`).

Los 3 ViewModels cambian una sola línea cada uno:
`val correct = gradeExerciseAnswer(exercise, userAnswer)`.

### 3. UI (`ExerciseAnswerCard.kt`)

**Reveal previo a responder:** el bloque genérico
`exercise.code?.let { CodeBlock(...) }` debe **ocultarse para `parsons`
antes de responder** (si no, filtra la respuesta armada). De paso se corrige
`filledAnswer`, que hoy se calcula con `type != MCQ_TYPE` (funciona por
casualidad hoy, pero capturaría también `predict_output`) — pasa a ser
explícito: `type == FILL_BLANK`.

**Parsons — armado por toques** (decisión de producto, 2026-07-23: tocar en
orden, no drag-and-drop — reduce riesgo de bugs sutiles de gestos/animación
en Compose):
- Estado nuevo por ejercicio (mismo patrón que `mcqOptions`,
  `remember(exercise.id)`): `available: List<String>` (líneas mezcladas) y
  `built: List<String>` (secuencia armada).
- Tocar una línea disponible la mueve al final de `built`; tocar una línea ya
  puesta la devuelve a `available`.
- El área armada se renderiza reutilizando `CodeBlock(code =
  built.joinToString("\n"))` directamente — ya tiene el estilo monoespaciado
  correcto, no requiere componente nuevo.
- Las líneas disponibles se listan verticalmente como chips tocables (nuevo
  composable privado `ParsonsLineChip`, fuente monoespaciada; no se reutiliza
  `McqOptionButton` porque su máquina de estados CORRECT/INCORRECT no aplica
  aquí).
- COMPROBAR se habilita solo cuando `built.size == exercise.lines.size`. Al
  enviar: `onSubmit(built.joinToString("\n"))` — el contrato `(String) ->
  Unit` del ViewModel no cambia.
- Tras responder: se oculta el armador y aparece el `CodeBlock` genérico con
  `exercise.code` (snippet correcto completo) — mismo mecanismo de reveal que
  `fill_blank` ya usa. Sin diff visual de dónde se equivocó (fuera de
  alcance).

**predict_output — input multilínea:**
- El `OutlinedTextField` de texto libre pasa a `singleLine = false` (con
  `minLines` chico) solo para este tipo, con fuente monoespaciada para que
  combine visualmente con el snippet de arriba.

**`FeedbackBanner` — ajuste necesario:** hoy arma el título como
`"Incorrecto. Respuesta: $answer"` en una sola línea — se ve mal con
respuestas multilínea. Cambia a recibir el tipo de ejercicio:
- `parsons` incorrecto → título simple `"Incorrecto"` (la respuesta correcta
  ya se reveló arriba en el `CodeBlock`, no hace falta repetirla).
- `predict_output` incorrecto → título `"Incorrecto"` + bloque aparte "Salida
  esperada:" con el `answer` en monoespaciado (no hay reveal en otro lado
  para este tipo).
- `mcq`/`fill_blank` → sin cambios.

### 4. Contenido real (mínimo, rebanada vertical)

Se agregan ejercicios a los packs ya curados, sin tocar el loader (ya es
agnóstico al `type`).

`streams.json` — encaja naturalmente con Parsons (pipelines encadenados) y
predict_output:

```json
{
  "id": "streams-parsons-01",
  "type": "parsons",
  "difficulty": 2,
  "prompt": "Ordena las lineas para filtrar pares y sumarlos:",
  "lines": ["numeros.stream()", ".filter(n -> n % 2 == 0)", ".mapToInt(Integer::intValue)", ".sum()"],
  "answer": "numeros.stream()\n.filter(n -> n % 2 == 0)\n.mapToInt(Integer::intValue)\n.sum()",
  "code": "numeros.stream()\n.filter(n -> n % 2 == 0)\n.mapToInt(Integer::intValue)\n.sum()",
  "explanation": "stream() abre el pipeline, filter() selecciona pares, mapToInt() desempaqueta y sum() reduce a un total."
}
```

```json
{
  "id": "streams-predict-01",
  "type": "predict_output",
  "difficulty": 2,
  "prompt": "Que imprime este codigo?",
  "code": "Stream.of(\"a\", \"b\", \"c\")\n    .limit(2)\n    .forEach(System.out::println);",
  "answer": "a\nb",
  "explanation": "limit(2) trunca el stream a los primeros 2 elementos antes de forEach."
}
```

`java-fundamentals.json` — Parsons para estructura de programa (unidad
`fund-types-and-main`), predict_output para lectura básica (unidades
`fund-what-is-java`/`fund-class-structure`). Mismo formato, adaptado al
contenido de esas unidades.

Meta: **~4 ejercicios Parsons + ~4 predict_output**, repartidos entre ambos
packs — suficiente para validar el motor jugando contenido real, no
curación exhaustiva (eso es Fase 2.3).

### 5. Testing

Sin tests de ViewModel/Compose en el proyecto hoy (patrón establecido: solo
tests de dominio con fakes + QA manual en dispositivo para UI) — se
mantiene:

- **`GradeExerciseAnswerUseCaseTest.kt`** (nuevo) — pieza central testeable:
  parsons exacto / con espacios / orden incorrecto; predict_output con
  mayúsculas distintas (debe fallar), espacios finales de línea (debe
  pasar), líneas en blanco sobrantes al inicio/final (debe pasar); regresión
  de `mcq`/`fill_blank` sin cambio de comportamiento.
- Los 3 ViewModels no llevan test nuevo (no lo tienen hoy) — su único cambio
  es delegar a la función ya testeada.
- **QA manual en dispositivo** (SM-A505G, igual que 2.1/2.1b): jugar un
  ejercicio Parsons completo (armar bien, armar mal, deshacer un toque), un
  predict_output (multilínea, mayúsculas correctas e incorrectas), y
  confirmar que teclado/scroll siguen funcionando bien con el input
  multilínea (relevante por el bug de teclado ya arreglado en 2.1).

## Decisiones registradas

| Decisión | Elegido | Fecha |
|---|---|---|
| Alcance de tipos nuevos | Parsons + predict_output (no solo Parsons) | 2026-07-23 |
| Interacción Parsons | Tocar en orden (no drag-and-drop) | 2026-07-23 |
| Curación de contenido en 2.2 | Motor + contenido real mínimo (no solo motor) | 2026-07-23 |
| Orden de secciones (Streams vs Fundamentos) | Sin cambios en 2.2, remapeo completo diferido a 2.3 | 2026-07-23 |
