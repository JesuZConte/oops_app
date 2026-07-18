# Handoff: OOPs! — Arcade Neón-Pixel (Fase 2)

> **Nombre de marca: `OOPs!`** — "OOP" en mayúsculas (Object Oriented Programming) + "s!". Usar exactamente así en todo wordmark y copy. (Reemplaza al anterior "Oops!".)

## Overview
**OOPs!** es una app Android de estudio diario para la certificación **Oracle Certified Professional: Java SE 21 Developer (1Z0-830)**. Formato "Duolingo de Java": sesiones cortas diarias con repetición espaciada (SM-2) que entrenan el recuerdo activo de la API de Java (Streams, Collections, JDBC…) sin IDE.

Este handoff cubre el **sistema visual de Fase 2**: una dirección de arte "Arcade" con dos modos —**oscuro Neón-Pixel** (primario) y **claro Papercraft**— aplicada a las pantallas existentes (Home, Session, Progress→**Ruta**), más un sistema de mascota por lenguaje.

## About the Design Files
Los archivos de este bundle son **referencias de diseño hechas en HTML** — prototipos que muestran el look y el comportamiento buscados, **no código de producción para copiar tal cual**. La tarea es **recrear estos diseños en el entorno del proyecto**: Android nativo con **Jetpack Compose + Material3**, expresando el sistema como un `ColorScheme` + `Typography` + `Shapes` (tokens), **no** como assets estáticos sueltos. Debe soportar **modo claro y oscuro**.

El archivo HTML (`Oops Design Directions.dc.html`) es un documento de exploración con varias direcciones descartadas. **Solo importan las selecciones finales** documentadas abajo (secciones/ids `7b`, `7a`, `4a`, `5a`, `2c`, `6d`, `6c`). **La revisión vigente es la combinación de `7a` + `7b`**: de `7a` toma las correcciones de pantallas (nombre OOPs!, taza funcional, tarjeta TU RUTA, sombras de color, Ajustes); de `7b` toma la **navegación ABAJO** (bottom nav arcade) — que **reemplaza** la nav superior mostrada en 7a. El resto del archivo es historial y puede ignorarse.

## Fidelity
**Alta fidelidad (hifi).** Colores, tipografía, radios y estados están definidos con valores exactos. Recrear la UI pixel-perfect con los componentes de Material3. Las únicas piezas pendientes de arte son las **ilustraciones de mascota** (ver Assets) — todo lo demás es implementable directo.

---

## Selecciones finales
| Aspecto | Elección | id en el HTML |
|---|---|---|
| Estética | Arcade (chunky, retro-game) | 1b |
| Modo oscuro (primario) | Neón-Pixel sobre negro | 4a |
| Modo claro | Papercraft (arcoíris sobre papel) | 5a |
| Pantalla de progreso | **Ruta línea de metro** (dominios = líneas) | 2c |
| Indicador Home | **Taza funcional** (vapor=racha, llenado=XP) | 6d |
| Mascota | **Sprite pixel-art** en la línea arcade, por lenguaje | 6c |
| **Navegación** | **Bottom nav arcade** (Home · Ruta · Ajustes) | **7b** |
| **Revisión de build** | Correcciones + nav + Ajustes | 7a + 7b |

---

## Navegación (bottom nav arcade) — 7b (DEFINITIVA)
**La navegación va ABAJO** (convención Android + alcance del pulgar), rehecha en lenguaje arcade. Presente en las 3 pantallas raíz. **Ignorar 7a (versión con nav arriba): fue descartada.**
- **3 pestañas:** Home · Ruta · Ajustes (icono + label). Labels en **Press Start 2P** (~6–7px, MAYÚSCULAS). Iconos lineales ~18px: **Home = la taza-mascota**, Ruta = nodos de metro, Ajustes = engranaje.
- **Arriba de cada pantalla NO hay tab bar:** solo el contenido/título (en Home, el wordmark **OOPs!** + emblema de lenguaje).
- **Pestaña activa:** pill de color relleno — cada tab su color (Home=magenta secondary, Ruta=azul primary, Ajustes=ámbar tertiary), texto/icono en contraste.
  - **Claro:** contenedor `surface` con borde 2px ink + **sombra dura** `4px 4px 0 ink` y `border-top:3px ink`; pill activa con borde 2px ink + sombra `2px 2px 0 ink`.
  - **Oscuro:** contenedor `surface` con `border-top:1px outline`; pill activa con **glow** del color (`0 0 12px rgba(color,.6)`).
- **Inactiva:** transparente, icono ink/onSurface, label muted.
- En Compose: `NavigationBar` de Material3 **con estilo custom** (o un Row propio) que reproduzca pill + sombra/glow; el contenido de cada pantalla va encima.

