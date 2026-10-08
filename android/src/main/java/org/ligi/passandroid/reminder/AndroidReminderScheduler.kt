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
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.util.Date
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
        val localizedSettings = settings.copy(strings = notificationStrings(context))
        val previous = readStored(context)
        val active = reminders.filter { it.endAtMillis > now }.distinctBy(PassReminder::id)
        val activeIds = active.mapTo(mutableSetOf(), PassReminder::id)
        previous.filterNot { it.id in activeIds }.forEach(::cancel)
        val activeLifecycleKeys = active.mapTo(mutableSetOf(), ::lifecycleKey)
        lifecycleOwners(previous).filterNot { lifecycleKey(it) in activeLifecycleKeys }.forEach {
            alarms.cancel(lifecycleIntent(context, it))
        }
        (previous.map(PassReminder::passId).toSet() - active.map(PassReminder::passId).toSet())
            .forEach { passId ->
                NotificationManagerCompat.from(context).cancel(notificationId(passId))
                NotificationManagerCompat.from(context).cancel(expirationNotificationId(passId))
            }
        val posted = active.filter { scheduleLeadOrShow(context, it, now, localizedSettings) }.mapTo(mutableSetOf(), PassReminder::id)
        lifecycleOwners(active).filter { it.triggerAtMillis <= now }.forEach {
            scheduleLifecycle(context, it, now, localizedSettings, refresh = it.id in posted || isVisible(context, notificationId(it)))
        }
        store(context, active)
        pruneDelivered(context, activeIds)
        storePolicySettings(context, localizedSettings)
    }

    private fun cancel(reminder: PassReminder) {
        alarms.cancel(leadIntent(context, reminder))
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
        val now = System.currentTimeMillis()
        val settings = readPolicySettings(context)
        if (intent.action == ACTION_SNOOZE) {
            NotificationManagerCompat.from(context).cancel(notificationId(reminder))
            scheduleSnooze(context, reminder, now, settings)
            return
        }
        val result = NotificationPolicy.evaluate(
            reminder,
            now,
            settings,
            snoozed = intent.action == ACTION_SNOOZE_DELIVERY,
        )
        if (result.disposition == NotificationDisposition.CANCEL) {
            removeStoredEvent(context, reminder)
            NotificationManagerCompat.from(context).cancel(notificationId(reminder))
            return
        }
        val posted = notificationsAllowed(context) &&
            showReminder(context, reminder, result, force = intent.action == ACTION_SNOOZE_DELIVERY)
        if (intent.action == ACTION_LIFECYCLE || intent.action == null && isLifecycleOwner(context, reminder)) {
            scheduleLifecycle(context, reminder, System.currentTimeMillis(), settings, refresh = posted)
        }
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RESCHEDULE_ACTIONS) AndroidReminderScheduler.rescheduleStored(context)
    }
}

fun ensureReminderNotificationChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    listOf(
        Triple(CHANNEL_ID, R.string.reminder_channel_name to R.string.reminder_channel_description, NotificationManager.IMPORTANCE_DEFAULT),
        Triple(CHANNEL_ACCESS, R.string.reminder_channel_access_name to R.string.reminder_channel_access_description, NotificationManager.IMPORTANCE_HIGH),
        Triple(CHANNEL_EXPIRATION, R.string.reminder_channel_expiration_name to R.string.reminder_channel_expiration_description, NotificationManager.IMPORTANCE_DEFAULT),
    ).forEach { (id, text, importance) ->
        manager.createNotificationChannel(
            NotificationChannel(id, context.getString(text.first), importance).apply {
                description = context.getString(text.second)
            },
        )
    }
}

/** Reminders can only reach the user when the app may notify and at least one channel is on. */
fun reminderNotificationsAvailable(context: Context): Boolean {
    ensureReminderNotificationChannel(context)
    val manager = context.getSystemService(NotificationManager::class.java)
    return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        REMINDER_CHANNELS.any { manager.getNotificationChannel(it)?.importance != NotificationManager.IMPORTANCE_NONE }
}

