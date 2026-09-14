package org.ligi.passandroid.repository

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.reminder.NotificationLockScreenDetail
import org.ligi.passandroid.reminder.NotificationAction

class DataStoreSettingsRepositoryTest {
    @Test
    fun persistsEveryApplicationSetting() {
        runBlocking {
            val repository = DataStoreSettingsRepository(
                InstrumentationRegistry.getInstrumentation().targetContext,
            )
            repository.setThemeMode(ThemeMode.DARK)
            repository.setAmoledBlackBackground(true)
            repository.setAutomaticBrightness(false)
            repository.setSortOrder(PassSortOrder.TYPE)
            repository.setPassOrder(listOf("pass-3", "pass-1"))
            val categories = defaultPassCategories + PassCategory("personal", "Personal", 0xFF006C4C)
            repository.setCategories(categories)
            repository.setHighlightTodayPasses(false)
            repository.setAutomaticallyMarkPast(true)
            repository.setOfferCalendarAfterImport(true)
            repository.setRemindersEnabled(true)
            repository.setReminderMinutes(setOf(15, 30))
            repository.setReminderExcludedPassIds(setOf("pass-2"))
            repository.setReminderLeadMinutesByPass(mapOf("pass-3" to 45))
            repository.setReminderExactPassIds(setOf("pass-4"))
            repository.setReminderActionsByPass(mapOf("pass-3" to setOf(NotificationAction.OPEN_CODE)))
            repository.setNotificationAccessWindowMinutes(30)
            repository.setNotificationExactTiming(true)
            repository.setNotificationActionsEnabled(false)
            repository.setNotificationLockScreenDetail(NotificationLockScreenDetail.HIDDEN)
            repository.setPassDetailLayout(
                listOf(PassDetailSection.BARCODE, PassDetailSection.ARTWORK),
                setOf(PassDetailSection.ARTWORK),
            )
            repository.setHomeCardLayout(
                defaultHomeCardSectionOrder,
                setOf(HomeCardSection.CREATOR),
            )
            repository.setLockAllPasses(true)
            repository.setShowProtectedPassLockIcon(true)
            repository.setBlurProtectedPassCards(true)
            repository.setSeparateProtectedPasses(true)
            repository.setBlockScreenshots(true)
            repository.setTrashEnabled(false)
            val imageExportOptions = PassImageExportOptions(
                aspectRatio = PassImageAspectRatio.RATIO_4_5,
                orientation = PassImageOrientation.LANDSCAPE,
                content = PassImageContent(artwork = false, barcode = false),
            )
            repository.setImageExportOptions(imageExportOptions)

            val restored = repository.settings.first {
                it.themeMode == ThemeMode.DARK && it.amoledBlackBackground && !it.automaticBrightness &&
                    it.sortOrder == PassSortOrder.TYPE && it.passOrder == listOf("pass-3", "pass-1") &&
                    it.categories == categories &&
                    !it.highlightTodayPasses && it.automaticallyMarkPast && it.offerCalendarAfterImport &&
                    it.remindersEnabled && it.reminderMinutes == setOf(15, 30) &&
                    it.reminderExcludedPassIds == setOf("pass-2") &&
                    it.reminderLeadMinutesByPass == mapOf("pass-3" to 45) &&
                    it.reminderExactPassIds == setOf("pass-4") &&
                    it.reminderActionsByPass == mapOf("pass-3" to setOf(NotificationAction.OPEN_CODE)) &&
                    it.notificationAccessWindowMinutes == 30 && it.notificationExactTiming &&
                    !it.notificationActionsEnabled &&
                    it.notificationLockScreenDetail == NotificationLockScreenDetail.HIDDEN &&
                    it.lockAllPasses && it.showProtectedPassLockIcon &&
                    it.blurProtectedPassCards && it.separateProtectedPasses && it.blockScreenshots &&
                    !it.trashEnabled &&
                    it.imageExportOptions == imageExportOptions &&
                    it.passDetailSectionOrder == listOf(
                        PassDetailSection.BARCODE,
                        PassDetailSection.ARTWORK,
                        PassDetailSection.FIELDS,
                        PassDetailSection.LOCATIONS,
                        PassDetailSection.CALENDAR,
                    ) && it.hiddenPassDetailSections == setOf(PassDetailSection.ARTWORK)
            }

            assertThat(restored).isEqualTo(
                AppSettings(
                    ThemeMode.DARK,
                    amoledBlackBackground = true,
                    automaticBrightness = false,
                    sortOrder = PassSortOrder.TYPE,
                    passOrder = listOf("pass-3", "pass-1"),
                    categories = categories,
                    highlightTodayPasses = false,
                    automaticallyMarkPast = true,
                    offerCalendarAfterImport = true,
                    remindersEnabled = true,
                    reminderMinutes = setOf(15, 30),
                    reminderExcludedPassIds = setOf("pass-2"),
                    reminderLeadMinutesByPass = mapOf("pass-3" to 45),
                    reminderExactPassIds = setOf("pass-4"),
                    reminderActionsByPass = mapOf("pass-3" to setOf(NotificationAction.OPEN_CODE)),
                    notificationAccessWindowMinutes = 30,
                    notificationExactTiming = true,
                    notificationActionsEnabled = false,
                    notificationLockScreenDetail = NotificationLockScreenDetail.HIDDEN,
                    passDetailSectionOrder = listOf(
                        PassDetailSection.BARCODE,
                        PassDetailSection.ARTWORK,
                        PassDetailSection.FIELDS,
                        PassDetailSection.LOCATIONS,
                        PassDetailSection.CALENDAR,
                    ),
                    hiddenPassDetailSections = setOf(PassDetailSection.ARTWORK),
                    hiddenHomeCardSections = setOf(HomeCardSection.CREATOR),
                    lockAllPasses = true,
                    showProtectedPassLockIcon = true,
                    blurProtectedPassCards = true,
                    separateProtectedPasses = true,
                    blockScreenshots = true,
                    trashEnabled = false,
                    imageExportOptions = imageExportOptions,
                ),
            )
        }
    }

