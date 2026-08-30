package org.ligi.passandroid.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import org.ligi.passandroid.repository.PassSnapshot
import java.time.Clock

class PassWidgetSnapshotPublisher(
    private val context: Context,
    private val store: PassWidgetSnapshotStore = PassWidgetSnapshotStore(context),
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun publish(
        passes: List<PassSnapshot>,
        excludedCategoryIds: Set<String>,
        quickCodePassId: String?,
    ) {
        val widgetPasses = passes.asSequence()
            .filterNot { it.categoryId in excludedCategoryIds }
            .map { pass ->
                WidgetPass(
                    id = pass.id,
                    title = pass.description.ifBlank { "Pass" },
                    startsAtEpochMillis = pass.calendarTimeSpan?.from?.toInstant()?.toEpochMilli(),
                    endsAtEpochMillis = pass.calendarTimeSpan?.to?.toInstant()?.toEpochMilli(),
                    location = pass.locations.firstNotNullOfOrNull { it.name?.takeIf(String::isNotBlank) },
                    supportingText = pass.fields.firstNotNullOfOrNull { field ->
                        field.value.takeIf { !field.hidden && it.isNotBlank() }
                    },
                    hasCode = !pass.barcodeMessage.isNullOrBlank(),
                )
            }
            .toList()
        store.replace(
            PassWidgetSnapshot(
                passes = widgetPasses,
                quickCodePassId = quickCodePassId,
                generatedAtEpochMillis = clock.millis(),
            ),
        )
        PassOverviewWidget().updateAll(context)
        QuickCodeWidget().updateAll(context)
    }
}
