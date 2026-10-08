package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PinnedTagGroupsTest {
    private val tags = mapOf(
        "a" to setOf("work"),
        "b" to setOf("trip", "work"),
        "c" to setOf("misc"),
        "d" to emptySet(),
    )

    private fun group(pinned: Set<String>) = groupByPinnedTags(tags.keys.toList(), pinned) { tags.getValue(it) }

    @Test
    fun `groups follow the pinned order and a pass joins only its first pinned tag`() {
        val result = group(linkedSetOf("trip", "work"))

        assertThat(result.groups).containsExactly("trip" to listOf("b"), "work" to listOf("a"))
        assertThat(result.rest).containsExactly("c", "d")
    }

    @Test
    fun `a pinned tag without passes makes no group`() {
        val result = group(linkedSetOf("empty"))

        assertThat(result.groups).isEmpty()
        assertThat(result.rest).containsExactly("a", "b", "c", "d")
    }
}
