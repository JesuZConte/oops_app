# Localización — sub-ciclo 1: Locale + ResourceBundle — Diseño

**Estado:** Aprobado, pendiente de plan de implementacion.

## Contexto

Primera sección nueva desde cero despues de que "I/O y NIO.2" cerrara
completo (ambos sub-ciclos, `d22ee54`/`cafa4c6`, QA'd y pusheados). El ADR
(`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md:150-156`) fija el
alcance completo de la sección en 3 bloques:

> - `Locale`: constructores/builders; codigos de idioma/pais/variante;
>   locale por defecto y como cambiarlo en codigo
> - `ResourceBundle`: carga de `.properties` y bundles basados en clases;
>   resolucion de claves y fallback; empaquetado seguro de textos/mensajes
> - Formateo: numeros/moneda/porcentajes via `NumberFormat`; fechas/horas/
>   periodos via `DateTimeFormatter` combinado con locale

Igual que Modulos y Empaquetado e I/O y NIO.2, se divide en 2 sub-ciclos
tematicamente distintos: sub-ciclo 1 (este spec) cubre `Locale` +
`ResourceBundle` completos; sub-ciclo 2 cubrira Formateo (`NumberFormat`
+ `DateTimeFormatter`).

Aplica la misma regla de distractores balanceados en largo/detalle
establecida desde I/O y NIO.2 sub-ciclo 1
([[feedback_mcq_distractor_length_balance]]), sin cambios.

## Verificacion tecnica previa (JDK 20 real, scratch project)

Igual que los ciclos anteriores, no se confio en memoria para
comportamiento runtime-observable: se escribio y ejecuto codigo Java real
contra OpenJDK 20 antes de fijar el diseño. Hechos confirmados:

1. **El constructor legado `new Locale(String, String)` esta deprecado**
   (warning real del compilador: "uses or overrides a deprecated API") a
   favor de `Locale.of(String, String)`, agregado en Java 19. Ambos
   producen un `Locale` equivalente (`equals()` da `true`), pero
   `Locale.of()` es la forma recomendada desde Java 19+.
2. **`toString()` y `toLanguageTag()` usan formatos distintos.**
   Confirmado sobre `Locale.of("es", "CL")`: `toString()` = `"es_CL"`
   (guion bajo, formato interno de Java), `toLanguageTag()` = `"es-CL"`
   (guion, formato BCP 47 estandar).
3. **`Locale.Builder` con un valor mal formado lanza
   `IllformedLocaleException`.** Confirmado con
   `setLanguage("not a valid language!!")` y con `setRegion("XX1")`
   (region de 3 caracteres, invalida): ambos lanzaron
   `IllformedLocaleException` con un mensaje que cita el valor invalido y
   el indice del error.
4. **`Locale.setDefault()` cambia el locale por defecto a nivel de toda
   la JVM**, no solo del hilo actual. Confirmado: tras
   `Locale.setDefault(Locale.of("fr", "FR"))`, una llamada posterior a
   `Locale.getDefault()` (sin relacion directa con el punto donde se
   hizo el cambio) devolvio `fr_FR`.
5. **El fallback de `ResourceBundle.getBundle()` NO es simplemente
   "cadena del locale pedido -> bundle base".** Es "cadena del locale
   pedido -> cadena del locale POR DEFECTO -> bundle base". Confirmado
   con un bundle base (`Messages.properties`, clave `greeting=Hello`) y
   una variante `Messages_es.properties` (`greeting=Hola`): pidiendo el
   bundle con `Locale.of("de", "DE")` (sin ningun archivo `de*`) y con el
   locale por defecto forzado a `es`, el resultado de `getString(
   "greeting")` fue `"Hola"` (via la cadena del locale POR DEFECTO), no
   `"Hello"` (el bundle base) -- a pesar de que `de_DE` no tiene ninguna
   relacion con `es`. Forzando el locale por defecto a `en_US` (sin
   archivos `en*`) en la misma prueba, el resultado si cayo al bundle
   base (`"Hello"`), confirmando que el bundle base es el ULTIMO
   eslabon, despues de agotar tanto el locale pedido como el locale por
   defecto.
6. **Una clave inexistente en un `ResourceBundle` lanza
   `MissingResourceException`.** Confirmado: `getString("nonexistent")`
   sobre un bundle valido lanzo `MissingResourceException` con un
   mensaje citando la clase del bundle y la clave.
7. **Un bundle basado en clase (`ListResourceBundle`) funciona como
   alternativa a un archivo `.properties`.** Confirmado: una clase que
   extiende `ListResourceBundle` e implementa `getContents()` devolviendo
   pares clave-valor se resuelve igual que un `.properties` via
   `ResourceBundle.getBundle()`.
8. **`MessageFormat.format()` con placeholders (`{0}`, `{1}`) permite que
   el mismo codigo funcione con patrones en distinto orden por idioma.**
   Confirmado: el patron `"{0} tiene {1} mensajes nuevos"` (español) y
   `"{0} has {1} new messages"` (ingles) producen ambos el resultado
   correcto con los mismos argumentos posicionales -- el orden de las
   palabras lo controla el patron (tipicamente cargado desde un
   `ResourceBundle` por locale), no el codigo que llama a `format()`. Una
   concatenacion directa de strings (`name + " tiene " + count + "..."`)
   no tiene este beneficio: el orden queda fijo en el codigo.

Todos los hechos anteriores fueron re-verificados ejecutando codigo real
(no citados de memoria), y se re-verificaran de nuevo al momento de
autoria de cada ejercicio.

## Diseño de unidades

### Unidad 1: "Locale"

`unitId: locale-basico`, `orderIndex: 1`, `certObjective: localizacion`.

3 conceptos, ~9 ejercicios:

1. **`locale-construccion`** — `Locale.of(idioma, pais)` es la forma
   recomendada desde Java 19+ (hecho #1); el constructor
   `new Locale(idioma, pais)` sigue compilando pero esta deprecado;
   `Locale.Builder` (`setLanguage()`/`setRegion()`/`.build()`) es la
   forma extendida para casos con variante/script; `toString()` (guion
   bajo, `es_CL`) vs `toLanguageTag()` (guion, `es-CL`, formato BCP 47)
   son representaciones de texto distintas del mismo `Locale` (hecho #2).
2. **`locale-codigos`** — `getLanguage()`/`getCountry()`/`getVariant()`
   leen los componentes de un `Locale` ya construido; `Locale.Builder`
   valida cada componente y lanza `IllformedLocaleException` ante un
   valor mal formado (hecho #3) -- trampa clasica: el error es en tiempo
   de ejecucion (al llamar `.build()`), no de compilacion.
3. **`locale-default`** — `Locale.getDefault()`/`Locale.setDefault()`
   leen/cambian el locale por defecto de toda la JVM, no solo del hilo o
   metodo actual (hecho #4) -- efecto secundario global que puede
   sorprender si se cambia sin querer en medio de una aplicacion.

### Unidad 2: "ResourceBundle"

`unitId: resourcebundle-basico`, `orderIndex: 2`, `certObjective:
localizacion` (misma convencion de sequenciado via `orderIndex` que
todos los sub-ciclos anteriores -- sin campo `dependsOn` a nivel de
unidad, ya que ese campo no existe en el esquema).

3 conceptos, ~9 ejercicios:

1. **`resourcebundle-properties-basico`** —
   `ResourceBundle.getBundle("nombre", locale)` carga un archivo
   `.properties` correspondiente (`PropertyResourceBundle` por debajo);
   una clave inexistente lanza `MissingResourceException` (hecho #6).
2. **`resourcebundle-fallback`** — la cadena de fallback real: locale
   pedido (mas especifico a menos especifico) -> locale POR DEFECTO
   (mismo orden) -> bundle base sin sufijo (hecho #5) -- el hecho no
   obvio central de esta unidad, con el ejemplo exacto verificado arriba
   (pedir `de_DE` con el default en `es` devuelve el valor de
   `Messages_es.properties`, no el del bundle base).
3. **`resourcebundle-empaquetado-seguro`** — bundles basados en clase
   (`ListResourceBundle` + `getContents()`) como alternativa a
   `.properties`, resuelta por el mismo `ResourceBundle.getBundle()`
   (hecho #7); `MessageFormat.format()` con placeholders posicionales
   (`{0}`, `{1}`) como forma segura de armar mensajes multi-idioma, en
   contraste con concatenar strings directamente, que fija el orden de
   las palabras en el codigo en vez de en el patron (hecho #8).

**Total sub-ciclo 1:** 6 conceptos, ~18 ejercicios, 2 unidades — mismo
tamaño que los sub-ciclos 1 de Modulos y Empaquetado e I/O y NIO.2.

## Fuera de alcance (deliberado)

- **`NumberFormat`/`DateTimeFormatter`**: explicitamente sub-ciclo 2, no
  este.
- **`Collator`** (ordenamiento de strings sensible a locale): no
  mencionado en el ADR, uso real menos frecuente en el examen que
  formateo/mensajes.
- **`Charset`/codificacion de texto** (UTF-8 vs otras codificaciones):
  tema de I/O, no de localizacion; ya fuera de alcance del ADR para esta
  seccion.
- **Extensiones Unicode de `Locale`** (la sintaxis `-u-` para
  calendarios/numeracion alternativos via `Locale.Builder
  .setExtension()`): variante avanzada del builder, no mencionada
  explicitamente en el ADR; el builder basico (idioma/region/variante)
  alcanza para enseñar el concepto de forma representativa.
- **Personalizar el algoritmo de carga/fallback** (`ResourceBundle
  .Control` y sus reemplazos mas recientes via el SPI de
  `ResourceBundleProvider`): API avanzada para casos de personalizacion
  profunda, de bajo uso real y no mencionada en el ADR — el
  comportamiento de fallback por defecto (hecho #5) es lo que el ADR
  pide enseñar.
- **`Locale.Category`** (separar el locale de formato del locale de
  visualizacion via `Locale.setDefault(Locale.Category, Locale)`): matiz
  avanzado de `setDefault()`, no mencionado en el ADR; el `setDefault()`
  de un solo argumento (hecho #4) alcanza para el concepto central.

## Cambios de codigo requeridos

Nueva sección, mismo patron que toda sección nueva de esta serie: nuevo
archivo `app/src/main/assets/content/localizacion.json`
(`sectionId: java-localizacion`, `orderIndex: 8`, `examVersion: core`),
mas una linea agregada a `ContentPackRegistry.assetPaths`. Bump de
`CURRENT_CONTENT_VERSION` (verificar el valor actual en `ContentSeeder.kt`
al momento de escribir el plan -- era `"23"` al momento de escribir este
spec, tras I/O y NIO.2 sub-ciclo 2).

## Reglas estandar aplicables (recordatorio, sin cambios)

Las reglas duras de siempre: sin colision de mayusculas entre
distractores y respuesta; un solo ejercicio solo/practice terminal por
concepto; `dependsOn` solo dentro de la misma unidad y identico entre
`intro`/`guided`/el terminal de un mismo concepto; `pathOrder` secuencial
0..n-1 por unidad; sin tildes en español (`LC_ALL=C grep -nP
"[\x80-\xFF]"`, esto tambien atrapa la `ñ`); sin voseo; para `fill_blank`
`solo`/`practice`, la respuesta no puede exigir recordar un identificador
que el `intro`/`guided` del mismo concepto nunca mostro. Distractores de
`mcq` balanceados en largo/detalle contra la respuesta correcta
([[feedback_mcq_distractor_length_balance]]) — se autoescribe cada
ejercicio con esto en mente desde el principio, no como pasada de
correccion posterior; no aplica a `fill_blank` (sus distractores no se
renderizan en la UI). `predict_output` no se usa para texto de excepcion/
mensaje de error exacto; los escenarios de excepcion de este sub-ciclo
(`IllformedLocaleException`, `MissingResourceException`) se enseñan como
`mcq`, citando el texto real del JDK solo dentro de `explanation`.

**Regla adicional de este ciclo, especifica del contenido**: cualquier
ejemplo de codigo que dependa del locale POR DEFECTO de la JVM (concepto
`resourcebundle-fallback` en particular) debe fijar explicitamente ese
locale por defecto dentro del propio snippet (`Locale.setDefault(...)`
antes del ejemplo relevante) en vez de asumir un valor -- el
comportamiento verificado en este spec (hecho #5) solo es determinista
si el locale por defecto esta fijado explicitamente; dejarlo implicito
haria que el resultado mostrado dependiera del entorno donde se ejecuta
el ejemplo real (como paso, de hecho, durante la verificacion tecnica de
este mismo spec, donde el locale por defecto real del entorno era
`es_419`).

## QA

Igual que los ciclos anteriores: revision SDD completa (implementer +
reviewer por tarea, revision final del modelo mas capaz disponible sobre
toda la rama), merge a main local, QA manual en dispositivo jugando
ambas unidades completas antes de dar el ciclo por cerrado (intentar en
forma automatizada de ser posible, sin usar `adb input tap` para
responder ejercicios, solo para navegacion/capturas de pantalla).
