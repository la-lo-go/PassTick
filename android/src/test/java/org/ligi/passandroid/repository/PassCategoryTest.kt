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
            PassCategoryRole.CUSTOM,
            PassCategoryRole.CUSTOM,
            PassCategoryRole.CUSTOM,
            PassCategoryRole.CUSTOM,
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

    @Test
    fun `recommended tags appear once and do not return after deletion`() {
        val firstRun = categoriesFrom(encoded = null, defaultsInitialized = false)
        val afterDeletion = categoriesFrom(
            encoded = """[{"id":"new","name":"Inbox","colorArgb":4282339765,"role":"INBOX","icon":"label"}]""",
            defaultsInitialized = true,
        )

        assertThat(firstRun.filter { it.role == PassCategoryRole.CUSTOM }.map(PassCategory::name))
            .containsExactly("Travel", "Events", "Loyalty", "Work")
        assertThat(afterDeletion).noneMatch { it.role == PassCategoryRole.CUSTOM }
    }
}
