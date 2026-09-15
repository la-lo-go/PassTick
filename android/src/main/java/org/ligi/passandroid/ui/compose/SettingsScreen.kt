package org.ligi.passandroid.ui.compose

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ColorStyle
import org.ligi.passandroid.repository.DEFAULT_ACCENT_COLOR
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.ui.state.SettingsAction
import org.ligi.passandroid.ui.theme.PassIcons
import org.ligi.passandroid.ui.theme.accentSeedColors
import org.ligi.passandroid.ui.theme.brandAccentColor
import org.ligi.passandroid.ui.theme.generateAccentColorScheme
import androidx.compose.ui.res.stringResource
import org.ligi.passandroid.R

@Composable
internal fun ReminderChoice(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    ListItem(
        trailingContent = { RadioButton(selected, enabled = enabled, onClick = onClick) },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(
            label,
            color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    scrollToNotifications: Boolean = false,
    onNotificationScrollConsumed: () -> Unit = {},
    onAction: (SettingsAction) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToNotifications) {
        if (scrollToNotifications) {
            listState.animateScrollToItem(RemindersSectionIndex)
            onNotificationScrollConsumed()
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_settings)) }, navigationIcon = { IconButton(onClick = { onAction(SettingsAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back)) } }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxHeight().widthIn(max = 760.dp).align(Alignment.TopCenter).testTag("settings_list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item { AppearanceSettings(settings, onAction) }
                item { HomeSettings(settings, onAction) }
                item { PassListSettings(onAction) }
                item { PrivacySettings(settings, onAction) }
                item { CalendarSettings(settings, onAction) }
                item { ReminderSettings(settings, onAction) }
                item { AboutSettings(onAction) }
            }
        }
    }
}

// Must match the order of the settings sections above.
private const val RemindersSectionIndex = 5

@Composable
private fun AppearanceSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    SettingsGroup(
        title = stringResource(R.string.settings_theme),
        entries = buildList {
            ThemeMode.entries.forEach { mode ->
                add(settingsItem { ThemeSetting(mode, settings.themeMode, onAction) })
            }
            if (dynamicAvailable) {
                add(
                    settingsItem {
                        SettingSwitch(stringResource(R.string.settings_dynamic_color), settings.dynamicColors) {
                            onAction(SettingsAction.SetDynamicColors(it))
                        }
                    },
                )
            }
            if (!dynamicAvailable || !settings.dynamicColors) {
                add(settingsHeader(stringResource(R.string.settings_accent_color)))
                add(settingsItem { AccentColorSelector(settings.accentColor, onAction) })
                add(settingsHeader(stringResource(R.string.settings_color_style)))
                add(settingsItem { ColorStyleSelector(settings.accentColor, settings.colorStyle, onAction) })
            }
            if (settings.themeMode == ThemeMode.DARK) {
                add(
                    settingsItem {
                        SettingSwitch(stringResource(R.string.settings_use_amoled_black_background), settings.amoledBlackBackground) {
                            onAction(SettingsAction.SetAmoledBlackBackground(it))
                        }
                    },
                )
            }
            add(
                settingsItem {
                    SettingSwitch(stringResource(R.string.settings_use_hdr_and_maximum_code_brightness), settings.automaticBrightness) {
                        onAction(SettingsAction.SetAutomaticBrightness(it))
                    }
                },
            )
        },
    )
}

