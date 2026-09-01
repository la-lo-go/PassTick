package org.ligi.passandroid.ui.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.theme.PassTheme

class HomeCardReorderGestureTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun verticalReorderDoesNotRevealTheArchiveSwipeLayer() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(state(), onAction = {}, showTodayHero = false)
            }
        }

        val handle = composeRule.onNodeWithContentDescription("Reorder Boarding pass")

        handle.performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(Offset(0f, 48f))
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Archive").assertDoesNotExist()

        handle.performTouchInput { up() }
    }

    @Test
    fun reorderRemainsAReversiblePreviewUntilTheFingerIsReleased() {
        val actions = mutableListOf<HomeAction>()
        composeRule.setContent {
            PassTheme(AppSettings().themeMode) {
                PassHomeScreen(state(), actions::add, showTodayHero = false)
            }
        }

        val card = composeRule.onNodeWithText("Boarding pass")
        val handle = composeRule.onNodeWithContentDescription("Reorder Boarding pass")
        val originalTop = card.fetchSemanticsNode().boundsInRoot.top

        handle.performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(Offset(0f, 120f))
        }
        composeRule.waitForIdle()

        assertThat(actions.filterIsInstance<HomeAction.ReorderPass>())
            .describedAs("persistent reorder actions before release")
            .isEmpty()
        assertThat(card.fetchSemanticsNode().boundsInRoot.top - originalTop)
            .describedAs("card displacement while crossing the reorder threshold")
            .isGreaterThan(90f)

        handle.performTouchInput { moveBy(Offset(0f, -110f)) }
        composeRule.waitForIdle()

        assertThat(actions.filterIsInstance<HomeAction.ReorderPass>()).isEmpty()
        assertThat(card.fetchSemanticsNode().boundsInRoot.top - originalTop)
            .describedAs("card displacement after reversing the drag")
            .isBetween(-5f, 25f)

        handle.performTouchInput { up() }
        composeRule.waitForIdle()

        assertThat(actions.filterIsInstance<HomeAction.ReorderPass>())
            .describedAs("a reorder after returning below the threshold")
            .isEmpty()
    }

    @Test
    fun reorderCommitsThePreviewedOrderExactlyOnceOnRelease() {
        val actions = mutableListOf<HomeAction>()
        composeRule.setContent {
            PassTheme(AppSettings().themeMode) {
                PassHomeScreen(state(), actions::add, showTodayHero = false)
            }
        }

        val first = composeRule.onNodeWithText("Boarding pass").fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithText("Event ticket").fetchSemanticsNode().boundsInRoot
        val handle = composeRule.onNodeWithContentDescription("Reorder Boarding pass")
        val deltaToSecond = second.center.y - first.center.y + 8f

        handle.performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(Offset(0f, deltaToSecond))
            up()
        }
        composeRule.waitForIdle()

        assertThat(actions.filterIsInstance<HomeAction.ReorderPass>())
            .containsExactly(HomeAction.ReorderPass(listOf("two", "one", "three")))
    }

    private fun state() = MainUiState(
        passes = listOf(
            pass("one", "Boarding pass"),
            pass("two", "Event ticket"),
            pass("three", "Museum ticket"),
        ),
    )

    private fun pass(id: String, description: String) = PassUiModel(
        id = id,
        description = description,
        creator = "Example issuer",
        type = PassType.EVENT,
        accentColor = 0xFF2859C5.toInt(),
        barcodeFormat = null,
        barcodeMessage = null,
        barcodeAlternativeText = null,
        fields = emptyList(),
        locations = emptyList(),
        calendarEvent = null,
    )
}
