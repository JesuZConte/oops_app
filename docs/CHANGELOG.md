# Changelog

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