@Composable
private fun ThemeSetting(mode: ThemeMode, selectedMode: ThemeMode, onAction: (SettingsAction) -> Unit) {
    ListItem(
        trailingContent = { RadioButton(mode == selectedMode, { onAction(SettingsAction.SetTheme(mode)) }) },
        modifier = Modifier.clickable { onAction(SettingsAction.SetTheme(mode)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }
}

@Composable
private fun AccentColorSelector(selectedColor: Long?, onAction: (SettingsAction) -> Unit) {
    var customColorOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val seeds = remember { listOf<Long?>(null) + accentSeedColors }
    val customColorSelected = selectedColor != null && selectedColor !in accentSeedColors
    LaunchedEffect(selectedColor) {
        val index = seeds.indexOf(selectedColor)
        if (index >= 0) listState.scrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().testTag("accent_color_row"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        items(seeds, key = { it ?: Long.MIN_VALUE }) { seed ->
            AccentColorSwatch(seed, selected = seed == selectedColor) {
                onAction(SettingsAction.SetAccentColor(seed))
            }
        }
        item {
            CustomColorSwatch(selected = customColorSelected) { customColorOpen = true }
        }
    }
    if (customColorOpen) {
        ColorPickerDialog(
            title = stringResource(R.string.settings_accent_color),
            initialColor = (selectedColor ?: DEFAULT_ACCENT_COLOR).toInt(),
            onDismiss = { customColorOpen = false },
            onColorSelected = { color ->
                onAction(SettingsAction.SetAccentColor(color.toLong() and 0xFFFFFFFFL))
                customColorOpen = false
            },
        )
    }
}

@Composable
private fun AccentColorSwatch(seed: Long?, selected: Boolean, onClick: () -> Unit) {
    val description = seed?.let { formatPickerColor(it.toInt()) } ?: stringResource(R.string.settings_accent_default)
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(48.dp)
            .clip(shape)
            .background(seed?.let(::Color) ?: brandAccentColor)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun CustomColorSwatch(selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.settings_accent_custom)
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(48.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Edit, null)
    }
}

@Composable
private fun ColorStyleSelector(accentColor: Long?, selectedStyle: ColorStyle, onAction: (SettingsAction) -> Unit) {
    val listState = rememberLazyListState()
    val dark = isSystemInDarkTheme()
    val seed = Color(accentColor ?: DEFAULT_ACCENT_COLOR)
    LaunchedEffect(selectedStyle) {
        val index = ColorStyle.entries.indexOf(selectedStyle)
        if (index >= 0) listState.scrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().testTag("color_style_row"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        items(ColorStyle.entries) { style ->
            val scheme = remember(seed, dark, style) { generateAccentColorScheme(seed, dark, style) }
            ColorStyleSwatch(style, scheme, selected = style == selectedStyle) {
                onAction(SettingsAction.SetColorStyle(style))
            }
        }
    }
}

@Composable
private fun ColorStyleSwatch(
    style: ColorStyle,
    scheme: ColorScheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val description = colorStyleLabel(style)
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(48.dp)
            .clip(shape)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.fillMaxSize().padding(4.dp).clip(shape).background(scheme.primary))
        Box(Modifier.align(Alignment.BottomEnd).fillMaxSize(0.5f).background(scheme.secondaryContainer))
    }
}

@Composable
private fun colorStyleLabel(style: ColorStyle): String = stringResource(
    when (style) {
        ColorStyle.TONAL_SPOT -> R.string.settings_color_style_tonal_spot
        ColorStyle.VIBRANT -> R.string.settings_color_style_vibrant
        ColorStyle.EXPRESSIVE -> R.string.settings_color_style_expressive
        ColorStyle.RAINBOW -> R.string.settings_color_style_rainbow
        ColorStyle.FRUIT_SALAD -> R.string.settings_color_style_fruit_salad
        ColorStyle.CONTENT -> R.string.settings_color_style_content
        ColorStyle.FIDELITY -> R.string.settings_color_style_fidelity
        ColorStyle.NEUTRAL -> R.string.settings_color_style_neutral
        ColorStyle.MONOCHROME -> R.string.settings_color_style_monochrome
    },
)

@Composable
private fun HomeSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup(
        title = stringResource(R.string.settings_home),
        entries = listOf(
            settingsItem {
                SettingSwitch(
                    stringResource(R.string.settings_highlight_todays_passes),
                    settings.highlightTodayPasses,
                    supportingText = stringResource(R.string.settings_show_todays_passes_before_other_passes),
                ) { onAction(SettingsAction.SetHighlightTodayPasses(it)) }
            },
            settingsItem {
                SettingSwitch(
                    stringResource(R.string.settings_keep_deleted_passes_in_trash),
                    settings.trashEnabled,
                    supportingText = stringResource(R.string.settings_deleted_passes_wait_in_trash),
                ) { onAction(SettingsAction.SetTrashEnabled(it)) }
            },
        ),
    )
}

