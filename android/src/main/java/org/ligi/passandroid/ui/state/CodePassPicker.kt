package org.ligi.passandroid.ui.state

import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.widget.sortedForDisplay
import org.ligi.passandroid.widget.widgetPasses
import java.time.Instant

data class CodePassPickerRow(
    val passId: String,
    val title: String,
    val subtitle: String?,
    val selected: Boolean,
)

/**
 * Rows for the code pass picker. The list follows the configured sort order and every row shows
 * the first visible home card lines, so the picker matches what the widget and the shortcuts show.
 */
fun codePassPickerRows(
    passes: List<PassSnapshot>,
    settings: AppSettings,
    now: Instant = Instant.now(),
): List<CodePassPickerRow> {
    val excludedCategoryIds = settings.categories
        .filter { it.role == PassCategoryRole.ARCHIVE || it.role == PassCategoryRole.TRASH }
        .mapTo(mutableSetOf()) { it.id }
    val eligible = widgetPasses(
        passes,
        excludedCategoryIds,
        settings.lockAllPasses,
        settings.homeCardSectionOrder,
        settings.hiddenHomeCardSections,
        settings.categories,
    )
    val snapshotsById = passes.associateBy(PassSnapshot::id)
    return sortedForDisplay(eligible, settings.sortOrder, settings.passOrder, now).map { pass ->
        val lines = snapshotsById[pass.id]
            ?.let { snapshot ->
                resolvePassCardTextLines(
                    PassUiModel.fromCardLines(snapshot),
                    settings.homeCardSectionOrder,
                    settings.hiddenHomeCardSections,
                    settings.categories,
                )
            }
            .orEmpty()
        CodePassPickerRow(
            passId = pass.id,
            title = lines.firstOrNull() ?: pass.title,
            subtitle = lines.getOrNull(1),
            selected = pass.id == settings.codePassId,
        )
    }
}
