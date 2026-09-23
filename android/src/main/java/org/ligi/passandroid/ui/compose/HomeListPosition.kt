package org.ligi.passandroid.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Scroll position of the home pass list.
 *
 * The position is shared between home compositions so navigation keeps the place. The list state
 * itself must stay per composition: the hidden list pane of the detail shell measures with a
 * degenerate viewport and corrupts a shared state, which leaves the visible list unable to scroll.
 */
internal class HomeListPosition {
    var firstVisibleItemIndex: Int = 0
    var firstVisibleItemScrollOffset: Int = 0
}

@Composable
internal fun rememberHomeListPosition(): HomeListPosition = rememberSaveable(
    saver = listSaver(
        save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
        restore = {
            HomeListPosition().apply {
                firstVisibleItemIndex = it[0]
                firstVisibleItemScrollOffset = it[1]
            }
        },
    ),
) {
    HomeListPosition()
}
