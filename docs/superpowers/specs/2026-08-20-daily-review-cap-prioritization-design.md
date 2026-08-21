# Tope y priorizacion del repaso diario (Fase B de GetTodaySessionUseCase) — Diseño

**Estado:** Aprobado, pendiente de plan de implementacion.

## Contexto

Bug reportado por Luis en vivo: al tocar "ESTUDIAR HOY" aparecieron mas de
280 preguntas en una sola sesion, algo sin sentido para un dia. Investigado
con `systematic-debugging` antes de proponer cualquier fix (ver
[[project_daily_review_cap_status]] para el detalle completo de la
investigacion):

**Causa raiz confirmada** (no es un bug de implementacion -- el codigo
hace exactamente lo que dice hacer, es un vacio de diseño):
`GetTodaySessionUseCase.kt:14` arma la sesion diaria en 2 fases:
- **Fase A** (contenido nuevo, ladder): `selectPathCandidates(...).take(
  newExercisesLimit)`, capada a 5 por defecto.
- **Fase B** (repaso SRS): `exerciseRepository.getDueExercises(today,
  limit = Int.MAX_VALUE)` -- **sin ningun tope**.

`ExerciseDao.getDue` es `SELECT ... WHERE review_state.dueDate <= :today`,
sin `ORDER BY`. Trae todo lo atrasado (no solo lo que vence exactamente
hoy), sin limite ni orden.

Verificado con la base real del dispositivo de Luis: de 306 filas en
`review_state`, 267 tenian `dueDate <= hoy` -- acumuladas en rafagas
(94 en un dia, 39 en otro) por haber jugado/probado mucho contenido en
sesiones de QA concentradas en pocos dias. SM-2 asigna intervalos cortos
al principio (1 dia tras el primer acierto), asi que una rafaga de
respuestas el mismo dia produce una pila de vencimientos casi juntos
pocos dias despues. Nunca se habia notado porque nunca se habia
acumulado un backlog tan grande de una vez.

## Diseño

### 1. Priorizacion en SQL: "los mas debiles primero"

`ExerciseDao.getDue` gana `ORDER BY review_state.repetitions ASC,
review_state.lastReviewedAt DESC, review_state.easeFactor ASC,
exercises.id ASC`.

Justificacion tecnica (via `SchedulerSm2.kt`): un row de `review_state`
solo se guarda DESPUES de pasar por el scheduler. Un acierto
(`quality >= 3`) sube `repetitions` a minimo 1 en esa misma escritura;
`repetitions = 0` en un row guardado solo puede significar que la ultima
respuesta fue una falla (el scheduler resetea `repetitions` a 0 cuando
`quality < 3`). Por lo tanto ordenar por `repetitions ASC` pone primero
lo que fallo mas recientemente o lo que menos se ha consolidado.

Es una jerarquia estricta, no un puntaje combinado: `repetitions` decide
primero, y `easeFactor` solo desempata DENTRO del mismo valor de
`repetitions` (confirmado explicitamente por Luis via AskUserQuestion,
descartando un puntaje ponderado que mezclara ambos numeros en uno solo
-- mas simple y consistente con el argumento semantico de arriba).

**`lastReviewedAt DESC` como segundo criterio** (agregado tras revision
del advisor, antes de `easeFactor`): sin esto, el ranking por
`repetitions` solo entierra el contenido recien introducido por Fase A.
Verificado contra el backlog real de Luis: de 267 filas atrasadas, 194
tienen `repetitions` en {0, 1} -- un ejercicio recien aprendido ayer
(`repetitions = 1`, `dueDate = mañana`) cae en el mismo bucket de
`repetitions` que 164 items del backlog de QA, y un acierto reciente
empuja `easeFactor` hacia atras en el orden ascendente (ej. 2.5 -> 2.6),
no hacia adelante. Con 10 cupos, cada item de ayer tendria muy poca
chance real de reaparecer antes de que el backlog de 267 se drene
(~27 dias). `lastReviewedAt DESC` hace que, dentro del mismo nivel de
`repetitions`, lo revisado mas recientemente gane su bucket sobre lo mas
antiguo -- protegiendo el refuerzo del dia siguiente que es el objetivo
completo de la progresion `worked_example` -> `guided` -> `solo`/
`practice` del ladder.

