package org.ligi.passandroid.ui.state

import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.ThemeMode

sealed interface PassDetailAction {
    data object Back : PassDetailAction
    data object Edit : PassDetailAction
    data object Delete : PassDetailAction
    data object Export : PassDetailAction
    data object Share : PassDetailAction
    data object Print : PassDetailAction
    data object AddToCalendar : PassDetailAction
    data object OpenCode : PassDetailAction
    data object UseForQuickCodeWidget : PassDetailAction
    data object ToggleReminder : PassDetailAction
    data class OpenLocation(val index: Int) : PassDetailAction
    data class MoveToCategory(val categoryId: String) : PassDetailAction
}

sealed interface EditPassAction {
    data object Back : EditPassAction
    data class Save(val draft: PassDraft) : EditPassAction
}

sealed interface SettingsAction {
    data object Back : SettingsAction
    data class SetTheme(val value: ThemeMode) : SettingsAction
    data class SetCondensedPasses(val value: Boolean) : SettingsAction
    data class SetAutomaticBrightness(val value: Boolean) : SettingsAction
    data class SetSortOrder(val value: PassSortOrder) : SettingsAction
    data object OpenCategories : SettingsAction
    data class SetHighlightTodayPasses(val value: Boolean) : SettingsAction
    data class SetAutomaticallyMarkPast(val value: Boolean) : SettingsAction
    data class SetOfferCalendarAfterImport(val value: Boolean) : SettingsAction
    data class SetRemindersEnabled(val value: Boolean) : SettingsAction
    data class SetDefaultReminderMinutes(val value: Int) : SettingsAction
}

sealed interface CategorySettingsAction {
    data object Back : CategorySettingsAction
    data class Save(val category: org.ligi.passandroid.repository.PassCategory) : CategorySettingsAction
    data class Delete(val categoryId: String) : CategorySettingsAction
    data class Move(val categoryId: String, val offset: Int) : CategorySettingsAction
}
