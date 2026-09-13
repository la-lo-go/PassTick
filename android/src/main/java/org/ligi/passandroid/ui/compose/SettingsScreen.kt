package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.reminder.NotificationLockScreenDetail
import org.ligi.passandroid.ui.state.SettingsAction

@Composable
internal fun ReminderChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        trailingContent = { RadioButton(selected, onClick) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) { Text(label) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    scrollToNotifications: Boolean = false,
    onNotificationScrollConsumed: () -> Unit = {},
    onAction: (SettingsAction) -> Unit,
) {
    val notificationRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(scrollToNotifications) {
        if (scrollToNotifications) {
            notificationRequester.bringIntoView()
            onNotificationScrollConsumed()
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = { onAction(SettingsAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight().widthIn(max = 760.dp).align(Alignment.TopCenter).testTag("settings_list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item { AppearanceSettings(settings, onAction) }
                item { HomeSettings(settings, onAction) }
                item { PassListSettings(onAction) }
                item { PrivacySettings(settings, onAction) }
                item { CalendarSettings(settings, onAction) }
                item { NotificationSettings(settings, onAction, Modifier.bringIntoViewRequester(notificationRequester)) }
                item { AboutSettings(onAction) }
            }
        }
    }
}

@Composable
private fun AppearanceSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Appearance") {
        Text("Theme", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
        ThemeMode.entries.forEach { mode -> ThemeSetting(mode, settings.themeMode, onAction) }
        if (settings.themeMode == ThemeMode.DARK) {
            SettingSwitch("Use AMOLED black background", settings.amoledBlackBackground) {
                onAction(SettingsAction.SetAmoledBlackBackground(it))
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        SettingSwitch("Use HDR and maximum code brightness", settings.automaticBrightness) {
            onAction(SettingsAction.SetAutomaticBrightness(it))
        }
    }
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
private fun HomeSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Home") {
        SettingSwitch(
            "Highlight today's passes",
            settings.highlightTodayPasses,
            supportingText = "Show today's passes before other passes",
        ) { onAction(SettingsAction.SetHighlightTodayPasses(it)) }
    }
}

@Composable
private fun PassListSettings(onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Customize pass list") {
        PassListSetting(Icons.Default.ViewAgenda, "Home cards") {
            onAction(SettingsAction.OpenHomeCardSettings)
        }
        PassListSetting(Icons.Default.Visibility, "Pass view") {
            onAction(SettingsAction.OpenPassViewSettings)
        }
        PassListSetting(Icons.AutoMirrored.Filled.Label, "Tags") {
            onAction(SettingsAction.OpenCategories)
        }
    }
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
    SettingsGroup("Privacy") {
        SettingSwitch(
            "Protect the app",
            settings.lockAllPasses,
            supportingText = "Require fingerprint or screen lock to open the app",
        ) { onAction(SettingsAction.SetLockAllPasses(it)) }
        SettingSwitch("Show a lock icon on protected passes", settings.showProtectedPassLockIcon) {
            onAction(SettingsAction.SetShowProtectedPassLockIcon(it))
        }
        SettingSwitch("Blur protected pass information", settings.blurProtectedPassCards) {
            onAction(SettingsAction.SetBlurProtectedPassCards(it))
        }
        SettingSwitch("Keep protected passes in a locked section", settings.separateProtectedPasses) {
            onAction(SettingsAction.SetSeparateProtectedPasses(it))
        }
        SettingSwitch(
            "Block screenshots",
            settings.blockScreenshots,
            supportingText = "Prevent screenshots on protected content",
        ) { onAction(SettingsAction.SetBlockScreenshots(it)) }
    }
}

@Composable
private fun CalendarSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Calendar") {
        SettingSwitch("Automatically add imported passes", settings.offerCalendarAfterImport) {
            onAction(SettingsAction.SetOfferCalendarAfterImport(it))
        }
    }
}

@Composable
private fun AboutSettings(onAction: (SettingsAction) -> Unit) {
    SettingsGroup("About") {
        PassListSetting(Icons.Default.PrivacyTip, "Privacy policy") {
            onAction(SettingsAction.OpenPrivacyPolicy)
        }
        PassListSetting(Icons.Default.Code, "Source code and license") {
            onAction(SettingsAction.OpenSourceCode)
        }
    }
}

@Composable
private fun NotificationSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit, modifier: Modifier = Modifier) {
    SettingsGroup("Notifications", modifier) {
        SettingSwitch("Pass reminders", settings.remindersEnabled) {
            onAction(SettingsAction.SetRemindersEnabled(it))
        }
        Text("Reminder times", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
        reminderOptions.forEach { (minutes, label) ->
            ReminderSetting(minutes, label, settings, onAction)
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        Text("Event access", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
        listOf(15 to "15 minutes", 30 to "30 minutes", 60 to "1 hour").forEach { (minutes, label) ->
            ReminderChoice(label, settings.notificationAccessWindowMinutes == minutes) {
                onAction(SettingsAction.SetNotificationAccessWindow(minutes))
            }
        }
        SettingSwitch(
            "Exact reminders",
            settings.notificationExactTiming,
            supportingText = "Use exact alarms when Android allows them",
        ) { onAction(SettingsAction.SetNotificationExactTiming(it)) }
        SettingSwitch("Notification actions", settings.notificationActionsEnabled) {
            onAction(SettingsAction.SetNotificationActionsEnabled(it))
        }
        Text("Lock screen", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
        NotificationLockScreenDetail.entries.forEach { detail ->
            ReminderChoice(detail.displayName(), settings.notificationLockScreenDetail == detail) {
                onAction(SettingsAction.SetNotificationLockScreenDetail(detail))
            }
        }
    }
}

private fun NotificationLockScreenDetail.displayName() = when (this) {
    NotificationLockScreenDetail.FULL -> "Show all"
    NotificationLockScreenDetail.HIDE_SENSITIVE -> "Hide protected details"
    NotificationLockScreenDetail.HIDDEN -> "Hide on lock screen"
}

private val reminderOptions = listOf(
    15 to "15 minutes before",
    30 to "30 minutes before",
    60 to "1 hour before",
    1440 to "1 day before",
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

@Composable
private fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.padding(vertical = 8.dp), content = content)
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
