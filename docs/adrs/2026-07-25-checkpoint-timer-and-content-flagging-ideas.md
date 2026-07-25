# ADR: Ideas futuras — timer de checkpoint y marcado de errores de contenido

**Estado:** Propuesto (no aceptado — captura de ideas para brainstormear en
detalle antes de implementar, no una decisión de arquitectura tomada).

## Contexto

Durante el brainstorming del fix de Ruta/SM-2 (2026-07-25), surgieron dos
ideas de producto no relacionadas con ese fix. Se decide no mezclarlas en
ese spec (mismo motivo por el que la Fase 2 completa se descompuso en
sub-fases) y dejarlas registradas acá para desarrollar cada una en su
propia sesión de brainstorming más adelante.

## Idea 1 — Timer en el checkpoint

**Motivación:** el examen real 1Z0-830 es con tiempo (50 preguntas en 90
minutos, ~1.8 min/pregunta en promedio). Un checkpoint sin límite de tiempo
no entrena esa condición del examen real.

**Opinión preliminar:** vale la pena, pero **como presupuesto de tiempo
total del checkpoint, no un timer por pregunta individual** — así el
jugador se autoadministra el tiempo como en el examen real, en vez de que
un `parsons`/`predict_output` (que naturalmente toma más leer/pensar) lo
penalice frente a un `mcq`. Debería aplicar solo a checkpoints (examen
simulado), no a la práctica diaria — la sesión diaria de bajo estrés es
parte del diseño original (repetición espaciada sin presión, para sostener
el hábito).

**Preguntas de diseño pendientes (no resueltas acá):**
- ¿Aplica a ambos tipos de checkpoint (voluntario y examen de ubicación),
  o solo a uno?
- ¿Qué pasa cuando se acaba el tiempo — auto-envío de lo respondido hasta
  ese momento, o corte duro?
- ¿Cómo se calibra el presupuesto total (fijo, o proporcional a la
  cantidad de preguntas del checkpoint)?
- Diseño visual del contador.

## Idea 2 — Marcar un ejercicio como "esto parece incorrecto"

**Motivación:** al escalar contenido a las 12 secciones del roadmap
(`docs/superpowers/specs/2026-07-24-fase2-3-content-scaling-design.md`),
errores de contenido (typos, respuesta mal etiquetada) se vuelven más
difíciles de detectar solo por Luis.

**Opinión preliminar:** un mecanismo general de "reportar bugs de la app"
no se justifica hoy — Luis es el único usuario real, y el canal actual
(reportarlo directamente en conversación) ya funciona bien y es inmediato.
Lo que sí tiene valor real: un marcador **acotado a errores de contenido**
por ejercicio (no de la app en general), dado que ese es un riesgo
concreto y creciente con el escalado de contenido, distinto de "bugs de
la app".

**Preguntas de diseño pendientes (no resueltas acá):**
- ¿Dónde vive el marcado (nueva tabla Room, o algo más simple como un
  archivo/log)?
- ¿Qué información se captura (solo el id del ejercicio, o también qué
  pareció mal)?
- ¿Se revisa manualmente después, o hay algún flujo de correción asistida?

## Pendiente

Ambas ideas requieren su propio ciclo de brainstorming → spec → plan
cuando se decida priorizarlas. No se implementa nada de esto hasta
entonces.
