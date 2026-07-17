# Fase 2 — Arcade Neón-Pixel Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the "Arcade Neón-Pixel" design system (dark neon / light papercraft) to the existing Home, Session, and Progress→Ruta screens, replacing the default Material3 look with the tokens and layouts specified in the Fase 2 design handoff.

**Architecture:** Design tokens (color/typography/shape) live in `ui/theme/`, consumed via `MaterialTheme` plus a custom `OopsExtendedColors` `CompositionLocal` for tokens Material3's `ColorScheme` doesn't model (code block colors, locked/gray state, hard-shadow color). Shared visual primitives (`ThemedCard`, `FunctionalCup`, `LanguageEmblem`, `CodeBlock`) live in a new `ui/components/` package and are reused across screens. Screen composables (`HomeScreen`, `SessionScreen`, `ProgressScreen`) are rewritten to use these tokens/primitives; their ViewModels are untouched except one small additive field needed for the session progress bar.

**Tech Stack:** Jetpack Compose + Material3 (already in the project from Fase 1). No new Gradle dependencies — only new font resources.

## Global Constraints

- Source of truth for every color/typography/layout value: `docs/design/design_handoff_oops_arcade/README.md`. Where this plan gives a hex value, it was copied from that file (or is a value this plan's design doc resolved — see below).
- Package base: `com.zconte.oopsapp` (unchanged from Fase 1).
- `OopsappTheme`'s `dynamicColor` parameter must be removed entirely — Android 12+ dynamic color would otherwise replace this custom palette. This is a deviation from the Fase 1-generated theme file, required by the design brief (see `docs/superpowers/specs/2026-07-16-fase2-arcade-theme-design.md`, decision 1).
- **Resolved ambiguity — dark mode `error` color:** the handoff lists two options ("`#FF2E7A` (o `#FF5747`)"). This plan uses **`#FF5747`**, distinct from `secondary` (`#FF2E7A`), so an incorrect MCQ answer never renders the same color as an ordinary call-to-action button.
- **Resolved simplification — cup/progress-bar fill value:** the handoff says the functional cup fills with "el XP del día", but the app has no day-scoped XP tracking (Fase 1's `UserStats.xp` is a lifetime total). This plan uses **progress toward the next 100-XP level** (`xp % 100 / 100f`) for both the cup fill and the Home XP progress bar. Adding real daily-XP tracking is a data-model change out of scope for this design pass.
- **Resolved simplification — Ruta sub-items:** the handoff's mockup shows multiple stations per line with named sub-skills (e.g. "stream() & filter ✓", "collect() — ahora ▶"). The app only has one aggregate readiness float per domain (`ProgressRepository.getReadinessByObjective(): Map<String, Float>`), no per-skill breakdown. This plan renders **one station per domain line**, not per-skill stations. Adding skill-level breakdown is a future data-model change, not part of this pass.
- **Resolved scope — Ruta locked domains:** Collections and SQL/JDBC render as fully locked placeholder lines (gray, 🔒, static hint text), matching the handoff's "línea 3 SQL" example — not the partially-progressed "línea 2 Collections" example, since no real progress data exists for either yet. This is UI-layer hardcoded content, not database rows.
- **Resolved scope — mascot:** only the functional cup (section 6d, pure geometry) and a flat language emblem (section 6a fallback) are implemented. The pixel-art sprite (section 6c) has no production art yet (the handoff's own mockup is a text placeholder) — out of scope for this pass.
- **Known platform limitation, not a bug to fix:** `Modifier.shadow(..., ambientColor, spotColor)` only renders tinted (colored) shadows on API 28+; on API 26–27 it falls back to an untinted gray shadow. minSdk is 26, so this graceful degradation is expected and acceptable.
- Every screen must render correctly in both light and dark mode — verify each task's screen with `adb shell cmd uimode night yes` and `adb shell cmd uimode night no`.
- File/class names in the existing codebase (`ProgressScreen.kt`, `ProgressViewModel.kt`, the `"progress"` nav route) are **not** renamed to "Ruta" — only the on-screen title text changes. Renaming is unnecessary churn for a purely visual relabel.

---

## File Structure

```
app/src/main/res/font/
├── nunito.ttf                                  # Task 1
├── jetbrains_mono.ttf                          # Task 1
└── press_start_2p.ttf                          # Task 1

app/src/main/java/com/zconte/oopsapp/ui/theme/
├── Color.kt                # rewritten: dark/light palettes + shared constants   — Task 1
├── ExtendedColors.kt       # new: OopsExtendedColors + CompositionLocal          — Task 1
├── Type.kt                 # rewritten: 3 FontFamily + Material3 Typography      — Task 1
├── Shape.kt                 # new: Material3 Shapes                              — Task 1
└── Theme.kt                # rewritten: no dynamicColor, provides ExtendedColors — Task 1

app/src/main/java/com/zconte/oopsapp/ui/components/
├── ThemedCard.kt            # glow (dark) / hard-shadow (light) container        — Task 2
├── FunctionalCup.kt         # animated cup meter (streak=steam, XP=fill)         — Task 2
├── LanguageEmblem.kt        # flat geometric Java cup icon                       — Task 2
└── CodeBlock.kt             # syntax-tinted code + blank chip (dashed border)    — Task 3

app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt         # rewritten — Task 4
app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt # +1 field  — Task 5
app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt   # rewritten — Task 5
app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt # rewritten — Task 6
```

---

### Task 1: Fuentes y tokens de tema

**Files:**
- Create: `app/src/main/res/font/nunito.ttf`, `jetbrains_mono.ttf`, `press_start_2p.ttf`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/theme/ExtendedColors.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/theme/Type.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/theme/Shape.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/theme/Theme.kt`

**Interfaces:**
- Produces: `Nunito`, `JetBrainsMono`, `PressStart2P` (`FontFamily`); `OopsTypography` (Material3 `Typography`, used as `MaterialTheme.typography`); `OopsShapes` (Material3 `Shapes`); `OopsExtendedColors` data class with fields `codeBackground, codeText, codeKeyword, codeType, success, lockedBackground, lockedBorder, lockedText, hardShadowColor, isDark`; `OopsTheme.extendedColors` (`@Composable` accessor, use as `OopsTheme.extendedColors.success` etc. from any later task); `RouteHeaderBackground` (theme-invariant `Color` constant). `OopsappTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)` — same call site as Fase 1's `MainActivity.kt` (`OopsappTheme { ... }`), no changes needed there.
- No domain logic — verified by build + visual check, not JUnit (consistent with Fase 1's UI-task convention).

- [ ] **Step 1: Download the three font files**

```bash
mkdir -p app/src/main/res/font
curl -sL "https://raw.githubusercontent.com/google/fonts/main/ofl/nunito/Nunito%5Bwght%5D.ttf" -o app/src/main/res/font/nunito.ttf
curl -sL "https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf" -o app/src/main/res/font/jetbrains_mono.ttf
curl -sL "https://raw.githubusercontent.com/google/fonts/main/ofl/pressstart2p/PressStart2P-Regular.ttf" -o app/src/main/res/font/press_start_2p.ttf
ls -la app/src/main/res/font/
```

Expected: three files, each larger than a few KB (not empty/error pages). Nunito and JetBrains Mono are **variable fonts** (single file, multiple weights via `FontVariation`) — this is intentional, see Step 3.

- [ ] **Step 2: Rewrite Color.kt with the full palette**

Replace the entire contents of `app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt`:

```kotlin
package com.zconte.oopsapp.ui.theme

import androidx.compose.ui.graphics.Color

// Dark "Neón-Pixel"
val NeonBackground = Color(0xFF0A0910)
val NeonSurface = Color(0xFF16141F)
val NeonSurfaceVariant = Color(0xFF211E2E)
val NeonOutline = Color(0xFF2C2838)
val NeonOnSurface = Color(0xFFEDEAF5)
val NeonOnSurfaceMuted = Color(0xFF8B86A3)
val NeonPrimary = Color(0xFF3D6BFF)
val NeonSecondary = Color(0xFFFF2E7A)
val NeonTertiary = Color(0xFFFF8A2E)
val NeonSuccess = Color(0xFF38D06B)
val NeonError = Color(0xFFFF5747)
val NeonLocked = Color(0xFF3A3548)

// Light "Papercraft"
val PaperBackground = Color(0xFFFAF5EC)
val PaperSurface = Color(0xFFFFFFFF)
val PaperInk = Color(0xFF2A2632)
val PaperOnSurfaceMuted = Color(0xFF8A8296)
val PaperPrimary = Color(0xFF3A72D6)
val PaperSecondary = Color(0xFFE5427E)
val PaperTertiary = Color(0xFFF59410)
val PaperSuccess = Color(0xFF4FB03A)
val PaperError = Color(0xFFE23B2E)
val PaperLockedBackground = Color(0xFFE2DCCB)
val PaperLockedBorder = Color(0xFFB7AD97)

// Code block: same look in both themes except background
val CodeBlockBackgroundDark = Color(0xFF16141F)
val CodeBlockBackgroundLight = Color(0xFF2A2632)
val CodeBlockText = Color(0xFFE7E3F2)
val CodeBlockKeyword = Color(0xFFFF8A2E)
val CodeBlockType = Color(0xFF7DD3FC)

// Ruta top bar: always dark, in both themes
val RouteHeaderBackground = Color(0xFF1A1730)
```

- [ ] **Step 3: Create ExtendedColors.kt**

Create `app/src/main/java/com/zconte/oopsapp/ui/theme/ExtendedColors.kt`:

```kotlin
package com.zconte.oopsapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class OopsExtendedColors(
    val codeBackground: Color,
    val codeText: Color,
    val codeKeyword: Color,
    val codeType: Color,
    val success: Color,
    val lockedBackground: Color,
    val lockedBorder: Color,
    val lockedText: Color,
    val hardShadowColor: Color,
    val isDark: Boolean
)

val DarkExtendedColors = OopsExtendedColors(
    codeBackground = CodeBlockBackgroundDark,
    codeText = CodeBlockText,
    codeKeyword = CodeBlockKeyword,
    codeType = CodeBlockType,
    success = NeonSuccess,
    lockedBackground = NeonLocked,
    lockedBorder = NeonOutline,
    lockedText = NeonOnSurfaceMuted,
    hardShadowColor = Color.Transparent,
    isDark = true
)

val LightExtendedColors = OopsExtendedColors(
    codeBackground = CodeBlockBackgroundLight,
    codeText = CodeBlockText,
    codeKeyword = CodeBlockKeyword,
    codeType = CodeBlockType,
    success = PaperSuccess,
    lockedBackground = PaperLockedBackground,
    lockedBorder = PaperLockedBorder,
    lockedText = PaperOnSurfaceMuted,
    hardShadowColor = PaperInk,
    isDark = false
)

val LocalOopsExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
```

- [ ] **Step 4: Rewrite Type.kt**

Replace the entire contents of `app/src/main/java/com/zconte/oopsapp/ui/theme/Type.kt`:

```kotlin
package com.zconte.oopsapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zconte.oopsapp.R

val Nunito = FontFamily(
    Font(
        R.font.nunito, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.nunito, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        R.font.nunito, weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    Font(
        R.font.nunito, weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900))
    )
)

val JetBrainsMono = FontFamily(
    Font(
        R.font.jetbrains_mono, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.jetbrains_mono, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.jetbrains_mono, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

val PressStart2P = FontFamily(Font(R.font.press_start_2p))

val OopsTypography = Typography(
    headlineSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = PressStart2P, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 14.sp)
)
```

Note: `Typography(...)` with only these 5 named parameters is valid — Material3's `Typography` constructor fills every unspecified slot with its own default `TextStyle`.

- [ ] **Step 5: Create Shape.kt**

Create `app/src/main/java/com/zconte/oopsapp/ui/theme/Shape.kt`:

```kotlin
package com.zconte.oopsapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val OopsShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)
```

- [ ] **Step 6: Rewrite Theme.kt**

Replace the entire contents of `app/src/main/java/com/zconte/oopsapp/ui/theme/Theme.kt`:

```kotlin
package com.zconte.oopsapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPrimary,
    onPrimary = Color.White,
    secondary = NeonSecondary,
    onSecondary = Color.White,
    tertiary = NeonTertiary,
    onTertiary = Color.White,
    background = NeonBackground,
    onBackground = NeonOnSurface,
    surface = NeonSurface,
    onSurface = NeonOnSurface,
    surfaceVariant = NeonSurfaceVariant,
    outline = NeonOutline,
    error = NeonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PaperPrimary,
    onPrimary = Color.White,
    secondary = PaperSecondary,
    onSecondary = Color.White,
    tertiary = PaperTertiary,
    onTertiary = Color.White,
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperSurface,
    onSurface = PaperInk,
    outline = PaperInk,
    error = PaperError,
    onError = Color.White
)