---

## Design Tokens

### ColorScheme — DARK (primario, "Neón-Pixel")
| Rol Material3 | Hex | Uso |
|---|---|---|
| `background` | `#0A0910` | fondo app |
| `surface` | `#16141F` | tarjetas |
| `surfaceVariant` | `#211E2E` | contenedores anidados |
| `outline` | `#2C2838` | bordes 1px |
| `onBackground` / `onSurface` | `#EDEAF5` | texto principal |
| onSurface muted | `#8B86A3` | texto secundario |
| `primary` (azul · Streams) | `#3D6BFF` | acción, línea Streams |
| `secondary` (magenta · Collections) | `#FF2E7A` | CTA principal, línea Collections |
| `tertiary` (ámbar · SQL/JDBC) | `#FF8A2E` | línea SQL/JDBC, código-keywords |
| success / verde | `#38D06B` | acierto, checks, % progreso |
| `error` | `#FF2E7A` (o `#FF5747`) | fallo |
| onPrimary/onSecondary | `#FFFFFF` | — |

**Glow (dark):** los elementos activos llevan `box-shadow` de color a baja opacidad, ej. primary: `0 0 22px rgba(61,107,255,.5)`; magenta CTA: `0 0 26px rgba(255,46,122,.55)`; nodos de ruta: `0 0 10px` del color de la línea. En Compose = capa de blur/`shadow` teñida o `drawBehind` con radial gradient.

### ColorScheme — LIGHT ("Papercraft")
| Rol Material3 | Hex | Uso |
|---|---|---|
| `background` | `#FAF5EC` | papel hueso |
| `surface` | `#FFFFFF` | tarjetas |
| `outline` / ink | `#2A2632` | bordes chunky 2px, texto |
| onSurface muted | `#8A8296` | texto secundario |
| `primary` (azul · Streams) | `#3A72D6` | — |
| `secondary` (magenta · Collections) | `#E5427E` | CTA principal |
| `tertiary` (ámbar · SQL/JDBC) | `#F59410` | — |
| success | `#4FB03A` | acierto |
| accent amarillo | `#F7C331` | sombra XP, detalles |
| `error` | `#E23B2E` | fallo |
| código bg (fill_blank card) | `#2A2632` | bloque de código (oscuro incluso en claro) |

**Sombras (light):** sombra dura desplazada, sin blur — `4px 4px 0 <ink|accent>` en tarjetas y botones; `3px 3px 0 #2A2632` en botones MCQ. Es el "chunky arcade". En Compose = borde 2px `#2A2632` + una capa/box offset detrás (no elevation con blur).

### Color por dominio (compartido en ambos modos)
- **Streams & lambdas → azul** (`#3D6BFF` dark / `#3A72D6` light)
- **Collections → magenta** (`#FF2E7A` / `#E5427E`)
- **SQL / JDBC → ámbar** (`#FF8A2E` / `#F59410`)
- Bloqueado/futuro → gris (`#3A3548` dark / `#E2DCCB` + borde `#B7AD97` light)

Nota: el sistema debe soportar **N dominios crecientes**; asignar color por índice desde una paleta ordenada (azul → magenta → ámbar → verde → …).

### Typography (Material3 scale)
- **UI / texto:** **Nunito** — `displaySmall/headline` 800–900, `title` 800, `body` 600, `label` 700. Redondeada y chunky.
- **Código y datos mono:** **JetBrains Mono** — snippets Java, valores numéricos técnicos (2/5, %). 400/500/700.
- **Scores retro:** **Press Start 2P** — SOLO números/labels cortos: racha ("07"), XP header, títulos de sección de ruta ("STREAMS · L1"), porcentajes. Nunca para texto corrido (ilegible). Tamaños 8–14px.

| Token | Fuente | Tamaño aprox |
|---|---|---|
| headline (título pantalla) | Nunito 900 | 22–24px |
| title (nombre tarjeta) | Nunito 800 | 15–16px |
| body | Nunito 600 | 13–14px |
| label mono | JetBrains Mono 500 | 11–12px |
| score pixel | Press Start 2P | 8–28px |
| code | JetBrains Mono 400/500 | 11–13px, line-height 1.6–1.7 |

### Shapes (Material3)
- `small` 10–12px · `medium` 14–16px · `large` 18–20px · botón pill/round 14–16px.
- Nodos de ruta = círculos (50%).
- **Dark:** borde 1px `outline` + glow. **Light:** borde 2px ink + sombra dura offset.

