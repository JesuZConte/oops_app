# Roadmap Fase 2 — Contenido estructurado (Secciones/Unidades/Checkpoints)

> Artefacto de planificación arquitectónica. Traduce el ADR
> `docs/adrs/2026-07-20-content-structure-sections-checkpoints.md` en una
> secuencia de sub-proyectos, cada uno con su propio ciclo spec → plan →
> implementación. **No es un plan de implementación** — es la descomposición
> y el orden.

## Por qué se descompone

El ADR toca cuatro subsistemas que hoy están acoplados en el modelo plano
`Topic → Exercise`:

1. **Modelo de datos** — introducir la jerarquía Sección → Unidad → Checkpoint
   sobre el `Topic` plano actual, con migración Room.
2. **Dominio** — nuevo tipo de sesión (checkpoint), lógica de gating (examen de
   ubicación al 68%), interacción con el motor SM-2 existente.
3. **UI** — Ruta pasa de líneas de dominio planas a un camino Sección→Unidad
   estilo Duolingo; Session renderiza tipos de ejercicio nuevos; Home refleja la
   jerarquía; flujo de checkpoint (voluntario y de ubicación).
4. **Contenido** — curación real: mapear capítulos del libro + features Java
   21/22-25 a Secciones/Unidades, y autoría de ejercicios.

Meterlo todo en un plan sería inmanejable. Se descompone en fases secuenciadas.

## Enfoque elegido: rebanada vertical

Decisión del usuario (2026-07-20): en vez de construir el motor completo en
abstracto y después el contenido, se hace primero una **rebanada vertical** que
pruebe el modelo end-to-end con contenido real y poco volumen. Menos riesgo de
construir una abstracción que el contenido real luego no calza, y de-riesga la
migración temprano.

## Fases

### Fase 2.1 — Fundación + rebanada vertical *(siguiente: spec en detalle)*

El modelo de datos es el cuello de botella (contenido y UI dependen de su
forma). Se valida con una rebanada delgada end-to-end:

- Modelo de datos mínimo para Sección → Unidad → Checkpoint + migración Room.
- Formato de contenido v2 (JSON packs con jerarquía).
- **Reorganizar Streams** en unidades — se incluye aquí a propósito porque
  fuerza a la migración a preservar el `review_state` SM-2 existente (no es
  greenfield); es el de-risking más importante de esta fase.
- Curar **Sección 1 (Fundamentos)** — pocas unidades, para validar el modelo con
  contenido nuevo.
- Use case de checkpoint (voluntario + examen de ubicación) con gate al 68%.
- Ruta y Session actualizadas al camino Sección→Unidad para estas dos secciones.

**Entregable:** rebanada jugable y testeable, con Fundamentos y Streams
conviviendo bajo el modelo nuevo.

### Fase 2.2 — Diversidad de tipos de ejercicio

Tipos del ADR (concepto/entrevista, clasificación de código, escenarios
prácticos con andamiaje variable). Empezar **constreñido** (fill_blank, opciones,
ordenar tipo Parsons) y **diferir la escritura libre de código desde cero**
(ver riesgo 1).

### Fase 2.3 — Escalado de contenido

Mapear y curar el resto de secciones (capítulos 2-11 del libro + features Java 21
de examen + extras 22-25 etiquetados como "moderno"), **incrementalmente, una
sección a la vez**. Meta arquitectónica: agregar una sección = un JSON pack nuevo
+ registrar la sección, sin tocar el motor.

### Fase 2.4 — Pulido

Del roadmap original (`PROJECT-OOPS.md` sección 11): estadísticas de áreas
débiles, notificación diaria. Independiente; puede ir en cualquier momento tras
2.1.

## Dependencias

```
2.1 Fundación ──┬──> 2.2 Tipos de ejercicio ──> 2.3 Escalado de contenido
                └──> (UI y contenido de 2.3 dependen del formato v2 de 2.1)
2.4 Pulido: independiente, tras 2.1
```

## Riesgos arquitectónicos a resolver en el spec de la Fase 2.1

1. **Grading de código libre** — sin compilador embebido hasta Fase 3, evaluar
   respuestas de código libres es difícil. El andamiaje "desde cero" del ADR
   arranca como hueco-a-rellenar / selección / ordenar; la escritura libre real
   se difiere.
2. **Migración del `review_state`** — no perder el progreso SM-2 de Streams al
   reorganizarlo en unidades. Es la parte más delicada de 2.1.
3. **Qué significa "completar" una unidad saltada** — cuando el examen de
   ubicación marca unidades como completas, ¿se siembra `review_state` para esos
   ejercicios? Si no, nunca resurgen en el SM-2 diario.
4. **Interacción checkpoint ↔ SM-2** — ¿los ejercicios de un checkpoint
   actualizan su estado SM-2, o el checkpoint es solo un gate aparte?

## Estado

- ADR aceptada: `docs/adrs/2026-07-20-content-structure-sections-checkpoints.md`.
- Enfoque de secuencia decidido: rebanada vertical.
- **Siguiente paso:** brainstorming → spec detallado de la Fase 2.1, resolviendo
  los 4 riesgos de arriba, luego plan de implementación (writing-plans) y
  ejecución (subagent-driven-development).
