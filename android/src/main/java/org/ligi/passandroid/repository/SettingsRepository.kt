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

enum class PassDetailSection {
    ARTWORK,
    BARCODE,
    FIELDS,
    LOCATIONS,
    CALENDAR,
}

enum class HomeCardSection {
    ARTWORK,
    TITLE,
    PRIMARY_FIELD,
    DATE,
    CREATOR,
    CATEGORY,
    PASS_TYPE,
}

val defaultHomeCardSectionOrder = HomeCardSection.entries
val defaultHiddenHomeCardSections = setOf(HomeCardSection.CREATOR)

fun normalizeHomeCardSectionOrder(sections: List<HomeCardSection>): List<HomeCardSection> =
    (sections.distinct() + defaultHomeCardSectionOrder).distinct()

val defaultPassDetailSectionOrder = PassDetailSection.entries

fun normalizePassDetailSectionOrder(sections: List<PassDetailSection>): List<PassDetailSection> =
    (sections.distinct() + defaultPassDetailSectionOrder).distinct()

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
    val amoledBlackBackground: Boolean = false,
    val automaticBrightness: Boolean = true,
    val sortOrder: PassSortOrder = PassSortOrder.DATE_DESC,
    val passOrder: List<String> = emptyList(),
    val categories: List<PassCategory> = defaultPassCategories,
    val highlightTodayPasses: Boolean = true,
    val automaticallyMarkPast: Boolean = false,
    val offerCalendarAfterImport: Boolean = false,
    val remindersEnabled: Boolean = false,
    val reminderMinutes: Set<Int> = setOf(60),
    val reminderExcludedPassIds: Set<String> = emptySet(),
    val reminderLeadMinutesByPass: Map<String, Int> = emptyMap(),
    val passDetailSectionOrder: List<PassDetailSection> = defaultPassDetailSectionOrder,
    val hiddenPassDetailSections: Set<PassDetailSection> = emptySet(),
    val homeCardSectionOrder: List<HomeCardSection> = defaultHomeCardSectionOrder,
    val hiddenHomeCardSections: Set<HomeCardSection> = defaultHiddenHomeCardSections,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(value: ThemeMode)
    suspend fun setAmoledBlackBackground(value: Boolean)
    suspend fun setAutomaticBrightness(value: Boolean)
    suspend fun setSortOrder(value: PassSortOrder)
    suspend fun setPassOrder(value: List<String>)
    suspend fun setCategories(value: List<PassCategory>)
    suspend fun setHighlightTodayPasses(value: Boolean)
    suspend fun setAutomaticallyMarkPast(value: Boolean)
    suspend fun setOfferCalendarAfterImport(value: Boolean)
    suspend fun setRemindersEnabled(value: Boolean)
    suspend fun setReminderMinutes(value: Set<Int>)
    suspend fun setReminderExcludedPassIds(value: Set<String>)
    suspend fun setReminderLeadMinutesByPass(value: Map<String, Int>)
    suspend fun setPassDetailLayout(order: List<PassDetailSection>, hidden: Set<PassDetailSection>)
    suspend fun setHomeCardLayout(order: List<HomeCardSection>, hidden: Set<HomeCardSection>)
}

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    override val settings = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[THEME]?.let { value ->
                if (value == "AMOLED") ThemeMode.DARK else runCatching { ThemeMode.valueOf(value) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            amoledBlackBackground = preferences[AMOLED_BLACK_BACKGROUND] ?: (preferences[THEME] == "AMOLED"),
            automaticBrightness = preferences[AUTOMATIC_BRIGHTNESS] ?: true,
            sortOrder = preferences[SORT]?.let { runCatching { PassSortOrder.valueOf(it) }.getOrNull() }
                ?: PassSortOrder.DATE_DESC,
            passOrder = preferences[PASS_ORDER]?.let(::decodePassOrder).orEmpty(),
            categories = preferences[CATEGORIES]?.let(::decodeCategories)?.let(::normalizeCategories)
                ?: defaultPassCategories,
            highlightTodayPasses = preferences[HIGHLIGHT_TODAY] ?: true,
            automaticallyMarkPast = preferences[AUTO_MARK_PAST] ?: false,
            offerCalendarAfterImport = preferences[OFFER_CALENDAR] ?: false,
            remindersEnabled = preferences[REMINDERS_ENABLED] ?: false,
            reminderMinutes = preferences[REMINDER_MINUTES_SET]
                ?.mapNotNull(String::toIntOrNull)
                ?.mapTo(mutableSetOf()) { it.coerceIn(0, 10_080) }
                ?: setOf((preferences[REMINDER_MINUTES] ?: 60).coerceIn(0, 10_080)),
            reminderExcludedPassIds = preferences[REMINDER_EXCLUDED_PASS_IDS].orEmpty(),
            reminderLeadMinutesByPass = preferences[REMINDER_LEAD_BY_PASS]?.let(::decodeReminderLeads).orEmpty(),
            passDetailSectionOrder = preferences[PASS_DETAIL_SECTION_ORDER]
                ?.let(::decodePassDetailSectionOrder)
                ?: defaultPassDetailSectionOrder,
            hiddenPassDetailSections = preferences[HIDDEN_PASS_DETAIL_SECTIONS]
                ?.mapNotNull { value -> runCatching { PassDetailSection.valueOf(value) }.getOrNull() }
                ?.toSet()
                .orEmpty(),
            homeCardSectionOrder = preferences[HOME_CARD_SECTION_ORDER]
                ?.let(::decodeHomeCardSectionOrder)
                ?: defaultHomeCardSectionOrder,
            hiddenHomeCardSections = preferences[HIDDEN_HOME_CARD_SECTIONS]
                ?.mapNotNull { value -> runCatching { HomeCardSection.valueOf(value) }.getOrNull() }
                ?.toSet()
                ?: defaultHiddenHomeCardSections,
        )
    }

    override suspend fun setThemeMode(value: ThemeMode) = update(THEME, value.name)
    override suspend fun setAmoledBlackBackground(value: Boolean) = update(AMOLED_BLACK_BACKGROUND, value)
    override suspend fun setAutomaticBrightness(value: Boolean) = update(AUTOMATIC_BRIGHTNESS, value)
    override suspend fun setSortOrder(value: PassSortOrder) = update(SORT, value.name)
    override suspend fun setPassOrder(value: List<String>) = update(PASS_ORDER, encodePassOrder(value))
    override suspend fun setCategories(value: List<PassCategory>) = update(
        CATEGORIES,
        encodeCategories(normalizeCategories(value)),
    )
    override suspend fun setHighlightTodayPasses(value: Boolean) = update(HIGHLIGHT_TODAY, value)
    override suspend fun setAutomaticallyMarkPast(value: Boolean) = update(AUTO_MARK_PAST, value)
    override suspend fun setOfferCalendarAfterImport(value: Boolean) = update(OFFER_CALENDAR, value)
    override suspend fun setRemindersEnabled(value: Boolean) = update(REMINDERS_ENABLED, value)
    override suspend fun setReminderMinutes(value: Set<Int>) = update(
        REMINDER_MINUTES_SET,
        value.mapTo(mutableSetOf()) { it.coerceIn(0, 10_080).toString() },
    )
    override suspend fun setReminderExcludedPassIds(value: Set<String>) = update(
        REMINDER_EXCLUDED_PASS_IDS,
        value,
    )
    override suspend fun setReminderLeadMinutesByPass(value: Map<String, Int>) = update(
        REMINDER_LEAD_BY_PASS,
        encodeReminderLeads(value),
    )
    override suspend fun setPassDetailLayout(order: List<PassDetailSection>, hidden: Set<PassDetailSection>) {
        context.settingsDataStore.edit {
            it[PASS_DETAIL_SECTION_ORDER] = encodePassDetailSectionOrder(order)
            it[HIDDEN_PASS_DETAIL_SECTIONS] = hidden.mapTo(mutableSetOf()) { section -> section.name }
        }
    }

    override suspend fun setHomeCardLayout(order: List<HomeCardSection>, hidden: Set<HomeCardSection>) {
        context.settingsDataStore.edit {
            it[HOME_CARD_SECTION_ORDER] = encodeHomeCardSectionOrder(order)
            it[HIDDEN_HOME_CARD_SECTIONS] = hidden.mapTo(mutableSetOf()) { section -> section.name }
        }
    }

    private suspend fun <T> update(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val AMOLED_BLACK_BACKGROUND = booleanPreferencesKey("amoled_black_background")
        val AUTOMATIC_BRIGHTNESS = booleanPreferencesKey("automatic_brightness")
        val SORT = stringPreferencesKey("sort_order")
        val PASS_ORDER = stringPreferencesKey("pass_order")
        val CATEGORIES = stringPreferencesKey("categories")
        val HIGHLIGHT_TODAY = booleanPreferencesKey("highlight_today_passes")
        val AUTO_MARK_PAST = booleanPreferencesKey("automatically_mark_past")
        val OFFER_CALENDAR = booleanPreferencesKey("offer_calendar_after_import")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val REMINDER_MINUTES_SET = stringSetPreferencesKey("reminder_minutes")
        val REMINDER_EXCLUDED_PASS_IDS = stringSetPreferencesKey("reminder_excluded_pass_ids")
        val REMINDER_LEAD_BY_PASS = stringPreferencesKey("reminder_lead_minutes_by_pass")
        val PASS_DETAIL_SECTION_ORDER = stringPreferencesKey("pass_detail_section_order")
        val HIDDEN_PASS_DETAIL_SECTIONS = stringSetPreferencesKey("hidden_pass_detail_sections")
        val HOME_CARD_SECTION_ORDER = stringPreferencesKey("home_card_section_order")
        val HIDDEN_HOME_CARD_SECTIONS = stringSetPreferencesKey("hidden_home_card_sections")
    }
}

