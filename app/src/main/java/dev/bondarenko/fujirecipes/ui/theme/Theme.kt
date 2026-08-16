package dev.bondarenko.fujirecipes.ui.theme

import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
 * The app's theme: Material 3 with Material You dynamic color support on Android 12+ (API 31+),
 * falling back to the curated Fuji palette on older Android versions or when dynamic color is disabled.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { animationsDisabled(context.contentResolver) }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FujiDarkColors
        else -> FujiLightColors
    }

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = if (reducedMotion) MotionScheme.standard() else MotionScheme.expressive(),
            typography = FujiTypography,
            shapes = FujiShapes,
            content = content,
        )
    }
}
