package org.ligi.passandroid.widget

import org.ligi.passandroid.model.comparator.PassSortOrder
import java.time.Instant
import java.time.ZoneId

/**
 * Resolves the pass shown by the code widget and the Quick Settings tile.
 *
 * Rule order:
 * 1. The pass chosen in the widget configuration, when still present in the snapshot.
 * 2. The first pinned pass in the current sort order.
 * 3. The next upcoming pass from the snapshot.
 * 4. Null, which surfaces open the app instead.
 *
 * The snapshot already excludes archived, hidden, protected, and locked passes, so
 * a missing [codePassId] falls back without ever leaking sensitive passes.
 */
fun resolveCodePass(
    passes: List<WidgetPass>,
    codePassId: String?,
    sortOrder: PassSortOrder = PassSortOrder.DATE_DESC,
    passOrder: List<String> = emptyList(),
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): WidgetPass? = codePassId
    ?.let { id -> passes.firstOrNull { it.id == id } }
    ?: sortedForDisplay(passes, sortOrder, passOrder, now).firstOrNull(WidgetPass::isPinned)
    ?: PassWidgetSnapshot(passes).select(now, zoneId).currentOrNext

internal fun sortedForDisplay(
    passes: List<WidgetPass>,
    sortOrder: PassSortOrder,
    passOrder: List<String>,
    now: Instant,
): List<WidgetPass> = when (sortOrder) {
    PassSortOrder.DATE_ASC -> passes.sortedWith(displayDateComparator())
    PassSortOrder.DATE_DESC -> passes.sortedWith(displayDateComparator(descending = true))
    PassSortOrder.TYPE -> passes.sortedWith(compareBy(WidgetPass::type).then(displayDateComparator()))
    PassSortOrder.DATE_DIFF -> passes.sortedWith(
        compareBy<WidgetPass> { pass -> distanceMillis(pass, now) }.then(displayDateComparator()),
    )
    PassSortOrder.MANUAL -> {
        val positions = passOrder.withIndex().associate { it.value to it.index }
        passes.sortedWith(compareBy<WidgetPass> { positions[it.id] ?: Int.MAX_VALUE }.thenBy(WidgetPass::id))
    }
}

private fun displayDateComparator(descending: Boolean = false): Comparator<WidgetPass> =
    Comparator<WidgetPass> { left, right ->
        val leftDate = left.startsAtEpochMillis
        val rightDate = right.startsAtEpochMillis
        when {
            leftDate == rightDate -> 0
            leftDate == null -> 1
            rightDate == null -> -1
            descending -> rightDate.compareTo(leftDate)
            else -> leftDate.compareTo(rightDate)
        }
    }.thenBy(WidgetPass::title).thenBy(WidgetPass::id)

private fun distanceMillis(pass: WidgetPass, now: Instant): Long? =
    pass.startsAtEpochMillis?.let { kotlin.math.abs(now.toEpochMilli() - it) }