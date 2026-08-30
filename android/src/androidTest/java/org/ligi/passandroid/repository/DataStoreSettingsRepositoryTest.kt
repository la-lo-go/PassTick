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
            repository.setCondensedPasses(true)
            repository.setAutomaticBrightness(false)
            repository.setSortOrder(PassSortOrder.TYPE)
            val categories = defaultPassCategories + PassCategory("travel", "Travel", 0xFF006C4C)
            repository.setCategories(categories)
            repository.setHighlightTodayPasses(false)
            repository.setAutomaticallyMarkPast(true)
            repository.setOfferCalendarAfterImport(true)
            repository.setRemindersEnabled(true)
            repository.setDefaultReminderMinutes(30)
            repository.setQuickCodePassId("pass-1")
            repository.setReminderExcludedPassIds(setOf("pass-2"))

            val restored = repository.settings.first {
                it.themeMode == ThemeMode.DARK && it.condensedPasses && !it.automaticBrightness &&
                    it.sortOrder == PassSortOrder.TYPE && it.categories == categories &&
                    !it.highlightTodayPasses && it.automaticallyMarkPast && it.offerCalendarAfterImport &&
                    it.remindersEnabled && it.defaultReminderMinutes == 30 &&
                    it.quickCodePassId == "pass-1" && it.reminderExcludedPassIds == setOf("pass-2")
            }

            assertThat(restored).isEqualTo(
                AppSettings(
                    ThemeMode.DARK,
                    condensedPasses = true,
                    automaticBrightness = false,
                    sortOrder = PassSortOrder.TYPE,
                    categories = categories,
                    highlightTodayPasses = false,
                    automaticallyMarkPast = true,
                    offerCalendarAfterImport = true,
                    remindersEnabled = true,
                    defaultReminderMinutes = 30,
                    quickCodePassId = "pass-1",
                    reminderExcludedPassIds = setOf("pass-2"),
                ),
            )
        }
    }
}
