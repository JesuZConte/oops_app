# Resumenes de unidad ("Tips") — Diseño

**Estado:** Aprobado, pendiente de plan de implementación.

## Contexto

Jugando Concurrencia, Luis se encontró respondiendo preguntas sobre
material que nunca habia estudiado, "adivinando" la respuesta. Pregunta:
Oops! no tiene ninguna forma de aprender o repasar un concepto **antes**
de que te lo pregunten por primera vez — el unico mecanismo de aprendizaje
hoy es el campo `explanation` que se muestra **despues** de responder
(correcto o no).

Comparacion con Duolingo (que Luis pregunto explicitamente): Duolingo es
"aprender haciendo" por diseno, pero SI tiene una pieza que a Oops! le
falta — las **"Tips"**: una tarjeta corta de gramatica/concepto, opcional,
leible antes de empezar una leccion. No es un libro ni una enciclopedia,
es un resumen de 30 segundos por unidad.

Nota de producto: la vision original de Oops! (`docs/specs/PROJECT-OOPS.md`)
asume que el jugador tiene el libro de referencia al lado — Oops! es un
"companero de practica", no un reemplazo del libro. Esta spec no cambia esa
vision; agrega una red de contencion minima para cuando el jugador no tiene
el libro a mano en ese momento, sin convertir la app en un curso completo.

## Decisión (resumen)

1. **Acceso opcional**, no obligatorio: un boton "Ver resumen" junto a
   cada unidad jugable en Ver Ruta, que no bloquea ni interrumpe el flujo
   de juego.
2. **Retrofit completo**: se escribe el resumen para las 19 unidades ya
   existentes (5 secciones), no solo las secciones futuras.
3. **El contenido vive en los mismos JSON de `content/*.json`** — no se
   persiste en Room, no requiere migracion.
4. **Formato**: parrafo corto (3-6 lineas) + un bloque de codigo de
   ejemplo, reutilizando el `CodeBlock` que ya usan los ejercicios.
5. **Presentacion**: pantalla completa nueva, con un boton "COMENZAR
   UNIDAD" al final.
6. **Solo para unidades jugables** (desbloqueadas o completadas) — no
   tiene sentido mostrar el resumen de una unidad bloqueada que todavia no
   se puede intentar.
7. **La busqueda del resumen vive detras de `ContentRepository`**, no en
   un use case que dependa directo de `ContentLoader` (correccion de
   diseno hecha en el self-review de esta spec: todo el resto del dominio
   solo depende de interfaces `domain/repository/*`, nunca de clases de
   `data/*` directamente — mantenerlo asi evita una excepcion a esa regla
   solo para esta feature).

## 1. Contenido y modelo de datos

Cada unidad en `content/*.json` gana un campo `summary` opcional:

```json
"summary": {
  "text": "Explicacion corta del concepto, 3-6 lineas, mismo estilo sin acentos que el resto del contenido.",
  "code": "// ejemplo de codigo ilustrativo, opcional"
}
```

`ContentPack.kt` gana:

```kotlin
@Serializable
data class UnitSummaryPack(
    val text: String,
    val code: String? = null
)

@Serializable
data class UnitPack(
    val unitId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int,
    val summary: UnitSummaryPack? = null,
    val exercises: List<ExerciseContent>
)
```

`summary` es **opcional a nivel de tipo** (nullable) como red de seguridad
para contenido futuro que lo olvide, pero para **esta** spec/plan es
requisito de contenido: las 19 unidades existentes + toda unidad nueva de
ahora en adelante deben traer su `summary` real. Un nuevo modelo de
dominio, `UnitSummary(text: String, code: String?)`, es lo que
`GetUnitSummaryUseCase` devuelve (`null` si la unidad no tiene resumen
todavia, o si el `unitId` no existe).

**No se toca Room**: `LearningUnit`/`UnitEntity` no ganan ningun campo
nuevo. El resumen se lee directo del archivo JSON on-demand, nunca se
inserta en la base de datos.

## 2. `ContentLoader` pasa a ser una interfaz (consumida solo por el repositorio)

Hoy `ContentLoader` es una clase concreta con `Context` de Android
inyectado — no es fakeable en un test JVM. Se extrae — no para que el use
case la use directo (ver decision 7), sino para que `ContentRepositoryImpl`
(unica clase que la va a consumir) sea testeable en JVM con un fake, igual
que ya se hace con `SettingsRepositoryImpl` (`SettingsRepositoryImplTest`,
el unico precedente hoy de un `RepositoryImpl` con test JVM directo):

