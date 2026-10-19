package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Tune

/** A list-owned action that stays available after the search/sort header scrolls away. */
@Composable
fun LibraryFiltersFab(activeCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = if (activeCount == 0) {
        stringResource(R.string.filters)
    } else {
        pluralStringResource(R.plurals.filters_fab_description, activeCount, activeCount)
    }
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        BadgedBox(
            badge = {
                if (activeCount > 0) {
                    Badge { Text(activeCount.toString(), Modifier.clearAndSetSemantics {}) }
                }
            },
        ) {
            Icon(FujiIcons.Tune, contentDescription = description)
        }
    }
}
