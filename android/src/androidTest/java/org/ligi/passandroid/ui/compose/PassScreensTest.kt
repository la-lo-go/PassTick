package org.ligi.passandroid.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.theme.PassTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat

class PassScreensTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun emptyListShowsImportAction() {
        composeRule.setContent {
            PassTheme(AppSettings().themeMode) {
                PassListScreen(MainUiState(), {}, {}, {}, {}, {})
            }
        }
        composeRule.mainClock.advanceTimeBy(1_000)

        composeRule.onNodeWithText("No passes").assertIsDisplayed()
        composeRule.onNodeWithTag("import_pass").assertIsDisplayed()
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

    @Test
    fun mobileLightLayoutRendersAtLargeFontScale() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))
                    .then(DeviceConfigurationOverride.FontScale(1.3f))
                    .then(DeviceConfigurationOverride.DarkMode(false)),
            ) {
                PassTheme(ThemeMode.SYSTEM) { PassListScreen(sampleState(), {}, {}, {}, {}, {}) }
            }
        }

        val image = composeRule.onRoot().captureToImage()
        assertThat(image.width).isGreaterThan(0)
        assertThat(image.height).isGreaterThan(0)
        composeRule.onNodeWithText("Boarding pass").assertIsDisplayed()
    }

    @Test
    fun tabletDarkLayoutRendersAtAccessibilityFontScale() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(840.dp, 900.dp))
                    .then(DeviceConfigurationOverride.FontScale(1.8f))
                    .then(DeviceConfigurationOverride.DarkMode(true)),
            ) {
                PassTheme(ThemeMode.SYSTEM) { PassListScreen(sampleState(), {}, {}, {}, {}, {}) }
            }
        }

        val image = composeRule.onRoot().captureToImage()
        assertThat(image.width).isGreaterThan(0)
        assertThat(image.height).isGreaterThan(0)
        composeRule.onNodeWithText("Boarding pass").assertIsDisplayed()
        composeRule.onNodeWithText("Event ticket").assertIsDisplayed()
    }

    private fun sampleState() = MainUiState(
        passes = listOf(
            pass("one", "Boarding pass", PassType.BOARDING),
            pass("two", "Event ticket", PassType.EVENT),
        ),
    )

    private fun pass(id: String, description: String, type: PassType) = PassUiModel(
        id = id,
        description = description,
        creator = "Example issuer",
        type = type,
        accentColor = 0xFF2859C5.toInt(),
        barcodeFormat = null,
        barcodeMessage = null,
        barcodeAlternativeText = null,
        fields = emptyList(),
        locations = emptyList(),
        calendarEvent = null,
    )
}
