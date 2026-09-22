package org.ligi.passandroid.ui.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.ui.state.PassCardLine
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.dateLabel
import org.ligi.passandroid.ui.state.displayArtwork
import org.ligi.passandroid.ui.state.isExpired
import org.ligi.passandroid.ui.state.resolvePassCardLines
import org.ligi.passandroid.ui.barcode.PassCodeImage
import org.ligi.passandroid.ui.theme.PassActionButtonGroup
import org.ligi.passandroid.ui.theme.PassActionButton
import org.ligi.passandroid.ui.theme.PassActionButtonGap
import org.ligi.passandroid.ui.theme.passActionButtonGroupWidth
import androidx.compose.ui.res.stringResource
import org.ligi.passandroid.R

@Composable
internal fun TicketFeed(
    passes: List<PassUiModel>,
    categories: List<PassCategory>,
    columns: Int,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    showProtectedPassLockIcon: Boolean,
    blurProtectedPassCards: Boolean,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean, String) -> Unit,
    onDelete: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleProtected: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onPreviewChanged: (String?) -> Unit,
    onPreviewOpeningChanged: (String?) -> Unit,
    onProtectedPreviewRequested: () -> Unit,
    openSwipePassId: androidx.compose.runtime.MutableState<String?>,
    tagCategories: List<PassCategory>,
    recentlyImportedIds: Set<String> = emptySet(),
) {
    val feeds = remember(passes, columns) {
        List(columns) { column -> passes.filterIndexed { index, _ -> index % columns == column } }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        feeds.forEach { feed ->
            if (feed.isNotEmpty()) {
                ReorderableTicketColumn(
                    passes = feed,
                    categories = categories,
                    hero = hero,
                    sectionOrder = sectionOrder,
                    hiddenSections = hiddenSections,
                    showProtectedPassLockIcon = showProtectedPassLockIcon,
                    blurProtectedPassCards = blurProtectedPassCards,
                    modifier = Modifier.weight(1f),
                    onOpen = onOpen,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onToggleFavorite = onToggleFavorite,
                    onToggleProtected = onToggleProtected,
                    onReorder = onReorder,
                    onPreviewChanged = onPreviewChanged,
                    onPreviewOpeningChanged = onPreviewOpeningChanged,
                    onProtectedPreviewRequested = onProtectedPreviewRequested,
                    openSwipePassId = openSwipePassId,
                    tagCategories = tagCategories,
                    recentlyImportedIds = recentlyImportedIds,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class FeedItemBounds(val top: Float, val height: Int) {
    val center: Float get() = top + height / 2f
}

@Composable
private fun ReorderableTicketColumn(
    passes: List<PassUiModel>,
    categories: List<PassCategory>,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    showProtectedPassLockIcon: Boolean,
    blurProtectedPassCards: Boolean,
    modifier: Modifier,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean, String) -> Unit,
    onDelete: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleProtected: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onPreviewChanged: (String?) -> Unit,
    onPreviewOpeningChanged: (String?) -> Unit,
    onProtectedPreviewRequested: () -> Unit,
    openSwipePassId: androidx.compose.runtime.MutableState<String?>,
    tagCategories: List<PassCategory>,
    recentlyImportedIds: Set<String>,
) {
    var visualPasses by remember { mutableStateOf(passes) }
    var measuredBounds by remember { mutableStateOf<Map<String, FeedItemBounds>>(emptyMap()) }
    var frozenBounds by remember { mutableStateOf<Map<String, FeedItemBounds>>(emptyMap()) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var draggedFromIndex by remember { mutableIntStateOf(-1) }
    var draggedTargetIndex by remember { mutableIntStateOf(-1) }
    var draggedOffset by remember { mutableFloatStateOf(0f) }
    var pendingCommitIds by remember { mutableStateOf<List<String>?>(null) }
    var settlingId by remember { mutableStateOf<String?>(null) }
    val settlingOffset = remember { Animatable(0f) }
    val hapticFeedback = LocalHapticFeedback.current
    val reorderScope = rememberCoroutineScope()
    val targetHysteresis = with(LocalDensity.current) { 8.dp.toPx() }

    LaunchedEffect(passes, draggedId, pendingCommitIds) {
        if (draggedId == null) {
            val incomingIds = passes.map(PassUiModel::id)
            val latestById = passes.associateBy(PassUiModel::id)
            if (pendingCommitIds == incomingIds) {
                pendingCommitIds = null
                visualPasses = passes
            } else if (pendingCommitIds != null && pendingCommitIds!!.toSet() == incomingIds.toSet()) {
                // Keep a locally committed reorder until the repository emits that order.
                visualPasses = pendingCommitIds!!.mapNotNull(latestById::get)
            } else {
                // A new sort or metadata update owns the incoming order.
                visualPasses = passes
            }
        }
    }

    fun clearDrag() {
        draggedId = null
        draggedFromIndex = -1
        draggedTargetIndex = -1
        draggedOffset = 0f
        frozenBounds = emptyMap()
    }

    val simulatedOrder = if (
        draggedFromIndex in visualPasses.indices && draggedTargetIndex in visualPasses.indices
    ) {
        visualPasses.toMutableList().apply {
            add(draggedTargetIndex, removeAt(draggedFromIndex))
        }
    } else {
        visualPasses
    }
    val itemGap = visualPasses.zipWithNext().firstNotNullOfOrNull { (current, next) ->
        val currentBounds = frozenBounds[current.id] ?: return@firstNotNullOfOrNull null
        val nextBounds = frozenBounds[next.id] ?: return@firstNotNullOfOrNull null
        nextBounds.top - currentBounds.top - currentBounds.height
    } ?: 0f
    var nextSimulatedTop = frozenBounds[visualPasses.firstOrNull()?.id]?.top ?: 0f
    val simulatedTops = buildMap {
        simulatedOrder.forEach { item ->
            put(item.id, nextSimulatedTop)
            nextSimulatedTop += (frozenBounds[item.id]?.height ?: 0) + itemGap
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        visualPasses.forEachIndexed { index, pass ->
            key(pass.id) {
                val category = categories.firstOrNull { it.id == pass.categoryId }
                val radius = if (hero) 28.dp else 20.dp
                val shape = RoundedCornerShape(
                    topStart = if (index == 0) radius else 0.dp,
                    topEnd = if (index == 0) radius else 0.dp,
                    bottomStart = if (index == visualPasses.lastIndex) radius else 0.dp,
                    bottomEnd = if (index == visualPasses.lastIndex) radius else 0.dp,
                )
                val siblingTargetOffset = if (pass.id != draggedId) {
                    val original = frozenBounds[pass.id]?.top
                    val target = simulatedTops[pass.id]
                    if (original != null && target != null) target - original else 0f
                } else {
                    0f
                }
                val siblingOffset by animateFloatAsState(
                    targetValue = siblingTargetOffset,
                    animationSpec = tween(120),
                    label = "reorderSiblingOffset",
                )
                val highlighted = pass.id in recentlyImportedIds
                val isDragged = pass.id == draggedId
                val isSettling = pass.id == settlingId
                val visualOffset = when {
                    isDragged -> draggedOffset
                    isSettling -> settlingOffset.value
                    else -> siblingOffset
                }
                val isLifted = isDragged || isSettling

                TicketSwipeContainer(
                    pass = pass,
                    category = category,
                    highlighted = highlighted,
                    hero = hero,
                    sectionOrder = sectionOrder,
                    hiddenSections = hiddenSections,
                    showProtectedPassLockIcon = showProtectedPassLockIcon,
                    blurProtectedPassCards = blurProtectedPassCards,
                    modifier = Modifier.fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            if (draggedId == null) {
                                measuredBounds = measuredBounds + (
                                    pass.id to FeedItemBounds(coordinates.positionInParent().y, coordinates.size.height)
                                )
                            }
                        }
                        .zIndex(if (isLifted) 2f else 0f)
                        .graphicsLayer {
                            translationY = visualOffset
                            scaleX = if (isLifted) 1.025f else 1f
                            scaleY = if (isLifted) 1.025f else 1f
                            shadowElevation = if (isLifted) 12.dp.toPx() else 0f
                            // The lifted shadow must follow the card outline instead of the rectangular layer bounds.
                            this.shape = shape
                            clip = isLifted
                        },
                    shape = shape,
                    reorderingActive = draggedId != null,
                    onOpen = onOpen,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onToggleFavorite = onToggleFavorite,
                    onToggleProtected = onToggleProtected,
                    onReorderStart = {
                        draggedId = pass.id
                        draggedFromIndex = index
                        draggedTargetIndex = index
                        draggedOffset = 0f
                        frozenBounds = measuredBounds
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onReorderDrag = { delta ->
                        draggedOffset += delta
                        val origin = frozenBounds[pass.id]
                        if (origin != null) {
                            val draggedCenter = origin.center + draggedOffset
                            var nextTarget = draggedTargetIndex
                            while (nextTarget < visualPasses.lastIndex) {
                                val currentCenter = frozenBounds[visualPasses[nextTarget].id]?.center ?: break
                                val followingCenter = frozenBounds[visualPasses[nextTarget + 1].id]?.center ?: break
                                if (draggedCenter > (currentCenter + followingCenter) / 2f + targetHysteresis) {
                                    nextTarget++
                                } else {
                                    break
                                }
                            }
                            while (nextTarget > 0) {
                                val currentCenter = frozenBounds[visualPasses[nextTarget].id]?.center ?: break
                                val precedingCenter = frozenBounds[visualPasses[nextTarget - 1].id]?.center ?: break
                                if (draggedCenter < (currentCenter + precedingCenter) / 2f - targetHysteresis) {
                                    nextTarget--
                                } else {
                                    break
                                }
                            }
                            if (nextTarget != draggedTargetIndex) {
                                draggedTargetIndex = nextTarget
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onReorderEnd = {
                        val from = draggedFromIndex
                        val to = draggedTargetIndex
                        val id = draggedId
                        val canCommitReorder = from in visualPasses.indices && to in visualPasses.indices && from != to
                        if (id != null && canCommitReorder) {
                            val reorderedPasses = visualPasses.toMutableList().apply { add(to, removeAt(from)) }
                            val reorderedIds = reorderedPasses.map(PassUiModel::id)
                            val originTop = frozenBounds[id]?.top ?: 0f
                            val targetTop = simulatedTops[id] ?: originTop
                            val releaseOffset = originTop + draggedOffset - targetTop
                            reorderScope.launch {
                                settlingOffset.snapTo(releaseOffset)
                                settlingId = id
                                pendingCommitIds = reorderedIds
                                visualPasses = reorderedPasses
                                clearDrag()
                                onReorder(reorderedIds)
                                settlingOffset.animateTo(0f, tween(180))
                                settlingId = null
                            }
                        } else {
                            clearDrag()
                        }
                    },
                    onReorderCancel = ::clearDrag,
                    onPreviewChanged = onPreviewChanged,
                    onPreviewOpeningChanged = onPreviewOpeningChanged,
                    onProtectedPreviewRequested = onProtectedPreviewRequested,
                    openSwipePassId = openSwipePassId,
                    tagCategories = tagCategories,
                )
            }
        }
    }
}

private enum class SwipeRevealAnchor { Closed, StartActions, StartCommit, EndActions }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TicketSwipeContainer(
    pass: PassUiModel,
    category: PassCategory?,
    highlighted: Boolean,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    showProtectedPassLockIcon: Boolean,
    blurProtectedPassCards: Boolean,
    modifier: Modifier,
    shape: Shape,
    reorderingActive: Boolean,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean, String) -> Unit,
    onDelete: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleProtected: (String) -> Unit,
    onReorderStart: () -> Unit,
    onReorderDrag: (Float) -> Unit,
    onReorderEnd: () -> Unit,
    onReorderCancel: () -> Unit,
    onPreviewChanged: (String?) -> Unit,
    onPreviewOpeningChanged: (String?) -> Unit,
    onProtectedPreviewRequested: () -> Unit,
    openSwipePassId: androidx.compose.runtime.MutableState<String?>,
    tagCategories: List<PassCategory>,
) {
    val restoring = pass.isArchived
    val archiveLabel = if (restoring) stringResource(R.string.home_restore) else stringResource(R.string.home_archive)
    val pinnedLabel = if (pass.isPinned) stringResource(R.string.home_unpin_pass) else stringResource(R.string.home_pin_pass)
    val protectLabel = if (pass.isProtected) stringResource(R.string.home_remove_protection) else stringResource(R.string.home_protect_pass)
    val deleteLabel = stringResource(R.string.category_delete)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val startRevealWidth = passActionButtonGroupWidth(1)
    val endRevealWidth = passActionButtonGroupWidth(3)
    val cardActionGap = PassActionButtonGap
    var measuredEndRevealWidthPx by remember { mutableIntStateOf(0) }
    var measuredCardWidthPx by remember { mutableIntStateOf(0) }
    val revealState = remember {
        AnchoredDraggableState(
            initialValue = SwipeRevealAnchor.Closed,
            anchors = DraggableAnchors {
                SwipeRevealAnchor.StartActions at with(density) { (startRevealWidth + cardActionGap).toPx() }
                SwipeRevealAnchor.Closed at 0f
                SwipeRevealAnchor.EndActions at -with(density) { (endRevealWidth + cardActionGap).toPx() }
            },
        )
    }
    SideEffect {
        val gapPx = with(density) { cardActionGap.toPx() }
        val startAnchor = with(density) { startRevealWidth.toPx() } + gapPx
        val endAnchor = (measuredEndRevealWidthPx.takeIf { it > 0 }?.toFloat()
            ?: with(density) { endRevealWidth.toPx() }) + gapPx
        val commitAnchor = maxOf(
            startAnchor + 1f,
            2f * measuredCardWidthPx * FULL_SWIPE_COMMIT_FRACTION - startAnchor,
        )
        revealState.updateAnchors(
            DraggableAnchors {
                SwipeRevealAnchor.StartActions at startAnchor
                SwipeRevealAnchor.StartCommit at commitAnchor
                SwipeRevealAnchor.Closed at 0f
                SwipeRevealAnchor.EndActions at -endAnchor
            },
        )
    }
    SyncSwipeReveal(pass.id, reorderingActive, openSwipePassId, revealState)
    LaunchedEffect(revealState) {
        snapshotFlow { revealState.settledValue }
            .distinctUntilChanged()
            .collect { anchor ->
                if (anchor == SwipeRevealAnchor.StartCommit) {
                    if (openSwipePassId.value == pass.id) openSwipePassId.value = null
                    onArchive(pass.id, restoring, pass.categoryId)
                    revealState.animateTo(SwipeRevealAnchor.Closed)
                }
            }
    }
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(revealState, measuredCardWidthPx) {
        if (measuredCardWidthPx == 0) return@LaunchedEffect
        val commitThreshold = measuredCardWidthPx * FULL_SWIPE_COMMIT_FRACTION
        var wasActionable = false
        snapshotFlow {
            val offset = revealState.offset
            !offset.isNaN() && offset >= commitThreshold
        }.collect { isActionable ->
            if (isActionable && !wasActionable) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            }
            wasActionable = isActionable
        }
    }
    fun runSwipeAction(action: () -> Unit) {
        action()
        if (openSwipePassId.value == pass.id) openSwipePassId.value = null
        scope.launch { revealState.animateTo(SwipeRevealAnchor.Closed) }
    }

    Box(modifier.clip(shape).onSizeChanged { measuredCardWidthPx = it.width }) {
        Box(Modifier.matchParentSize()) {
            val gapPx = with(density) { cardActionGap.toPx() }
            val minimumActionWidthPx = with(density) { startRevealWidth.toPx() }
            val rawOffset = revealState.offset.let { if (it.isNaN()) 0f else it.coerceAtLeast(0f) }
            val actionWidthPx = maxOf(minimumActionWidthPx, rawOffset - gapPx)
                .coerceAtMost(measuredCardWidthPx.takeIf { it > 0 }?.toFloat() ?: minimumActionWidthPx)
            val commitThresholdPx = measuredCardWidthPx * FULL_SWIPE_COMMIT_FRACTION
            val iconScale = if (commitThresholdPx > 0f) {
                1f + 0.16f * (rawOffset / commitThresholdPx).coerceIn(0f, 1f)
            } else {
                1f
            }
            PassActionButtonGroup(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight()
                    .graphicsLayer { alpha = if (revealState.offset.let { !it.isNaN() && it > 0f }) 1f else 0f }
                    .then(
                        if (revealState.currentValue == SwipeRevealAnchor.StartActions ||
                            revealState.currentValue == SwipeRevealAnchor.StartCommit
                        ) Modifier
                        else Modifier.clearAndSetSemantics {},
                    ),
            ) {
                customItem(
                    buttonGroupContent = {
                        PassActionButton(
                            icon = if (restoring) Icons.Default.Restore else Icons.Default.Archive,
                            label = archiveLabel,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            index = 0,
                            count = 1,
                            fillHeight = true,
                            width = with(density) { actionWidthPx.toDp() },
                            iconScale = iconScale,
                            onClick = { runSwipeAction { onArchive(pass.id, restoring, pass.categoryId) } },
                        )
                    },
                    menuContent = { _ -> },
                )
            }
            PassActionButtonGroup(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().wrapContentWidth()
                    .graphicsLayer { alpha = if (revealState.offset.let { !it.isNaN() && it < 0f }) 1f else 0f }
                    .onSizeChanged { measuredEndRevealWidthPx = it.width }
                    .testTag("pass_end_actions_${pass.id}")
                    .then(
                        if (revealState.currentValue == SwipeRevealAnchor.EndActions) Modifier
                        else Modifier.clearAndSetSemantics {},
                    ),
            ) {
                customItem(
                    buttonGroupContent = {
                        PassActionButton(
                            icon = Icons.Default.PushPin,
                            label = pinnedLabel,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            index = 0,
                            count = 3,
                            fillHeight = true,
                            onClick = { runSwipeAction { onToggleFavorite(pass.id) } },
                        )
                    },
                    menuContent = { _ -> },
                )
                customItem(
                    buttonGroupContent = {
                        PassActionButton(
                            icon = if (pass.isProtected) Icons.Default.LockOpen else Icons.Default.Lock,
                            label = protectLabel,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            index = 1,
                            count = 3,
                            fillHeight = true,
                            onClick = { runSwipeAction { onToggleProtected(pass.id) } },
                        )
                    },
                    menuContent = { _ -> },
                )
                customItem(
                    buttonGroupContent = {
                        PassActionButton(
                            icon = Icons.Default.Delete,
                            label = stringResource(R.string.category_delete),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            index = 2,
                            count = 3,
                            fillHeight = true,
                            onClick = { runSwipeAction { onDelete(pass.id, pass.categoryId) } },
                        )
                    },
                    menuContent = { _ -> },
                )
            }
        }
        Box(
            Modifier.testTag("pass_card_${pass.id}").offset {
                val swipeOffset = revealState.offset
                IntOffset(if (swipeOffset.isNaN()) 0 else swipeOffset.roundToInt(), 0)
            }.pointerInput(pass.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    openSwipePassId.value = pass.id
                    var pressed: Boolean
                    do {
                        pressed = awaitPointerEvent().changes.any { it.pressed }
                    } while (pressed)
                    if (revealState.currentValue == SwipeRevealAnchor.Closed && openSwipePassId.value == pass.id) {
                        openSwipePassId.value = null
                    }
                }
            }.anchoredDraggable(
                state = revealState,
                orientation = Orientation.Horizontal,
                enabled = !reorderingActive,
            ),
        ) {
            TicketRow(
                pass = pass,
                category = category,
                highlighted = highlighted,
                hero = hero,
                sectionOrder = sectionOrder,
                hiddenSections = hiddenSections,
                showProtectedPassLockIcon = showProtectedPassLockIcon,
                blurProtectedPassCards = blurProtectedPassCards,
                modifier = Modifier.semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(archiveLabel) {
                            runSwipeAction { onArchive(pass.id, restoring, pass.categoryId) }
                            true
                        },
                        CustomAccessibilityAction(deleteLabel) {
                            runSwipeAction { onDelete(pass.id, pass.categoryId) }
                            true
                        },
                        CustomAccessibilityAction(pinnedLabel) {
                            runSwipeAction { onToggleFavorite(pass.id) }
                            true
                        },
                        CustomAccessibilityAction(protectLabel) {
                            runSwipeAction { onToggleProtected(pass.id) }
                            true
                        },
                    )
                },
                shape = shape,
                onOpen = onOpen,
                onReorderStart = onReorderStart,
                onReorderDrag = onReorderDrag,
                onReorderEnd = onReorderEnd,
                onReorderCancel = onReorderCancel,
                onPreviewChanged = { visible -> onPreviewChanged(pass.id.takeIf { visible }) },
                onPreviewOpeningChanged = { opening -> onPreviewOpeningChanged(pass.id.takeIf { opening }) },
                onProtectedPreviewRequested = onProtectedPreviewRequested,
                tagCategories = tagCategories,
            )
        }
    }
}

@Composable
private fun SyncSwipeReveal(
    passId: String,
    reorderingActive: Boolean,
    openPassId: androidx.compose.runtime.MutableState<String?>,
    state: AnchoredDraggableState<SwipeRevealAnchor>,
) {
    LaunchedEffect(reorderingActive) {
        if (reorderingActive) {
            state.animateTo(SwipeRevealAnchor.Closed)
            if (openPassId.value == passId) openPassId.value = null
        }
    }
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeRevealAnchor.Closed) {
            if (openPassId.value == passId) openPassId.value = null
        } else {
            openPassId.value = passId
        }
    }
    LaunchedEffect(openPassId.value) {
        if (openPassId.value != null && openPassId.value != passId) state.animateTo(SwipeRevealAnchor.Closed)
    }
}

@Composable
private fun TicketRow(
    pass: PassUiModel,
    category: PassCategory?,
    highlighted: Boolean,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    showProtectedPassLockIcon: Boolean,
    blurProtectedPassCards: Boolean,
    modifier: Modifier,
    shape: Shape,
    onOpen: (String) -> Unit,
    onReorderStart: () -> Unit,
    onReorderDrag: (Float) -> Unit,
    onReorderEnd: () -> Unit,
    onReorderCancel: () -> Unit,
    onPreviewChanged: (Boolean) -> Unit,
    onPreviewOpeningChanged: (Boolean) -> Unit,
    onProtectedPreviewRequested: () -> Unit,
    tagCategories: List<PassCategory>,
) {
    val interactionSource = remember(pass.id) { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val windowInfo = LocalWindowInfo.current
    val entranceDistancePx = remember(windowInfo.containerSize) {
        windowInfo.containerSize.width.toFloat()
    }
    val slideIn = remember(pass.id) { Animatable(0f) }
    LaunchedEffect(pass.id, highlighted) {
        if (highlighted) {
            slideIn.snapTo(1f)
            slideIn.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        } else {
            slideIn.snapTo(0f)
        }
    }
    val settleGlow by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 250),
        label = "settleGlow",
    )
    Surface(
        color = if (hero) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (hero) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = shape,
        border = BorderStroke(
            2.dp * settleGlow,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f * settleGlow),
        ),
        modifier = modifier.fillMaxWidth().clip(shape).graphicsLayer {
            translationX = slideIn.value * entranceDistancePx
            alpha = 1f - slideIn.value * 0.35f
        }.animateContentSize()
            .indication(interactionSource, LocalIndication.current),
    ) {
        Box {
        Row(
            Modifier.fillMaxWidth().padding(if (hero) 20.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val contentBlurred = pass.isProtected && blurProtectedPassCards
            val blurredDescription = stringResource(R.string.home_protected_pass_information_blurred)
            val openLabel = if (contentBlurred) {
                stringResource(R.string.home_open_protected_pass)
            } else {
                stringResource(R.string.home_open_pass_description, pass.description)
            }
            Box(Modifier.weight(1f)) {
            HomeCardBody(
                pass = pass,
                hero = hero,
                sectionOrder = sectionOrder,
                hiddenSections = hiddenSections,
                tagCategories = tagCategories,
                modifier = Modifier.fillMaxWidth()
                    .softProtectedBlur(contentBlurred)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        if (pass.isProtected && blurProtectedPassCards) {
                            contentDescription = blurredDescription
                        }
                        onClick(label = openLabel) {
                            onOpen(pass.id)
                            true
                        }
                    }
                    .pointerInput(pass.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val press = PressInteraction.Press(down.position)
                            interactionSource.tryEmit(press)
                            var releasedBeforeLongPress = false
                            var movedBeforeLongPress = false
                            val held = withTimeoutOrNull(750) {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        releasedBeforeLongPress = true
                                        return@withTimeoutOrNull false
                                    }
                                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                        movedBeforeLongPress = true
                                        return@withTimeoutOrNull false
                                    }
                                }
                                @Suppress("UNREACHABLE_CODE") false
                            } ?: true
                            if (held && pass.isProtected) {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                onProtectedPreviewRequested()
                            } else if (held) {
                                onPreviewChanged(true)
                                var opening = false
                                var releasedAfterPreview = false
                                try {
                                    while (true) {
                                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                        val aboveOpenThreshold = change != null && down.position.y - change.position.y > 72.dp.toPx()
                                        if (!opening && aboveOpenThreshold) {
                                            opening = true
                                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                                            onPreviewOpeningChanged(true)
                                        } else if (opening && !aboveOpenThreshold) {
                                            opening = false
                                            onPreviewOpeningChanged(false)
                                        }
                                        change?.consume()
                                        if (change == null || !change.pressed) {
                                            releasedAfterPreview = change != null
                                            break
                                        }
                                    }
                                } finally {
                                    if (opening && releasedAfterPreview) {
                                        scope.launch {
                                            delay(180)
                                            onPreviewChanged(false)
                                            onOpen(pass.id)
                                        }
                                    } else {
                                        interactionSource.tryEmit(PressInteraction.Release(press))
                                        onPreviewChanged(false)
                                    }
                                }
                            } else if (!held && releasedBeforeLongPress && !movedBeforeLongPress) {
                                interactionSource.tryEmit(PressInteraction.Release(press))
                                onOpen(pass.id)
                            } else {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                            }
                        }
                },
            )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.DragHandle,
                    stringResource(R.string.home_reorder_pass_description, pass.description),
                    Modifier.size(40.dp).pointerInput(pass.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onReorderStart() },
                            onDragEnd = onReorderEnd,
                            onDragCancel = onReorderCancel,
                            onDrag = { change, amount ->
                                change.consume()
                                onReorderDrag(amount.y)
                            },
                        )
                    }.padding(8.dp),
                )
            }
        }
        if (pass.isProtected && showProtectedPassLockIcon) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(bottomStart = 18.dp),
            ) {
                Icon(Icons.Default.Lock, stringResource(R.string.pass_detail_protected_pass), Modifier.padding(8.dp).size(18.dp))
            }
        }
        }
    }
}

