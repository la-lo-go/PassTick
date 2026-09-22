package org.ligi.passandroid.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

internal fun scheduleNextWidgetRefresh(
    context: Context,
    snapshot: PassWidgetSnapshot,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    val alarmManager = requireNotNull(context.getSystemService(AlarmManager::class.java))
    val pendingIntent = widgetRefreshIntent(context)
    val hasInstances = WIDGET_RECEIVERS.any { receiver ->
        val componentName = ComponentName(context, receiver)
        AppWidgetManager.getInstance(context).getAppWidgetIds(componentName).isNotEmpty()
    }
    if (!hasInstances) {
        alarmManager.cancel(pendingIntent)
        return
    }
    alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        snapshot.nextRefreshAt(now, zoneId).toEpochMilli(),
        pendingIntent,
    )
}

class PassWidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in REFRESH_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snapshot = PassWidgetSnapshotStore(context).read()
                PassOverviewWidget().updateAll(context)
                PassCodeWidget().updateAll(context)
                scheduleNextWidgetRefresh(context, snapshot)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private fun widgetRefreshIntent(context: Context) = PendingIntent.getBroadcast(
    context,
    0,
    Intent(context, PassWidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH_WIDGET),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private val WIDGET_RECEIVERS = listOf(
    PassOverviewWidgetReceiver::class.java,
    PassCodeWidgetReceiver::class.java,
)

private const val ACTION_REFRESH_WIDGET = "org.ligi.passandroid.action.REFRESH_WIDGET"
private val REFRESH_ACTIONS = setOf(
    ACTION_REFRESH_WIDGET,
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
)
