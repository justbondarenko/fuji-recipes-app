package dev.bondarenko.fujirecipes.ui.raweditor

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Help
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RawEditorHelp(text: String, modifier: Modifier = Modifier) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = state,
        focusable = true,
        onDismissRequest = { state.dismiss() },
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) {
            Icon(FujiIcons.Help, stringResource(R.string.raw_editor_help))
        }
    }
}
