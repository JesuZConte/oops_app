# Changelog

## 2026-08-04 — Giro de visión (Path autosuficiente) + escaleras "aprender haciendo" (slice 1)

Ronda de brainstorming (`docs/adrs/2026-07-30-self-teaching-path-vision.md`) que redefine la premisa del producto: Oops! deja de ser un "compañero de práctica" que asume que el jugador tiene el libro de referencia al lado, y pasa a ser un **Path autosuficiente estilo Duolingo** — enseña cada tema desde cero dentro de la app, con el examen OCP Java SE 21 (1Z0-830) como meta final. `docs/specs/PROJECT-OOPS.md` se reescribió como una constitución corta y durable (propósito, filosofía, restricciones duras, no-objetivos, punteros a ADRs/specs vigentes), eliminando el detalle técnico obsoleto que arrastraba desde Fase 1.

Implementado vía `docs/superpowers/plans/2026-07-31-learn-by-doing-ladders-slice1.md` (6 tareas, subagent-driven-development en worktree aislado), a partir del spec `docs/superpowers/specs/2026-07-30-learn-by-doing-ladders-slice1-design.md`. Prueba el motor completo end-to-end en una sola unidad piloto (`streams-collectors`).

- **Escaleras de primera exposición**: cada concepto nuevo gana un tipo de ejercicio `worked_example` (tarjeta didáctica no rastreada, sin puntaje ni `ReviewState`) que se muestra una vez antes de sus ejercicios `guided`/`solo` normales. Todo viaja dentro del `payload` JSON existente (`conceptId`, `role`, `pathOrder`, `dependsOn`) — sin migración de Room.
- **Sesión diaria en dos fases**: `GetTodaySessionUseCase` pasa de "vencidos + N nuevos al azar" a "vencidos (Fase B, SRS sin cambios) + próximos pasos del Path en `pathOrder` (Fase A)". Un concepto se filtra de la Fase A completo (incluida su intro) una vez "nacido".
- **Composición como ciudadana de primera clase**: un concepto puede declarar `dependsOn` sobre otros conceptos: no aparece hasta que todas sus dependencias nacieron. La unidad piloto incluye el caso de entrevista real que motivó todo el giro de visión (separar usuarios por sueldo y agrupar por departamento, combinando `partitioningBy` + `groupingBy`).
- **Grandfather**: los ids de ejercicio existentes de `streams-collectors` (`streams-14`, `streams-19`, `streams-parsons-02`) se preservaron intactos al re-autorar la unidad, para que el progreso SM-2 real de Luis sobre esos ejercicios no se perdiera.

### Decisiones de diseño resueltas durante la implementación

- **Modelo de "nacido" simplificado dos veces**: la primera vuelta reemplazó la idea original (`intro`/`guided` "consumidos pero nunca agendados", irrepresentable sin tocar Room) por "solo el `intro` es especial; `guided`/`solo` son ejercicios SRS normales". La revisión final de rama encontró que esto igual dejaba un caso huérfano: responder solo el `guided` de un concepto y salir a mitad de escalera marcaba el concepto "nacido" y el `solo` pendiente quedaba inalcanzable (sin `ReviewState`, invisible en ambas fases). Se corrigió redefiniendo "nacido" a exigir específicamente `solo`/`practice` — decisión tomada explícitamente por Luis entre dos alternativas presentadas.
- **Metadata de escalera "payload-only"**: se descartó un `ConceptPack` a nivel de unidad (propuesta original del spec) en favor de que todos los campos de escalera vivan en `ExerciseContent`, evitando agregar un método a `ContentRepository` que hubiera roto la compilación de todos los fakes de test existentes.

### Bugs encontrados y corregidos

