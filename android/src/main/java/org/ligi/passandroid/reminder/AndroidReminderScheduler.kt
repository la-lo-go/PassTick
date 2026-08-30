package org.ligi.passandroid.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.ligi.passandroid.MainActivity
import org.ligi.passandroid.R
import org.ligi.passandroid.navigation.passDeepLink

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarmManager = requireNotNull(context.getSystemService(AlarmManager::class.java))

    override fun sync(reminders: List<PassReminder>) {
        val previous = readStored(context)
        val nextIds = reminders.mapTo(mutableSetOf(), PassReminder::id)
        previous.filterNot { it.id in nextIds }.forEach { cancel(it) }
        reminders.forEach { schedule(it) }
        store(context, reminders)
    }

    private fun schedule(reminder: PassReminder) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.triggerAtMillis,
            reminderIntent(context, reminder),
        )
    }

    private fun cancel(reminder: PassReminder) {
        alarmManager.cancel(reminderIntent(context, reminder))
    }

    companion object {
        fun rescheduleStored(context: Context) {
            val now = System.currentTimeMillis()
            val pending = readStored(context)
                .filter { it.eventAtMillis > now }
                .map { it.copy(triggerAtMillis = maxOf(now, it.triggerAtMillis)) }
            AndroidReminderScheduler(context.applicationContext).sync(pending)
        }
    }
}

class PassReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val passId = intent.getStringExtra(EXTRA_PASS_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Pass reminder"
        removeStored(context, reminderId)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Pass reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications for dated passes"
            },
        )
        val openPass = PendingIntent.getActivity(
            context,
            passId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = passDeepLink(passId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pass)
            .setContentTitle(title)
            .setContentText("Open your pass")
            .setContentIntent(openPass)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(passId.hashCode(), notification)
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) AndroidReminderScheduler.rescheduleStored(context)
    }
}

private const val CHANNEL_ID = "pass_reminders"
private const val EXTRA_REMINDER_ID = "reminder_id"
private const val EXTRA_PASS_ID = "pass_id"
private const val EXTRA_TITLE = "title"
private const val PREFS = "pass_reminder_schedule"
private const val STORED = "reminders"

private fun reminderIntent(context: Context, reminder: PassReminder) = PendingIntent.getBroadcast(
    context,
    reminder.id.hashCode(),
    Intent(context, PassReminderReceiver::class.java).apply {
        putExtra(EXTRA_REMINDER_ID, reminder.id)
        putExtra(EXTRA_PASS_ID, reminder.passId)
        putExtra(EXTRA_TITLE, reminder.title)
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private fun store(context: Context, reminders: List<PassReminder>) {
    val encoded = reminders.mapTo(mutableSetOf()) { reminder ->
        JSONObject()
            .put("id", reminder.id)
            .put("passId", reminder.passId)
            .put("title", reminder.title)
            .put("eventAt", reminder.eventAtMillis)
            .put("triggerAt", reminder.triggerAtMillis)
            .toString()
    }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(STORED, encoded).apply()
}

private fun readStored(context: Context): List<PassReminder> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(STORED, emptySet()).orEmpty()
        .mapNotNull { encoded ->
            runCatching {
                val value = JSONObject(encoded)
                PassReminder(
                    id = value.getString("id"),
                    passId = value.getString("passId"),
                    title = value.getString("title"),
                    eventAtMillis = value.getLong("eventAt"),
                    triggerAtMillis = value.getLong("triggerAt"),
                )
            }.getOrNull()
        }

private fun removeStored(context: Context, reminderId: String) {
    val remaining = readStored(context).filterNot { it.id == reminderId }
    store(context, remaining)
}