`easeFactor ASC` sigue como tercer criterio: prioriza, dentro de un
mismo `repetitions` y "frescura" de revision, lo que historicamente ha
costado mas (el scheduler nunca deja `easeFactor` bajar de 1.3, y baja
mas cuanto peor la respuesta). `exercises.id ASC` cierra como ultimo
desempate solo para que el orden sea deterministico ante empates
exactos en los tres criterios anteriores (relevante para los tests a
nivel `ExerciseDao`, no cambia el comportamiento observable en la app).

La antiguedad del vencimiento (dias desde `dueDate`) NO entra en este
puntaje de debilidad -- ver el mecanismo separado de la seccion 3, que
cubre antiguedad con una regla propia en vez de mezclarla en el mismo
`ORDER BY`.

### 2. Tope: `dueExercisesLimit: Int = 10`

Sesion diaria de repaso queda acotada a un maximo de 10 items (mas los 5
de Fase A, sesion total ≤15), en linea con el tamaño que Luis ya jugaba
antes de que el backlog se acumulara.

### 3. Valvula de escape por antiguedad: 1 cupo reservado, sorteado, entre los vencidos hace 45+ dias

Refinamiento sobre la propuesta inicial (que dejaba la antiguedad
completamente fuera): sin esto, un item facil (ease alto, muchas
repeticiones) que ya no es debil podria quedar esperando su turno
indefinidamente si el backlog de items debiles nunca se vacia del todo
-- el ranking por debilidad nunca lo dejaria pasar. Luis pidio una
garantia minima: que un item vencido, por facil que sea, eventualmente
se vuelva a ver.

**Mecanismo exacto** (decidido explicitamente por Luis, con una
correccion respecto a mi primera propuesta de meterlo en el mismo
`ORDER BY`): un umbral de 45 dias define **elegibilidad**, no prioridad
directa -- cualquier item con `dueDate <= hoy - 45 dias` entra al pool de
"candidatos antiguos". De ese pool se sortea **exactamente 1** al azar
para ocupar **1 solo cupo** de los 10 de la sesion. Los otros 9 cupos
(o los 10, si el pool de candidatos antiguos esta vacio) se llenan con
el ranking normal de debilidad de la seccion 1, excluyendo el id que ya
gano el cupo antiguo (para no repetir el mismo ejercicio dos veces en la
misma sesion).

Deliberadamente NO es "cualquier item de 45+ dias entra" -- eso
inundaria la sesion con todo lo que cruzo el umbral el mismo dia y
reemplazaria el criterio de debilidad por pura antiguedad, que es
justo lo que Luis pidio evitar. El umbral filtra QUIENES PUEDEN competir
por el cupo; el sorteo decide CUAL de ellos lo gana; sigue siendo 1 solo
cupo pase lo que pase.

Con 267 items atrasados hoy, cualquiera de los que ya superen 45 dias de
atraso empieza a tener una oportunidad real de aparecer -- y como el
resto del backlog se sigue drenando por debilidad, el pool de
"candidatos antiguos" no crece sin limite: en cuanto un item gana su
cupo y se responde, sale del pool (su `dueDate` se recalcula hacia
adelante).

### 4. Randomizacion del orden de presentacion

Los (hasta) 10 items finales de Fase B (9 o 10 por debilidad + el 1 del
cupo antiguo, si gano alguno) se mezclan entre si antes de combinarse
con la Fase A, para que la sesion no siempre empiece con "el mas dificil
primero" ni delate cual fue el pick antiguo por su posicion. Decision
explicita de Luis via AskUserQuestion: la randomizacion de PRESENTACION
es distinta de la randomizacion de SELECCION (seccion 3) -- ambas usan
el mismo generador `Random` inyectado, pero cumplen roles distintos: una
elige QUIEN gana el cupo antiguo, la otra decide en que ORDEN se ve todo
Fase B ya elegido.

Para que esto sea testeable deterministicamente, `GetTodaySessionUseCase`
recibe un `kotlin.random.Random` inyectable (mismo patron ya usado en
este codebase para `today: LocalDate` -- nunca se lee el reloj/generador
directo dentro del use case, siempre se recibe como parametro con un
valor por defecto para produccion). Firma resultante:

```kotlin
suspend operator fun invoke(
    today: LocalDate,
    newExercisesLimit: Int = 5,
    dueExercisesLimit: Int = 10,
    staleThresholdDays: Long = 45,
    random: Random = Random.Default
): List<Exercise>
```

