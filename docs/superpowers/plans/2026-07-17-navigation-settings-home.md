# Navegación persistente, Ajustes y reestructuración de Home — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar una barra de navegación persistente (Home / Ruta / Ajustes), una pantalla de Ajustes con selector de tema (Sistema/Claro/Oscuro) persistido, un resumen de ruta en Home, y el cambio de título a "OOPs!", según lo acordado en `docs/adrs/2026-07-17-navigation-and-home-restructure.md`.

**Architecture:** El tema pasa de seguir ciegamente `isSystemInDarkTheme()` a leer una preferencia persistida (`SettingsRepository`, respaldado por Jetpack DataStore Preferences) resuelta a un booleano vía una función pura (`resolveDarkTheme`). La navegación se centraliza en un único `Scaffold` en `MainActivity` con un `NavigationBar` que se oculta en la ruta de Session (que sigue siendo un flujo de estudio a pantalla completa). Home y Ajustes usan `ThemedCard` y los tokens de tema ya existentes de Fase 2 — no se introduce ningún componente visual nuevo más allá de un selector de tema simple con `RadioButton` (estable, Material3 core).

**Tech Stack:** Jetpack Compose + Material3 + Navigation Compose + Hilt (ya en el proyecto). Nuevo: Jetpack DataStore Preferences (persistencia de la preferencia de tema) y `material-icons-extended` (íconos Home/Route/Settings para la barra de navegación).

## Global Constraints

