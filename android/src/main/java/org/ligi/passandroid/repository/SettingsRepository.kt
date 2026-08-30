package org.ligi.passandroid.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class PassCategoryRole { INBOX, FAVORITES, ARCHIVE, PAST, TRASH, CUSTOM }

data class PassCategory(
    val id: String,
    val name: String,
    val colorArgb: Long,
    val role: PassCategoryRole = PassCategoryRole.CUSTOM,
)

val defaultPassCategories = listOf(
    PassCategory("new", "Inbox", 0xFF3F51B5, PassCategoryRole.INBOX),
    PassCategory("favorites", "Favorites", 0xFFC2185B, PassCategoryRole.FAVORITES),
    PassCategory("archive", "Archive", 0xFF546E7A, PassCategoryRole.ARCHIVE),
    PassCategory("past", "Past", 0xFF6D4C41, PassCategoryRole.PAST),
    PassCategory("trash", "Trash", 0xFFC62828, PassCategoryRole.TRASH),
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val condensedPasses: Boolean = false,
    val automaticBrightness: Boolean = true,
    val sortOrder: PassSortOrder = PassSortOrder.DATE_DESC,
    val categories: List<PassCategory> = defaultPassCategories,
    val highlightTodayPasses: Boolean = true,
    val automaticallyMarkPast: Boolean = false,
    val offerCalendarAfterImport: Boolean = false,
    val remindersEnabled: Boolean = false,
    val defaultReminderMinutes: Int = 60,
    val quickCodePassId: String? = null,
    val reminderExcludedPassIds: Set<String> = emptySet(),
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(value: ThemeMode)
    suspend fun setCondensedPasses(value: Boolean)
    suspend fun setAutomaticBrightness(value: Boolean)
    suspend fun setSortOrder(value: PassSortOrder)
    suspend fun setCategories(value: List<PassCategory>)
    suspend fun setHighlightTodayPasses(value: Boolean)
    suspend fun setAutomaticallyMarkPast(value: Boolean)
    suspend fun setOfferCalendarAfterImport(value: Boolean)
    suspend fun setRemindersEnabled(value: Boolean)
    suspend fun setDefaultReminderMinutes(value: Int)
    suspend fun setQuickCodePassId(value: String?)
    suspend fun setReminderExcludedPassIds(value: Set<String>)
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
            categories = preferences[CATEGORIES]?.let(::decodeCategories)?.let(::normalizeCategories)
                ?: defaultPassCategories,
            highlightTodayPasses = preferences[HIGHLIGHT_TODAY] ?: true,
            automaticallyMarkPast = preferences[AUTO_MARK_PAST] ?: false,
            offerCalendarAfterImport = preferences[OFFER_CALENDAR] ?: false,
            remindersEnabled = preferences[REMINDERS_ENABLED] ?: false,
            defaultReminderMinutes = (preferences[REMINDER_MINUTES] ?: 60).coerceIn(0, 10_080),
            quickCodePassId = preferences[QUICK_CODE_PASS_ID],
            reminderExcludedPassIds = preferences[REMINDER_EXCLUDED_PASS_IDS].orEmpty(),
        )
    }

    override suspend fun setThemeMode(value: ThemeMode) = update(THEME, value.name)
    override suspend fun setCondensedPasses(value: Boolean) = update(CONDENSED, value)
    override suspend fun setAutomaticBrightness(value: Boolean) = update(AUTOMATIC_BRIGHTNESS, value)
    override suspend fun setSortOrder(value: PassSortOrder) = update(SORT, value.name)
    override suspend fun setCategories(value: List<PassCategory>) = update(
        CATEGORIES,
        encodeCategories(normalizeCategories(value)),
    )
    override suspend fun setHighlightTodayPasses(value: Boolean) = update(HIGHLIGHT_TODAY, value)
    override suspend fun setAutomaticallyMarkPast(value: Boolean) = update(AUTO_MARK_PAST, value)
    override suspend fun setOfferCalendarAfterImport(value: Boolean) = update(OFFER_CALENDAR, value)
    override suspend fun setRemindersEnabled(value: Boolean) = update(REMINDERS_ENABLED, value)
    override suspend fun setDefaultReminderMinutes(value: Int) = update(REMINDER_MINUTES, value.coerceIn(0, 10_080))
    override suspend fun setQuickCodePassId(value: String?) {
        context.settingsDataStore.edit { preferences ->
            if (value == null) preferences.remove(QUICK_CODE_PASS_ID) else preferences[QUICK_CODE_PASS_ID] = value
        }
    }
    override suspend fun setReminderExcludedPassIds(value: Set<String>) = update(
        REMINDER_EXCLUDED_PASS_IDS,
        value,
    )

    private suspend fun <T> update(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val CONDENSED = booleanPreferencesKey("condensed_passes")
        val AUTOMATIC_BRIGHTNESS = booleanPreferencesKey("automatic_brightness")
        val SORT = stringPreferencesKey("sort_order")
        val CATEGORIES = stringPreferencesKey("categories")
        val HIGHLIGHT_TODAY = booleanPreferencesKey("highlight_today_passes")
        val AUTO_MARK_PAST = booleanPreferencesKey("automatically_mark_past")
        val OFFER_CALENDAR = booleanPreferencesKey("offer_calendar_after_import")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val QUICK_CODE_PASS_ID = stringPreferencesKey("quick_code_pass_id")
        val REMINDER_EXCLUDED_PASS_IDS = stringSetPreferencesKey("reminder_excluded_pass_ids")
    }
}

private fun encodeCategories(categories: List<PassCategory>) = JSONArray().apply {
    categories.forEach { category ->
        put(
            JSONObject()
                .put("id", category.id)
                .put("name", category.name)
                .put("colorArgb", category.colorArgb)
                .put("role", category.role.name),
        )
    }
}.toString()

private fun decodeCategories(value: String): List<PassCategory>? = runCatching {
    val array = JSONArray(value)
    buildList {
        repeat(array.length()) { index ->
            val item = array.getJSONObject(index)
            add(
                PassCategory(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    colorArgb = item.getLong("colorArgb"),
                    role = item.optString("role")
                        .takeIf(String::isNotBlank)
                        ?.let(PassCategoryRole::valueOf)
                        ?: PassCategoryRole.CUSTOM,
                ),
            )
        }
    }
}.getOrNull()

private fun normalizeCategories(categories: List<PassCategory>): List<PassCategory> {
    val systemById = defaultPassCategories.associateBy(PassCategory::id)
    val normalized = categories.mapNotNull { category ->
        val id = category.id.trim()
        val name = category.name.trim()
        if (id.isEmpty() || name.isEmpty()) null else category.copy(
            id = id,
            name = name,
            role = systemById[id]?.role ?: PassCategoryRole.CUSTOM,
        )
    }.distinctBy(PassCategory::id).toMutableList()
    val roles = normalized.mapTo(mutableSetOf(), PassCategory::role)
    defaultPassCategories.filterNot { it.role in roles }.forEach(normalized::add)
    return normalized
}