@Composable
internal fun HomeCardBody(
    pass: PassUiModel,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    tagCategories: List<PassCategory>,
    modifier: Modifier = Modifier,
    thumbnailFallback: (@Composable (Modifier) -> Unit)? = null,
) {
    val cardLines = resolvePassCardLines(pass, sectionOrder, hiddenSections, tagCategories, hero)
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (HomeCardSection.ARTWORK !in hiddenSections) {
            PassThumbnail(pass, Modifier.size(if (hero) 44.dp else 32.dp), thumbnailFallback)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            cardLines.forEachIndexed { index, line ->
                when (line) {
                    is PassCardLine.Text -> Text(
                        line.value,
                        style = homeCardLineStyle(index),
                        fontWeight = homeCardLineWeight(index),
                        maxLines = line.maxLines,
                        overflow = if (line.ellipsize) TextOverflow.Ellipsis else TextOverflow.Clip,
                    )
                    is PassCardLine.Metadata -> CardMetadataRow(
                        typeLabel = line.typeLabel,
                        tags = line.tags,
                        typeStyle = homeCardLineStyle(index),
                        typeWeight = homeCardLineWeight(index),
                    )
                }
            }
            if (pass.isExpired()) ExpiredBadge()
        }
    }
}

@Composable
private fun homeCardLineStyle(lineIndex: Int) = when (lineIndex) {
    0 -> MaterialTheme.typography.titleMedium
    1 -> MaterialTheme.typography.bodyMedium
    else -> MaterialTheme.typography.bodySmall
}

