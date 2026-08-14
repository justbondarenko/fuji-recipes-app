package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The one loading indicator in this app.
 *
 * **It is not M3's `LoadingIndicator`, and cannot be yet.** That component is part of the
 * Material 3 Expressive surface, which is `internal` at `material3` 1.4.0 — established by
 * compiling against it, twice, not by reading release notes. Reaching it means
 * `material3` 1.5.0-alpha → Compose 1.12-beta → AGP 9.1 → `compileSdk` 37, the toolchain
 * move recorded in `steering/tech-stack.md` §6 and deliberately deferred for v1.
 *
 * Everything that spins in this app goes through here, so the day that move happens this is
 * the single body to change rather than a hunt through screens.
 */
// ponytail: CircularProgressIndicator standing in for M3's LoadingIndicator. Swap the body
// when the Expressive line is reachable — every caller already routes through this.
@Composable
fun FujiLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    CircularProgressIndicator(
        modifier = modifier.then(Modifier),
        color = color,
        strokeWidth = 2.dp,
    )
}
