# Changelog

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