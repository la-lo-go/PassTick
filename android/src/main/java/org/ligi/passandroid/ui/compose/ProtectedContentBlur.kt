package org.ligi.passandroid.ui.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

internal fun Modifier.softProtectedBlur(enabled: Boolean): Modifier =
    if (enabled) blur(radius = 9.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded) else this