- Package base: `com.zconte.oopsapp` (sin cambios).
- Fuente de verdad de las decisiones de este plan: `docs/adrs/2026-07-17-navigation-and-home-restructure.md`.
- `Session` (`OopsDestinations.SESSION`) nunca muestra la barra de navegación inferior — sigue siendo un flujo de estudio a pantalla completa, exactamente como hoy.
- El `Scaffold` de `MainActivity` es responsable de aplicar el padding inferior (altura de la barra de navegación + inset del sistema) a las 3 pantallas de nivel superior (Home, Ruta, Ajustes). Cada pantalla sigue manejando su propio inset superior (`statusBarsPadding()`), tal como se estableció en Fase 2 — no se aplica un `innerPadding` superior global, porque Ruta pinta su header a sangrado completo detrás de la status bar deliberadamente.
- No se renombran archivos/clases/rutas existentes (`ProgressScreen.kt`, `ProgressViewModel.kt`, la ruta `"progress"`) — solo se ajusta el manejo de insets donde corresponde.
- El selector de tema por defecto es **Sistema** (preserva el comportamiento actual para quien no toque el ajuste).
- Cada tarea que modifique una pantalla debe verificarse en el dispositivo en modo claro y oscuro (`adb shell cmd uimode night yes|no`).
- Sondas de verificación temporales (código agregado solo para probar algo en el dispositivo, luego revertido) deben eliminarse antes del commit de la tarea — no se commitea código de prueba temporal, siguiendo el patrón ya usado en Fase 2 (Task 1's font-check probe).

---

## File Structure

```
gradle/libs.versions.toml                                          # +datastore, +material-icons-extended — Task 1
app/build.gradle.kts                                                # +deps, +buildConfig=true — Task 1

app/src/main/java/com/zconte/oopsapp/domain/model/ThemeMode.kt      # nuevo enum — Task 1
app/src/main/java/com/zconte/oopsapp/domain/repository/SettingsRepository.kt  # nueva interfaz — Task 1
app/src/main/java/com/zconte/oopsapp/data/repository/SettingsRepositoryImpl.kt # nuevo, DataStore — Task 1
app/src/main/java/com/zconte/oopsapp/di/DataStoreModule.kt          # nuevo — Task 1
app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt         # +bind SettingsRepository — Task 1
app/src/test/java/com/zconte/oopsapp/data/repository/SettingsRepositoryImplTest.kt  # nuevo — Task 1

app/src/main/java/com/zconte/oopsapp/ui/theme/ThemeResolver.kt      # nueva función pura — Task 2
app/src/test/java/com/zconte/oopsapp/ui/theme/ThemeResolverTest.kt  # nuevo — Task 2
app/src/main/java/com/zconte/oopsapp/MainActivity.kt                # lee SettingsRepository — Task 2, reestructura Scaffold — Task 4

app/src/main/java/com/zconte/oopsapp/ui/settings/SettingsViewModel.kt  # nuevo — Task 3
app/src/main/java/com/zconte/oopsapp/ui/settings/SettingsScreen.kt     # nuevo — Task 3
app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt    # +SETTINGS — Task 3
app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt         # +ruta settings, +modifier param — Task 3

app/src/main/java/com/zconte/oopsapp/navigation/OopsBottomBar.kt       # nuevo — Task 4
app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt             # systemBarsPadding→statusBarsPadding — Task 4, tarjeta de ruta + título — Task 5
app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt     # quita navigationBarsPadding redundante — Task 4

app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt          # +streamsReadiness — Task 5
```

---

### Task 1: SettingsRepository respaldado por DataStore

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/ThemeMode.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/repository/SettingsRepository.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/repository/SettingsRepositoryImpl.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/di/DataStoreModule.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/data/repository/SettingsRepositoryImplTest.kt`

**Interfaces:**
- Produces: `ThemeMode` enum (`SYSTEM`, `LIGHT`, `DARK`) — consumido por Task 2 (`ThemeResolver`), Task 3 (`SettingsViewModel`/`SettingsScreen`).
- Produces: `SettingsRepository` — `val themeMode: Flow<ThemeMode>`, `suspend fun setThemeMode(mode: ThemeMode)` — consumido por Task 2 (`MainActivity`) y Task 3 (`SettingsViewModel`).

- [ ] **Step 1: Agregar dependencias**

En `gradle/libs.versions.toml`, agregar a `[versions]` (después de `hiltNavigationCompose = "1.2.0"`):

```toml
datastorePreferences = "1.1.1"
```

Agregar a `[libraries]` (después de `androidx-lifecycle-runtime-compose`). `material-icons-extended` es del mismo grupo `androidx.compose.material` que ya gestiona el Compose BOM del proyecto (igual que `androidx-compose-material3`, que tampoco fija versión propia) — no le pongas `version.ref`, para que tome la versión que resuelva `platform(libs.androidx.compose.bom)`:

```toml
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```

En `app/build.gradle.kts`, agregar dentro del bloque `android { }`:

```kotlin
    buildFeatures {
        compose = true
        buildConfig = true
    }
```

(reemplaza el bloque `buildFeatures { compose = true }` existente — ya está en el archivo, solo agrega la línea `buildConfig = true`).

Y en el bloque `dependencies { }`, agregar después de `implementation(libs.androidx.lifecycle.runtime.compose)`:

```kotlin
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.material.icons.extended)
```

- [ ] **Step 2: Crear `ThemeMode`**

```kotlin
package com.zconte.oopsapp.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }
```

- [ ] **Step 3: Crear la interfaz `SettingsRepository`**

```kotlin
package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

- [ ] **Step 4: Escribir el test de `SettingsRepositoryImpl` (falla primero)**

```kotlin
package com.zconte.oopsapp.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.zconte.oopsapp.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createRepository(): SettingsRepositoryImpl {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
            produceFile = { tempFolder.newFile("test.preferences_pb") }
        )
        return SettingsRepositoryImpl(dataStore)
    }

    @Test
    fun `defaults to SYSTEM when nothing stored`() = runTest {
        val repository = createRepository()

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun `setThemeMode persists and is reflected in the flow`() = runTest {
        val repository = createRepository()

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun `setThemeMode overwrites a previous value`() = runTest {
        val repository = createRepository()

        repository.setThemeMode(ThemeMode.DARK)
        repository.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
    }
}
```

- [ ] **Step 5: Ejecutar el test y confirmar que falla**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.repository.SettingsRepositoryImplTest"`
Expected: FAIL — `SettingsRepositoryImpl` unresolved reference (no existe todavía).

- [ ] **Step 6: Implementar `SettingsRepositoryImpl`**

```kotlin
package com.zconte.oopsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zconte.oopsapp.domain.model.ThemeMode
import com.zconte.oopsapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val stored = prefs[THEME_MODE_KEY]
        stored?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
    }
}
```