El orden final de la lista devuelta sigue siendo: repaso (Fase B,
acotado+priorizado+con su cupo antiguo+mezclado) primero, luego nuevo
(Fase A, sin cambios, sigue en `pathOrder` estricto -- mezclar el
contenido nuevo del ladder rompería la progresion pedagogica
intro->guided->solo).

`GetCheckpointSessionUseCase` sigue pidiendo `getDueExercises(today,
Int.MAX_VALUE)` sin cambios -- solo usa el resultado para armar un `Set`
de ids a excluir/incluir en el pool del checkpoint, nunca le importo el
orden ni el tope, asi que el nuevo `ORDER BY` no lo afecta en absoluto.

### 5. Drenaje del backlog: sin logica nueva, efecto secundario del tope

Los items atrasados que no entren en la seleccion de un dia (ni por
debilidad ni por el cupo antiguo) siguen con su `dueDate` en el pasado
-- vuelven a competir al dia siguiente junto con lo que se sume nuevo.
No hace falta ningun mecanismo de "carryover" explicito: es una
consecuencia directa de que `dueDate <= hoy` sigue siendo cierto hasta
que el item se responda. El backlog de 267 se consumiria en ~27 dias a
10/dia, priorizando lo mas debil primero salvo el cupo antiguo diario.

## Fuera de alcance (deliberado)

- **Ponderar antiguedad como parte continua del puntaje de debilidad**
  (ej. sumarla como un tercer criterio de `ORDER BY`): descartado a
  favor del mecanismo de cupo unico + sorteo de la seccion 3 -- Luis
  fue explicito en que la antiguedad NO debe reemplazar ni diluir el
  criterio de debilidad para el grueso de la sesion, solo garantizar
  que nada quede olvidado para siempre via un cupo aislado.
- **Ajustar el tope de Fase A** (contenido nuevo, sigue en 5): no
  reportado como problema, fuera del alcance de este fix.
- **Cambiar el algoritmo SM-2 en si** (`SchedulerSm2.kt`): el problema no
  es como se calculan `easeFactor`/`intervalDays`/`dueDate`, es que
  Fase B nunca tuvo tope. El scheduler queda intacto.
- **Mas de 1 cupo antiguo, o un tamaño de cupo que escale con el
  backlog**: Luis fue explicito en que es 1 cupo fijo, no un porcentaje
  ni un numero que crezca -- mantiene el mecanismo como una valvula de
  escape minima, no como un segundo criterio de peso comparable a la
  debilidad.
- **Persistir o exponer el `Random` inyectado mas alla de esta invocacion**:
  el parametro es solo para testabilidad determinista del shuffle, no un
  cambio de arquitectura mayor.

## Cambios de codigo requeridos

- `app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt`:
  - `getDue` gana `ORDER BY review_state.repetitions ASC,
    review_state.lastReviewedAt DESC, review_state.easeFactor ASC,
    exercises.id ASC` (sin cambiar su firma ni su `WHERE`).
  - Nueva query `getStale(cutoff: Long): List<ExerciseEntity>`, mismo
    `INNER JOIN` que `getDue` pero `WHERE review_state.dueDate <=
    :cutoff` y sin `ORDER BY` (la seleccion dentro del pool es aleatoria,
    no importa el orden en que SQL los devuelva).
- `app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt`
  y `ExerciseRepositoryImpl.kt`: nuevo metodo `getStaleExercises(cutoff:
  LocalDate): List<Exercise>` que delega a `exerciseDao.getStale(cutoff
  .toEpochDay())`. `getDueExercises(today, limit)` no cambia de firma.
- `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt`:
  - Nuevos parametros `dueExercisesLimit: Int = 10`, `staleThresholdDays:
    Long = 45`, `random: Random = Random.Default`.
  - Calcula `staleCutoff = today.minusDays(staleThresholdDays)`, pide
    `getStaleExercises(staleCutoff)`, sortea 1 (`.shuffled(random)
    .firstOrNull()`) como `agedPick`.
  - Pide `getDueExercises(today, Int.MAX_VALUE)` (ya viene ordenado
    debil-primero por el nuevo `ORDER BY`), filtra fuera el id de
    `agedPick` si gano alguno, y toma `dueExercisesLimit - (si agedPick
    != null, 1, si no 0)` de ese resultado filtrado.
  - Combina `agedPick` (si existe) + esa seleccion por debilidad,
    mezcla el conjunto completo con `.shuffled(random)`, y antepone el
    resultado a Fase A como antes.
