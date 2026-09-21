package org.ligi.passandroid.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.domain.timeline.normalizedTimeSpan
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.resolvePassCardTitle
import java.time.Clock

/** Publishes a widget snapshot so ViewModels can stay independent from the Android widget code. */
fun interface WidgetSnapshotPublisher {
    suspend fun publish(
        passes: List<PassSnapshot>,
        excludedCategoryIds: Set<String>,
        lockAllPasses: Boolean,
        homeCardSectionOrder: List<HomeCardSection>,
        hiddenHomeCardSections: Set<HomeCardSection>,
        tagCategories: List<PassCategory>,
    )
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
        homeCardSectionOrder: List<HomeCardSection>,
        hiddenHomeCardSections: Set<HomeCardSection>,
        tagCategories: List<PassCategory>,
    ) {
        val snapshot = PassWidgetSnapshot(
            passes = widgetPasses(
                passes,
                excludedCategoryIds,
                lockAllPasses,
                homeCardSectionOrder,
                hiddenHomeCardSections,
                tagCategories,
            ),
            generatedAtEpochMillis = clock.millis(),
        )
        store.replace(snapshot)
        PassOverviewWidget().updateAll(context)
        PassCodeWidget().updateAll(context)
        scheduleNextWidgetRefresh(context, snapshot)
    }
}

internal fun widgetPasses(
    passes: List<PassSnapshot>,
    excludedCategoryIds: Set<String>,
    lockAllPasses: Boolean,
    homeCardSectionOrder: List<HomeCardSection>,
    hiddenHomeCardSections: Set<HomeCardSection>,
    tagCategories: List<PassCategory>,
): List<WidgetPass> = passes.asSequence()
    .filterNot { it.isArchived || it.categoryId in excludedCategoryIds }
    .filterNot { lockAllPasses || it.isProtected }
    .map { pass ->
        val span = pass.normalizedTimeSpan()
        WidgetPass(
            id = pass.id,
            title = pass.description.ifBlank { "Pass" },
            primaryLine = resolvePassCardTitle(
                PassUiModel.fromCardLines(pass),
                homeCardSectionOrder,
                hiddenHomeCardSections,
                tagCategories,
            ).ifBlank { null },
            issuer = pass.creator?.takeIf(String::isNotBlank),
            type = pass.type,
            isPinned = pass.isPinned,
            startsAtEpochMillis = span?.startsAt?.toEpochMilli(),
            endsAtEpochMillis = span?.endsAt?.toEpochMilli(),
            location = pass.locations.firstNotNullOfOrNull { it.name?.takeIf(String::isNotBlank) },
            supportingText = pass.fields.firstNotNullOfOrNull { field ->
                field.value.takeIf { !field.hidden && it.isNotBlank() }
            },
            barcodeFormat = pass.barcodeFormat,
            barcodeMessage = pass.barcodeMessage?.takeIf(String::isNotBlank),
        )
    }
    .toList()