- [ ] **Step 7: Crear el módulo de DI para el `DataStore`**

```kotlin
package com.zconte.oopsapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore
}
```

- [ ] **Step 8: Vincular `SettingsRepository` en `RepositoryModule`**

Modificar `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt` para que quede:

```kotlin
package com.zconte.oopsapp.di

import com.zconte.oopsapp.data.repository.ExerciseRepositoryImpl
import com.zconte.oopsapp.data.repository.ProgressRepositoryImpl
import com.zconte.oopsapp.data.repository.SettingsRepositoryImpl
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
```

- [ ] **Step 9: Ejecutar el test y confirmar que pasa**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.repository.SettingsRepositoryImplTest"`
Expected: PASS — 3/3 tests.

- [ ] **Step 10: Build completo**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL` (confirma que el DI graph de Hilt resuelve `SettingsRepository` sin errores, aunque nada lo consuma todavía).

- [ ] **Step 11: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
  app/src/main/java/com/zconte/oopsapp/domain/model/ThemeMode.kt \
  app/src/main/java/com/zconte/oopsapp/domain/repository/SettingsRepository.kt \
  app/src/main/java/com/zconte/oopsapp/data/repository/SettingsRepositoryImpl.kt \
  app/src/main/java/com/zconte/oopsapp/di/DataStoreModule.kt \
  app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt \
  app/src/test/java/com/zconte/oopsapp/data/repository/SettingsRepositoryImplTest.kt
git commit -m "feat: add DataStore-backed SettingsRepository for theme preference"
```

---

### Task 2: `MainActivity` lee el tema desde `SettingsRepository`

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/ui/theme/ThemeResolver.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/ui/theme/ThemeResolverTest.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/MainActivity.kt`

**Interfaces:**
- Consumes: `SettingsRepository.themeMode: Flow<ThemeMode>` (Task 1).
- Produces: `resolveDarkTheme(mode: ThemeMode, systemInDarkTheme: Boolean): Boolean` — pura, sin dependencias de Compose ni Android, para que Task 4 no tenga que volver a tocar esta lógica al reestructurar el `Scaffold`.

- [ ] **Step 1: Escribir el test de `resolveDarkTheme` (falla primero)**

```kotlin
package com.zconte.oopsapp.ui.theme

import com.zconte.oopsapp.domain.model.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {

    @Test
    fun `SYSTEM mode follows the system dark-theme flag when it is true`() {
        assertTrue(resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = true))
    }

    @Test
    fun `SYSTEM mode follows the system dark-theme flag when it is false`() {
        assertFalse(resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = false))
    }

    @Test
    fun `LIGHT mode is always false regardless of the system flag`() {
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemInDarkTheme = true))
    }

    @Test
    fun `DARK mode is always true regardless of the system flag`() {
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemInDarkTheme = false))
    }
}
```

- [ ] **Step 2: Ejecutar el test y confirmar que falla**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.ui.theme.ThemeResolverTest"`
Expected: FAIL — `resolveDarkTheme` unresolved reference.

- [ ] **Step 3: Implementar `resolveDarkTheme`**

```kotlin
package com.zconte.oopsapp.ui.theme

import com.zconte.oopsapp.domain.model.ThemeMode

fun resolveDarkTheme(mode: ThemeMode, systemInDarkTheme: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemInDarkTheme
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
```

- [ ] **Step 4: Ejecutar el test y confirmar que pasa**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.ui.theme.ThemeResolverTest"`
Expected: PASS — 4/4 tests.

- [ ] **Step 5: Reescribir `MainActivity.kt` para leer el tema desde `SettingsRepository`**

```kotlin
package com.zconte.oopsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.domain.model.ThemeMode
import com.zconte.oopsapp.domain.repository.SettingsRepository
import com.zconte.oopsapp.navigation.OopsNavHost
import com.zconte.oopsapp.ui.theme.OopsappTheme
import com.zconte.oopsapp.ui.theme.resolveDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = resolveDarkTheme(themeMode, isSystemInDarkTheme())

            OopsappTheme(darkTheme = darkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OopsNavHost()
                }
            }
        }
    }
}
```

