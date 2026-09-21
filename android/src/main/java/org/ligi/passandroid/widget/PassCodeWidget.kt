package org.ligi.passandroid.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import org.ligi.passandroid.MainActivity
import org.ligi.passandroid.R
import org.ligi.passandroid.repository.DataStoreSettingsRepository
import org.ligi.passandroid.ui.barcode.CrispBarcodeRenderer
import java.time.Instant
import java.time.ZoneId

private val CodeSmallSize = DpSize(110.dp, 110.dp)
private val CodeWideSize = DpSize(250.dp, 110.dp)

class PassCodeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(CodeSmallSize, CodeWideSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = DataStoreSettingsRepository(context).settings.first()
        val snapshot = PassWidgetSnapshotStore(context).read()
        val pass = resolveCodePass(
            passes = snapshot.passes,
            codePassId = settings.codePassId,
            sortOrder = settings.sortOrder,
            passOrder = settings.passOrder,
            now = Instant.now(),
            zoneId = ZoneId.systemDefault(),
        )
        provideContent {
            GlanceTheme {
                PassCodeContent(pass)
            }
        }
    }
}

class PassCodeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PassCodeWidget()
}

@Composable
private fun PassCodeContent(pass: WidgetPass?) {
    val context = LocalContext.current
    val wide = LocalSize.current.width >= CodeWideSize.width
    val horizontalPadding = if (wide) 20.dp else 12.dp
    val verticalPadding = 10.dp
    val density = context.resources.displayMetrics.density
    val availableWidthPx = ((LocalSize.current.width.value - 2 * horizontalPadding.value) * density).toInt()
    val availableHeightPx = ((LocalSize.current.height.value - 2 * verticalPadding.value - 34) * density).toInt()
    val barcode = remember(pass?.id, availableWidthPx, availableHeightPx) {
        pass
            ?.barcodeMessage
            ?.let { message ->
                pass.barcodeFormat
                    ?.takeIf(CrispBarcodeRenderer::supports)
                    ?.let { format ->
                        CrispBarcodeRenderer.renderBitmap(
                            data = message,
                            format = format,
                            maxWidthPx = availableWidthPx,
                            maxHeightPx = availableHeightPx,
                        )
                    }
            }
    }
    val openApp = GlanceModifier.clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    val contentModifier = if (pass == null) {
        GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).padding(16.dp).then(openApp)
    } else {
        GlanceModifier.fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .clickable(actionStartActivity(openPassIntent(context, pass.id, showCode = true)))
    }

    if (pass == null) {
        Column(
            modifier = contentModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = context.getString(R.string.code_widget_empty),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        return
    }

    if (wide) {
        Row(modifier = contentModifier, verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.width(90.dp)) {
                Text(
                    text = pass.primaryLine ?: pass.title,
                    maxLines = 2,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                pass.issuer?.let { issuer ->
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = issuer,
                        maxLines = 2,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                    )
                }
            }
            Spacer(GlanceModifier.width(12.dp))
            BarcodeImage(barcode, context)
        }
    } else {
        Column(modifier = contentModifier) {
            Text(
                text = pass.primaryLine ?: pass.title,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            pass.issuer?.let { issuer ->
                Text(
                    text = issuer,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            BarcodeImage(barcode, context)
        }
    }
}

@Composable
private fun BarcodeImage(barcode: android.graphics.Bitmap?, context: Context) {
    if (barcode == null) {
        Text(
            text = context.getString(R.string.code_widget_no_code),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
        return
    }
    Image(
        provider = ImageProvider(barcode),
        contentDescription = null,
        modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
        contentScale = ContentScale.Fit,
    )
}