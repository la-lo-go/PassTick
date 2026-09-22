package org.ligi.passandroid.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.ligi.passandroid.ui.state.ARCHIVED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.PINNED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.PROTECTED_PASSES_CATEGORY_ID
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

/** Selects the passes of the active filter. Virtual categories use pass facts, not stored ids. */
internal fun selectHomePasses(
    passes: List<PassUiModel>,
    selectedCategoryId: String?,
    hiddenCategoryIds: Set<String>,
    protectedPassIds: Set<String>,
): List<PassUiModel> = when (selectedCategoryId) {
    PROTECTED_PASSES_CATEGORY_ID -> passes.filter { it.id in protectedPassIds }
    PINNED_PASSES_CATEGORY_ID -> passes.filter(PassUiModel::isFavorite)
    ARCHIVED_PASSES_CATEGORY_ID -> passes.filter(PassUiModel::isArchived)
    null -> passes.filterNot { it.categoryId in hiddenCategoryIds || it.isArchived }
    else -> passes.filter { it.categoryId == selectedCategoryId || selectedCategoryId in it.tagIds }
}
