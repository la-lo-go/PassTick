package org.ligi.passandroid.ui.state

import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.PassDetailSection
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.navigation.PassDateField
import org.ligi.passandroid.repository.PassImageExportOptions

sealed interface PassDetailAction {
    data object Back : PassDetailAction
    data object Edit : PassDetailAction
    data class EditDate(val field: PassDateField) : PassDetailAction
    data object Share : PassDetailAction
    data object Export : PassDetailAction
    data object OpenImageExport : PassDetailAction
    data object SaveBarcodeImage : PassDetailAction
    data object AddToCalendar : PassDetailAction
    data object OpenReminderSettings : PassDetailAction
    data object OpenPassViewSettings : PassDetailAction
    data object OpenPassCustomization : PassDetailAction
    data object OpenTagSettings : PassDetailAction
    data class ConfigureReminder(
        val enabled: Boolean,
        val leadMinutes: Int?,
        val exactAtEvent: Boolean = false,
    ) : PassDetailAction
    data class SetReminderActions(val actions: Set<org.ligi.passandroid.reminder.NotificationAction>?) : PassDetailAction
    data class SetFlashlightEnabled(val enabled: Boolean) : PassDetailAction
    data class OpenLocation(val index: Int) : PassDetailAction
    data class MoveToCategory(val categoryId: String) : PassDetailAction
    data class SetTags(val tagIds: Set<String>) : PassDetailAction
    data class SetProtected(val isProtected: Boolean) : PassDetailAction
    data class SetArchived(val isArchived: Boolean) : PassDetailAction
    data object Delete : PassDetailAction
}

sealed interface PassCustomizationAction {
    data object Back : PassCustomizationAction
    data object OpenLayout : PassCustomizationAction
    data class SelectArtwork(val kind: org.ligi.passandroid.repository.PassArtworkKind?) : PassCustomizationAction
}

sealed interface EditPassAction {
    data object Back : EditPassAction
    data class Save(val draft: PassDraft) : EditPassAction
}

sealed interface PassImageExportAction {
    data object Back : PassImageExportAction
    data class SetOptions(val value: PassImageExportOptions) : PassImageExportAction
    data object Save : PassImageExportAction
    data object Share : PassImageExportAction
    data object Print : PassImageExportAction
}

sealed interface SettingsAction {
    data object Back : SettingsAction
    data class SetTheme(val value: ThemeMode) : SettingsAction
    data class SetAmoledBlackBackground(val value: Boolean) : SettingsAction
    data class SetAutomaticBrightness(val value: Boolean) : SettingsAction
    data class SetSortOrder(val value: PassSortOrder) : SettingsAction
    data object OpenCategories : SettingsAction
    data object OpenPassViewSettings : SettingsAction
    data object OpenHomeCardSettings : SettingsAction
    data class SetHighlightTodayPasses(val value: Boolean) : SettingsAction
    data class SetAutomaticallyMarkPast(val value: Boolean) : SettingsAction
    data class SetTrashEnabled(val value: Boolean) : SettingsAction
    data class SetOfferCalendarAfterImport(val value: Boolean) : SettingsAction
    data class SetRemindersEnabled(val value: Boolean) : SettingsAction
    data class SetReminderMinutes(val value: Set<Int>) : SettingsAction
    data class SetNotificationAccessWindow(val minutes: Int) : SettingsAction
    data class SetNotificationExactTiming(val value: Boolean) : SettingsAction
    data class SetNotificationActionsEnabled(val value: Boolean) : SettingsAction
    data class SetLockAllPasses(val value: Boolean) : SettingsAction
    data class SetShowProtectedPassLockIcon(val value: Boolean) : SettingsAction
    data class SetBlurProtectedPassCards(val value: Boolean) : SettingsAction
    data class SetSeparateProtectedPasses(val value: Boolean) : SettingsAction
    data class SetBlockScreenshots(val value: Boolean) : SettingsAction
    data object OpenPrivacyPolicy : SettingsAction
    data object OpenSourceCode : SettingsAction
    data object OpenBugReportGitHub : SettingsAction
    data object OpenBugReportEmail : SettingsAction
}

sealed interface PassDetailLayoutSettingsAction {
    data object Back : PassDetailLayoutSettingsAction
    data class Move(val section: PassDetailSection, val offset: Int) : PassDetailLayoutSettingsAction
    data class SetVisible(val section: PassDetailSection, val visible: Boolean) : PassDetailLayoutSettingsAction
}

sealed interface HomeCardLayoutSettingsAction {
    data object Back : HomeCardLayoutSettingsAction
    data class Move(val section: HomeCardSection, val offset: Int) : HomeCardLayoutSettingsAction
    data class SetVisible(val section: HomeCardSection, val visible: Boolean) : HomeCardLayoutSettingsAction
}

sealed interface CategorySettingsAction {
    data object Back : CategorySettingsAction
    data class Save(val category: org.ligi.passandroid.repository.PassCategory) : CategorySettingsAction
    data class Delete(val categoryId: String) : CategorySettingsAction
    data class Move(val categoryId: String, val offset: Int) : CategorySettingsAction
}
