package org.ligi.passandroid.widget

import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WidgetPass(
    val id: String,
    val title: String,
    /** First visible line of the home card layout. Null in snapshots written before this field. */
    val primaryLine: String? = null,
    val issuer: String?,
    val type: PassType,
    val isPinned: Boolean,
    val startsAtEpochMillis: Long?,
    val endsAtEpochMillis: Long?,
    val location: String?,
    val supportingText: String?,
    val barcodeFormat: PassBarCodeFormat?,
    val barcodeMessage: String?,
)

data class PassWidgetSnapshot(
    val passes: List<WidgetPass> = emptyList(),
    val generatedAtEpochMillis: Long = 0L,
)

data class WidgetPassSelection(
    val currentOrNext: WidgetPass?,
    val today: List<WidgetPass>,
    val active: List<WidgetPass>,
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
    val active = ordered.filter { pass ->
        val end = pass.endsAtEpochMillis ?: pass.startsAtEpochMillis
        end == null || end >= nowMillis
    }
    return WidgetPassSelection(
        currentOrNext = currentOrNext,
        today = today,
        active = active,
    )
}

fun PassWidgetSnapshot.nextRefreshAt(now: Instant, zoneId: ZoneId): Instant {
    val nowMillis = now.toEpochMilli()
    val nextMidnight = now.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant()
    val eventBoundary = passes.asSequence()
        .flatMap { sequenceOf(it.startsAtEpochMillis, it.endsAtEpochMillis).filterNotNull() }
        .filter { it > nowMillis }
        .minOrNull()
        ?.let { Instant.ofEpochMilli(it + 1) }
    return listOfNotNull(nextMidnight, eventBoundary).minOrNull() ?: nextMidnight
}

private fun WidgetPass.occursOn(date: LocalDate, zoneId: ZoneId): Boolean {
    val start = startsAtEpochMillis ?: return false
    val end = endsAtEpochMillis ?: start
    val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val nextDayStart = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return end >= dayStart && start < nextDayStart
}
