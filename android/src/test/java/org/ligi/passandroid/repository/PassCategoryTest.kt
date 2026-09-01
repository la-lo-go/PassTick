package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PassCategoryTest {
    @Test
    fun `inbox and trash remain internal workflow states`() {
        val visible = defaultPassCategories.filter(PassCategory::isUserOrganized)

        assertThat(visible.map(PassCategory::role)).containsExactly(
            PassCategoryRole.FAVORITES,
            PassCategoryRole.ARCHIVE,
            PassCategoryRole.PAST,
        )
        assertThat(defaultPassCategories.map(PassCategory::role)).contains(
            PassCategoryRole.INBOX,
            PassCategoryRole.TRASH,
        )
    }

    @Test
    fun `custom categories are user organized`() {
        val category = PassCategory("travel", "Travel", 0xFF006C4C, PassCategoryRole.CUSTOM)

        assertThat(category.isUserOrganized()).isTrue()
    }
}
