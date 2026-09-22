package org.ligi.passandroid.reminder

import kotlin.math.max

enum class NotificationPhase { UPCOMING, ACCESS }

enum class NotificationAction { OPEN_CODE, DIRECTIONS, SNOOZE }

enum class NotificationDisposition { SCHEDULE, SHOW, CANCEL }

enum class NotificationLockScreenDetail { FULL, HIDE_SENSITIVE, HIDDEN }

data class NotificationStrings(
    val passReminder: String = "Pass reminder",
    val passExpired: String = "Pass expired",
    val startsIn: (durationMillis: Long) -> String = { "Starts in ${durationLabel(it)}" },
    val expiresIn: (durationMillis: Long) -> String = { "Expires in ${durationLabel(it)}" },
)

data class NotificationPolicySettings(
    val accessWindowMinutes: Int = 15,
    val exactTiming: Boolean = false,
    val actionsEnabled: Boolean = true,
    val lockScreenDetail: NotificationLockScreenDetail = NotificationLockScreenDetail.HIDE_SENSITIVE,
    val lockAllPasses: Boolean = false,
    val strings: NotificationStrings = NotificationStrings(),
)

data class NotificationPolicyResult(
    val title: String,
    val body: String,
    val publicTitle: String,
    val publicBody: String,
    val phase: NotificationPhase,
    val actions: Set<NotificationAction>,
    val disposition: NotificationDisposition,
    val nextTriggerAtMillis: Long?,
    val exactTiming: Boolean,
    val lockScreenDetail: NotificationLockScreenDetail,
)

object NotificationPolicy {
    /**
     * A snooze delivery is always shown; the CANCEL branch would drop a reminder whose event started.
     */
    fun evaluate(
        reminder: PassReminder,
        nowMillis: Long,
        settings: NotificationPolicySettings = NotificationPolicySettings(),
        snoozed: Boolean = false,
    ): NotificationPolicyResult {
        val phase = phase(reminder, nowMillis, settings.accessWindowMinutes)
        val disposition = when {
            snoozed -> NotificationDisposition.SHOW
            nowMillis >= reminder.eventAtMillis -> NotificationDisposition.CANCEL
            nowMillis < reminder.triggerAtMillis -> NotificationDisposition.SCHEDULE
            else -> NotificationDisposition.SHOW
        }
        val body = body(reminder, nowMillis, phase, settings)
        val isProtected = reminder.isProtected || settings.lockAllPasses
        val title = if (reminder.isExpiration) settings.strings.passExpired else reminder.title
        return NotificationPolicyResult(
            title = title,
            body = body,
            publicTitle = if (isProtected) settings.strings.passReminder else title,
            publicBody = if (isProtected) publicBody(reminder, nowMillis, phase, settings) else body,
            phase = phase,
            actions = if (disposition == NotificationDisposition.CANCEL) emptySet() else actions(reminder, settings),
            disposition = disposition,
            nextTriggerAtMillis = nextTrigger(reminder, nowMillis, phase, disposition, settings),
            exactTiming = reminder.exactTiming ?: settings.exactTiming,
            lockScreenDetail = settings.lockScreenDetail,
        )
    }

    private fun phase(reminder: PassReminder, nowMillis: Long, accessWindowMinutes: Int) = when {
        reminder.eventAtMillis - nowMillis <= accessWindowMinutes.coerceAtLeast(0) * MINUTE -> NotificationPhase.ACCESS
        else -> NotificationPhase.UPCOMING
    }

    private fun body(
        reminder: PassReminder,
        nowMillis: Long,
        phase: NotificationPhase,
        settings: NotificationPolicySettings,
    ): String = listOfNotNull(
        phaseText(reminder, nowMillis, phase, settings),
        reminder.locationLabel?.trim()?.takeIf(String::isNotEmpty),
    ).joinToString(" · ")

    private fun publicBody(
        reminder: PassReminder,
        nowMillis: Long,
        phase: NotificationPhase,
        settings: NotificationPolicySettings,
    ): String = phaseText(reminder, nowMillis, phase, settings)

    private fun phaseText(
        reminder: PassReminder,
        nowMillis: Long,
        phase: NotificationPhase,
        settings: NotificationPolicySettings,
    ): String = when (phase) {
        NotificationPhase.ACCESS, NotificationPhase.UPCOMING ->
            if (reminder.isExpiration) {
                settings.strings.expiresIn(reminder.eventAtMillis - nowMillis)
            } else {
                settings.strings.startsIn(reminder.eventAtMillis - nowMillis)
            }
    }

    private fun actions(
        reminder: PassReminder,
        settings: NotificationPolicySettings,
    ): Set<NotificationAction> = buildSet {
        if (!settings.actionsEnabled) return@buildSet
        val enabled = reminder.enabledActions ?: NotificationAction.entries.toSet()
        if (reminder.hasBarcode && NotificationAction.OPEN_CODE in enabled) add(NotificationAction.OPEN_CODE)
        if (reminder.hasLocation && NotificationAction.DIRECTIONS in enabled) add(NotificationAction.DIRECTIONS)
        if (NotificationAction.SNOOZE in enabled) add(NotificationAction.SNOOZE)
    }

    private fun nextTrigger(
        reminder: PassReminder,
        nowMillis: Long,
        phase: NotificationPhase,
        disposition: NotificationDisposition,
        settings: NotificationPolicySettings,
    ): Long? = when {
        disposition == NotificationDisposition.CANCEL -> null
        disposition == NotificationDisposition.SCHEDULE -> reminder.triggerAtMillis
        !reminder.ownsLifecycle -> null
        phase == NotificationPhase.UPCOMING -> max(
            nowMillis + MINUTE,
            reminder.eventAtMillis - settings.accessWindowMinutes.coerceAtLeast(0) * MINUTE,
        )
        phase == NotificationPhase.ACCESS -> reminder.eventAtMillis
        else -> reminder.endAtMillis
    }?.takeIf { it > nowMillis }

    private val PassReminder.hasLocation: Boolean
        get() = !locationLabel.isNullOrBlank() || latitude != null && longitude != null
}

internal fun durationMinutes(durationMillis: Long): Long = max(1, (durationMillis + MINUTE - 1) / MINUTE)

internal fun durationHours(minutes: Long): Long = (minutes + 59) / 60

internal fun durationLabel(durationMillis: Long): String {
    val minutes = durationMinutes(durationMillis)
    if (minutes < 60) return "$minutes ${if (minutes == 1L) "minute" else "minutes"}"
    val hours = durationHours(minutes)
    return "$hours ${if (hours == 1L) "hour" else "hours"}"
}

private const val MINUTE = 60_000L