Nota: `innerPadding` sigue sin usarse en este paso — el `Scaffold` no tiene todavía `bottomBar` (eso es Task 4), así que no hay nada que compensar aún. No agregar `Modifier.padding` aquí, sería trabajo prematuro para un valor que en este punto siempre es cero.

- [ ] **Step 6: Verificar en el dispositivo con una sonda temporal**

Agregar temporalmente, dentro de `setContent` justo antes de `OopsappTheme`, la línea:

```kotlin
androidx.compose.runtime.LaunchedEffect(Unit) { settingsRepository.setThemeMode(ThemeMode.DARK) }
```

(requiere agregar `import androidx.compose.runtime.LaunchedEffect` y `import kotlinx.coroutines.launch` no es necesario, `LaunchedEffect` ya da un `CoroutineScope`).

Run: `./gradlew installDebug`
Con el sistema en modo claro (`adb shell cmd uimode night no`), lanza la app: debe verse en modo **oscuro** de todas formas (la preferencia forzada gana). Confirma con una captura.

Revertir la línea temporal (y su import si quedó sin uso), y confirmar que sin ella la app vuelve a seguir el modo del sistema:

```bash
adb shell cmd uimode night yes && adb shell am start -n com.zconte.oopsapp/.MainActivity
adb shell cmd uimode night no && adb shell am start -n com.zconte.oopsapp/.MainActivity
```

Expected: la app sigue el modo del sistema en ambos casos (sin la sonda, `themeMode` es `SYSTEM` por defecto — Task 1 así lo garantiza).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/theme/ThemeResolver.kt \
  app/src/test/java/com/zconte/oopsapp/ui/theme/ThemeResolverTest.kt \
  app/src/main/java/com/zconte/oopsapp/MainActivity.kt
git commit -m "feat: resolve dark theme from persisted preference instead of system-only"
```

---

### Task 3: Pantalla de Ajustes

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`