- La revisión final de rama encontró que `ContentSeeder.CURRENT_CONTENT_VERSION` nunca se subió durante la implementación — sin ese bump, el contenido nuevo de escaleras nunca se re-sembraría en una instalación existente (como la de Luis), dejando el feature invisible en producción pese a estar "terminado". Corregido antes de mergear.
- Un subagente implementador (Tarea 1) commiteó directo al checkout de `main` pese a una instrucción explícita de verificación previa al commit — la instrucción en prosa no fue suficiente por sí sola. Se recuperó vía `git cherry-pick` sin pérdida de trabajo, y las tareas siguientes (2 en adelante) se reforzaron con reglas mecánicas (`git -C <ruta absoluta>` obligatorio en cada comando) que no volvieron a fallar.
- Un test nuevo de checkpoint (`GetCheckpointSessionUseCaseTest`) usaba una muestra aleatoria (`shuffled().take()`) sobre un pool grande, dándole solo ~50% de probabilidad real de detectar una regresión — corregido a un pool pequeño y determinista.

### QA en dispositivo real

Instalación in-place sobre el dispositivo de Luis (preservando datos reales, ejercitando el camino de actualización con contenido versión 6→7). Confirmado en vivo: arranque sin crashes, progreso real preservado (racha/XP/unidades completas), filtro de tarjetas `worked_example` correcto contra contenido real (7/10 en repetición de unidad), y renderizado correcto de un ejercicio `guided` nuevo. La tarjeta `worked_example` en sí no se pudo observar en vivo — bloqueada por los checkpoints obligatorios pendientes del dispositivo real, cuyo timeout de seguridad se disparó varias veces durante los intentos (confirmando que esa función pre-existente funciona, a costa de tiempo real de sesión). Luis aceptó la evidencia recolectada como suficiente sin seguir persiguiendo esa observación puntual.

### Estado del repo

Mergeado a `main` local (`1286fb6`) y pusheado a `https://github.com/JesuZConte/oops_app`. `.gitignore` corregido en la misma tanda para excluir `.idea/` y `.claude/` completos (antes solo unos pocos archivos sueltos de `.idea` estaban listados, sin cubrir el directorio real que aparecía sin trackear).

### Próximo paso natural

- **Rebanada 1b**: tope de material nuevo *por día* (hoy el tope es por sesión) + estado "por hoy no hay nada nuevo, vuelve mañana" — requiere diseñar un mecanismo de fecha-de-nacimiento de concepto que no existe todavía.
- **Rebanada 2+**: re-autorar las 18 unidades restantes como escaleras, sección por sección, al ritmo de contenido habitual.
- Ver en vivo la tarjeta `worked_example` cuando surja naturalmente en el uso diario de Luis (o forzando el escenario en un dispositivo/perfil de prueba aparte).
- Idea diferida, no comprometida: un "check" visual al llegar a 3 aciertos de un ejercicio (mencionado por Luis durante el brainstorming, separable del motor de escaleras).

## 2026-07-21 — Fase 2.1: Secciones, Unidades y Checkpoint de repaso

Implementado vía `docs/superpowers/plans/2026-07-20-fase2-1-sections-units-checkpoints.md` (11 tareas, subagent-driven-development), a partir del ADR `docs/adrs/2026-07-20-content-structure-sections-checkpoints.md` y el spec `docs/specs/2026-07-20-fase2-1-foundation-spec.md`. Reemplaza el modelo plano `Topic → Exercise` por una jerarquía **Sección → Unidad → Ejercicio** estilo Duolingo, probada end-to-end con dos secciones reales.

