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
}

enum class PassImageOrientation { PORTRAIT, LANDSCAPE }

data class PassImageContent(
    val artwork: Boolean = true,
    val details: Boolean = true,
    val barcode: Boolean = true,
    val dateTime: Boolean = true,
    val location: Boolean = true,
    val hiddenFields: Boolean = false,
)

data class PassImageExportOptions(
    val aspectRatio: PassImageAspectRatio = PassImageAspectRatio.AUTO_HEIGHT,
    val orientation: PassImageOrientation = PassImageOrientation.PORTRAIT,
    val content: PassImageContent = PassImageContent(),
)
