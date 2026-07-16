# Oops! — Design Brief for Fase 2

## Qué es la app

"Oops!" es una app Android de estudio diario para la certificación **Oracle Certified Professional: Java SE 21 Developer (1Z0-830)**. Formato "Duolingo de Java": sesiones cortas y diarias de repetición espaciada (SM-2) que entrenan el recuerdo activo de la API de Java (Streams, Collections, JDBC, etc.) sin IDE ni autocompletado — igual que en el examen real.

El nombre juega con dos sentidos: **OOP** (Object-Oriented Programming) y **oops** (el momento de fallar un ejercicio y que el sistema te lo vuelva a agendar mañana — la mecánica misma del SRS).

## Tono / personalidad

**Juguetón y gamificado**, estilo Duolingo: colores vivos, celebra los aciertos, microanimaciones, la racha y el XP tienen peso emocional real (no son solo un contador). El objetivo final es serio (una certificación profesional), pero la experiencia diaria debe sentirse liviana y motivante — es una app que el usuario debe querer abrir todos los días, no una herramienta que se siente como trabajo.

## Stack técnico (restricción de diseño)

- Android nativo, **Jetpack Compose + Material3**.
- El diseño debe poder expresarse como un Material3 `ColorScheme` + `Typography` + `Shapes` (tokens: colores primary/secondary/tertiary/error/surface, escala tipográfica, radios de esquina), no como assets estáticos sueltos.
- Debe soportar **modo claro y oscuro**.
- Hoy no hay ninguna personalización visual — tema por defecto de Android Studio, sin paleta, sin iconografía, sin ilustraciones.

## Pantallas actuales (Fase 1, ya funcionando)

**Home** — racha (días consecutivos), XP acumulado, botón "Estudiar hoy" (deshabilitado brevemente en el primer arranque mientras se siembra el contenido), botón "Ver progreso".

**Session** — una pantalla por ejercicio: enunciado, snippet de código Java con un `_____` a completar, campo de respuesta libre (tipo `fill_blank`) o 4 botones seleccionables mezclados (tipo `mcq`), feedback "Correcto!"/"Incorrecto" + explicación pedagógica, botón "Siguiente". Al terminar la sesión vuelve a Home con racha/XP actualizados.

**Progress** — lista de dominios del temario del examen (hoy solo "Streams y lambdas") con porcentaje de dominio y barra de progreso.

## Contenido que condiciona el diseño

- Los **snippets de código Java** necesitan tratamiento tipográfico monoespaciado, con buen contraste y legibilidad — es el elemento central de cada ejercicio.
- Los ejercicios `fill_blank` muestran el hueco a completar (`_____`) dentro del código — vale la pena pensar cómo destacarlo visualmente (no solo texto plano).
- Los ejercicios `mcq` son 4 botones de opción; hoy son botones genéricos apilados, sin distinguir visualmente "seleccionado" vs "no seleccionado" antes de responder.
- El feedback correcto/incorrecto hoy es solo texto — es un buen candidato para color + icono + microanimación (encaja con el tono gamificado).
- La racha y el XP son los dos indicadores de progreso más importantes de Home — hoy son texto plano ("Racha: 3 dias", "XP: 40"), sin jerarquía visual ni ningún elemento gráfico.
- Progress agrupa por objetivo del examen — en Fase 2 va a haber más de un dominio (se suma SQL/JDBC), así que el layout debe soportar una lista creciente de dominios, no solo uno.

## Qué necesitamos del diseño

1. **Sistema de color** (paleta primaria + estados de éxito/error) mapeable a un Material3 `ColorScheme`, en claro y oscuro.
2. **Tipografía** — una fuente para texto/UI y una monoespaciada para código, con la escala Material3 (`headline`, `title`, `body`, `label`).
3. **Componentes clave**:
   - Tarjeta/contenedor de ejercicio (prompt + código)
   - Botón de opción MCQ (estados: normal, seleccionado, correcto, incorrecto)
   - Indicador de racha y de XP (Home) — con algo de personalidad, no solo texto
   - Barra/indicador de progreso por dominio (Progress)
   - Estado de feedback correcto/incorrecto (Session) — color + posible icono/ilustración simple
4. **Mockups de las 3 pantallas** (Home, Session en sus dos variantes fill_blank/mcq, Progress) en claro y oscuro.
5. (Opcional, si da tiempo) Ideas de microanimación para: racha al incrementar, transición correcto/incorrecto, avance entre ejercicios.

## Fuera de alcance para esta ronda

- Pantallas que todavía no existen (onboarding, configuración, selector de dominio/idioma) — se agregan en Fase 2/3, no hace falta diseñarlas ahora.
- Mascota o personaje ilustrado a medida (si se propone, que sea opcional/simple — no bloquear el resto del sistema de diseño en eso).
- Ejecución de código, sync, simulacro de examen — funcionalidad de Fase 3, sin impacto visual inmediato.

## Preguntas abiertas para la sesión de diseño

- ¿Un solo color de acento o un sistema de color por dominio del examen (ej. Streams = un color, futuro JDBC = otro)?
- ¿Vale la pena una mascota/icono simple para el "momento oops" (fallo) sin que se sienta infantil dado que el usuario final es un desarrollador adulto?
- ¿Cómo se ve "vacío"/completado el día (todas las sesiones de hoy hechas) en Home — hay oportunidad de una pequeña celebración ahí?