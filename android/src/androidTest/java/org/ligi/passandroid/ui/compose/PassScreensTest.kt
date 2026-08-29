package org.ligi.passandroid.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.theme.PassTheme

class PassScreensTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun emptyListShowsImportAction() {
        composeRule.setContent {
            PassTheme(AppSettings().themeMode) {
                PassListScreen(MainUiState(), {}, {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText("No passes").assertIsDisplayed()
        composeRule.onNodeWithText("Import pass").assertIsDisplayed()
    }

    @Test
    fun settingsExposeThemeAndAccessibilityOptions() {
        composeRule.setContent {
            PassTheme(AppSettings().themeMode) {
                SettingsScreen(AppSettings(), {}, {})
            }
        }

        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Automatic barcode brightness").assertIsDisplayed()
    }
}
