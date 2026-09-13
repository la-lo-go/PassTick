package org.ligi.passandroid.ui.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.theme.PassTheme

class HomeInteractionRegressionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun pinnedStateRecompositionMovesCardToPinnedSection() {
        var state by mutableStateOf(MainUiState(passes = listOf(pass("one", "Ticket")), isContentLoading = false))
        composeRule.setContent { PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, {}) } }
        composeRule.onNodeWithContentDescription("Pinned pass").assertDoesNotExist()
        state = state.copy(passes = state.passes.map { it.copy(isFavorite = true) })
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Pinned pass").assertDoesNotExist()
    }

    @Test
    fun protectedPresentationRefreshesWhenOnlyProtectionChanges() {
        var state by mutableStateOf(
            MainUiState(
                passes = listOf(pass("one", "Ticket")),
                settings = AppSettings(blurProtectedPassCards = true, showProtectedPassLockIcon = true),
                isContentLoading = false,
            ),
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(state, onAction = { action ->
                    if (action is HomeAction.ToggleProtected) {
                        state = state.copy(passes = state.passes.map { it.copy(isProtected = !it.isProtected) })
                    }
                })
            }
        }
        composeRule.onNodeWithText("Ticket").performTouchInput { swipeLeft() }
        composeRule.onNodeWithContentDescription("Protect pass").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Protected pass").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Protected pass information blurred").assertIsDisplayed()
        composeRule.onNodeWithText("Ticket").performTouchInput { swipeLeft() }
        composeRule.onNodeWithContentDescription("Remove protection").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Protected pass").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Protected pass information blurred").assertDoesNotExist()
    }

    @Test
    fun openingSecondCardSideMenuClosesFirst() {
        val state = MainUiState(
            passes = listOf(pass("one", "First"), pass("two", "Second")),
            isContentLoading = false,
        )
        composeRule.setContent { PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, {}) } }
        composeRule.onNodeWithText("First").performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("Second").performTouchInput { swipeLeft() }
        composeRule.onAllNodesWithContentDescription("Delete").assertCountEquals(1)
    }

    @Test
    fun swipingArchivedCardRightDispatchesRestore() {
        val actions = mutableListOf<HomeAction>()
        val state = MainUiState(
            passes = listOf(pass("one", "Ticket").copy(isArchived = true)),
            selectedCategoryId = "archived",
            isContentLoading = false,
        )
        composeRule.setContent { PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, actions::add) } }
        composeRule.onNodeWithText("Ticket").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        assertThat(actions.filterIsInstance<HomeAction.Restore>()).hasSize(1)
    }

    @Test
    fun sortingSelectionDispatchesOneOrderAction() {
        val actions = mutableListOf<HomeAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(MainUiState(passes = listOf(pass("one", "Ticket")), settings = AppSettings(sortOrder = PassSortOrder.TYPE), isContentLoading = false), actions::add)
            }
        }
        composeRule.onNodeWithText("Newest").performClick()
        assertThat(actions).containsExactly(HomeAction.SetSortOrder(PassSortOrder.DATE_DESC))
    }

    @Test
    fun ascendingDateSortUsesOldestLabel() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(
                    MainUiState(
                        passes = listOf(pass("one", "Ticket")),
                        settings = AppSettings(sortOrder = PassSortOrder.DATE_ASC),
                        isContentLoading = false,
                    ),
                    {},
                )
            }
        }
        composeRule.onNodeWithText("Oldest").assertIsDisplayed()
        composeRule.onNodeWithText("Newest").assertDoesNotExist()
    }

    @Test
    fun changingSortRefreshesExistingCardModelsWithSameIds() {
        val first = pass("one", "First")
        val second = pass("two", "Second")
        var state by mutableStateOf(
            MainUiState(
                passes = listOf(first, second),
                settings = AppSettings(sortOrder = PassSortOrder.MANUAL),
                isContentLoading = false,
            ),
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(state, onAction = { action ->
                    if (action is HomeAction.SetSortOrder) {
                        state = state.copy(
                            settings = state.settings.copy(sortOrder = action.order),
                            passes = state.passes.reversed(),
                        )
                    }
                })
            }
        }
        val before = composeRule.onNodeWithText("First").fetchSemanticsNode().boundsInRoot.top
        composeRule.onNodeWithText("Newest").performClick()
        composeRule.waitForIdle()
        val after = composeRule.onNodeWithText("First").fetchSemanticsNode().boundsInRoot.top
        assertThat(after).isNotEqualTo(before)
    }

    @Test
    fun drawerShowsDynamicTagsAndFiltersWhenSelected() {
        val tag = PassCategory("concerts", "Concerts", 0xFF336699, PassCategoryRole.CUSTOM)
        val state = MainUiState(
            passes = listOf(pass("one", "Concert ticket").copy(tagIds = setOf("concerts"))),
            categories = listOf(tag),
            isContentLoading = false,
        )
        val actions = mutableListOf<HomeAction>()
        composeRule.setContent { PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, actions::add) } }
        composeRule.onNodeWithContentDescription("Navigation menu").performClick()
        composeRule.onNodeWithTag("drawer_filter_concerts").assertIsDisplayed().performClick()
        assertThat(actions).contains(HomeAction.SelectCategory("concerts"))
    }

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