### Sintaxis de código (highlight)
- keywords/métodos (`stream`, `filter`, `map`, `collect`, `of`, `forEach`): **ámbar** (`#FF8A2E` dark / keyword amarillo `#F7C331` en bloque oscuro).
- tipos (`List`): azul/teal (`#7DD3FC`/`#7FB8AD`).
- texto normal: `#E7E3F2` sobre bloque `#16141F` (dark) / `#2A2632` (light).
- **hueco `_____` (fill_blank):** chip destacado — fondo tenue del primary + borde punteado del primary + texto primary. Es el foco visual del ejercicio.

---

## Screens / Views

### 1) Home
**Propósito:** pantalla de entrada; muestra racha, XP y lanza la sesión del día.
**Layout:** columna, padding 16px, `gap` ~13px. De arriba a abajo:
1. **Header:** logo "Oops!" (Nunito 900, "!" en secondary) + emblema de lenguaje (cuadro 32px, gradiente primary→secondary).
2. **(light) franja-espectro** 6px arcoíris de acento (opcional, decorativa).
3. **Taza funcional (mascota-medidor, 6d):** tarjeta `surface`. Contiene la **taza** cuyo relleno = XP del día y **vapor** cuyas volutas crecen con la racha. Muestra racha grande ("07", Press Start 2P) + "días seguidos · récord 12".
4. **Tarjeta XP:** label "XP" + valor + barra de progreso al siguiente nivel (relleno primary).
5. Spacer (flex:1).
6. **Botón primario "ESTUDIAR HOY"** (secondary/magenta, full-width). *Deshabilitado brevemente en el primer arranque mientras se siembra contenido.*
7. **Botón secundario "Ver ruta"** (outline con **texto tinta/onSurface**, NO estilo link azul).

> **Correcciones vs. build (ver 7a):** el wordmark es **OOPs!** ("!" en secondary); bajo él va la **franja-espectro** de acento 5px (solo claro); la **taza** es funcional (relleno=XP, vapor=racha), no un contorno; el subtítulo incluye **"récord 12"**; la **barra XP** se rellena en **azul primary** (no lavanda); las **sombras en claro son de color por tarjeta** (racha=magenta, XP=amarillo, TU RUTA=azul).

**Nueva tarjeta — TU RUTA (Home):** entre XP y los botones. Tarjeta `surface` (sombra azul en claro / glow azul en oscuro) con label "TU RUTA" (Press Start 2P), nombre del dominio actual + "68% ▶" y una mini-barra de progreso del dominio. Toca → abre la pantalla Ruta en el dominio actual.

**Copy exacto:** "OOPs!", "STREAK", "días seguidos · récord 12", "XP", "TU RUTA", "ESTUDIAR HOY", "Ver ruta".

### 2) Session — variante `fill_blank`
**Propósito:** un ejercicio de completar un hueco en código.
**Layout:** columna, padding 16px, gap 14px:
1. **Barra de progreso de sesión** (ej. 40%) + contador mono "2/5".
2. **Prompt** (Nunito 800): p.ej. "Completá el operador terminal que colecta el stream en una List."
3. **Tarjeta de código** (`surface`/bloque oscuro, JetBrains Mono): snippet con el hueco **`_____`** como chip resaltado (borde punteado primary).
4. Spacer.
5. **Campo de respuesta** (outline primary, mono) con cursor parpadeante.
6. **Botón "Comprobar"** (primary).

**Snippet ejemplo:**
```java
List<String> r = list.stream()
    .filter(s -> s.length() > 3)
    ._____(toList());   // respuesta: collect
```

### 3) Session — variante `mcq` (con feedback)
**Propósito:** elegir 1 de 4 opciones; muestra estado de feedback.
**Layout:** columna, gap 10px:
1. Barra progreso + "2/5".
2. Prompt corto ("¿Qué imprime?").
3. Tarjeta de código.
4. **4 botones de opción** apilados. Estados:
   - **normal:** outline `outline`, texto onSurface, mono.
   - **seleccionado (antes de responder):** borde primary + fondo tenue primary.
   - **correcto:** borde/fondo **success** (verde) + "✓" a la derecha (dark: glow verde; light: sombra dura).
   - **incorrecto:** borde/fondo **error** + "✗".
5. Spacer.
6. **Banner de feedback:** verde con borde-izq 3px (dark) / tarjeta verde con borde 2px (light). Título "¡Correcto! +10 XP" (+ "🎉" en light) + explicación pedagógica (body, 1–2 líneas).
7. **Botón "SIGUIENTE"** (primary/azul).

**Snippet + opciones ejemplo:**
```java
Stream.of("a","b","c")
    .map(String::toUpperCase)
    .forEach(System.out::print);
```
Opciones: **ABC** (correcta) · abc · a b c · Error.
Explicación: "map transforma cada elemento; forEach imprime sin separador."

