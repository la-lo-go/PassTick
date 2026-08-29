package org.ligi.passandroid.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ligi.passandroid.model.comparator.PassSortOrder

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val condensedPasses: Boolean = false,
    val automaticBrightness: Boolean = true,
    val sortOrder: PassSortOrder = PassSortOrder.DATE_DESC,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(value: ThemeMode)
    suspend fun setCondensedPasses(value: Boolean)
    suspend fun setAutomaticBrightness(value: Boolean)
    suspend fun setSortOrder(value: PassSortOrder)
}

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    override val settings = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            condensedPasses = preferences[CONDENSED] ?: false,
            automaticBrightness = preferences[AUTOMATIC_BRIGHTNESS] ?: true,
            sortOrder = preferences[SORT]?.let { runCatching { PassSortOrder.valueOf(it) }.getOrNull() }
                ?: PassSortOrder.DATE_DESC,
        )
    }

    override suspend fun setThemeMode(value: ThemeMode) = update(THEME, value.name)
    override suspend fun setCondensedPasses(value: Boolean) = update(CONDENSED, value)
    override suspend fun setAutomaticBrightness(value: Boolean) = update(AUTOMATIC_BRIGHTNESS, value)
    override suspend fun setSortOrder(value: PassSortOrder) = update(SORT, value.name)

    private suspend fun <T> update(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val CONDENSED = booleanPreferencesKey("condensed_passes")
        val AUTOMATIC_BRIGHTNESS = booleanPreferencesKey("automatic_brightness")
        val SORT = stringPreferencesKey("sort_order")
    }
}
