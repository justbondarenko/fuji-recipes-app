package dev.bondarenko.fujirecipes.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.settings.NavDestinationId
import dev.bondarenko.fujirecipes.ui.common.readableWidth
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.icons.Add
import dev.bondarenko.fujirecipes.ui.theme.icons.BookmarkStacks
import dev.bondarenko.fujirecipes.ui.theme.icons.CameraRoll
import dev.bondarenko.fujirecipes.ui.theme.icons.CleaningServices
import dev.bondarenko.fujirecipes.ui.theme.icons.FileExport
import dev.bondarenko.fujirecipes.ui.theme.icons.FileSave
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.ImageSearch
import dev.bondarenko.fujirecipes.ui.theme.icons.Keep
import dev.bondarenko.fujirecipes.ui.theme.icons.LinkedCamera
import dev.bondarenko.fujirecipes.ui.theme.icons.MoreVert
import dev.bondarenko.fujirecipes.ui.theme.icons.Science
import dev.bondarenko.fujirecipes.ui.theme.icons.RawOn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pages can omit their camera trigger when the shell provides it in the navigation rail. */
val LocalRailActionProvided = compositionLocalOf { false }

private const val LONG_PRESS_EDIT_MS = 500L
private val PinYellow = Color(0xFFFBC02D)

internal fun usesNavigationRail(width: Dp, height: Dp, hasBottomAction: Boolean): Boolean =
    width >= 600.dp && height >= if (hasBottomAction) 600.dp else 480.dp

val NavDestinationId.navLabelRes: Int
    get() = when (this) {
        NavDestinationId.LIBRARY -> R.string.nav_library
        NavDestinationId.ANALYZE -> R.string.nav_read
        NavDestinationId.LAB -> R.string.nav_lab
        NavDestinationId.PHOTOS -> R.string.nav_camera_photos
        NavDestinationId.RAW_EDITOR -> R.string.raw_editor_title
        NavDestinationId.CLEANUP -> R.string.nav_cleanup
        NavDestinationId.IMPORT_CAMERA -> R.string.import_title
        NavDestinationId.IMPORT_FILE -> R.string.nav_file_import
        NavDestinationId.EXPORT -> R.string.export_title
    }

val NavDestinationId.moreTitleRes: Int
    get() = when (this) {
        NavDestinationId.LIBRARY -> R.string.nav_library
        NavDestinationId.ANALYZE -> R.string.nav_read
        NavDestinationId.LAB -> R.string.nav_lab
        NavDestinationId.PHOTOS -> R.string.nav_camera_photos
        NavDestinationId.RAW_EDITOR -> R.string.raw_editor_title
        NavDestinationId.CLEANUP -> R.string.cleanup_title
        NavDestinationId.IMPORT_CAMERA -> R.string.settings_import_camera_title
        NavDestinationId.IMPORT_FILE -> R.string.file_import_title
        NavDestinationId.EXPORT -> R.string.export_title
    }

val NavDestinationId.moreSubtitleRes: Int
    get() = when (this) {
        NavDestinationId.LIBRARY -> R.string.nav_library_subtitle
        NavDestinationId.ANALYZE -> R.string.nav_read_subtitle
        NavDestinationId.LAB -> R.string.nav_lab_subtitle
        NavDestinationId.PHOTOS -> R.string.nav_camera_photos_subtitle
        NavDestinationId.RAW_EDITOR -> R.string.raw_editor_subtitle
        NavDestinationId.CLEANUP -> R.string.cleanup_subtitle
        NavDestinationId.IMPORT_CAMERA -> R.string.settings_import_camera_subtitle
        NavDestinationId.IMPORT_FILE -> R.string.file_import_subtitle
        NavDestinationId.EXPORT -> R.string.export_subtitle
    }

val NavDestinationId.icon: ImageVector
    get() = when (this) {
        NavDestinationId.LIBRARY -> FujiIcons.BookmarkStacks
        NavDestinationId.ANALYZE -> FujiIcons.ImageSearch
        NavDestinationId.LAB -> FujiIcons.Science
        NavDestinationId.PHOTOS -> FujiIcons.CameraRoll
        NavDestinationId.RAW_EDITOR -> FujiIcons.RawOn
        NavDestinationId.CLEANUP -> FujiIcons.CleaningServices
        NavDestinationId.IMPORT_CAMERA -> FujiIcons.LinkedCamera
        NavDestinationId.IMPORT_FILE -> FujiIcons.FileSave
        NavDestinationId.EXPORT -> FujiIcons.FileExport
    }

