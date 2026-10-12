package org.ligi.passandroid.repository

enum class PassImageAspectRatio(
    val ratioWidth: Float,
    val ratioHeight: Float,
) {
    AUTO_HEIGHT(0f, 0f),
    SQUARE(1f, 1f),
    RATIO_4_5(4f, 5f),
    RATIO_3_4(3f, 4f),
    RATIO_9_16(9f, 16f),
    RATIO_16_9(16f, 9f),
    A4(210f, 297f),
    LETTER(8.5f, 11f),
}

enum class PassImageOrientation { PORTRAIT, LANDSCAPE }

enum class PassExportFormat { PNG, PDF }

/** The layout of an exported pass. Each template has its own ratios, content, and style options. */
enum class PassExportTemplate(
    val aspectRatios: List<PassImageAspectRatio>,
    val content: Set<PassExportContent>,
    val styles: Set<PassExportStyle>,
) {
    TICKET(
        listOf(
            PassImageAspectRatio.AUTO_HEIGHT,
            PassImageAspectRatio.SQUARE,
            PassImageAspectRatio.RATIO_4_5,
            PassImageAspectRatio.RATIO_3_4,
            PassImageAspectRatio.RATIO_9_16,
            PassImageAspectRatio.A4,
        ),
        PassExportContent.entries.toSet(),
        setOf(PassExportStyle.MONOCHROME, PassExportStyle.OPAQUE_BACKGROUND),
    ),
    STORY(
        listOf(PassImageAspectRatio.RATIO_9_16, PassImageAspectRatio.RATIO_3_4, PassImageAspectRatio.RATIO_4_5, PassImageAspectRatio.SQUARE),
        setOf(PassExportContent.ARTWORK, PassExportContent.DETAILS, PassExportContent.BARCODE, PassExportContent.DATE_TIME, PassExportContent.LOCATION),
        setOf(PassExportStyle.MONOCHROME),
    ),
    SHEET(
        listOf(PassImageAspectRatio.A4, PassImageAspectRatio.LETTER),
        PassExportContent.entries.toSet(),
        setOf(PassExportStyle.MONOCHROME),
    ),
    CODE(
        listOf(
            PassImageAspectRatio.AUTO_HEIGHT,
            PassImageAspectRatio.SQUARE,
            PassImageAspectRatio.RATIO_4_5,
            PassImageAspectRatio.RATIO_9_16,
        ),
        // The details toggle adds a caption with the pass name and date below the code.
        setOf(PassExportContent.DETAILS),
        setOf(PassExportStyle.OPAQUE_BACKGROUND),
    ),
    ;

    val defaultOptions: PassImageExportOptions
        get() = when (this) {
            TICKET -> PassImageExportOptions()
            STORY -> PassImageExportOptions(aspectRatio = PassImageAspectRatio.RATIO_9_16)
            SHEET -> PassImageExportOptions(aspectRatio = PassImageAspectRatio.A4, format = PassExportFormat.PDF)
            CODE -> PassImageExportOptions(content = CODE_ONLY_CONTENT)
        }

    /** True when the orientation control applies. Story is always tall, Auto follows the content, and a square has no orientation. */
    fun hasOrientation(ratio: PassImageAspectRatio): Boolean =
        this != STORY && ratio != PassImageAspectRatio.AUTO_HEIGHT && ratio != PassImageAspectRatio.SQUARE
}

enum class PassExportContent { ARTWORK, DETAILS, NOTES, BARCODE, DATE_TIME, LOCATION, HIDDEN_FIELDS }

enum class PassExportStyle { MONOCHROME, OPAQUE_BACKGROUND }

data class PassImageContent(
    val artwork: Boolean = true,
    val details: Boolean = true,
    val notes: Boolean = true,
    val barcode: Boolean = true,
    val dateTime: Boolean = true,
    val location: Boolean = true,
    val hiddenFields: Boolean = false,
) {
    operator fun get(item: PassExportContent): Boolean = when (item) {
        PassExportContent.ARTWORK -> artwork
        PassExportContent.DETAILS -> details
        PassExportContent.NOTES -> notes
        PassExportContent.BARCODE -> barcode
        PassExportContent.DATE_TIME -> dateTime
        PassExportContent.LOCATION -> location
        PassExportContent.HIDDEN_FIELDS -> hiddenFields
    }

    fun with(item: PassExportContent, value: Boolean): PassImageContent = when (item) {
        PassExportContent.ARTWORK -> copy(artwork = value)
        PassExportContent.DETAILS -> copy(details = value)
        PassExportContent.NOTES -> copy(notes = value)
        PassExportContent.BARCODE -> copy(barcode = value)
        PassExportContent.DATE_TIME -> copy(dateTime = value)
        PassExportContent.LOCATION -> copy(location = value)
        PassExportContent.HIDDEN_FIELDS -> copy(hiddenFields = value)
    }
}

