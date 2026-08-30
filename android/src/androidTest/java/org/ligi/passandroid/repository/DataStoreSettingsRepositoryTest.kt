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

            val restored = repository.settings.first {
                it.themeMode == ThemeMode.DARK && it.condensedPasses && !it.automaticBrightness &&
                    it.sortOrder == PassSortOrder.TYPE && it.categories == categories
            }

            assertThat(restored).isEqualTo(
                AppSettings(
                    ThemeMode.DARK,
                    condensedPasses = true,
                    automaticBrightness = false,
                    sortOrder = PassSortOrder.TYPE,
                    categories = categories,
                ),
            )
        }
    }
}
