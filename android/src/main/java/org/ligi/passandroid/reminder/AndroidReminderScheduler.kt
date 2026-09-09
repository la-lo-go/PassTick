package org.ligi.passandroid.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
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
import org.ligi.passandroid.platform.PlatformLocation
import org.ligi.passandroid.platform.createLocationIntent

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarms = requireNotNull(context.getSystemService(AlarmManager::class.java))

    override fun sync(reminders: List<PassReminder>, settings: NotificationPolicySettings) {
        val now = System.currentTimeMillis()
        val previous = readStored(context)
        val active = reminders.filter { it.endAtMillis > now }.distinctBy(PassReminder::id)
        val activeIds = active.mapTo(mutableSetOf(), PassReminder::id)
        previous.filterNot { it.id in activeIds }.forEach(::cancel)
        val activeLifecycleKeys = active.mapTo(mutableSetOf(), ::lifecycleKey)
        lifecycleOwners(previous).filterNot { lifecycleKey(it) in activeLifecycleKeys }.forEach {
            alarms.cancel(lifecycleIntent(context, it))
        }
        (previous.map(PassReminder::passId).toSet() - active.map(PassReminder::passId).toSet())
            .forEach { NotificationManagerCompat.from(context).cancel(notificationId(it)) }
        active.forEach { scheduleLeadOrShow(context, it, now, settings) }
        lifecycleOwners(active).filter { it.triggerAtMillis <= now }.forEach {
            scheduleLifecycle(context, it, now, settings)
        }
        store(context, active)
        storePolicySettings(context, settings)
    }

    private fun cancel(reminder: PassReminder) {
        alarms.cancel(leadIntent(context, reminder))
        alarms.cancel(snoozeDeliveryIntent(context, reminder))
    }

    companion object {
        fun rescheduleStored(context: Context) {
            val now = System.currentTimeMillis()
            AndroidReminderScheduler(context.applicationContext).sync(
                readStored(context).filter { it.endAtMillis > now },
                readPolicySettings(context),
            )
        }
    }
}

class PassReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminder = intent.reminderOrNull() ?: return
        val settings = readPolicySettings(context)
        val result = NotificationPolicy.evaluate(reminder, System.currentTimeMillis(), settings)
        if (result.disposition == NotificationDisposition.CANCEL) {
            removeStoredEvent(context, reminder)
            NotificationManagerCompat.from(context).cancel(notificationId(reminder.passId))
            return
        }
        if (notificationsAllowed(context)) showReminder(context, reminder, result)
        if (intent.action == ACTION_LIFECYCLE || intent.action == null && isLifecycleOwner(context, reminder)) {
            scheduleLifecycle(context, reminder, System.currentTimeMillis(), settings)
        }
    }
}

class ReminderSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminder = intent.reminderOrNull() ?: return
        requireNotNull(context.getSystemService(AlarmManager::class.java)).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + DEFAULT_SNOOZE_MILLIS,
            snoozeDeliveryIntent(context, reminder),
        )
        NotificationManagerCompat.from(context).cancel(notificationId(reminder.passId))
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RESCHEDULE_ACTIONS) AndroidReminderScheduler.rescheduleStored(context)
    }
}

fun ensureReminderNotificationChannel(context: Context) {
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Pass reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Notifications for dated passes"
        },
    )
}

fun reminderNotificationsAvailable(context: Context): Boolean {
    ensureReminderNotificationChannel(context)
    val manager = context.getSystemService(NotificationManager::class.java)
    return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
}

internal fun notificationId(passId: String): Int = passId.hashCode()
internal fun useExactAlarm(enabled: Boolean, allowed: Boolean): Boolean = enabled && allowed
internal fun notificationVisibility(detail: NotificationLockScreenDetail): Int = when (detail) {
    NotificationLockScreenDetail.FULL -> NotificationCompat.VISIBILITY_PUBLIC
    NotificationLockScreenDetail.HIDE_SENSITIVE -> NotificationCompat.VISIBILITY_PRIVATE
    NotificationLockScreenDetail.HIDDEN -> NotificationCompat.VISIBILITY_SECRET
}
internal fun showsPublicVersion(detail: NotificationLockScreenDetail): Boolean =
    detail == NotificationLockScreenDetail.HIDE_SENSITIVE

internal enum class ReminderDeliveryDecision { SCHEDULE_LEAD, SHOW_DUE, CANCEL }

internal fun deliveryDecision(result: NotificationPolicyResult): ReminderDeliveryDecision = when (result.disposition) {
    NotificationDisposition.SCHEDULE -> ReminderDeliveryDecision.SCHEDULE_LEAD
    NotificationDisposition.SHOW -> ReminderDeliveryDecision.SHOW_DUE
    NotificationDisposition.CANCEL -> ReminderDeliveryDecision.CANCEL
}

internal fun lifecycleOwners(reminders: List<PassReminder>): List<PassReminder> = reminders
    .filter(PassReminder::ownsLifecycle)
    .groupBy(::lifecycleKey)
    .values
    .map { group -> group.minWith(compareBy(PassReminder::triggerAtMillis, PassReminder::id)) }

