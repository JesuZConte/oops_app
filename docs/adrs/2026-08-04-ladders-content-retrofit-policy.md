# ADR: Política de retrofit de escaleras y orden del roadmap de contenido

**Estado:** Aceptado

## Contexto

`docs/adrs/2026-07-30-self-teaching-path-vision.md` y la rebanada 1 de
"escaleras aprender-haciendo" (`docs/superpowers/specs/2026-07-30-learn-by-doing-ladders-slice1-design.md`,
mergeada 2026-08-04) establecieron el modelo de escaleras de primera
exposición (`worked_example` → `guided` → `solo`) como parte de
`PROJECT-OOPS.md`, la constitución del proyecto: "Learn by doing, not
read-then-practice." Ese giro de visión reemplaza la premisa original de
"compañero de práctica" (asume que el jugador ya tiene el libro de
referencia) por un Path autosuficiente.

La rebanada 1 solo re-autoró **una unidad piloto** (`streams-collectors`)
para probar el motor end-to-end. Las otras **19 unidades ya shippeadas**
(secciones Fundamentos de Java, Generics y Colecciones, Streams y lambdas
—las 3 unidades restantes—, Manejo de Excepciones, Concurrencia) siguen sin
escaleras. Al mismo tiempo, el proceso vigente para las secciones que
faltan del roadmap (`docs/superpowers/specs/2026-07-24-fase2-3-content-scaling-design.md`,
`project_fase2_3_status` en memoria) es explícitamente anterior al giro de
visión: "cada sección futura repite este mismo proceso liviano — no se
necesita brainstorming nuevo, solo un plan de autoría de contenido" — es
decir, seguiría produciendo unidades sin escaleras si no se corrige. (El
roadmap de secciones en sí también resultó estar mal derivado — corregido
por separado en `docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`,
que pasa de 12 a 13 secciones; esta ADR no depende de ese número exacto.)

Sin esta ADR, el Path terminaría con un estilo de enseñanza inconsistente:
un jugador nuevo que lo recorre de punta a punta tendría escaleras en
`streams-collectors` y en las secciones futuras, pero no en el resto — una
contradicción directa con la promesa de "self-sufficient, learn by doing"
de la constitución, no un detalle cosmético.

Las escaleras solo se muestran en la primera exposición a un concepto (el
`worked_example` antes de `guided`/`solo`); una vez "nacido" un concepto
para un jugador, retrofitear su unidad no cambia nada para ese jugador. El
valor del retrofit es para cualquier instalación futura desde cero — la
condición de una app que se sostiene sola, sin depender de qué tan lejos
llegó Luis con el contenido viejo.

## Decisión

### 1. Política: todo el contenido converge a escaleras, sin excepciones

Ninguna unidad — existente o futura — queda sin escaleras de primera
exposición para sus conceptos. Se retira la excepción de "sección nueva =
proceso liviano sin escaleras" que tenía Fase 2.3 desde antes del giro de
visión: de ahora en más, el "plan de autoría de contenido" liviano de cada
ciclo de sección incluye escaleras por defecto, igual que ya incluye la
guía de "3 sabores" de pregunta establecida en Fase 2.3.

**Criterio por concepto:** todo ejercicio con `conceptId` recibe su
`worked_example`. Uniforme, sin juicio caso-por-caso de "esto es trivial"
— evita un criterio subjetivo que envejezca mal o quede inconsistente
entre unidades autoradas en momentos distintos.

### 2. Orden: retrofit de lo existente antes que contenido nuevo

El retrofit de las 19 unidades ya shippeadas va **antes** que la sección 6
(Modulos) del roadmap. El roadmap de contenido nuevo queda pausado hasta
terminar el retrofit — evita seguir agregando secciones que ya nacerían
"correctas" mientras el resto del Path queda desalineado con la
constitución por más tiempo del necesario.

### 3. Ejecución: un ciclo liviano por sección, no un plan monolítico

19 unidades es demasiado para un solo ciclo de implementación. Se
decompone con el mismo patrón ya probado en Fase 2.3 (roadmap de secciones
+ un ciclo liviano de autoría por sección, sin brainstorming repetido): un
ciclo de retrofit por sección, en el orden original del roadmap:

