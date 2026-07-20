# ADR: Estructura de contenido — Secciones, Unidades y Checkpoints

**Estado:** Aceptado

## Contexto

El roadmap actual (`docs/specs/PROJECT-OOPS.md`, sección 11) describe la Fase 2 de contenido en una sola línea: "SQL/JDBC como segundo dominio, más tipos de ejercicio (Parsons, predict_output), estadísticas de áreas débiles, notificación diaria." Esto tiene dos problemas:

1. **No hay alcance ni orden reales.** La única fuente del orden de dominios hoy es la pantalla Ruta ya construida (Streams → Collections → SQL/JDBC), que ni siquiera coincide con lo que dice el roadmap ("SQL/JDBC como segundo dominio"). No hay un criterio detrás de ese orden — se generó ad-hoc en Fase 1/2.
2. **El modelo de progreso es plano.** Cada `Topic` (dominio) es una unidad monolítica de ejercicios sin sub-estructura, y el desbloqueo entre dominios es un único umbral de readiness agregado (60%). No hay forma de mezclar tipos de pregunta desde el principio, ni de reforzar contenido de dominios anteriores más allá de lo que ya hace el motor SM-2 de forma automática e implícita.

El usuario tiene el libro **"OCP [Oracle Certified Professional] Complete Study Guide"** (edición Java 11) y quiere usar su tabla de contenidos como referencia de alcance temático — sin copiar su orden de forma literal, y sin perder de vista que la app apunta específicamente al examen **1Z0-830 (Java SE 21)**.

Índice de referencia del libro (Java 11):

1. Java Fundamentals
2. Annotations
3. Generics and Collections
4. Functional Programming
5. Exceptions, Assertions, and Localization
6. Modular Applications
7. Concurrency
8. I/O
9. NIO.2
10. JDBC
11. Security

La visión de producto (`PROJECT-OOPS.md`, sección 1) explícitamente busca ir más allá de solo aprobar el examen: "avanzar hacia un dominio real de Java." Esto motiva incluir features de versiones posteriores a Java 21, no solo lo que entra en el 1Z0-830.

## Decisión

### 1. Jerarquía de contenido: Sección → Unidad → Checkpoint

Se adopta el modelo de progresión de Duolingo en vez del modelo plano actual (`Topic` == dominio único):

- **Sección**: un tema amplio (ej. "Fundamentos de Java", "Streams y lambdas"). Puede mapear a uno o varios capítulos del libro de referencia.
- **Unidad**: dentro de una Sección, un mini-juego enfocado en una porción concreta del tema (ej., dentro de "Fundamentos": "¿Qué es Java? JDK/JRE/JVM", "Clases y objetos", "Fields, methods, constructors"). Cada unidad mezcla tipos de ejercicio desde el principio — no se gatea por tipo de pregunta, se gatea por tema.
- **Checkpoint**: cuestionario acumulativo que mezcla ejercicios de la Sección recién completada con una muestra de Secciones anteriores — igual que un checkpoint de fin de sección en Duolingo, y análogo a cómo una entrevista de trabajo real mezcla preguntas básicas con preguntas del tema del momento.

### 2. El Checkpoint tiene dos entradas, un solo mecanismo

- **Repaso voluntario** (fin de Sección): se ofrece al terminar todas las unidades de una sección. **No bloquea el avance** — el jugador puede seguir a la siguiente sección sin tomarlo. Es una herramienta de refuerzo, complementaria al resurfacing automático que ya hace el motor SM-2 en la sesión diaria.
- **Examen de ubicación** (saltar contenido): si el jugador toca una sección o unidad que todavía no alcanzó en orden, se le presenta el checkpoint de todo lo que se está saltando **antes** de dejarlo entrar. Si lo aprueba, se marcan como completadas todas las unidades salteadas y se desbloquea el destino. Si no lo aprueba, no se otorga el salto y debe seguir el camino normal.

**Umbral de aprobación: 68%** para ambos casos — el mismo umbral real del examen 1Z0-830 (50 preguntas, 68% para aprobar, confirmado en `education.oracle.com` y reportes de quienes lo rindieron), para que el jugador se acostumbre al estándar real del examen desde el día uno, no a un número arbitrario.

### 3. Streams y lambdas se reorganiza retroactivamente

El dominio ya construido (20 ejercicios semilla, hoy un solo bloque plano) se re-empaqueta en el nuevo modelo: se subdivide en Unidades (ej. creación de streams, operaciones intermedias, operaciones terminales, lambdas y method references) y gana su propio Checkpoint de sección. No se descarta contenido existente, se reestructura.

### 4. Fundamentos de Java pasa a ser la Sección 1

Streams dejó de ser el primer dominio por decisión de producto en Fase 1 (para tener algo jugable rápido), pero conceptualmente no es el punto de entrada correcto. La Sección 1 real pasa a ser **Fundamentos de Java** (qué es la JDK/JRE/JVM, estructura de clases, fields/methods/constructors, etc. — capítulo 1 del libro de referencia), y Streams se reubica en el orden que le corresponda una vez mapeadas todas las secciones (probablemente cerca de Generics/Collections, dado que en el libro "Functional Programming" es el capítulo 4, inmediatamente después de "Generics and Collections").