private fun scheduleLeadOrShow(context: Context, reminder: PassReminder, now: Long, settings: NotificationPolicySettings) {
    val result = NotificationPolicy.evaluate(reminder, now, settings)
    when (deliveryDecision(result)) {
        ReminderDeliveryDecision.SCHEDULE_LEAD -> setAlarm(context, reminder.triggerAtMillis, result, leadIntent(context, reminder))
        ReminderDeliveryDecision.SHOW_DUE -> if (notificationsAllowed(context)) showReminder(context, reminder, result)
        ReminderDeliveryDecision.CANCEL -> NotificationManagerCompat.from(context).cancel(notificationId(reminder.passId))
    }
}

private fun scheduleLifecycle(context: Context, reminder: PassReminder, now: Long, settings: NotificationPolicySettings) {
    val result = NotificationPolicy.evaluate(reminder, now, settings)
    val triggerAt = result.nextTriggerAtMillis ?: return
    setAlarm(context, triggerAt, result, lifecycleIntent(context, reminder))
}

private fun setAlarm(context: Context, triggerAt: Long, result: NotificationPolicyResult, operation: PendingIntent) {
    val manager = requireNotNull(context.getSystemService(AlarmManager::class.java))
    val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    if (useExactAlarm(result.exactTiming, exactAllowed)) {
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    } else {
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    }
}

private fun showReminder(context: Context, reminder: PassReminder, policy: NotificationPolicyResult) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    ensureReminderNotificationChannel(context)
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_pass)
        .setContentTitle(policy.title)
        .setContentText(policy.body)
        .setContentIntent(openPassIntent(context, reminder.passId, false))
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setVisibility(notificationVisibility(policy.lockScreenDetail))
    if (showsPublicVersion(policy.lockScreenDetail)) builder.setPublicVersion(publicNotification(context, policy))
    policy.actions.take(MAX_ACTIONS).forEach { builder.addReminderAction(context, reminder, it) }
    NotificationManagerCompat.from(context).notify(notificationId(reminder.passId), builder.build())
}

private fun NotificationCompat.Builder.addReminderAction(context: Context, reminder: PassReminder, action: NotificationAction) {
    when (action) {
        NotificationAction.OPEN_CODE -> addAction(0, "Open code", openPassIntent(context, reminder.passId, true))
        NotificationAction.DIRECTIONS -> addAction(0, "Directions", directionsIntent(context, reminder))
        NotificationAction.SNOOZE -> addAction(0, "Snooze", snoozeIntent(context, reminder))
    }
}

private fun publicNotification(context: Context, policy: NotificationPolicyResult): Notification =
    NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_pass)
        .setContentTitle(policy.publicTitle)
        .setContentText(policy.publicBody)
        .build()

private fun openPassIntent(context: Context, passId: String, showCode: Boolean) = PendingIntent.getActivity(
    context,
    (passId + showCode).hashCode(),
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = passDeepLink(passId).buildUpon().apply { if (showCode) appendQueryParameter("view", "code") }.build()
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    },
    PENDING_FLAGS,
)

private fun directionsIntent(context: Context, reminder: PassReminder) = PendingIntent.getActivity(
    context,
    (reminder.passId + "directions").hashCode(),
    createLocationIntent(PlatformLocation(reminder.locationLabel, reminder.latitude, reminder.longitude)),
    PENDING_FLAGS,
)

private fun snoozeIntent(context: Context, reminder: PassReminder) = PendingIntent.getBroadcast(
    context,
    reminder.id.hashCode(),
    Intent(context, ReminderSnoozeReceiver::class.java).putReminder(reminder),
    PENDING_FLAGS,
)

private fun leadIntent(context: Context, reminder: PassReminder) = PendingIntent.getBroadcast(
    context,
    reminder.id.hashCode(),
    Intent(context, PassReminderReceiver::class.java).putReminder(reminder),
    PENDING_FLAGS,
)

private fun lifecycleIntent(context: Context, reminder: PassReminder) = PendingIntent.getBroadcast(
    context,
    lifecycleKey(reminder).hashCode(),
    Intent(context, PassReminderReceiver::class.java).apply { action = ACTION_LIFECYCLE }.putReminder(reminder),
    PENDING_FLAGS,
)

private fun snoozeDeliveryIntent(context: Context, reminder: PassReminder) = PendingIntent.getBroadcast(
    context,
    (reminder.id + "snooze").hashCode(),
    Intent(context, PassReminderReceiver::class.java).apply { action = ACTION_SNOOZE }.putReminder(reminder),
    PENDING_FLAGS,
)

private fun Intent.putReminder(reminder: PassReminder): Intent = putExtra(EXTRA_REMINDER, encodeReminder(reminder))
private fun Intent.reminderOrNull(): PassReminder? = getStringExtra(EXTRA_REMINDER)?.let(::decodeReminder)

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun store(context: Context, reminders: List<PassReminder>) {
    val encoded = reminders.mapTo(mutableSetOf(), ::encodeReminder)
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(STORED, encoded).apply()
}