@Composable
fun OopsappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalOopsExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OopsTypography,
            shapes = OopsShapes,
            content = content
        )
    }
}

object OopsTheme {
    val extendedColors: OopsExtendedColors
        @Composable
        get() = LocalOopsExtendedColors.current
}
```

- [ ] **Step 7: Build and verify on device**

Run: `./gradlew assembleDebug installDebug`
Expected: `BUILD SUCCESSFUL`, no font-loading crash.

Launch the app (`adb shell am start -n com.zconte.oopsapp/.MainActivity`) and screenshot Home. Since Home's *layout* hasn't changed yet (Task 4 does that), confirm only that: the app doesn't crash, and the background/button colors have visibly shifted away from the Fase 1 default purple Material theme (proof the new `ColorScheme` is live). Toggle dark/light and confirm both render without crashing:

```bash
adb shell cmd uimode night yes && adb shell am start -n com.zconte.oopsapp/.MainActivity
adb shell cmd uimode night no && adb shell am start -n com.zconte.oopsapp/.MainActivity
```

- [ ] **Step 8: Verify the variable-font approach actually renders distinct weights (temporary check)**

This is the riskiest, most novel technique in the whole plan — `FontVariation.Settings` driving a single variable-font `.ttf` file to produce multiple named weights. Nothing exercises it yet: Home's current layout uses the default `bodyLarge` style, which doesn't route through the new `OopsTypography` styles until Task 4. Validate it now, before three more tasks build on top of `OopsTypography`, rather than discovering a silent failure after Task 4.

Temporarily replace `MainActivity`'s `setContent` body with this probe (inside `OopsappTheme`):

```kotlin
setContent {
    OopsappTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Nunito SemiBold 600", fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Text("Nunito Black 900", fontFamily = NunitoFamily, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("JetBrains Mono 400", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp)
                Text("JetBrains Mono 700", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("PRESS START 2P", fontFamily = PressStart2PFamily, fontSize = 14.sp)
            }
        }
    }
}
```

Run: `./gradlew installDebug && adb shell am start -n com.zconte.oopsapp/.MainActivity`
Screenshot: `adb exec-out screencap -p > /tmp/font-check.png`, then view it.

Expected: five visually distinct lines. The two Nunito lines show a clear weight contrast (SemiBold vs. Black — not identical). The two JetBrains Mono lines show a clear weight contrast *and* a fixed-width, monospaced look distinct from Nunito's proportional letterforms. The Press Start 2P line renders as blocky, pixelated capital letters, visually unlike either other family. If any two lines look the same, `FontVariation.Settings` isn't taking effect on this device/toolchain — stop and re-investigate (check `FontVariation.Settings` axis tag `"wght"` support, confirm `Build.VERSION.SDK_INT >= 26`, inspect the actual `.ttf`'s `fvar` table) before continuing to Task 2.

Revert `MainActivity` back to its Fase 1 content — **do not commit this probe.**

- [ ] **Step 9: Commit**

```bash
git add app/src/main/res/font app/src/main/java/com/zconte/oopsapp/ui/theme
git commit -m "feat: add Arcade Neón-Pixel design tokens (color, type, shape)"
```

---

### Task 2: Componentes compartidos — ThemedCard, FunctionalCup, LanguageEmblem

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/ui/components/ThemedCard.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/components/FunctionalCup.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/ui/components/LanguageEmblem.kt`

