# ADR: Estrategia de testing de regresión — ViewModels y smoke tests de Compose

**Estado:** Aceptado

## Contexto

La Fase 2.3 (`docs/superpowers/specs/2026-07-24-fase2-3-content-scaling-design.md`)
verificó por inspección de código que agregar una sección de contenido nueva
no requiere ningún cambio de motor ni de UI — es JSON + registro en
`ContentSeeder`. Esto significa que **el riesgo real de regresión no viene
del contenido, viene de cambios al motor o a la UI** (ej. los tipos de
ejercicio nuevos de la Fase 2.2, o cambios en `ExerciseAnswerCard`).

Hoy, sin embargo, la única forma de verificar que el motor/UI siguen
funcionando después de un cambio es el QA manual en dispositivo de Luis.
El proyecto tiene:

- Tests JUnit4 de dominio/use cases (Kotlin puro, con fakes) — buena
  cobertura ahí.
- Un test instrumentado real (`MigrationTest.kt`, migraciones de Room).
- `ExampleInstrumentedTest.kt` — boilerplate del template de Android
  Studio, no es un test real.
- **Cero tests de ViewModel** (`SessionViewModel`, `CheckpointViewModel`,
  `PlacementCheckpointViewModel` — ya notado como hueco durante la Fase 2.2).
- **Cero tests de Compose UI.**
- **Sin CI/CD** (no existe `.github/workflows` ni pipeline alguno).

El detonante de esta ADR: al cerrar la Fase 2.3, Luis preguntó si existían
pruebas de humo (smoke tests), motivado por que el QA manual completo se
vuelve cada vez más largo a medida que el roadmap de contenido avanza (9
secciones más planificadas). Se decide agregar cobertura automatizada,
pero apuntada al riesgo real (motor/UI), no a revalidar contenido.

## Decisión

Se adoptan **dos niveles de inversión, secuenciales** (Nivel 1 primero, es
barato y cierra un hueco ya identificado; Nivel 2 después, tiene costo de
setup real):

### Nivel 1 — Tests de ViewModel (JVM puro, con fakes)

Cobertura para `SessionViewModel`, `CheckpointViewModel` y
`PlacementCheckpointViewModel`, siguiendo el mismo patrón ya usado en toda
la capa de dominio de este proyecto: fakes escritos a mano (no librería de
mocking), `kotlinx-coroutines-test`'s `runTest`. Corren como parte de
`testDebugUnitTest`, sin emulador ni dispositivo.

Objetivo: verificar la lógica de estado/grading/navegación de sesión
(¿se llama bien `gradeExerciseAnswer`?, ¿se actualiza bien el `UiState` al
responder?, ¿se completa bien un checkpoint y se dispara SM-2/streak
correctamente?) de forma aislada de Compose.

### Nivel 2 — Smoke tests instrumentados de Compose (flujo mínimo end-to-end)

Un puñado de tests que lanzan la app real y recorren un flujo mínimo
representativo: Home → Ruta → jugar un ejercicio de cada uno de los 4 tipos
(`mcq`, `fill_blank`, `parsons`, `predict_output`) → completar un
checkpoint. **Explícitamente sin exhaustividad de contenido** — usan datos
de prueba controlados, no el contenido real de las secciones, para que
agregar una sección/unidad nueva nunca obligue a tocar estos tests.

Requiere agregar infraestructura de testing con Hilt que hoy no existe:
la dependencia `hilt-android-testing` y un test runner custom
(`HiltTestRunner`, para poder inyectar fakes/una DB en memoria en un test
instrumentado). Las dependencias de Compose Testing
(`androidx.compose.ui.test.junit4`, `androidx.compose.ui.test.manifest`)
ya están en `app/build.gradle.kts` (puestas por el template de Android
Studio), sin usar hasta ahora.

## Consecuencias

**Positivas:**
- Reduce el volumen de QA manual necesario en cada ronda de cambios de
  motor/UI — el objetivo es que Luis no tenga que rejugar toda la app cada
  vez que se agrega una sección de contenido.
- El Nivel 1 es barato (sin infraestructura nueva) y cierra un hueco de
  cobertura ya identificado en la Fase 2.2.
- Ninguno de los dos niveles depende del contenido real de las secciones,
  así que agregar secciones/unidades nuevas (el trabajo recurrente de la
  Fase 2.3 en adelante) nunca requiere mantener estos tests.

**Trade-offs:**
- El Nivel 2 tiene costo de setup real: hoy no existe infraestructura de
  testing con Hilt en el proyecto.
- **Ninguno de los dos niveles reemplaza el QA manual completo.** En
  particular, no detectan regresiones puramente visuales (ej. el bug de
  contraste de color en los chips de Parsons encontrado en la Fase 2.2 QA)
  — eso requeriría además una herramienta de regresión visual
  (Paparazzi/Roborazzi) o aserciones de contraste sobre el árbol de
  semántica de Compose, que queda fuera de alcance de esta decisión.

## Pendiente (fuera de alcance de esta ADR)

- Diseño de implementación de ambos niveles (qué casos exactos cubre cada
  ViewModel test, qué flujo exacto arma el smoke test, cómo se inyectan
  fakes/DB en memoria con Hilt en el test instrumentado) — es trabajo de
  diseño de implementación separado (brainstorming → spec → plan), no
  decidido en esta ADR.
- Herramienta de regresión visual (screenshot testing) — explícitamente
  fuera de alcance; se evalúa por separado si en el futuro se necesita.
- Integración en CI — hoy no existe pipeline; fuera de alcance de esta ADR.
