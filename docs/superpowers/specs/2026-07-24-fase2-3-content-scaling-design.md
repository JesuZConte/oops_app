# Fase 2.3 — Escalado de contenido — Diseño

**Estado:** Aprobado, pendiente de plan de implementación.

## Contexto

El roadmap de Fase 2 (`docs/specs/2026-07-20-fase2-content-roadmap.md`)
describe 2.3 como: *"Mapear y curar el resto de secciones (capítulos 2-11
del libro + features Java 21 de examen + extras 22-25 etiquetados como
'moderno'), incrementalmente, una sección a la vez. Meta arquitectónica:
agregar una sección = un JSON pack nuevo + registrar la sección, sin tocar
el motor."*

Esto son en realidad dos trabajos de naturaleza distinta:

1. **Una decisión arquitectónica única y liviana**: el mapeo completo y
   definitivo de las secciones que faltan (qué capítulo del libro va en qué
   sección, en qué orden, qué es "de examen" vs. "extra moderno"). Es una
   decisión de producto/contenido, no código.
2. **Curación de contenido real, sección por sección** — se repite ~9-10
   veces, no cabe en un solo ciclo de spec→plan→implementación (mismo
   motivo por el que la Fase 2 completa se descompuso en 2.1/2.2/2.3/2.4).

Esta spec resuelve (1) por completo y hace (2) para **una sola sección**
(Genéricos y Colecciones), como rebanada vertical de este ciclo — mismo
enfoque de riesgo usado en 2.1 (Fundamentos) y 2.2 (contenido mínimo real
para validar los tipos nuevos).

**Verificación de la meta arquitectónica (2026-07-24):** se confirmó por
inspección de código que agregar una sección nueva hoy es efectivamente
cero-código: `ContentSeeder.kt` itera `packAssetPaths: List<String>` sin
ninguna rama por-paquete; `GetLearningPathUseCase` construye la Ruta
ordenando `getSections()` por `orderIndex` sin IDs ni conteos
hardcodeados; `GetCheckpointSessionUseCase`/`GetPlacementCheckpointSessionUseCase`
muestrean genéricamente sobre cualquier número de secciones/unidades. No
se encontró ninguna excepción a esto en `ui/` ni en el resto del código.
Ningún cambio de schema Room es necesario (`SectionEntity`/`UnitEntity`/
`ExerciseEntity` usan IDs de tipo `String` + `orderIndex`, sin restricción
de cardinalidad).

## 1. Mapeo completo de secciones (decisión de una sola vez)

Basado en el índice del libro de referencia (ADR
`docs/adrs/2026-07-20-content-structure-sections-checkpoints.md`) y los
dominios reales del examen 1Z0-830 (`docs/specs/PROJECT-OOPS.md` sección 8),
priorizando lo que realmente entra al examen por sobre una transcripción
literal del libro (edición Java 11, no coincide 1:1 con el temario Java 21
del 1Z0-830):

| # | Sección | Origen | Estado |
|---|---|---|---|
| 1 | Fundamentos de Java | libro cap. 1 | **Hecha** (Fase 2.1) |
| 2 | Genéricos y Colecciones | libro cap. 3 | **Este ciclo** |
| 3 | Streams y lambdas | libro cap. 4 ("Functional Programming") | Hecha (Fase 2.1), **reubicada** de posición 2 a 3 en este ciclo |
| 4 | Manejo de Excepciones | libro cap. 5 (sin Assertions/Localization — bajo valor para el examen) | Futuro |
| 5 | Concurrencia | libro cap. 7 (incluye virtual threads, Java 21) | Futuro |
| 6 | Módulos | libro cap. 6 | Futuro |
| 7 | I/O + NIO.2 | libro caps. 8+9, fusionados (ambos son "manejo de archivos") | Futuro |
| 8 | JDBC | libro cap. 10 | Futuro |
| 9 | Features de Java 21 (de examen) | records, sealed classes, pattern matching, text blocks | Futuro |
| 10 | Extra Moderno (Java 22-25) | unnamed variables/patterns, stream gatherers, scoped values, structured concurrency — etiquetado explícitamente como no-examen en la UI | Futuro |
| 11 | Annotations | libro cap. 2 | Futuro, baja prioridad |
| 12 | Security | libro cap. 11 | Futuro, baja prioridad |

Decisión (2026-07-24): Annotations y Security no están en los dominios
reales del 1Z0-830 ni en la visión de producto (`PROJECT-OOPS.md` sección 1)
— se agregan al mapeo como secciones 11 y 12, pero se curan al final,
después de las 10 principales.

**Este mapeo es la referencia para futuros ciclos de 2.3** — cada ciclo
futuro toma la siguiente sección "Futuro" de esta tabla, sin necesitar
rediseñar el orden.

### Reubicación de Streams (orderIndex 2 → 3)