**Interfaces:**
- Consumes: `OopsTheme.extendedColors` (Task 1).
- Produces: `ThemedCard(modifier: Modifier = Modifier, accentColor: Color = MaterialTheme.colorScheme.primary, content: @Composable ColumnScope.() -> Unit)`; `FunctionalCup(xpLevelFraction: Float, streakDays: Int, modifier: Modifier = Modifier)`; `LanguageEmblem(modifier: Modifier = Modifier)`. All three are consumed directly by Task 4 (Home).

No domain logic — verified by build + a temporary preview screenshot, not JUnit.

- [ ] **Step 1: Create ThemedCard.kt**

Create `app/src/main/java/com/zconte/oopsapp/ui/components/ThemedCard.kt`:

```kotlin
package com.zconte.oopsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zconte.oopsapp.ui.theme.OopsTheme

@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    val extended = OopsTheme.extendedColors
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (!extended.isDark) {
                    val offsetPx = 4.dp.toPx()
                    drawRoundRect(
                        color = extended.hardShadowColor,
                        topLeft = Offset(offsetPx, offsetPx),
                        size = size,
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                }
            }
            .then(
                if (extended.isDark) {
                    Modifier.shadow(elevation = 12.dp, shape = shape, ambientColor = accentColor, spotColor = accentColor)
                } else {
                    Modifier
                }
            )
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(
                width = if (extended.isDark) 1.dp else 2.dp,
                color = if (extended.isDark) MaterialTheme.colorScheme.outline else extended.hardShadowColor,
                shape = shape
            )
            .padding(16.dp),
        content = content
    )
}
```

