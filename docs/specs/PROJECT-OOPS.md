# Oops! — App de estudio diario de Java (Android)

> El nombre juega con dos sentidos: **OOP** (Object-Oriented Programming, el
> paradigma central de Java) y **oops** (el momento de fallar un ejercicio y
> que el sistema te lo vuelva a agendar mañana — la mecánica misma del SRS).

> Fuente de verdad del proyecto. Este documento describe la visión, la
> arquitectura y las tareas. Está pensado para orquestar el desarrollo con
> Claude Code: cada tarea de la sección "Plan de construcción" es un prompt
> autocontenido que puede ejecutarse en orden.

---

## 1. Visión

Una app estilo "Duolingo de Java": sesiones cortas y diarias basadas en
repetición espaciada que entrenan la **recuperación desde memoria** de la API de
Java (métodos de Streams, Collections, SQL/JDBC, etc.) sin depender del IDE.

El objetivo no es solo pasar entrevistas, sino avanzar hacia un dominio real de
Java, con una meta final concreta: preparar la certificación **Oracle Certified
Professional: Java SE 21 Developer (examen 1Z0-830)**. Ese examen se rinde sin
IDE, sin compilador y sin documentación, así que entrenar el recuerdo activo es
exactamente la habilidad que valida.

Filosofía de producto: empezar simple y sumar en el tiempo. No se busca una app
que "lo sepa todo" desde el día uno.

---

## 2. Objetivos y no-objetivos

### Objetivos (v1 / Fase 1)
- Sesiones diarias con repetición espaciada (SM-2).
- Un dominio completo del temario como primer contenido: **Streams y lambdas**.
- Racha, XP y persistencia local. Todo funciona offline.
- Progreso agrupado por objetivo del examen (cert readiness).

### No-objetivos (por ahora, explícitamente diferidos)
- Ejecución real de código / compilador embebido → Fase 3.
- Backend y sincronización entre dispositivos → Fase 3.
- iOS → fuera de alcance. Solo Android.
- Generación de ejercicios con IA → Fase 3.

---

## 3. Stack y restricciones

- **Plataforma:** Android únicamente.
- **Lenguaje:** Kotlin.
- **UI:** Jetpack Compose + Navigation Compose.
- **Persistencia:** Room (SQLite local). Arquitectura *local-first*, sin red.
- **Inyección de dependencias:** Hilt.
- **Serialización:** kotlinx.serialization (para los packs de contenido JSON).
- **Tests:** JUnit para el motor SRS y los use cases (Kotlin puro).
- **Package base:** `com.zconte.oopsapp` (ya generado por Android Studio; el proyecto se creó con este paquete).

Restricción de diseño clave: el motor SRS y los use cases son **Kotlin puro sin
dependencias de Android**, para que sean 100% testeables sin emulador.

---

## 4. Arquitectura

MVVM con separación por capas dentro de un único módulo `app`.

```
app/src/main/java/com/zconte/oopsapp/
├── data/
│   ├── local/          # Room: entities, DAOs, AppDatabase
│   ├── content/        # loader de JSON desde assets/content/
│   └── repository/     # ExerciseRepository, ProgressRepository
├── domain/
│   ├── model/          # modelos limpios: Exercise, ReviewState, Topic
│   ├── srs/            # SchedulerSm2  ← Kotlin puro, testeable
│   └── usecase/        # GetTodaySession, SubmitAnswer, UpdateStreak
├── ui/
│   ├── home/           # racha + botón "estudiar hoy"
│   ├── session/        # pantalla de ejercicio
│   ├── progress/       # dominio por objetivo del examen
│   └── theme/
└── di/                 # módulos Hilt
```

Flujo del bucle diario (vive en la capa domain):
`agenda (GetTodaySession) → muestra ejercicio → evalúa (SubmitAnswer) →
actualiza intervalo (SchedulerSm2) → reagenda`.

---

## 5. Modelo de datos (Room)

Decisión de diseño: cada ejercicio guarda su contenido como blob JSON
(`payload`), no como columnas fijas. Así los distintos tipos de ejercicio
conviven sin migrar el esquema.

```kotlin
@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val name: String,
    val certObjective: String,   // p.ej. "streams-lambdas"
    val orderIndex: Int
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val type: String,            // "fill_blank" | "mcq" | "parsons" | ...
    val payload: String,         // JSON: prompt, opciones, respuesta, explicación
    val difficulty: Int          // 1..5
)

@Entity(tableName = "review_state")
data class ReviewStateEntity(
    @PrimaryKey val exerciseId: String,
    val easeFactor: Double,      // arranca en 2.5
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: Long            // epoch day
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 0,
    val streak: Int,
    val xp: Int,
    val lastStudyDate: Long
)
```

---

## 6. Motor de repetición espaciada (SM-2)

Objeto Kotlin puro. `quality` va de 0 a 5 (qué tan bien se recordó); menor a 3
cuenta como fallo.

```kotlin
object SchedulerSm2 {
    fun review(state: ReviewState, quality: Int, today: LocalDate): ReviewState {
        // El easeFactor se recalcula siempre, acierto o fallo (SM-2 canónico).
        val newEase = (state.easeFactor +
            (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))
            .coerceAtLeast(1.3)

        // Fallaste: reinicia, se vuelve a ver mañana
        if (quality < 3) {
            return state.copy(easeFactor = newEase, repetitions = 0,
                              intervalDays = 1, dueDate = today.plusDays(1))
        }
        val reps = state.repetitions + 1
        val interval = when (reps) {
            1 -> 1
            2 -> 6
            // Usa el easeFactor ANTERIOR a este repaso, no el recién calculado.
            else -> Math.round(state.intervalDays * state.easeFactor).toInt()
        }
        return state.copy(
            easeFactor = newEase, repetitions = reps,
            intervalDays = interval, dueDate = today.plusDays(interval.toLong())
        )
    }
}
```

