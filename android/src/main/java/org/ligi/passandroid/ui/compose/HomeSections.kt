package org.ligi.passandroid.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.ligi.passandroid.ui.state.PassUiModel

internal data class HomePassSections<T>(
    val today: List<T>,
    val pinned: List<T>,
    val other: List<T>,
) {
    val showOtherHeading: Boolean get() = other.isNotEmpty() && (today.isNotEmpty() || pinned.isNotEmpty())
}

internal fun <T> deriveHomePassSections(
    passes: List<T>,
    highlightTodayPasses: Boolean,
    isToday: (T) -> Boolean,
    isPinned: (T) -> Boolean,
): HomePassSections<T> {
    val today = passes.filter { highlightTodayPasses && isToday(it) }
    val pinned = passes.filter { !(highlightTodayPasses && isToday(it)) && isPinned(it) }
    val other = passes.filter { !(highlightTodayPasses && isToday(it)) && !isPinned(it) }
    return HomePassSections(today, pinned, other)
}
