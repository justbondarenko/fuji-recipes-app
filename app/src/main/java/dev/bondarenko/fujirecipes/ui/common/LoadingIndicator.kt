package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The one loading indicator in this app.
 *
 * M3's shape-morphing `LoadingIndicator`, reachable since the AGP 9.3 / `compileSdk` 37 move.
 * Everything that spins in this app goes through here, so a future change of mind about what
 * loading looks like is one body, not a hunt through screens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LoadingIndicator(
        modifier = modifier.size(size),
        color = color,
    )
}
