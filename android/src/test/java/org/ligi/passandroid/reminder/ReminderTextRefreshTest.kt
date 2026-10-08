package org.ligi.passandroid.reminder

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ReminderTextRefreshTest {
    private val minute = 60_000L

    @Test
    fun `refresh lands just after the minute label changes`() {
        assertThat(labelChangeDelayMillis(45 * minute)).isEqualTo(minute + 1_000)
        assertThat(labelChangeDelayMillis(44 * minute + 30_000)).isEqualTo(30_000 + 1_000)
    }

    @Test
    fun `hour labels change when the remaining time drops below the previous whole hour`() {
        assertThat(labelChangeDelayMillis(90 * minute)).isEqualTo(30 * minute + 1_000)
        assertThat(labelChangeDelayMillis(60 * minute)).isEqualTo(minute + 1_000)
    }

    @Test
    fun `the body shows the remaining time followed by the start time in parentheses`() {
        val reminder = PassReminder(id = "e", passId = "p", title = "Show", eventAtMillis = 100 * minute, triggerAtMillis = 0)
        val settings = NotificationPolicySettings(
            strings = NotificationStrings(timeLabel = { "18:30" }),
        )

        val body = NotificationPolicy.evaluate(reminder, 55 * minute, settings).body

        assertThat(body).isEqualTo("Starts in 45 minutes (18:30)")
    }
}