**Interfaces:**
- Consumes: `SettingsRepository` (Task 1), `ThemeMode` (Task 1), `ThemedCard` (Fase 2 Task 2), `OopsTheme.extendedColors`/`MaterialTheme` (Fase 2 Task 1), `BuildConfig.VERSION_NAME` (generado por Task 1's `buildConfig = true`).
- Produces: `OopsDestinations.SETTINGS = "settings"`, `SettingsScreen()` composable — consumido por Task 4 (barra de navegación).

- [ ] **Step 1: Agregar la constante de ruta**

Modificar `app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt`:

```kotlin
package com.zconte.oopsapp.navigation

object OopsDestinations {
    const val HOME = "home"
    const val SESSION = "session"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
}
```

- [ ] **Step 2: Crear `SettingsViewModel`**

```kotlin
package com.zconte.oopsapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.ThemeMode
import com.zconte.oopsapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
}
```

- [ ] **Step 3: Crear `SettingsScreen`**

```kotlin
package com.zconte.oopsapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.BuildConfig
import com.zconte.oopsapp.domain.model.ThemeMode
import com.zconte.oopsapp.ui.components.ThemedCard

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        ThemedCard(accentColor = MaterialTheme.colorScheme.primary) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TEMA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                ThemeOptionRow("Sistema", ThemeMode.SYSTEM, themeMode, viewModel::setThemeMode)
                ThemeOptionRow("Claro", ThemeMode.LIGHT, themeMode, viewModel::setThemeMode)
                ThemeOptionRow("Oscuro", ThemeMode.DARK, themeMode, viewModel::setThemeMode)
            }
        }

        ThemedCard(accentColor = MaterialTheme.colorScheme.tertiary) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "VERSIÓN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selectedMode == mode, onClick = { onSelect(mode) })
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

- [ ] **Step 4: Registrar la ruta en `OopsNavHost`**

Reescribir `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`:

```kotlin
package com.zconte.oopsapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zconte.oopsapp.ui.home.HomeScreen
import com.zconte.oopsapp.ui.progress.ProgressScreen
import com.zconte.oopsapp.ui.session.SessionScreen
import com.zconte.oopsapp.ui.settings.SettingsScreen

@Composable
fun OopsNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = OopsDestinations.HOME, modifier = modifier) {
        composable(OopsDestinations.HOME) {
            HomeScreen(
                onStudyClick = { navController.navigate(OopsDestinations.SESSION) },
                onProgressClick = { navController.navigate(OopsDestinations.PROGRESS) }
            )
        }
        composable(OopsDestinations.SESSION) {
            SessionScreen(
                onSessionComplete = { navController.popBackStack() }
            )
        }
        composable(OopsDestinations.PROGRESS) {
            ProgressScreen()
        }
        composable(OopsDestinations.SETTINGS) {
            SettingsScreen()
        }
    }
}
```

- [ ] **Step 5: Verificar en el dispositivo con un acceso temporal**

`SettingsScreen` ya es alcanzable por ruta, pero todavía no hay ningún botón que navegue a ella (eso es Task 4). Para verificarla ahora, modifica temporalmente `HomeScreen.kt`: en el `onClick` del botón "Ver ruta" (`OutlinedButton`), cambia `onProgressClick` por una lambda que navegue a `OopsDestinations.SETTINGS` — o más simple, agrega temporalmente un tercer botón `TextButton(onClick = { /* navega a settings */ })`. La forma más directa sin tocar la firma de `HomeScreen`: edita momentáneamente `OopsNavHost.kt`'s `startDestination` a `OopsDestinations.SETTINGS` para lanzar directo ahí.

Run: `./gradlew installDebug && adb shell am start -n com.zconte.oopsapp/.MainActivity`

Verifica en modo claro y oscuro (`adb shell cmd uimode night yes|no`): el título "Ajustes", las 3 opciones de tema con `RadioButton`, que tocar cada una actualiza cuál está seleccionado (persiste — fuerza-cierra y reabre la app con `adb shell am force-stop com.zconte.oopsapp` para confirmar que sobrevive), y que la versión (`1.0`) se muestra.

Revertir el cambio temporal de `startDestination` (vuelve a `OopsDestinations.HOME`) antes de continuar — no se commitea.

- [ ] **Step 6: Build y tests**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, tests previos siguen pasando (no se agregan tests nuevos en este paso — `SettingsScreen`/`SettingsViewModel` son capa de UI, verificados en el dispositivo, igual que el resto de las pantallas de este proyecto).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/settings/ \
  app/src/main/java/com/zconte/oopsapp/navigation/OopsDestinations.kt \
  app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt
git commit -m "feat: add Settings screen with theme selector and app version"
```

---

### Task 4: Barra de navegación inferior

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/navigation/OopsBottomBar.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/MainActivity.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `OopsDestinations.{HOME,PROGRESS,SETTINGS,SESSION}` (Task 3 agregó `SETTINGS`), `OopsNavHost(navController, modifier)` (Task 3).
- Produces: `OopsBottomBar(navController: NavHostController, currentRoute: String?)` — consumido solo por `MainActivity`.

- [ ] **Step 1: Crear `OopsBottomBar`**

```kotlin
package com.zconte.oopsapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

private data class BottomBarDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomBarDestinations = listOf(
    BottomBarDestination(OopsDestinations.HOME, "Home", Icons.Filled.Home),
    BottomBarDestination(OopsDestinations.PROGRESS, "Ruta", Icons.Filled.Route),
    BottomBarDestination(OopsDestinations.SETTINGS, "Ajustes", Icons.Filled.Settings)
)

@Composable
fun OopsBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        bottomBarDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}
```

- [ ] **Step 2: Reestructurar `MainActivity` para usar el `Scaffold` con `bottomBar`**