1. Fundamentos de Java
2. Generics y Colecciones
3. Streams y lambdas (3 unidades restantes — `streams-collectors` ya está
   hecha)
4. Manejo de Excepciones
5. Concurrencia

Cada ciclo repite: plan de autoría de contenido (análogo a
`docs/superpowers/plans/2026-07-28-fase2-3-exception-handling.md`) →
subagent-driven-development de una sola tarea (transcripción de contenido,
mismo patrón implementador+revisor haiku ya usado en los últimos ciclos de
Fase 2.3) → revisión → merge → bump de `CURRENT_CONTENT_VERSION` → QA en
el dispositivo real de Luis.

**Enmienda: "retrofit" es más que agregar `worked_example`.** Al planificar
el Ciclo 1 se encontró que las 5 secciones núcleo ya shippeadas tienen
gaps reales de cobertura contra los objetivos verdaderos de 1Z0-830, no
solo contra el índice del libro de Java 11 que originó el roadmap viejo —
ver `docs/adrs/2026-08-04-1z0-830-roadmap-correction.md` (ADR hermana,
auditoría completa de las 5 secciones + las nuevas). Por lo tanto, cada
uno de los 5 ciclos de este documento **entrega escaleras y cobertura
completa de objetivos en la misma pasada**, con el detalle concreto de
qué le falta a cada sección viviendo en esa ADR (no acá, para no
duplicar/desincronizar). El orden de los 5 ciclos no cambia — siguen
siendo "primero" porque son las primeras secciones del Path, no porque
sean más baratas de tocar.

**Sabor "entrevista" — framing de empresa:** las preguntas de la
categoría "interview/judgment" (guía de 3 sabores de Fase 2.3) usan
framing genérico ("una consultora IT grande", "una empresa de servicios
financieros") en vez de nombrar marcas reales — evita cualquier
asociación de marca innecesaria en contenido que eventualmente puede
volverse público (visión bilingüe EN+ES).

Se mantienen sin cambios las reglas ya establecidas en la rebanada 1:

- **Ids de ejercicio preservados byte a byte** al re-autorar cada unidad,
  para no perder el `review_state` real de Luis sobre esos ejercicios.
- **Bump de versión de contenido por ciclo de sección, no todo junto al
  final** — permite detectar cualquier problema de re-siembra temprano en
  vez de acumular el riesgo de 5 secciones en un solo bump.
- Ningún cambio de código Kotlin/UI esperado — confirmado por el mismo
  hecho arquitectónico que Fase 2.3 ya estableció: agregar/editar
  secciones es contenido puro (JSON), el motor ya es genérico sobre
  cantidad de secciones y sobre la presencia de `worked_example`.

### 4. Fuera de alcance de esta ADR

- La guía de "3 sabores" de contenido (exam/syntax, clasificación,
  interview/judgment) — eje de diseño de contenido distinto al de
  escaleras, no se revisa acá.
- La rebanada 1b de escaleras (tope de contenido nuevo por día + estado
  "no hay nada nuevo hoy") — depende de una fecha de nacimiento de
  concepto que hoy no existe (el "nacido" se deriva en vivo de
  `review_state`, no se persiste), probablemente una migración de Room.
  Sigue como ítem de roadmap separado.
- El resto del backlog (tests de humo Compose Nivel 2, contenido EN+ES,
  marcado de errores de contenido, simulacro de examen completo).

## Consecuencias

- El roadmap de las secciones restantes (Modulos en adelante, ver ADR de
  corrección de roadmap) se pospone hasta terminar los 5 ciclos de
  retrofit.
- El "próximo paso natural" que dejó el CHANGELOG del 2026-08-04
  ("Rebanada 2+: re-autorar las 18 unidades restantes como escaleras,
  sección por sección") queda formalizado por esta ADR como el trabajo
  inmediato, con orden y proceso explícitos.
- El primer ciclo de ejecución (retrofit de Fundamentos de Java) se
  planifica a continuación de esta ADR, siguiendo `superpowers:writing-plans`.
