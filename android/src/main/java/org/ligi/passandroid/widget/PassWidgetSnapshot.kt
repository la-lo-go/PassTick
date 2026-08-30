package org.ligi.passandroid.widget

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WidgetPass(
    val id: String,
    val title: String,
    val startsAtEpochMillis: Long?,
    val endsAtEpochMillis: Long?,
    val location: String?,
    val supportingText: String?,
    val hasCode: Boolean,
)

data class PassWidgetSnapshot(
    val passes: List<WidgetPass> = emptyList(),
    val quickCodePassId: String? = null,
    val generatedAtEpochMillis: Long = 0L,
)

data class WidgetPassSelection(
    val currentOrNext: WidgetPass?,
    val today: List<WidgetPass>,
    val quickCode: WidgetPass?,
)

fun PassWidgetSnapshot.select(
    now: Instant,
    zoneId: ZoneId,
    todayLimit: Int = 4,
): WidgetPassSelection {
    require(todayLimit >= 0) { "Today limit must not be negative" }
    val ordered = passes.sortedWith(
        compareBy<WidgetPass> { it.startsAtEpochMillis ?: Long.MAX_VALUE }
            .thenBy(WidgetPass::title)
            .thenBy(WidgetPass::id),
    )
    val nowMillis = now.toEpochMilli()
    val currentOrNext = ordered.firstOrNull { pass ->
        val end = pass.endsAtEpochMillis ?: pass.startsAtEpochMillis
        end == null || end >= nowMillis
    }
    val todayDate = now.atZone(zoneId).toLocalDate()
    val today = ordered.asSequence()
        .filter { pass -> pass.occursOn(todayDate, zoneId) }
        .filter { pass -> (pass.endsAtEpochMillis ?: Long.MAX_VALUE) >= nowMillis }
        .take(todayLimit)
        .toList()
    return WidgetPassSelection(
        currentOrNext = currentOrNext,
        today = today,
        quickCode = quickCodePassId?.let { id -> passes.firstOrNull { it.id == id && it.hasCode } },
    )
}

private fun WidgetPass.occursOn(date: LocalDate, zoneId: ZoneId): Boolean {
    val start = startsAtEpochMillis ?: return false
    val end = endsAtEpochMillis ?: start
    val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val nextDayStart = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return end >= dayStart && start < nextDayStart
}