- `GetCheckpointSessionUseCase` no se toca -- sigue pidiendo
  `getDueExercises(today, Int.MAX_VALUE)`, inmune al nuevo `ORDER BY` y
  ajeno al nuevo `getStaleExercises`.
- `CURRENT_CONTENT_VERSION` **no** se toca -- este es un fix de motor,
  no un cambio de contenido.

## Testing

TDD, extendiendo `GetTodaySessionUseCaseTest.kt` (ya cubre extensivamente
Fase A) y su `FakeExerciseRepositoryForSession` (gana soporte para
`getStaleExercises`). Casos nuevos para Fase B:
- Con mas items debido que `dueExercisesLimit` y ningun candidato
  antiguo (pool de `getStaleExercises` vacio), solo se devuelven los N
  mas debiles (menor `repetitions`, luego `lastReviewedAt` mas reciente,
  luego menor `easeFactor` como desempate) -- el resto atrasado NO
  aparece, y no se reserva cupo.
- Con dos items del mismo `repetitions` pero `lastReviewedAt` distinto
  (uno revisado ayer, el otro hace semanas), el revisado mas
  recientemente aparece primero -- prueba directa de que el contenido
  recien introducido por Fase A no queda enterrado detras del backlog
  de QA acumulado en el mismo bucket de `repetitions`.
- Con al menos un candidato antiguo, exactamente 1 de los N resultados
  es del pool antiguo (verificable con un `Random` fijo que determine
  cual gana), y los otros N-1 siguen viniendo del ranking de debilidad,
  excluyendo el id que ya gano el cupo (no debe repetirse).
- Con multiples candidatos antiguos elegibles, un `Random` fijo distinto
  hace ganar a un candidato distinto -- prueba que la seleccion del cupo
  es realmente aleatoria y no, por ejemplo, siempre "el primero de la
  lista".
- Con un `Random` fijo inyectado, el orden de presentacion final del
  conjunto de Fase B (debilidad + cupo antiguo ya combinados) es
  verificable (no solo el conjunto).
- `GetCheckpointSessionUseCase` sigue recibiendo el set completo de ids
  debido sin cambios (test de regresion, no deberia requerir tocar su
  test existente ya que `getDueExercises` no cambia de firma ni de
  contrato para `limit = Int.MAX_VALUE`).
- El orden relativo Fase B (repaso) antes de Fase A (nuevo) se preserva,
  igual que el test existente `session lists due exercises before new
  ones`.

La priorizacion en SQL (`ORDER BY` de `getDue`, `WHERE` de `getStale`) se
verifica con un test de `ExerciseDao` a nivel Room (o, si el proyecto
prefiere mantener eso invisible a nivel unitario y confiar en el test de
`GetTodaySessionUseCase` con un fake repository que ya devuelva la lista
pre-ordenada/pre-filtrada, seguir el patron existente de
`FakeExerciseRepositoryForSession` -- decision para el plan de
implementacion, no bloqueante para este spec).

## QA

Fix de motor puro (sin cambio de contenido), mismo patron que
[[project_session_unit_extension_fix_status]] y
[[project_locked_unit_checkpoint_routing_fix_status]]: TDD directo en el
arbol de trabajo principal (o un worktree si se prefiere dado el tamaño),
sin necesidad de un ciclo SDD completo de autoria de contenido. QA en
dispositivo: confirmar que "ESTUDIAR HOY" con el backlog real de 267
items devuelve una sesion de tamaño razonable (≤15), y que los items
elegidos son consistentes con la logica de debilidad (verificable via
sqlite comparando `repetitions`/`easeFactor` de los elegidos vs los no
elegidos), y que el backlog se reduce dia a dia jugando sesiones
sucesivas.

**Nota sobre el cupo antiguo**: al momento de escribir este spec, el
item mas atrasado del backlog real de Luis lleva ~21 dias de atraso
(`dueDate` minimo en epoch 20664 vs hoy en epoch ~20686) -- nada cruza
todavia el umbral de 45 dias, asi que el mecanismo de la seccion 3 no
tendra ningun candidato elegible en el QA inicial y por diseño no
reservara cupo (comportamiento correcto: pool vacio = los 10 cupos van
integros a debilidad). Verificar el cupo antiguo en si mismo queda
cubierto por los tests unitarios con `Random` fijo y candidatos
sinteticos; en dispositivo real solo sera observable una vez que algun
item real cruce los 45 dias, no antes.