- **Modelo de datos**: migración Room v1→v2 (`MIGRATION_1_2`, pura DDL) que reemplaza `topics`/`exercises` planos por `sections`/`units`/`exercises` v2, más `unit_progress`, `checkpoint_attempts` y `content_meta`. El progreso del usuario (`review_state`, `user_stats`) nunca se toca — validado con un test instrumentado (`MigrationTest`, `androidTest`) que siembra una base v1 real, corre la migración, y confirma que el progreso sobrevive intacto y el contenido v2 se re-siembra correctamente.
- **Contenido**: nueva sección "Fundamentos de Java" (17 ejercicios, 3 unidades) y la sección "Streams y lambdas" existente reorganizada en 4 unidades — los 20 ids de ejercicio existentes se preservaron byte a byte para que el `review_state` de usuarios existentes siga resolviendo tras la migración. `ContentSeeder` pasa de un guard de "tabla vacía" a un guard de versión de contenido (`content_meta`), para que las actualizaciones de contenido futuras se re-siembren correctamente sin depender de que la base esté vacía.
- **Progreso por unidad**: una unidad se completa por "primera pasada" (todos sus ejercicios respondidos al menos una vez, sin requerir dominio SM-2). Gating secuencial: la primera sección y su primera unidad siempre están desbloqueadas; una unidad se desbloquea al completar la anterior en su sección; una sección se desbloquea cuando todas las unidades de la sección previa están completas.
- **Checkpoint de repaso**: mini-cuestionario opcional al final de cada sección (~12 preguntas: la sección recién completada + hasta 3 de secciones anteriores), con umbral de aprobación del **68%** (igual al examen real 1Z0-830). Las respuestas del checkpoint alimentan el motor SM-2 exactamente igual que una sesión diaria — el pass/fail es un cálculo agregado por encima, no una evaluación aislada. Completar un checkpoint también actualiza la racha diaria, igual que cualquier otra sesión.
- **UI**: `ExerciseAnswerCard` extraído de `SessionScreen` como componente compartido (reubicación exacta, sin rediseño), reutilizado por la nueva `CheckpointScreen`. `SessionViewModel` generalizado para reproducir tanto la cola diaria como la de una unidad específica. Ruta rediseñada: de líneas de dominio planas a un camino Sección→Unidad con filas de unidad jugables/bloqueadas/completas y una fila de Checkpoint al final de cada sección completa — se retira el chip estático "collect() — ahora ▶" (placeholder de la ronda anterior), reemplazado por navegación real. La tarjeta "TU RUTA" de Home ahora refleja la sección actual del jugador (primera sección incompleta) y su progreso real de unidades, en vez de un porcentaje de dominio fijo a Streams.

### Fuera de alcance (diferido a Fase 2.1b)

El checkpoint de **ubicación** (saltar unidades adelantándose, con siembra híbrida de SM-2 para las unidades saltadas) quedó explícitamente fuera de esta ronda — solo se implementó el checkpoint de repaso voluntario. Documentado en el ADR y el roadmap, no olvidado.

### Bugs encontrados y corregidos durante la implementación

- El test de migración instrumentado encontró que `MIGRATION_1_2` creaba `checkpoint_attempts.id` sin `NOT NULL` (difería del esquema exportado de Room) — una migración real en un dispositivo habría fallado al abrir la base tras el upgrade. Corregido antes de continuar.
- La revisión de tarea encontró una condición de carrera en `CheckpointViewModel.nextExercise()`: un doble-tap en la última pregunta del checkpoint podía lanzar dos inserciones concurrentes en `checkpoint_attempts`. Corregido con el mismo guard `isCompleting` que ya usaba `SessionViewModel`.
- La revisión final de rama encontró un pipeline de "readiness" basado en dominio SM-2 (`ProgressRepository.getReadinessByObjective` y su cadena de DAOs) que había quedado inalcanzable tras el rediseño de Home — eliminado junto con otro código muerto menor (`ExerciseDao.count()`, un color sin uso de la Ruta anterior).

### Estado del repo

Suite de tests unitarios y test instrumentado de migración (`connectedAndroidTest`) verificados en verde sobre la rama ensamblada. Recorrido completo en dispositivo real (claro y oscuro) de todo el flujo: desbloqueo de unidades, gating entre secciones, checkpoint con aprobación, tarjetas de Home actualizadas.

## 2026-07-20 — Correcciones de diseño (handoff 7a/7b)

Implementado vía `docs/superpowers/plans/2026-07-18-design-corrections-arcade-7b.md` (7 tareas, subagent-driven-development), a partir del handoff actualizado en `docs/design/design_handoff_oops_arcade/` (secciones 7a/7b, definitivas).

