package org.ligi.passandroid.ui.state

import android.net.Uri
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.ThemeMode

sealed interface PassListAction {
    data class OpenPass(val id: String) : PassListAction
    data object ImportPass : PassListAction
    data object FindPassFiles : PassListAction
    data object OpenSettings : PassListAction
    data object OpenHelp : PassListAction
}

sealed interface PassDetailAction {
    data object Back : PassDetailAction
    data object Edit : PassDetailAction
    data object Delete : PassDetailAction
    data object Export : PassDetailAction
    data object Share : PassDetailAction
    data object Print : PassDetailAction
    data object AddToCalendar : PassDetailAction
    data class OpenLocation(val index: Int) : PassDetailAction
}

sealed interface EditPassAction {
    data object Back : EditPassAction
    data class Save(val draft: PassDraft) : EditPassAction
}

sealed interface PassFinderAction {
    data object Back : PassFinderAction
    data class Import(val uris: List<Uri>) : PassFinderAction
}

sealed interface SettingsAction {
    data object Back : SettingsAction
    data class SetTheme(val value: ThemeMode) : SettingsAction
    data class SetCondensedPasses(val value: Boolean) : SettingsAction
    data class SetAutomaticBrightness(val value: Boolean) : SettingsAction
    data class SetSortOrder(val value: PassSortOrder) : SettingsAction
}