internal fun channelId(reminder: PassReminder, phase: NotificationPhase): String = when {
    reminder.isExpiration -> CHANNEL_EXPIRATION
    phase == NotificationPhase.ACCESS -> CHANNEL_ACCESS
    else -> CHANNEL_ID
}

/**
 * A reminder is posted once. Later syncs and lifecycle alarms only refresh a notification that is
 * still visible, so a dismissed notification does not come back. A snooze delivery is always posted.
 */
internal fun shouldPost(alreadyDelivered: Boolean, visible: Boolean, force: Boolean): Boolean =
    force || !alreadyDelivered || visible

internal fun notificationId(passId: String): Int = passId.hashCode()
internal fun expirationNotificationId(passId: String): Int = "$passId$EXPIRATION_NOTIFICATION_KEY".hashCode()
internal fun notificationId(reminder: PassReminder): Int =
    if (reminder.isExpiration) expirationNotificationId(reminder.passId) else notificationId(reminder.passId)
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

/** Returns true when the call posted a notification. */
private fun scheduleLeadOrShow(context: Context, reminder: PassReminder, now: Long, settings: NotificationPolicySettings): Boolean {
    val result = NotificationPolicy.evaluate(reminder, now, settings)
    return when (deliveryDecision(result)) {
        ReminderDeliveryDecision.SCHEDULE_LEAD -> {
            setAlarm(context, reminder.triggerAtMillis, result, leadIntent(context, reminder))
            false
        }
        ReminderDeliveryDecision.SHOW_DUE -> notificationsAllowed(context) && showReminder(context, reminder, result, force = false)
        ReminderDeliveryDecision.CANCEL -> {
            NotificationManagerCompat.from(context).cancel(notificationId(reminder))
            false
        }
    }
}

private fun scheduleSnooze(context: Context, reminder: PassReminder, now: Long, settings: NotificationPolicySettings) {
    val deliveryAt = now + SNOOZE_MILLIS
    val result = NotificationPolicy.evaluate(reminder, deliveryAt, settings, snoozed = true)
    setAlarm(context, deliveryAt, result, snoozeDeliveryIntent(context, reminder))
}

/**
 * While the notification is on screen, the alarm also fires each time the remaining-time text changes,
 * so the text stays current. A dismissed notification only keeps the phase and end alarms.
 */
private fun scheduleLifecycle(
    context: Context,
    reminder: PassReminder,
    now: Long,
    settings: NotificationPolicySettings,
    refresh: Boolean,
) {
    val result = NotificationPolicy.evaluate(reminder, now, settings)
    val phaseAt = result.nextTriggerAtMillis ?: return
    val triggerAt = if (refresh) minOf(phaseAt, now + labelChangeDelayMillis(reminder.eventAtMillis - now)) else phaseAt
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

/** Returns true when the call posted or updated the notification. */
private fun showReminder(context: Context, reminder: PassReminder, policy: NotificationPolicyResult, force: Boolean): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false
    ensureReminderNotificationChannel(context)
    val id = notificationId(reminder)
    val key = deliveryKey(reminder)
    if (!shouldPost(key in readDelivered(context), isVisible(context, id), force)) return false
    val channel = channelId(reminder, policy.phase)
    val builder = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_notification_pass)
        .setContentTitle(policy.title)
        .setContentText(policy.body)
        .setContentIntent(openPassIntent(context, reminder.passId, false))
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setVisibility(notificationVisibility(policy.lockScreenDetail))
    val remaining = reminder.eventAtMillis - System.currentTimeMillis()
    if (remaining > 0) builder.setTimeoutAfter(remaining)
    if (showsPublicVersion(policy.lockScreenDetail)) builder.setPublicVersion(publicNotification(context, policy, channel))
    policy.actions.take(MAX_ACTIONS).forEach { builder.addReminderAction(context, reminder, it) }
    NotificationManagerCompat.from(context).notify(id, builder.build())
    markDelivered(context, key)
    return true
}

