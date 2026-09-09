package org.ligi.passandroid.reminder

import androidx.core.app.NotificationCompat
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AndroidReminderSchedulerTest {
    @Test
    fun resyncShowsReminderWhoseLeadTriggerAlreadyPassed() {
        val reminder = PassReminder("lead", "pass", "Title", 20_000, 10_000, 30_000)
        val result = NotificationPolicy.evaluate(reminder, nowMillis = 15_000)
        assertThat(deliveryDecision(result)).isEqualTo(ReminderDeliveryDecision.SHOW_DUE)
    }

    @Test
    fun oneReminderOwnsLifecycleTransitionsForMultipleLeadTimes() {
        val early = PassReminder("60-min", "pass", "Title", 100_000, 40_000, 120_000)
        val late = early.copy(id = "15-min", triggerAtMillis = 85_000, ownsLifecycle = false)
        assertThat(lifecycleOwners(listOf(early, late))).containsExactly(early)
    }

    @Test
    fun notificationIdIsStablePerPass() {
        assertThat(notificationId("pass-1")).isEqualTo(notificationId("pass-1"))
        assertThat(notificationId("pass-1")).isNotEqualTo(notificationId("pass-2"))
    }

    @Test
    fun exactAlarmRequiresPreferenceAndSystemAccess() {
        assertThat(useExactAlarm(enabled = false, allowed = true)).isFalse()
        assertThat(useExactAlarm(enabled = true, allowed = false)).isFalse()
        assertThat(useExactAlarm(enabled = true, allowed = true)).isTrue()
    }

    @Test
    fun reminderEncodingPreservesExactOverride() {
        val reminder = PassReminder(
            "id", "pass", "Title", 2_000, 1_000, 3_000,
            exactTiming = true,
            enabledActions = setOf(NotificationAction.OPEN_CODE),
            ownsLifecycle = false,
        )
        assertThat(decodeReminder(encodeReminder(reminder))).isEqualTo(reminder)
        assertThat(decodeReminder(encodeReminder(reminder.copy(exactTiming = null)))?.exactTiming).isNull()
    }

    @Test
    fun lockScreenLevelsMapToNotificationVisibility() {
        assertThat(notificationVisibility(NotificationLockScreenDetail.FULL)).isEqualTo(NotificationCompat.VISIBILITY_PUBLIC)
        assertThat(notificationVisibility(NotificationLockScreenDetail.HIDE_SENSITIVE)).isEqualTo(NotificationCompat.VISIBILITY_PRIVATE)
        assertThat(notificationVisibility(NotificationLockScreenDetail.HIDDEN)).isEqualTo(NotificationCompat.VISIBILITY_SECRET)
        assertThat(showsPublicVersion(NotificationLockScreenDetail.HIDE_SENSITIVE)).isTrue()
        assertThat(showsPublicVersion(NotificationLockScreenDetail.HIDDEN)).isFalse()
    }
}
