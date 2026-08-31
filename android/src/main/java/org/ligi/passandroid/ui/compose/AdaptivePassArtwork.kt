package org.ligi.passandroid.ui.compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
    contentDescription: String,
    modifier: Modifier,
    cornerRadius: Dp,
    contentPadding: Dp = 8.dp,
) {
    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } ?: return
    val luminance = remember(bitmap) { bitmap.averageVisibleLuminance() }
    val background = when {
        luminance >= 0.72 -> Color(0xFF171717)
        luminance <= 0.28 -> Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
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