- **Marca**: wordmark "OOPs!" con el "!" en color secondary (antes un solo color).
- **Sombras de tarjeta en modo claro**: `ThemedCard` ahora usa el color de cada tarjeta (`accentColor`) para su sombra dura en claro, en vez de un color tinta fijo — habilita sombras por color en Home y Ajustes (racha=magenta, XP=amarillo, TU RUTA=azul, TEMA=magenta, VERSIÓN=amarillo/ámbar).
- **Bottom nav arcade**: rediseño completo de la barra inferior — pestaña activa como pill de color por tab (Home=magenta, Ruta=azul, Ajustes=ámbar), sombra dura+borde tinta en claro / glow sin borde en oscuro, labels en Press Start 2P, iconos propios dibujados a mano (taza para Home, nodos de metro para Ruta, engranaje para Ajustes) en vez de iconos genéricos de Material. Se agregó el inset del sistema (`navigationBarsPadding`) y un borde superior separando la barra del contenido — ambos ausentes en el `NavigationBar` por defecto que reemplaza.
- **Home**: franja-espectro de 5px bajo el wordmark (solo modo claro); tarjeta "TU RUTA" reestructurada (nombre de dominio + "NN% ▶" + mini-barra de progreso separada); botón "Ver ruta" con texto tinta en vez de azul-link.
- **Home — tarjeta STREAK como hero**: se agregó el ícono de llama (🔥) que faltaba junto al número de racha, se corrigió el color del número (tinta en claro / tertiary en oscuro — antes tertiary fijo en ambos) y se subió su tamaño a 22sp, todo según el mockup 7b. Se le dio más aire a STREAK y se compactó XP/TU RUTA para reforzar la jerarquía visual (STREAK domina, XP y TU RUTA son secundarias) — feedback de una primera pasada de verificación en dispositivo.
- **Ruta**: chip "collect() — ahora ▶" bajo la línea de dominio actualmente desbloqueada (Streams).
- **Ajustes**: radios de tema reemplazados por un radio "chunky" a medida (anillo tinta/acento + punto de acento), reemplazando el `RadioButton` de Material3 por defecto.

### Decisiones de alcance resueltas durante esta ronda

- El handoff nuevo implica tres datos que no existen en el modelo actual (récord histórico de racha, XP del día para el relleno de la taza, y el "paso actual" por dominio en Ruta) — las tres ya se habían diferido antes por requerir cambios de Room. Se optó por valores estáticos/proxy para esta pasada visual: "récord 12" fijo en el copy, la taza sigue usando el progreso de nivel como proxy de relleno, y el chip de Ruta usa un texto fijo. Sin cambios de schema.
- La paleta de la taza (contorno terracota / relleno café, elegida en una ronda anterior para dar identidad "Java" a la mascota) se mantuvo intacta, como desviación deliberada del nuevo mockup (que pedía contorno tinta/ámbar + relleno tertiary) — decisión explícita del usuario.

### Nota de accesibilidad (no bloqueante)

Las pestañas de la bottom nav y los radios "chunky" son composables a medida que reemplazan `NavigationBarItem`/`RadioButton` de Material3 — pierden la semántica de accesibilidad que esos componentes daban gratis (rol de tab/radio, anuncio de selección para TalkBack). Pendiente de una pasada de accesibilidad futura.

### Estado del repo

Todo mergeado a `main` y pusheado a `https://github.com/JesuZConte/oops_app`. Sin cambios de schema de Room. Verificado en dispositivo (Home, Ruta, Ajustes en claro y oscuro) además de la suite de tests, que sigue pasando completa.

## 2026-07-17 — Fase 2 (Arcade Neón-Pixel) + navegación, Ajustes y rediseño de Home

### Fase 2: tema visual "Arcade Neón-Pixel"

Implementado vía `docs/superpowers/plans/2026-07-16-fase2-arcade-theme.md` (6 tareas, subagent-driven-development), a partir del handoff de diseño en `docs/design/design_handoff_oops_arcade/`.

