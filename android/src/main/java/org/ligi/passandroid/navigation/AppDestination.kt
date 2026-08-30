package org.ligi.passandroid.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppDestination : NavKey {
    @Serializable data object PassList : AppDestination
    @Serializable data class PassDetail(val passId: String) : AppDestination
    @Serializable data class EditPass(val passId: String) : AppDestination
    @Serializable data object CreatePass : AppDestination
    @Serializable data object Scanner : AppDestination
    @Serializable data object Settings : AppDestination
    @Serializable data object CategorySettings : AppDestination
    @Serializable data object Help : AppDestination
}