private fun isVisible(context: Context, id: Int): Boolean =
    context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == id }

private fun NotificationCompat.Builder.addReminderAction(context: Context, reminder: PassReminder, action: NotificationAction) {
    when (action) {
        NotificationAction.OPEN_CODE -> addAction(0, context.getString(R.string.reminder_open_code), openPassIntent(context, reminder.passId, true))
        NotificationAction.DIRECTIONS -> addAction(0, context.getString(R.string.reminder_directions), directionsIntent(context, reminder))
        NotificationAction.SNOOZE -> addAction(0, context.getString(R.string.reminder_snooze), snoozeIntent(context, reminder))
    }
}

private fun publicNotification(context: Context, policy: NotificationPolicyResult, channel: String): Notification =
    NotificationCompat.Builder(context, channel)
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

private fun leadIntent(context: Context, reminder: PassReminder) = PendingIntent.getBroadcast(
    context,
    reminder.id.hashCode(),
    Intent(context, PassReminderReceiver::class.java).putReminder(reminder),
    PENDING_FLAGS,
)

private fun snoozeIntent(context: Context, reminder: PassReminder) = PendingIntent.getBroadcast(
    context,
    "$SNOOZE_REQUEST_KEY:${reminder.id}".hashCode(),
    Intent(context, PassReminderReceiver::class.java).apply { action = ACTION_SNOOZE }.putReminder(reminder),
    PENDING_FLAGS,
)

private fun snoozeDeliveryIntent(context: Context, reminder: PassReminder) = PendingIntent.getBroadcast(
    context,
    "$SNOOZE_DELIVERY_REQUEST_KEY:${reminder.id}".hashCode(),
    Intent(context, PassReminderReceiver::class.java).apply { action = ACTION_SNOOZE_DELIVERY }.putReminder(reminder),
    PENDING_FLAGS,
)

private fun lifecycleIntent(context: Context, reminder: PassReminder) = PendingIntent.getBroadcast(
    context,
    lifecycleKey(reminder).hashCode(),
    Intent(context, PassReminderReceiver::class.java).apply { action = ACTION_LIFECYCLE }.putReminder(reminder),
    PENDING_FLAGS,
)

private fun Intent.putReminder(reminder: PassReminder): Intent = putExtra(EXTRA_REMINDER, encodeReminder(reminder))
private fun Intent.reminderOrNull(): PassReminder? = getStringExtra(EXTRA_REMINDER)?.let(::decodeReminder)

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun store(context: Context, reminders: List<PassReminder>) {
    val encoded = reminders.mapTo(mutableSetOf(), ::encodeReminder)
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putStringSet(STORED, encoded) }
}

private fun deliveryKey(reminder: PassReminder) = "${reminder.id}$DELIVERY_SEPARATOR${reminder.triggerAtMillis}"

private fun readDelivered(context: Context): Set<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(DELIVERED, emptySet()).orEmpty()

private fun markDelivered(context: Context, key: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putStringSet(DELIVERED, readDelivered(context) + key) }
}

private fun pruneDelivered(context: Context, activeIds: Set<String>) {
    val kept = readDelivered(context).filterTo(mutableSetOf()) { it.substringBeforeLast(DELIVERY_SEPARATOR) in activeIds }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putStringSet(DELIVERED, kept) }
}

private fun storePolicySettings(context: Context, settings: NotificationPolicySettings) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
        putInt(ACCESS_WINDOW, settings.accessWindowMinutes)
        putBoolean(EXACT_TIMING, settings.exactTiming)
        putBoolean(ACTIONS_ENABLED, settings.actionsEnabled)
        putString(LOCK_SCREEN_DETAIL, settings.lockScreenDetail.name)
        putBoolean(LOCK_ALL_PASSES, settings.lockAllPasses)
    }
}

