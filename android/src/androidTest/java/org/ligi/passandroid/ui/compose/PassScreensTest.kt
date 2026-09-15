package org.ligi.passandroid.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.defaultPassCategories
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
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
import org.ligi.passandroid.ui.state.PROTECTED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.TRASHED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction
import org.ligi.passandroid.ui.state.SettingsAction
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
    fun trashViewReplacesTheImportActionWithEmptyTrash() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(trashState(), {}) }
        }

        composeRule.onNodeWithContentDescription("Import passes").assertDoesNotExist()
        composeRule.onNodeWithTag("empty_trash_fab").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Empty trash").assertIsDisplayed()
    }

    @Test
    fun emptyTrashHidesTheFloatingAction() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(
                    MainUiState(selectedCategoryId = TRASHED_PASSES_CATEGORY_ID, isContentLoading = false),
                    {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Import passes").assertDoesNotExist()
        composeRule.onNodeWithTag("empty_trash_fab").assertDoesNotExist()
    }

    @Test
    fun emptyTrashFabConfirmsBeforeDispatching() {
        val actions = mutableListOf<HomeAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(trashState(), actions::add) }
        }

        composeRule.onNodeWithTag("empty_trash_fab").performClick()
        composeRule.onNodeWithText("Empty the trash?").assertIsDisplayed()
        composeRule.onNode(hasText("Empty trash") and hasAnyAncestor(isDialog())).performClick()

        assertThat(actions).containsExactly(HomeAction.EmptyTrash)
    }

    @Test
    fun trashCardActionsDispatchRestoreAndDeleteForever() {
        val actions = mutableListOf<HomeAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(trashState(), actions::add) }
        }

        composeRule.onNodeWithContentDescription("Restore").performClick()
        assertThat(actions).containsExactly(HomeAction.RestoreFromTrash("one"))

        composeRule.onNodeWithContentDescription("Delete forever").performClick()
        composeRule.onNodeWithText("Delete this pass forever?").assertIsDisplayed()
    }

    @Test
    fun settingsExposeThemeAndAccessibilityOptions() {
        val settings = AppSettings(themeMode = ThemeMode.DARK, dynamicColors = false)
        composeRule.setContent {
            PassTheme(settings.themeMode, settings.amoledBlackBackground, dynamicColors = false) {
                SettingsScreen(settings, onAction = {})
            }
        }

        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Accent color").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Use AMOLED black background").assertIsDisplayed()
        composeRule.onNodeWithText("Use max brightness for codes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun accentColorRowCapturesSelectionAction() {
        val actions = mutableListOf<SettingsAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT, dynamicColors = false) {
                SettingsScreen(AppSettings(dynamicColors = false), onAction = actions::add)
            }
        }

        composeRule.onNodeWithContentDescription("#FF48C9B0").performClick()

        assertThat(actions).containsExactly(
            SettingsAction.SetAccentColor(0xFF48C9B0L),
        )
    }

    @Test
    fun accentAndStyleRowsRenderWhenDynamicColorsAreOff() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT, dynamicColors = false) {
                SettingsScreen(AppSettings(dynamicColors = false), onAction = {})
            }
        }

        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Accent color").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Color style").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun colorStyleRowCapturesSelectionAction() {
        val actions = mutableListOf<SettingsAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT, dynamicColors = false) {
                SettingsScreen(AppSettings(dynamicColors = false), onAction = actions::add)
            }
        }

        composeRule.onNodeWithContentDescription("Vibrant").performScrollTo().performClick()

        assertThat(actions).containsExactly(
            SettingsAction.SetColorStyle(org.ligi.passandroid.repository.ColorStyle.VIBRANT),
        )
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
        composeRule.onNodeWithText("Keep all protected passes in a locked section").assertIsDisplayed()
    }

    @Test
    fun settingsExposeNotificationPolicyOptions() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { SettingsScreen(AppSettings(remindersEnabled = true), onAction = {}) }
        }

        composeRule.onNodeWithTag("settings_list").performScrollToIndex(5)
        composeRule.onNodeWithText("Exact reminder timing").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Notification actions").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Lock screen").assertDoesNotExist()
    }

    @Test
    fun settingsAboutShowsBugReportAction() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { SettingsScreen(AppSettings(), onAction = {}) }
        }

        composeRule.onNodeWithTag("settings_list").performScrollToIndex(6)
        composeRule.onNodeWithText("Report a bug").assertIsDisplayed()
    }

    @Test
    fun bugReportDialogOffersGitHubAndEmail() {
        val actions = mutableListOf<SettingsAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { SettingsScreen(AppSettings(), onAction = actions::add) }
        }

        composeRule.onNodeWithTag("settings_list").performScrollToIndex(6)
        composeRule.onNodeWithText("Report a bug").performClick()
        composeRule.onNodeWithText("GitHub issue").assertIsDisplayed().performClick()

        assertThat(actions).containsExactly(SettingsAction.OpenBugReportGitHub)
    }

    @Test
    fun settingsScrollsToRemindersWhenRequested() {
        var consumed = false
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                SettingsScreen(
                    AppSettings(),
                    scrollToNotifications = true,
                    onNotificationScrollConsumed = { consumed = true },
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("Reminders").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 2_000) { consumed }
    }

    @Test
    fun drawerClosesOnScrimTapAfterReopening() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(sampleState(), {}) }
        }

        fun openDrawer() = composeRule.onNodeWithContentDescription("Navigation menu").performClick()
        fun tapScrim() = composeRule.onRoot().performTouchInput {
            click(Offset(width - 24f, height / 2f))
        }

        openDrawer()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        tapScrim()
        composeRule.onNodeWithText("Settings").assertIsNotDisplayed()

        openDrawer()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        tapScrim()
        composeRule.onNodeWithText("Settings").assertIsNotDisplayed()
    }

    @Test
    fun drawerClosesOnBackAfterReopening() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(sampleState(), {}) }
        }

        fun openDrawer() = composeRule.onNodeWithContentDescription("Navigation menu").performClick()
        fun pressBack() = composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        openDrawer()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("Settings").assertIsNotDisplayed()

        openDrawer()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("Settings").assertIsNotDisplayed()
    }

    @Test
    fun drawerClosesOnScrimTapAfterScrollingTheList() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(sampleState(), {}) }
        }

        composeRule.onNodeWithTag("pass_card_one").performTouchInput { swipeUp() }
        composeRule.onNodeWithContentDescription("Navigation menu").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { click(Offset(width - 24f, height / 2f)) }
        composeRule.onNodeWithText("Settings").assertIsNotDisplayed()
    }

    @Test
    fun eventAccessIsDisabledUntilRemindersAreEnabled() {
        val actions = mutableListOf<SettingsAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { SettingsScreen(AppSettings(remindersEnabled = false), onAction = actions::add) }
        }

        composeRule.onNodeWithTag("settings_list").performScrollToIndex(5)
        composeRule.onNodeWithText("30 minutes").performScrollTo().assertIsNotEnabled().performClick()

        assertThat(actions).isEmpty()
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
                        selectedCategoryId = PROTECTED_PASSES_CATEGORY_ID,
                        isContentLoading = false,
                    ),
                    {},
                    protectedPassesUnlocked = true,
                )
            }
        }

        composeRule.onNodeWithText("Boarding pass").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Protected pass information blurred").assertDoesNotExist()
    }

    @Test
    fun separatedProtectedPassesStayOutOfAllAfterUnlock() {
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

        composeRule.onNodeWithText("Boarding pass").assertDoesNotExist()
        composeRule.onNodeWithText("Protected passes").assertDoesNotExist()
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
    fun creatorAndPassTypeShareALineWhenAdjacent() {
        val state = sampleState().copy(
            settings = AppSettings(
                homeCardSectionOrder = listOf(
                    HomeCardSection.ARTWORK,
                    HomeCardSection.TITLE,
                    HomeCardSection.CREATOR,
                    HomeCardSection.PASS_TYPE,
                    HomeCardSection.DATE,
                    HomeCardSection.PRIMARY_FIELD,
                    HomeCardSection.CATEGORY,
                ),
                hiddenHomeCardSections = emptySet(),
            ),
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, {}) }
        }

        composeRule.onNodeWithText("Example issuer • Boarding").assertIsDisplayed()
    }

    @Test
    fun creatorAndPassTypeStaySeparateWhenAnotherSectionIsBetween() {
        val state = sampleState().copy(
            passes = listOf(pass("one", "Boarding pass", PassType.BOARDING)),
            settings = AppSettings(
                homeCardSectionOrder = listOf(
                    HomeCardSection.ARTWORK,
                    HomeCardSection.TITLE,
                    HomeCardSection.CREATOR,
                    HomeCardSection.DATE,
                    HomeCardSection.PASS_TYPE,
                    HomeCardSection.PRIMARY_FIELD,
                    HomeCardSection.CATEGORY,
                ),
                hiddenHomeCardSections = emptySet(),
            ),
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, {}) }
        }

        composeRule.onNodeWithText("Example issuer • Boarding").assertDoesNotExist()
        composeRule.onNodeWithText("Example issuer").assertIsDisplayed()
        composeRule.onNodeWithText("Boarding").assertIsDisplayed()
    }

    @Test
    fun homeCardShowsTagNamesWhenTheyFit() {
        val tags = listOf(
            PassCategory("alpha", "Alpha", 0xFF1565C0, PassCategoryRole.CUSTOM),
            PassCategory("beta", "Beta", 0xFF7B1FA2, PassCategoryRole.CUSTOM),
        )
        val taggedPass = pass("one", "Boarding pass", PassType.BOARDING).copy(tagIds = setOf("alpha", "beta"))
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassHomeScreen(
                    MainUiState(passes = listOf(taggedPass), categories = defaultPassCategories + tags, isContentLoading = false),
                    {},
                )
            }
        }

        composeRule.onNode(hasText("Alpha") and hasAnyAncestor(hasTestTag("pass_card_one"))).assertIsDisplayed()
        composeRule.onNode(hasText("Beta") and hasAnyAncestor(hasTestTag("pass_card_one"))).assertIsDisplayed()
    }

    @Test
    fun homeCardUsesTagDotsWhenNamesOverflow() {
        val simpleTag = PassCategory("simple", "Simple", 0xFF006C4C, PassCategoryRole.CUSTOM)
        val manyTags = (1..12).map { index ->
            PassCategory("tag$index", "Very long tag name number $index", 0xFF1565C0 + index, PassCategoryRole.CUSTOM)
        }
        val state = MainUiState(
            passes = listOf(
                pass("one", "Simple pass", PassType.BOARDING).copy(tagIds = setOf(simpleTag.id)),
                pass("two", "Tagged pass", PassType.BOARDING).copy(tagIds = manyTags.mapTo(mutableSetOf(), PassCategory::id)),
            ),
            categories = defaultPassCategories + manyTags + simpleTag,
            isContentLoading = false,
        )
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassHomeScreen(state, {}) }
        }

        composeRule.onNode(hasText("Very long tag name number 1") and hasAnyAncestor(hasTestTag("pass_card_two"))).assertDoesNotExist()
        composeRule.onNode(hasText("+6") and hasAnyAncestor(hasTestTag("pass_card_two"))).assertIsDisplayed()
        val simpleHeight = composeRule.onNodeWithTag("pass_card_one").fetchSemanticsNode().size.height
        val overflowHeight = composeRule.onNodeWithTag("pass_card_two").fetchSemanticsNode().size.height
        assertThat(overflowHeight).isLessThan(simpleHeight * 2)
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

    @Test
    fun notesSectionStaysHiddenWhenThePassHasNoNote() {
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassDetailScreen(pass("one", "Boarding pass", PassType.BOARDING), onAction = {})
            }
        }

        composeRule.onNodeWithText("Note").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Pass actions").performClick()
        composeRule.onNodeWithText("Edit note").assertDoesNotExist()
        composeRule.onNodeWithText("Remove note").assertDoesNotExist()
        composeRule.onNodeWithText("Add note").assertIsDisplayed()
    }

    @Test
    fun notesSectionShowsTheNoteAndEditsItThroughTheDialog() {
        val actions = mutableListOf<PassDetailAction>()
        val notedPass = pass("one", "Boarding pass", PassType.BOARDING).copy(notes = "Gate opens at 6")
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassDetailScreen(notedPass, onAction = actions::add) }
        }

        composeRule.onNodeWithText("Note").assertIsDisplayed()
        composeRule.onNodeWithText("Gate opens at 6").performClick()
        composeRule.onNodeWithTag("note_draft").performTextInput(", row 1")
        composeRule.onNodeWithText("Save note").performClick()

        assertThat(actions).containsExactly(PassDetailAction.SetNotes("Gate opens at 6, row 1"))
    }

    @Test
    fun notesOverflowCanRemoveTheNote() {
        val actions = mutableListOf<PassDetailAction>()
        val notedPass = pass("one", "Boarding pass", PassType.BOARDING).copy(notes = "Gate opens at 6")
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) { PassDetailScreen(notedPass, onAction = actions::add) }
        }

        composeRule.onNodeWithContentDescription("Pass actions").performClick()
        composeRule.onNodeWithText("Remove note").performClick()

        assertThat(actions).containsExactly(PassDetailAction.SetNotes(""))
    }

    @Test
    fun passDetailLayoutListsNotesWithAWorkingSwitch() {
        val actions = mutableListOf<PassDetailLayoutSettingsAction>()
        composeRule.setContent {
            PassTheme(ThemeMode.LIGHT) {
                PassDetailLayoutSettingsScreen(PassDetailSection.entries, emptySet(), actions::add)
            }
        }

        composeRule.onNodeWithText("Notes").assertIsDisplayed()
        composeRule.onAllNodes(isToggleable())[PassDetailSection.entries.indexOf(PassDetailSection.NOTES)].performClick()

        assertThat(actions).containsExactly(PassDetailLayoutSettingsAction.SetVisible(PassDetailSection.NOTES, false))
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

    private fun trashState() = MainUiState(
        trashedPasses = listOf(pass("one", "Boarding pass", PassType.BOARDING)),
        selectedCategoryId = TRASHED_PASSES_CATEGORY_ID,
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
