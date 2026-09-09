package org.ligi.passandroid.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.domain.timeline.normalizedTimeSpan
import java.time.Clock

class PassWidgetSnapshotPublisher(
    private val context: Context,
    private val store: PassWidgetSnapshotStore = PassWidgetSnapshotStore(context),
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun publish(
        passes: List<PassSnapshot>,
        excludedCategoryIds: Set<String>,
    ) {
        val widgetPasses = passes.asSequence()
            .filterNot { it.isArchived || it.categoryId in excludedCategoryIds }
            .map { pass ->
                val span = pass.normalizedTimeSpan()
                WidgetPass(
                    id = pass.id,
                    title = pass.description.ifBlank { "Pass" },
                    startsAtEpochMillis = span?.startsAt?.toEpochMilli(),
                    endsAtEpochMillis = span?.endsAt?.toEpochMilli(),
                    location = pass.locations.firstNotNullOfOrNull { it.name?.takeIf(String::isNotBlank) },
                    supportingText = pass.fields.firstNotNullOfOrNull { field ->
                        field.value.takeIf { !field.hidden && it.isNotBlank() }
                    },
                )
            }
            .toList()
        val snapshot = PassWidgetSnapshot(
            passes = widgetPasses,
            generatedAtEpochMillis = clock.millis(),
        )
        store.replace(snapshot)
        PassOverviewWidget().updateAll(context)
        scheduleNextWidgetRefresh(context, snapshot)
    }
}