private fun readPolicySettings(context: Context): NotificationPolicySettings =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).run {
        NotificationPolicySettings(
            accessWindowMinutes = getInt(ACCESS_WINDOW, 15),
            exactTiming = getBoolean(EXACT_TIMING, false),
            actionsEnabled = getBoolean(ACTIONS_ENABLED, true),
            lockScreenDetail = getString(LOCK_SCREEN_DETAIL, null)
                ?.let { runCatching { NotificationLockScreenDetail.valueOf(it) }.getOrNull() }
                ?: NotificationLockScreenDetail.HIDE_SENSITIVE,
            lockAllPasses = getBoolean(LOCK_ALL_PASSES, false),
            strings = notificationStrings(context),
        )
    }

private fun notificationStrings(context: Context) = NotificationStrings(
    passReminder = context.getString(R.string.reminder_pass_reminder),
    passExpired = context.getString(R.string.reminder_pass_expired),
    timeLabel = { epochMillis -> DateFormat.getTimeFormat(context).format(Date(epochMillis)) },
    startsIn = { remaining ->
        val minutes = durationMinutes(remaining)
        if (minutes < 60) {
            context.resources.getQuantityString(R.plurals.reminder_starts_in_minutes, minutes.toInt(), minutes.toInt())
        } else {
            val hours = durationHours(minutes)
            context.resources.getQuantityString(R.plurals.reminder_starts_in_hours, hours.toInt(), hours.toInt())
        }
    },
    expiresIn = { remaining ->
        val minutes = durationMinutes(remaining)
        if (minutes < 60) {
            context.resources.getQuantityString(R.plurals.reminder_expires_in_minutes, minutes.toInt(), minutes.toInt())
        } else {
            val hours = durationHours(minutes)
            context.resources.getQuantityString(R.plurals.reminder_expires_in_hours, hours.toInt(), hours.toInt())
        }
    },
)

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
        .put("isExpiration", isExpiration)
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
        isExpiration = value.optBoolean("isExpiration", false),
    )
}.getOrNull()

private val RESCHEDULE_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)
/** The first channel keeps its original id so existing user choices survive the split. */
private const val CHANNEL_ID = "pass_reminders"
private const val CHANNEL_ACCESS = "pass_access"
private const val CHANNEL_EXPIRATION = "pass_expiration"
private val REMINDER_CHANNELS = listOf(CHANNEL_ID, CHANNEL_ACCESS, CHANNEL_EXPIRATION)
private const val DELIVERED = "delivered"
private const val DELIVERY_SEPARATOR = "@"
private const val EXTRA_REMINDER = "reminder"
private const val PREFS = "pass_reminder_schedule"
private const val STORED = "reminders"
private const val ACCESS_WINDOW = "access_window"
private const val EXACT_TIMING = "exact_timing"
private const val ACTIONS_ENABLED = "actions_enabled"
private const val LOCK_SCREEN_DETAIL = "lock_screen_detail"
private const val LOCK_ALL_PASSES = "lock_all_passes"
private const val ACTION_LIFECYCLE = "dev.lalogo.passtick.action.REMINDER_LIFECYCLE"
private const val ACTION_SNOOZE = "dev.lalogo.passtick.action.REMINDER_SNOOZE"
private const val ACTION_SNOOZE_DELIVERY = "dev.lalogo.passtick.action.REMINDER_SNOOZE_DELIVERY"
private const val EXPIRATION_NOTIFICATION_KEY = ":expiration"
private const val SNOOZE_REQUEST_KEY = "snooze"
private const val SNOOZE_DELIVERY_REQUEST_KEY = "snooze-delivery"
private const val MAX_ACTIONS = 3
private const val PENDING_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
internal const val SNOOZE_MILLIS = 10 * 60_000L
