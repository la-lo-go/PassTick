package org.ligi.passandroid.ui.state

import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.PassDetailSection

sealed interface PassDetailAction {
    data object Back : PassDetailAction
    data object Edit : PassDetailAction
    data object Delete : PassDetailAction
    data object Share : PassDetailAction
    data object Print : PassDetailAction
    data object AddToCalendar : PassDetailAction
    data object OpenReminderSettings : PassDetailAction
    data class ConfigureReminder(val enabled: Boolean, val leadMinutes: Int?) : PassDetailAction
    data class SetFlashlightEnabled(val enabled: Boolean) : PassDetailAction
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
    data class SetAmoledBlackBackground(val value: Boolean) : SettingsAction
    data class SetAutomaticBrightness(val value: Boolean) : SettingsAction
    data class SetSortOrder(val value: PassSortOrder) : SettingsAction
    data object OpenCategories : SettingsAction
    data object OpenPassDetailLayout : SettingsAction
    data class SetHighlightTodayPasses(val value: Boolean) : SettingsAction
    data class SetAutomaticallyMarkPast(val value: Boolean) : SettingsAction
    data class SetOfferCalendarAfterImport(val value: Boolean) : SettingsAction
    data class SetRemindersEnabled(val value: Boolean) : SettingsAction
    data class SetReminderMinutes(val value: Set<Int>) : SettingsAction
}

sealed interface PassDetailLayoutSettingsAction {
    data object Back : PassDetailLayoutSettingsAction
    data class Move(val section: PassDetailSection, val offset: Int) : PassDetailLayoutSettingsAction
    data class SetVisible(val section: PassDetailSection, val visible: Boolean) : PassDetailLayoutSettingsAction
}

sealed interface CategorySettingsAction {
    data object Back : CategorySettingsAction
    data class Save(val category: org.ligi.passandroid.repository.PassCategory) : CategorySettingsAction
    data class Delete(val categoryId: String) : CategorySettingsAction
    data class Move(val categoryId: String, val offset: Int) : CategorySettingsAction
}
