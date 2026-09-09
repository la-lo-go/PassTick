package org.ligi.passandroid.reminder

import kotlin.math.max

enum class NotificationPhase { UPCOMING, ACCESS }

enum class NotificationAction { OPEN_CODE, DIRECTIONS }

enum class NotificationDisposition { SCHEDULE, SHOW, CANCEL }

enum class NotificationLockScreenDetail { FULL, HIDE_SENSITIVE, HIDDEN }

data class NotificationPolicySettings(
    val accessWindowMinutes: Int = 15,
    val exactTiming: Boolean = false,
    val actionsEnabled: Boolean = true,
    val lockScreenDetail: NotificationLockScreenDetail = NotificationLockScreenDetail.HIDE_SENSITIVE,
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
    fun evaluate(
        reminder: PassReminder,
        nowMillis: Long,
        settings: NotificationPolicySettings = NotificationPolicySettings(),
    ): NotificationPolicyResult {
        val phase = phase(reminder, nowMillis, settings.accessWindowMinutes)
        val disposition = when {
            nowMillis >= reminder.eventAtMillis -> NotificationDisposition.CANCEL
            nowMillis < reminder.triggerAtMillis -> NotificationDisposition.SCHEDULE
            else -> NotificationDisposition.SHOW
        }
        val body = body(reminder, nowMillis, phase)
        return NotificationPolicyResult(
            title = reminder.title,
            body = body,
            publicTitle = if (reminder.isProtected) "Pass reminder" else reminder.title,
            publicBody = if (reminder.isProtected) publicBody(reminder, nowMillis, phase) else body,
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

    private fun body(reminder: PassReminder, nowMillis: Long, phase: NotificationPhase): String =
        listOfNotNull(phaseText(reminder, nowMillis, phase), reminder.locationLabel?.trim()?.takeIf(String::isNotEmpty))
            .joinToString(" · ")

    private fun publicBody(reminder: PassReminder, nowMillis: Long, phase: NotificationPhase): String =
        phaseText(reminder, nowMillis, phase)

    private fun phaseText(reminder: PassReminder, nowMillis: Long, phase: NotificationPhase): String = when (phase) {
        NotificationPhase.ACCESS, NotificationPhase.UPCOMING -> "Starts in ${durationLabel(reminder.eventAtMillis - nowMillis)}"
    }

    private fun actions(
        reminder: PassReminder,
        settings: NotificationPolicySettings,
    ): Set<NotificationAction> = buildSet {
        if (!settings.actionsEnabled) return@buildSet
        val enabled = reminder.enabledActions ?: NotificationAction.entries.toSet()
        if (reminder.hasBarcode && NotificationAction.OPEN_CODE in enabled) add(NotificationAction.OPEN_CODE)
        if (reminder.hasLocation && NotificationAction.DIRECTIONS in enabled) add(NotificationAction.DIRECTIONS)
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

    private fun durationLabel(durationMillis: Long): String {
        val minutes = max(1, (durationMillis + MINUTE - 1) / MINUTE)
        if (minutes < 60) return "$minutes ${if (minutes == 1L) "minute" else "minutes"}"
        val hours = (minutes + 59) / 60
        return "$hours ${if (hours == 1L) "hour" else "hours"}"
    }

    private val PassReminder.hasLocation: Boolean
        get() = !locationLabel.isNullOrBlank() || latitude != null && longitude != null

    private const val MINUTE = 60_000L
}