### 4) Ruta (línea de metro) — reemplaza a "Progress"
**Propósito:** mapa de avance hacia la certificación; el usuario ve todo el examen y salta entre temas. Sensación de camino hacia una meta.
**Layout:**
- **Top bar** (`ink`/oscuro en ambos modos): título "Ruta 1Z0-830" (Nunito 900) + % global (Press Start 2P, en success/accent).
- **Cuerpo scrollable**, columna con `gap` ~20px. Cada **dominio = una "línea"** con su color:
  - Columna izquierda = **rieles + estaciones**: círculos (15px) unidos por segmentos verticales de 4px. Estación **completa** = relleno del color de línea; **actual** = círculo hueco (fondo bg + borde 3px color, con glow en dark); segmento **futuro** = color desaturado/gris.
  - Columna derecha = contenido: label de línea (Press Start 2P, color de línea, ej. "STREAMS · L1"), sub-items (ej. "stream() & filter ✓", chip "collect() — ahora ▶" resaltado), y estados bloqueados ("Iterators 🔒").
- Líneas bloqueadas: estación gris + texto muted + regla de desbloqueo ("Se abre al 60% de Streams").

**Mapeo ejemplo:** Streams (azul, L1, en curso "collect()") → Collections (magenta, L2, 45%, Iterators 🔒) → SQL/JDBC (ámbar, L3, 🔒).
**Escalabilidad:** lista vertical que crece con más dominios; nada de layout fijo a 3.

---

## Mascota (sprite pixel-art) — 6c + 6d
- **Estilo:** pixel-art 8-bit, coherente con el arcade. Por lenguaje: **Java = taza de café ☕**; futuro Python = serpiente, JS, Rust (🦀), Go, Kotlin…
- **Dónde aparece:**
  1. **Home:** integrada como la **taza funcional (6d)** — vapor = racha, relleno = XP.
  2. **Session feedback:** anima en acierto (celebra) y en el "oops"/fallo (se derrama — juego con el nombre de la app).
  3. **Ruta:** compañero que "avanza" por la línea de metro.
  4. **Día completado / vacíos:** pequeña celebración.
- **Sistema por lenguaje ("theming slot"):** cada lenguaje aporta {color de acento primary/secondary + set de sprites de su mascota}. El resto del sistema (racha, XP, ruta, feedback, tipografía) permanece idéntico → sumar lenguaje = contenido, no rediseño.

---

## Interactions & Behavior
- **Estados MCQ:** normal → seleccionado → (al comprobar) correcto/incorrecto. Bloquear opciones tras responder.
- **Microanimaciones (sugeridas):**
  - **Racha al incrementar:** número hace "pop" (scale 0.7→1.08→1) + vapor de la taza sube un nivel.
  - **Feedback correcto:** banner entra + sprite mascota celebra + "+10 XP" cuenta hacia arriba.
  - **Feedback incorrecto:** shake sutil de la tarjeta + mascota "derrame" (oops).
  - **Avance en la ruta:** estación completada cambia de hueca→rellena con un flash del color de línea; el compañero se desplaza al siguiente nodo.
- **Navegación:** Home → Session (secuencia de ejercicios) → al terminar vuelve a Home con racha/XP actualizados. Home → Ruta.
- **Cursor parpadeante** en campos `fill_blank`.

## State Management
- `streakDays` (int), `streakRecord` (int)
- `xpTotal` (int), `xpToday` (int), `levelProgress` (0–1)
- Sesión: `exercises[]`, `currentIndex`, `sessionProgress`, `selectedOption`, `answerState` (idle/correct/incorrect), respuesta libre para fill_blank
- Ruta: `domains[]` con `{ name, color, level, masteryPct, items[], locked, unlockRule }`
- `themeMode` (light/dark) y `languagePack` (color + sprites de la mascota activa)
- SRS/SM-2: agendamiento de ejercicios (ya existe en Fase 1)

## Assets
- **Referencias de paleta** (en `refs/`): `neon-pixel-reference.avif` (fuente del dark), `papercraft-wheel-reference.webp` (fuente del claro). Son inspiración de color, no assets finales.
- **Pendiente de producción:** sprites pixel-art de la mascota (Java = taza) — idealmente varias poses: idle, celebración, "derrame/oops", caminar. En el HTML están representados con emojis/placeholders; **no usar emojis en producción**, sustituir por el sprite real.
- **Fuentes (Google Fonts):** Nunito, JetBrains Mono, Press Start 2P.

## Files
- `Oops Design Directions.dc.html` — prototipo HTML con todas las direcciones. Selecciones finales en los ids: **4a** (dark), **5a** (light), **2c** (ruta), **6d** (taza funcional), **6c** (mascota pixel). El resto es historial descartable.
- `refs/` — imágenes de referencia de color.
