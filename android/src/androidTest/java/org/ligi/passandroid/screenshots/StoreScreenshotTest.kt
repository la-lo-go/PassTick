package org.ligi.passandroid.screenshots

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.functions.CalendarEvent
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.defaultPassCategories
import org.ligi.passandroid.repository.recommendedPassTags
import org.ligi.passandroid.ui.compose.CategorySettingsScreen
import org.ligi.passandroid.ui.compose.EditPassScreen
import org.ligi.passandroid.ui.compose.ExportImageScreen
import org.ligi.passandroid.ui.compose.PassDetailScreen
import org.ligi.passandroid.ui.compose.PassHomeScreen
import org.ligi.passandroid.ui.compose.SettingsScreen
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassArtworkUiModel
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationUiModel
import org.ligi.passandroid.ui.state.PassTimeSpanUiModel
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.theme.PassTheme
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassImageExportOptions
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.io.File

/**
 * Renders the showcase state on a real device and writes PNG captures for the store listing.
 * Run this class with the capture script in tools/store-assets, then pull the files from the
 * app external files directory.
 */
class StoreScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext = InstrumentationRegistry.getInstrumentation().context

    private val artworkCache = mutableMapOf<String, ByteArray>()

    @Test
    fun homeShowsTodaysPasses() {
        setBrandContent {
            PassHomeScreen(showcaseState(), onAction = {})
        }

        capture("home-today")
    }

    @Test
    fun homeShowsPinnedTaggedAndProtectedPasses() {
        setBrandContent {
            PassHomeScreen(showcaseState(highlightToday = false), onAction = {}, showTodayHero = false)
        }

        capture("home-all")
    }

    @Test
    fun passDetailShowsArtworkCodeAndActions() {
        setBrandContent {
            PassDetailScreen(
                pass = coastalExpressPass(),
                categories = defaultPassCategories,
                flashlightAvailable = true,
                calendarEventPresent = true,
                onAction = {},
            )
        }

        capture("pass-detail")
    }

    @Test
    fun exportImageShowsPreviewAndOptions() {
        composeRule.setContent {
            PassTheme(ThemeMode.DARK, dynamicColors = false, accentColor = CORAL) {
                ExportImageScreen(
                    pass = coastalExpressPass(),
                    options = PassImageExportOptions(),
                    isBusy = false,
                    onAction = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription("Export preview").fetchSemanticsNodes().isNotEmpty()
        }
        capture("export-image")
    }

    @Test
    fun settingsShowBrandAppearance() {
        setBrandContent {
            SettingsScreen(showcaseSettings(), onAction = {})
        }

        capture("settings-appearance")
    }

    @Test
    fun settingsShowPrivacyOptions() {
        setBrandContent {
            SettingsScreen(showcaseSettings(), onAction = {})
        }

        composeRule.onNodeWithTag("settings_list").performScrollToIndex(4)
        capture("settings-privacy")
    }

    @Test
    fun editPassShowsEveryEditableDetail() {
        setBrandContent(themeMode = ThemeMode.LIGHT) {
            EditPassScreen(coastalExpressPass(), onAction = {})
        }

        capture("edit-pass")
    }

    @Test
    fun tagsScreenShowsColoredTags() {
        setBrandContent {
            CategorySettingsScreen(recommendedPassTags.take(6), onAction = {})
        }

        capture("tags")
    }

    private fun setBrandContent(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
        composeRule.setContent {
            PassTheme(themeMode, dynamicColors = false, accentColor = CORAL) { content() }
        }
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(context.getExternalFilesDir(null), "screenshots")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create $directory" }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun showcaseState(highlightToday: Boolean = true) = MainUiState(
        passes = showcasePasses(),
        settings = showcaseSettings(highlightToday),
        categories = defaultPassCategories,
        isContentLoading = false,
    )

    private fun showcaseSettings(highlightToday: Boolean = true) = AppSettings(
        themeMode = ThemeMode.DARK,
        dynamicColors = false,
        accentColor = CORAL,
        highlightTodayPasses = highlightToday,
        blurProtectedPassCards = true,
        showProtectedPassLockIcon = true,
    )

    private fun showcasePasses() = listOf(
        coastalExpressPass(),
        lumenLivePass(),
        glassHorizonsPass(),
        skylineFitnessPass(),
        auroraAirPass(),
        bellaNapoliPass(),
        solFestivalPass(),
    )

    private fun coastalExpressPass() = pass(
        id = "coastal-express",
        description = "Coastal Express",
        creator = "Iberian Rail",
        type = PassType.BOARDING,
        accentColor = 0xFF1F4E79.toInt(),
        format = PassBarCodeFormat.QR_CODE,
        message = "IR2609BCNVLC004812",
        alternativeText = "0048 12",
        fields = listOf(
            field("route", "Route", "Barcelona → Valencia", hint = "primaryFields"),
            field("departure", "Departure", "17:00", hint = "secondaryFields"),
            field("coach", "Coach", "3", hint = "secondaryFields"),
            field("seat", "Seat", "12A", hidden = true, hint = "backFields"),
        ),
        locations = listOf(PassLocationUiModel("Barcelona Sants", 41.3792, 2.1400)),
        timeSpan = todayAt(17, 0) to todayAt(19, 48),
        artworkName = "coastal-express",
        tagIds = setOf("travel", "work"),
        notes = "Platform 7, coach 3, seat 12A",
    )

    private fun lumenLivePass() = pass(
        id = "lumen-live",
        description = "Lumen Live",
        creator = "Lumen",
        type = PassType.EVENT,
        accentColor = 0xFF5B2C87.toInt(),
        format = PassBarCodeFormat.QR_CODE,
        message = "LUMEN-LIVE-GA-4417",
        alternativeText = "Floor B",
        fields = listOf(
            field("admission", "Admission", "Floor B", hint = "primaryFields"),
            field("doors", "Doors", "19:45", hint = "secondaryFields"),
            field("entry", "Entry", "East", hint = "secondaryFields"),
        ),
        locations = listOf(PassLocationUiModel("Lumen Arena", 41.3802, 2.1220)),
        timeSpan = todayAt(20, 30) to todayAt(23, 30),
        artworkName = "lumen-live",
        tagIds = setOf("music", "events"),
    )

    private fun glassHorizonsPass() = pass(
        id = "glass-horizons",
        description = "Glass Horizons",
        creator = "Museum of Light",
        type = PassType.EVENT,
        accentColor = 0xFF00696E.toInt(),
        format = PassBarCodeFormat.PDF_417,
        message = "GLASS26|ADULT|77120493",
        alternativeText = "Adult",
        fields = listOf(
            field("admission", "Admission", "Adult", hint = "primaryFields"),
            field("entry", "Entry time", "11:00", hint = "secondaryFields"),
            field("gallery", "Gallery", "Atrium 2", hint = "secondaryFields"),
        ),
        locations = listOf(PassLocationUiModel("Museum of Light", 41.3875, 2.1150)),
        timeSpan = todayAt(11, 0) to todayAt(18, 0),
        artworkName = "glass-horizons",
        tagIds = setOf("events"),
    )

    private fun skylineFitnessPass() = pass(
        id = "skyline-fitness",
        description = "Skyline Fitness",
        creator = "Skyline",
        type = PassType.LOYALTY,
        accentColor = 0xFF006C4C.toInt(),
        format = PassBarCodeFormat.CODE_128,
        message = "SKYLINE-88231",
        alternativeText = "88231",
        fields = listOf(
            field("member", "Member", "Gold", hint = "primaryFields"),
            field("until", "Valid until", "December 2026", hint = "secondaryFields"),
        ),
        tagIds = setOf("health", "loyalty"),
        favorite = true,
    )

    private fun auroraAirPass() = pass(
        id = "aurora-air",
        description = "Aurora Air",
        creator = "Aurora",
        type = PassType.BOARDING,
        accentColor = 0xFF1565C0.toInt(),
        format = PassBarCodeFormat.AZTEC,
        message = "M1SMITH/JANE EABC123 MADCPH AU 0214 212Y014A0002",
        alternativeText = "ABC123",
        fields = listOf(
            field("route", "Route", "Madrid → Copenhagen", hint = "primaryFields"),
            field("gate", "Gate", "J42", hint = "secondaryFields"),
            field("boarding", "Boarding", "07:35", hint = "secondaryFields"),
        ),
        locations = listOf(PassLocationUiModel("Adolfo Suárez Madrid–Barajas", 40.4983, -3.5676)),
        timeSpan = daysFromNow(7, 7, 35) to daysFromNow(7, 10, 20),
        tagIds = setOf("travel", "work"),
        protected = true,
    )

    private fun bellaNapoliPass() = pass(
        id = "bella-napoli",
        description = "Bella Napoli",
        creator = "Bella Napoli",
        type = PassType.COUPON,
        accentColor = 0xFFD84315.toInt(),
        format = PassBarCodeFormat.CODE_128,
        message = "BELLA-2X1-77219",
        alternativeText = "77219",
        fields = listOf(
            field("offer", "Offer", "2 for 1", hint = "primaryFields"),
            field("until", "Valid until", "September 30", hint = "secondaryFields"),
        ),
        tagIds = setOf("food"),
    )

    private fun solFestivalPass() = pass(
        id = "sol-festival",
        description = "Sol Festival",
        creator = "Sol Festival",
        type = PassType.EVENT,
        accentColor = 0xFF9A4522.toInt(),
        format = PassBarCodeFormat.PDF_417,
        message = "SOL26|WEEKEND|GA|8792310042",
        alternativeText = "Weekend",
        fields = listOf(
            field("access", "Access", "Weekend", hint = "primaryFields"),
            field("zone", "Zone", "General admission", hint = "secondaryFields"),
        ),
        locations = listOf(PassLocationUiModel("Parque del Río", 40.4111, -3.7221)),
        timeSpan = daysFromNow(30, 16, 0) to daysFromNow(32, 2, 0),
        tagIds = setOf("music", "events"),
    )

    private fun pass(
        id: String,
        description: String,
        creator: String,
        type: PassType,
        accentColor: Int,
        format: PassBarCodeFormat?,
        message: String?,
        alternativeText: String?,
        fields: List<PassFieldUiModel> = emptyList(),
        locations: List<PassLocationUiModel> = emptyList(),
        timeSpan: Pair<ZonedDateTime, ZonedDateTime>? = null,
        artworkName: String? = null,
        tagIds: Set<String> = emptySet(),
        notes: String = "",
        protected: Boolean = false,
        favorite: Boolean = false,
    ) = PassUiModel(
        id = id,
        description = description,
        creator = creator,
        type = type,
        accentColor = accentColor,
        barcodeFormat = format,
        barcodeMessage = message,
        barcodeAlternativeText = alternativeText,
        fields = fields,
        locations = locations,
        calendarEvent = timeSpan?.let { span ->
            CalendarEvent(
                title = description,
                beginTimeMillis = span.first.toEpochSecond() * 1000,
                endTimeMillis = span.second.toEpochSecond() * 1000,
                location = locations.firstOrNull()?.name,
                description = null,
            )
        },
        artwork = artworkName?.let {
            val bytes = artworkCache.getOrPut(it) { testContext.assets.open("showcase/$it.png").use { stream -> stream.readBytes() } }
            listOf(
                PassArtworkUiModel(PassArtworkKind.STRIP, bytes),
                PassArtworkUiModel(PassArtworkKind.THUMBNAIL, bytes),
            )
        }.orEmpty(),
        calendarTimeSpan = timeSpan?.let { PassTimeSpanUiModel(it.first, it.second) },
        tagIds = tagIds,
        notes = notes,
        isProtected = protected,
        isFavorite = favorite,
    )

    private fun field(
        key: String,
        label: String,
        value: String,
        hidden: Boolean = false,
        hint: String? = null,
    ) = PassFieldUiModel(key, label, value, hidden, hint)

    private fun todayAt(hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.now(ZoneId.systemDefault()).withHour(hour).withMinute(minute).withSecond(0).withNano(0)

    private fun daysFromNow(days: Long, hour: Int, minute: Int): ZonedDateTime =
        todayAt(hour, minute).plusDays(days)

    private companion object {
        const val CORAL = 0xFFFF6249L
    }
}
