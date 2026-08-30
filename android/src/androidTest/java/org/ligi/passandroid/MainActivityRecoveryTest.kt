package org.ligi.passandroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MainActivityRecoveryTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun restoresTheCurrentDestinationAfterActivityRecreation() {
        composeRule.onNodeWithText("Recovery pass").performClick()
        composeRule.onNodeWithContentDescription("Edit").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithContentDescription("Edit").assertIsDisplayed()
        composeRule.onNodeWithText("Recovery pass").assertIsDisplayed()
    }
}