private fun storePolicySettings(context: Context, settings: NotificationPolicySettings) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putInt(ACCESS_WINDOW, settings.accessWindowMinutes)
        .putBoolean(EXACT_TIMING, settings.exactTiming)
        .putBoolean(ACTIONS_ENABLED, settings.actionsEnabled)
        .putBoolean(SNOOZE_ENABLED, settings.snoozeEnabled)
        .putString(LOCK_SCREEN_DETAIL, settings.lockScreenDetail.name)
        .putBoolean(UPDATE_AT_EVENT_START, settings.updateAtEventStart)
        .apply()
}

private fun readPolicySettings(context: Context): NotificationPolicySettings =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).run {
        NotificationPolicySettings(
            accessWindowMinutes = getInt(ACCESS_WINDOW, 15),
            exactTiming = getBoolean(EXACT_TIMING, false),
            actionsEnabled = getBoolean(ACTIONS_ENABLED, true),
            snoozeEnabled = getBoolean(SNOOZE_ENABLED, true),
            lockScreenDetail = getString(LOCK_SCREEN_DETAIL, null)
                ?.let { runCatching { NotificationLockScreenDetail.valueOf(it) }.getOrNull() }
                ?: NotificationLockScreenDetail.HIDE_SENSITIVE,
            updateAtEventStart = getBoolean(UPDATE_AT_EVENT_START, true),
        )
    }

private fun readStored(context: Context): List<PassReminder> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(STORED, emptySet()).orEmpty().mapNotNull(::decodeReminder)

private fun removeStoredEvent(context: Context, reminder: PassReminder) =
    store(context, readStored(context).filterNot { lifecycleKey(it) == lifecycleKey(reminder) })

private fun isLifecycleOwner(context: Context, reminder: PassReminder): Boolean =
    reminder.ownsLifecycle && lifecycleOwners(readStored(context)).any { it.id == reminder.id }

private fun lifecycleKey(reminder: PassReminder): String =
    "${reminder.passId}:${reminder.eventAtMillis}:${reminder.endAtMillis}"

internal fun encodeReminder(reminder: PassReminder): String = with(reminder) {
    JSONObject()
        .put("id", id).put("passId", passId).put("title", title)
        .put("eventAt", eventAtMillis).put("triggerAt", triggerAtMillis).put("endAt", endAtMillis)
        .put("locationLabel", locationLabel).put("latitude", latitude).put("longitude", longitude)
        .put("hasBarcode", hasBarcode).put("isProtected", isProtected).put("exactTiming", exactTiming)
        .put("enabledActions", enabledActions?.joinToString(",") { it.name }).put("ownsLifecycle", ownsLifecycle)
        .toString()
}

internal fun decodeReminder(encoded: String): PassReminder? = runCatching {
    val value = JSONObject(encoded)
    PassReminder(
        id = value.getString("id"), passId = value.getString("passId"), title = value.getString("title"),
        eventAtMillis = value.getLong("eventAt"), triggerAtMillis = value.getLong("triggerAt"),
        endAtMillis = value.optLong("endAt", value.getLong("eventAt")),
        locationLabel = value.optString("locationLabel").takeIf { it.isNotBlank() && it != "null" },
        latitude = value.optDouble("latitude").takeUnless(Double::isNaN),
        longitude = value.optDouble("longitude").takeUnless(Double::isNaN),
        hasBarcode = value.optBoolean("hasBarcode"), isProtected = value.optBoolean("isProtected"),
        exactTiming = value.optBoolean("exactTiming").takeIf { value.has("exactTiming") && !value.isNull("exactTiming") },
        enabledActions = value.optString("enabledActions").takeIf { it.isNotBlank() && it != "null" }
            ?.split(',')?.mapNotNullTo(mutableSetOf()) { runCatching { NotificationAction.valueOf(it) }.getOrNull() },
        ownsLifecycle = value.optBoolean("ownsLifecycle", true),
    )
}.getOrNull()

private val RESCHEDULE_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)
private const val CHANNEL_ID = "pass_reminders"
private const val EXTRA_REMINDER = "reminder"
private const val PREFS = "pass_reminder_schedule"
private const val STORED = "reminders"
private const val ACCESS_WINDOW = "access_window"
private const val EXACT_TIMING = "exact_timing"
private const val ACTIONS_ENABLED = "actions_enabled"
private const val SNOOZE_ENABLED = "snooze_enabled"
private const val LOCK_SCREEN_DETAIL = "lock_screen_detail"
private const val UPDATE_AT_EVENT_START = "update_at_event_start"
private const val ACTION_SNOOZE = "dev.lalogo.passtick.action.SNOOZE_DELIVERY"
private const val ACTION_LIFECYCLE = "dev.lalogo.passtick.action.REMINDER_LIFECYCLE"
private const val DEFAULT_SNOOZE_MILLIS = 10 * 60_000L
private const val MAX_ACTIONS = 3
private const val PENDING_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