@Composable
private fun PassListSettings(onAction: (SettingsAction) -> Unit) {
    SettingsGroup(
        title = stringResource(R.string.settings_customize_pass_list),
        entries = listOf(
            settingsItem {
                PassListSetting(Icons.Default.ViewAgenda, "Home cards") {
                    onAction(SettingsAction.OpenHomeCardSettings)
                }
            },
            settingsItem {
                PassListSetting(Icons.Default.Visibility, "Pass view") {
                    onAction(SettingsAction.OpenPassViewSettings)
                }
            },
            settingsItem {
                PassListSetting(Icons.AutoMirrored.Filled.Label, "Tags") {
                    onAction(SettingsAction.OpenCategories)
                }
            },
        ),
    )
}

@Composable
private fun PassListSetting(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) { Text(title) }
}

@Composable
private fun PrivacySettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup(
        title = stringResource(R.string.settings_privacy),
        entries = listOf(
            settingsItem {
                SettingSwitch(
                    stringResource(R.string.settings_protect_the_app),
                    settings.lockAllPasses,
                    supportingText = stringResource(R.string.settings_require_fingerprint_or_screen_lock_to_open_the_a),
                ) { onAction(SettingsAction.SetLockAllPasses(it)) }
            },
            settingsItem {
                SettingSwitch(stringResource(R.string.settings_show_a_lock_icon_on_protected_passes), settings.showProtectedPassLockIcon) {
                    onAction(SettingsAction.SetShowProtectedPassLockIcon(it))
                }
            },
            settingsItem {
                SettingSwitch(stringResource(R.string.settings_blur_protected_pass_information), settings.blurProtectedPassCards) {
                    onAction(SettingsAction.SetBlurProtectedPassCards(it))
                }
            },
            settingsItem {
                SettingSwitch(stringResource(R.string.settings_keep_protected_passes_in_a_locked_section), settings.separateProtectedPasses) {
                    onAction(SettingsAction.SetSeparateProtectedPasses(it))
                }
            },
            settingsItem {
                SettingSwitch(
                    stringResource(R.string.settings_block_screenshots),
                    settings.blockScreenshots,
                    supportingText = stringResource(R.string.settings_prevent_screenshots_on_protected_content),
                ) { onAction(SettingsAction.SetBlockScreenshots(it)) }
            },
        ),
    )
}

@Composable
private fun CalendarSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup(
        title = stringResource(R.string.edit_pass_calendar),
        entries = listOf(
            settingsItem {
                SettingSwitch(stringResource(R.string.settings_automatically_add_imported_passes), settings.offerCalendarAfterImport) {
                    onAction(SettingsAction.SetOfferCalendarAfterImport(it))
                }
            },
        ),
    )
}

@Composable
private fun AboutSettings(onAction: (SettingsAction) -> Unit) {
    var showBugReportDialog by remember { mutableStateOf(false) }
    SettingsGroup(
        title = stringResource(R.string.settings_about),
        entries = listOf(
            settingsItem {
                PassListSetting(Icons.Default.PrivacyTip, "Privacy policy") {
                    onAction(SettingsAction.OpenPrivacyPolicy)
                }
            },
            settingsItem {
                PassListSetting(PassIcons.GitHub, "Source code and license") {
                    onAction(SettingsAction.OpenSourceCode)
                }
            },
            settingsItem {
                PassListSetting(Icons.Default.BugReport, stringResource(R.string.settings_report_a_bug)) {
                    showBugReportDialog = true
                }
            },
        ),
    )
    if (showBugReportDialog) {
        BugReportDialog(
            onDismiss = { showBugReportDialog = false },
            onGitHub = {
                onAction(SettingsAction.OpenBugReportGitHub)
                showBugReportDialog = false
            },
            onEmail = {
                onAction(SettingsAction.OpenBugReportEmail)
                showBugReportDialog = false
            },
        )
    }
}

@Composable
private fun BugReportDialog(
    onDismiss: () -> Unit,
    onGitHub: () -> Unit,
    onEmail: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(Icons.Default.BugReport, null, Modifier.padding(10.dp))
                    }
                    Text(
                        stringResource(R.string.settings_report_a_bug),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    stringResource(R.string.settings_bug_report_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BugReportOption(
                    icon = PassIcons.GitHub,
                    title = stringResource(R.string.settings_bug_report_github),
                    onClick = onGitHub,
                )
                BugReportOption(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.settings_bug_report_email),
                    onClick = onEmail,
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.pass_detail_cancel))
                }
            }
        }
    }
}