- Tokens de tema: paletas dark "Neón-Pixel" / light "Papercraft", tipografía (Nunito, JetBrains Mono, Press Start 2P — fuentes variables bundleadas localmente), `OopsExtendedColors` para tokens que Material3 no modela (glow/sombra dura, colores de código, estado bloqueado).
- Componentes compartidos: `ThemedCard` (glow en oscuro / sombra dura en claro), `FunctionalCup` (taza animada: relleno = progreso de XP, vapor = racha), `LanguageEmblem`, `CodeBlock` (resaltado de sintaxis + chip de hueco para `fill_blank`).
- Las 3 pantallas rediseñadas: Home, Session (flujo MCQ de 2 pasos seleccionar→confirmar con estados temáticos, feedback con glow/sombra), Ruta (antes "Progress" — header oscuro a sangrado completo, líneas de dominio con Collections/SQL bloqueados).
- Revisión final de rama encontró y corrigió: header de Home sin protección contra la status bar.

### Bugs encontrados en prueba real (post-Fase 2)

- **Taza sin contraste en modo oscuro**: el contorno usaba `MaterialTheme.colorScheme.outline` (casi invisible contra el fondo). Corregido con una paleta dedicada tipo taza de café Java (contorno terracota, relleno café) — resuelve el bug y le da identidad visual de Java a la taza.
- **Botón "COMPROBAR" tapado por el teclado**: `SessionScreen` no reservaba espacio para el IME. Agregado `.imePadding()`.

### Navegación persistente, Ajustes y rediseño de Home

Decisiones documentadas en `docs/adrs/2026-07-17-navigation-and-home-restructure.md`, implementadas vía `docs/superpowers/plans/2026-07-17-navigation-settings-home.md` (5 tareas).

- **Barra de navegación inferior** (Home / Ruta / Ajustes) — persistente en las 3 pantallas de nivel superior, oculta en Session (que sigue siendo un flujo de estudio a pantalla completa). Resuelve la falta de botón de volver en Ruta y centraliza el manejo de insets que se había ido parchando pantalla por pantalla en Fase 2.
- **Ajustes** (pantalla nueva): selector de tema Sistema / Claro / Oscuro, persistido con Jetpack DataStore Preferences; versión de la app.
- **Home**: se agregó una tarjeta "TU RUTA" con el progreso real de Streams (mismo dato que muestra Ruta), manteniendo racha y XP como estaban. Título cambiado de "Oops!" a "OOPs!".
- Durante la implementación se encontró y corrigió un bug real de Material3: `Scaffold` no resuelve `innerPadding` a `0.dp` cuando `bottomBar` está vacío (cae al inset del sistema igual), lo que duplicaba el padding inferior en Session — corregido con un condicional por ruta.

### Bug encontrado en prueba real (post-navegación)

- **La pestaña "Home" no hacía nada si se llegaba a Ruta por "Ver ruta" o la tarjeta "TU RUTA"**: esos dos navegaban con un `navController.navigate()` simple, distinto del patrón `popUpTo`/`launchSingleTop`/`restoreState` que usa la barra de navegación inferior — mezclar ambos estilos hacia el mismo destino de nivel superior dejaba el back stack inconsistente. Corregido alineando ambos callbacks al mismo patrón de navegación.

### Estado del repo

Todo mergeado a `main` y pusheado a `https://github.com/JesuZConte/oops_app`. Sin cambios de schema de Room. Tests unitarios existentes (SM-2, casos de uso) más los nuevos (`SettingsRepositoryImplTest`, `ThemeResolverTest`) — todos pasando.

### Pendiente / fuera de alcance de hoy

- Flash breve de tema en el arranque en frío para quien fuerce Claro/Oscuro contra el ajuste del sistema (trade-off aceptado por leer DataStore de forma asíncrona; mitigable con `androidx.core.splashscreen` si se vuelve molesto).
- Consolidar la lógica "Session es pantalla completa" (hoy repetida en dos condicionales en `MainActivity`) en una sola fuente de verdad — sugerencia de mantenibilidad, no bloqueante.
- Récord de racha histórica en Home (requiere trackear el máximo, no solo la racha actual — cambio de modelo de datos, diferido).
- Sprite pixel-art real de la mascota (el handoff de diseño lo deja pendiente de producción).