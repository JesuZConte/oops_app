# Checkpoint obligatorio, acumulativo, creciente y cronometrado — Diseño

**Estado:** Aprobado, pendiente de plan de implementación.

## Contexto

Hoy el checkpoint de fin de sección es **voluntario**: el ADR
`docs/adrs/2026-07-20-content-structure-sections-checkpoints.md` (línea 42)
lo define como "repaso voluntario… **no bloquea el avance**". En la
práctica, completar todas las unidades de una sección desbloquea la
siguiente sola (`GetLearningPathUseCase`: sección completa = todas sus
unidades completas), sin pasar nunca por el checkpoint.

Problema que Luis identificó: nunca se prueba de verdad si retienes lo de
una sección antes de avanzar, y la sesión diaria se vuelve cada vez más un
"resumen aleatorio de todo" sin un examen real que cierre cada sección.
Cada sección debería preguntar sobre su tema; el checkpoint debería
preguntar sobre **todo lo recorrido**, y ser cada vez más parecido al
examen real (más acumulativo, con presión de tiempo).

**Esta spec enmienda la decisión "no bloquea el avance" del ADR
2026-07-20**: el checkpoint de sección pasa a ser obligatorio, acumulativo
de verdad, creciente y cronometrado.

## Decisión (resumen)

1. El checkpoint de sección es **obligatorio** para desbloquear la
   siguiente sección. No aprobarlo no bloquea la práctica diaria, pero sí
   el avance.
2. Es **acumulativo**: muestrea de todas las secciones recorridas, con más
   peso en la recién terminada y en tus ítems débiles (vencidos).
3. **Crece** con el avance (con piso y techo), aproximándose al examen real
   sin llegar a ser el simulacro completo (ese es un modo aparte, futuro).
4. Es **cronometrado**: presupuesto de tiempo total (no por pregunta),
   proporcional a la cantidad de preguntas (~1.8 min/pregunta, ritmo real
   del 1Z0-830).
5. Al **reprobar**, no avanzas y no hay tope de intentos; el reintento se
   habilita solo tras re-estudiar en la práctica diaria lo que fallaste.
6. El **streak no se ve afectado** por aprobar/reprobar un checkpoint.
7. Umbral: se mantiene **68%** (el real del 1Z0-830, ADR línea 45).

## 1. Gate obligatorio

Una sección pasa a estar "completa" (y por tanto desbloquea la siguiente)
solo cuando **todas sus unidades están completas Y su checkpoint fue
aprobado** (≥68%). Hoy el gate es solo "todas las unidades completas".

