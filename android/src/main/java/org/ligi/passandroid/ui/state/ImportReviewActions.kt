package org.ligi.passandroid.ui.state

import org.ligi.passandroid.imports.NormalizedRect

sealed interface ImportReviewAction {
    data class SetTitle(val title: String) : ImportReviewAction
    data class SetAccentColor(val color: Int) : ImportReviewAction
    data class SelectCode(val index: Int?) : ImportReviewAction
    data object Rotate : ImportReviewAction
    data class SetCrop(val crop: NormalizedRect) : ImportReviewAction
    data class SetCropEditing(val editing: Boolean) : ImportReviewAction
    data object Confirm : ImportReviewAction
    data object Discard : ImportReviewAction
}
