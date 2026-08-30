package org.ligi.passandroid.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.time.Instant
import java.time.ZoneId

private val QuickCodeCompact = DpSize(120.dp, 110.dp)
private val QuickCodeWide = DpSize(250.dp, 110.dp)

class QuickCodeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(QuickCodeCompact, QuickCodeWide))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val pass = PassWidgetSnapshotStore(context).read().select(
            now = Instant.now(),
            zoneId = ZoneId.systemDefault(),
        ).quickCode
        provideContent {
            GlanceTheme {
                QuickCodeContent(pass)
            }
        }
    }
}

class QuickCodeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCodeWidget()
}

@Composable
private fun QuickCodeContent(pass: WidgetPass?) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp)
            .let { modifier ->
                pass?.let {
                    modifier.clickable(actionStartActivity(openPassIntent(context, it.id, showCode = true)))
                } ?: modifier
            },
    ) {
        Text(
            text = "Quick code",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (pass == null) {
            EmptyWidget("Choose a pass in settings")
        } else {
            Text(
                text = pass.title,
                maxLines = 2,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "Open full-screen code",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
    }
}