```kotlin
package com.zconte.oopsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zconte.oopsapp.domain.model.ThemeMode
import com.zconte.oopsapp.domain.repository.SettingsRepository
import com.zconte.oopsapp.navigation.OopsBottomBar
import com.zconte.oopsapp.navigation.OopsDestinations
import com.zconte.oopsapp.navigation.OopsNavHost
import com.zconte.oopsapp.ui.theme.OopsappTheme
import com.zconte.oopsapp.ui.theme.resolveDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = resolveDarkTheme(themeMode, isSystemInDarkTheme())

            OopsappTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute != OopsDestinations.SESSION) {
                            OopsBottomBar(navController, currentRoute)
                        }
                    }
                ) { innerPadding ->
                    OopsNavHost(
                        navController = navController,
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Ajustar el inset superior de `HomeScreen`**

En `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`, el `Scaffold` de `MainActivity` ahora aporta el padding inferior — `HomeScreen` ya no debe reservar espacio inferior propio, solo el superior (status bar). Cambiar:

```kotlin
import androidx.compose.foundation.layout.systemBarsPadding
```
por
```kotlin
import androidx.compose.foundation.layout.statusBarsPadding
```

y en el modifier chain del `Column` raíz, cambiar:
```kotlin
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
```
por:
```kotlin
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
```

- [ ] **Step 4: Quitar el `navigationBarsPadding()` redundante de `ProgressScreen`**

En `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`, el `LazyColumn` ya no necesita su propio `navigationBarsPadding()` — el `Scaffold` de `MainActivity` ya reserva ese espacio. Quitar el import `androidx.compose.foundation.layout.navigationBarsPadding` y, en el modifier chain del `LazyColumn`, cambiar:

```kotlin
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(18.dp),
```
por:
```kotlin
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
```

- [ ] **Step 5: Verificar en el dispositivo**

Run: `./gradlew installDebug && adb shell am start -n com.zconte.oopsapp/.MainActivity`

Confirma en modo claro y oscuro:
- La barra de navegación inferior aparece en Home, Ruta y Ajustes, con la pestaña activa resaltada.
- Tocar cada pestaña navega correctamente y no duplica pantallas en el back stack (usa `adb shell input keyevent KEYCODE_BACK` desde cualquiera de las 3 y confirma que sale de la app, no que retrocede entre pestañas — comportamiento esperado de destinos de nivel superior).
- Ningún contenido queda tapado por la barra inferior (revisa especialmente el botón "Ver ruta" al final de Home y la última línea de Ruta).
- Desde Home, toca "ESTUDIAR HOY": la barra de navegación **no** aparece en Session. Completa o abandona la sesión (botón atrás) y confirma que vuelves a Home con la barra visible de nuevo.
- El header de Ruta sigue pintándose a sangrado completo detrás de la status bar (no debe haber aparecido una franja del color de fondo entre la status bar y el header).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/navigation/OopsBottomBar.kt \
  app/src/main/java/com/zconte/oopsapp/MainActivity.kt \
  app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt \
  app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt
git commit -m "feat: add persistent bottom navigation bar (Home/Ruta/Ajustes)"
```

---

### Task 5: Home — resumen de ruta y título "OOPs!"

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `ProgressRepository.getReadinessByObjective(): Map<String, Float>` (ya existe, usado igual que en `ProgressViewModel`), `ThemedCard`, `onProgressClick` (ya existe en `HomeScreen`, ahora también usado por la nueva tarjeta).

- [ ] **Step 1: Agregar `streamsReadiness` a `HomeUiState`**

Reescribir `app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.data.content.ContentSeeder
import com.zconte.oopsapp.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val streak: Int = 0,
    val xp: Int = 0,
    val isReady: Boolean = false,
    val streamsReadiness: Float = 0f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val contentSeeder: ContentSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contentSeeder.seedIfEmpty()
            refreshStats()
            _uiState.update { it.copy(isReady = true) }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshStats() }
    }

    private suspend fun refreshStats() {
        val stats = progressRepository.getUserStats()
        val readiness = progressRepository.getReadinessByObjective()
        _uiState.update {
            it.copy(
                streak = stats.streak,
                xp = stats.xp,
                streamsReadiness = readiness["streams-lambdas"] ?: 0f
            )
        }
    }
}
```

- [ ] **Step 2: Agregar la tarjeta de resumen de ruta y cambiar el título en `HomeScreen`**

En `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`, cambiar el texto del título:

```kotlin
            Text(
                text = "Oops!",
```
por:
```kotlin
            Text(
                text = "OOPs!",
```

Agregar el import `androidx.compose.foundation.clickable` a la lista de imports.

Agregar una nueva `ThemedCard` entre la tarjeta de XP y el `Spacer(Modifier.weight(1f))`:

