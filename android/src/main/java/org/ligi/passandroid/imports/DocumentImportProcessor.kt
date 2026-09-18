package org.ligi.passandroid.imports

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.ReaderException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

object DocumentImportProcessor {

    const val MAX_DIMENSION = 2048
    const val MAX_DETECTION_PAGES = 40
    const val DETECTION_WIDTH_PX = 1600

    private val DEFAULT_ACCENT_COLOR = 0xFF1A73E8.toInt()

    private val FALLBACK_TITLE_DATE = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")
    private val CAMERA_NAME = Regex("(?i)^(img|dsc|pxl|photo)[-_ ]?\\d+$")
    private val DIGITS_ONLY_NAME = Regex("^\\d+$")
    private val UUID_NAME = Regex("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    private val ROTATIONS = intArrayOf(0, 90, 180, 270)
    private val DECODE_HINTS: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to PassBarCodeFormat.entries.map { it.zxingBarCodeFormat() },
    )

    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver
            .query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

    fun copyToFile(context: Context, uri: Uri, target: File) {
        target.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open import source")
    }

    fun decodeNormalizedBitmap(context: Context, uri: Uri, maxDimension: Int = MAX_DIMENSION): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null
            applyExifOrientation(decoded, readExifOrientation(context, uri))
        } catch (error: OutOfMemoryError) {
            null
        } catch (error: Exception) {
            null
        }
    }

    fun decodeNormalizedBitmap(file: File, maxDimension: Int = MAX_DIMENSION): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            applyExifOrientation(decoded, readExifOrientation(file))
        } catch (error: OutOfMemoryError) {
            null
        } catch (error: Exception) {
            null
        }
    }

    fun suggestTitle(displayName: String?, fallbackLabel: String, now: ZonedDateTime): String {
        val stem = displayName?.substringBeforeLast('.')?.trim()?.takeIf { it.isNotEmpty() }
        if (stem != null && stem.any { it.isLetter() } && !isGeneratedName(stem)) return stem
        return "$fallbackLabel · ${now.format(FALLBACK_TITLE_DATE)}"
    }

    fun suggestAccentColor(bitmap: Bitmap): Int {
        val accumulator = AccentAccumulator()
        val step = (maxOf(bitmap.width, bitmap.height) / 64).coerceAtLeast(1)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap[x, y]
                if (Color.alpha(pixel) != 0) {
                    accumulator.add(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
                }
                x += step
            }
            y += step
        }
        return accumulator.result()
    }

    private class AccentAccumulator {
        private val histogram = HashMap<Int, Int>()
        private var redSum = 0L
        private var greenSum = 0L
        private var blueSum = 0L
        private var visible = 0L

        fun add(red: Int, green: Int, blue: Int) {
            redSum += red
            greenSum += green
            blueSum += blue
            visible++
            if (isUsableAccent(red, green, blue)) {
                val key = ((red shr 4) shl 8) or ((green shr 4) shl 4) or (blue shr 4)
                histogram[key] = (histogram[key] ?: 0) + 1
            }
        }

        fun result(): Int {
            if (visible == 0L) return DEFAULT_ACCENT_COLOR
            val dominant = histogram.maxByOrNull { it.value }?.key
            if (dominant != null) {
                return Color.rgb(
                    ((dominant shr 8) and 0xF) * 17,
                    ((dominant shr 4) and 0xF) * 17,
                    (dominant and 0xF) * 17,
                )
            }
            return Color.rgb(
                (redSum / visible).toInt().coerceIn(0, 255),
                (greenSum / visible).toInt().coerceIn(0, 255),
                (blueSum / visible).toInt().coerceIn(0, 255),
            )
        }
    }

    private fun isUsableAccent(red: Int, green: Int, blue: Int): Boolean {
        val saturation = maxOf(red, green, blue) - minOf(red, green, blue)
        val luminance = (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255.0
        return saturation >= 32 && luminance in 0.12..0.85
    }

    fun detectCodes(bitmap: Bitmap): List<DetectedCode> = runCatching {
        val found = LinkedHashMap<Pair<PassBarCodeFormat, String>, DetectedCode>()
        for (degrees in ROTATIONS) {
            decodeAtRotation(bitmap, degrees, found)
        }
        found.values.toList()
    }.getOrElse { emptyList() }

    private fun decodeAtRotation(
        bitmap: Bitmap,
        degrees: Int,
        found: MutableMap<Pair<PassBarCodeFormat, String>, DetectedCode>,
    ) {
        val candidate = if (degrees == 0) bitmap else rotate(bitmap, degrees)
        try {
            collectCodes(candidate, found)
        } finally {
            if (candidate !== bitmap) candidate.recycle()
        }
    }

    fun encodePng(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        output.toByteArray()
    }

    fun encodeJpeg(bitmap: Bitmap, quality: Int = 90): ByteArray = ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        output.toByteArray()
    }

    fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        if (maxDimension <= 0 || longestSide <= maxDimension) return bitmap
        val scaleFactor = maxDimension.toFloat() / longestSide
        return bitmap.scale(
            (bitmap.width * scaleFactor).roundToInt().coerceAtLeast(1),
            (bitmap.height * scaleFactor).roundToInt().coerceAtLeast(1),
            filter = true,
        )
    }

    fun decodePng(bytes: ByteArray): Bitmap? = runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return bitmap
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun crop(bitmap: Bitmap, crop: NormalizedRect): Bitmap {
        val clamped = NormalizedRect(
            left = crop.left.coerceIn(0f, 1f),
            top = crop.top.coerceIn(0f, 1f),
            right = crop.right.coerceIn(0f, 1f),
            bottom = crop.bottom.coerceIn(0f, 1f),
        )
        if (isFullFrame(clamped)) return bitmap
        if (!clamped.isValid()) return bitmap
        val x = (clamped.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (clamped.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val width = (clamped.width * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
        val height = (clamped.height * bitmap.height).toInt().coerceIn(1, bitmap.height - y)
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    private fun isFullFrame(rect: NormalizedRect): Boolean {
        val coversWidth = rect.left <= 0.001f && rect.right >= 0.999f
        val coversHeight = rect.top <= 0.001f && rect.bottom >= 0.999f
        return coversWidth && coversHeight
    }

    fun pdfPageCount(file: File): Int = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
        }
    }.getOrDefault(0)

    fun renderPdfPage(file: File, pageIndex: Int, targetWidthPx: Int): Bitmap? = try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer -> renderPage(renderer, pageIndex, targetWidthPx) }
        }
    } catch (error: OutOfMemoryError) {
        null
    } catch (error: Exception) {
        null
    }

    private fun renderPage(renderer: PdfRenderer, pageIndex: Int, targetWidthPx: Int): Bitmap? {
        if (pageIndex !in 0 until renderer.pageCount) return null
        return renderer.openPage(pageIndex).use { page -> renderIntoBitmap(page, targetWidthPx) }
    }

    private fun renderIntoBitmap(page: PdfRenderer.Page, targetWidthPx: Int): Bitmap {
        val width = targetWidthPx.coerceAtLeast(1)
        val scale = if (page.width > 0) width.toFloat() / page.width else 1f
        val height = (page.height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Untouched pixels stay transparent; barcode detection needs a light background.
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    private fun collectCodes(bitmap: Bitmap, found: MutableMap<Pair<PassBarCodeFormat, String>, DetectedCode>) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        decodePixels(bitmap.width, bitmap.height, pixels).forEach { addCode(it, found) }
        decodePixels(bitmap.width, bitmap.height, invertPixels(pixels)).forEach { addCode(it, found) }
    }

    private fun addCode(code: DetectedCode?, found: MutableMap<Pair<PassBarCodeFormat, String>, DetectedCode>) {
        if (code != null) found.putIfAbsent(code.format to code.message, code)
    }

    /** A page can hold several barcodes; the multi reader finds all of them. */
    private fun decodePixels(width: Int, height: Int, pixels: IntArray): List<DetectedCode> {
        val reader = MultiFormatReader().apply { setHints(DECODE_HINTS) }
        return try {
            val source = RGBLuminanceSource(width, height, pixels)
            val binary = BinaryBitmap(HybridBinarizer(source))
            val multiple = GenericMultipleBarcodeReader(reader)
                .decodeMultiple(binary)
                .mapNotNull(::toDetectedCode)
            if (multiple.isNotEmpty()) multiple else listOfNotNull(toDetectedCode(reader.decode(binary)))
        } catch (error: ReaderException) {
            emptyList()
        } catch (error: RuntimeException) {
            emptyList()
        } finally {
            reader.reset()
        }
    }

    private fun toDetectedCode(result: com.google.zxing.Result): DetectedCode? = runCatching {
        DetectedCode(PassBarCodeFormat.valueOf(result.barcodeFormat.name), result.text)
    }.getOrNull()

    private fun invertPixels(pixels: IntArray): IntArray = IntArray(pixels.size) { index ->
        val pixel = pixels[index]
        val alpha = pixel and 0xFF000000.toInt()
        val red = 0xFF - ((pixel shr 16) and 0xFF)
        val green = 0xFF - ((pixel shr 8) and 0xFF)
        val blue = 0xFF - (pixel and 0xFF)
        alpha or (red shl 16) or (green shl 8) or blue
    }

    private fun isGeneratedName(stem: String): Boolean =
        CAMERA_NAME.matches(stem) || DIGITS_ONLY_NAME.matches(stem) || UUID_NAME.matches(stem)

    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (maxDimension <= 0) return 1
        var sample = 1
        // Use the largest power-of-two sample size that keeps both dimensions within the limit.
        while (width / sample > maxDimension || height / sample > maxDimension) sample *= 2
        return sample
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        val fromStream = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull()
        if (fromStream != null) return fromStream
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = uri.path
            if (path != null) return readExifOrientation(File(path))
        }
        return ExifInterface.ORIENTATION_NORMAL
    }

    private fun readExifOrientation(file: File): Int = runCatching {
        ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = exifMatrix(orientation) ?: return bitmap
        val oriented = try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (error: Exception) {
            return bitmap
        }
        if (oriented !== bitmap) bitmap.recycle()
        return oriented
    }

    private fun exifMatrix(orientation: Int): Matrix? {
        val rotation = exifRotationDegrees(orientation)
        val mirrorHorizontally = orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE
        val mirrorVertically = orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL
        if (rotation == 0f && !mirrorHorizontally && !mirrorVertically) return null
        return Matrix().apply {
            if (rotation != 0f) postRotate(rotation)
            if (mirrorHorizontally) postScale(-1f, 1f)
            if (mirrorVertically) postScale(1f, -1f)
        }
    }

    private fun exifRotationDegrees(orientation: Int): Float = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> -90f
        else -> 0f
    }
}
