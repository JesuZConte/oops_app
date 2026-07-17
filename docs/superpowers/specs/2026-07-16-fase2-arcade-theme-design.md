# Fase 2 — Arcade Neón-Pixel Theme: Implementation Design

> Fuente de verdad visual: `docs/design/design_handoff_oops_arcade/README.md` (tokens de color,
> tipografía, layouts exactos de Home/Session/Ruta, sistema de mascota). Este documento cubre
> únicamente las **decisiones técnicas de implementación** que ese handoff no especifica, porque
> fue escrito por una sesión de diseño sin contexto del código Kotlin/Compose existente.

## Alcance

Implementar todo el sistema en una sola pasada: tokens de color/tipografía + las 3 pantallas
(Home, Session en sus dos variantes `fill_blank`/`mcq`, y el rediseño de Progress→Ruta). Sigue
al plan de Fase 1 (`docs/superpowers/plans/2026-07-15-oops-mvp-fase1.md`), que ya está mergeado
en `main`.

## Decisiones técnicas (no cubiertas por el handoff de diseño)

1. **`OopsappTheme` tiene `dynamicColor = true` por defecto** (`ui/theme/Theme.kt`). En Android
   12+ esto reemplaza la paleta custom por colores extraídos del wallpaper del usuario — hay que
   fijarlo en `false` para que el sistema Arcade se vea en cualquier dispositivo.

2. **Tokens que Material3 `ColorScheme` no modela nativamente** (glow en modo oscuro, sombra dura
   offset en modo claro, fondo de bloque de código, colores de sintaxis, color por dominio del
   examen) viven en un objeto de extensión propio (`OopsExtendedColors`, expuesto vía
   `CompositionLocal` junto al `MaterialTheme` estándar) — no se fuerzan dentro de roles de
   `ColorScheme` que no les corresponden semánticamente.

3. **Fuentes bundleadas como recursos locales** (`res/font/`), no Google Fonts descargables en
   runtime — consistente con la restricción "local-first, sin red" del spec de producto
   (`docs/specs/PROJECT-OOPS.md`). Nunito, JetBrains Mono y Press Start 2P se descargan (son
   open-source, licencia OFL) desde el repo de Google Fonts y se empaquetan en el APK.

4. **Color por dominio calculado por índice ordenado**, tal como pide el handoff ("asignar color
   por índice desde una paleta ordenada"). Streams (índice 0) = `primary`/azul, Collections
   (índice 1) = `secondary`/magenta, SQL/JDBC (índice 2) = `tertiary`/ámbar, siguiente = gris de
   "bloqueado". Esto reutiliza los roles de Material3 ya definidos en vez de inventar un mapeo
   paralelo.

5. **Mascota — alcance de esta ronda:**
   - **Taza funcional (sección 6d del handoff)**: se implementa completa. Es geometría pura
     (forma de taza vía `Canvas`/`Shape`, relleno proporcional al XP del día, volutas de vapor
     animadas con `Canvas` + `Animatable`) — no depende de ningún asset de arte.
   - **Emblema plano por lenguaje** (mencionado como fallback en la sección 6a del handoff): un
     ícono geométrico simple de taza para Java, usado en el header de Home y como marcador en
     Ruta.
   - **Sprite pixel-art real (sección 6c)**: el handoff lo marca explícitamente como "pendiente
     de producción" (el mockup solo tiene un placeholder de texto). Queda **fuera de esta ronda**
     — el emblema plano ocupa su lugar. Cuando exista el arte, reemplazar el emblema por el
     sprite es un cambio de asset localizado, no un rediseño.

6. **Ruta — dominios bloqueados como contenido de UI, no filas de base de datos.** Streams
   renderiza con datos reales de `ProgressRepository.getReadinessByObjective()`. Collections y
   SQL/JDBC aparecen como líneas bloqueadas con candado y el texto estático del handoff ("Se abre
   al 60% de Streams") — una lista hardcodeada en la capa de UI/ViewModel, no entidades Room
   fantasma. Cuando Fase 2 agregue contenido real de esos dominios, sus líneas se activan solas
   sin tocar este código.

## Estructura de archivos (nuevo/modificado)

```
app/src/main/res/font/
├── nunito_*.ttf                      # pesos 600/700/800/900
├── jetbrains_mono_*.ttf              # pesos 400/500/700
└── press_start_2p_regular.ttf

app/src/main/java/com/zconte/oopsapp/ui/theme/
├── Color.kt              # paleta dark Neón-Pixel + light Papercraft (modificado)
├── Type.kt                # FontFamily × 3 + escala Material3 (modificado)
├── Shape.kt               # radios small/medium/large + pill (nuevo)
├── ExtendedColors.kt      # OopsExtendedColors + CompositionLocal (nuevo)
└── Theme.kt                # dynamicColor=false, provee ExtendedColors (modificado)

app/src/main/java/com/zconte/oopsapp/ui/components/
├── FunctionalCup.kt        # taza+vapor+relleno, Canvas (nuevo)
├── LanguageEmblem.kt       # ícono plano por lenguaje (nuevo)
├── CodeBlock.kt             # bloque de código con syntax tint + chip de hueco (nuevo)
└── HardShadowCard.kt / GlowCard.kt   # contenedor con sombra dura (light) / glow (dark) (nuevo)

app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt         # rediseño (modificado)
app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt   # rediseño, 2 variantes (modificado)
app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt # renombrar/rediseñar a Ruta (modificado)
```

## Fuera de alcance para esta ronda

- Sprite pixel-art real de la mascota (arte pendiente).
- Animaciones de celebración/derrame del sprite en Session (dependen del sprite real).
- Compañero caminando por la Ruta (depende del sprite real).
- Contenido real de Collections y SQL/JDBC (trabajo de contenido de Fase 2, no de diseño).
- Onboarding, configuración, selector de idioma — no existen pantallas para ellos todavía.