private fun encodePassOrder(values: List<String>) = JSONArray(values.filter(String::isNotBlank).distinct()).toString()

private fun decodePassOrder(value: String): List<String> = runCatching {
    val json = JSONArray(value)
    buildList { repeat(json.length()) { index -> add(json.getString(index)) } }
}.getOrDefault(emptyList())

private fun encodePassDetailSectionOrder(values: List<PassDetailSection>) =
    JSONArray(normalizePassDetailSectionOrder(values).map(PassDetailSection::name)).toString()

private fun decodePassDetailSectionOrder(value: String): List<PassDetailSection> = runCatching {
    val json = JSONArray(value)
    normalizePassDetailSectionOrder(
        buildList {
            repeat(json.length()) { index ->
                runCatching { PassDetailSection.valueOf(json.getString(index)) }.getOrNull()?.let(::add)
            }
        },
    )
}.getOrDefault(defaultPassDetailSectionOrder)

private fun encodeHomeCardSectionOrder(values: List<HomeCardSection>) =
    JSONArray(normalizeHomeCardSectionOrder(values).map(HomeCardSection::name)).toString()

private fun decodeHomeCardSectionOrder(value: String): List<HomeCardSection> = runCatching {
    val json = JSONArray(value)
    normalizeHomeCardSectionOrder(
        buildList {
            repeat(json.length()) { index ->
                runCatching { HomeCardSection.valueOf(json.getString(index)) }.getOrNull()?.let(::add)
            }
        },
    )
}.getOrDefault(defaultHomeCardSectionOrder)

private fun encodeReminderLeads(values: Map<String, Int>) = JSONObject().apply {
    values.forEach { (passId, minutes) ->
        if (passId.isNotBlank()) put(passId, minutes.coerceIn(0, 10_080))
    }
}.toString()

private fun decodeReminderLeads(value: String): Map<String, Int> = runCatching {
    val json = JSONObject(value)
    buildMap {
        json.keys().forEach { passId -> put(passId, json.getInt(passId).coerceIn(0, 10_080)) }
    }
}.getOrDefault(emptyMap())

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
