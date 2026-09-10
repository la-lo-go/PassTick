package org.ligi.passandroid.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.ligi.passandroid.repository.PassArtworkKind

@Serializable
enum class PassDateField { START, END }

sealed interface AppDestination : NavKey {
    @Serializable data object PassList : AppDestination
    @Serializable data class PassDetail(val passId: String) : AppDestination
    @Serializable data class EditPass(val passId: String, val dateField: PassDateField? = null) : AppDestination
    @Serializable data class PassCustomization(val passId: String) : AppDestination
    @Serializable data class ExportImage(val passId: String) : AppDestination
    @Serializable data object Settings : AppDestination
    @Serializable data object CategorySettings : AppDestination
    @Serializable data object PassDetailLayoutSettings : AppDestination
    @Serializable data object HomeCardLayoutSettings : AppDestination
    @Serializable data object Timeline : AppDestination
}

internal fun passCustomizationDestination(
    passId: String,
    artworkKinds: Iterable<PassArtworkKind>,
): AppDestination = if (artworkKinds.distinct().count() > 1) {
    AppDestination.PassCustomization(passId)
} else {
    AppDestination.PassDetailLayoutSettings
}
