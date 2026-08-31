package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HomeCardSettingsTest {
    @Test
    fun `creator is hidden by default`() {
        assertThat(AppSettings().hiddenHomeCardSections).containsExactly(HomeCardSection.CREATOR)
    }

    @Test
    fun `normalization preserves custom order and appends new sections`() {
        assertThat(normalizeHomeCardSectionOrder(listOf(HomeCardSection.DATE, HomeCardSection.TITLE)))
            .containsExactly(
                HomeCardSection.ARTWORK,
                HomeCardSection.DATE,
                HomeCardSection.TITLE,
                HomeCardSection.PRIMARY_FIELD,
                HomeCardSection.CREATOR,
                HomeCardSection.CATEGORY,
                HomeCardSection.PASS_TYPE,
            )
    }
}