@Composable
private fun BugReportOption(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ReminderSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup(
        title = stringResource(R.string.settings_reminders),
        entries = buildList {
            add(
                settingsItem {
                    SettingSwitch(stringResource(R.string.settings_pass_reminders), settings.remindersEnabled) {
                        onAction(SettingsAction.SetRemindersEnabled(it))
                    }
                },
            )
            add(settingsHeader(stringResource(R.string.settings_reminder_times)))
            reminderOptions.forEach { (minutes, label) ->
                add(settingsItem { ReminderSetting(minutes, stringResource(label), settings, onAction) })
            }
            add(settingsHeader(stringResource(R.string.settings_event_access)))
            listOf(15 to R.string.settings_minutes_15, 30 to R.string.settings_minutes_30, 60 to R.string.settings_hour_1).forEach { (minutes, label) ->
                add(
                    settingsItem {
                        ReminderChoice(
                            stringResource(label),
                            settings.notificationAccessWindowMinutes == minutes,
                            enabled = settings.remindersEnabled,
                        ) {
                            onAction(SettingsAction.SetNotificationAccessWindow(minutes))
                        }
                    },
                )
            }
            add(
                settingsItem {
                    SettingSwitch(
                        stringResource(R.string.settings_exact_reminders),
                        settings.notificationExactTiming,
                        supportingText = stringResource(R.string.settings_use_exact_alarms_when_android_allows_them),
                    ) { onAction(SettingsAction.SetNotificationExactTiming(it)) }
                },
            )
            add(
                settingsItem {
                    SettingSwitch(stringResource(R.string.settings_notification_actions), settings.notificationActionsEnabled) {
                        onAction(SettingsAction.SetNotificationActionsEnabled(it))
                    }
                },
            )
        },
    )
}

private val reminderOptions = listOf(
    15 to R.string.settings_reminder_before_15_minutes,
    30 to R.string.settings_reminder_before_30_minutes,
    60 to R.string.settings_reminder_before_1_hour,
    1440 to R.string.settings_reminder_before_1_day,
)

@Composable
private fun ReminderSetting(
    minutes: Int,
    label: String,
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
) {
    val toggle = { onAction(SettingsAction.SetReminderMinutes(settings.reminderMinutes.toggle(minutes))) }
    ListItem(
        trailingContent = {
            Checkbox(
                checked = minutes in settings.reminderMinutes,
                enabled = settings.remindersEnabled,
                onCheckedChange = { toggle() },
            )
        },
        modifier = Modifier.clickable(enabled = settings.remindersEnabled, onClick = toggle),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(
            label,
            color = if (settings.remindersEnabled) Color.Unspecified
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

private fun Set<Int>.toggle(value: Int) = toMutableSet().apply {
    if (!add(value)) remove(value)
}

private sealed interface SettingsEntry {
    data class Header(val text: String) : SettingsEntry
    data class Item(val content: @Composable () -> Unit) : SettingsEntry
}

private fun settingsItem(content: @Composable () -> Unit) = SettingsEntry.Item(content)

private fun settingsHeader(text: String) = SettingsEntry.Header(text)

@Composable
private fun SettingsGroup(title: String, modifier: Modifier = Modifier, entries: List<SettingsEntry>) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEachIndexed { index, entry ->
                when (entry) {
                    is SettingsEntry.Header -> Text(
                        entry.text,
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    is SettingsEntry.Item -> {
                        val roundTop = index == 0 || entries[index - 1] is SettingsEntry.Header
                        val roundBottom = index == entries.lastIndex || entries[index + 1] is SettingsEntry.Header
                        val shape = RoundedCornerShape(
                            topStart = if (roundTop) 28.dp else 0.dp,
                            topEnd = if (roundTop) 28.dp else 0.dp,
                            bottomStart = if (roundBottom) 28.dp else 0.dp,
                            bottomEnd = if (roundBottom) 28.dp else 0.dp,
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(shape),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) { entry.content() }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    supportingText: String? = null,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        supportingContent = supportingText?.let { text -> { Text(text) } },
        trailingContent = { Switch(checked, onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(label)
    }
}
