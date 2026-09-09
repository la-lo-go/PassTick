package org.ligi.passandroid.repository

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
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
            repository.setNotificationSnoozeEnabled(false)
            repository.setNotificationLockScreenDetail(NotificationLockScreenDetail.HIDDEN)
            repository.setUpdateNotificationAtEventStart(false)
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
                    !it.notificationActionsEnabled && !it.notificationSnoozeEnabled &&
                    it.notificationLockScreenDetail == NotificationLockScreenDetail.HIDDEN &&
                    !it.updateNotificationAtEventStart &&
                    it.lockAllPasses && it.showProtectedPassLockIcon &&
                    it.blurProtectedPassCards && it.separateProtectedPasses &&
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
                    notificationSnoozeEnabled = false,
                    notificationLockScreenDetail = NotificationLockScreenDetail.HIDDEN,
                    updateNotificationAtEventStart = false,
                    passDetailSectionOrder = listOf(
                        PassDetailSection.BARCODE,
                        PassDetailSection.ARTWORK,
                        PassDetailSection.FIELDS,
                        PassDetailSection.LOCATIONS,
                        PassDetailSection.CALENDAR,
                    ),
                    hiddenPassDetailSections = setOf(PassDetailSection.ARTWORK),
                    lockAllPasses = true,
                    showProtectedPassLockIcon = true,
                    blurProtectedPassCards = true,
                    separateProtectedPasses = true,
                ),
            )
        }
    }
}
