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
import org.ligi.passandroid.reminder.NotificationPolicySettings
import org.ligi.passandroid.reminder.NotificationLockScreenDetail
import org.ligi.passandroid.reminder.NotificationAction

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class PassCategoryRole { INBOX, FAVORITES, ARCHIVE, PAST, TRASH, CUSTOM }

enum class PassDetailSection {
    ARTWORK,
    BARCODE,
    FIELDS,
    LOCATIONS,
    CALENDAR,
    NOTES,
}

enum class HomeCardSection {
    ARTWORK,
    TITLE,
    PRIMARY_FIELD,
    DATE,
    CREATOR,
    PASS_TYPE,
    CATEGORY,
}

val defaultHomeCardSectionOrder = HomeCardSection.entries
val defaultHiddenHomeCardSections = setOf(HomeCardSection.CREATOR, HomeCardSection.PASS_TYPE)

fun normalizeHomeCardSectionOrder(sections: List<HomeCardSection>): List<HomeCardSection> =
    listOf(HomeCardSection.ARTWORK) +
        (sections + defaultHomeCardSectionOrder).distinct().filterNot { it == HomeCardSection.ARTWORK }

val defaultPassDetailSectionOrder = PassDetailSection.entries

fun normalizePassDetailSectionOrder(sections: List<PassDetailSection>): List<PassDetailSection> =
    (sections.distinct() + defaultPassDetailSectionOrder).distinct()