val CODE_ONLY_CONTENT = PassImageContent(
    artwork = false,
    details = false,
    notes = false,
    barcode = true,
    dateTime = false,
    location = false,
)

data class PassImageExportOptions(
    val aspectRatio: PassImageAspectRatio = PassImageAspectRatio.AUTO_HEIGHT,
    val orientation: PassImageOrientation = PassImageOrientation.PORTRAIT,
    val content: PassImageContent = PassImageContent(),
    val format: PassExportFormat = PassExportFormat.PNG,
    val monochrome: Boolean = false,
    val opaqueBackground: Boolean = false,
) {
    operator fun get(style: PassExportStyle): Boolean = when (style) {
        PassExportStyle.MONOCHROME -> monochrome
        PassExportStyle.OPAQUE_BACKGROUND -> opaqueBackground
    }

    fun with(style: PassExportStyle, value: Boolean): PassImageExportOptions = when (style) {
        PassExportStyle.MONOCHROME -> copy(monochrome = value)
        PassExportStyle.OPAQUE_BACKGROUND -> copy(opaqueBackground = value)
    }
}

/** The selected template and the remembered options of every template. */
data class PassExportSettings(
    val template: PassExportTemplate = PassExportTemplate.TICKET,
    val options: Map<PassExportTemplate, PassImageExportOptions> = emptyMap(),
) {
    fun optionsFor(template: PassExportTemplate): PassImageExportOptions = options[template] ?: template.defaultOptions

    val selectedOptions: PassImageExportOptions get() = optionsFor(template)
}

/** The stored values of one template. Null means the value was never stored. */
internal data class StoredPassExportOptions(
    val aspectRatio: String?,
    val orientation: String?,
    val content: Set<String>?,
    val style: Set<String>?,
)

// Notes default on, so the key stores the disabled state. Stored sets from older versions lack the key and keep notes on.
private const val NOTES_DISABLED_KEY = "notesDisabled"
private const val PDF_KEY = "pdf"
private const val MONOCHROME_KEY = "monochrome"
private const val OPAQUE_BACKGROUND_KEY = "opaqueBackground"

internal fun encodePassImageContent(value: PassImageContent): Set<String> = buildSet {
    if (value.artwork) add("artwork")
    if (value.details) add("details")
    if (value.barcode) add("barcode")
    if (value.dateTime) add("dateTime")
    if (value.location) add("location")
    if (value.hiddenFields) add("hiddenFields")
    if (!value.notes) add(NOTES_DISABLED_KEY)
}

internal fun decodePassImageContent(values: Set<String>?): PassImageContent =
    if (values == null) {
        PassImageContent()
    } else {
        PassImageContent(
            artwork = "artwork" in values,
            details = "details" in values,
            barcode = "barcode" in values,
            dateTime = "dateTime" in values,
            location = "location" in values,
            hiddenFields = "hiddenFields" in values,
            notes = NOTES_DISABLED_KEY !in values,
        )
    }

internal fun encodePassExportStyle(value: PassImageExportOptions): Set<String> = buildSet {
    if (value.format == PassExportFormat.PDF) add(PDF_KEY)
    if (value.monochrome) add(MONOCHROME_KEY)
    if (value.opaqueBackground) add(OPAQUE_BACKGROUND_KEY)
}

/** Decodes the stored options. A missing or unknown value takes the template default, and a ratio must fit the template. */
internal fun decodePassExportOptions(template: PassExportTemplate, stored: StoredPassExportOptions): PassImageExportOptions {
    val defaults = template.defaultOptions
    val ratio = stored.aspectRatio
        ?.let { runCatching { PassImageAspectRatio.valueOf(it) }.getOrNull() }
        // Orientation selects the long side, so 16:9 is the same shape as 9:16 and only 9:16 stays a choice.
        ?.let { if (it == PassImageAspectRatio.RATIO_16_9) PassImageAspectRatio.RATIO_9_16 else it }
        ?.takeIf { it in template.aspectRatios }
        ?: defaults.aspectRatio
    val orientation = stored.orientation?.let { runCatching { PassImageOrientation.valueOf(it) }.getOrNull() } ?: defaults.orientation
    val style = stored.style
    return PassImageExportOptions(
        aspectRatio = ratio,
        orientation = orientation,
        content = stored.content?.let(::decodePassImageContent) ?: defaults.content,
        format = if (style == null) defaults.format else if (PDF_KEY in style) PassExportFormat.PDF else PassExportFormat.PNG,
        monochrome = style?.let { MONOCHROME_KEY in it } ?: defaults.monochrome,
        opaqueBackground = style?.let { OPAQUE_BACKGROUND_KEY in it } ?: defaults.opaqueBackground,
    )
}