- **Estado nuevo requerido:** "¿el checkpoint de la sección X fue
  aprobado?". Los intentos ya se persisten vía
  `CheckpointRepository.recordAttempt(sectionId, kind, scorePct, passed,
  takenAt)`, pero esa interfaz **hoy solo escribe, no lee** — hay que
  agregar una query de lectura ("¿existe algún intento aprobado de tipo
  `review` para la sección X?"). No requiere una tabla nueva para *esta*
  parte, solo exponer lo que ya se guarda. `GetLearningPathUseCase` pasa a
  consultar ese estado al calcular `sectionComplete`.
- **Cuándo se ofrece / se puede diferir:** al terminar la última unidad de
  una sección, la siguiente queda bloqueada hasta aprobar el checkpoint —
  pero **no se obliga a rendirlo en ese instante**. El jugador puede seguir
  haciendo práctica diaria y rendirlo cuando se sienta listo (dejar que
  consolide es deseable para la retención).
- **Interacción con el examen de ubicación:** aprobar el examen de
  ubicación sobre secciones saltadas **cuenta como aprobar el checkpoint de
  esas secciones** (satisface el gate para ellas). El examen de ubicación
  es un test más exigente que cubre varias unidades saltadas; sería
  incoherente pedirlo dos veces. Mecánicamente: una sección cuyas unidades
  se completaron todas vía `PLACEMENT` debe tratarse como
  checkpoint-satisfecho (opción: registrar un intento aprobado sintético al
  desbloquear, o que el gate reconozca la completitud-vía-placement como
  equivalente). A resolver en el plan.

## 2. Contenido acumulativo y creciente

El ADR ya concebía el checkpoint como acumulativo (línea 38: "mezcla
ejercicios de la Sección recién completada con una muestra de Secciones
anteriores"). Hoy la implementación (`GetCheckpointSessionUseCase`) está
desbalanceada hacia lo nuevo: ~12 preguntas, hasta 3 de secciones
anteriores y el resto de la actual. Se rebalancea:

- **Ponderado, no uniforme:** ~50% de la sección recién terminada (lo menos
  consolidado), ~50% repartido de todo lo anterior, con **sesgo hacia tus
  ítems vencidos** (los que el motor SM-2 marca como debidos = tus áreas
  más débiles). Así se cumple el deseo de "repetir donde más fallas" sin
  lógica adaptativa nueva: el sesgo hacia vencidos ya apunta a tus fallos.
- **Crece con el avance, con piso y techo.** La cantidad de preguntas
  escala con cuántas secciones has recorrido, para que el checkpoint se
  vuelva "cada vez más parecido al examen real". Parámetros **propuestos
  (revisables)**:
  - Piso: **8** preguntas (que los primeros nunca sean triviales).
  - Crecimiento: **+2** por sección recorrida.
  - Techo: **20** preguntas (que el de la sección 10 no sea un monstruo de
    50 — para eso está el simulacro completo, futuro, fuera de alcance).
  - Ejemplos (con `nPreguntas = 8 + 2×(seccionesRecorridas − 1)`, tope 20):
    sección 2 → 10q; sección 5 → 16q; sección 7+ → 20q (tope).
- **Distinción con el simulacro completo (futuro):** el checkpoint de
  sección es un gate acotado y creciente; el simulacro de examen real
  (~50q/90min, modo aparte) queda fuera de esta spec (ver "Fuera de
  alcance").

## 3. Cronometrado

- **Presupuesto de tiempo total**, no timer por pregunta — así el jugador
  se autoadministra el tiempo como en el examen real, y un
  `parsons`/`predict_output` (que naturalmente toma más leer) no queda
  penalizado frente a un `mcq`. (Confirma la opinión preliminar del ADR
  `2026-07-25-checkpoint-timer-and-content-flagging-ideas.md`, que con esta
  spec pasa de "propuesto" a "aceptado".)
- **Fórmula propuesta (revisable):** `presupuesto = round(nPreguntas ×
  1.8) minutos`, el ritmo real del 1Z0-830 (50 preguntas / 90 min). Ej.:
  10q → 18 min; 20q → 36 min.
- **Al agotarse el tiempo:** auto-envío de lo respondido hasta ese momento;
  las no respondidas cuentan como incorrectas (realista respecto al examen
  real). No es un corte duro que expulse a mitad de pregunta.
- **Alcance del timer:** aplica al checkpoint de sección. El examen de
  ubicación probablemente también debería cronometrarse por consistencia,
  pero se confirma en el plan (su flujo de buffer/defer-on-fail es
  distinto).
- Diseño visual del contador: detalle de UI, se define en el plan.

## 4. Barrera de reintento

- Al reprobar: no avanzas, **sin tope de intentos** (nunca un callejón sin
  salida — la recuperación siempre es posible).
- Los ejercicios que fallaste ya quedan agendados como vencidos por el
  motor SM-2 existente (respuesta incorrecta → `quality=2` → vence mañana).
- **El reintento se habilita cuando cada ejercicio que fallaste en tu
  último intento reprobado ha sido re-expuesto (respondido de nuevo) en una
  sesión diaria posterior a ese intento.** Esto impone naturalmente al
  menos un día de espera, y garantiza que re-estudiaste tus fallos. Basta
  con **volver a verlo** — no se exige acertarlo (de la corrección se
  encarga el agendamiento SM-2; exigir acierto podría re-trancarte).
- **Persistencia nueva requerida:** hay que guardar, por intento reprobado,
  el conjunto de `exerciseId` que se fallaron (y la fecha), para poder
  evaluar la condición de re-exposición. Si es una tabla nueva o una
  extensión de la de intentos se define en el plan.
- **El reintento es un examen fresco**, no un "aquí están otra vez las que
  fallaste": se re-muestrea acumulativo desde cero. La remediación dirigida
  ya ocurre por el lado de la práctica diaria (que ahora resurfacea justo
  tus fallos). Así el checkpoint sigue siendo un test honesto.

## 5. Streak desacoplado

Aprobar o reprobar un checkpoint **no toca el streak**. El streak mide
hábito diario (estudiaste, no te saltaste un día); la maestría es otro eje.
Reprobar no debe penalizar el hábito.

## 6. Umbral

Se mantiene **68%** — el umbral real del 1Z0-830 (ADR línea 45), para
acostumbrar al estándar real desde el día uno.

## Riesgo / dependencia: error de contenido

El gate obligatorio significa que una pregunta con error (respuesta mal
etiquetada, salida esperada equivocada) podría **trancar el avance** en una
pregunta rota. Esto sube la prioridad del marcador de "esto parece
incorrecto" acotado a contenido (idea 2 del ADR
`2026-07-25-checkpoint-timer-and-content-flagging-ideas.md`). No se
construye en esta spec, pero se registra como dependencia. Mitigación
interina: Luis es el autor del contenido y puede corregir el JSON +
reinstalar si una pregunta rota lo tranca.

## Migración

Sin backfill, reinstalación limpia (mismo criterio que ciclos anteriores,
usuario único). Las secciones ya "completadas" sin checkpoint aprobado
pasarían a estar gated — irrelevante tras reinstalación limpia.

## Explícitamente fuera de alcance (roadmap, ciclos futuros)

Se registran en el ADR `2026-07-25-checkpoint-timer-and-content-flagging-ideas.md`:

- **Modo simulacro de examen completo** — standalone, a demanda,
  full-length (~50 preguntas / 90 min), sobre todo el temario,
  cronometrado. **Diagnóstico, no gate** (no bloquea nada, repetible):
  "¿ya estoy listo?". Idealmente alimenta SM-2 (los fallos vuelven a la
  rotación). Su propio ciclo brainstorming → spec → plan.
- **Preparación de entrevistas** — género distinto (razonamiento y criterio
  abierto, no certificación); se cruza con el problema diferido de evaluar
  código/texto libre sin compilador. Futuro lejano, su propia exploración.

## Testing

- **Tests de dominio/use case:** muestreo acumulativo-creciente
  (ponderación, piso, techo, sesgo a vencidos), lógica del gate (sección
  completa solo con checkpoint aprobado; placement satisface el gate),
  lógica de la barrera de reintento (habilita solo tras re-exposición de
  los fallos), y el cálculo del presupuesto de tiempo como función pura.
- **Tests de ViewModel:** los estados de timer (cuenta regresiva,
  auto-envío al agotarse) y de bloqueo/habilitación de reintento —
  reusando la infraestructura de tests de ViewModel ya construida
  (`app/src/test/java/com/zconte/oopsapp/testutil/`, Nivel 1 de la ADR de
  testing).
- **QA manual en dispositivo (reinstalación limpia):** terminar una
  sección y confirmar que la siguiente queda bloqueada hasta aprobar el
  checkpoint; reprobar a propósito y confirmar que no se puede reintentar
  hasta re-estudiar los fallos en la práctica diaria; confirmar que el
  contador corre y auto-envía al agotarse; confirmar que el examen de
  ubicación sigue satisfaciendo el gate de las secciones saltadas;
  confirmar que el streak no se ve afectado.

## Decisiones registradas

| Decisión | Elegido | Fecha |
|---|---|---|
| Checkpoint de sección | Obligatorio para avanzar (enmienda "no bloquea" del ADR 2026-07-20) | 2026-07-26 |
| Barrera de reintento | Re-exponer los fallos en práctica diaria; sin tope de intentos | 2026-07-26 |
| Contenido | Acumulativo, ponderado a lo nuevo + sesgado a vencidos | 2026-07-26 |
| Tamaño | Creciente con piso 8 / +2 por sección / techo 20 (revisable) | 2026-07-26 |
| Tiempo | Presupuesto total, ~1.8 min/pregunta; auto-envío al agotarse | 2026-07-26 |
| Streak | Desacoplado (reprobar no lo afecta) | 2026-07-26 |
| Umbral | 68% (real del 1Z0-830, sin cambio) | 2026-07-26 |
| Examen de ubicación | Aprobarlo cuenta como aprobar el checkpoint de las secciones saltadas | 2026-07-26 |
| Simulacro completo / entrevistas | Fuera de alcance, al roadmap | 2026-07-26 |
| Migración | Reinstalación limpia, sin backfill | 2026-07-26 |
