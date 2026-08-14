package dev.bondarenko.fujirecipes.ui.theme

import android.content.ContentResolver
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the device has animations turned off.
 *
 * Read once and provided down the tree so a composable can drop a wavy progress indicator
 * for a plain one, or skip a shape morph, without each one reaching for a system setting.
 * `design-system.md` §5 requires the fallback; this is what makes it available.
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * `ANIMATOR_DURATION_SCALE == 0` is how "remove animations" surfaces to an app. It is a
 * global setting rather than a per-app one, so reading it at theme level is right — and it
 * changes rarely enough that reading it per composition is not worth a content observer.
 */
internal fun animationsDisabled(resolver: ContentResolver): Boolean =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * The app's theme: the web client's palette, type and shape, on Material 3.
 *
 * **Not `MaterialExpressiveTheme`, and this is a pinned toolchain limit rather than a
 * preference.** At `material3` 1.4.0 — the newest release that builds against AGP 8.13 —
 * the whole Expressive surface is `internal`: `MaterialExpressiveTheme`, `MotionScheme`,
 * `LinearWavyProgressIndicator`, `ButtonGroup`, `HorizontalFloatingToolbar` and
 * `MaterialShapes` are all either internal or absent. Verified by compiling against them,
 * not by reading release notes.
 *
 * Reaching them means `material3` 1.5.0-alpha, which pulls Compose 1.12.0-beta, which
 * requires AGP 9.1 and `compileSdk` 37. That is a toolchain decision, recorded in
 * `specs/roadmap.md` §6 rather than taken quietly here.
 *
 * What is unaffected: colour, type, shape and the reduced-motion signal below are the
 * design system, and all of them are in place. Expressive would add motion and four
 * components, and every place that wants one is a later feature (the write-progress bar in
 * FEAT-004, the slot picker's button group, the form's floating toolbar).
 */
// ponytail: standard MaterialTheme until the AGP 9 / compileSdk 37 move. Swap this call for
// MaterialExpressiveTheme and add `motionScheme = if (reducedMotion) standard() else
// expressive()` — the reduced-motion value it needs is already computed here.
@Composable
fun FujiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { animationsDisabled(context.contentResolver) }

    /**
     * **No dynamic colour.** `dynamicLightColorScheme()` would replace this palette with the
     * user's wallpaper, which is exactly the parity with the web client that the palette
     * exists to hold (`design-system.md` §2). Not an oversight — do not add it.
     */
    val colorScheme = if (darkTheme) FujiDarkColors else FujiLightColors

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FujiTypography,
            shapes = FujiShapes,
            content = content,
        )
    }
}
