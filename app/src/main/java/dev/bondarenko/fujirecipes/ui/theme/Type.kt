package dev.bondarenko.fujirecipes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type, from `specs/steering/design-system.md` §4.
 *
 * Serif for titles, sans for content — the web client's split, where every heading is
 * `font-serif` (Lora) and every paragraph is `font-sans` (Inter).
 *
 * The families below are the platform's serif and sans **placeholders**. Swapping in the
 * real ones is dropping the TTFs into `res/font` and changing these two declarations to
 * `FontFamily(Font(R.font.lora_regular, FontWeight.Normal), …)`. Nothing else moves,
 * which is why they are named here rather than inlined into the scale.
 */
// ponytail: system serif/sans stand in for Lora and Inter until the font files are
// vendored. Everything downstream reads these two values, so the swap is a two-line diff.
// Tracked as FEAT-000 T-07 in the roadmap.
val FujiSans: FontFamily = FontFamily.SansSerif

/**
 * Digits that do not jitter as a value changes.
 *
 * The web client's `.stable-number` class, which it puts on slider values, the recipe
 * counter, and the write progress counter. A proportional-figure countdown reflowing on
 * every tick is a defect, not a detail.
 */
const val TabularFigures = "tnum"

val FujiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp, lineHeight = 52.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp, lineHeight = 44.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp,
    ),

    headlineLarge = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp,
    ),
    // Screen titles — "Fuji Recipes" on the list.
    headlineMedium = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp,
    ),

    // 💡 SECTION TITLES (SectionHeader.kt) use titleLarge:
    // Change `fontSize = 18.sp` and `fontWeight` here to change all section headers globally!
    titleLarge = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    // 💡 FIELD LABELS & VALUES (EditorControls.kt & RecipeViewScreen.kt) use bodyMedium:
    // Change `fontSize = 14.sp` and `fontWeight` here to change all field labels globally!
    bodyMedium = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    // The simulation label and other secondary metadata.
    bodySmall = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),

    labelLarge = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FujiSans, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp,
    ),
)
