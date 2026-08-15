package dev.bondarenko.fujirecipes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape, from `specs/steering/design-system.md` §5.
 *
 * More rounded than the M3 default. Cards and sheets should read as soft paper on a warm
 * stone ground rather than as panels, which is also why nothing in this app uses elevation
 * to separate a card from its background — spacing and a hairline outline do that.
 */
val FujiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