`GetTodaySession` consulta ejercicios con `dueDate <= hoy`, agrega N ejercicios
nuevos (nunca vistos) y arma la sesión del día.

---

## 7. Formato de contenido (packs JSON)

Los ejercicios viven en `assets/content/<topic>.json`. Agregar un nuevo dominio
(o más adelante otro lenguaje) es agregar un archivo, no tocar la app.

Ejemplo `assets/content/streams.json`:

```json
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
      "explanation": "collect() es una operación terminal que acumula elementos."
    }
  ]
}
```

Tipos de ejercicio previstos (implementar de a poco):
- `fill_blank` — escribir el método que falta.
- `mcq` — elegir el método correcto entre distractores.
- `predict_output` — predecir la salida de un snippet.
- `parsons` — ordenar líneas para formar un pipeline (Fase 2+).

---

## 8. Árbol de habilidades = temario del 1Z0-830

Cada `Topic` mapea a un objetivo real del examen mediante `certObjective`. El
progreso se agrupa por estos objetivos para mostrar *readiness* por dominio.

Dominios de referencia (examen 1Z0-830, Java SE 21):
- Fundamentos del lenguaje y OOP
- Genéricos y colecciones
- **Streams y lambdas**  ← primer contenido a construir
- Manejo de excepciones
- Concurrencia (incluye virtual threads)
- Módulos
- JDBC y NIO.2 (I/O de archivos)
- Features de Java 21: records, sealed classes, pattern matching, text blocks

Feature de graduación (Fase 2/3): simulacro adaptativo estilo Duolingo Test —
50 preguntas, umbral de aprobación 68%, presentado sin ayudas, que estima tu
readiness real para rendir la certificación.

---

## 9. Plan de construcción (tareas para Claude Code)

Ejecutar en orden. Cada tarea deja algo funcionando o testeable.

**Tarea 1 — Bootstrap del proyecto.**
Crear el proyecto Android con Kotlin, Jetpack Compose, Room, Hilt y Navigation
Compose configurados. Definir la estructura de paquetes de la sección 4.
Crear tres pantallas vacías (Home, Session, Progress) con navegación entre
ellas. Sin lógica todavía.

**Tarea 2 — Motor SRS (domain/srs).**
Implementar `SchedulerSm2` (sección 6) como Kotlin puro y sus modelos de dominio
(`ReviewState`). Escribir tests unitarios que cubran: primer repaso, segundo
repaso, fallo (quality < 3), crecimiento del intervalo y piso del easeFactor en
1.3. Sin dependencias de Android.

**Tarea 3 — Capa de datos (data/local + data/content).**
Definir las entities, los DAOs y `AppDatabase` (sección 5). Implementar el loader
que parsea `assets/content/streams.json` con kotlinx.serialization y siembra la
base la primera vez. Incluir 20 ejercicios semilla de Streams en el JSON.

**Tarea 4 — Use cases (domain/usecase).**
Implementar `GetTodaySession` (ejercicios vencidos + nuevos), `SubmitAnswer`
(evalúa respuesta, llama a `SchedulerSm2`, persiste el nuevo `ReviewState`) y
`UpdateStreak`. Tests unitarios de la lógica de sesión.

**Tarea 5 — Pantalla de sesión (ui/session).**
Componer la UI que muestra un ejercicio, recibe la respuesta, muestra si fue
correcta con la explicación, y avanza al siguiente. Conectar con los use cases
vía un ViewModel.

**Tarea 6 — Home con racha y XP (ui/home).**
Pantalla de inicio con la racha actual, XP y un botón "estudiar hoy" que lanza la
sesión. Actualizar racha/XP al terminar una sesión.

**Tarea 7 — Progreso (ui/progress).**
Pantalla que agrupa el dominio de los ejercicios por `certObjective` y muestra el
porcentaje de readiness por objetivo del examen.

Al terminar la Tarea 7 se cierra la Fase 1: una app usable a diario, con Streams
como primer dominio del 1Z0-830.

---

## 10. Convenciones

- Kotlin idiomático; funciones puras en domain, sin efectos secundarios ocultos.
- El motor SRS y los use cases no importan nada de `android.*`.
- Un ViewModel por pantalla; el estado de UI como `StateFlow` inmutable.
- Nombres de identificadores en inglés; contenido de ejercicios en el idioma que
  prefieras.
- Cada tarea del plan debe incluir sus tests antes de darse por terminada
  (cuando aplique lógica de dominio).

---

## 11. Roadmap

- **Fase 0 — Prototipo:** formato de ejercicio + una pantalla que lo muestre.
- **Fase 1 — MVP diario:** SM-2, racha/XP, persistencia, dominio Streams. *(este documento)*
- **Fase 2 — Contenido y pulido:** SQL/JDBC como segundo dominio, más tipos de
  ejercicio (Parsons, predict_output), estadísticas de áreas débiles,
  notificación diaria.
- **Fase 3 — Avanzado:** backend + sync, ejecución real de código (sandbox),
  simulacro adaptativo de certificación, generación de ejercicios con IA.
