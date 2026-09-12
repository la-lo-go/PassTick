package org.ligi.passandroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.ExperimentalTestApi
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MainActivityRecoveryTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun restoresTheCurrentDestinationAfterActivityRecreation() {
        composeRule.waitUntilAtLeastOneExists(hasText("Recovery pass"), timeoutMillis = 15_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Recovery pass").performClick()
        composeRule.onNodeWithContentDescription("Edit pass").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Recovery pass"), timeoutMillis = 15_000)
        composeRule.onNodeWithContentDescription("Edit pass").assertIsDisplayed()
        composeRule.onNodeWithText("Recovery pass").assertIsDisplayed()
    }
}
