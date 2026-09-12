package org.ligi.passandroid.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.defaultPassCategories
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.PassDetailSection
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassArtworkUiModel
import org.ligi.passandroid.ui.state.PassCustomizationAction
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.theme.PassTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import androidx.activity.ComponentActivity

class PassScreensTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

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
                SettingsScreen(settings, onAction = {})
            }
        }

        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Use AMOLED black background").assertIsDisplayed()
        composeRule.onNodeWithText("Use HDR and maximum code brightness").assertIsDisplayed()
    }

    @Test
    fun settingsExposeProtectedPassPrivacyOptions() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { SettingsScreen(AppSettings(), onAction = {}) }
        }

        composeRule.onNodeWithTag("settings_list").performScrollToIndex(3)
        composeRule.onNodeWithText("Privacy").assertIsDisplayed()
        composeRule.onNodeWithText("Protect the app").assertIsDisplayed()
        composeRule.onNodeWithText("Show a lock icon on protected passes").assertIsDisplayed()
        composeRule.onNodeWithText("Blur protected pass information").assertIsDisplayed()
        composeRule.onNodeWithText("Keep protected passes in a locked section").assertIsDisplayed()
    }

    @Test
    fun settingsExposeNotificationPolicyOptions() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { SettingsScreen(AppSettings(remindersEnabled = true), onAction = {}) }
        }

        composeRule.onNodeWithTag("settings_list").performScrollToIndex(5)
        composeRule.onNodeWithText("Exact reminders").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Notification actions").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Hide protected details").assertExists()
    }

    @Test
    fun settingsGroupPassListCustomizationLinks() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { SettingsScreen(AppSettings(), onAction = {}) }
        }

        composeRule.onNodeWithTag("settings_list").performScrollToIndex(2)
        composeRule.onNodeWithText("Customize pass list").assertIsDisplayed()
        composeRule.onNodeWithText("Pass view").assertIsDisplayed()
        composeRule.onNodeWithText("Home cards").assertIsDisplayed()
        composeRule.onNodeWithText("Tags").assertIsDisplayed()
        composeRule.onNodeWithText("Choose sections and their order").assertDoesNotExist()
        composeRule.onNodeWithText("Choose card content and order").assertDoesNotExist()
        composeRule.onNodeWithText("Manage names, icons, colors, and order").assertDoesNotExist()
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
            PassTheme(ThemeMode.LIGHT) { EditPassScreen(pass, onAction = {}) }
        }

        composeRule.onNodeWithTag("edit_pass_list").performScrollToIndex(1)
        composeRule.onNodeWithText("Code").performClick()
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
                EditPassScreen(pass("one", "Boarding pass", PassType.BOARDING), onAction = actions::add)
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
        composeRule.onNodeWithText("Newest").assertDoesNotExist()

        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitUntil(timeoutMillis = 2_000) {
            runCatching {
                search.assertIsNotFocused()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithContentDescription("Pass search").assertIsNotFocused()

        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithContentDescription("Pass search").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Search passes").assertIsDisplayed()
    }

    @Test
    fun expandedSearchUsesOnlyTheTextFieldWithoutASecondSearchIcon() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(sampleState(), {}) }
        }

        composeRule.onNodeWithContentDescription("Search passes").performClick()

        composeRule.onNodeWithContentDescription("Pass search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Search passes").assertDoesNotExist()
        composeRule.onNodeWithText("Search passes").assertIsDisplayed()
    }

    @Test
    fun protectedPassIsExcludedFromSearchAndHoldPreview() {
        val protectedPass = pass("private", "Private ticket", PassType.EVENT).copy(isProtected = true)
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(MainUiState(passes = listOf(protectedPass), isContentLoading = false), {})
            }
        }

        composeRule.onNodeWithText("Private ticket").performTouchInput {
            down(center)
            advanceEventTime(1_200)
            up()
        }
        composeRule.onNodeWithContentDescription("Pass preview scrim").assertDoesNotExist()
        composeRule.onNodeWithText("Unlock the pass to preview it").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Protected pass").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Search passes").performClick()
        composeRule.onNodeWithText("Private ticket").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Pass search").performTextInput("Private")

        composeRule.onNodeWithText("No matching passes").assertIsDisplayed()
    }

    @Test
    fun protectedPassPresentationRespectsPrivacySettings() {
        val protectedPass = pass("private", "Private ticket", PassType.EVENT).copy(isProtected = true)
        val settings = AppSettings(showProtectedPassLockIcon = false, blurProtectedPassCards = true)
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(
                    MainUiState(passes = listOf(protectedPass), settings = settings, isContentLoading = false),
                    {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Protected pass").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Protected pass information blurred").assertIsDisplayed()
    }

    @Test
    fun lockAllCanMovePassesBehindOneUnlockableSection() {
        val actions = mutableListOf<HomeAction>()
        val settings = AppSettings(lockAllPasses = true, separateProtectedPasses = true)
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(
                    MainUiState(
                        passes = listOf(pass("one", "Boarding pass", PassType.BOARDING)),
                        settings = settings,
                        isContentLoading = false,
                    ),
                    actions::add,
                )
            }
        }

        composeRule.onNodeWithText("Boarding pass").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Unlock protected passes").performClick()
        assertThat(actions).containsExactly(HomeAction.UnlockProtectedPasses)
    }

    @Test
    fun unlockedProtectedSectionShowsItsPassesFreely() {
        val settings = AppSettings(lockAllPasses = true, separateProtectedPasses = true)
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(
                    MainUiState(
                        passes = listOf(pass("one", "Boarding pass", PassType.BOARDING)),
                        settings = settings,
                        isContentLoading = false,
                    ),
                    {},
                    protectedPassesUnlocked = true,
                )
            }
        }

        composeRule.onNodeWithText("Protected passes").assertIsDisplayed()
        composeRule.onNodeWithText("Boarding pass").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Protected pass information blurred").assertDoesNotExist()
    }

    @Test
    fun inactiveDateSortSelectsNewestBeforeItCanToggleToOldest() {
        val actions = mutableListOf<HomeAction>()
        val state = sampleState().copy(settings = AppSettings(sortOrder = PassSortOrder.TYPE))
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, actions::add) }
        }

        composeRule.onNodeWithText("Newest").performClick()

        assertThat(actions).containsExactly(HomeAction.SetSortOrder(PassSortOrder.DATE_DESC))
    }

    @Test
    fun activeDateSortTogglesBetweenNewestAndOldest() {
        val actions = mutableListOf<HomeAction>()
        val state = sampleState().copy(settings = AppSettings(sortOrder = PassSortOrder.DATE_ASC))
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, actions::add) }
        }

        composeRule.onNodeWithText("Oldest").performClick()

        assertThat(actions).containsExactly(HomeAction.SetSortOrder(PassSortOrder.DATE_DESC))
    }

    @Test
    fun manualOrderIsHiddenUntilManualSortingIsActive() {
        val state = sampleState().copy(settings = AppSettings(sortOrder = PassSortOrder.TYPE))
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, {}) }
        }

        composeRule.onNodeWithText("Manual").assertDoesNotExist()
    }

    @Test
    fun manualOrderAppearsBeforeTheDateSortControl() {
        val state = sampleState().copy(settings = AppSettings(sortOrder = PassSortOrder.MANUAL))
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, {}) }
        }

        val manualLeft = composeRule.onNodeWithText("Manual").fetchSemanticsNode().boundsInRoot.left
        val newestLeft = composeRule.onNodeWithText("Newest").fetchSemanticsNode().boundsInRoot.left
        assertThat(manualLeft).isLessThan(newestLeft)
    }

    @Test
    fun passWithoutArtworkUsesTheCardArtworkPlaceholder() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(sampleState(), {}) }
        }

        composeRule.onAllNodesWithContentDescription("Pass artwork").assertCountEquals(2)
    }

    @Test
    fun homeDrawerShowsDynamicTagsAndKeepsCustomizationInSettings() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(sampleState(), {}) }
        }

        composeRule.onNodeWithContentDescription("Navigation menu").performClick()

        composeRule.onNodeWithText("Pass view").assertDoesNotExist()
        composeRule.onNodeWithText("Home cards").assertDoesNotExist()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun categorySettingsExposeColorsAndManagementActions() {
        val customTag = org.ligi.passandroid.repository.PassCategory("travel", "Travel", 0xFF6750A4, org.ligi.passandroid.repository.PassCategoryRole.CUSTOM)
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { CategorySettingsScreen(listOf(customTag), {}) }
        }

        composeRule.onNodeWithText("Inbox").assertDoesNotExist()
        composeRule.onNodeWithText("Trash").assertDoesNotExist()
        composeRule.onNodeWithText("Travel").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add tag").assertIsDisplayed()
    }

    @Test
    fun homeCardCustomizationUsesControlsWithoutVisibilityNarration() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                HomeCardLayoutSettingsScreen(HomeCardSection.entries, setOf(HomeCardSection.CREATOR), {})
            }
        }

        composeRule.onNodeWithText("Shown").assertDoesNotExist()
        composeRule.onNodeWithText("Hidden").assertDoesNotExist()
        composeRule.onNodeWithText("Creator is hidden by default", substring = true).assertDoesNotExist()
    }

    @Test
    fun passDetailCustomizationUsesControlsWithoutVisibilityNarration() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassDetailLayoutSettingsScreen(PassDetailSection.entries, emptySet(), {})
            }
        }
        composeRule.onNodeWithText("Shown").assertDoesNotExist()
        composeRule.onNodeWithText("Hidden").assertDoesNotExist()
    }

    @Test
    fun passActionsExposeDeleteWithoutTrash() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassDetailScreen(pass("one", "Boarding pass", PassType.BOARDING), onAction = {})
            }
        }

        composeRule.onNodeWithContentDescription("Pass actions").performClick()
        composeRule.onNodeWithText("Delete permanently").assertDoesNotExist()
        composeRule.onNodeWithText("Move to Trash").assertDoesNotExist()
        composeRule.onNodeWithText("Delete pass").assertIsDisplayed()
    }

    @Test
    fun passTagMenuSupportsMultipleCustomTagsWithoutExposingInbox() {
        val actions = mutableListOf<PassDetailAction>()
        val taggedPass = pass("one", "Boarding pass", PassType.BOARDING).copy(tagIds = setOf("travel"))
        val tags = listOf(
            org.ligi.passandroid.repository.PassCategory("travel", "Travel", 0xFF6750A4, org.ligi.passandroid.repository.PassCategoryRole.CUSTOM),
            org.ligi.passandroid.repository.PassCategory("work", "Work", 0xFF3F51B5, org.ligi.passandroid.repository.PassCategoryRole.CUSTOM),
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassDetailScreen(taggedPass, categories = tags, onAction = actions::add)
            }
        }

        composeRule.onNodeWithContentDescription("Pass actions").performClick()
        composeRule.onNodeWithText("Manage tags").performClick()
        composeRule.onNodeWithText("Inbox").assertDoesNotExist()
        composeRule.onNodeWithText("Work").performClick()

        assertThat(actions).contains(PassDetailAction.SetTags(setOf("travel", "work")))
    }

    @Test
    fun passTagMenuAlwaysOffersTagCreation() {
        val actions = mutableListOf<PassDetailAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassDetailScreen(pass("one", "Boarding pass", PassType.BOARDING), onAction = actions::add)
            }
        }

        composeRule.onNodeWithContentDescription("Pass actions").performClick()
        composeRule.onNodeWithText("Manage tags").performClick()
        composeRule.onNodeWithText("Add new tag").performClick()

        assertThat(actions).contains(PassDetailAction.OpenTagSettings)
    }

    @Test
    fun passActionsOpenPassCustomization() {
        val actions = mutableListOf<PassDetailAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassDetailScreen(pass("one", "Boarding pass", PassType.BOARDING), onAction = actions::add)
            }
        }

        composeRule.onNodeWithContentDescription("Pass actions").performClick()
        composeRule.onNodeWithText("Customize pass").performClick()

        assertThat(actions).contains(PassDetailAction.OpenPassCustomization)
    }

    @Test
    fun passCustomizationSelectsAnEmbeddedImage() {
        val actions = mutableListOf<PassCustomizationAction>()
        val pass = pass("one", "Boarding pass", PassType.BOARDING).copy(
            artwork = listOf(PassArtworkUiModel(PassArtworkKind.LOGO, byteArrayOf(1))),
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassCustomizationScreen(pass, actions::add) }
        }

        composeRule.onNodeWithText("Automatic").assertIsDisplayed()
        composeRule.onNodeWithText("Logo").performClick()

        assertThat(actions).contains(PassCustomizationAction.SelectArtwork(PassArtworkKind.LOGO))
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
        isContentLoading = false,
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
