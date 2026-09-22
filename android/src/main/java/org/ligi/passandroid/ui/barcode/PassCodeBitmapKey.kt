package org.ligi.passandroid.ui.barcode

import org.ligi.passandroid.model.pass.PassBarCodeFormat

/**
 * Cache key for a rendered code bitmap. The extra quiet zone and the quarter turn change the
 * pixels, so they must be part of the key. The size step reaches the key through the target size.
 */
internal data class PassCodeBitmapKey(
    val format: PassBarCodeFormat,
    val message: String,
    val widthPx: Int,
    val heightPx: Int,
    val extraQuietZone: Boolean,
    val rotateQuarterTurn: Boolean,
)

internal fun passCodeBitmapKey(
    format: PassBarCodeFormat,
    message: String,
    widthPx: Int,
    heightPx: Int,
    options: CodeViewOptions,
): PassCodeBitmapKey = PassCodeBitmapKey(
    format = format,
    message = message,
    widthPx = widthPx,
    heightPx = heightPx,
    extraQuietZone = options.extraQuietZone,
    rotateQuarterTurn = options.rotateQuarterTurn,
)
