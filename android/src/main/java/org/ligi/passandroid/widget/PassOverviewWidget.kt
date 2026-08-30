package org.ligi.passandroid.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CompactWidget = DpSize(120.dp, 110.dp)
private val ListWidget = DpSize(250.dp, 180.dp)

class PassOverviewWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(CompactWidget, ListWidget))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val selection = PassWidgetSnapshotStore(context).read().select(
            now = Instant.now(),
            zoneId = ZoneId.systemDefault(),
        )
        provideContent {
            GlanceTheme {
                PassOverviewContent(selection)
            }
        }
    }
}

class PassOverviewWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PassOverviewWidget()
}

@Composable
private fun PassOverviewContent(selection: WidgetPassSelection) {
    val large = LocalSize.current.width >= ListWidget.width && LocalSize.current.height >= ListWidget.height
    val context = LocalContext.current
    val baseModifier = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .padding(if (large) 20.dp else 16.dp)
    val containerModifier = if (!large && selection.currentOrNext != null) {
        baseModifier.clickable(actionStartActivity(openPassIntent(context, selection.currentOrNext.id)))
    } else {
        baseModifier
    }
    Column(
        modifier = containerModifier,
    ) {
        Text(
            text = if (large) "Active passes" else "Next pass",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(GlanceModifier.height(10.dp))
        if (large) {
            if (selection.active.isEmpty()) {
                EmptyWidget("No active passes")
            } else {
                LazyColumn(GlanceModifier.fillMaxSize()) {
                    items(selection.active, itemId = { it.id.hashCode().toLong() }) { pass ->
                        PassRow(pass)
                        Spacer(GlanceModifier.height(8.dp))
                    }
                }
            }
        } else {
            selection.currentOrNext?.let { pass ->
                Text(
                    text = pass.title,
                    maxLines = 2,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = if (large) 22.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                pass.subtitle()?.let { subtitle ->
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        text = subtitle,
                        maxLines = 2,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                    )
                }
            } ?: EmptyWidget("No upcoming passes")
        }
    }
}

@Composable
private fun PassRow(pass: WidgetPass) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(openPassIntent(context, pass.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pass.timeLabel()?.let { time ->
            Text(
                text = time,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.width(12.dp))
        }
        Column {
            Text(
                text = pass.title,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            pass.location?.let { location ->
                Text(
                    text = location,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                )
            }
        }
    }
}

@Composable
internal fun EmptyWidget(message: String) {
    Text(
        text = message,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
    )
}

private fun WidgetPass.subtitle(): String? = listOfNotNull(timeLabel(), location, supportingText)
    .distinct()
    .joinToString(" · ")
    .takeIf(String::isNotBlank)

private fun WidgetPass.timeLabel(): String? = startsAtEpochMillis?.let { epochMillis ->
    DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}
