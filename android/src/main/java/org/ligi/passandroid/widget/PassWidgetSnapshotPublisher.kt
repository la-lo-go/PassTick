package org.ligi.passandroid.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.domain.timeline.normalizedTimeSpan
import java.time.Clock

/** Publishes a widget snapshot so ViewModels can stay independent from the Android widget code. */
fun interface WidgetSnapshotPublisher {
    suspend fun publish(passes: List<PassSnapshot>, excludedCategoryIds: Set<String>, lockAllPasses: Boolean)
}

class PassWidgetSnapshotPublisher(
    private val context: Context,
    private val store: PassWidgetSnapshotStore = PassWidgetSnapshotStore(context),
    private val clock: Clock = Clock.systemUTC(),
) : WidgetSnapshotPublisher {
    override suspend fun publish(
        passes: List<PassSnapshot>,
        excludedCategoryIds: Set<String>,
        lockAllPasses: Boolean,
    ) {
        val snapshot = PassWidgetSnapshot(
            passes = widgetPasses(passes, excludedCategoryIds, lockAllPasses),
            generatedAtEpochMillis = clock.millis(),
        )
        store.replace(snapshot)
        PassOverviewWidget().updateAll(context)
        scheduleNextWidgetRefresh(context, snapshot)
    }
}

internal fun widgetPasses(
    passes: List<PassSnapshot>,
    excludedCategoryIds: Set<String>,
    lockAllPasses: Boolean,
): List<WidgetPass> = passes.asSequence()
    .filterNot { it.isArchived || it.categoryId in excludedCategoryIds }
    .filterNot { lockAllPasses || it.isProtected }
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