This is the version to actually implement: dark mode gets a subtle tinted `shadow()` (glow, graceful fallback pre-API28 per Global Constraints) plus a thin outline; light mode gets a solid offset rectangle drawn behind the card (`drawBehind`, same size as the card so it always matches, regardless of content height) plus a thick ink border — the "chunky hard shadow" look.

- [ ] **Step 2: Create LanguageEmblem.kt**

Create `app/src/main/java/com/zconte/oopsapp/ui/components/LanguageEmblem.kt`:

```kotlin
package com.zconte.oopsapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LanguageEmblem(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    ) {
        val cupWidth = size.width * 0.5f
        val cupHeight = size.height * 0.4f
        val left = (size.width - cupWidth) / 2f
        val top = size.height * 0.42f

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(cupWidth, cupHeight),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        drawArc(
            color = Color.White,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(left + cupWidth - 3.dp.toPx(), top + cupHeight * 0.15f),
            size = Size(cupWidth * 0.35f, cupHeight * 0.6f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
```

A flat rounded square in the current `primary` color with a white cup-glyph outline (body + handle) drawn via `Canvas` — no image assets, matches the handoff's "emblema simple... plano y geométrico" fallback.

- [ ] **Step 3: Create FunctionalCup.kt**

Create `app/src/main/java/com/zconte/oopsapp/ui/components/FunctionalCup.kt`:

```kotlin
package com.zconte.oopsapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun FunctionalCup(
    xpLevelFraction: Float,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val fillColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline.let {
        if (it == androidx.compose.ui.graphics.Color.Unspecified) MaterialTheme.colorScheme.onSurface else it
    }
    val steamColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val animatedFill by animateFloatAsState(targetValue = xpLevelFraction.coerceIn(0f, 1f), label = "cupFill")
    val wisps = when {
        streakDays <= 0 -> 0
        streakDays < 3 -> 1
        streakDays < 7 -> 2
        else -> 3
    }
    val infiniteTransition = rememberInfiniteTransition(label = "steam")
    val steamPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "steamPhase"
    )

    Canvas(modifier = modifier.size(width = 56.dp, height = 52.dp)) {
        val cupWidth = size.width * 0.75f
        val cupHeight = size.height * 0.7f
        val cupTop = size.height - cupHeight

        clipRect(0f, cupTop, cupWidth, cupTop + cupHeight) {
            drawRect(
                color = fillColor,
                topLeft = Offset(0f, cupTop + cupHeight * (1f - animatedFill)),
                size = Size(cupWidth, cupHeight * animatedFill)
            )
        }
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(0f, cupTop),
            size = Size(cupWidth, cupHeight),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
        drawArc(
            color = outlineColor,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cupWidth - 6.dp.toPx(), cupTop + cupHeight * 0.2f),
            size = Size(18.dp.toPx(), cupHeight * 0.5f),
            style = Stroke(width = 3.dp.toPx())
        )
        repeat(wisps) { i ->
            val baseX = cupWidth * (0.25f + i * 0.25f)
            val phase = (steamPhase + i * 0.33f) % 1f
            val yOffset = -phase * 18.dp.toPx()
            val alpha = (1f - phase) * 0.5f
            val path = Path().apply {
                moveTo(baseX, cupTop + yOffset)
                cubicTo(
                    baseX - 4.dp.toPx(), cupTop + yOffset - 6.dp.toPx(),
                    baseX + 4.dp.toPx(), cupTop + yOffset - 12.dp.toPx(),
                    baseX, cupTop + yOffset - 18.dp.toPx()
                )
            }
            drawPath(path, color = steamColor.copy(alpha = alpha), style = Stroke(width = 2.dp.toPx()))
        }
    }
}
```

`xpLevelFraction` is the cup's fill level (0f–1f); `streakDays` drives how many steam wisps animate (0/1/2/3). The corner radius is uniform (not the asymmetric 6px-top/12px-bottom from the mockup) — a deliberate simplification to avoid hand-rolled per-corner `Path` geometry for a decorative meter; note this if a future round wants pixel-exact matching.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. These components have no screen wiring yet (that's Task 4) — a clean build is the only verification available at this point; visual confirmation happens once Task 4 places them on Home.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/components/ThemedCard.kt app/src/main/java/com/zconte/oopsapp/ui/components/FunctionalCup.kt app/src/main/java/com/zconte/oopsapp/ui/components/LanguageEmblem.kt
git commit -m "feat: add ThemedCard, FunctionalCup, and LanguageEmblem components"
```

---

### Task 3: CodeBlock (resaltado de sintaxis + chip de hueco)

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/ui/components/CodeBlock.kt`