private fun homeCardLineWeight(lineIndex: Int): FontWeight? = if (lineIndex == 0) FontWeight.SemiBold else null

@Composable
private fun CardMetadataRow(
    typeLabel: String?,
    tags: List<PassCategory>,
    typeStyle: TextStyle,
    typeWeight: FontWeight?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val typeLabelStyle = typeStyle.copy(fontWeight = typeWeight ?: typeStyle.fontWeight)
        val itemGap = 8.dp
        val compactGap = 6.dp
        val dotSize = 10.dp
        val badgePadding = 8.dp
        val badgeInnerGap = 6.dp
        val safetyMargin = 4.dp
        val fullWidth = with(density) {
            val children = (if (typeLabel != null) 1 else 0) + tags.size
            var width = itemGap.toPx() * (children - 1).coerceAtLeast(0)
            if (typeLabel != null) {
                width += measurer.measure(AnnotatedString(typeLabel), style = typeLabelStyle, maxLines = 1, softWrap = false).size.width
            }
            tags.forEach { tag ->
                val nameWidth = measurer.measure(AnnotatedString(tag.name), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false).size.width
                width += badgePadding.toPx() * 2 + dotSize.toPx() + badgeInnerGap.toPx() + nameWidth
            }
            width
        }
        val compact = with(density) { fullWidth + safetyMargin.toPx() } > with(density) { maxWidth.toPx() }
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (compact) compactGap else itemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            typeLabel?.let { Text(it, style = typeStyle, fontWeight = typeWeight) }
            if (compact) {
                tags.take(MAX_CARD_TAG_DOTS).forEach { CategoryDot(it.colorArgb) }
                if (tags.size > MAX_CARD_TAG_DOTS) {
                    Text(
                        "+${tags.size - MAX_CARD_TAG_DOTS}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                tags.forEach { CategoryBadge(it) }
            }
        }
    }
}

