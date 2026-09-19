package org.ligi.passandroid.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.domain.timeline.buildPassTimeline
import org.ligi.passandroid.functions.CalendarEvent
import org.ligi.passandroid.functions.generateBarCodeBitmap
import org.ligi.passandroid.imports.ImportSource
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ColorStyle
import org.ligi.passandroid.repository.PassArtworkSnapshot
import org.ligi.passandroid.repository.PassFieldSnapshot
import org.ligi.passandroid.repository.PassLocationSnapshot
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassTimeSpanSnapshot
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.defaultPassCategories
import org.ligi.passandroid.ui.compose.EditPassScreen
import org.ligi.passandroid.ui.compose.ExportImageScreen
import org.ligi.passandroid.ui.compose.PassDetailScreen
import org.ligi.passandroid.ui.compose.PassDocumentPages
import org.ligi.passandroid.ui.compose.PassHomeScreen
import org.ligi.passandroid.ui.compose.SettingsScreen
import org.ligi.passandroid.ui.compose.TimelineScreen
import org.ligi.passandroid.ui.compose.TimelineUiState
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassArtworkUiModel
import org.ligi.passandroid.ui.state.PassBarcodeUiModel
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationUiModel
import org.ligi.passandroid.ui.state.PassTimeSpanUiModel
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.theme.PassTheme
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassImageExportOptions
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

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

    @Before
    fun hideNavigationBar() {
        composeRule.activityRule.scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    @Test
    fun heroShowsLightPassWithCode() {
        setBrandContent(themeMode = ThemeMode.LIGHT) {
            PassDetailScreen(
                pass = heroDayPasses().first(),
                categories = defaultPassCategories,
                flashlightAvailable = true,
                calendarEventPresent = true,
                onAction = {},
            )
        }

        capture("hero-day")
    }

    @Test
    fun heroShowsNightHome() {
        setBrandContent {
            PassHomeScreen(heroState(heroNightPasses()), onAction = {})
        }

        capture("hero-night")
    }

    @Test
    fun importPassShowsEveryDetectedCode() {
        setBrandContent {
            PassDetailScreen(
                pass = importedDocumentPass(),
                categories = defaultPassCategories,
                flashlightAvailable = true,
                calendarEventPresent = false,
                documentPages = PassDocumentPages { _, _, _ -> importPagePng() },
                onAction = {},
            )
        }

        val codeBadge = hasText("Code 1 of 3")
        val scroller = composeRule.onNode(hasScrollAction() and hasAnyDescendant(codeBadge))
        scroller.performScrollToNode(codeBadge)
        scroller.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, 350f) }
        capture("import-detail")
    }

    @Test
    fun timelineShowsUpcomingEvents() {
        val passes = showcasePasses()
        setBrandContent {
            TimelineScreen(
                state = TimelineUiState(
                    timeline = buildPassTimeline(passes.map { it.toSnapshot() }, Instant.now(), ZONE),
                    passCardTitles = passes.associate { it.id to it.description },
                ),
                onAction = {},
            )
        }

        composeRule.onNodeWithContentDescription("Agenda view").performClick()
        val todayLabel = LocalDate.now(ZONE).format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
        composeRule.onNodeWithContentDescription(todayLabel, substring = true).performClick()
        capture("timeline")
    }

    @Test
    fun exportImageShowsPreviewAndOptions() {
        composeRule.setContent {
            PassTheme(ThemeMode.DARK, dynamicColors = false, accentColor = CORAL, colorStyle = ColorStyle.FIDELITY) {
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
    fun themesShowMaterialYou() {
        var variant by mutableStateOf(ThemeVariant("coral", CORAL, ColorStyle.FIDELITY, ThemeMode.DARK))
        composeRule.setContent {
            PassTheme(variant.mode, dynamicColors = false, accentColor = variant.accent, colorStyle = variant.style) {
                PassHomeScreen(showcaseState(), onAction = {})
            }
        }

        themeVariants.forEach { themeVariant ->
            variant = themeVariant
            capture("theme-${themeVariant.name}")
        }
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

    private fun setBrandContent(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
        composeRule.setContent {
            PassTheme(themeMode, dynamicColors = false, accentColor = CORAL, colorStyle = ColorStyle.FIDELITY) { content() }
        }
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        // Let the system bar hide animation settle before the raster.
        Thread.sleep(400)
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(context.getExternalFilesDir(null), "screenshots")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create $directory" }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun showcaseState() = MainUiState(
        passes = showcasePasses(),
        settings = showcaseSettings(),
        categories = defaultPassCategories,
        isContentLoading = false,
    )

    private fun showcaseSettings() = AppSettings(
        themeMode = ThemeMode.DARK,
        dynamicColors = false,
        accentColor = CORAL,
        colorStyle = ColorStyle.FIDELITY,
        blurProtectedPassCards = true,
        showProtectedPassLockIcon = true,
    )

    private fun showcasePasses() = listOf(
        auroraAirPass(),
        coastalExpressPass(),
        lumenLivePass(),
        glassHorizonsPass(),
        skylineFitnessPass(),
        bellaNapoliPass(),
        solFestivalPass(),
    )

    private fun heroState(passes: List<PassUiModel>) = MainUiState(
        passes = passes,
        settings = showcaseSettings(),
        categories = defaultPassCategories,
        isContentLoading = false,
    )

    private fun heroDayPasses() = listOf(
        pass(
            id = "nordic-lines",
            description = "Nordic Lines",
            creator = "Nordic Ferries",
            type = PassType.BOARDING,
            accentColor = 0xFF0F6E8C.toInt(),
            format = PassBarCodeFormat.QR_CODE,
            message = "NL-OSLO-771204",
            alternativeText = "Deck 7",
            fields = listOf(
                field("route", "Route", "Copenhagen – Oslo", hint = "primaryFields"),
                field("departure", "Departure", "14:20", hint = "secondaryFields"),
                field("cabin", "Cabin", "Deck 7", hint = "secondaryFields"),
            ),
            locations = listOf(PassLocationUiModel("Copenhagen Terminal", 55.6984, 12.5994)),
            timeSpan = todayAt(14, 20) to todayAt(23, 45),
            artworkBytes = artworkBytes('N', 0xFF0F6E8C.toInt(), 0xFF79C7DC.toInt()),
            tagIds = setOf("travel"),
            favorite = true,
        ),
        pass(
            id = "veldt-festival",
            description = "Veldt Festival",
            creator = "Veldt",
            type = PassType.EVENT,
            accentColor = 0xFFD84315.toInt(),
            format = PassBarCodeFormat.PDF_417,
            message = "VELDT26|DAY2|GA|4412093",
            alternativeText = "Day 2",
            fields = listOf(
                field("admission", "Admission", "Day 2", hint = "primaryFields"),
                field("gate", "Gate", "North", hint = "secondaryFields"),
                field("doors", "Doors", "18:00", hint = "secondaryFields"),
            ),
            locations = listOf(PassLocationUiModel("Veldt Grounds", 41.3745, 2.1560)),
            timeSpan = daysFromNow(1, 18, 0) to daysFromNow(1, 23, 59),
            artworkBytes = artworkBytes('V', 0xFFD84315.toInt(), 0xFFF6A26B.toInt()),
            tagIds = setOf("music"),
        ),
        pass(
            id = "cafe-central",
            description = "Café Central",
            creator = "Café Central",
            type = PassType.LOYALTY,
            accentColor = 0xFFE07A00.toInt(),
            format = PassBarCodeFormat.CODE_128,
            message = "CENTRAL-4OF6-88231",
            alternativeText = "4 of 6",
            fields = listOf(
                field("stamps", "Stamps", "4 of 6", hint = "primaryFields"),
                field("reward", "Reward", "Free coffee", hint = "secondaryFields"),
            ),
            artworkBytes = artworkBytes('C', 0xFFE07A00.toInt(), 0xFFFFC46B.toInt()),
            tagIds = setOf("food"),
        ),
        pass(
            id = "city-bikes",
            description = "City Bikes",
            creator = "City Bikes",
            type = PassType.LOYALTY,
            accentColor = 0xFF006C4C.toInt(),
            format = PassBarCodeFormat.CODE_39,
            message = "BIKE-88231",
            alternativeText = "88231",
            fields = listOf(
                field("plan", "Plan", "Annual", hint = "primaryFields"),
                field("until", "Valid until", "March 2027", hint = "secondaryFields"),
            ),
            artworkBytes = artworkBytes('B', 0xFF006C4C.toInt(), 0xFF6FCF9B.toInt()),
            tagIds = setOf("health"),
        ),
    )

    private fun heroNightPasses() = listOf(
        pass(
            id = "aurora-nights",
            description = "Aurora Nights",
            creator = "Aurora",
            type = PassType.EVENT,
            accentColor = 0xFF7B1FA2.toInt(),
            format = PassBarCodeFormat.QR_CODE,
            message = "AURORA-NIGHT-GA-7712",
            alternativeText = "Floor 2",
            fields = listOf(
                field("admission", "Admission", "Floor 2", hint = "primaryFields"),
                field("doors", "Doors", "20:30", hint = "secondaryFields"),
            ),
            locations = listOf(PassLocationUiModel("Aurora Hall", 41.3901, 2.1650)),
            timeSpan = todayAt(21, 30) to todayAt(23, 30),
            artworkBytes = artworkBytes('A', 0xFF7B1FA2.toInt(), 0xFFCE93D8.toInt()),
            tagIds = setOf("music"),
            favorite = true,
        ),
        pass(
            id = "hotel-meridian",
            description = "Hotel Meridian",
            creator = "Meridian",
            type = PassType.LOYALTY,
            accentColor = 0xFF00696E.toInt(),
            format = PassBarCodeFormat.AZTEC,
            message = "MERIDIAN-ROOM-912",
            alternativeText = "Room 912",
            fields = listOf(
                field("room", "Room", "912", hint = "primaryFields"),
                field("checkin", "Check-in", "15:00", hint = "secondaryFields"),
            ),
            locations = listOf(PassLocationUiModel("Hotel Meridian", 41.3855, 2.1710)),
            timeSpan = daysFromNow(3, 15, 0) to daysFromNow(5, 12, 0),
            artworkBytes = artworkBytes('M', 0xFF00696E.toInt(), 0xFF7FD1D6.toInt()),
            tagIds = setOf("travel"),
        ),
        pass(
            id = "night-owl",
            description = "Night Owl Line",
            creator = "City Rail",
            type = PassType.BOARDING,
            accentColor = 0xFF1F4E79.toInt(),
            format = PassBarCodeFormat.CODE_128,
            message = "OWL-2300-PLATFORM4",
            alternativeText = "04",
            fields = listOf(
                field("route", "Route", "Aeroport – Estació de França", hint = "primaryFields"),
                field("departure", "Departure", "23:00", hint = "secondaryFields"),
            ),
            timeSpan = daysFromNow(2, 23, 0) to daysFromNow(3, 0, 15),
            artworkBytes = artworkBytes('O', 0xFF1F4E79.toInt(), 0xFF8AB6E0.toInt()),
            tagIds = setOf("travel"),
        ),
        pass(
            id = "bistro-verde",
            description = "Bistro Verde",
            creator = "Bistro Verde",
            type = PassType.COUPON,
            accentColor = 0xFFB3261E.toInt(),
            format = PassBarCodeFormat.EAN_13,
            message = "5901234123457",
            alternativeText = "Table 8",
            fields = listOf(
                field("offer", "Offer", "Dessert on the house", hint = "primaryFields"),
                field("until", "Valid until", "October 31", hint = "secondaryFields"),
            ),
            artworkBytes = artworkBytes('V', 0xFFB3261E.toInt(), 0xFFF2A8A3.toInt()),
            tagIds = setOf("food"),
        ),
    )

    private fun artworkBytes(letter: Char, startColor: Int, endColor: Int): ByteArray {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            startColor,
            endColor,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null
        paint.color = Color.argb(56, 255, 255, 255)
        canvas.drawCircle(size * 0.78f, size * 0.24f, size * 0.42f, paint)
        paint.color = Color.argb(40, 0, 0, 0)
        canvas.drawCircle(size * 0.18f, size * 0.84f, size * 0.38f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size * 0.46f
        val metrics = paint.fontMetrics
        canvas.drawText(letter.toString(), size / 2f, size / 2f - (metrics.ascent + metrics.descent) / 2f, paint)
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
    }

    private fun importedDocumentPass() = pass(
        id = "nordic-air-document",
        description = "Nordic Air boarding pass",
        creator = "Nordic Air",
        type = PassType.BOARDING,
        accentColor = 0xFF0F6E8C.toInt(),
        format = PassBarCodeFormat.QR_CODE,
        message = "NA214 CPH OSL 19SEP JDOE 12A",
        alternativeText = "Seat 12A",
        fields = listOf(
            field("route", "Route", "Copenhagen – Oslo", hint = "primaryFields"),
            field("boarding", "Boarding", "13:40", hint = "secondaryFields"),
            field("gate", "Gate", "J42", hint = "secondaryFields"),
            field("seat", "Seat", "12A", hidden = true, hint = "backFields"),
        ),
        locations = listOf(PassLocationUiModel("Copenhagen Airport", 55.6180, 12.6508)),
        timeSpan = todayAt(14, 20) to todayAt(15, 45),
        tagIds = setOf("travel"),
        importSource = ImportSource.PDF,
        hasDocument = true,
        documentPageCount = 2,
        barcodes = listOf(
            PassBarcodeUiModel(PassBarCodeFormat.QR_CODE, "NA214 CPH OSL 19SEP JDOE 12A", "Seat 12A"),
            PassBarcodeUiModel(PassBarCodeFormat.CODE_128, "NA214CPHOSL19SEPJDOE12A", null),
            PassBarcodeUiModel(PassBarCodeFormat.PDF_417, "NA|214|CPH|OSL|19SEP2026|1340|J42|12A", null),
        ),
    )

    private fun importPagePng(): ByteArray {
        val width = 1240
        val height = 1754
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ink = Color.rgb(28, 32, 38)
        val muted = Color.rgb(100, 106, 114)
        val hairline = Color.rgb(214, 219, 224)

        paint.color = Color.rgb(15, 110, 140)
        canvas.drawRect(0f, 0f, width.toFloat(), 190f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 54f
        canvas.drawText("NORDIC AIR", 80f, 100f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 32f
        canvas.drawText("BOARDING PASS", 80f, 152f, paint)

        val details = listOf(
            "PASSENGER" to "J. DOE", "SEAT" to "12A",
            "DATE" to "19 SEP 2026", "CLASS" to "ECONOMY",
            "BOARDING" to "13:40", "GATE" to "J42",
        )
        details.forEachIndexed { index, (label, value) ->
            val x = if (index % 2 == 0) 80f else 660f
            val y = 300f + (index / 2) * 130f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 26f
            paint.color = muted
            canvas.drawText(label, x, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 38f
            paint.color = ink
            canvas.drawText(value, x, y + 48f, paint)
        }

        paint.color = hairline
        canvas.drawRect(80f, 660f, 1160f, 663f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 84f
        paint.color = ink
        canvas.drawText("CPH", 80f, 790f, paint)
        canvas.drawText("OSL", 500f, 790f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 28f
        paint.color = muted
        canvas.drawText("COPENHAGEN", 80f, 842f, paint)
        canvas.drawText("OSLO", 500f, 842f, paint)
        drawArrow(canvas, 290f, 760f, 440f, ink)

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 26f
        canvas.drawText("FLIGHT", 1160f, 730f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 40f
        paint.color = ink
        canvas.drawText("NA 214", 1160f, 780f, paint)
        paint.textAlign = Paint.Align.LEFT

        paint.color = hairline
        canvas.drawRect(80f, 900f, 1160f, 903f, paint)

        drawBarcode(canvas, "NA214 CPH OSL 19SEP JDOE 12A", PassBarCodeFormat.QR_CODE, 80f, 970f, 360f, 1250f)
        drawBarcode(canvas, "NA214CPHOSL19SEPJDOE12A", PassBarCodeFormat.CODE_128, 430f, 1000f, 1100f, 1160f)
        drawBarcode(canvas, "NA|214|CPH|OSL|19SEP2026|1340|J42|12A", PassBarCodeFormat.PDF_417, 430f, 1220f, 1100f, 1390f)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 26f
        paint.color = muted
        canvas.drawText("BAGGAGE", 80f, 1450f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 34f
        paint.color = ink
        canvas.drawText("1 × 23 KG  ·  CABIN 1 × 8 KG", 80f, 1495f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 22f
        paint.color = muted
        canvas.drawText("Arrive at the gate at least 30 minutes before departure.", 80f, 1550f, paint)
        canvas.drawText("Nordic Air · CPH Airport · Ref NA214CPHOSL19SEPJDOE12A", 80f, 1590f, paint)

        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
    }

    private fun drawArrow(canvas: Canvas, startX: Float, y: Float, endX: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 9f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val head = 34f
        canvas.drawLine(startX, y, endX, y, paint)
        canvas.drawLine(endX, y, endX - head, y - head, paint)
        canvas.drawLine(endX, y, endX - head, y + head, paint)
    }

    private fun drawBarcode(
        canvas: Canvas,
        message: String,
        format: PassBarCodeFormat,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        val bitmap = generateBarCodeBitmap(message, format) ?: return
        canvas.drawBitmap(bitmap, null, RectF(left, top, right, bottom), null)
    }

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
        timeSpan = daysFromNow(1, 20, 30) to daysFromNow(1, 23, 30),
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
        timeSpan = daysFromNow(2, 11, 0) to daysFromNow(2, 18, 0),
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
        artworkBytes: ByteArray? = null,
        tagIds: Set<String> = emptySet(),
        notes: String = "",
        protected: Boolean = false,
        favorite: Boolean = false,
        importSource: ImportSource? = null,
        hasDocument: Boolean = false,
        documentPageCount: Int = 0,
        barcodes: List<PassBarcodeUiModel> = emptyList(),
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
        artwork = when {
            artworkName != null -> {
                val bytes = artworkCache.getOrPut(artworkName) {
                    testContext.assets.open("showcase/$artworkName.png").use { stream -> stream.readBytes() }
                }
                listOf(
                    PassArtworkUiModel(PassArtworkKind.STRIP, bytes),
                    PassArtworkUiModel(PassArtworkKind.THUMBNAIL, bytes),
                )
            }
            artworkBytes != null -> listOf(PassArtworkUiModel(PassArtworkKind.THUMBNAIL, artworkBytes))
            else -> emptyList()
        },
        calendarTimeSpan = timeSpan?.let { PassTimeSpanUiModel(it.first, it.second) },
        tagIds = tagIds,
        notes = notes,
        isProtected = protected,
        isFavorite = favorite,
        importSource = importSource,
        hasDocument = hasDocument,
        documentPageCount = documentPageCount,
        barcodes = barcodes,
    )

    private fun field(
        key: String,
        label: String,
        value: String,
        hidden: Boolean = false,
        hint: String? = null,
    ) = PassFieldUiModel(key, label, value, hidden, hint)

    private fun PassUiModel.toSnapshot() = PassSnapshot(
        id = id,
        description = description,
        creator = creator,
        type = type,
        accentColor = accentColor,
        barcodeFormat = barcodeFormat,
        barcodeMessage = barcodeMessage,
        barcodeAlternativeText = barcodeAlternativeText,
        fields = fields.map { PassFieldSnapshot(it.key, it.label, it.value, it.hidden, it.hint) },
        locations = locations.map { PassLocationSnapshot(it.name, it.latitude, it.longitude) },
        calendarTimeSpan = calendarTimeSpan?.let { PassTimeSpanSnapshot(it.from, it.to) },
        artwork = artwork.map { PassArtworkSnapshot(it.kind, it.bytes) },
        isProtected = isProtected,
        isFavorite = isFavorite,
        tagIds = tagIds,
        notes = notes,
    )

    private data class ThemeVariant(
        val name: String,
        val accent: Long,
        val style: ColorStyle,
        val mode: ThemeMode,
    )

    private val themeVariants = listOf(
        ThemeVariant("coral", CORAL, ColorStyle.FIDELITY, ThemeMode.DARK),
        ThemeVariant("coral-light", CORAL, ColorStyle.FIDELITY, ThemeMode.LIGHT),
        ThemeVariant("ocean-light", 0xFF0F6E8CL, ColorStyle.VIBRANT, ThemeMode.LIGHT),
        ThemeVariant("violet", 0xFF7B1FA2L, ColorStyle.VIBRANT, ThemeMode.DARK),
        ThemeVariant("amber", 0xFFF57C00L, ColorStyle.VIBRANT, ThemeMode.DARK),
        ThemeVariant("forest-light", 0xFF2E7D32L, ColorStyle.TONAL_SPOT, ThemeMode.LIGHT),
    )

    private fun todayAt(hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.now(ZONE).withHour(hour).withMinute(minute).withSecond(0).withNano(0)

    private fun daysFromNow(days: Long, hour: Int, minute: Int): ZonedDateTime =
        todayAt(hour, minute).plusDays(days)

    private companion object {
        const val CORAL = 0xFFFF6249L
        val ZONE = ZoneId.of("Europe/Madrid")
    }
}
