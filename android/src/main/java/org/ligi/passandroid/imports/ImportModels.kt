package org.ligi.passandroid.imports

import org.ligi.passandroid.model.pass.PassBarCodeFormat

enum class ImportSource { IMAGE, CAMERA, PDF }

data class DetectedCode(val format: PassBarCodeFormat, val message: String)

/** Crop rectangle in normalized image coordinates: 0..1, origin at the top left. */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)

    fun isValid(): Boolean = width > 0.02f && height > 0.02f

    companion object {
        val Full = NormalizedRect(0f, 0f, 1f, 1f)
    }
}

/** A staged document import that waits in the review screen. */
data class ImportDraft(
    val id: String,
    val source: ImportSource,
    val suggestedTitle: String,
    val suggestedAccentColor: Int,
    val pageCount: Int,
    val previewPng: ByteArray,
    val detectedCodes: List<DetectedCode>,
)

/** User changes applied when the staged import is committed. */
data class ImportEdits(
    val title: String,
    val accentColor: Int,
    val rotationDegrees: Int = 0,
    val crop: NormalizedRect = NormalizedRect.Full,
    /** Indices of [ImportDraft.detectedCodes]; the first one becomes the primary code. */
    val selectedCodeIndices: Set<Int> = emptySet(),
)