    @After
    fun resetSettings() {
        runBlocking {
            val repository = DataStoreSettingsRepository(
                InstrumentationRegistry.getInstrumentation().targetContext,
            )
            repository.setThemeMode(ThemeMode.SYSTEM)
            repository.setAmoledBlackBackground(false)
            repository.setAutomaticBrightness(true)
            repository.setSortOrder(PassSortOrder.DATE_DESC)
            repository.setPassOrder(emptyList())
            repository.setCategories(defaultPassCategories)
            repository.setHighlightTodayPasses(true)
            repository.setAutomaticallyMarkPast(false)
            repository.setOfferCalendarAfterImport(false)
            repository.setRemindersEnabled(false)
            repository.setReminderMinutes(setOf(60))
            repository.setReminderExcludedPassIds(emptySet())
            repository.setReminderLeadMinutesByPass(emptyMap())
            repository.setReminderExactPassIds(emptySet())
            repository.setReminderActionsByPass(emptyMap())
            repository.setNotificationAccessWindowMinutes(15)
            repository.setNotificationExactTiming(false)
            repository.setNotificationActionsEnabled(true)
            repository.setNotificationLockScreenDetail(NotificationLockScreenDetail.HIDE_SENSITIVE)
            repository.setPassDetailLayout(defaultPassDetailSectionOrder, emptySet())
            repository.setHomeCardLayout(defaultHomeCardSectionOrder, defaultHiddenHomeCardSections)
            repository.setLockAllPasses(false)
            repository.setShowProtectedPassLockIcon(true)
            repository.setBlurProtectedPassCards(false)
            repository.setSeparateProtectedPasses(false)
            repository.setBlockScreenshots(false)
            repository.setTrashEnabled(true)
            repository.setImageExportOptions(PassImageExportOptions())
        }
    }
}