private fun <T> List<T>.move(item: T, offset: Int): List<T> {
    val from = indexOf(item)
    if (from < 0 || size < 2) return this
    val to = (from + offset).coerceIn(indices)
    if (from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

private fun <T> Set<T>.withVisibility(item: T, visible: Boolean): Set<T> =
    toMutableSet().apply { if (visible) remove(item) else add(item) }

data class PassCategory(
    val id: String,
    val name: String,
    val colorArgb: Long,
    val role: PassCategoryRole = PassCategoryRole.CUSTOM,
    val icon: String = "label",
)

fun PassCategory.isUserOrganized(): Boolean = when (role) {
    PassCategoryRole.INBOX,
    PassCategoryRole.TRASH,
    -> false
    PassCategoryRole.FAVORITES,
    PassCategoryRole.ARCHIVE,
    PassCategoryRole.PAST,
    PassCategoryRole.CUSTOM,
    -> true
}

private val builtInPassCategories = listOf(
    PassCategory("new", "Inbox", 0xFF3F51B5, PassCategoryRole.INBOX),
    PassCategory("favorites", "Pinned", 0xFFC2185B, PassCategoryRole.FAVORITES),
    PassCategory("archive", "Archive", 0xFF546E7A, PassCategoryRole.ARCHIVE),
    PassCategory("past", "Past", 0xFF6D4C41, PassCategoryRole.PAST),
    PassCategory("trash", "Trash", 0xFFC62828, PassCategoryRole.TRASH),
)

val recommendedPassTags = listOf(
    PassCategory("travel", "Travel", 0xFF1565C0, icon = "flight"),
    PassCategory("events", "Events", 0xFF7B1FA2, icon = "event"),
    PassCategory("loyalty", "Loyalty", 0xFFF57C00, icon = "star"),
    PassCategory("work", "Work", 0xFF00796B, icon = "work"),
    PassCategory("food", "Food and drink", 0xFFD84315, icon = "food"),
    PassCategory("shopping", "Shopping", 0xFFAD1457, icon = "shopping"),
    PassCategory("sports", "Sports", 0xFF2E7D32, icon = "sports"),
    PassCategory("entertainment", "Entertainment", 0xFF6A1B9A, icon = "movie"),
    PassCategory("music", "Music", 0xFF4527A0, icon = "music"),
    PassCategory("health", "Health", 0xFF00838F, icon = "health"),
    PassCategory("education", "Education", 0xFF283593, icon = "school"),
    PassCategory("transport", "Transport", 0xFF37474F, icon = "train"),
)

val defaultPassCategories = builtInPassCategories + recommendedPassTags

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
    val reminderExactPassIds: Set<String> = emptySet(),
    val reminderActionsByPass: Map<String, Set<NotificationAction>> = emptyMap(),
    val notificationAccessWindowMinutes: Int = 15,
    val notificationExactTiming: Boolean = false,
    val notificationActionsEnabled: Boolean = true,
    val notificationLockScreenDetail: NotificationLockScreenDetail = NotificationLockScreenDetail.HIDE_SENSITIVE,
    val passDetailSectionOrder: List<PassDetailSection> = defaultPassDetailSectionOrder,
    val hiddenPassDetailSections: Set<PassDetailSection> = emptySet(),
    val homeCardSectionOrder: List<HomeCardSection> = defaultHomeCardSectionOrder,
    val hiddenHomeCardSections: Set<HomeCardSection> = defaultHiddenHomeCardSections,
    val lockAllPasses: Boolean = false,
    val showProtectedPassLockIcon: Boolean = true,
    val blurProtectedPassCards: Boolean = false,
    val separateProtectedPasses: Boolean = false,
    val blockScreenshots: Boolean = false,
    val trashEnabled: Boolean = true,
    val imageExportOptions: PassImageExportOptions = PassImageExportOptions(),
) {
    val notificationPolicySettings: NotificationPolicySettings get() = NotificationPolicySettings(
        accessWindowMinutes = notificationAccessWindowMinutes,
        exactTiming = notificationExactTiming,
        actionsEnabled = notificationActionsEnabled,
        lockScreenDetail = notificationLockScreenDetail,
        lockAllPasses = lockAllPasses,
    )
}

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
    suspend fun setReminderExactPassIds(value: Set<String>)
    suspend fun setReminderActionsByPass(value: Map<String, Set<NotificationAction>>)
    suspend fun setNotificationAccessWindowMinutes(value: Int)
    suspend fun setNotificationExactTiming(value: Boolean)
    suspend fun setNotificationActionsEnabled(value: Boolean)
    suspend fun setNotificationLockScreenDetail(value: NotificationLockScreenDetail)
    suspend fun setPassDetailLayout(order: List<PassDetailSection>, hidden: Set<PassDetailSection>)
    suspend fun setHomeCardLayout(order: List<HomeCardSection>, hidden: Set<HomeCardSection>)
    suspend fun movePassDetailSection(section: PassDetailSection, offset: Int)
    suspend fun setPassDetailSectionVisible(section: PassDetailSection, visible: Boolean)
    suspend fun moveHomeCardSection(section: HomeCardSection, offset: Int)
    suspend fun setHomeCardSectionVisible(section: HomeCardSection, visible: Boolean)
    suspend fun setLockAllPasses(value: Boolean)
    suspend fun setShowProtectedPassLockIcon(value: Boolean)
    suspend fun setBlurProtectedPassCards(value: Boolean)
    suspend fun setSeparateProtectedPasses(value: Boolean)
    suspend fun setBlockScreenshots(value: Boolean)
    suspend fun setTrashEnabled(value: Boolean)
    suspend fun setImageExportOptions(value: PassImageExportOptions)
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
            categories = categoriesFrom(preferences[CATEGORIES], preferences[DEFAULT_TAGS_INITIALIZED] == true),
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
            reminderExactPassIds = preferences[REMINDER_EXACT_PASS_IDS].orEmpty(),
            reminderActionsByPass = preferences[REMINDER_ACTIONS_BY_PASS]?.let(::decodeReminderActions).orEmpty(),
            notificationAccessWindowMinutes = (preferences[NOTIFICATION_ACCESS_WINDOW] ?: 15).coerceIn(0, 120),
            notificationExactTiming = preferences[NOTIFICATION_EXACT_TIMING] ?: false,
            notificationActionsEnabled = preferences[NOTIFICATION_ACTIONS] ?: true,
            notificationLockScreenDetail = preferences[NOTIFICATION_LOCK_SCREEN]?.let {
                runCatching { NotificationLockScreenDetail.valueOf(it) }.getOrNull()
            } ?: NotificationLockScreenDetail.HIDE_SENSITIVE,
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
            lockAllPasses = preferences[LOCK_ALL_PASSES] ?: false,
            showProtectedPassLockIcon = preferences[SHOW_PROTECTED_PASS_LOCK_ICON] ?: true,
            blurProtectedPassCards = preferences[BLUR_PROTECTED_PASS_CARDS] ?: false,
            separateProtectedPasses = preferences[SEPARATE_PROTECTED_PASSES] ?: false,
            blockScreenshots = preferences[BLOCK_SCREENSHOTS] ?: false,
            trashEnabled = preferences[TRASH_ENABLED] ?: true,
            imageExportOptions = PassImageExportOptions(
                aspectRatio = preferences[IMAGE_EXPORT_ASPECT_RATIO]?.let {
                    runCatching { PassImageAspectRatio.valueOf(it) }.getOrNull()
                } ?: PassImageAspectRatio.AUTO_HEIGHT,
                orientation = preferences[IMAGE_EXPORT_ORIENTATION]?.let {
                    runCatching { PassImageOrientation.valueOf(it) }.getOrNull()
                } ?: PassImageOrientation.PORTRAIT,
                content = decodePassImageContent(preferences[IMAGE_EXPORT_CONTENT]),
            ),
        )
    }

    override suspend fun setThemeMode(value: ThemeMode) {
        StartupAppearanceStore.write(context, value, StartupAppearanceStore.read(context).amoledBlackBackground)
        update(THEME, value.name)
    }

    override suspend fun setAmoledBlackBackground(value: Boolean) {
        StartupAppearanceStore.write(context, StartupAppearanceStore.read(context).themeMode, value)
        update(AMOLED_BLACK_BACKGROUND, value)
    }
    override suspend fun setAutomaticBrightness(value: Boolean) = update(AUTOMATIC_BRIGHTNESS, value)
    override suspend fun setSortOrder(value: PassSortOrder) = update(SORT, value.name)
    override suspend fun setPassOrder(value: List<String>) = update(PASS_ORDER, encodePassOrder(value))
    override suspend fun setCategories(value: List<PassCategory>) {
        context.settingsDataStore.edit {
            it[CATEGORIES] = encodeCategories(normalizeCategories(value))
            it[DEFAULT_TAGS_INITIALIZED] = true
        }
    }
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
    override suspend fun setReminderExactPassIds(value: Set<String>) = update(REMINDER_EXACT_PASS_IDS, value)
    override suspend fun setReminderActionsByPass(value: Map<String, Set<NotificationAction>>) =
        update(REMINDER_ACTIONS_BY_PASS, encodeReminderActions(value))
    override suspend fun setNotificationAccessWindowMinutes(value: Int) =
        update(NOTIFICATION_ACCESS_WINDOW, value.coerceIn(0, 120))
    override suspend fun setNotificationExactTiming(value: Boolean) = update(NOTIFICATION_EXACT_TIMING, value)
    override suspend fun setNotificationActionsEnabled(value: Boolean) = update(NOTIFICATION_ACTIONS, value)
    override suspend fun setNotificationLockScreenDetail(value: NotificationLockScreenDetail) =
        update(NOTIFICATION_LOCK_SCREEN, value.name)
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

    override suspend fun movePassDetailSection(section: PassDetailSection, offset: Int) {
        context.settingsDataStore.edit { preferences ->
            val order = preferences[PASS_DETAIL_SECTION_ORDER]
                ?.let(::decodePassDetailSectionOrder)
                ?: defaultPassDetailSectionOrder
            preferences[PASS_DETAIL_SECTION_ORDER] = encodePassDetailSectionOrder(order.move(section, offset))
        }
    }

    override suspend fun setPassDetailSectionVisible(section: PassDetailSection, visible: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[HIDDEN_PASS_DETAIL_SECTIONS] = preferences[HIDDEN_PASS_DETAIL_SECTIONS]
                .orEmpty()
                .withVisibility(section.name, visible)
        }
    }

    override suspend fun moveHomeCardSection(section: HomeCardSection, offset: Int) {
        context.settingsDataStore.edit { preferences ->
            val order = preferences[HOME_CARD_SECTION_ORDER]
                ?.let(::decodeHomeCardSectionOrder)
                ?: defaultHomeCardSectionOrder
            preferences[HOME_CARD_SECTION_ORDER] = encodeHomeCardSectionOrder(order.move(section, offset))
        }
    }

    override suspend fun setHomeCardSectionVisible(section: HomeCardSection, visible: Boolean) {
        context.settingsDataStore.edit { preferences ->
            val hidden = preferences[HIDDEN_HOME_CARD_SECTIONS] ?: defaultHiddenHomeCardSections.mapTo(mutableSetOf()) { it.name }
            preferences[HIDDEN_HOME_CARD_SECTIONS] = hidden.withVisibility(section.name, visible)
        }
    }

    override suspend fun setLockAllPasses(value: Boolean) = update(LOCK_ALL_PASSES, value)
    override suspend fun setShowProtectedPassLockIcon(value: Boolean) = update(SHOW_PROTECTED_PASS_LOCK_ICON, value)
    override suspend fun setBlurProtectedPassCards(value: Boolean) = update(BLUR_PROTECTED_PASS_CARDS, value)
    override suspend fun setSeparateProtectedPasses(value: Boolean) = update(SEPARATE_PROTECTED_PASSES, value)
    override suspend fun setBlockScreenshots(value: Boolean) = update(BLOCK_SCREENSHOTS, value)
    override suspend fun setTrashEnabled(value: Boolean) = update(TRASH_ENABLED, value)

    override suspend fun setImageExportOptions(value: PassImageExportOptions) {
        context.settingsDataStore.edit {
            it[IMAGE_EXPORT_ASPECT_RATIO] = value.aspectRatio.name
            it[IMAGE_EXPORT_ORIENTATION] = value.orientation.name
            it[IMAGE_EXPORT_CONTENT] = encodePassImageContent(value.content)
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
        val DEFAULT_TAGS_INITIALIZED = booleanPreferencesKey("default_tags_initialized")
        val HIGHLIGHT_TODAY = booleanPreferencesKey("highlight_today_passes")
        val AUTO_MARK_PAST = booleanPreferencesKey("automatically_mark_past")
        val OFFER_CALENDAR = booleanPreferencesKey("offer_calendar_after_import")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val REMINDER_MINUTES_SET = stringSetPreferencesKey("reminder_minutes")
        val REMINDER_EXCLUDED_PASS_IDS = stringSetPreferencesKey("reminder_excluded_pass_ids")
        val REMINDER_LEAD_BY_PASS = stringPreferencesKey("reminder_lead_minutes_by_pass")
        val REMINDER_EXACT_PASS_IDS = stringSetPreferencesKey("reminder_exact_pass_ids")
        val REMINDER_ACTIONS_BY_PASS = stringPreferencesKey("reminder_actions_by_pass")
        val NOTIFICATION_ACCESS_WINDOW = intPreferencesKey("notification_access_window_minutes")
        val NOTIFICATION_EXACT_TIMING = booleanPreferencesKey("notification_exact_timing")
        val NOTIFICATION_ACTIONS = booleanPreferencesKey("notification_actions_enabled")
        val NOTIFICATION_LOCK_SCREEN = stringPreferencesKey("notification_lock_screen_detail")
        val PASS_DETAIL_SECTION_ORDER = stringPreferencesKey("pass_detail_section_order")
        val HIDDEN_PASS_DETAIL_SECTIONS = stringSetPreferencesKey("hidden_pass_detail_sections")
        val HOME_CARD_SECTION_ORDER = stringPreferencesKey("home_card_section_order")
        val HIDDEN_HOME_CARD_SECTIONS = stringSetPreferencesKey("hidden_home_card_sections")
        val LOCK_ALL_PASSES = booleanPreferencesKey("lock_all_passes")
        val SHOW_PROTECTED_PASS_LOCK_ICON = booleanPreferencesKey("show_protected_pass_lock_icon")
        val BLUR_PROTECTED_PASS_CARDS = booleanPreferencesKey("blur_protected_pass_cards")
        val SEPARATE_PROTECTED_PASSES = booleanPreferencesKey("separate_protected_passes")
        val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
        val TRASH_ENABLED = booleanPreferencesKey("trash_enabled")
        val IMAGE_EXPORT_ASPECT_RATIO = stringPreferencesKey("image_export_aspect_ratio")
        val IMAGE_EXPORT_ORIENTATION = stringPreferencesKey("image_export_orientation")
        val IMAGE_EXPORT_CONTENT = stringSetPreferencesKey("image_export_content")
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

private fun encodeReminderActions(values: Map<String, Set<NotificationAction>>) = JSONObject().apply {
    values.filterKeys(String::isNotBlank).forEach { (passId, actions) ->
        put(passId, JSONArray(actions.map(NotificationAction::name)))
    }
}.toString()

private fun decodeReminderActions(value: String): Map<String, Set<NotificationAction>> = runCatching {
    val json = JSONObject(value)
    buildMap {
        json.keys().forEach { passId ->
            val actions = json.getJSONArray(passId)
            put(passId, buildSet {
                repeat(actions.length()) { index ->
                    runCatching { NotificationAction.valueOf(actions.getString(index)) }.getOrNull()?.let(::add)
                }
            })
        }
    }
}.getOrDefault(emptyMap())

private fun encodePassImageContent(value: PassImageContent): Set<String> = buildSet {
    if (value.artwork) add("artwork")
    if (value.details) add("details")
    if (value.barcode) add("barcode")
    if (value.dateTime) add("dateTime")
    if (value.location) add("location")
    if (value.hiddenFields) add("hiddenFields")
}

private fun decodePassImageContent(values: Set<String>?): PassImageContent =
    if (values == null) {
        PassImageContent()
    } else {
        PassImageContent(
            artwork = "artwork" in values,
            details = "details" in values,
            barcode = "barcode" in values,
            dateTime = "dateTime" in values,
            location = "location" in values,
            hiddenFields = "hiddenFields" in values,
        )
    }

private fun encodeCategories(categories: List<PassCategory>) = JSONArray().apply {
    categories.forEach { category ->
        put(
            JSONObject()
                .put("id", category.id)
                .put("name", category.name)
                .put("colorArgb", category.colorArgb)
                .put("role", category.role.name)
                .put("icon", category.icon),
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
                    icon = item.optString("icon").ifBlank { "label" },
                ),
            )
        }
    }
}.getOrNull()

internal fun categoriesFrom(encoded: String?, defaultsInitialized: Boolean): List<PassCategory> {
    val stored = encoded?.let(::decodeCategories)?.let(::normalizeCategories) ?: return defaultPassCategories
    return if (defaultsInitialized || stored.any { it.role == PassCategoryRole.CUSTOM }) {
        stored
    } else {
        normalizeCategories(stored + recommendedPassTags)
    }
}

private fun normalizeCategories(categories: List<PassCategory>): List<PassCategory> {
    val systemById = builtInPassCategories.associateBy(PassCategory::id)
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
    builtInPassCategories.filterNot { it.role in roles }.forEach(normalized::add)
    return normalized
}