```kotlin
// domain-adjacent, pero conceptualmente sigue siendo "cargar un pack de
// un asset" -- se mantiene en data/content, solo se convierte en interfaz
interface ContentLoader {
    fun loadPack(assetPath: String): ContentPack
}

class AssetContentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : ContentLoader {
    override fun loadPack(assetPath: String): ContentPack {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return json.decodeFromString(ContentPack.serializer(), text)
    }
}
```

Se agrega el binding en `RepositoryModule.kt` (mismo patron ya usado para
`Clock`/`SystemClock`):

```kotlin
@Binds
abstract fun bindContentLoader(impl: AssetContentLoader): ContentLoader
```

`ContentSeeder` (que ya depende de `ContentLoader`) no cambia mas que el
tipo del parametro pasa a ser la interfaz — Hilt resuelve el binding
automaticamente.

## 3. `ContentRepository` gana `getUnitSummary`; `ContentPackRegistry` centraliza los paths

`ContentRepository` (interfaz de dominio existente, hoy respaldada por
Room via `ContentRepositoryImpl`) gana un metodo nuevo:

```kotlin
interface ContentRepository {
    // ... metodos existentes sin cambios ...
    suspend fun getUnitSummary(unitId: String): UnitSummary?
}
```

`GetUnitSummaryUseCase` pasa a ser un wrapper delgado de una linea sobre
`ContentRepository` — igual que el resto de los use cases del proyecto,
ninguno depende de una clase de `data/*` directamente:

```kotlin
class GetUnitSummaryUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(unitId: String): UnitSummary? =
        contentRepository.getUnitSummary(unitId)
}
```

La logica real vive en `ContentRepositoryImpl`, que gana `contentLoader:
ContentLoader` como nuevo parametro de constructor (ademas de los DAOs de
Room que ya tiene — un repositorio puede tener mas de una fuente de datos,
ese es justamente el punto de la interfaz: el dominio no necesita saber
que el resumen vive en un JSON y el resto vive en Room).

`ContentRepositoryImpl.getUnitSummary(unitId)` necesita saber en cual
`content/*.json` esta esa unidad. Los nombres de archivo no son 100%
derivables del `sectionId` (ej. `java-fundamentals` -> `java-fundamentals.json`,
pero `java-generics-collections` -> `generics-collections.json`, sin el
prefijo `java-`) — no hay una convencion mecanica confiable.

**Solucion**: la lista de asset paths (`packAssetPaths`, hoy privada
dentro de `ContentSeeder`) se mueve a un objeto compartido,
`ContentPackRegistry`, para que `ContentSeeder` y `ContentRepositoryImpl`
usen la **misma** lista — una sola fuente de verdad. Esto tambien cierra un
riesgo real: hasta ahora, cada ciclo de contenido nuevo solo tiene que
recordar actualizar una lista (la de `ContentSeeder`); si hubiera dos
listas independientes, un ciclo futuro podria actualizar una y olvidar la
otra.

```kotlin
// data/content/ContentPackRegistry.kt
object ContentPackRegistry {
    val assetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json",
        "content/exception-handling.json",
        "content/concurrency.json"
    )
}
```

`ContentRepositoryImpl.getUnitSummary` recorre `ContentPackRegistry.assetPaths`,
carga cada pack con `contentLoader.loadPack(path)` hasta encontrar el
`unitId` buscado, y devuelve su `summary` mapeado a `UnitSummary` (o `null`
si no aparece en ningun pack o si la unidad no tiene `summary`). Costo: en
el peor caso, N parseos de JSON pequeños (hoy 5, crecera hacia ~12) —
aceptable porque esto solo corre cuando el jugador toca "Ver resumen" una
vez, no en un loop ni en cada render de Ver Ruta. Sin cache para esta
primera version; si el numero de secciones creciera mucho mas, se puede
optimizar despues.

## 4. UI: boton en Ver Ruta

