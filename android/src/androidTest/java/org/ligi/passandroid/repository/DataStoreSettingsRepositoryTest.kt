package org.ligi.passandroid.repository

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.comparator.PassSortOrder

class DataStoreSettingsRepositoryTest {
    @Test
    fun persistsEveryApplicationSetting() {
        runBlocking {
            val repository = DataStoreSettingsRepository(
                InstrumentationRegistry.getInstrumentation().targetContext,
            )
            repository.setThemeMode(ThemeMode.DARK)
            repository.setAutomaticBrightness(false)
            repository.setSortOrder(PassSortOrder.TYPE)
            val categories = defaultPassCategories + PassCategory("travel", "Travel", 0xFF006C4C)
            repository.setCategories(categories)
            repository.setHighlightTodayPasses(false)
            repository.setAutomaticallyMarkPast(true)
            repository.setOfferCalendarAfterImport(true)
            repository.setRemindersEnabled(true)
            repository.setReminderMinutes(setOf(15, 30))
            repository.setReminderExcludedPassIds(setOf("pass-2"))
            repository.setReminderLeadMinutesByPass(mapOf("pass-3" to 45))

            val restored = repository.settings.first {
                it.themeMode == ThemeMode.DARK && !it.automaticBrightness &&
                    it.sortOrder == PassSortOrder.TYPE && it.categories == categories &&
                    !it.highlightTodayPasses && it.automaticallyMarkPast && it.offerCalendarAfterImport &&
                    it.remindersEnabled && it.reminderMinutes == setOf(15, 30) &&
                    it.reminderExcludedPassIds == setOf("pass-2") &&
                    it.reminderLeadMinutesByPass == mapOf("pass-3" to 45)
            }

            assertThat(restored).isEqualTo(
                AppSettings(
                    ThemeMode.DARK,
                    automaticBrightness = false,
                    sortOrder = PassSortOrder.TYPE,
                    categories = categories,
                    highlightTodayPasses = false,
                    automaticallyMarkPast = true,
                    offerCalendarAfterImport = true,
                    remindersEnabled = true,
                    reminderMinutes = setOf(15, 30),
                    reminderExcludedPassIds = setOf("pass-2"),
                    reminderLeadMinutesByPass = mapOf("pass-3" to 45),
                ),
            )
        }
    }
}