**Interfaces:**
- Consumes: `OopsTheme.extendedColors` (Task 1).
- Produces: `CodeBlock(code: String, modifier: Modifier = Modifier)`. Consumed by Task 5 (`SessionScreen`) for `fill_blank` exercises, whose `code` field always contains a `_____` run (verified in Fase 1's seed content, `assets/content/streams.json`).

- [ ] **Step 1: Create CodeBlock.kt**

Create `app/src/main/java/com/zconte/oopsapp/ui/components/CodeBlock.kt`:

```kotlin
package com.zconte.oopsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zconte.oopsapp.ui.theme.JetBrainsMono
import com.zconte.oopsapp.ui.theme.OopsTheme

private val CodeKeywords = setOf(
    "collect", "filter", "map", "forEach", "sorted", "limit", "skip", "reduce",
    "anyMatch", "allMatch", "noneMatch", "joining", "flatMap", "range", "min", "max",
    "groupingBy", "stream", "distinct", "count", "toList"
)
private val CodeTypes = setOf("List", "Stream", "IntStream", "Optional", "Collectors", "Comparator")
private const val BLANK_ID = "blank"

@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier) {
    val extended = OopsTheme.extendedColors
    val primary = MaterialTheme.colorScheme.primary

    val annotated = remember(code, extended) { highlightCode(code, extended) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(extended.codeBackground)
            .padding(16.dp)
    ) {
        Text(
            text = annotated,
            color = extended.codeText,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            lineHeight = 22.sp,
            inlineContent = mapOf(
                BLANK_ID to androidx.compose.foundation.text.InlineTextContent(
                    placeholder = Placeholder(
                        width = 4.em,
                        height = 1.1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .dashedBorder(width = 1.5.dp, color = primary, cornerRadius = 6.dp)
                            .background(primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    )
                }
            )
        )
    }
}

private fun highlightCode(code: String, extended: com.zconte.oopsapp.ui.theme.OopsExtendedColors): AnnotatedString =
    buildAnnotatedString {
        val blankRegex = Regex("_+")
        val tokenRegex = Regex("[A-Za-z]+")
        var index = 0
        while (index < code.length) {
            val blank = blankRegex.find(code, index)
            val token = tokenRegex.find(code, index)
            val next = sequenceOf(blank, token).filterNotNull().minByOrNull { it.range.first }
            if (next == null) {
                append(code.substring(index))
                break
            }
            if (next.range.first > index) {
                append(code.substring(index, next.range.first))
            }
            when {
                next === blank -> appendInlineContent(BLANK_ID, next.value)
                next.value in CodeKeywords -> withStyle(SpanStyle(color = extended.codeKeyword)) { append(next.value) }
                next.value in CodeTypes -> withStyle(SpanStyle(color = extended.codeType)) { append(next.value) }
                else -> append(next.value)
            }
            index = next.range.last + 1
        }
    }

private fun Modifier.dashedBorder(width: Dp, color: Color, cornerRadius: Dp): Modifier = drawWithContent {
    drawContent()
    drawRoundRect(
        color = color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx())
    )
}
```

Walks the code string once, matching whichever comes first — a run of `_` (the blank) or a run of letters (a potential keyword/type) — and either appends plain text, appends a color-tinted span, or inserts the `BLANK_ID` inline placeholder (rendered as an actual `Box` with a dashed primary-colored border, positioned inline with the text via Compose's `inlineContent` mechanism — not just colored text).

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. No screen wiring yet (Task 5 wires this into `SessionScreen`) — clean build is the only check available now.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/components/CodeBlock.kt
git commit -m "feat: add CodeBlock with syntax highlighting and blank chip"
```

---

### Task 4: Home rediseñado

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `ThemedCard`, `FunctionalCup`, `LanguageEmblem` (Task 2); `HomeViewModel`/`HomeUiState` (Fase 1, **unchanged** — `streak: Int`, `xp: Int`, `isReady: Boolean`).
- Produces: `HomeScreen(onStudyClick: () -> Unit, onProgressClick: () -> Unit, modifier: Modifier = Modifier)` — same public signature `OopsNavHost` already calls; not touched by this task.

No ViewModel changes in this task — purely a UI rewrite over existing state.

- [ ] **Step 1: Replace HomeScreen.kt**

Replace the entire contents of `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.ui.components.FunctionalCup
import com.zconte.oopsapp.ui.components.LanguageEmblem
import com.zconte.oopsapp.ui.components.ThemedCard
import com.zconte.oopsapp.ui.theme.PressStart2P

@Composable
fun HomeScreen(
    onStudyClick: () -> Unit,
    onProgressClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val levelFraction = (uiState.xp % 100) / 100f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Oops!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            LanguageEmblem()
        }

        ThemedCard(accentColor = MaterialTheme.colorScheme.tertiary) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FunctionalCup(xpLevelFraction = levelFraction, streakDays = uiState.streak)
                Column {
                    Text(
                        text = uiState.streak.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PressStart2P, fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "días seguidos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        ThemedCard(accentColor = MaterialTheme.colorScheme.primary) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = uiState.xp.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    progress = { levelFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStudyClick,
            enabled = uiState.isReady,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("ESTUDIAR HOY", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedButton(
            onClick = onProgressClick,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Ver ruta", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

Fix before running: `fontSize = 20.sp()` is invalid Kotlin (stray parentheses) — use `fontSize = 20.sp` (the `sp` extension property, no call parentheses). Apply that correction while implementing this step.

- [ ] **Step 2: Build and verify on device, light and dark**

Run: `./gradlew assembleDebug installDebug`
Expected: `BUILD SUCCESSFUL`.

```bash
adb shell cmd uimode night no
adb shell am start -n com.zconte.oopsapp/.MainActivity
adb exec-out screencap -p > /tmp/home-light.png
adb shell cmd uimode night yes
adb shell am start -n com.zconte.oopsapp/.MainActivity
adb exec-out screencap -p > /tmp/home-dark.png
```

Confirm in both screenshots: "Oops!" header with the language emblem, the functional-cup card showing the streak number in the pixel font, the XP card with its progress bar, and both buttons — no crash, no clipped/overlapping text, dark mode shows the neon palette and light mode shows the papercraft palette.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt
git commit -m "feat: redesign Home screen with Arcade Neón-Pixel components"
```

---

### Task 5: Session rediseñado (fill_blank + mcq + feedback)

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt`

**Interfaces:**
- Consumes: `CodeBlock` (Task 3); `OopsTheme.extendedColors` (Task 1).
- Produces: `SessionScreen(onSessionComplete: () -> Unit, modifier: Modifier = Modifier)` — same signature `OopsNavHost` already calls, unchanged. `SessionUiState` gains one new field: `totalExercises: Int = 0`.

- [ ] **Step 1: Add `totalExercises` to SessionUiState and set it once in `init`**

In `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt`, add a field to `SessionUiState`:

```kotlin
data class SessionUiState(
    val queue: List<Exercise> = emptyList(),
    val currentExercise: ExerciseContent? = null,
    val selectedAnswer: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val isCompleting: Boolean = false,
    val isSessionComplete: Boolean = false,
    val totalExercises: Int = 0
)
```

And in the `init` block's non-empty branch, set it once when the queue first loads:

```kotlin
    init {
        viewModelScope.launch {
            val queue = getTodaySessionUseCase(LocalDate.now())
            if (queue.isEmpty()) {
                _uiState.update { it.copy(isSessionComplete = true) }
            } else {
                _uiState.update {
                    it.copy(queue = queue, totalExercises = queue.size, currentExercise = decode(queue.first()))
                }
            }
        }
    }
```

No other change to `SessionViewModel.kt` — `totalExercises` is set once and never modified again; `nextExercise()` and `submitAnswer()` are untouched. The UI derives the current exercise index from `totalExercises - queue.size + 1` (queue shrinks by one each time `nextExercise()` advances).

- [ ] **Step 2: Replace SessionScreen.kt**

Replace the entire contents of `app/src/main/java/com/zconte/oopsapp/ui/session/SessionScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.ui.components.CodeBlock
import com.zconte.oopsapp.ui.theme.OopsTheme

private const val MCQ_TYPE = "mcq"

@Composable
fun SessionScreen(
    onSessionComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSessionComplete) {
        if (uiState.isSessionComplete) onSessionComplete()
    }

    val exercise = uiState.currentExercise
    if (exercise == null) {
        Text(
            "Cargando sesion...",
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    var answer by remember(exercise.id) { mutableStateOf("") }
    val mcqOptions = remember(exercise.id) {
        if (exercise.type == MCQ_TYPE) (exercise.distractors + exercise.answer).shuffled() else emptyList()
    }
    val currentIndex = (uiState.totalExercises - uiState.queue.size + 1).coerceAtLeast(1)
    val progressFraction = if (uiState.totalExercises > 0) currentIndex / uiState.totalExercises.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.weight(1f).height(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$currentIndex/${uiState.totalExercises}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = exercise.prompt,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        exercise.code?.let { code ->
            CodeBlock(code = code, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.weight(1f))

        if (!uiState.isAnswered) {
            if (exercise.type == MCQ_TYPE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mcqOptions.forEach { option ->
                        McqOptionButton(
                            text = option,
                            state = McqOptionState.NORMAL,
                            onClick = { viewModel.submitAnswer(option) }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.submitAnswer(answer) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("COMPROBAR", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            if (exercise.type == MCQ_TYPE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mcqOptions.forEach { option ->
                        val state = when {
                            option != uiState.selectedAnswer -> McqOptionState.NORMAL
                            uiState.isCorrect -> McqOptionState.CORRECT
                            else -> McqOptionState.INCORRECT
                        }
                        McqOptionButton(text = option, state = state, onClick = {})
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            FeedbackBanner(
                isCorrect = uiState.isCorrect,
                answer = exercise.answer,
                explanation = exercise.explanation
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.nextExercise() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SIGUIENTE", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private enum class McqOptionState { NORMAL, CORRECT, INCORRECT }

@Composable
private fun McqOptionButton(text: String, state: McqOptionState, onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
    val borderColor = when (state) {
        McqOptionState.NORMAL -> MaterialTheme.colorScheme.outline
        McqOptionState.CORRECT -> extended.success
        McqOptionState.INCORRECT -> MaterialTheme.colorScheme.error
    }
    val backgroundColor = when (state) {
        McqOptionState.NORMAL -> MaterialTheme.colorScheme.surface
        McqOptionState.CORRECT -> extended.success.copy(alpha = 0.15f)
        McqOptionState.INCORRECT -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = state == McqOptionState.NORMAL, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            if (state == McqOptionState.CORRECT) {
                Text("✓", color = extended.success, style = MaterialTheme.typography.titleMedium)
            }
            if (state == McqOptionState.INCORRECT) {
                Text("✗", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FeedbackBanner(isCorrect: Boolean, answer: String, explanation: String) {
    val extended = OopsTheme.extendedColors
    val color = if (isCorrect) extended.success else MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(2.dp, color, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (isCorrect) "¡Correcto! +10 XP" else "Incorrecto. Respuesta: $answer",
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

Note: for MCQ exercises, once answered, all 4 buttons re-render with `onClick = {}` (disabled via `clickable(enabled = false)` for non-NORMAL states) — matches the brief's "bloquear opciones tras responder". The feedback banner's `+10 XP` is hardcoded copy matching Fase 4's `XP_PER_SESSION` constant (10) — if that constant ever changes, update this string too (no shared source of truth between them; acceptable for this pass, flag if it drifts).

- [ ] **Step 3: Build and verify on device — both variants, both themes**

Run: `./gradlew assembleDebug installDebug`
Expected: `BUILD SUCCESSFUL`.

Clear app data for a clean run (`adb shell pm clear com.zconte.oopsapp`), start a session, and screenshot:
- A `fill_blank` exercise (e.g. `streams-01`) before and after answering, in light and dark.
- An `mcq` exercise (e.g. `streams-04`) before and after answering (both a correct and an incorrect run), in light and dark.

Confirm: the code block renders with amber keywords, blue types, and the blank as a dashed-border chip; MCQ buttons show the border/background state change on answer; the feedback banner colors match correct/incorrect; the progress bar and "N/5" counter advance correctly across the session; no crash on session completion.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/session
git commit -m "feat: redesign Session screen with code highlighting and MCQ states"
```

---

### Task 6: Ruta (Progress rediseñado)

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `ProgressViewModel`/`ProgressUiState` (Fase 1, **unchanged** — `readinessByObjective: Map<String, Float>`); `OopsTheme.extendedColors`, `RouteHeaderBackground`, `PressStart2P` (Task 1).
- Produces: `ProgressScreen(modifier: Modifier = Modifier)` — same signature `OopsNavHost` already calls, unchanged.

- [ ] **Step 1: Replace ProgressScreen.kt**

Replace the entire contents of `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.ui.theme.OopsTheme
import com.zconte.oopsapp.ui.theme.PressStart2P
import com.zconte.oopsapp.ui.theme.RouteHeaderBackground

private data class RouteLine(
    val label: String,
    val statusLine: String,
    val color: Color?,
    val locked: Boolean,
    val lockedHint: String? = null
)

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extended = OopsTheme.extendedColors
    val streamsReadiness = uiState.readinessByObjective["streams-lambdas"] ?: 0f
    val globalPercent = if (uiState.readinessByObjective.isNotEmpty()) {
        (uiState.readinessByObjective.values.average() * 100).toInt()
    } else 0

    val lines = listOf(
        RouteLine(
            label = "STREAMS · L1",
            statusLine = "${(streamsReadiness * 100).toInt()}% dominado",
            color = MaterialTheme.colorScheme.primary,
            locked = false
        ),
        RouteLine(
            label = "COLLECTIONS · L2",
            statusLine = "Colecciones y Map/Set",
            color = null,
            locked = true,
            lockedHint = "Se abre al 60% de Streams"
        ),
        RouteLine(
            label = "SQL/JDBC · L3",
            statusLine = "JDBC y NIO.2",
            color = null,
            locked = true,
            lockedHint = "Proximamente"
        )
    )

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RouteHeaderBackground)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ruta 1Z0-830",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "$globalPercent%",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
                color = extended.success
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            items(lines) { line -> RouteLineRow(line) }
        }
    }
}

@Composable
private fun RouteLineRow(line: RouteLine) {
    val extended = OopsTheme.extendedColors
    val stationColor = line.color ?: extended.lockedBorder
    val labelColor = line.color ?: extended.lockedText

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (line.locked) extended.lockedBackground else stationColor)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = line.label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
                color = labelColor
            )
            Text(
                text = line.statusLine,
                style = MaterialTheme.typography.titleMedium,
                color = if (line.locked) extended.lockedText else MaterialTheme.colorScheme.onBackground
            )
            if (line.locked && line.lockedHint != null) {
                Text(
                    text = "🔒 ${line.lockedHint}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.lockedText
                )
            }
        }
    }
}
```

Note the deliberate simplification from Global Constraints: **one station per domain line**, not the mockup's multi-station-per-line breakdown — there's no per-skill data to back more than one station per domain yet.

- [ ] **Step 2: Build and verify on device, light and dark**

Run: `./gradlew assembleDebug installDebug`
Expected: `BUILD SUCCESSFUL`.

From Home, tap "Ver ruta". Screenshot in both light and dark. Confirm: dark header bar with "Ruta 1Z0-830" + global percentage in the pixel font, Streams line colored and showing its real readiness percentage, Collections and SQL/JDBC lines rendered gray with a 🔒 hint line each.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt
git commit -m "feat: redesign Progress screen as the Ruta metro-line view"
```

---

## Self-Review Notes

- **Spec coverage:** dark/light palettes, typography roles, shapes, and all three screens from `docs/design/design_handoff_oops_arcade/README.md` map to a task each. The three explicit deferrals (pixel-art sprite, per-skill Ruta stations, day-scoped XP) are called out in Global Constraints, not silently dropped.
- **Placeholder scan:** caught and fixed two issues on self-review — an earlier draft of Task 2 Step 1 left a malformed intermediate snippet before the real `ThemedCard.kt` code (removed, only the correct version remains), and Task 4's `HomeScreen.kt` had an invalid `20.sp()` call plus a missing `sp` import (both fixed in place).
- **Type consistency:** `OopsExtendedColors` fields (`success`, `codeBackground`, `lockedBackground`, `lockedBorder`, `lockedText`, `hardShadowColor`, `isDark`) are defined once in Task 1 and used with identical names in Tasks 2, 3, 5, and 6. `FunctionalCup(xpLevelFraction, streakDays)` and `CodeBlock(code)` signatures from Task 2/3 match exactly how Task 4/5 call them. `SessionUiState.totalExercises` is introduced in Task 5 Step 1 and consumed in the same task's Step 2 — no cross-task drift.
- **Scope check:** appropriately sized for one implementation plan — six tasks, each independently buildable and visually verifiable, mirroring Fase 1's granularity.
- **Post-write amendments (advisor pass):** Task 1 originally verified only "no crash + colors shifted" — nothing on screen actually exercised `OopsTypography` until Task 4, so a broken `FontVariation.Settings` approach (the plan's most novel technique) would only surface after three tasks were built on top of it. Added Step 8, a temporary on-device probe rendering Nunito/JetBrains Mono/Press Start 2P at contrasting weights, screenshotted and visually confirmed distinct before Task 2 starts (probe reverted, not committed). Also widened `CodeBlock`'s `remember(code)` to `remember(code, extended)` since it captures `extended` in its lambda — harmless today (code-block syntax colors are theme-invariant in this design) but a latent staleness trap if that changes.