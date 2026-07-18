# PROMPT para Claude Code

Copia y pega esto en Claude Code, abierto en la raíz de tu repo Android (con esta carpeta `design_handoff_oops_arcade/` dentro del proyecto).

---

Estás implementando correcciones de diseño en **OOPs!**, una app Android (Jetpack Compose + Material3) de estudio para la certificación Java SE 21 (1Z0-830).

**Antes de tocar código:**
1. Lee `design_handoff_oops_arcade/README.md` completo — tiene los tokens exactos (ColorScheme claro+oscuro, Typography, Shapes), el layout de cada pantalla, estados y copy.
2. Abre `design_handoff_oops_arcade/Oops Design Directions.dc.html` en el navegador como referencia visual. Las secciones que mandan son **7b** y **7a** (las más nuevas, arriba del todo). El resto del archivo es historial: ignóralo.

**Regla de oro sobre la navegación:** la versión definitiva es **7b (bottom nav arcade, ABAJO)**. La nav superior que aparece en 7a fue **descartada** — no la implementes.

**Cambios a aplicar (recrea con componentes Material3, soportando claro y oscuro):**
1. **Nombre de marca:** `OOPs!` (OOP en mayúsculas → Object Oriented Programming), con el "!" en color secondary. Reemplaza cualquier "Oops!".
2. **Bottom nav arcade (7b):** Home · Ruta · Ajustes, abajo. Pestaña activa = pill de color por tab (Home=magenta, Ruta=azul, Ajustes=ámbar); claro = borde 2px ink + sombra dura `4px 4px 0`; oscuro = glow del color. Labels en Press Start 2P. **Icono de Home = la taza-mascota.** Sin tab bar arriba.
3. **Home — taza funcional:** reemplaza el contorno naranja por la taza medidor (relleno = XP del día, vapor = racha). Subtítulo "días seguidos · récord 12".
4. **Home — barra XP:** rellena en **azul primary** con el %, no lavanda.
5. **Home — tarjeta TU RUTA:** entre XP y el botón; dominio actual + "68% ▶" + mini-barra; toca → abre Ruta en ese dominio.
6. **Home — franja-espectro** 5px bajo el wordmark (solo claro) y botón "Ver ruta" como outline con texto tinta (no link azul).
7. **Sombras en claro:** de color por tarjeta (racha=magenta, XP=amarillo, TU RUTA=azul), no todas negras.
8. **Ruta:** muestra el chip del paso actual "collect() — ahora ▶".
9. **Ajustes:** pantalla con TEMA (radios chunky: borde tinta + punto de acento) y VERSIÓN; sombras de color en claro / glow en oscuro.

**Fidelidad:** alta. Usa los valores hex/tamaños exactos del README. No inventes colores fuera de la paleta.

**Pendiente (no bloqueante):** el sprite pixel-art real de la taza está por producirse; en el proto es un placeholder — deja el icono/ilustración detrás de un recurso reemplazable.

Cuando termines, muéstrame un resumen de los archivos que tocaste y cualquier decisión donde el diseño no era claro.
