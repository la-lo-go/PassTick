package org.ligi.passandroid.ui.compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun AdaptivePassArtwork(
    bytes: ByteArray,
    accentColor: Int,
    contentDescription: String,
    modifier: Modifier,
    contentPadding: Dp = 8.dp,
) {
    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } ?: return
    val luminance = remember(bitmap) { bitmap.averageVisibleLuminance() }
    val accent = Color(accentColor)
    val accentLuminance = accent.luminance()
    val background = when {
        kotlin.math.abs(luminance - accentLuminance) >= 0.32 -> accent
        luminance >= 0.5 -> Color(0xFF171717)
        else -> Color.White
    }
    BoxWithConstraints(modifier) {
        val radius = (minOf(maxWidth, maxHeight) * 0.2f).coerceIn(12.dp, 32.dp)
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(radius),
            color = background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private fun Color.luminance(): Double =
    red.toDouble() * 0.2126 + green.toDouble() * 0.7152 + blue.toDouble() * 0.0722

private fun Bitmap.averageVisibleLuminance(): Double {
    val xStep = (width / 16).coerceAtLeast(1)
    val yStep = (height / 16).coerceAtLeast(1)
    var total = 0.0
    var count = 0
    for (y in 0 until height step yStep) {
        for (x in 0 until width step xStep) {
            val pixel = getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 32) continue
            total += android.graphics.Color.red(pixel) / 255.0 * 0.2126 +
                android.graphics.Color.green(pixel) / 255.0 * 0.7152 +
                android.graphics.Color.blue(pixel) / 255.0 * 0.0722
            count++
        }
    }
    return if (count == 0) 0.5 else total / count
}