private data class BarEntry(
    val destination: NavDestinationId? = null,
    val labelRes: Int,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
    val isMore: Boolean = false,
)

/**
 * The chrome every top-level screen sits inside.
 *
 * Implements standard Material 3 navigation:
 * - Bottom navigation on narrow windows; a rail on wider windows with room for its controls.
 * - Long-pressing More enters Edit Mode: icons jiggle and a top
 *   sheet lets the user pin/unpin up to 4 bottom-bar or 8 sidebar destinations.
 * - An [ExtendedFloatingActionButton] CTA in the bottom-right corner of the content.
 */
@Composable
fun AppShell(
    showChrome: Boolean,
    isLibrarySelected: Boolean,
    isReadSelected: Boolean,
    isLabSelected: Boolean = false,
    isCameraPhotosSelected: Boolean = false,
    isMoreSelected: Boolean,
    onLibraryClick: () -> Unit,
    onReadClick: () -> Unit,
    onLabClick: () -> Unit = {},
    onCameraPhotosClick: () -> Unit = {},
    onMoreClick: () -> Unit,
    isSelecting: Boolean = false,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
    topBarAction: (@Composable () -> Unit)? = null,
    railBottomAction: (@Composable () -> Unit)? = null,
    hideBottomNavigation: Boolean = false,
    pinnedDestinations: List<NavDestinationId> = NavDestinationId.DEFAULT_PINNED,
    selectedDestination: NavDestinationId? = null,
    onDestinationClick: ((NavDestinationId) -> Unit)? = null,
    onUpdatePinnedDestinations: (List<NavDestinationId>) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var suppressNextClick by remember { mutableStateOf(false) }
    var localPinned by remember { mutableStateOf(pinnedDestinations) }
    var isDraggingBarItem by remember { mutableStateOf(false) }
    var draggingDest by remember { mutableStateOf<NavDestinationId?>(null) }
    var dragFingerPosition by remember { mutableFloatStateOf(0f) }
    var dragInitialGrabOffset by remember { mutableFloatStateOf(0f) }
    var navigationExtentPx by remember { mutableFloatStateOf(0f) }
    val currentOnUpdatePinned by rememberUpdatedState(onUpdatePinnedDestinations)
    val haptic = LocalHapticFeedback.current

    if (isEditMode) {
        BackHandler {
            draggingDest = null
            isDraggingBarItem = false
            isEditMode = false
        }
    }

    val jiggleAngleState = if (isEditMode) {
        val jiggleTransition = rememberInfiniteTransition(label = "nav-jiggle")
        jiggleTransition.animateFloat(
            initialValue = -3.5f,
            targetValue = 3.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 115, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "jiggle-angle",
        )
    } else {
        null
    }
    val jiggleOffsetYState = if (isEditMode) {
        val jiggleTransition = rememberInfiniteTransition(label = "nav-jiggle-y")
        jiggleTransition.animateFloat(
            initialValue = -1.2f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "jiggle-offset-y",
        )
    } else {
        null
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = usesNavigationRail(maxWidth, maxHeight, railBottomAction != null)
        val maxPinned = if (useRail) NavDestinationId.MAX_RAIL_PINNED else NavDestinationId.MAX_PINNED

        LaunchedEffect(pinnedDestinations, useRail, isDraggingBarItem) {
            if (!isDraggingBarItem) localPinned = pinnedDestinations
        }

        val entries: List<BarEntry> = buildList {
            localPinned.take(maxPinned).forEach { dest ->
                val isSelected = if (onDestinationClick != null) {
                    selectedDestination == dest && !isMoreSelected
                } else {
                    when (dest) {
                        NavDestinationId.LIBRARY -> isLibrarySelected
                        NavDestinationId.ANALYZE -> isReadSelected
                        NavDestinationId.LAB -> isLabSelected
                        NavDestinationId.PHOTOS -> isCameraPhotosSelected
                        else -> false
                    }
                }
                val clickAction: () -> Unit = if (onDestinationClick != null) {
                    { onDestinationClick(dest) }
                } else {
                    when (dest) {
                        NavDestinationId.LIBRARY -> onLibraryClick
                        NavDestinationId.ANALYZE -> onReadClick
                        NavDestinationId.LAB -> onLabClick
                        NavDestinationId.PHOTOS -> onCameraPhotosClick
                        else -> ({})
                    }
                }
                add(
                    BarEntry(
                        destination = dest,
                        labelRes = dest.navLabelRes,
                        icon = dest.icon,
                        selected = isSelected,
                        onClick = clickAction,
                    ),
                )
            }
            add(
                BarEntry(
                    labelRes = R.string.nav_more,
                    icon = FujiIcons.MoreVert,
                    selected = isMoreSelected,
                    onClick = onMoreClick,
                    isMore = true,
                ),
            )
        }

        val editDragModifier = if (isEditMode) {
            Modifier.pointerInput(useRail, entries.size) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val count = entries.size
                    val slotSize = if (count > 0) (if (useRail) size.height else size.width).toFloat() / count else 0f
                    if (slotSize <= 0f) return@awaitEachGesture
                    val startSlot = ((if (useRail) down.position.y else down.position.x) / slotSize).toInt().coerceIn(0, count - 1)
                    if (startSlot >= localPinned.size) return@awaitEachGesture

                    val dragged = localPinned[startSlot]
                    val initialSlotCenter = (startSlot + 0.5f) * slotSize
                    val grabOffset = (if (useRail) down.position.y else down.position.x) - initialSlotCenter
                    var activeDrag = false
                    var accumulatedDistance = 0f
                    val slop = viewConfiguration.touchSlop * 0.5f

                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull { it.pressed }
                            if (change == null || !change.pressed) {
                                if (activeDrag) {
                                    event.changes.forEach { it.consume() }
                                }
                                break
                            }
                            val delta = if (useRail) change.position.y - change.previousPosition.y else change.position.x - change.previousPosition.x
                            accumulatedDistance += delta
                            if (!activeDrag && kotlin.math.abs(accumulatedDistance) > slop) {
                                activeDrag = true
                                isDraggingBarItem = true
                                draggingDest = dragged
                                dragFingerPosition = if (useRail) change.position.y else change.position.x
                                dragInitialGrabOffset = grabOffset
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (activeDrag) {
                                change.consume()
                                dragFingerPosition = if (useRail) change.position.y else change.position.x
                                val targetCenter = dragFingerPosition - dragInitialGrabOffset
                                val hysteresis = slotSize * 0.15f
                                val curIdx = localPinned.indexOf(dragged)
                                if (curIdx >= 0) {
                                    if (targetCenter > (curIdx + 1) * slotSize + hysteresis && curIdx < localPinned.lastIndex) {
                                        val mutable = localPinned.toMutableList()
                                        val nextIdx = curIdx + 1
                                        mutable[curIdx] = mutable[nextIdx]
                                        mutable[nextIdx] = dragged
                                        localPinned = mutable
                                        currentOnUpdatePinned(mutable)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (targetCenter < curIdx * slotSize - hysteresis && curIdx > 0) {
                                        val mutable = localPinned.toMutableList()
                                        val prevIdx = curIdx - 1
                                        mutable[curIdx] = mutable[prevIdx]
                                        mutable[prevIdx] = dragged
                                        localPinned = mutable
                                        currentOnUpdatePinned(mutable)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        }
                    } finally {
                        draggingDest = null
                        isDraggingBarItem = false
                    }
                }
            }
        } else {
            Modifier
        }

        val moreLongPressModifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val releasedBeforeTimeout = withTimeoutOrNull(LONG_PRESS_EDIT_MS) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.none { it.pressed }) {
                            return@withTimeoutOrNull true
                        }
                    }
                    @Suppress("UNREACHABLE_CODE")
                    true
                } ?: false

                if (!releasedBeforeTimeout) {
                    suppressNextClick = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isEditMode = true
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                        if (event.changes.none { it.pressed }) break
                    }
                }
            }
        }

        val itemModifier: (Int, BarEntry) -> Modifier = { index, entry ->
            val dest = entry.destination
            val dragging = isEditMode && dest != null && draggingDest == dest
            Modifier.testTag("nav-item-${dest?.id ?: "more"}").then(when {
                entry.isMore -> moreLongPressModifier
                isEditMode && dest != null -> Modifier.zIndex(if (dragging) 2f else 0f)
                    .graphicsLayer {
                        if (dragging && navigationExtentPx > 0f) {
                            val slotSize = navigationExtentPx / entries.size
                            val offset = dragFingerPosition - dragInitialGrabOffset - (index + .5f) * slotSize
                            if (useRail) translationY = offset else translationX = offset
                            scaleX = 1.08f
                            scaleY = 1.08f
                        }
                    }
                else -> Modifier
            })
        }

        Row(Modifier.fillMaxSize()) {
            if (showChrome && useRail) {
                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                    BoxWithConstraints(Modifier.fillMaxHeight()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center)
                                .heightIn(max = (maxHeight - if (railBottomAction != null) 128.dp else 0.dp).coerceAtLeast(0.dp))
                                .verticalScroll(rememberScrollState())
                                .onSizeChanged { navigationExtentPx = it.height.toFloat() }
                                .then(editDragModifier).testTag("nav-rail-items"),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            entries.forEachIndexed { index, entry ->
                                val direction = if (index % 2 == 0) 1f else -1f
                                val interactionSource = remember(index) { MutableInteractionSource() }
                                if (entry.isMore) {
                                    LaunchedEffect(interactionSource) {
                                        var holdJob: Job? = null
                                        interactionSource.interactions.collect { interaction ->
                                            when (interaction) {
                                                is PressInteraction.Press -> {
                                                    holdJob?.cancel()
                                                    holdJob = launch {
                                                        delay(LONG_PRESS_EDIT_MS)
                                                        suppressNextClick = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        isEditMode = true
                                                    }
                                                }
                                                is PressInteraction.Release, is PressInteraction.Cancel -> {
                                                    holdJob?.cancel()
                                                }
                                            }
                                        }
                                    }
                                }
                                NavigationRailItem(
                                    selected = entry.selected,
                                    onClick = {
                                        if (suppressNextClick) {
                                            suppressNextClick = false
                                        } else if (!isEditMode) {
                                            entry.onClick()
                                        }
                                    },
                                    modifier = itemModifier(index, entry),
                                    interactionSource = interactionSource,
                                    icon = {
                                        Icon(
                                            imageVector = entry.icon,
                                            contentDescription = null,
                                            modifier = if (isEditMode) {
                                                Modifier.graphicsLayer {
                                                    rotationZ = (jiggleAngleState?.value ?: 0f) * direction
                                                    translationY = (jiggleOffsetYState?.value ?: 0f) * direction
                                                }
                                            } else {
                                                Modifier
                                            },
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(entry.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                )
                            }
                        }
                        if (railBottomAction != null) {
                            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
                                railBottomAction()
                            }
                        }
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    if (showChrome && !isEditMode && topBarAction != null && (!useRail || railBottomAction == null)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            topBarAction()
                        }
                    }
                },
                bottomBar = {
                    if (showChrome && !useRail && !hideBottomNavigation) {
                        NavigationBar(
                            modifier = Modifier
                                .onSizeChanged { navigationExtentPx = it.width.toFloat() }
                                .then(editDragModifier).testTag("nav-bottom-bar"),
                        ) {
                            entries.forEachIndexed { index, entry ->
                                val direction = if (index % 2 == 0) 1f else -1f
                                val dest = entry.destination
                                val interactionSource = remember(dest ?: "more") { MutableInteractionSource() }
                                if (entry.isMore) {
                                    LaunchedEffect(interactionSource) {
                                        var holdJob: Job? = null
                                        interactionSource.interactions.collect { interaction ->
                                            when (interaction) {
                                                is PressInteraction.Press -> {
                                                    holdJob?.cancel()
                                                    holdJob = launch {
                                                        delay(LONG_PRESS_EDIT_MS)
                                                        suppressNextClick = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        isEditMode = true
                                                    }
                                                }
                                                is PressInteraction.Release, is PressInteraction.Cancel -> {
                                                    holdJob?.cancel()
                                                }
                                            }
                                        }
                                    }
                                }
                                NavigationBarItem(
                                    selected = entry.selected,
                                    onClick = {
                                        if (suppressNextClick) {
                                            suppressNextClick = false
                                        } else if (!isEditMode) {
                                            entry.onClick()
                                        }
                                    },
                                    modifier = itemModifier(index, entry),
                                    interactionSource = interactionSource,
                                    icon = {
                                        Icon(
                                            imageVector = entry.icon,
                                            contentDescription = null,
                                            modifier = if (isEditMode) {
                                                Modifier.graphicsLayer {
                                                    rotationZ = (jiggleAngleState?.value ?: 0f) * direction
                                                    translationY = (jiggleOffsetYState?.value ?: 0f) * direction
                                                }
                                            } else {
                                                Modifier
                                            },
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(entry.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = showChrome && isLibrarySelected && !isSelecting && !isEditMode,
                        enter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth * 2 },
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioLowBouncy,
                            ),
                        ) + fadeIn(animationSpec = tween(durationMillis = 150)),
                        exit = slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth * 2 },
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) + fadeOut(animationSpec = tween(durationMillis = 150)),
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = onCreateClick,
                            icon = {
                                Icon(
                                    imageVector = FujiIcons.Add,
                                    contentDescription = null,
                                )
                            },
                            text = { Text(stringResource(R.string.nav_create)) },
                        )
                    }
                },
                floatingActionButtonPosition = FabPosition.End,
            ) { innerPadding ->
                Box(Modifier.fillMaxSize().readableWidth(1200.dp)) {
                    CompositionLocalProvider(LocalRailActionProvided provides (showChrome && useRail && railBottomAction != null)) {
                        content(innerPadding)
                    }

                    // Top sheet overlay when customizing navigation
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isEditMode,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(180)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { isEditMode = false },
                                ),
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isEditMode,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(220)),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(180)),
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        CustomizeNavigationTopSheet(
                            pinnedDestinations = localPinned,
                            useRail = useRail,
                            maxPinned = maxPinned,
                            onTogglePin = { dest ->
                                val current = localPinned.toMutableList()
                                if (dest in current) {
                                    if (current.size > 1) {
                                        current.remove(dest)
                                        localPinned = current
                                        onUpdatePinnedDestinations(current)
                                    }
                                } else if (current.size < maxPinned) {
                                    current.add(dest)
                                    localPinned = current
                                    onUpdatePinnedDestinations(current)
                                }
                            },
                            onDone = { isEditMode = false; suppressNextClick = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizeNavigationTopSheet(
    pinnedDestinations: List<NavDestinationId>,
    useRail: Boolean,
    maxPinned: Int,
    onTogglePin: (NavDestinationId) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nav_customize_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(if (useRail) R.string.nav_customize_rail_subtitle else R.string.nav_customize_subtitle, pinnedDestinations.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = onDone) {
                    Text(stringResource(R.string.action_done))
                }
            }

            val allOptions = NavDestinationId.entries
            val rows = allOptions.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowItems.forEach { dest ->
                            val isPinned = dest in pinnedDestinations
                            val canToggle = isPinned && pinnedDestinations.size > 1 ||
                                !isPinned && pinnedDestinations.size < maxPinned
                            PinnableOptionTile(
                                destination = dest,
                                isPinned = isPinned,
                                enabled = canToggle,
                                onClick = { onTogglePin(dest) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnableOptionTile(
    destination: NavDestinationId,
    isPinned: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDisabled = !enabled && !isPinned
    val containerColor = when {
        isPinned -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        isDisabled -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val borderColor = when {
        isPinned -> PinYellow.copy(alpha = 0.7f)
        isDisabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .testTag("nav-pin-${destination.id}")
            .semantics { selected = isPinned }
            .height(92.dp)
            .alpha(if (isDisabled) 0.38f else 1f),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            // Pin badge in top-right corner of the icon tile
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isPinned -> PinYellow.copy(alpha = 0.18f)
                            isDisabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FujiIcons.Keep,
                    contentDescription = null,
                    tint = when {
                        isPinned -> PinYellow
                        isDisabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(14.dp),
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = null,
                    tint = when {
                        isPinned -> MaterialTheme.colorScheme.onSecondaryContainer
                        isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = stringResource(destination.navLabelRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isPinned) FontWeight.SemiBold else FontWeight.Medium,
                    color = when {
                        isPinned -> MaterialTheme.colorScheme.onSecondaryContainer
                        isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(name = "Shell — unfolded", showBackground = true, widthDp = 840, heightDp = 900)
@Preview(name = "Shell — cover", showBackground = true, widthDp = 320, heightDp = 700)
@Preview(name = "Shell — light", showBackground = true, heightDp = 400)
@Preview(name = "Shell — dark", showBackground = true, uiMode = 0x20, heightDp = 400)
@Composable
private fun AppShellPreview() {
    FujiTheme {
        AppShell(
            showChrome = true,
            isLibrarySelected = true,
            isReadSelected = false,
            isMoreSelected = false,
            onLibraryClick = {},
            onReadClick = {},
            onMoreClick = {},
            onCreateClick = {},
        ) { padding ->
            dev.bondarenko.fujirecipes.ui.common.PlaceholderScreen(
                titleRes = R.string.placeholder_more_title,
                bodyRes = R.string.placeholder_more_body,
                contentPadding = padding,
            )
        }
    }
}
