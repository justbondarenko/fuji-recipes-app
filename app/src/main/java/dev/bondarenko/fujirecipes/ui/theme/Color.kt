package dev.bondarenko.fujirecipes.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The palette, transcribed from `specs/steering/design-system.md` §2.
 *
 * Parity with the shipped Nuxt theme: PrimeVue Aura with `surface.stone` primitives and the
 * `noir` primary preset, which aliases the primary ramp onto the surface ramp. Stone
 * neutrals throughout, a near-black primary in light and near-white in dark, amber as the
 * single accent.
 *
 * Source of truth is `fuji-recipes-book/src/assets/theme/{theme,primary,surface}.ts`. If
 * this file and those disagree, those win.
 *
 * **This supersedes `PRD.md` §5.2 ("Cappuccino", `#CC785C`)**, which describes a palette
 * the web client never shipped.
 */

// Stone ramp — the whole neutral vocabulary, named once.
private val Stone0 = Color(0xFFFFFFFF)
private val Stone50 = Color(0xFFFAFAF9)
private val Stone100 = Color(0xFFF5F5F4)
private val Stone200 = Color(0xFFE7E5E4)
private val Stone300 = Color(0xFFD6D3D1)
private val Stone400 = Color(0xFFA8A29E)
private val Stone500 = Color(0xFF78716C)
private val Stone600 = Color(0xFF57534E)
private val Stone700 = Color(0xFF44403C)
private val Stone800 = Color(0xFF292524)
private val Stone900 = Color(0xFF1C1917)
private val Stone950 = Color(0xFF0C0A09)

// Amber — the one accent. Ratings, progress, the hover state on a recipe name.
private val Amber100 = Color(0xFFFEF3C7)
private val Amber200 = Color(0xFFFDE68A)
private val Amber400 = Color(0xFFFBBF24)
private val Amber600 = Color(0xFFD97706)
private val Amber900 = Color(0xFF78350F)
private val Amber950 = Color(0xFF451A03)

// Red — failure only.
private val Red100 = Color(0xFFFEE2E2)
private val Red200 = Color(0xFFFECACA)
private val Red400 = Color(0xFFF87171)
private val Red600 = Color(0xFFDC2626)
private val Red900 = Color(0xFF7F1D1D)
private val Red950 = Color(0xFF450A0A)

val FujiLightColors: ColorScheme = lightColorScheme(
    primary = Stone950,
    onPrimary = Stone0,
    primaryContainer = Stone200,
    onPrimaryContainer = Stone900,
    inversePrimary = Stone50,

    secondary = Stone600,
    onSecondary = Stone0,
    secondaryContainer = Stone100,
    onSecondaryContainer = Stone800,

    tertiary = Amber600,
    onTertiary = Stone0,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber900,

    error = Red600,
    onError = Stone0,
    errorContainer = Red100,
    onErrorContainer = Red900,

    background = Stone200,
    onBackground = Stone900,
    surface = Stone200,
    onSurface = Stone900,
    onSurfaceVariant = Stone600,
    surfaceVariant = Stone300,

    surfaceContainerLowest = Stone0,
    surfaceContainerLow = Stone50,
    surfaceContainer = Stone100,
    surfaceContainerHigh = Stone200,
    surfaceContainerHighest = Stone300,

    outline = Stone500,
    outlineVariant = Stone300,

    inverseSurface = Stone900,
    inverseOnSurface = Stone50,

    scrim = Stone950,
)

val FujiDarkColors: ColorScheme = darkColorScheme(
    primary = Stone50,
    onPrimary = Stone950,
    primaryContainer = Stone700,
    onPrimaryContainer = Stone100,
    inversePrimary = Stone950,

    secondary = Stone300,
    onSecondary = Stone800,
    secondaryContainer = Stone700,
    onSecondaryContainer = Stone200,

    tertiary = Amber400,
    onTertiary = Amber950,
    tertiaryContainer = Amber900,
    onTertiaryContainer = Amber200,

    error = Red400,
    onError = Red950,
    errorContainer = Red900,
    onErrorContainer = Red200,

    background = Stone900,
    onBackground = Stone50,
    surface = Stone900,
    onSurface = Stone50,
    onSurfaceVariant = Stone300,
    surfaceVariant = Stone700,

    surfaceContainerLowest = Stone950,
    surfaceContainerLow = Stone900,
    surfaceContainer = Stone800,
    surfaceContainerHigh = Stone700,
    surfaceContainerHighest = Stone600,

    outline = Stone400,
    outlineVariant = Stone700,

    inverseSurface = Stone50,
    inverseOnSurface = Stone900,

    scrim = Stone950,
)
