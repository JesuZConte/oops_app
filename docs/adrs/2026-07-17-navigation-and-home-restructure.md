# ADR: Navegación persistente, Ajustes y reestructuración de Home

**Estado:** Aceptado

## Contexto

Después de probar la app ya con el tema Arcade Neón-Pixel de Fase 2 en un dispositivo real, surgieron varios puntos de fricción de uso que no estaban cubiertos por el diseño original de Fase 1/2:

1. **No hay navegación persistente entre pantallas.** Home, Session y Ruta viven aisladas. En particular, Ruta no tiene forma de volver salvo el botón físico/gesto de "atrás" del sistema.
2. **No existe ningún lugar para cambiar el tema (claro/oscuro) manualmente ni para ver la versión de la app.** El tema hoy sigue automáticamente `isSystemInDarkTheme()`, sin override.
3. **Home muestra racha y XP como protagonistas, pero no da ninguna señal de en qué parte de la ruta de certificación vas** — hay que entrar a Ruta para saberlo.
4. **El título "Oops!" no comunica el juego de palabras con "Object-Oriented Programming"** que le da sentido al nombre.

Cada uno de estos puntos podría resolverse por separado (una flecha de volver aquí, un ícono de ajustes allá), pero los primeros dos apuntan a la misma causa raíz: no hay ninguna estructura de navegación compartida entre pantallas.

## Decisión

**Navegación:** se agrega un `NavigationBar` (Material3) persistente con 3 destinos de nivel superior — **Home / Ruta / Ajustes**. `Session` queda deliberadamente fuera de esta barra: sigue siendo un flujo de estudio enfocado, sin distracciones, con su entrada/salida actual (Home → Session → vuelve a Home). Esto resuelve la navegación de Ruta sin agregar una flecha de volver ad-hoc, ya que las 3 pestañas son destinos de nivel superior sin pila de "atrás" entre ellas.

Como beneficio colateral, centralizar la navegación en un `Scaffold` con `NavigationBar` es también la oportunidad natural para resolver el manejo de insets (status bar / nav bar) de forma consistente, en vez del parcheo pantalla por pantalla que se hizo durante Fase 2 (Tasks 4, 5 y 6 cada una agregó su propio `.systemBarsPadding()`/`.navigationBarsPadding()` de forma independiente).

**Home:** mantiene las tarjetas de racha y XP tal como están (siguen siendo la señal de gamificación diaria). Se agrega una tarjeta adicional de resumen de ruta (ej. "Streams · 45% · Continuar"), reutilizando el componente `ThemedCard` ya existente, con tap para saltar directo a la pestaña Ruta. El título cambia de **"Oops!"** a **"OOPs!"** para reforzar el doble sentido con Object-Oriented Programming.

**Ajustes (pantalla nueva):** selector de tema de 3 estados — **Sistema / Claro / Oscuro** (Sistema por defecto, preservando el comportamiento actual para quien no toque el ajuste) — persistido con Jetpack DataStore (Preferences). Además, versión de la app de solo lectura, leída de `BuildConfig.VERSION_NAME`. No se agrega nada más en esta pasada (idioma, notificaciones, etc. quedan fuera de alcance).

**Ruta:** sin cambios de contenido. Pierde la necesidad de una flecha de volver propia porque pasa a vivir detrás de la barra de navegación.

## Consecuencias

**Positivas:**
- Resuelve de una sola vez la navegación de Ruta, el acceso a Ajustes, y dado que se centraliza el manejo de insets en el `Scaffold`, reduce el riesgo de que una futura pantalla repita el mismo bug de padding que tuvieron Home/Session/Ruta en Fase 2.
- Deja la app lista para agregar futuras pantallas de nivel superior (ej. Perfil) sin rediseñar la navegación otra vez.
- El resumen de ruta en Home responde directamente al feedback de que "no se ve en qué vas" sin sacrificar la señal de racha/XP que ya funciona bien.

**Trade-offs:**
- Se introduce una dependencia nueva (Jetpack DataStore Preferences) solo para persistir la preferencia de tema — justificado porque es el mecanismo estándar recomendado por Android para este caso de uso, liviano y sin necesidad de Room.
- `OopsappTheme`/`MainActivity` necesitan leer el tema desde el DataStore en vez de solo `isSystemInDarkTheme()`, lo que agrega un nivel de indirección (flujo asíncrono) a un punto que hoy es puramente síncrono.
- Home vuelve a cambiar de layout poco después de haberse rediseñado en Fase 2 — aceptable porque es una adición (tarjeta de ruta) y un cambio de texto (título), no una reescritura.