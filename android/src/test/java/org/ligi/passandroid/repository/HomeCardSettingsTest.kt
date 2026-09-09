package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HomeCardSettingsTest {
    @Test
    fun `creator and pass type are hidden by default`() {
        assertThat(AppSettings().hiddenHomeCardSections)
            .containsExactlyInAnyOrder(HomeCardSection.CREATOR, HomeCardSection.PASS_TYPE)
    }

    @Test
    fun `tags come after pass type in the default order`() {
        assertThat(AppSettings().homeCardSectionOrder)
            .containsExactly(
                HomeCardSection.ARTWORK,
                HomeCardSection.TITLE,
                HomeCardSection.PRIMARY_FIELD,
                HomeCardSection.DATE,
                HomeCardSection.CREATOR,
                HomeCardSection.PASS_TYPE,
                HomeCardSection.CATEGORY,
            )
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
                HomeCardSection.PASS_TYPE,
                HomeCardSection.CATEGORY,
            )
    }
}