En `ProgressScreen`'s `UnitRow`, cuando `unitProgress.unlocked ||
unitProgress.completed` (la misma condicion que ya usa para "jugable"),
aparece un segundo punto de entrada ademas de tocar la fila entera —
un icono/boton "Ver resumen" — que navega a `unit_summary/{unitId}` sin
disparar la navegacion de "jugar". Para una unidad bloqueada, no aparece
(no tiene sentido previsualizar algo que todavia no se puede intentar).

## 5. Pantalla nueva

`UnitSummaryScreen` + `UnitSummaryViewModel`, ruta nueva
`unit_summary/{unitId}`:

- Carga via `GetUnitSummaryUseCase(unitId)`.
- Muestra: nombre de la unidad, el texto del resumen, `CodeBlock(code)` si
  `code != null` (reutilizando el componente existente, sin
  `filledAnswer`).
- Si el resumen es `null` (caso defensivo — no deberia pasar una vez
  completado el retrofit, pero protege contra que un autor futuro se
  olvide): mensaje simple "Resumen no disponible todavia" en vez de dejar
  la pantalla vacia o crashear.
- Boton "COMENZAR UNIDAD" al final, que navega a `unit_session/{unitId}`
  reemplazando esta pantalla en el back stack (volver atras desde la
  sesion no debe pasar de nuevo por el resumen).

## Alcance

**Incluido en este plan:**
- Campo `summary` en el schema JSON + modelo de dominio.
- Extraccion de `ContentLoader` a interfaz + `ContentPackRegistry`
  compartido.
- `GetUnitSummaryUseCase`.
- `UnitSummaryScreen`/`UnitSummaryViewModel` + ruta de navegacion nueva.
- Boton "Ver resumen" en `UnitRow`.
- Retrofit: escribir el `summary` de las 19 unidades ya existentes (5
  secciones).

**Explícitamente fuera de alcance:**
- Gate obligatorio antes de jugar por primera vez (rechazado — opcional
  por diseno, ver seccion 1).
- Persistir el resumen en Room (rechazado — se lee del asset).
- Una "enciclopedia"/libro completo navegable independiente de Ruta — idea
  mas grande, para considerar a futuro si el resumen corto no alcanza.
- Busqueda de texto entre resumenes.

## Testing

- **`ContentRepositoryImpl.getUnitSummary`**: aqui vive la logica real
  (recorrer packs hasta encontrar la unidad), y es totalmente testeable en
  JVM gracias a la extraccion de `ContentLoader` a interfaz — nuevo
  `ContentRepositoryImplTest` (primer precedente ademas de
  `SettingsRepositoryImplTest`), con un fake hand-written
  (`FakeContentLoader`, con packs canned por asset path) cubriendo: unidad
  encontrada en el primer pack revisado, unidad encontrada en un pack mas
  alla del primero (prueba que sigue buscando), unidad sin `summary`
  (`null` explicito en el pack) devuelve `null`, `unitId` que no existe en
  ningun pack devuelve `null`. Los metodos existentes de
  `ContentRepositoryImpl` (respaldados por Room) siguen sin test JVM
  dedicado, sin cambios respecto a hoy.
- **`GetUnitSummaryUseCase`**: wrapper de una linea sobre
  `ContentRepository` — se prueba con el `FakeContentRepository`
  compartido (extendido con un mapa `unitSummaries` opcional en su
  constructor), confirmando que delega correctamente y propaga `null`.
- **Contenido**: mismo patron que las secciones ya curadas — JSON
  validado (`python3 -m json.tool`) + suite completa como regresion. Sin
  test dedicado de contenido mas alla de eso.
- **UI**: sin test de Compose (Nivel 2 de la ADR de testing sigue sin
  empezar) — se verifica por compilacion + QA manual en dispositivo.
- **QA manual en dispositivo**: confirmar que el boton "Ver resumen"
  aparece solo en unidades jugables; confirmar que el resumen de al menos
  una unidad de cada una de las 5 secciones existentes se ve bien (texto +
  codigo con highlighting); confirmar que "COMENZAR UNIDAD" lleva
  correctamente a la sesion de esa unidad; confirmar que volver atras
  desde la sesion no vuelve a mostrar el resumen.

## Decisiones registradas

| Decisión | Elegido | Fecha |
|---|---|---|
| Acceso al resumen | Opcional, boton aparte (no gate obligatorio) | 2026-07-29 |
| Alcance del retrofit | Las 19 unidades existentes + toda unidad futura | 2026-07-29 |
| Donde vive el contenido | En los JSON de `content/*.json`, sin Room | 2026-07-29 |
| Formato | Texto corto + bloque de codigo de ejemplo | 2026-07-29 |
| Presentacion | Pantalla completa nueva, no bottom sheet | 2026-07-29 |
| Testabilidad de `ContentLoader` | Se extrae a interfaz (mismo patron que `Clock`) | 2026-07-29 |
| Enciclopedia completa | Fuera de alcance, idea para el futuro | 2026-07-29 |
