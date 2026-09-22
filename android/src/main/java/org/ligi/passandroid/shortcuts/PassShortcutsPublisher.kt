package org.ligi.passandroid.shortcuts

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.resolvePassCardTitle
import org.ligi.passandroid.widget.openPassIntent

fun interface ShortcutsPublisher {
    suspend fun publish(
        passes: List<PassSnapshot>,
        lockAllPasses: Boolean,
        homeCardSectionOrder: List<HomeCardSection>,
        hiddenHomeCardSections: Set<HomeCardSection>,
        tagCategories: List<PassCategory>,
    )
}

data class PassShortcutSpec(
    val id: String,
    val passId: String,
    val title: String,
)

/**
 * Pure shortcut definition. Privacy is enforced here: protected passes are dropped and
 * lock-all shows no shortcuts at all.
 */
internal fun shortcutSpecs(
    passes: List<PassSnapshot>,
    lockAllPasses: Boolean,
    homeCardSectionOrder: List<HomeCardSection>,
    hiddenHomeCardSections: Set<HomeCardSection>,
    tagCategories: List<PassCategory>,
): List<PassShortcutSpec> = if (lockAllPasses) {
    emptyList()
} else {
    passes.asSequence()
        .filterNot(PassSnapshot::isProtected)
        .filter(PassSnapshot::isPinned)
        .take(MAX_CODE_SHORTCUTS)
        .map { pass ->
            PassShortcutSpec(
                id = "code-${pass.id}",
                passId = pass.id,
                title = resolvePassCardTitle(
                    PassUiModel.fromCardLines(pass),
                    homeCardSectionOrder,
                    hiddenHomeCardSections,
                    tagCategories,
                ).ifBlank { "Pass" },
            )
        }
        .toList()
}

internal fun PassShortcutSpec.toShortcutInfoCompat(context: Context): ShortcutInfoCompat =
    ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(title)
        .setLongLabel(title)
        .setIntent(openPassIntent(context, passId, showCode = true))
        .build()

class PassShortcutsPublisher(private val context: Context) : ShortcutsPublisher {
    override suspend fun publish(
        passes: List<PassSnapshot>,
        lockAllPasses: Boolean,
        homeCardSectionOrder: List<HomeCardSection>,
        hiddenHomeCardSections: Set<HomeCardSection>,
        tagCategories: List<PassCategory>,
    ) {
        val specs = shortcutSpecs(
            passes,
            lockAllPasses,
            homeCardSectionOrder,
            hiddenHomeCardSections,
            tagCategories,
        )
        ShortcutManagerCompat.setDynamicShortcuts(context, specs.map { it.toShortcutInfoCompat(context) })
    }
}

private const val MAX_CODE_SHORTCUTS = 4