```kotlin
        ThemedCard(
            modifier = Modifier.clickable(onClick = onProgressClick),
            accentColor = MaterialTheme.colorScheme.primary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TU RUTA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Streams · ${(uiState.streamsReadiness * 100).toInt()}% · Continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.weight(1f))
```

(el `Spacer(Modifier.weight(1f))` que ya existía se reemplaza por el bloque de arriba, que lo incluye al final — no queden dos spacers duplicados).

- [ ] **Step 3: Verificar en el dispositivo**

Run: `./gradlew installDebug && adb shell am start -n com.zconte.oopsapp/.MainActivity`

Confirma en modo claro y oscuro:
- El título dice "OOPs!".
- Aparece la tarjeta "TU RUTA" entre XP y los botones, mostrando el porcentaje real de Streams (compáralo con lo que muestra la pestaña Ruta — deben coincidir).
- Tocar la tarjeta navega a Ruta (mismo destino que "Ver ruta").
- Las tarjetas de racha y XP siguen exactamente igual que antes.

- [ ] **Step 4: Build y tests**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, todos los tests existentes siguen pasando (no se agregan tests nuevos — `HomeViewModel` no tenía tests antes de este plan y este cambio es un `map` adicional sobre datos ya usados en `ProgressViewModelTest`-equivalente lookup, consistente con que este proyecto no testea ViewModels directamente, solo la capa de dominio).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt \
  app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt
git commit -m "feat: add route summary card to Home and rename title to OOPs!"
```

---

## Self-Review Notes

- **Spec coverage:** las 4 decisiones del ADR (nav bar excluyendo Session, Home con racha/XP + resumen de ruta + título "OOPs!", Ajustes con selector de tema de 3 estados persistido + versión, Ruta sin flecha propia) tienen cada una una tarea que las implementa. La consecuencia técnica del ADR sobre centralizar insets también está cubierta explícitamente en Task 4, Steps 3-4.
- **Placeholder scan:** caught and fixed one real issue on self-review — Task 4 Step 2's `MainActivity.kt` code block originally included 4 dead imports (`calculateEndPadding`, `calculateStartPadding`, `LocalLayoutDirection`, `LocalContext`, left over from considering and discarding a full-inset approach) plus a parenthetical note telling the implementer not to include them. Fixed by deleting the dead imports directly from the code block and removing the note — the block now contains only what's actually used. No other `TODO`/`TBD`/"similar a la tarea"/descripciones sin código remain.
- **Type consistency:** `ThemeMode` (Task 1) se usa con el mismo nombre en Task 2 (`resolveDarkTheme`), Task 3 (`SettingsViewModel`/`SettingsScreen`) y Task 4 (`MainActivity`). `SettingsRepository.themeMode`/`setThemeMode` se consumen con la misma firma en los 3 sitios. `OopsNavHost(navController, modifier)` definido en Task 3 coincide con la llamada de Task 4. `HomeUiState.streamsReadiness` se define y consume solo dentro de Task 5, sin fugas a otras tareas.
- **Scope check:** 5 tareas, cada una con un entregable verificable en el dispositivo (o con tests unitarios donde la lógica lo permite) antes de pasar a la siguiente. Tamaño similar al de Fase 2.
- **Segundo issue corregido en self-review:** `materialIconsExtended` tenía una versión propia fijada (`1.7.8`) en `[versions]`, pero `material-icons-extended` es del mismo grupo `androidx.compose.material` que el resto de las librerías de Compose que este proyecto deja sin `version.ref` porque las resuelve `platform(libs.androidx.compose.bom)` (así está `androidx-compose-material3` hoy). Fijarla aparte podía chocar con la versión que el BOM ya resuelve para el resto del árbol de Compose. Corregido quitando el `version.ref` de esa entrada.
- **Riesgo más alto que queda abierto:** los nombres exactos de íconos de `material-icons-extended` (`Icons.Filled.Route` en particular, menos común que `Home`/`Settings`). Si no compila en la versión que resuelva el BOM, es una tarea de Task 4 Step 1 — el implementador debe tratarlo igual que los "missing import" de Fase 2: un error de compilación visible de inmediato, no un fallo silencioso, y se corrige con el nombre correcto del mismo paquete.