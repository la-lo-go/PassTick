package org.ligi.passandroid.ui.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.MainActivity
import org.ligi.passandroid.TestApp
import org.ligi.passandroid.model.pass.PassImpl

/**
 * The home list keeps scrolling after a pass detail round trip.
 *
 * The detail shell composes a second home for its list pane; the pane measures with a degenerate
 * viewport. Sharing one list state between both homes left the visible list unable to scroll.
 */
@OptIn(ExperimentalTestApi::class)
class HomeScrollAfterDetailTest {

    init {
        TestApp.passStore.setList(
            (1..30).map { index -> PassImpl("pass-$index").apply { description = "Pass $index" } },
        )
    }

    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun restorePassStore() {
        TestApp.passStore.setList(
            listOf(PassImpl("recovery-pass").apply { description = "Recovery pass" }),
        )
    }

    @Test
    fun homeListScrollsAfterReturningFromPassDetail() {
        composeRule.waitUntilAtLeastOneExists(hasText("Pass 1"), timeoutMillis = 20_000)
        composeRule.waitForIdle()
        val positionBefore = scrollOffset()

        val first = visibleCards().firstOrNull() ?: error("no visible card")
        composeRule.onNodeWithText("Pass " + first.removePrefix("pass-")).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertThat(detailNodes()).isEqualTo(1)

        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Pass 1"), timeoutMillis = 10_000)
        composeRule.waitForIdle()

        assertThat(scrollOffset()).isEqualTo(positionBefore)

        composeRule.onNodeWithTag("home_list").performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertThat(scrollOffset()).isGreaterThan(positionBefore + 400f)
    }

    private fun scrollOffset(): Float {
        val config = composeRule.onNodeWithTag("home_list").fetchSemanticsNode().config
        return config[SemanticsProperties.VerticalScrollAxisRange].value()
    }

    private fun detailNodes(): Int =
        composeRule.onAllNodesWithContentDescription("Edit pass").fetchSemanticsNodes().size

    private fun visibleCards(): List<String> =
        (1..30).mapNotNull { index ->
            val id = "pass-$index"
            composeRule.onAllNodesWithTag("pass_card_$id").fetchSemanticsNodes()
                .map { it.boundsInRoot }
                .firstOrNull { it.width > 0f && it.top > 200f && it.bottom < 2100f }
                ?.let { id }
        }
}