Cambio mecánico: `streams.json`'s `orderIndex` pasa de `2` a `3`;
`generics-collections.json` (nuevo) toma `orderIndex: 2`. No afecta
progreso guardado — el progreso se persiste por `unitId`/`exerciseId`, no
por posición; solo cambia el orden en que Ruta muestra las secciones.

## 2. Contenido de este ciclo: Genéricos y Colecciones

### Unidades (4)

1. **Generics** — tipos parametrizados, clases/métodos genéricos, bounded
   types (`<T extends X>`), wildcards (`? extends`, `? super`).
2. **Listas y Sets** — `List`/`Set`, implementaciones (`ArrayList` vs
   `LinkedList`, `HashSet` vs `LinkedHashSet` vs `TreeSet`).
3. **Maps y Deques** — `Map`/`Deque`, implementaciones (`HashMap`,
   `TreeMap`, `LinkedHashMap`, `ArrayDeque`).
4. **Comparadores y colecciones inmutables** — `Comparable` vs
   `Comparator`, ordenar colecciones, `List.of`/`Set.of`/`Map.of` (Java 9+).

### Mezcla de tipos de ejercicio (guía de autoría, aplica a este ciclo y a los futuros)

Cada unidad mezcla **tres sabores** de pregunta, no solo examen/sintaxis
(corrección hecha en esta sesión de brainstorming — el diseño original de
la ADR sección 6 ya pedía esto, pero la propuesta inicial de contenido se
había inclinado demasiado hacia examen):

- **Examen/sintaxis** (`fill_blank`, `parsons`, `predict_output`) — sintaxis
  exacta, armar pipelines, y predecir resultados donde el comportamiento es
  la sorpresa real del examen (ej. orden de iteración de `HashSet` vs
  `LinkedHashSet` vs `TreeSet`, o de un `HashMap`).
- **Clasificación de código** (`mcq` sobre un snippet) — "¿qué hace este
  bloque?", igual que ya se usa en Fundamentos para getter/setter/constructor.
- **Entrevista/criterio** (`mcq` de tipo "por qué/cuándo/qué problema
  resuelve") — prueba razonamiento y criterio práctico, no memoria de
  sintaxis. Ejemplos concretos para esta sección:
  - "¿Por qué usarías Generics en vez de simplemente tipar todo como `Object`?"
  - "¿Qué problema práctico resuelve un `Deque` que una `List` no resuelve tan bien?"
  - "Te piden elegir entre `HashMap` y `TreeMap` para cachear resultados por clave — ¿cuál eliges y por qué?"
  - "¿Cuándo preferirías `LinkedList` sobre `ArrayList` en un caso real?"

### Volumen

~5-6 ejercicios por unidad (~20-24 en total) — similar a Fundamentos (17) y
Streams (19).

### Checkpoints

Sin trabajo adicional — el checkpoint voluntario y el examen de ubicación
ya son genéricos sobre cualquier número de secciones (verificado en la
sección de Contexto arriba). Agregar la sección nueva simplemente la hace
elegible como "sección actual" o "sección anterior" en el muestreo de
checkpoint, sin cambios de código.

## Alcance

**Incluido en este ciclo:**
- El mapeo completo de las 12 secciones (tabla de arriba), como decisión
  documentada de una sola vez.
- Reubicación de Streams (`orderIndex` 2 → 3).
- Contenido real para **Genéricos y Colecciones** (4 unidades, ~20-24
  ejercicios, mezcla de los 3 sabores de pregunta), registrado como nueva
  sección `orderIndex: 2`.

**Explícitamente fuera de alcance:**
- Curación de las 8 secciones "Futuro" restantes de la tabla — quedan
  mapeadas pero sin curar, para ciclos futuros que repiten este mismo
  proceso liviano (no requieren nueva sesión de brainstorming/spec, solo
  un plan de contenido puntual).
- Cualquier cambio de motor/UI — ya verificado que no hace falta.
- Annotations y Security — quedan al final de la tabla, sin fecha.

## Testing

Mismo patrón que 2.2: sin tests de contenido dedicados más allá de los
tests genéricos de parsing (`ContentPackParsingTest`) — validación de JSON
bien formado (`python3 -m json.tool`) más la suite completa de tests
unitarios como regresión, y QA manual en dispositivo (SM-A505G) jugando la
sección nueva completa, incluyendo su checkpoint de fin de sección y
probando que aparezca correctamente en el examen de ubicación si se salta.

## Decisiones registradas

| Decisión | Elegido | Fecha |
|---|---|---|
| Alcance del ciclo | Mapeo completo (12 secciones) + curar solo 1 sección nueva | 2026-07-24 |
| Reubicar Streams ahora | Sí, `orderIndex` 2 → 3 | 2026-07-24 |
| Annotations/Security | Incluidas en el mapeo, baja prioridad, al final | 2026-07-24 |
| Próxima sección a curar | Genéricos y Colecciones | 2026-07-24 |
| Mezcla de tipos de pregunta | 3 sabores: examen/sintaxis, clasificación de código, entrevista/criterio | 2026-07-24 |
