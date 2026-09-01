package org.ligi.passandroid.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.defaultPassCategories
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.theme.PassTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import androidx.test.espresso.Espresso.pressBack

class PassScreensTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun emptyHomeShowsImportAction() {
        composeRule.setContent {
            PassTheme(AppSettings().themeMode) {
                PassHomeScreen(MainUiState(isContentLoading = false), {})
            }
        }
        composeRule.mainClock.advanceTimeBy(1_000)

        composeRule.onNodeWithText("Your passes live here").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Import passes").assertIsDisplayed()
    }

    @Test
    fun settingsExposeThemeAndAccessibilityOptions() {
        val settings = AppSettings(themeMode = ThemeMode.DARK)
        composeRule.setContent {
            PassTheme(settings.themeMode, settings.amoledBlackBackground) {
                SettingsScreen(settings, {})
            }
        }

        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Use AMOLED black background").assertIsDisplayed()
        composeRule.onNodeWithText("Use HDR and maximum code brightness").assertIsDisplayed()
    }

    @Test
    fun mobileLightLayoutRendersAtLargeFontScale() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))
                    .then(DeviceConfigurationOverride.FontScale(1.3f))
                    .then(DeviceConfigurationOverride.DarkMode(false)),
            ) {
                PassTheme(ThemeMode.SYSTEM) { PassHomeScreen(sampleState(), {}) }
            }
        }

        val image = composeRule.onRoot().captureToImage()
        assertVisualContent(image)
        composeRule.onNodeWithText("Boarding pass").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reorder Boarding pass").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pass actions").assertDoesNotExist()
    }

    @Test
    fun tabletDarkLayoutRendersAtAccessibilityFontScale() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(840.dp, 900.dp))
                    .then(DeviceConfigurationOverride.FontScale(1.8f))
                    .then(DeviceConfigurationOverride.DarkMode(true)),
            ) {
                PassTheme(ThemeMode.SYSTEM) { PassHomeScreen(sampleState(), {}) }
            }
        }

        val image = composeRule.onRoot().captureToImage()
        assertVisualContent(image)
        composeRule.onNodeWithText("Boarding pass").assertIsDisplayed()
        composeRule.onNodeWithText("Event ticket").assertIsDisplayed()
    }

    @Test
    fun editorExposesPassFieldsAndBarcodeWithoutArtworkControls() {
        val pass = pass("one", "Boarding pass", PassType.BOARDING).copy(
            barcodeFormat = PassBarCodeFormat.QR_CODE,
            barcodeMessage = "payload",
            fields = listOf(PassFieldUiModel("gate", "Gate", "A12", false, null)),
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { EditPassScreen(pass, {}) }
        }

        composeRule.onNodeWithText("Barcode: QR CODE").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_pass_list").performScrollToIndex(4)
        composeRule.onNodeWithText("Artwork").assertDoesNotExist()
        composeRule.onNodeWithText("Gate").assertIsDisplayed()
        composeRule.onNodeWithText("Add field").assertIsDisplayed()
    }

    @Test
    fun unchangedEditorClosesWithoutSaving() {
        val actions = mutableListOf<EditPassAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                EditPassScreen(pass("one", "Boarding pass", PassType.BOARDING), actions::add)
            }
        }

        composeRule.onNodeWithContentDescription("Save and go back").performClick()

        assertThat(actions).containsExactly(EditPassAction.Back)
    }

    @Test
    fun passHomeShowsOnlyTheSelectedCategory() {
        val state = sampleState().copy(
            passes = listOf(
                pass("one", "Boarding pass", PassType.BOARDING).copy(categoryId = "new"),
                pass("two", "Event ticket", PassType.EVENT).copy(categoryId = "archive"),
            ),
            categories = defaultPassCategories,
            selectedCategoryId = "new",
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, {}) }
        }

        composeRule.onNodeWithText("Boarding pass").assertIsDisplayed()
        composeRule.onNodeWithText("Event ticket").assertDoesNotExist()
    }

    @Test
    fun homeSearchBackFirstClearsFocusThenClosesSearch() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(sampleState(), {}) }
        }

        composeRule.onNodeWithContentDescription("Search passes").performClick()
        val search = composeRule.onNodeWithContentDescription("Pass search")
        search.performClick()
        search.assertIsFocused()
        composeRule.onNodeWithText("All").assertDoesNotExist()
        composeRule.onNodeWithText("Newest first").assertDoesNotExist()

        pressBack()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            runCatching {
                search.assertIsNotFocused()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithContentDescription("Pass search").assertIsNotFocused()

        pressBack()
        composeRule.onNodeWithContentDescription("Pass search").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Search passes").assertIsDisplayed()
    }

    @Test
    fun homeDrawerContainsPassViewCustomization() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(sampleState(), {}) }
        }

        composeRule.onNodeWithContentDescription("Navigation menu").performClick()

        composeRule.onNodeWithText("Pass view").assertIsDisplayed()
        composeRule.onNodeWithText("Home cards").assertIsDisplayed()
    }

    @Test
    fun categorySettingsExposeColorsAndManagementActions() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { CategorySettingsScreen(defaultPassCategories, {}) }
        }

        composeRule.onNodeWithText("Inbox").assertDoesNotExist()
        composeRule.onNodeWithText("Trash").assertDoesNotExist()
        composeRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add category").assertIsDisplayed()
    }

    @Test
    fun permanentDeleteIsNotExposed() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassDetailScreen(pass("one", "Boarding pass", PassType.BOARDING), onAction = {})
            }
        }

        composeRule.onNodeWithContentDescription("Pass actions").performClick()
        composeRule.onNodeWithText("Delete permanently").assertDoesNotExist()
    }

    @Test
    fun expandedPassCodeUsesModalPresentation() {
        val pass = pass("one", "Boarding pass", PassType.BOARDING).copy(
            barcodeFormat = PassBarCodeFormat.QR_CODE,
            barcodeMessage = "payload",
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassDetailScreen(pass, onAction = {}, initialCodeExpanded = true)
            }
        }

        composeRule.onNodeWithContentDescription("Expanded pass code").assertIsDisplayed()
    }

    private fun assertVisualContent(image: androidx.compose.ui.graphics.ImageBitmap) {
        assertThat(image.width).isGreaterThan(0)
        assertThat(image.height).isGreaterThan(0)
        val pixels = image.toPixelMap()
        val colors = buildSet {
            val xStep = maxOf(1, image.width / 20)
            val yStep = maxOf(1, image.height / 20)
            for (y in 0 until image.height step yStep) {
                for (x in 0 until image.width step xStep) add(pixels[x, y])
            }
        }
        assertThat(colors.size).isGreaterThan(3)
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