**El mapeo completo y definitivo de todas las Secciones (los 11 capítulos del libro + features modernas) queda fuera del alcance de esta ADR** — es un trabajo de curación de contenido separado, no una decisión de arquitectura. Ver "Pendiente" más abajo.

### 5. Alcance de versión: examen (Java 21) vs. extra moderno (hasta Java 25 LTS)

El 1Z0-830 evalúa específicamente Java 21. Java 25 es la LTS actual y con soporte más reciente (lanzada sept. 2025, soporte hasta sept. 2028; Java 26 ya existe pero es non-LTS de soporte corto, no es un buen target de "estable y con soporte"). Se decide:

- Cubrir features de Java 21 como contenido **de examen** (records, sealed classes, pattern matching, text blocks, virtual threads, etc.).
- Cubrir features introducidas entre Java 22 y 25 LTS como contenido **extra moderno**, explícitamente etiquetado como tal en la UI (ej. unnamed variables/patterns, stream gatherers, scoped values, structured concurrency) — para que el jugador sepa qué es examinable en el 1Z0-830 y qué es "estar al día" más allá del examen.
- No se persigue el último non-LTS (hoy Java 26) como target de contenido — cambia cada 6 meses y quedaría desactualizado rápido.

### 6. Diversidad de tipos de ejercicio desde el principio

No se gatea la diversidad de tipos de pregunta por nivel de dificultad — desde la Sección 1 se mezclan: preguntas conceptuales estilo entrevista ("¿qué es una clase abstracta?"), preguntas de lectura de código ("¿esto es un getter, un setter o un constructor?"), y preguntas factuales ("¿qué es la JDK?"), además del `fill_blank` ya implementado. Estas variantes probablemente no requieren nuevos valores de `type` en el modelo de datos — se modelan como variantes de `mcq` (ya previsto en `PROJECT-OOPS.md` sección 7) con distinto formato de prompt (texto vs. snippet de código a clasificar), a confirmar en el diseño de implementación.

Además de la mezcla teórico/lectura de código de arriba, dentro de cada Unidad se mezclan también ejercicios **teóricos** (concepto, definición, lectura) con ejercicios **prácticos** (resolver un problema concreto). Un ejercicio práctico plantea un escenario real, no solo sintaxis aislada — ej., en la unidad de Streams: "tienes una lista de manzanas y necesitas dos listas separadas, una de las rojas y otra de las verdes: ¿cómo lo resuelves?". El mismo escenario práctico puede tener distintos niveles de andamiaje (a definir en el diseño de implementación, no aquí): solución casi completa con un hueco a rellenar, selección entre opciones de solución, o escritura libre desde cero — variando la dificultad sin cambiar el problema de fondo.

## Consecuencias

**Positivas:**
- El alcance y el orden de contenido dejan de ser ad-hoc y pasan a tener una fuente de referencia real (el libro) más los objetivos oficiales del 1Z0-830.
- El checkpoint acumulativo ataca directamente el riesgo de "se olvida lo aprendido" de una progresión puramente lineal, sin duplicar lo que ya hace el motor SM-2 — son complementarios, no redundantes.
- Un jugador que ya sabe una sección no queda forzado a repetir contenido que domina (examen de ubicación), lo cual es importante para el público real de la app (gente que ya sabe Java y estudia para certificarse, no solo principiantes).
- Separar "examen" de "extra moderno" evita que el jugador se confunda sobre qué necesita saber específicamente para el 1Z0-830.

**Trade-offs:**
- Cambio de modelo de datos no trivial: hoy `Topic` es plano (un dominio = una fila, `certObjective` como único agrupador). Se necesita una capa de agrupación nueva (Sección) sobre las Unidades, más un tipo de sesión nuevo para el Checkpoint (distinto de `GetTodaySession`, que hoy solo mezcla vencidos + nuevos del SM-2). Diseño de implementación pendiente.
- Reestructurar Streams retroactivamente y curar el contenido de Fundamentos como nueva Sección 1 es trabajo de contenido no trivial, más allá de lo que el roadmap original ("SQL/JDBC como segundo dominio") anticipaba.
- El examen de ubicación es una aproximación — pasar un checkpoint de N preguntas no garantiza dominio real de toda una sección, igual que el propio 1Z0-830 aproxima el dominio del temario completo con 50 preguntas muestreadas. Se acepta el mismo nivel de aproximación que usa el examen real.
- El índice del libro es de la edición Java 11 — no mapea 1:1 a los objetivos del 1Z0-830 (Java 21) ni incluye nada posterior a Java 11. Requiere curación manual capítulo por capítulo, no una transcripción directa.

## Pendiente (fuera de alcance de esta ADR)

- Mapeo completo y ordenado de todas las Secciones (11 capítulos del libro + Java 21 exam features + extras Java 22-25), con sus Unidades dentro de cada una.
- Diseño de implementación: nuevas entidades/campos de Room para Sección y Checkpoint, nuevo use case de sesión de checkpoint, cambios en Ruta/Home para reflejar la jerarquía Sección/Unidad, curación de contenido (JSON packs) para Fundamentos y la reorganización de Streams.