@Composable
internal fun PassHoldPreview(pass: PassUiModel, opening: Boolean, modifier: Modifier) {
    val openingProgress by animateFloatAsState(
        targetValue = if (opening) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "passPreviewOpening",
    )
    Surface(
        modifier = modifier.graphicsLayer {
            translationY = -96.dp.toPx() * openingProgress
            scaleX = 1f + (0.04f * openingProgress)
            scaleY = 1f + (0.04f * openingProgress)
        },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(pass.description, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            pass.creator?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (pass.barcodeFormat != null && !pass.barcodeMessage.isNullOrBlank()) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                    PassCodeImage(
                        format = pass.barcodeFormat,
                        message = pass.barcodeMessage,
                        modifier = Modifier.fillMaxWidth().height(if (pass.barcodeFormat.isQuadratic()) 280.dp else 160.dp)
                            .padding(12.dp),
                        contentDescription = stringResource(R.string.home_preview_pass_code),
                    )
                }
            }
            pass.dateLabel()?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            pass.fields.filterNot { it.hidden }.take(4).forEach { field ->
                Text("${field.label}: ${field.value}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PassThumbnail(pass: PassUiModel, modifier: Modifier, fallback: (@Composable (Modifier) -> Unit)? = null) {
    val artworkDescription = stringResource(R.string.pass_detail_pass_artwork)
    val artworkKinds = if (pass.isDocumentPass) {
        listOf(PassArtworkKind.THUMBNAIL, PassArtworkKind.STRIP, PassArtworkKind.ICON, PassArtworkKind.LOGO)
    } else {
        listOf(PassArtworkKind.ICON, PassArtworkKind.THUMBNAIL, PassArtworkKind.LOGO)
    }
    val artwork = pass.displayArtwork(artworkKinds)
    if (artwork != null) {
        AdaptivePassArtwork(
            bytes = artwork.bytes,
            kind = artwork.kind,
            accentColor = pass.accentColor,
            contentDescription = artworkDescription,
            modifier = modifier,
            context = PassArtworkContext.HOME_THUMBNAIL,
        )
    } else if (fallback != null) {
        fallback(modifier)
    } else {
        Surface(
            modifier = modifier.semantics { contentDescription = artworkDescription },
            shape = RoundedCornerShape(16),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = pass.homeCardInitial(),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: PassCategory) {
    Surface(color = Color(category.colorArgb.toInt()).copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryDot(category.colorArgb)
            Spacer(Modifier.size(6.dp))
            Text(category.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
private fun ExpiredBadge() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(50),
        modifier = Modifier.testTag("expired_badge"),
    ) {
        Text(
            stringResource(R.string.home_expired),
            Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun CategoryDot(colorArgb: Long) {
    Box(Modifier.size(10.dp).background(Color(colorArgb.toInt()), RoundedCornerShape(50)))
}

private const val MAX_CARD_TAG_DOTS = 6

private const val FULL_SWIPE_COMMIT_FRACTION = 0.62f
